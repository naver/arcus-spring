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

import org.junit.Test;

import org.springframework.cache.interceptor.KeyGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class KeyGeneratorTest {
  private final StringKeyGenerator stringKeyGenerator = new StringKeyGenerator();
  private final SimpleStringKeyGenerator simpleStringKeyGenerator = new SimpleStringKeyGenerator();

  private void testGenExtract(KeyGenerator keyGenerator) throws Exception {
    StringBuilder longParam = new StringBuilder();
    for (int i = 0; i < 255; i++) {
      longParam.append(i);
    }
    String key = ((ArcusStringKey) (keyGenerator.generate(null, null, longParam))).getStringKey();
    assertTrue(key.length() > 255);
    System.out.println(key);
  }

  private void testGenDuplicatedKeysWithColons(KeyGenerator keyGenerator, boolean allowDupKey) {
    ArcusStringKey arcusKey1 = (ArcusStringKey) keyGenerator.generate(null, null, "a,b", "c", "de");
    ArcusStringKey arcusKey2 = (ArcusStringKey) keyGenerator.generate(null, null, "a,b", "c,de");

    if (allowDupKey) {
      assertEquals(arcusKey1.getStringKey(), arcusKey2.getStringKey());
    } else {
      assertNotEquals(arcusKey1.getStringKey(), arcusKey2.getStringKey());
    }
  }

  @Test
  public void testExtract() throws Exception {
    testGenExtract(stringKeyGenerator);
    testGenExtract(simpleStringKeyGenerator);
  }

  @Test
  public void testDuplicatedKeysWithColons() {
    testGenDuplicatedKeysWithColons(stringKeyGenerator, false);
    testGenDuplicatedKeysWithColons(simpleStringKeyGenerator, true);
  }
}