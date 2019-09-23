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
  private ArcusClientPool client;
  protected ArcusCacheConfiguration defaultConfiguration;
  protected Map<String, ArcusCacheConfiguration> initialCacheConfigs;
  private boolean internalClient;

  /**
   * 외부에서 생성한 Arcus 클라이언트를 이용해 캐시 매니저를 생성합니다.
   * 캐시 매니저의 생성, 소멸시 Arcus 클라이언트의 연결, 해제 작업을 하지 않습니다.
   *
   * @param client               ArcusClientPool 유형의 클라이언트
   * @param defaultConfiguration 정의되지 않은 캐시의 기본 설정
   * @param initialCacheConfigs  생성할 캐시들의 이름과 설정들의 집합
   */
  public ArcusCacheManager(
    ArcusClientPool client,
    ArcusCacheConfiguration defaultConfiguration,
    Map<String, ArcusCacheConfiguration> initialCacheConfigs
  ) {
    this.client = client;
    this.defaultConfiguration = defaultConfiguration;
    this.initialCacheConfigs = initialCacheConfigs;
    this.internalClient = false;
  }

  /**
   * 캐시 매니저 내부에서 Arcus 클라이언트를 생성 및 소멸을 관리하기 위한 생성자.
   *
   * @param adminAddress             Arcus 클라이언트를 생성하기 위해 필요한 캐시의 주소
   * @param serviceCode              Arcus 클라이언트를 생성하기 위해 필요한 서비스 코드
   * @param connectionFactoryBuilder Arcus 클라이언트를 생성하기 위해 필요한 ConnectionFactory 빌더
   * @param poolSize                 Arcus 클라이언트를 생성하기 위해 필요한 클라이언트 풀 사이즈
   * @param defaultConfiguration     정의되지 않은 캐시의 기본 설정
   * @param initialCacheConfigs      생성할 캐시들의 이름과 설정들의 집합
   */
  public ArcusCacheManager(
    String adminAddress,
    String serviceCode,
    ConnectionFactoryBuilder connectionFactoryBuilder,
    int poolSize,
    ArcusCacheConfiguration defaultConfiguration,
    Map<String, ArcusCacheConfiguration> initialCacheConfigs
  ) {
    this(
      ArcusClient.createArcusClientPool(adminAddress, serviceCode, connectionFactoryBuilder, poolSize),
      defaultConfiguration,
      initialCacheConfigs
    );
    this.internalClient = true;
  }

  @Override
  protected Collection<? extends Cache> loadCaches() {
    List<Cache> caches = new ArrayList<Cache>(initialCacheConfigs.size());
    for (Map.Entry<String, ArcusCacheConfiguration> entry : initialCacheConfigs.entrySet()) {
      caches.add(
        createCache(entry.getKey(), entry.getValue()));
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
  @SuppressWarnings("deprecation")
  protected Cache createCache(String name, ArcusCacheConfiguration configuration) {
    ArcusCache cache = new ArcusCache();
    cache.setName(name);
    cache.setServiceId(configuration.getServiceId());
    cache.setPrefix(configuration.getPrefix());
    cache.setArcusClient(client);
    cache.setExpireSeconds(configuration.getExpireSeconds());
    cache.setTimeoutMilliSeconds(configuration.getTimeoutMilliSeconds());
    cache.setOperationTranscoder(configuration.getOperationTranscoder());
    cache.setWantToGetException(true);

    return cache;
  }

    @Override
    public void destroy() {
      if (internalClient) {
        client.shutdown();
      }
    }
}
