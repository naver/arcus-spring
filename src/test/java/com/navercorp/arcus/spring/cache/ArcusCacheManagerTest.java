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

import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ArcusCacheManagerTest {
  private static final String ADMIN_ADDRESS = "127.0.0.1:2181";
  private static final String SERVICE_CODE = "test";
  private static final String SERVICE_ID = "test-service-id";
  private static final Transcoder<Object> DEFAULT_TRANSCODER = new SerializingTranscoder();
  private static final ConnectionFactoryBuilder CONNECTION_FACTORY_BUILDER = new ConnectionFactoryBuilder();
  private static final int POOL_SIZE = 4;
  private static final int TIMEOUT_MILLIS = 100;
  private static final int DEFAULT_EXPIRE_SECONDS = 1;
  private static final boolean WANT_TO_GET_EXCEPTION = true;
  private static final Map<String, Integer> NAME_TO_EXPIRE_SECONDS = new HashMap<String, Integer>();
  private static final String PRE_DEFINED_CACHE_NAME = "pre-defined-cache";
  private static final int PRE_DEFINED_EXPIRE_SECONDS = 2;
  static {
    NAME_TO_EXPIRE_SECONDS.put(PRE_DEFINED_CACHE_NAME, PRE_DEFINED_EXPIRE_SECONDS);
  }

  private ArcusCacheManager cacheManager;

  @Before
  public void setUp() {
    this.cacheManager = new ArcusCacheManager(
      ADMIN_ADDRESS,
      SERVICE_CODE,
      SERVICE_ID,
      CONNECTION_FACTORY_BUILDER,
      DEFAULT_TRANSCODER,
      POOL_SIZE,
      TIMEOUT_MILLIS,
      DEFAULT_EXPIRE_SECONDS,
      NAME_TO_EXPIRE_SECONDS,
      WANT_TO_GET_EXCEPTION);
  }

  @After
  public void tearDown() {
    cacheManager.destroy();
  }

  @Test
  public void testGetPreDefinedCache() {
    ArcusCache cache = (ArcusCache)this.cacheManager.getCache(PRE_DEFINED_CACHE_NAME);

    assertEquals(PRE_DEFINED_CACHE_NAME, cache.getName());
    assertEquals(PRE_DEFINED_EXPIRE_SECONDS, cache.getExpireSeconds());
    assertEquals(SERVICE_ID, cache.getServiceId());
    assertEquals(TIMEOUT_MILLIS, cache.getTimeoutMilliSeconds());
    assertEquals(DEFAULT_TRANSCODER, cache.getOperationTranscoder());
    assertEquals(WANT_TO_GET_EXCEPTION, cache.isWantToGetException());
  }

  @Test
  public void testGetMissingCache() {
    String nonDefinedCache = "non-defined-cache";
    ArcusCache cache = (ArcusCache)this.cacheManager.getCache(nonDefinedCache);

    assertEquals(nonDefinedCache, cache.getName());
    assertEquals(DEFAULT_EXPIRE_SECONDS, cache.getExpireSeconds());
    assertEquals(SERVICE_ID, cache.getServiceId());
    assertEquals(TIMEOUT_MILLIS, cache.getTimeoutMilliSeconds());
    assertEquals(DEFAULT_TRANSCODER, cache.getOperationTranscoder());
    assertEquals(WANT_TO_GET_EXCEPTION, cache.isWantToGetException());
  }

  @Test
  public void testGetPreDefinedCacheNames() {
    Collection<String> preDefinedCacheNames = this.cacheManager.getCacheNames();

    assertEquals(1, preDefinedCacheNames.size());
    assertTrue(preDefinedCacheNames.contains(PRE_DEFINED_CACHE_NAME));
  }

  @Test
  public void testGetMissingCacheNames() {
    String nonDefinedCache = "non-defined-cache";
    this.cacheManager.getCache(nonDefinedCache); // Create missing cache
    Collection<String> cacheNames = this.cacheManager.getCacheNames();

    assertEquals(2, cacheNames.size());
    assertTrue(cacheNames.contains(PRE_DEFINED_CACHE_NAME));
    assertTrue(cacheNames.contains(nonDefinedCache));
  }
}
