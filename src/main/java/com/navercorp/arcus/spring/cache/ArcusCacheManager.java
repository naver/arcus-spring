/*
 * arcus-spring - Arcus as a caching provider for the Spring Cache Abstraction
 * Copyright 2019-2021 JaM2in Co., Ltd.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.spy.memcached.ArcusClient;
import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.ConnectionFactoryBuilder;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.cache.Cache;
import org.springframework.cache.transaction.AbstractTransactionSupportingCacheManager;
import org.springframework.util.Assert;

/**
 * 스프링 CacheManager의 Arcus 구현체.
 * 미리 정의하지 않은 이름의 캐시에 대해 get 요청을 받으면 (SimpleCacheManager와 다르게) 기본 설정으로 새 캐시를 생성하고 저장합니다.
 */
public class ArcusCacheManager extends AbstractTransactionSupportingCacheManager implements DisposableBean {
  private final ArcusClientPool client;
  private final ArcusCacheConfiguration defaultConfiguration;
  private final Map<String, ArcusCacheConfiguration> initialCacheConfigs;
  private boolean internalClient;

  /**
   * 외부에서 생성한 Arcus 클라이언트를 이용해 캐시 매니저를 생성합니다.
   * 캐시 매니저의 생성, 소멸시 Arcus 클라이언트의 연결, 해제 작업을 하지 않습니다.
   *
   * @param client               ArcusClientPool 유형의 클라이언트
   * @param defaultConfiguration 정의되지 않은 캐시의 기본 설정
   * @param initialCacheConfigs  생성할 캐시들의 이름과 설정들의 집합
   */
  public ArcusCacheManager(ArcusClientPool client, ArcusCacheConfiguration defaultConfiguration,
                           Map<String, ArcusCacheConfiguration> initialCacheConfigs) {
    this.client = client;
    this.defaultConfiguration = defaultConfiguration;
    this.initialCacheConfigs = initialCacheConfigs;
    this.internalClient = false;
  }

  /**
   * 캐시 매니저 내부에서 Arcus 클라이언트를 생성 및 소멸을 관리하기 위한 생성자.
   *
   * @param adminAddress             Arcus 클라이언트를 생성하기 위해 필요한 ZooKeeper 주소
   * @param serviceCode              Arcus 클라이언트를 생성하기 위해 필요한 서비스 코드
   * @param connectionFactoryBuilder Arcus 클라이언트를 생성하기 위해 필요한 ConnectionFactory 빌더
   * @param poolSize                 Arcus 클라이언트를 생성하기 위해 필요한 클라이언트 풀 사이즈
   * @param defaultConfiguration     정의되지 않은 캐시의 기본 설정
   * @param initialCacheConfigs      생성할 캐시들의 이름과 설정들의 집합
   */
  public ArcusCacheManager(String adminAddress, String serviceCode, ConnectionFactoryBuilder connectionFactoryBuilder,
                           int poolSize, ArcusCacheConfiguration defaultConfiguration,
                           Map<String, ArcusCacheConfiguration> initialCacheConfigs) {
    this.client = ArcusClient.createArcusClientPool(adminAddress, serviceCode, connectionFactoryBuilder, poolSize);
    this.defaultConfiguration = defaultConfiguration;
    this.initialCacheConfigs = initialCacheConfigs;
    this.internalClient = true;
  }

  public static ArcusCacheManagerBuilder builder(ArcusClientPool arcusClientPool) {
    return new ArcusCacheManagerBuilder(arcusClientPool);
  }

  public static ArcusCacheManagerBuilder builder(String adminAddress,
                                                 String serviceCode,
                                                 ConnectionFactoryBuilder connectionFactoryBuilder,
                                                 int poolSize) {
    return new ArcusCacheManagerBuilder(adminAddress, serviceCode, connectionFactoryBuilder, poolSize);
  }

  @Override
  protected Collection<? extends Cache> loadCaches() {
    List<Cache> caches = new ArrayList<>(initialCacheConfigs.size());
    for (Map.Entry<String, ArcusCacheConfiguration> entry : initialCacheConfigs.entrySet()) {
      caches.add(createCache(entry.getKey(), entry.getValue()));
    }

    return caches;
  }

  @Override
  protected Cache getMissingCache(String name) {
    return createCache(name, defaultConfiguration);
  }

  /**
   * 캐시를 생성합니다.
   *
   * @param name          생성할 캐시 이름
   * @param configuration 생성할 캐시의 속성
   * @return 생성된 캐시
   */
  protected Cache createCache(String name, ArcusCacheConfiguration configuration) {
    return new ArcusCache(name, client, configuration);
  }

  @Override
  public void destroy() {
    if (internalClient) {
      client.shutdown();
    }
  }

  public static class ArcusCacheManagerBuilder {
    private final ArcusClientPool arcusClientPool;
    private final boolean internalClient;
    private final Map<String, ArcusCacheConfiguration> initialCaches = new LinkedHashMap<>();
    private boolean enableTransactions;
    private ArcusCacheConfiguration defaultConfiguration = new ArcusCacheConfiguration();

    private ArcusCacheManagerBuilder(ArcusClientPool arcusClientPool) {
      this.arcusClientPool = arcusClientPool;
      this.internalClient = false;
    }

    private ArcusCacheManagerBuilder(String adminAddress,
                                    String serviceCode,
                                    ConnectionFactoryBuilder connectionFactoryBuilder,
                                    int poolSize) {
      this.arcusClientPool = ArcusClient.createArcusClientPool(
              adminAddress, serviceCode, connectionFactoryBuilder, poolSize);
      this.internalClient = true;
    }

    public ArcusCacheManagerBuilder cacheDefaults(ArcusCacheConfiguration defaultCacheConfiguration) {
      this.defaultConfiguration = defaultCacheConfiguration;
      return this;
    }

    public ArcusCacheManagerBuilder initialCacheNames(Set<String> cacheNames) {
      Assert.notNull(cacheNames, "Cache names must not be null");

      cacheNames.forEach(cacheName -> initialCaches.put(cacheName, defaultConfiguration));
      return this;
    }

    public ArcusCacheManagerBuilder transactionAware() {
      this.enableTransactions = true;
      return this;
    }

    public ArcusCacheManagerBuilder withCacheConfiguration(String cacheName, ArcusCacheConfiguration cacheConfiguration) {
      Assert.notNull(cacheName, "Cache name must not be null");
      Assert.notNull(cacheConfiguration, "Cache configuration must not be null");

      this.initialCaches.put(cacheName, cacheConfiguration);
      return this;
    }

    public ArcusCacheManagerBuilder withInitialCacheConfigurations(Map<String, ArcusCacheConfiguration> cacheConfigurations) {
      Assert.notNull(cacheConfigurations, "Cache configurations must not be null");

      this.initialCaches.putAll(cacheConfigurations);
      return this;
    }

    public Optional<ArcusCacheConfiguration> getCacheConfigurationFor(String cacheName) {
      return Optional.ofNullable(this.initialCaches.get(cacheName));
    }

    public Set<String> getConfiguredCaches() {
      return Collections.unmodifiableSet(this.initialCaches.keySet());
    }

    /**
     * Create new instance of {@link ArcusCacheManager} with configuration options applied.
     *
     * @return new instance of {@link ArcusCacheManager}.
     */
    public ArcusCacheManager build() {
      Assert.state(arcusClientPool != null, "ArcusClient must not be null");

      ArcusCacheManager cacheManager = new ArcusCacheManager(arcusClientPool, defaultConfiguration, initialCaches);
      cacheManager.internalClient = this.internalClient;
      cacheManager.setTransactionAware(this.enableTransactions);

      return cacheManager;
    }

  }
}
