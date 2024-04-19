/*
 * arcus-spring - Arcus as a caching provider for the Spring Cache Abstraction
 * Copyright 2021 JaM2in Co., Ltd.
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
import com.navercorp.arcus.spring.concurrent.KeyLockProvider;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cache.Cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArcusCacheTest {

  private static final ArcusStringKey ARCUS_STRING_KEY = new ArcusStringKey("KEY");
  private static final Object VALUE = "VALUE";
  private static final int EXPIRE_SECONDS = 100;
  private static final int FRONT_EXPIRE_SECONDS = 50;
  private static final Transcoder<Object> OPERATION_TRANSCODER = new SerializingTranscoder();

  private ArcusCache arcusCache;
  private ArcusClientPool arcusClientPool;
  private ArcusFrontCache arcusFrontCache;
  private String arcusKey;
  private Callable<?> valueLoader;
  private KeyLockProvider keyLockProvider;
  private ReadWriteLock readWriteLock;
  private Lock lock;

  @Before
  public void before() {
    arcusClientPool = mock(ArcusClientPool.class);

    arcusFrontCache = mock(ArcusFrontCache.class);

    arcusCache = new ArcusCache();
    arcusCache.setServiceId("SERVICEID");
    arcusCache.setPrefix("PREFIX");
    arcusCache.setArcusClient(arcusClientPool);

    arcusKey = arcusCache.createArcusKey(ARCUS_STRING_KEY);

    valueLoader = mock(Callable.class);

    keyLockProvider = mock(KeyLockProvider.class);

    readWriteLock = mock(ReadWriteLock.class);

    lock = mock(Lock.class);
  }

  @Test
  public void testGet() {
    // given
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(VALUE));

    // when
    Cache.ValueWrapper value = arcusCache.get(ARCUS_STRING_KEY);

    // then
    verify(arcusClientPool, times(1))
        .asyncGet(arcusKey);
    assertNotNull(value);
    assertEquals(VALUE, value.get());
  }

  @Test
  public void testGet_OperationTranscoder() {
    // given
    arcusCache.setOperationTranscoder(OPERATION_TRANSCODER);
    when(arcusClientPool.asyncGet(arcusKey, OPERATION_TRANSCODER))
        .thenReturn(createGetFuture(VALUE));

    // when
    Cache.ValueWrapper value = arcusCache.get(ARCUS_STRING_KEY);

    // then
    verify(arcusClientPool, times(1))
        .asyncGet(arcusKey, OPERATION_TRANSCODER);
    assertNotNull(value);
    assertEquals(VALUE, value.get());
  }

  @Test(expected = TestException.class)
  @SuppressWarnings("deprecation")
  public void testGet_WantToGetException() {
    // given
    arcusCache.setWantToGetException(true);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenThrow(new TestException());

    // when
    arcusCache.get(ARCUS_STRING_KEY);
  }

  @Test
  public void testGet_Exception() {
    // given
    when(arcusClientPool.asyncGet(arcusKey))
        .thenThrow(new TestException());

    // when
    Cache.ValueWrapper value = arcusCache.get(ARCUS_STRING_KEY);

    // then
    assertNull(value);
  }

  @Test
  public void testGet_FutureException() {
    // given
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFutureException());

    // when
    Cache.ValueWrapper value = arcusCache.get(ARCUS_STRING_KEY);

    // then
    assertNull(value);
  }

  @Test
  public void testGet_FrontCache_CacheHit() {
    // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    arcusCache.setFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
    when(arcusFrontCache.get(arcusKey))
        .thenReturn(VALUE);

    // when
    Cache.ValueWrapper value = arcusCache.get(ARCUS_STRING_KEY);

    // then
    verify(arcusClientPool, never())
        .asyncGet(arcusKey);
    verify(arcusFrontCache, times(1))
        .get(arcusKey);
    verify(arcusFrontCache, never())
        .set(arcusKey, VALUE, FRONT_EXPIRE_SECONDS);
    assertNotNull(value);
    assertEquals(VALUE, value.get());
  }

  @Test
  public void testGet_FrontCache_CacheMiss() {
    // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    arcusCache.setFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(VALUE));
    when(arcusFrontCache.get(arcusKey))
        .thenReturn(null);

    // when
    Cache.ValueWrapper value = arcusCache.get(ARCUS_STRING_KEY);

    // then
    verify(arcusClientPool, times(1))
        .asyncGet(arcusKey);
    verify(arcusFrontCache, times(1))
        .get(arcusKey);
    verify(arcusFrontCache, times(1))
        .set(arcusKey, VALUE, FRONT_EXPIRE_SECONDS);
    assertNotNull(value);
    assertEquals(VALUE, value.get());
  }

  @Test
  public void testGet_FrontCache_Null() {
    // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    arcusCache.setFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null));
    when(arcusFrontCache.get(arcusKey))
        .thenReturn(null);

    // when
    Cache.ValueWrapper value = arcusCache.get(ARCUS_STRING_KEY);

    // then
    verify(arcusClientPool, times(1))
        .asyncGet(arcusKey);
    verify(arcusFrontCache, times(1))
        .get(arcusKey);
    verify(arcusFrontCache, never())
        .set(arcusKey, VALUE, FRONT_EXPIRE_SECONDS);
    assertNull(value);
  }

  @Test
  public void testPut() {
    // given
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));

    // when
    arcusCache.put(ARCUS_STRING_KEY, VALUE);

    // then
    verify(arcusClientPool, times(1))
        .set(arcusKey, EXPIRE_SECONDS, VALUE);
  }

  @Test
  public void testPut_OperationTranscoder() {
    // given
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setOperationTranscoder(OPERATION_TRANSCODER);
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE, OPERATION_TRANSCODER))
        .thenReturn(createOperationFuture(true));

    // when
    arcusCache.put(ARCUS_STRING_KEY, VALUE);

    // then
    verify(arcusClientPool, times(1))
        .set(arcusKey, EXPIRE_SECONDS, VALUE, OPERATION_TRANSCODER);
  }

  @Test
  public void testPut_Null() {
    // given
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, null))
        .thenReturn(createOperationFuture(true));

    // when
    arcusCache.put(ARCUS_STRING_KEY, null);

    // then
    verify(arcusClientPool, never())
        .set(arcusKey, EXPIRE_SECONDS, null);
  }

  @Test(expected = TestException.class)
  @SuppressWarnings("deprecation")
  public void testPut_WantToGetException() {
    // given
    arcusCache.setWantToGetException(true);
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenThrow(new TestException());

    // when
    arcusCache.put(ARCUS_STRING_KEY, VALUE);
  }

  @Test
  public void testPut_FrontCache() {
    // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));

    // when
    arcusCache.put(ARCUS_STRING_KEY, VALUE);

    // then
    verify(arcusClientPool, times(1))
        .set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(arcusFrontCache, times(1))
        .set(arcusKey, VALUE, FRONT_EXPIRE_SECONDS);
  }

  @Test
  public void testPut_FrontCache_Failure() {
    // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(false));

    // when
    arcusCache.put(ARCUS_STRING_KEY, VALUE);

    // then
    verify(arcusClientPool, times(1))
        .set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(arcusFrontCache, never())
        .set(arcusKey, VALUE, FRONT_EXPIRE_SECONDS);
  }

  @Test
  public void testPut_FrontCache_Failure_ForceFrontCaching() {
    // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
    arcusCache.setForceFrontCaching(true);
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(false));

    // when
    arcusCache.put(ARCUS_STRING_KEY, VALUE);

    // then
    verify(arcusClientPool, times(1))
        .set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(arcusFrontCache, times(1))
        .set(arcusKey, VALUE, FRONT_EXPIRE_SECONDS);
  }

  @Test
  public void testPut_FrontCache_Exception() {
    // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenThrow(new TestException());

    // when
    arcusCache.put(ARCUS_STRING_KEY, VALUE);

    // then
    verify(arcusClientPool, times(1))
        .set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(arcusFrontCache, never())
        .set(arcusKey, VALUE, FRONT_EXPIRE_SECONDS);
  }

  @Test
  public void testPut_FrontCache_Exception_ForceFrontCaching() {
    // given
    arcusCache.setForceFrontCaching(true);
    arcusCache.setArcusFrontCache(arcusFrontCache);
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenThrow(new TestException());

    // when
    arcusCache.put(ARCUS_STRING_KEY, VALUE);

    // then
    verify(arcusClientPool, times(1))
        .set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(arcusFrontCache, times(1))
        .set(arcusKey, VALUE, FRONT_EXPIRE_SECONDS);
  }

  @Test
  public void testPut_FrontCache_FutureException() {
    // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFutureException());

    // when
    arcusCache.put(ARCUS_STRING_KEY, VALUE);

    // then
    verify(arcusClientPool, times(1))
        .set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(arcusFrontCache, never())
        .set(arcusKey, VALUE, FRONT_EXPIRE_SECONDS);
  }

  @Test
  public void testPut_FrontCache_FutureException_ForceFrontCaching() {
    // given
    arcusCache.setForceFrontCaching(true);
    arcusCache.setArcusFrontCache(arcusFrontCache);
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFutureException());

    // when
    arcusCache.put(ARCUS_STRING_KEY, VALUE);

    // then
    verify(arcusClientPool, times(1))
        .set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(arcusFrontCache, times(1))
        .set(arcusKey, VALUE, FRONT_EXPIRE_SECONDS);
  }

  @Test
  public void testEvict() {
    // given
    when(arcusClientPool.delete(arcusKey))
        .thenReturn(createOperationFuture(true));

    // when
    arcusCache.evict(ARCUS_STRING_KEY);

    // then
    verify(arcusClientPool, times(1))
        .delete(arcusKey);
  }

  @Test(expected = TestException.class)
  @SuppressWarnings("deprecation")
  public void testEvict_WantToGetException() {
    // given
    arcusCache.setWantToGetException(true);
    when(arcusClientPool.delete(arcusKey))
        .thenThrow(new TestException());

    // when
    arcusCache.evict(ARCUS_STRING_KEY);
  }

  @Test
  public void testEvict_FrontCache() {
    // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    when(arcusClientPool.delete(arcusKey))
        .thenReturn(createOperationFuture(true));

    // when
    arcusCache.evict(ARCUS_STRING_KEY);

    // then
    verify(arcusClientPool, times(1))
        .delete(arcusKey);
    verify(arcusFrontCache, times(1))
        .delete(arcusKey);
  }

  @Test
  public void testEvict_FrontCache_Failure() {
    // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    when(arcusClientPool.delete(arcusKey))
        .thenReturn(createOperationFuture(false));

    // when
    arcusCache.evict(ARCUS_STRING_KEY);

    // then
    verify(arcusClientPool, times(1))
        .delete(arcusKey);
    verify(arcusFrontCache, never())
        .delete(arcusKey);
  }

  @Test
  public void testEvict_FrontCache_Failure_ForceFrontCaching() {
    // given
    arcusCache.setForceFrontCaching(true);
    arcusCache.setArcusFrontCache(arcusFrontCache);
    when(arcusClientPool.delete(arcusKey))
        .thenReturn(createOperationFuture(false));

    // when
    arcusCache.evict(ARCUS_STRING_KEY);

    // then
    verify(arcusClientPool, times(1))
        .delete(arcusKey);
    verify(arcusFrontCache, times(1))
        .delete(arcusKey);
  }

  @Test
  public void testEvict_FrontCache_Exception() {
    // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    when(arcusClientPool.delete(arcusKey))
        .thenThrow(new TestException());

    // when
    arcusCache.evict(ARCUS_STRING_KEY);

    // then
    verify(arcusClientPool, times(1))
        .delete(arcusKey);
    verify(arcusFrontCache, never())
        .delete(arcusKey);
  }

  @Test
  public void testEvict_FrontCache_Exception_ForceFrontCaching() {
    // given
    arcusCache.setForceFrontCaching(true);
    arcusCache.setArcusFrontCache(arcusFrontCache);
    when(arcusClientPool.delete(arcusKey))
        .thenThrow(new TestException());

    // when
    arcusCache.evict(ARCUS_STRING_KEY);

    // then
    verify(arcusClientPool, times(1))
        .delete(arcusKey);
    verify(arcusFrontCache, times(1))
        .delete(arcusKey);
  }

  @Test
  public void testEvict_FrontCache_FutureException() {
    // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    when(arcusClientPool.delete(arcusKey))
        .thenReturn(createOperationFutureException());

    // when
    arcusCache.evict(ARCUS_STRING_KEY);

    // then
    verify(arcusClientPool, times(1))
        .delete(arcusKey);
    verify(arcusFrontCache, never())
        .delete(arcusKey);
  }

  @Test
  public void testEvict_FrontCache_FutureException_ForceFrontCaching() {
    // given
    arcusCache.setForceFrontCaching(true);
    arcusCache.setArcusFrontCache(arcusFrontCache);
    when(arcusClientPool.delete(arcusKey))
        .thenReturn(createOperationFutureException());

    // when
    arcusCache.evict(ARCUS_STRING_KEY);

    // then
    verify(arcusClientPool, times(1))
        .delete(arcusKey);
    verify(arcusFrontCache, times(1))
        .delete(arcusKey);
  }

  @Test
  public void testClear() {
    // given
    when(arcusClientPool.flush(arcusCache.getServiceId() + arcusCache.getPrefix()))
        .thenReturn(createOperationFuture(true));

    // when
    arcusCache.clear();

    // then
    verify(arcusClientPool, times(1))
        .flush(arcusCache.getServiceId() + arcusCache.getPrefix());
  }

  @Test(expected = TestException.class)
  @SuppressWarnings("deprecation")
  public void testClear_WantToGetException() {
    // given
    arcusCache.setWantToGetException(true);
    when(arcusClientPool.flush(arcusCache.getServiceId() + arcusCache.getPrefix()))
        .thenReturn(createOperationFutureException());

    // when
    arcusCache.clear();
  }

  @Test
  public void testClear_FrontCache() {
    // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    when(arcusClientPool.flush(arcusCache.getServiceId() + arcusCache.getPrefix()))
        .thenReturn(createOperationFuture(true));

    // when
    arcusCache.clear();

    // then
    verify(arcusClientPool, times(1))
        .flush(arcusCache.getServiceId() + arcusCache.getPrefix());
    verify(arcusFrontCache, times(1))
        .clear();
  }

  @Test
  public void testClear_FrontCache_Failure() {
    // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    when(arcusClientPool.flush(arcusCache.getServiceId() + arcusCache.getPrefix()))
        .thenReturn(createOperationFuture(false));

    // when
    arcusCache.clear();

    // then
    verify(arcusClientPool, times(1))
        .flush(arcusCache.getServiceId() + arcusCache.getPrefix());
    verify(arcusFrontCache, never())
        .clear();
  }

  @Test
  public void testClear_FrontCache_Failure_ForceFrontCaching() {
    // given
    arcusCache.setForceFrontCaching(true);
    arcusCache.setArcusFrontCache(arcusFrontCache);
    when(arcusClientPool.flush(arcusCache.getServiceId() + arcusCache.getPrefix()))
        .thenReturn(createOperationFuture(false));

    // when
    arcusCache.clear();

    // then
    verify(arcusClientPool, times(1))
        .flush(arcusCache.getServiceId() + arcusCache.getPrefix());
    verify(arcusFrontCache, times(1))
        .clear();
  }

  @Test
  public void testClear_FrontCache_Exception() {
    // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    when(arcusClientPool.flush(arcusCache.getServiceId() + arcusCache.getPrefix()))
        .thenThrow(new TestException());

    // when
    arcusCache.clear();

    // then
    verify(arcusClientPool, times(1))
        .flush(arcusCache.getServiceId() + arcusCache.getPrefix());
    verify(arcusFrontCache, never())
        .clear();
  }

  @Test
  public void testClear_FrontCache_Exception_ForceFrontCaching() {
    // given
    arcusCache.setForceFrontCaching(true);
    arcusCache.setArcusFrontCache(arcusFrontCache);
    when(arcusClientPool.flush(arcusCache.getServiceId() + arcusCache.getPrefix()))
        .thenThrow(new TestException());

    // when
    arcusCache.clear();

    // then
    verify(arcusClientPool, times(1))
        .flush(arcusCache.getServiceId() + arcusCache.getPrefix());
    verify(arcusFrontCache, times(1))
        .clear();
  }

  @Test
  public void testClear_FrontCache_FutureException() {
    // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    when(arcusClientPool.flush(arcusCache.getServiceId() + arcusCache.getPrefix()))
        .thenReturn(createOperationFutureException());

    // when
    arcusCache.clear();

    // then
    verify(arcusClientPool, times(1))
        .flush(arcusCache.getServiceId() + arcusCache.getPrefix());
    verify(arcusFrontCache, never())
        .clear();
  }

  @Test
  public void testClear_FrontCache_FutureException_ForceFrontCaching() {
    // given
    arcusCache.setForceFrontCaching(true);
    arcusCache.setArcusFrontCache(arcusFrontCache);
    when(arcusClientPool.flush(arcusCache.getServiceId() + arcusCache.getPrefix()))
        .thenReturn(createOperationFutureException());

    // when
    arcusCache.clear();

    // then
    verify(arcusClientPool, times(1))
        .flush(arcusCache.getServiceId() + arcusCache.getPrefix());
    verify(arcusFrontCache, times(1))
        .clear();
  }

  @Test
  public void testGetType() {
    // given
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(VALUE));

    // when
    Object value = arcusCache.get(ARCUS_STRING_KEY, String.class);

    // then
    verify(arcusClientPool, times(1))
        .asyncGet(arcusKey);
    assertEquals(VALUE, value);
  }

  @Test(expected = IllegalStateException.class)
  public void testGetType_DifferentType() {
    // given
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(VALUE));

    // when
    arcusCache.get(ARCUS_STRING_KEY, Integer.class);
  }

  @Test(expected = TestException.class)
  public void testGetType_Exception() {
    // given
    when(arcusClientPool.asyncGet(arcusKey))
        .thenThrow(new TestException());

    // when
    arcusCache.get(ARCUS_STRING_KEY, String.class);
  }

  @Test(expected = TestException.class)
  public void testGetType_FutureException() {
    // given
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFutureException());

    // when
    arcusCache.get(ARCUS_STRING_KEY, String.class);
  }

  @Test
  public void testGetValueLoader_Get_CacheHit() throws Exception {
    // given
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(VALUE));
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(valueLoader.call())
        .thenReturn(VALUE);
    when(keyLockProvider.getLockForKey(arcusKey))
        .thenReturn(readWriteLock);
    when(readWriteLock.writeLock())
        .thenReturn(lock);

    // when
    Object value = arcusCache.get(ARCUS_STRING_KEY, valueLoader);

    // then
    verify(arcusClientPool, times(1)).asyncGet(arcusKey);
    verify(arcusClientPool, never()).set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(valueLoader, never()).call();
    verify(lock, never()).lock();
    verify(lock, never()).unlock();
    assertEquals(VALUE, value);
  }

  @Test
  public void testGetValueLoader_Get_SecondCacheHit() throws Exception {
    // given
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null))
        .thenReturn(createGetFuture(VALUE));
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFutureException());
    when(valueLoader.call())
        .thenReturn(VALUE);
    when(keyLockProvider.getLockForKey(arcusKey))
        .thenReturn(readWriteLock);
    when(readWriteLock.writeLock())
        .thenReturn(lock);

    // when
    Object value = arcusCache.get(ARCUS_STRING_KEY, valueLoader);

    // then
    verify(arcusClientPool, times(2)).asyncGet(arcusKey);
    verify(arcusClientPool, never()).set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(valueLoader, never()).call();
    verify(lock, times(1)).lock();
    verify(lock, times(1)).unlock();
    assertEquals(VALUE, value);
  }

  @Test
  public void testGetValueLoader_Get_CacheMiss() throws Exception {
    // given
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null));
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(valueLoader.call())
        .thenReturn(VALUE);
    when(keyLockProvider.getLockForKey(arcusKey))
        .thenReturn(readWriteLock);
    when(readWriteLock.writeLock())
        .thenReturn(lock);

    // when
    Object value = arcusCache.get(ARCUS_STRING_KEY, valueLoader);

    // then
    verify(arcusClientPool, times(2)).asyncGet(arcusKey);
    verify(arcusClientPool, times(1)).set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(valueLoader, times(1)).call();
    verify(lock, times(1)).lock();
    verify(lock, times(1)).unlock();
    assertEquals(VALUE, value);
  }

  @Test
  public void testGetValueLoader_Get_Exception() throws Exception {
    // given
    TestException exception = null;
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenThrow(new TestException());
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(valueLoader.call())
        .thenReturn(VALUE);
    when(keyLockProvider.getLockForKey(arcusKey))
        .thenReturn(readWriteLock);
    when(readWriteLock.writeLock())
        .thenReturn(lock);

    // when
    try {
      arcusCache.get(ARCUS_STRING_KEY, valueLoader);
    } catch (TestException e) {
      exception = e;
    }

    // then
    verify(arcusClientPool, times(1)).asyncGet(arcusKey);
    verify(arcusClientPool, never()).set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(valueLoader, never()).call();
    verify(lock, never()).lock();
    verify(lock, never()).unlock();
    assertNotNull(exception);
  }

  @Test
  public void testGetValueLoader_Get_FutureException() throws Exception {
    // given
    TestException exception = null;
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFutureException());
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(valueLoader.call())
        .thenReturn(VALUE);
    when(keyLockProvider.getLockForKey(arcusKey))
        .thenReturn(readWriteLock);
    when(readWriteLock.writeLock())
        .thenReturn(lock);

    // when
    try {
      arcusCache.get(ARCUS_STRING_KEY, valueLoader);
    } catch (TestException e) {
      exception = e;
    }

    // then
    verify(arcusClientPool, times(1)).asyncGet(arcusKey);
    verify(arcusClientPool, never()).set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(valueLoader, never()).call();
    verify(lock, never()).lock();
    verify(lock, never()).unlock();
    assertNotNull(exception);
  }

  @Test
  public void testGetValueLoader_Get_SecondException() throws Exception {
    // given
    TestException exception = null;
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null))
        .thenThrow(new TestException());
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(valueLoader.call())
        .thenReturn(VALUE);
    when(keyLockProvider.getLockForKey(arcusKey))
        .thenReturn(readWriteLock);
    when(readWriteLock.writeLock())
        .thenReturn(lock);

    // when
    try {
      arcusCache.get(ARCUS_STRING_KEY, valueLoader);
    } catch (TestException e) {
      exception = e;
    }

    // then
    verify(arcusClientPool, times(2)).asyncGet(arcusKey);
    verify(arcusClientPool, never()).set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(valueLoader, never()).call();
    verify(lock, times(1)).lock();
    verify(lock, times(1)).unlock();
    assertNotNull(exception);
  }

  @Test
  public void testGetValueLoader_Get_SecondFutureException() throws Exception {
    // given
    TestException exception = null;
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null))
        .thenReturn(createGetFutureException());
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(valueLoader.call())
        .thenReturn(VALUE);
    when(keyLockProvider.getLockForKey(arcusKey))
        .thenReturn(readWriteLock);
    when(readWriteLock.writeLock())
        .thenReturn(lock);

    // when
    try {
      arcusCache.get(ARCUS_STRING_KEY, valueLoader);
    } catch (TestException e) {
      exception = e;
    }

    // then
    verify(arcusClientPool, times(2)).asyncGet(arcusKey);
    verify(arcusClientPool, never()).set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(valueLoader, never()).call();
    verify(lock, times(1)).lock();
    verify(lock, times(1)).unlock();
    assertNotNull(exception);
  }

  @Test
  public void testGetValueLoader_Put_Exception() throws Exception {
    // given
    TestException exception = null;
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null));
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenThrow(new TestException());
    when(valueLoader.call())
        .thenReturn(VALUE);
    when(keyLockProvider.getLockForKey(arcusKey))
        .thenReturn(readWriteLock);
    when(readWriteLock.writeLock())
        .thenReturn(lock);

    // when
    try {
      arcusCache.get(ARCUS_STRING_KEY, valueLoader);
    } catch (TestException e) {
      exception = e;
    }

    // then
    verify(arcusClientPool, times(2)).asyncGet(arcusKey);
    verify(arcusClientPool, times(1)).set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(valueLoader, times(1)).call();
    verify(lock, times(1)).lock();
    verify(lock, times(1)).unlock();
    assertNotNull(exception);
  }

  @Test
  public void testGetValueLoader_Put_FutureException() throws Exception {
    // given
    TestException exception = null;
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null));
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFutureException());
    when(valueLoader.call())
        .thenReturn(VALUE);
    when(keyLockProvider.getLockForKey(arcusKey))
        .thenReturn(readWriteLock);
    when(readWriteLock.writeLock())
        .thenReturn(lock);

    // when
    try {
      arcusCache.get(ARCUS_STRING_KEY, valueLoader);
    } catch (TestException e) {
      exception = e;
    }

    // then
    verify(arcusClientPool, times(2)).asyncGet(arcusKey);
    verify(arcusClientPool, times(1)).set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(valueLoader, times(1)).call();
    verify(lock, times(1)).lock();
    verify(lock, times(1)).unlock();
    assertNotNull(exception);
  }

   @Test
  public void testGetValueLoader_ValueLoader_Null() throws Exception {
    // given
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null));
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(valueLoader.call())
        .thenReturn(null);
    when(keyLockProvider.getLockForKey(arcusKey))
        .thenReturn(readWriteLock);
    when(readWriteLock.writeLock())
        .thenReturn(lock);

    // when
    Object value = arcusCache.get(ARCUS_STRING_KEY, valueLoader);

    // then
    verify(arcusClientPool, times(2)).asyncGet(arcusKey);
    verify(arcusClientPool, never()).set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(valueLoader, times(1)).call();
    verify(lock, times(1)).lock();
    verify(lock, times(1)).unlock();
    assertNull(value);
  }

  @Test
  public void testGetValueLoader_ValueLoader_Exception() throws Exception {
    // given
    Cache.ValueRetrievalException exception = null;
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null));
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(valueLoader.call())
        .thenThrow(new TestException());
    when(keyLockProvider.getLockForKey(arcusKey))
        .thenReturn(readWriteLock);
    when(readWriteLock.writeLock())
        .thenReturn(lock);

    // when
    try {
      arcusCache.get(ARCUS_STRING_KEY, valueLoader);
    } catch (Cache.ValueRetrievalException e) {
      exception = e;
    }

    // then
    verify(arcusClientPool, times(2)).asyncGet(arcusKey);
    verify(arcusClientPool, never()).set(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(valueLoader, times(1)).call();
    verify(lock, times(1)).lock();
    verify(lock, times(1)).unlock();
    assertNotNull(exception);
  }

  @Test
  public void testPutIfAbsent() {
    // given
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    when(arcusClientPool.add(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(arcusClientPool.get(arcusKey))
        .thenReturn(createGetFuture(VALUE));

    // when
    Object value = arcusCache.putIfAbsent(ARCUS_STRING_KEY, VALUE);

    // then
    verify(arcusClientPool, times(1))
        .add(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(arcusClientPool, never())
        .get(arcusKey);
    assertNull(value);
  }

  @Test
  public void testPutIfAbsent_OperationTranscoder() {
    // given
    arcusCache.setOperationTranscoder(OPERATION_TRANSCODER);
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    when(arcusClientPool.add(arcusKey, EXPIRE_SECONDS, VALUE, OPERATION_TRANSCODER))
        .thenReturn(createOperationFuture(true));
    when(arcusClientPool.get(arcusKey, OPERATION_TRANSCODER))
        .thenReturn(createGetFuture(VALUE));

    // when
    Object value = arcusCache.putIfAbsent(ARCUS_STRING_KEY, VALUE);

    // then
    verify(arcusClientPool, times(1))
        .add(arcusKey, EXPIRE_SECONDS, VALUE, OPERATION_TRANSCODER);
    verify(arcusClientPool, never())
        .get(arcusKey, OPERATION_TRANSCODER);
    assertNull(value);
  }

  @Test
  public void testPutIfAbsent_FrontCache() {
     // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
    when(arcusClientPool.add(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(VALUE));

    // when
    Cache.ValueWrapper value = arcusCache.putIfAbsent(ARCUS_STRING_KEY, VALUE);

    // then
    verify(arcusClientPool, times(1))
        .add(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(arcusClientPool, never())
        .asyncGet(arcusKey);
    verify(arcusFrontCache, times(1))
        .set(arcusKey, VALUE, FRONT_EXPIRE_SECONDS);
    assertNull(value);
  }

  @Test
  public void testPutIfAbsent_FrontCache_Failure() {
     // given
    arcusCache.setArcusFrontCache(arcusFrontCache);
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
    when(arcusClientPool.add(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(false));
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(VALUE.toString() + VALUE.toString()));

    // when
    Cache.ValueWrapper value = arcusCache.putIfAbsent(ARCUS_STRING_KEY, VALUE);

    // then
    verify(arcusClientPool, times(1))
        .add(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(arcusClientPool, times(1))
        .asyncGet(arcusKey);
    verify(arcusFrontCache, times(1))
        .set(arcusKey, VALUE.toString() + VALUE.toString(), FRONT_EXPIRE_SECONDS);
    assertNotNull(value);
    assertEquals(VALUE.toString() + VALUE.toString(), value.get());
  }

  @Test
  public void testPutIfAbsent_FrontCache_Failure_ForceFrontCaching() {
    arcusCache.setForceFrontCaching(true);
    testPutIfAbsent_FrontCache_Failure();
  }

  @Test
  public void testPutIfAbsent_FrontCache_Exception() {
     // given
    TestException exception = null;
    arcusCache.setArcusFrontCache(arcusFrontCache);
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
    when(arcusClientPool.add(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenThrow(new TestException());
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(VALUE));

    // when
    try {
      arcusCache.putIfAbsent(ARCUS_STRING_KEY, VALUE);
    } catch (TestException e) {
      exception = e;
    }

    // then
    verify(arcusClientPool, times(1))
        .add(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(arcusClientPool, never())
        .asyncGet(arcusKey);
    verify(arcusFrontCache, never())
        .set(arcusKey, VALUE, FRONT_EXPIRE_SECONDS);
    assertNotNull(exception);
  }

  @Test
  public void testPutIfAbsent_FrontCache_Exception_ForceFrontCaching() {
    arcusCache.setForceFrontCaching(true);
    testPutIfAbsent_FrontCache_Exception();
  }

  @Test
  public void testPutIfAbsent_FrontCache_FutureException() {
     // given
    TestException exception = null;
    arcusCache.setArcusFrontCache(arcusFrontCache);
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
    when(arcusClientPool.add(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFutureException());
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(VALUE));

    // when
    try {
      arcusCache.putIfAbsent(ARCUS_STRING_KEY, VALUE);
    } catch (TestException e) {
      exception = e;
    }

    // then
    verify(arcusClientPool, times(1))
        .add(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(arcusClientPool, never())
        .asyncGet(arcusKey);
    verify(arcusFrontCache, never())
        .set(arcusKey, VALUE, FRONT_EXPIRE_SECONDS);
    assertNotNull(exception);
  }

  @Test
  public void testPutIfAbsent_FrontCache_FutureException_ForceFrontCaching() {
    arcusCache.setForceFrontCaching(true);
    testPutIfAbsent_FrontCache_FutureException();
  }

  @Test
  public void testPutIfAbsent_FrontCache_Null() {
    // given
    IllegalArgumentException exception = null;
    arcusCache.setArcusFrontCache(arcusFrontCache);
    arcusCache.setExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
    when(arcusClientPool.add(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(VALUE));

    // when
    try {
      arcusCache.putIfAbsent(ARCUS_STRING_KEY, null);
    } catch (IllegalArgumentException e) {
      exception = e;
    }

    // then
    verify(arcusClientPool, never())
        .add(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(arcusClientPool, never())
        .asyncGet(arcusKey);
    verify(arcusFrontCache, never())
        .set(arcusKey, VALUE, FRONT_EXPIRE_SECONDS);
    assertNotNull(exception);
  }

  private static GetFuture<Object> createGetFuture(
      @SuppressWarnings("SameParameterValue") final Object value) {
    return new GetFuture<Object>(null, 0) {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
      }

      @Override
      public boolean isCancelled() {
        return false;
      }

      @Override
      public boolean isDone() {
        return false;
      }

      @Override
      public Object get() {
        return value;
      }

      @Override
      public Object get(long timeout, TimeUnit unit) {
        return value;
      }
    };
  }

  private static GetFuture<Object> createGetFutureException() {
    return new GetFuture<Object>(null, 0) {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
      }

      @Override
      public boolean isCancelled() {
        return false;
      }

      @Override
      public boolean isDone() {
        return false;
      }

      @Override
      public Object get() {
        throw new TestException();
      }

      @Override
      public Object get(long timeout, TimeUnit unit) {
        throw new TestException();
      }
    };
  }

  private static OperationFuture<Boolean> createOperationFuture(
      @SuppressWarnings("SameParameterValue") final Boolean value) {
    return new OperationFuture<Boolean>(null, 0) {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
      }

      @Override
      public boolean isCancelled() {
        return false;
      }

      @Override
      public boolean isDone() {
        return false;
      }

      @Override
      public Boolean get() {
        return value;
      }

      @Override
      public Boolean get(long timeout, TimeUnit unit) {
        return value;
      }
    };
  }

  private static OperationFuture<Boolean> createOperationFutureException() {
    return new OperationFuture<Boolean>(null, 0) {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
      }

      @Override
      public boolean isCancelled() {
        return false;
      }

      @Override
      public boolean isDone() {
        return false;
      }

      @Override
      public Boolean get() {
        throw new TestException();
      }

      @Override
      public Boolean get(long timeout, TimeUnit unit) {
        throw new TestException();
      }
    };
  }

  private static class TestException extends RuntimeException {
    private static final long serialVersionUID = -3103959625477003804L;
  }

}
