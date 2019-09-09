/*
 * arcus-spring - Arcus as a caching provider for the Spring Cache Abstraction
 * Copyright 2011-2014 NAVER Corp.
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

import net.spy.memcached.ArcusClient;
import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.transcoders.Transcoder;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 스프링 CacheManager의 Arcus 구현체.
 *
 * 미리 정의하지 않은 이름의 캐시에 대해 get 요청을 받으면 (SimpleCacheManager와 다르게) 기본 설정으로 새 캐시를 생성하고 저장합니다.
 */
public class ArcusCacheManager extends AbstractCacheManager implements DisposableBean {
  private final ArcusClientPool arcusClientPool;
  private final String serviceId;
  private final Transcoder<Object> operationTranscoder;
  private final int timeoutMillis;
  private final int defaultExpireSeconds;
  private final Map<String, Integer> nameToExpireSeconds;
  private final boolean wantToGetException;

  public ArcusCacheManager(
    String adminAddress,
    String serviceCode,
    String serviceId,
    ConnectionFactoryBuilder connectionFactoryBuilder,
    Transcoder<Object> operationTranscoder,
    int poolSize,
    int timeoutMillis,
    int defaultExpireSeconds,
    Map<String, Integer> nameToExpireSeconds,
    boolean wantToGetException) {

    this.arcusClientPool =
      ArcusClient.createArcusClientPool(adminAddress, serviceCode, connectionFactoryBuilder, poolSize);
    this.serviceId = serviceId;
    this.operationTranscoder = operationTranscoder;
    this.timeoutMillis = timeoutMillis;
    this.defaultExpireSeconds = defaultExpireSeconds;
    this.nameToExpireSeconds = nameToExpireSeconds;
    this.wantToGetException = wantToGetException;
  }

  @Override
  protected Collection<? extends Cache> loadCaches() {
    List<Cache> caches = new ArrayList<Cache>(this.nameToExpireSeconds.size());
    for (Map.Entry<String, Integer> nameAndExpireSeconds : this.nameToExpireSeconds.entrySet()) {
      caches.add(
        this.createCache(nameAndExpireSeconds.getKey(), nameAndExpireSeconds.getValue()));
    }

    return caches;
  }

  @Override
  protected Cache getMissingCache(String name) {
    return this.createCache(name, this.defaultExpireSeconds);
  }

  /**
   * 캐시를 생성합니다.
   *
   * @param name          생성할 캐시 이름
   * @param expireSeconds 생성할 캐시의 TTL (초)
   * @return 생성된 캐시
   */
  private Cache createCache(String name, int expireSeconds) {
    ArcusCache cache = new ArcusCache();
    cache.setName(name);
    cache.setExpireSeconds(expireSeconds);
    cache.setServiceId(this.serviceId);
    cache.setTimeoutMilliSeconds(this.timeoutMillis);
    cache.setArcusClient(this.arcusClientPool);
    cache.setOperationTranscoder(this.operationTranscoder);
    cache.setWantToGetException(this.wantToGetException);

    return cache;
  }

  @Override
  public void destroy() {
    this.arcusClientPool.shutdown();
  }
}
