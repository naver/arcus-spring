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

import net.spy.memcached.ArcusClient;
import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.transcoders.SerializingTranscoder;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/arcus_spring_arcusCacheManager_test.xml")
public class ArcusCacheManagerTest {

  private static final String SERVICE_ID = "test-service-id";
  private static final String SERVICE_PREFIX = "test-prefix";
  private static final int TIMEOUT_MILLIS = 100;
  private static final int DEFAULT_EXPIRE_SECONDS = 1;
  private static final boolean WANT_TO_GET_EXCEPTION = true;
  private static final String PRE_DEFINED_CACHE_NAME = "pre-defined-cache";
  private static final int PRE_DEFINED_EXPIRE_SECONDS = 2;

  @Value("#{arcusConfig['url']}")
  private String url;

  @Value("#{arcusConfig['serviceCode']}")
  private String serviceCode;

  @Autowired
  private ArcusCacheManager arcusCacheManagerFromClient;

  @Autowired
  private ArcusCacheManager arcusCacheManagerFromAddress;

  @SuppressWarnings("deprecation")
  @Test
  public void testGetPreDefinedCache() {
    ArcusCache cache = (ArcusCache)this.arcusCacheManagerFromClient.getCache(PRE_DEFINED_CACHE_NAME);

    assertEquals(PRE_DEFINED_CACHE_NAME, cache.getName());
    assertEquals(SERVICE_ID, cache.getServiceId());
    assertEquals(SERVICE_PREFIX, cache.getPrefix());
    assertEquals(PRE_DEFINED_EXPIRE_SECONDS, cache.getExpireSeconds());
    assertEquals(TIMEOUT_MILLIS, cache.getTimeoutMilliSeconds());
    assertTrue(cache.getOperationTranscoder() instanceof SerializingTranscoder);
    assertEquals(WANT_TO_GET_EXCEPTION, cache.isWantToGetException());
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testGetMissingCache() {
    String nonDefinedCache = "non-defined-cache";
    ArcusCache cache = (ArcusCache)this.arcusCacheManagerFromClient.getCache(nonDefinedCache);

    assertEquals(nonDefinedCache, cache.getName());
    assertEquals(SERVICE_ID, cache.getServiceId());
    assertEquals(SERVICE_PREFIX, cache.getPrefix());
    assertEquals(DEFAULT_EXPIRE_SECONDS, cache.getExpireSeconds());
    assertEquals(TIMEOUT_MILLIS, cache.getTimeoutMilliSeconds());
    assertTrue(cache.getOperationTranscoder() instanceof SerializingTranscoder);
    assertEquals(WANT_TO_GET_EXCEPTION, cache.isWantToGetException());
  }

  @Test
  public void testGetCacheNameAndSize() {
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
  public void testDestroy() {
    ArcusClientPool clientPool;

    Field clientField = ReflectionUtils.findField(ArcusCacheManager.class, "client");
    clientField.setAccessible(true);

    Field deadField = ReflectionUtils.findField(ArcusClient.class, "dead");
    deadField.setAccessible(true);

    this.arcusCacheManagerFromClient.destroy();
    clientPool = (ArcusClientPool) ReflectionUtils.getField(clientField, this.arcusCacheManagerFromClient);
    for (ArcusClient client : clientPool.getAllClients()) {
      assertFalse((Boolean) ReflectionUtils.getField(deadField, client));
    }

    this.arcusCacheManagerFromAddress.destroy();
    clientPool = (ArcusClientPool) ReflectionUtils.getField(clientField, this.arcusCacheManagerFromAddress);
    for (ArcusClient client : clientPool.getAllClients()) {
      assertTrue((Boolean) ReflectionUtils.getField(deadField, client));
    }
  }

}
