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

package com.navercorp.arcus.spring.cache.front;

import java.io.Serializable;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.ObjectExistsException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class DefaultArcusFrontCacheTest {

  @AfterEach
  public void after() {
    CacheManager.getInstance().shutdown();
  }

  @Test
  public void testConstruct_NameConflict() {
    new DefaultArcusFrontCache("test", 3L, false, false);
    assertThrows(ObjectExistsException.class, () -> {
      new DefaultArcusFrontCache("test", 3L, false, false);
    });
  }

  @Test
  public void testMultipleCache() {
    // given
    DefaultArcusFrontCache frontCache1 = new DefaultArcusFrontCache("test1", 3L, false, false);
    DefaultArcusFrontCache frontCache2 = new DefaultArcusFrontCache("test2", 3L, false, false);

    // when
    frontCache1.set("1", 1, 60);
    frontCache1.set("2", 2, 60);
    frontCache1.set("3", 3, 60);

    // then
    assertEquals(1, frontCache1.get("1"));
    assertEquals(2, frontCache1.get("2"));
    assertEquals(3, frontCache1.get("3"));
    assertNull(frontCache2.get("1"));
    assertNull(frontCache2.get("2"));
    assertNull(frontCache2.get("3"));
  }

  @Test
  public void testMaxEntries() {
    // given
    DefaultArcusFrontCache frontCache = new DefaultArcusFrontCache("test", 3L, false, false);
    frontCache.set("1", 1, 60);
    frontCache.set("2", 2, 60);
    frontCache.set("3", 3, 60);
    frontCache.get("1");
    frontCache.get("2");

    // when
    frontCache.set("4", 4, 60);

    // then
    assertEquals(1, frontCache.get("1"));
    assertEquals(2, frontCache.get("2"));
    assertNull(frontCache.get("3"));
    assertEquals(4, frontCache.get("4"));
  }

  @Test
  public void testExpires() throws InterruptedException {
    // given
    DefaultArcusFrontCache frontCache = new DefaultArcusFrontCache("test", 3L, false, false);

    // when
    frontCache.set("1", 1, 1);
    Thread.sleep(2000);

    // then
    assertNull(frontCache.get("1"));
  }

  @Test
  public void testCopyOnRead_False() {
    // given
    DefaultArcusFrontCache frontCache = new DefaultArcusFrontCache("test", 3L, false, false);
    frontCache.set("1", new TestObject(1), 60);

    // when
    TestObject object = (TestObject) frontCache.get("1");
    assertNotNull(object);
    object.value = 2;

    // then
    Object value = frontCache.get("1");
    assertNotNull(value);
    assertEquals(object.value, ((TestObject) value).value);
  }

  @Test
  public void testCopyOnRead_True() {
    // given
    DefaultArcusFrontCache frontCache = new DefaultArcusFrontCache("test", 3L, true, false);
    frontCache.set("1", new TestObject(1), 60);

    // when
    TestObject object = (TestObject) frontCache.get("1");
    assertNotNull(object);
    object.value = 2;

    // then
    Object value = frontCache.get("1");
    assertNotNull(value);
    assertNotEquals(object.value, ((TestObject) value).value);
  }

  @Test
  public void testCopyOnWrite_False() {
    // given
    DefaultArcusFrontCache frontCache = new DefaultArcusFrontCache("test", 3L, true, false);
    TestObject object = new TestObject(1);
    frontCache.set("1", object, 60);

    // when
    object.value = 2;

    // then
    Object value = frontCache.get("1");
    assertNotNull(value);
    assertEquals(object.value, ((TestObject) value).value);
  }

  @Test
  public void testCopyOnWrite_True() {
    // given
    DefaultArcusFrontCache frontCache = new DefaultArcusFrontCache("test", 3L, true, true);
    frontCache.set("1", new TestObject(1), 60);

    // when
    TestObject object = (TestObject) frontCache.get("1");
    assertNotNull(object);
    object.value = 2;

    // then
    Object value = frontCache.get("1");
    assertNotNull(value);
    assertNotEquals(object.value, ((TestObject) value).value);
  }

  @Test
  public void testDelete() {
    // given
    DefaultArcusFrontCache frontCache = new DefaultArcusFrontCache("test", 3L, false, false);
    frontCache.set("1", 1, 60);
    frontCache.set("2", 2, 60);

    // when
    frontCache.delete("1");

    // then
    assertNull(frontCache.get("1"));
    assertEquals(2, frontCache.get("2"));
  }

  @Test
  public void testClear() {
    // given
    DefaultArcusFrontCache frontCache = new DefaultArcusFrontCache("test", 3L, false, false);
    frontCache.set("1", 1, 60);
    frontCache.set("2", 2, 60);

    // when
    frontCache.clear();

    // then
    assertNull(frontCache.get("1"));
    assertNull(frontCache.get("2"));
  }

  static class TestObject implements Serializable {
    private static final long serialVersionUID = 7471908963811659119L;

    private int value;

    public TestObject(int value) {
      this.value = value;
    }
  }

}
