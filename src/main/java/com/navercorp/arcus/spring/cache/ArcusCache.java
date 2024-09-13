/*
 * arcus-spring - Arcus as a caching provider for the Spring Cache Abstraction
 * Copyright 2011-2014 NAVER Corp.
 * Copyright 2014-2021 JaM2in Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.arcus.spring.cache;

import com.navercorp.arcus.spring.cache.front.ArcusFrontCache;
import com.navercorp.arcus.spring.concurrent.DefaultKeyLockProvider;
import com.navercorp.arcus.spring.concurrent.KeyLockProvider;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.lang.NonNull;

import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.transcoders.Transcoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;

/**
 * 스프링 Cache의 Arcus 구현체.
 * <p>
 * Arcus 캐시 키의 기본 구조는 prefix:subkey 입니다. prefix는 사용자가 그룹으로 생성하고자 하는 subkey들의 집합이며
 * ArcusCache에서는 서비스 또는 빌드 단계 등의 구분을 위해 serviceId + <prefix | name> 문자열을 캐시 키의 prefix로 정의합니다.
 *
 * </p>
 * <pre>{@code
 * <bean id="operationTranscoderA" class="net.spy.memcached.transcoders.SerializingTranscoder">
 *   <property name="charset" value="UTF-8" />
 *   <property name="compressionThreshold" value="400" />
 * </bean>
 *
 * <bean id="operationTranscoderB" class="net.spy.memcached.transcoders.SerializingTranscoder">
 *   <property name="charset" value="UTF-8" />
 *   <property name="compressionThreshold" value="1024" />
 * </bean>
 *
 * <bean id="arcusCacheManager" class="org.springframework.cache.support.SimpleCacheManager">
 *   <property name="caches">
 *     <list>
 *       <bean p:name="member" p:timeoutMilliSeconds="500" parent="defaultArcusCache" p:operationTranscoder-ref="operationTranscoderA" />
 *       <bean p:name="memberList" p:expireSeconds="3000" parent="defaultArcusCache" p:operationTranscoder-ref="operationTranscoderB" />
 *     </list>
 *   </property>
 * </bean>
 *
 * <bean id="defaultArcusCache" class="com.navercorp.arcus.spring.cache.ArcusCache"
 * p:arcusClient-ref="arcusClient" p:timeoutMilliSeconds="500"
 * p:expireSeconds="3000" abstract="true" serviceId="beta-" />
 * }</pre>
 * <p>
 * 이렇게 설정했을때, 캐시의 키 값으로 생성되는 값은 <span>beta-member:메서드 매개변수로 만든 문자열</span>이 됩니다.
 * </p>
 */
@SuppressWarnings({"DeprecatedIsStillUsed", "deprecation"})
public class ArcusCache extends AbstractValueAdaptingCache {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private String name;
  private ArcusClientPool arcusClient;
  private final ArcusCacheConfiguration configuration;
  private KeyLockProvider keyLockProvider = new DefaultKeyLockProvider();

  /**
   * This constructor doesn't guarantee to make safe and complete instance.
   * because essential fields such as name or arcusClient are not checked.
   */
  @Deprecated
  public ArcusCache() {
    super(ArcusCacheConfiguration.DEFAULT_ALLOW_NULL_VALUES);
    this.configuration = new ArcusCacheConfiguration();
  }

  public ArcusCache(String name, ArcusClientPool clientPool) {
    this(name, clientPool, new ArcusCacheConfiguration());
  }

  public ArcusCache(String name, ArcusClientPool clientPool, ArcusCacheConfiguration configuration) {
    super(requireNonNull(configuration).isAllowNullValues());
    this.setName(name);
    this.setArcusClient(clientPool);
    this.configuration = configuration;
  }

  /*
   *  Verify if it is not null before calling the super method.
   */
  private static ArcusCacheConfiguration requireNonNull(ArcusCacheConfiguration cacheConfig) {
    Assert.notNull(cacheConfig, "Cache configuration must not be null.");
    return cacheConfig;
  }

  @Override
  @NonNull
  public String getName() {
    return this.name;
  }

  @Override
  @NonNull
  public Object getNativeCache() {
    return this.arcusClient;
  }

  public KeyLockProvider getKeyLockProvider() {
    return keyLockProvider;
  }

  public void setKeyLockProvider(KeyLockProvider keyLockProvider) {
    Assert.notNull(keyLockProvider, "ArcusCache's 'keyLockProvider' property must not be null.");
    this.keyLockProvider = keyLockProvider;
  }

  public ArcusCacheConfiguration getCacheConfiguration() {
    return this.configuration;
  }

  @Nullable
  @Override
  protected Object lookup(Object key) {
    String arcusKey = createArcusKey(key);
    try {
      return getValue(arcusKey);
    } catch (Exception e) {
      if (e instanceof InterruptedException || configuration.isWantToGetException()) {
        throw toRuntimeException(e);
      }
      logger.info("failed to lookup. error: {}, key: {}", e.getMessage(), arcusKey);
      return null;
    }
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(Object key, Callable<T> valueLoader) {
    ValueWrapper result = super.get(key);
    return result != null ? (T) result.get() : getSynchronized(key, valueLoader);
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private <T> T getSynchronized(Object key, Callable<T> valueLoader) {
    try {
      acquireWriteLockOnKey(key);
      ValueWrapper result = super.get(key);
      return result != null ? (T) result.get() : loadValue(key, valueLoader);
    } finally {
      releaseWriteLockOnKey(key);
    }
  }

  private <T> T loadValue(Object key, Callable<T> valueLoader) {
    T value;
    try {
      value = valueLoader.call();
    } catch (Exception e) {
      throw new ValueRetrievalException(key, valueLoader, e);
    }

    put(key, value);

    return value;
  }

  @Override
  public void put(final Object key, final Object value) {
    if (value == null && !isAllowNullValues()) {
      throw new IllegalArgumentException(String.format("Cache '%s' does not allow 'null' values. " +
              "Avoid storing null via '@Cacheable(unless=\"#result == null\")' or configure ArcusCache " +
              "to allow 'null' via ArcusCacheConfiguration.", name));
    }

    String arcusKey = createArcusKey(key);
    try {
      putValue(arcusKey, toStoreValue(value));
    } catch (Exception e) {
      if (e instanceof InterruptedException || configuration.isWantToGetException()) {
        throw toRuntimeException(e);
      }
      logger.info("failed to put. error: {}, key: {}", e.getMessage(), arcusKey);
    }
  }

  /**
   * @param key key
   * @param value value
   * @return 지정된 키에 대한 캐시 아이템이 존재하지 않았으며 지정된 값을 캐시에 저장하였다면 null을 리턴,
   * 캐시 아이템이 이미 존재하였다면 지정된 키에 대한 값을 불러와 리턴한다. 값을 불러올 때 비원자적으로
   * 수행되기 때문에 중간에 다른 캐시 연산 수행으로 인하여 새로운 값이 리턴 될 수 있으며 혹은 캐시 만료로 인해
   * ValueWrapper의 내부 value가 null이 되어 리턴될 수 있다.
   */
  @Nullable
  @Override
  public ValueWrapper putIfAbsent(Object key, Object value) {
    if (value == null && !isAllowNullValues()) {
      logger.info(String.format("Cache '%s' does not allow 'null' values. " +
              "Avoid storing null via '@Cacheable(unless=\"#result == null\")' or configure ArcusCache " +
              "to allow 'null' via ArcusCacheConfiguration.", name));
      return super.get(key);
    }

    String arcusKey = createArcusKey(key);
    try {
      return putIfAbsentValue(arcusKey, toStoreValue(value));
    } catch (Exception e) {
      if (e instanceof InterruptedException || configuration.isWantToGetException()) {
        throw toRuntimeException(e);
      }
      logger.info("failed to putIfAbsent. error: {}, key: {}", e.getMessage(), arcusKey);
      return super.get(key);
    }
  }

  @Override
  public void evict(final Object key) {
    String arcusKey = createArcusKey(key);
    logger.debug("evicting a key: {}", arcusKey);

    boolean success = false;

    try {
      OperationFuture<Boolean> future = arcusClient.delete(arcusKey);
      success = future.get(configuration.getTimeoutMilliSeconds(), TimeUnit.MILLISECONDS);
      if (!success) {
        OperationStatus status = future.getStatus();
        logger.info("failed to evict a key: {}, status: {}", arcusKey, status.getMessage());
      }
    } catch (Exception e) {
      if (e instanceof InterruptedException || configuration.isWantToGetException()) {
        throw toRuntimeException(e);
      }
      logger.info("failed to evict. error: {}, key: {}", e.getMessage(), arcusKey);
    } finally {
      ArcusFrontCache arcusFrontCache = configuration.getArcusFrontCache();
      if (arcusFrontCache != null && (success || configuration.isForceFrontCaching())) {
        arcusFrontCache.delete(arcusKey);
      }
    }
  }

  @Override
  public void clear() {
    String serviceId = configuration.getServiceId();
    String prefix = configuration.getPrefix();
    String arcusPrefix = serviceId + ((prefix != null) ? prefix : name);
    logger.debug("evicting every key that uses the prefix: {}", arcusPrefix);

    boolean success = false;

    try {
      OperationFuture<Boolean> future = arcusClient.flush(arcusPrefix);
      success = future.get(configuration.getTimeoutMilliSeconds(), TimeUnit.MILLISECONDS);
      if (!success) {
        OperationStatus status = future.getStatus();
        logger.info("failed to clear a prefix: {}, status: {}", arcusPrefix, status.getMessage());
      }
    } catch (Exception e) {
      if (e instanceof InterruptedException || configuration.isWantToGetException()) {
        throw toRuntimeException(e);
      }
      logger.info("failed to clear. error: {}, prefix: {}", e.getMessage(), arcusPrefix);
    } finally {
      ArcusFrontCache arcusFrontCache = configuration.getArcusFrontCache();
      if (arcusFrontCache != null && (success || configuration.isForceFrontCaching())) {
        arcusFrontCache.clear();
      }
    }
  }

  /**
   * serviceId, prefix, name 값을 사용하여 Arcus 캐시 키를 생성합니다.
   * <p> 캐시 키는 serviceId + (prefix | name) + ":" + key.toString() 형태로 구성됩니다. </p>
   * <p> prefix가 주어지지 않았다면, name을 prefix처럼 사용합니다. </p>
   * <p> 캐시 키의 길이가 250자를 넘을 경우에는 key.toString() 부분을 MD5로 해싱하여 사용합니다. </p>
   *
   * @param key key
   * @return 입력받은 키를 기반으로 캐시 키를 생성하고 반환한다. 입력받은 키의 타입에 따라 캐시 키의 형태가 달라질 수 있다
   */
  public String createArcusKey(final Object key) {
    Assert.notNull(key, "key must not be null.");
    String keyString;

    if (key instanceof ArcusStringKey) {
      keyString = ((ArcusStringKey) key).getStringKey();
    } else if (key instanceof Integer) {
      keyString = key.toString();
    } else {
      keyString = key.toString();
      int hash = ArcusStringKey.light_hash(keyString);
      keyString = keyString.replace(' ', '_') + hash;
    }

    String prefixString = getPrefixString();

    if (prefixString.length() + keyString.length() > 250) {
      return prefixString + DigestUtils.md5DigestAsHex(keyString.getBytes());
    }
    return prefixString + keyString;
  }

  private String getPrefixString() {
    String serviceId = configuration.getServiceId();
    String prefix = configuration.getPrefix();
    return serviceId + ((prefix != null) ? prefix : name) + ":";
  }

  @Deprecated
  public void setName(String name) {
    Assert.notNull(name, "ArcusCache's 'name' property must have a value.");
    this.name = name;
  }

  @Deprecated
  public void setExpireSeconds(int expireSeconds) {
    configuration.setExpireSeconds(expireSeconds);
  }

  @Deprecated
  public void setTimeoutMilliSeconds(long timeoutMilliseconds) {
    configuration.setTimeoutMilliSeconds(timeoutMilliseconds);
  }

  @Deprecated
  public void setArcusClient(ArcusClientPool arcusClient) {
    Assert.notNull(arcusClient, "ArcusCache's 'arcusClient' property must not be null.");
    this.arcusClient = arcusClient;
  }

  @Deprecated
  public String getServiceId() {
    return configuration.getServiceId();
  }

  @Deprecated
  public void setServiceId(String serviceId) {
    Assert.notNull(serviceId, "ArcusCache's 'serviceId' property must have a value.");
    configuration.setServiceId(serviceId);
  }

  @Deprecated
  public boolean isWantToGetException() {
    return configuration.isWantToGetException();
  }

  @Deprecated
  public void setWantToGetException(boolean wantToGetException) {
    configuration.setWantToGetException(wantToGetException);
  }

  @Deprecated
  public int getExpireSeconds() {
    return configuration.getExpireSeconds();
  }

  @Deprecated
  public long getTimeoutMilliSeconds() {
    return configuration.getTimeoutMilliSeconds();
  }

  @Deprecated
  public ArcusClientPool getArcusClient() {
    return arcusClient;
  }

  @Deprecated
  @Nullable
  public Transcoder<Object> getOperationTranscoder() {
    return configuration.getOperationTranscoder();
  }

  @Deprecated
  public void setOperationTranscoder(Transcoder<Object> operationTranscoder) {
    Assert.notNull(operationTranscoder, "ArcusCache's 'operationTranscoder' property must not be null.");
    configuration.setOperationTranscoder(operationTranscoder);
  }

  @Deprecated
  @Nullable
  public String getPrefix() {
    return configuration.getPrefix();
  }

  @Deprecated
  public void setPrefix(String prefix) {
    Assert.notNull(prefix, "ArcusCache's 'prefix' property must have a value.");
    configuration.setPrefix(prefix);
  }

  @Deprecated
  @Nullable
  public ArcusFrontCache getArcusFrontCache() {
    return configuration.getArcusFrontCache();
  }

  @Deprecated
  public void setArcusFrontCache(ArcusFrontCache arcusFrontCache) {
    Assert.notNull(arcusFrontCache, "ArcusCache's 'arcusFrontCache' property must not be null.");
    configuration.setArcusFrontCache(arcusFrontCache);
  }

  @Deprecated
  public int getFrontExpireSeconds() {
    return configuration.getFrontExpireSeconds();
  }

  @Deprecated
  public void setFrontExpireSeconds(int frontExpireSeconds) {
    configuration.setFrontExpireSeconds(frontExpireSeconds);
  }

  @Deprecated
  public void setForceFrontCaching(boolean forceFrontCaching) {
    this.configuration.setForceFrontCaching(forceFrontCaching);
  }

  @Deprecated
  public boolean getForceFrontCaching() {
    return this.configuration.isForceFrontCaching();
  }

  private void acquireWriteLockOnKey(Object key) {
    keyLockProvider.getLockForKey(key).writeLock().lock();
  }

  private void releaseWriteLockOnKey(Object key) {
    keyLockProvider.getLockForKey(key).writeLock().unlock();
  }

  private RuntimeException toRuntimeException(Exception e) {
    if (e instanceof RuntimeException) {
      return (RuntimeException) e;
    } else {
      return new RuntimeException(e);
    }
  }

  @Nullable
  private Object getValue(String arcusKey) throws Exception {
    logger.debug("getting value by key: {}", arcusKey);
    Object value;

    ArcusFrontCache arcusFrontCache = configuration.getArcusFrontCache();
    if (arcusFrontCache != null && (value = arcusFrontCache.get(arcusKey)) != null) {
      logger.debug("front cache hit for {}", arcusKey);
      return value;
    }

    GetFuture<Object> future;
    Transcoder<Object> operationTranscoder = configuration.getOperationTranscoder();
    if (operationTranscoder != null) {
      future = arcusClient.asyncGet(arcusKey, operationTranscoder);
    } else {
      future = arcusClient.asyncGet(arcusKey);
    }

    value = future.get(configuration.getTimeoutMilliSeconds(), TimeUnit.MILLISECONDS);
    if (value != null) {
      logger.debug("arcus cache hit for {}", arcusKey);
      if (arcusFrontCache != null) {
        arcusFrontCache.set(arcusKey, value, configuration.getFrontExpireSeconds());
      }
    } else {
      logger.debug("arcus cache miss for {}", arcusKey);
      OperationStatus status = future.getStatus();
      if (!status.isSuccess()) {
        logger.info("failed to get a key: {}, status: {}", arcusKey, status.getMessage());
      }
    }

    return value;
  }

  private void putValue(String arcusKey, Object value) throws Exception {
    logger.debug("trying to put key: {}", arcusKey);

    boolean success = false;

    try {
      OperationFuture<Boolean> future;
      Transcoder<Object> operationTranscoder = configuration.getOperationTranscoder();
      if (operationTranscoder != null) {
        future = arcusClient.set(arcusKey, configuration.getExpireSeconds(), value, operationTranscoder);
      } else {
        future = arcusClient.set(arcusKey, configuration.getExpireSeconds(), value);
      }

      success = future.get(configuration.getTimeoutMilliSeconds(), TimeUnit.MILLISECONDS);
      if (!success) {
        OperationStatus status = future.getStatus();
        logger.info("failed to put a key: {}, status: {}", arcusKey, status.getMessage());
      }
    } finally {
      ArcusFrontCache arcusFrontCache = configuration.getArcusFrontCache();
      if (arcusFrontCache != null && (success || configuration.isForceFrontCaching())) {
        arcusFrontCache.set(arcusKey, value, configuration.getFrontExpireSeconds());
      }
    }
  }

  private ValueWrapper putIfAbsentValue(String arcusKey, Object value) throws Exception {
    logger.debug("trying to add(putIfAbsent) key: {}", arcusKey);

    OperationFuture<Boolean> future;
    Transcoder<Object> operationTranscoder = configuration.getOperationTranscoder();
    int expireSeconds = configuration.getExpireSeconds();
    if (operationTranscoder != null) {
      future = arcusClient.add(arcusKey, expireSeconds, value, operationTranscoder);
    } else {
      future = arcusClient.add(arcusKey, expireSeconds, value);
    }

    boolean success = future.get(configuration.getTimeoutMilliSeconds(), TimeUnit.MILLISECONDS);
    if (!success) {
      OperationStatus status = future.getStatus();
      logger.info("failed to putIfAbsent a key: {}, status: {}", arcusKey, status.getMessage());
    } else {
      ArcusFrontCache arcusFrontCache = configuration.getArcusFrontCache();
      if (arcusFrontCache != null) {
        arcusFrontCache.set(arcusKey, value, configuration.getFrontExpireSeconds());
      }
    }

    return success ? null : toValueWrapper(getValue(arcusKey));
  }

}
