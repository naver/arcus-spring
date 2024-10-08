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

import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("/arcus_spring_arcusCache_test.xml")
class ArcusCacheIntegrationTest {
  private static final String TEST_KEY = "arcus_test_key";
  private static final String TEST_VALUE = "arcus_test_value";

  @Autowired
  private ArcusCache arcusCache;

  @Autowired
  private ArcusCache arcusCacheWithoutAllowingNullValue;

  @AfterEach
  void tearDown() {
    arcusCache.evict(TEST_KEY);
  }

  @Test
  void putAndGet() {
    arcusCache.put(TEST_KEY, TEST_VALUE);
    Cache.ValueWrapper value = arcusCache.get(TEST_KEY);

    assertNotNull(value);
    assertEquals(TEST_VALUE, value.get());
  }

  @Test
  void getAndEvict() {
    assertNull(arcusCache.get(TEST_KEY));

    arcusCache.put(TEST_KEY, TEST_VALUE);
    Cache.ValueWrapper value = arcusCache.get(TEST_KEY);

    assertNotNull(value);
    assertEquals(TEST_VALUE, value.get());

    arcusCache.evict(TEST_KEY);
    assertNull(arcusCache.get(TEST_KEY));
  }

  @Test
  void getWithClassType() {
    assertNull(arcusCache.get(TEST_KEY));

    arcusCache.put(TEST_KEY, TEST_VALUE);
    assertEquals(TEST_VALUE, arcusCache.get(TEST_KEY, (Class<?>) null));
    assertEquals(TEST_VALUE, arcusCache.get(TEST_KEY, String.class));
    assertNull(arcusCache.get(TEST_KEY + TEST_KEY, Integer.class));
  }

  @Test
  void throwExceptionIfGetWithDifferentClassType() {
    assertNull(arcusCache.get(TEST_KEY));

    arcusCache.put(TEST_KEY, TEST_VALUE);
    assertThrows(IllegalStateException.class, () -> arcusCache.get(TEST_KEY, Integer.class));
  }

  @Test
  void getWithValueLoader() {
    Callable<String> valueLoader = () -> TEST_VALUE;

    assertNull(arcusCache.get(TEST_KEY));
    assertEquals(TEST_VALUE, arcusCache.get(TEST_KEY, valueLoader));
  }

  @Test
  void throwExceptionIfGetWithValueLoaderFailed() {
    Callable<String> valueLoader = () -> {
      throw new RuntimeException();
    };

    assertNull(arcusCache.get(TEST_KEY));
    assertThrows(RuntimeException.class, () -> assertEquals(TEST_VALUE, arcusCache.get(TEST_KEY, valueLoader)));
  }

  @Test
  void putIfAbsentAndGet() {
    assertNull(arcusCache.get(TEST_KEY));
    assertNull(arcusCache.putIfAbsent(TEST_KEY, TEST_VALUE));

    Cache.ValueWrapper value = arcusCache.get(TEST_KEY);
    assertNotNull(value);
    assertEquals(TEST_VALUE, value.get());

    value = arcusCache.putIfAbsent(TEST_KEY, TEST_VALUE + TEST_VALUE);
    assertNotNull(value);
    assertEquals(TEST_VALUE, value.get());

    value = arcusCache.get(TEST_KEY);
    assertNotNull(value);
    assertEquals(TEST_VALUE, value.get());
  }

  @Test
  void putExternalizableAndSerializableValue() {
    ExternalizableTestClass externalizable = new ExternalizableTestClass();
    externalizable.setAge(30);

    SerializableTestClass SerializableTestClass = new SerializableTestClass();
    SerializableTestClass.setName("someone");
    externalizable.setSerializable(SerializableTestClass);
    arcusCache.put(TEST_KEY, externalizable);

    Cache.ValueWrapper value = arcusCache.get(TEST_KEY);
    assertNotNull(value);

    ExternalizableTestClass loadedBar = (ExternalizableTestClass) value.get();
    assertNotNull(loadedBar);
    assertEquals(30, loadedBar.getAge());
    assertEquals("someone", loadedBar.getSerializable().getName());
  }

  @Test
  void putTheNullValueIfAllowNullValuesIsTrue() {

    arcusCache.put(TEST_KEY, null);

    Cache.ValueWrapper result = arcusCache.get(TEST_KEY);
    assertNotNull(result);
    assertNull(result.get());
  }

  @Test
  void putTheNullValueIfAllowNullValuesIsFalse() {
    assertThrows(IllegalArgumentException.class, () -> arcusCacheWithoutAllowingNullValue.put(TEST_KEY, null));
  }

  @Test
  void returnNullIfClearSucceed() {
    assertNull(arcusCache.get(TEST_KEY));
    arcusCache.put(TEST_KEY, TEST_VALUE);
    assertNull(arcusCache.get(TEST_KEY + "123"));
    arcusCache.put(TEST_KEY + "123", TEST_VALUE + "123");
    arcusCache.clear();

    assertNull(arcusCache.get(TEST_KEY));
    assertNull(arcusCache.get(TEST_KEY + "123"));
  }

  @Test
  void createDifferentCacheKey() {
    String key1 = arcusCache.createArcusKey("hello arcus");
    String key2 = arcusCache.createArcusKey("hello_arcus");

    assertNotEquals(key1, key2);
  }
}
