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

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.Callable;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/arcus_spring_arcusCache_test.xml")
public class ArcusCacheIntegrationTest {
  private static final String TEST_KEY = "arcus_test_key";
  private static final String TEST_VALUE = "arcus_test_value";

  @Autowired
  private ArcusCache arcusCache;

  @After
  public void tearDown() {
    arcusCache.evict(TEST_KEY);
  }

  @Test
  public void testPut() {
    arcusCache.put(TEST_KEY, TEST_VALUE);
    assertEquals(TEST_VALUE, arcusCache.get(TEST_KEY).get());
  }

  @Test
  public void testGetAndEvict() {
    assertNull(arcusCache.get(TEST_KEY));
    arcusCache.put(TEST_KEY, TEST_VALUE);
    assertEquals(TEST_VALUE, arcusCache.get(TEST_KEY).get());
    arcusCache.evict(TEST_KEY);
    assertNull(arcusCache.get(TEST_KEY));
  }

  @Test
  public void testGetWithClass() {
    assertNull(arcusCache.get(TEST_KEY));
    arcusCache.put(TEST_KEY, TEST_VALUE);
    assertEquals(TEST_VALUE, arcusCache.get(TEST_KEY, (Class) null));
    assertEquals(TEST_VALUE, arcusCache.get(TEST_KEY, String.class));
    assertNull(arcusCache.get(TEST_KEY + TEST_KEY, Integer.class));
  }

  @Test(expected = IllegalStateException.class)
  public void testGetWithClassExpectedException() {
    assertNull(arcusCache.get(TEST_KEY));
    arcusCache.put(TEST_KEY, TEST_VALUE);
    arcusCache.get(TEST_KEY, Integer.class);
  }

  @Test
  public void testGetWithValueLoader() {
    Callable<String> valueLoader = new Callable<String>() {
      @Override
      public String call() {
        return TEST_VALUE;
      }
    };

    assertNull(arcusCache.get(TEST_KEY));
    assertEquals(TEST_VALUE, arcusCache.get(TEST_KEY, valueLoader));
  }

  @Test(expected = RuntimeException.class)
  public void testGetWithValueLoaderExpectedException() {
    Callable<String> valueLoader = new Callable<String>() {
      @Override
      public String call() {
        throw new RuntimeException();
      }
    };

    assertNull(arcusCache.get(TEST_KEY));
    assertEquals(TEST_VALUE, arcusCache.get(TEST_KEY, valueLoader));
  }

  @Test
  public void testPutIfAbsent() {
    assertNull(arcusCache.get(TEST_KEY));
    assertNull(arcusCache.putIfAbsent(TEST_KEY, TEST_VALUE));
    assertEquals(TEST_VALUE, arcusCache.get(TEST_KEY).get());
    assertEquals(TEST_VALUE, arcusCache.putIfAbsent(TEST_KEY, TEST_VALUE + TEST_VALUE).get());
    assertEquals(TEST_VALUE, arcusCache.get(TEST_KEY).get());
  }

  @Test
  public void testExternalizableAndSerializable() {
    ExternalizableTestClass externalizable = new ExternalizableTestClass();
    externalizable.setAge(30);

    SerializableTestClass SerializableTestClass = new SerializableTestClass();
    SerializableTestClass.setName("someone");
    externalizable.setSerializable(SerializableTestClass);

    arcusCache.put(TEST_KEY, externalizable);
    ExternalizableTestClass loadedBar = (ExternalizableTestClass) arcusCache.get(TEST_KEY).get();
    assertNotNull(loadedBar);
    assertEquals(30, loadedBar.getAge());
    assertEquals("someone", loadedBar.getSerializable().getName());
  }

  @Test
  public void putTheNullValue() {
    arcusCache.put(TEST_KEY, null);

    Object result = arcusCache.get(TEST_KEY);
    assertNull(result);
  }

  @Test
  public void testClear() {
    assertNull(arcusCache.get(TEST_KEY));
    arcusCache.put(TEST_KEY, TEST_VALUE);
    assertNull(arcusCache.get(TEST_KEY + "123"));
    arcusCache.put(TEST_KEY + "123", TEST_VALUE + "123");
    arcusCache.clear();

    assertNull(arcusCache.get(TEST_KEY));
    assertNull(arcusCache.get(TEST_KEY + "123"));
  }

  @Test
  public void testCreateArcusKey() {
    String key1 = arcusCache.createArcusKey("hello arcus");
    String key2 = arcusCache.createArcusKey("hello_arcus");

    assertTrue(!(key1.equals(key2)));
  }
}
