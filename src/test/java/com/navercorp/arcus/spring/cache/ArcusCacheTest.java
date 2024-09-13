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
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatusCode;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.support.NullValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArcusCacheTest {

  private static final ArcusStringKey ARCUS_STRING_KEY = new ArcusStringKey("KEY");
  private static final String VALUE = "VALUE";
  private static final int EXPIRE_SECONDS = 100;
  private static final int FRONT_EXPIRE_SECONDS = 50;
  private static final Transcoder<Object> OPERATION_TRANSCODER = new SerializingTranscoder();

  private ArcusCache arcusCache;
  private ArcusClientPool arcusClientPool;
  private ArcusFrontCache arcusFrontCache;
  private String arcusKey;
  private Callable<Object> valueLoader;
  private KeyLockProvider keyLockProvider;
  private ReadWriteLock readWriteLock;
  private Lock lock;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void before() {
    arcusClientPool = mock(ArcusClientPool.class);

    arcusFrontCache = mock(ArcusFrontCache.class);

    ArcusCacheConfiguration config = new ArcusCacheConfiguration()
            .withServiceId("SERVICEID")
            .withPrefix("PREFIX");
    arcusCache = new ArcusCache("test", arcusClientPool, config);

    arcusKey = arcusCache.createArcusKey(ARCUS_STRING_KEY);

    valueLoader = mock(Callable.class);

    keyLockProvider = mock(KeyLockProvider.class);

    readWriteLock = mock(ReadWriteLock.class);

    lock = mock(Lock.class);
  }

  @Test
  void get() {
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
  void getWithCustomOperationTranscoder() {
    // given
    arcusCache.getCacheConfiguration().withOperationTranscoder(OPERATION_TRANSCODER);
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

  @Test
  void throwExceptionIfGetWithWantToGetException() {
    // given
    arcusCache.getCacheConfiguration().enableGettingException();
    when(arcusClientPool.asyncGet(arcusKey))
        .thenThrow(new TestException());

    // when
    assertThrows(TestException.class, () -> arcusCache.get(ARCUS_STRING_KEY));
  }

  @Test
  void notThrowExceptionIfWantToGetExceptionIsFalse() {
    // given
    when(arcusClientPool.asyncGet(arcusKey))
        .thenThrow(new TestException());

    // when
    Cache.ValueWrapper value = arcusCache.get(ARCUS_STRING_KEY);

    // then
    assertNull(value);
  }

  @Test
  void notThrowFutureExceptionIfWantToGetExceptionIsFalse() {
    // given
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFutureException());

    // when
    Cache.ValueWrapper value = arcusCache.get(ARCUS_STRING_KEY);

    // then
    assertNull(value);
  }

  @Test
  void alwaysThrowInterruptedException() {
    // given
    GetFuture<Object> future = new GetFuture<Object>(null, 0) {
      @Override
      public Object get(long timeout, TimeUnit unit) throws InterruptedException {
        throw new InterruptedException();
      }
    };

    // when
    when(arcusClientPool.asyncGet(arcusKey))
            .thenReturn(future);
    try {
      arcusCache.get(ARCUS_STRING_KEY);
    } catch (Exception e) {
      // then
      assertEquals(RuntimeException.class, e.getClass());
      assertEquals(InterruptedException.class, e.getCause().getClass());
    }
  }

  @Test
  void getFromFrontCache() {
    // given
    arcusCache.getCacheConfiguration()
            .withArcusFrontCache(arcusFrontCache)
            .withFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
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
  void getFromArcusIfFrontCacheMissed() {
    // given
    arcusCache.getCacheConfiguration()
            .withArcusFrontCache(arcusFrontCache)
            .withFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
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
  void getFromArcusIfFrontCacheReturnNull() {
    // given
    arcusCache.getCacheConfiguration()
            .withArcusFrontCache(arcusFrontCache)
            .withFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
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
  void returnNullIfArcusReturnNullValue() {
    // given
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(NullValue.INSTANCE));

    // when
    Cache.ValueWrapper value = arcusCache.get(ARCUS_STRING_KEY);

    // then
    verify(arcusClientPool, times(1))
        .asyncGet(arcusKey);
    assertNotNull(value);
    assertNull(value.get());
  }

  @Test
  void put() {
    // given
    arcusCache.getCacheConfiguration().withExpireSeconds(EXPIRE_SECONDS);
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));

    // when
    arcusCache.put(ARCUS_STRING_KEY, VALUE);

    // then
    verify(arcusClientPool, times(1))
        .set(arcusKey, EXPIRE_SECONDS, VALUE);
  }

  @Test
  void putWithCustomOperationTranscoder() {
    // given
    arcusCache.getCacheConfiguration()
            .withExpireSeconds(EXPIRE_SECONDS)
            .withOperationTranscoder(OPERATION_TRANSCODER);
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE, OPERATION_TRANSCODER))
        .thenReturn(createOperationFuture(true));

    // when
    arcusCache.put(ARCUS_STRING_KEY, VALUE);

    // then
    verify(arcusClientPool, times(1))
        .set(arcusKey, EXPIRE_SECONDS, VALUE, OPERATION_TRANSCODER);
  }

  @Test
  void putNull() {
    // given
    arcusCache.getCacheConfiguration().withExpireSeconds(EXPIRE_SECONDS);
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, NullValue.INSTANCE))
        .thenReturn(createOperationFuture(true));

    // when
    arcusCache.put(ARCUS_STRING_KEY, null);

    // then
    verify(arcusClientPool, never())
            .set(arcusKey, EXPIRE_SECONDS, null);
    verify(arcusClientPool, atLeastOnce())
        .set(arcusKey, EXPIRE_SECONDS, NullValue.INSTANCE);
  }

  @Test
  void throwExceptionIfPutWithWantToGetException() {
    // given
    arcusCache.getCacheConfiguration()
            .enableGettingException()
            .withExpireSeconds(EXPIRE_SECONDS);
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenThrow(new TestException());

    // when
    assertThrows(TestException.class, () -> {
      arcusCache.put(ARCUS_STRING_KEY, VALUE);
    });
  }

  @Test
  void putWithFrontCache() {
    // given
    arcusCache.getCacheConfiguration().withArcusFrontCache(arcusFrontCache)
            .withExpireSeconds(EXPIRE_SECONDS)
            .withFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
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
  void doNotPutFrontCacheIfArcusFailed() {
    // given
    arcusCache.getCacheConfiguration().withArcusFrontCache(arcusFrontCache)
            .withExpireSeconds(EXPIRE_SECONDS)
            .withFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
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
  void putFrontCacheIfArcusFailedButForceFrontCachingIsTrue() {
    // given
    arcusCache.getCacheConfiguration().withArcusFrontCache(arcusFrontCache)
            .withExpireSeconds(EXPIRE_SECONDS)
            .withFrontExpireSeconds(FRONT_EXPIRE_SECONDS)
            .enableForcingFrontCache();
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
  void doNotPutFrontCacheIfArcusHasException() {
    // given
    arcusCache.getCacheConfiguration().withArcusFrontCache(arcusFrontCache)
            .withExpireSeconds(EXPIRE_SECONDS)
            .withFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
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
  void putFrontCacheIfArcusHasExceptionButForceFrontCachingIsTrue() {
    // given
    arcusCache.getCacheConfiguration().withArcusFrontCache(arcusFrontCache)
            .withExpireSeconds(EXPIRE_SECONDS)
            .withFrontExpireSeconds(FRONT_EXPIRE_SECONDS)
            .enableForcingFrontCache();
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
  void doNotPutFrontCacheIfFutureHasException() {
    // given
    arcusCache.getCacheConfiguration().withArcusFrontCache(arcusFrontCache)
            .withExpireSeconds(EXPIRE_SECONDS)
            .withFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
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
  void putFrontCacheIfFutureHasExceptionButForceFrontCachingIsTrue() {
    // given
    arcusCache.getCacheConfiguration().withArcusFrontCache(arcusFrontCache)
            .withExpireSeconds(EXPIRE_SECONDS)
            .withFrontExpireSeconds(FRONT_EXPIRE_SECONDS)
            .enableForcingFrontCache();
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
  void evict() {
    // given
    when(arcusClientPool.delete(arcusKey))
        .thenReturn(createOperationFuture(true));

    // when
    arcusCache.evict(ARCUS_STRING_KEY);

    // then
    verify(arcusClientPool, times(1))
        .delete(arcusKey);
  }

  @Test
  void throwExceptionIfEvictWithWantToGetException() {
    // given
    arcusCache.getCacheConfiguration().enableGettingException();
    when(arcusClientPool.delete(arcusKey))
        .thenThrow(new TestException());

    // when
    assertThrows(TestException.class, () -> {
      arcusCache.evict(ARCUS_STRING_KEY);
    });
  }

  @Test
  void evictWithFrontCache() {
    // given
    arcusCache.getCacheConfiguration().withArcusFrontCache(arcusFrontCache);
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
  void doNotEvictFrontCacheIfArcusFailed() {
    // given
    arcusCache.getCacheConfiguration().withArcusFrontCache(arcusFrontCache);
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
  void evictFrontCacheIfArcusFailedButForceFrontCachingIsTrue() {
    // given
    arcusCache.getCacheConfiguration()
            .withArcusFrontCache(arcusFrontCache)
            .enableForcingFrontCache();
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
  void doNotEvictFrontCacheIfArcusHasException() {
    // given
    arcusCache.getCacheConfiguration().withArcusFrontCache(arcusFrontCache);
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
  void evictFrontCacheIfArcusHasExceptionButForceFrontCachingIsTrue() {
    // given
    arcusCache.getCacheConfiguration()
            .withArcusFrontCache(arcusFrontCache)
            .enableForcingFrontCache();
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
  void doNotEvictFrontCacheIfFutureHasException() {
    // given
    arcusCache.getCacheConfiguration().withArcusFrontCache(arcusFrontCache);
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
  void evictFrontCacheIfFutureHasExceptionButForceFrontCachingIsTrue() {
    // given
    arcusCache.getCacheConfiguration()
            .withArcusFrontCache(arcusFrontCache)
            .enableForcingFrontCache();
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
  void clear() {
    // given
    String prefix = arcusCache.getCacheConfiguration().getServiceId() + arcusCache.getCacheConfiguration().getPrefix();
    when(arcusClientPool.flush(prefix))
        .thenReturn(createOperationFuture(true));

    // when
    arcusCache.clear();

    // then
    verify(arcusClientPool, times(1))
        .flush(prefix);
  }

  @Test
  void clearWithWantToGetException() {
    // given
    arcusCache.getCacheConfiguration().enableGettingException();
    String prefix = arcusCache.getCacheConfiguration().getServiceId() + arcusCache.getCacheConfiguration().getPrefix();
    when(arcusClientPool.flush(prefix))
        .thenReturn(createOperationFutureException());

    // when
    assertThrows(TestException.class, () -> {
      arcusCache.clear();
    });
  }

  @Test
  void clearWithFrontCache() {
    // given
    arcusCache.getCacheConfiguration().withArcusFrontCache(arcusFrontCache);
    String prefix = arcusCache.getCacheConfiguration().getServiceId() + arcusCache.getCacheConfiguration().getPrefix();
    when(arcusClientPool.flush(prefix))
        .thenReturn(createOperationFuture(true));

    // when
    arcusCache.clear();

    // then
    verify(arcusClientPool, times(1))
        .flush(prefix);
    verify(arcusFrontCache, times(1))
        .clear();
  }

  @Test
  void doNotClearFrontCacheIfArcusFailed() {
    // given
    arcusCache.getCacheConfiguration().withArcusFrontCache(arcusFrontCache);
    String prefix = arcusCache.getCacheConfiguration().getServiceId() + arcusCache.getCacheConfiguration().getPrefix();
    when(arcusClientPool.flush(prefix))
        .thenReturn(createOperationFuture(false));

    // when
    arcusCache.clear();

    // then
    verify(arcusClientPool, times(1))
        .flush(prefix);
    verify(arcusFrontCache, never())
        .clear();
  }

  @Test
  void clearFrontCacheIfArcusFailedButForceFrontCachingIsTrue() {
    // given
    arcusCache.getCacheConfiguration()
            .withArcusFrontCache(arcusFrontCache)
            .enableForcingFrontCache();
    String prefix = arcusCache.getCacheConfiguration().getServiceId() + arcusCache.getCacheConfiguration().getPrefix();
    when(arcusClientPool.flush(prefix))
        .thenReturn(createOperationFuture(false));

    // when
    arcusCache.clear();

    // then
    verify(arcusClientPool, times(1))
        .flush(prefix);
    verify(arcusFrontCache, times(1))
        .clear();
  }

  @Test
  void doNotClearFrontCacheIfArcusHasException() {
    // given
    arcusCache.getCacheConfiguration().withArcusFrontCache(arcusFrontCache);
    String prefix = arcusCache.getCacheConfiguration().getServiceId() + arcusCache.getCacheConfiguration().getPrefix();
    when(arcusClientPool.flush(prefix))
        .thenThrow(new TestException());

    // when
    arcusCache.clear();

    // then
    verify(arcusClientPool, times(1))
        .flush(prefix);
    verify(arcusFrontCache, never())
        .clear();
  }

  @Test
  void clearFrontCacheIfArcusHasExceptionButForceFrontCachingIsTrue() {
    // given
    arcusCache.getCacheConfiguration()
            .withArcusFrontCache(arcusFrontCache)
            .enableForcingFrontCache();
    String prefix = arcusCache.getCacheConfiguration().getServiceId() + arcusCache.getCacheConfiguration().getPrefix();
    when(arcusClientPool.flush(prefix))
        .thenThrow(new TestException());

    // when
    arcusCache.clear();

    // then
    verify(arcusClientPool, times(1))
        .flush(prefix);
    verify(arcusFrontCache, times(1))
        .clear();
  }

  @Test
  void doNotClearFrontCacheIfFutureHasException() {
    // given
    arcusCache.getCacheConfiguration().withArcusFrontCache(arcusFrontCache);
    String prefix = arcusCache.getCacheConfiguration().getServiceId() + arcusCache.getCacheConfiguration().getPrefix();
    when(arcusClientPool.flush(prefix))
        .thenReturn(createOperationFutureException());

    // when
    arcusCache.clear();

    // then
    verify(arcusClientPool, times(1))
        .flush(prefix);
    verify(arcusFrontCache, never())
        .clear();
  }

  @Test
  void clearFrontCacheIfFutureHasExceptionButForceFrontCachingIsTrue() {
    // given
    arcusCache.getCacheConfiguration()
            .withArcusFrontCache(arcusFrontCache)
            .enableForcingFrontCache();
    String prefix = arcusCache.getCacheConfiguration().getServiceId() + arcusCache.getCacheConfiguration().getPrefix();
    when(arcusClientPool.flush(prefix))
        .thenReturn(createOperationFutureException());

    // when
    arcusCache.clear();

    // then
    verify(arcusClientPool, times(1))
        .flush(prefix);
    verify(arcusFrontCache, times(1))
        .clear();
  }

  @Test
  void getWithClassType() {
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

  @Test
  void getWithDifferentClassType() {
    // given
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(VALUE));

    // when
    assertThrows(IllegalStateException.class, () -> {
      arcusCache.get(ARCUS_STRING_KEY, Integer.class);
    });
  }

  @Test
  void throwExceptionIfGetWithClassTypeWithWantToGetException() {
    // given
    arcusCache.getCacheConfiguration().enableGettingException();
    when(arcusClientPool.asyncGet(arcusKey))
        .thenThrow(new TestException());

    // when
    assertThrows(TestException.class, () -> {
      arcusCache.get(ARCUS_STRING_KEY, String.class);
    });
  }

  @Test
  void throwExceptionIfGetWithClassTypeHasFutureExceptionWithWantToGetException() {
    // given
    arcusCache.getCacheConfiguration().enableGettingException();
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFutureException());

    // when
    assertThrows(TestException.class, () -> {
      arcusCache.get(ARCUS_STRING_KEY, String.class);
    });
  }

  @Test
  void getWithoutValueLoaderIfArcusSucceed() throws Exception {
    // given
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(VALUE));
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(valueLoader.call())
        .thenReturn(VALUE);
    when(keyLockProvider.getLockForKey(ARCUS_STRING_KEY))
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
  void getWithoutValueLoaderIfArcusSucceedInSecondTry() throws Exception {
    // given
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null))
        .thenReturn(createGetFuture(VALUE));
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFutureException());
    when(valueLoader.call())
        .thenReturn(VALUE);
    when(keyLockProvider.getLockForKey(ARCUS_STRING_KEY))
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
  void getWithValueLoaderIfArcusFailed() throws Exception {
    // given
    arcusCache.getCacheConfiguration().withExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null));
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(valueLoader.call())
        .thenReturn(VALUE);
    when(keyLockProvider.getLockForKey(ARCUS_STRING_KEY))
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
  void throwExceptionIfGetWithValueLoaderAndArcusHasException() throws Exception {
    // given
    TestException exception = null;
    arcusCache.getCacheConfiguration().enableGettingException();
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
  void throwExceptionIfGetWithValueLoaderAndArcusHasFutureException() throws Exception {
    // given
    TestException exception = null;
    arcusCache.getCacheConfiguration().enableGettingException();
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
  void throwExceptionIfGetWithValueLoaderAndArcusHasExceptionInSecondTry() throws Exception {
    // given
    TestException exception = null;
    arcusCache.getCacheConfiguration().enableGettingException();
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null))
        .thenThrow(new TestException());
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(valueLoader.call())
        .thenReturn(VALUE);
    when(keyLockProvider.getLockForKey(ARCUS_STRING_KEY))
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
  void throwExceptionIfGetWithValueLoaderAndArcusHasFutureExceptionInSecondTry() throws Exception {
    // given
    TestException exception = null;
    arcusCache.getCacheConfiguration().enableGettingException();
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null))
        .thenReturn(createGetFutureException());
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(valueLoader.call())
        .thenReturn(VALUE);
    when(keyLockProvider.getLockForKey(ARCUS_STRING_KEY))
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
  void throwExceptionIfGetWithValueLoaderAndArcusSetHasException() throws Exception {
    // given
    TestException exception = null;
    arcusCache.getCacheConfiguration()
            .withExpireSeconds(EXPIRE_SECONDS)
            .enableGettingException();
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null));
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenThrow(new TestException());
    when(valueLoader.call())
        .thenReturn(VALUE);
    when(keyLockProvider.getLockForKey(ARCUS_STRING_KEY))
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
  void throwExceptionIfGetWithValueLoaderAndArcusSetHasFutureException() throws Exception {
    // given
    TestException exception = null;
    arcusCache.getCacheConfiguration()
            .withExpireSeconds(EXPIRE_SECONDS)
            .enableGettingException();
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null));
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFutureException());
    when(valueLoader.call())
        .thenReturn(VALUE);
    when(keyLockProvider.getLockForKey(ARCUS_STRING_KEY))
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
  void returnNullIfValueLoaderReturnNull() throws Exception {
    // given
    arcusCache.getCacheConfiguration().withExpireSeconds(EXPIRE_SECONDS);
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null));
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(valueLoader.call())
        .thenReturn(null);
    when(keyLockProvider.getLockForKey(ARCUS_STRING_KEY))
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
  void throwValueRetrievalExceptionWhenValueLoaderHasException() throws Exception {
    // given
    Cache.ValueRetrievalException exception = null;
    arcusCache.setKeyLockProvider(keyLockProvider);
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(null));
    when(arcusClientPool.set(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(valueLoader.call())
        .thenThrow(new TestException());
    when(keyLockProvider.getLockForKey(ARCUS_STRING_KEY))
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
  void putIfAbsent() {
    // given
    arcusCache.getCacheConfiguration().withExpireSeconds(EXPIRE_SECONDS);
    when(arcusClientPool.add(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(true));
    when(arcusClientPool.asyncGet(arcusKey))
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
  void putIfAbsentWithCustomOperationTranscoder() {
    // given
    arcusCache.getCacheConfiguration()
            .withExpireSeconds(EXPIRE_SECONDS)
            .withOperationTranscoder(OPERATION_TRANSCODER);
    when(arcusClientPool.add(arcusKey, EXPIRE_SECONDS, VALUE, OPERATION_TRANSCODER))
        .thenReturn(createOperationFuture(true));
    when(arcusClientPool.asyncGet(arcusKey, OPERATION_TRANSCODER))
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
  void putIfAbsentWithFrontCache() {
     // given
    arcusCache.getCacheConfiguration()
            .withArcusFrontCache(arcusFrontCache)
            .withExpireSeconds(EXPIRE_SECONDS)
            .withFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
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
  void returnOriginDataIfArcusFailedInPutIfAbsent() {
     // given
    arcusCache.getCacheConfiguration()
            .withArcusFrontCache(arcusFrontCache)
            .withExpireSeconds(EXPIRE_SECONDS)
            .withFrontExpireSeconds(FRONT_EXPIRE_SECONDS);
    when(arcusClientPool.add(arcusKey, EXPIRE_SECONDS, VALUE))
        .thenReturn(createOperationFuture(false));
    when(arcusClientPool.asyncGet(arcusKey))
        .thenReturn(createGetFuture(VALUE + VALUE));

    // when
    Cache.ValueWrapper value = arcusCache.putIfAbsent(ARCUS_STRING_KEY, VALUE);

    // then
    verify(arcusClientPool, times(1))
        .add(arcusKey, EXPIRE_SECONDS, VALUE);
    verify(arcusClientPool, times(1))
        .asyncGet(arcusKey);
    verify(arcusFrontCache, times(1))
        .set(arcusKey, VALUE + VALUE, FRONT_EXPIRE_SECONDS);
    assertNotNull(value);
    assertEquals(VALUE + VALUE, value.get());
  }

  @Test
  void returnOriginDataIfPutIfAbsentWithFrontCacheAndArcusFailedAndForceFrontCachingIsTrue() {
    arcusCache.getCacheConfiguration().enableForcingFrontCache();
    returnOriginDataIfArcusFailedInPutIfAbsent();
  }

  @Test
  void throwExceptionIfPutIfAbsentHasException() {
     // given
    TestException exception = null;
    arcusCache.getCacheConfiguration()
            .withArcusFrontCache(arcusFrontCache)
            .withExpireSeconds(EXPIRE_SECONDS)
            .withFrontExpireSeconds(FRONT_EXPIRE_SECONDS)
            .enableGettingException();
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
  void throwExceptionIfPutIfAbsentHasExceptionWithForceFrontCaching() {
    arcusCache.getCacheConfiguration().enableForcingFrontCache();
    throwExceptionIfPutIfAbsentHasException();
  }

  @Test
  void throwExceptionIfPutIfAbsentHasFutureException() {
     // given
    TestException exception = null;
    arcusCache.getCacheConfiguration()
            .withArcusFrontCache(arcusFrontCache)
            .withExpireSeconds(EXPIRE_SECONDS)
            .withFrontExpireSeconds(FRONT_EXPIRE_SECONDS)
            .enableGettingException();
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
  void throwExceptionIfPutIfAbsentHasFutureExceptionWithForceFrontCaching() {
    arcusCache.getCacheConfiguration().enableForcingFrontCache();
    throwExceptionIfPutIfAbsentHasFutureException();
  }

  @Test
  void putNullIfAbsent() {
    // given
    Exception exception = null;
    arcusCache.getCacheConfiguration()
            .withArcusFrontCache(arcusFrontCache)
            .withExpireSeconds(EXPIRE_SECONDS)
            .withFrontExpireSeconds(FRONT_EXPIRE_SECONDS)
            .enableGettingException();
    when(arcusClientPool.add(arcusKey, EXPIRE_SECONDS, NullValue.INSTANCE))
            .thenReturn(createOperationFuture(true));

    // when
    try {
      arcusCache.putIfAbsent(ARCUS_STRING_KEY, null);
    } catch (Exception e) {
      exception = e;
    }

    // then
    verify(arcusClientPool, times(1))
            .add(arcusKey, EXPIRE_SECONDS, NullValue.INSTANCE);
    verify(arcusFrontCache, times(1))
            .set(arcusKey, NullValue.INSTANCE, FRONT_EXPIRE_SECONDS);
    verify(arcusClientPool, never())
            .asyncGet(arcusKey);
    assertNull(exception);
  }

  private static GetFuture<Object> createGetFuture(final Object value) {
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

      @Override
      public OperationStatus getStatus() {
        return new OperationStatus(true, "END", StatusCode.SUCCESS);
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

      @Override
      public OperationStatus getStatus() {
        return new OperationStatus(false, "UNDEFINED", StatusCode.UNDEFINED);
      }
    };
  }

  private static OperationFuture<Boolean> createOperationFuture(final Boolean value) {
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

      @Override
      public OperationStatus getStatus() {
        if (value) {
          return new OperationStatus(true, "OK", StatusCode.SUCCESS);
        }
        return new OperationStatus(false, "UNDEFINED", StatusCode.UNDEFINED);
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

      @Override
      public OperationStatus getStatus() {
        return new OperationStatus(false, "UNDEFINED", StatusCode.UNDEFINED);
      }
    };
  }

  private static class TestException extends RuntimeException {
    private static final long serialVersionUID = -3103959625477003804L;
  }

}
