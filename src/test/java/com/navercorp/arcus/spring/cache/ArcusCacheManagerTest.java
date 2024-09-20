/*
 * arcus-spring - Arcus as a caching provider for the Spring Cache Abstraction
 * Copyright 2019 JaM2in Co., Ltd.
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

import java.lang.reflect.Field;
import java.util.Collection;

import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.transcoders.SerializingTranscoder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.ReflectionUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("/arcus_spring_arcusCacheManager_test.xml")
class ArcusCacheManagerTest {

  private static final String SERVICE_ID = "test-service-id";
  private static final String SERVICE_PREFIX = "test-prefix";
  private static final int TIMEOUT_MILLIS = 100;
  private static final int DEFAULT_EXPIRE_SECONDS = 1;
  private static final boolean WANT_TO_GET_EXCEPTION = false;
  private static final String PRE_DEFINED_CACHE_NAME = "pre-defined-cache";
  private static final int PRE_DEFINED_EXPIRE_SECONDS = 2;

  @Autowired
  private ArcusCacheManager arcusCacheManagerFromClient;

  @Autowired
  private ArcusCacheManager arcusCacheManagerFromAddress;

  @Test
  void getPreDefinedCache() {
    ArcusCache cache = (ArcusCache) this.arcusCacheManagerFromClient.getCache(PRE_DEFINED_CACHE_NAME);
    assertNotNull(cache);
    ArcusCacheConfiguration config = cache.getCacheConfiguration();

    assertEquals(PRE_DEFINED_CACHE_NAME, cache.getName());
    assertEquals(SERVICE_ID, config.getServiceId());
    assertEquals(SERVICE_PREFIX, config.getPrefix());
    assertEquals(PRE_DEFINED_EXPIRE_SECONDS, config.getExpireSeconds());
    assertEquals(TIMEOUT_MILLIS, config.getTimeoutMilliSeconds());
    assertInstanceOf(SerializingTranscoder.class, config.getOperationTranscoder());
    assertEquals(WANT_TO_GET_EXCEPTION, config.isWantToGetException());
  }

  @Test
  void getMissingCache() {
    String nonDefinedCache = "non-defined-cache";
    ArcusCache cache = (ArcusCache) this.arcusCacheManagerFromClient.getCache(nonDefinedCache);
    assertNotNull(cache);
    ArcusCacheConfiguration config = cache.getCacheConfiguration();

    assertEquals(nonDefinedCache, cache.getName());
    assertEquals(SERVICE_ID, config.getServiceId());
    assertEquals(SERVICE_PREFIX, config.getPrefix());
    assertEquals(DEFAULT_EXPIRE_SECONDS, config.getExpireSeconds());
    assertEquals(TIMEOUT_MILLIS, config.getTimeoutMilliSeconds());
    assertInstanceOf(SerializingTranscoder.class, config.getOperationTranscoder());
    assertEquals(WANT_TO_GET_EXCEPTION, config.isWantToGetException());
  }

  @Test
  void getCacheNameAndSize() {
    String nonDefinedCache = "non-defined-cache";
    this.arcusCacheManagerFromClient.getCache(nonDefinedCache); // Create missing cache

    String nonDefinedCache2 = "non-defined-cache-2"; // Create missing cache
    this.arcusCacheManagerFromClient.getCache(nonDefinedCache2);

    String nonDefinedCache3 = "non-defined-cache-3"; // Create missing cache
    this.arcusCacheManagerFromClient.getCache(nonDefinedCache3);

    Collection<String> cacheNames = this.arcusCacheManagerFromClient.getCacheNames();

    assertEquals(4, cacheNames.size());
    assertTrue(cacheNames.contains(PRE_DEFINED_CACHE_NAME));
    assertTrue(cacheNames.contains(nonDefinedCache));
    assertTrue(cacheNames.contains(nonDefinedCache2));
    assertTrue(cacheNames.contains(nonDefinedCache3));
  }

  @Test
  void shutdownClientIfArcusClientPoolManagedInCacheManager() throws Exception {
    Field clientField = ReflectionUtils.findField(ArcusCacheManager.class, "client");
    assertNotNull(clientField);
    clientField.setAccessible(true);

    this.arcusCacheManagerFromClient.destroy();
    ArcusClientPool client1 = (ArcusClientPool) ReflectionUtils.getField(clientField, this.arcusCacheManagerFromClient);
    assertNotNull(client1);

    String key = Math.random() + this.getClass().getSimpleName();
    String value = this.getClass().getSimpleName() + Math.random();

    assertTrue(client1.set(key, 0, value).get());
    assertEquals(client1.get(key), value);

    this.arcusCacheManagerFromAddress.destroy();
    ArcusClientPool client2 = (ArcusClientPool) ReflectionUtils.getField(clientField, this.arcusCacheManagerFromAddress);
    assertNotNull(client2);

    assertThrows(IllegalStateException.class, () -> client2.get(key));
  }
}
