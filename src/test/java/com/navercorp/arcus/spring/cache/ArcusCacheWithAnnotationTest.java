/*
 * arcus-spring - Arcus as a caching provider for the Spring Cache Abstraction
 * Copyright 2016 JaM2in Co., Ltd.
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Random;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/arcus_spring_arcusCache_annotation_test.xml")
public class ArcusCacheWithAnnotationTest {
  @Autowired
  private TestService testService;

  @After
  public void tearDown() {
    testService.cacheEvict("param1", "param2");
  }

  @Test
  public void testArcusCacheWithAnnotation() {
    String response1 = testService.cachePopulate("param1", "param2");
    String response2 = testService.cachePopulate("param1", "param2");

    assertEquals(response2, response1);
  }

  private interface TestService {
    String cachePopulate(String param1, String param2);

    void cacheEvict(String param1, String param2);
  }

  private static class TestServiceImpl implements TestService {
    @Override
    @Cacheable(value = "arcusCache")
    public String cachePopulate(String param1, String param2) {
      return "response " + new Random().nextInt();
    }

    @CacheEvict(value = "arcusCache")
    public void cacheEvict(String param1, String param2) {
      // Do nothing
    }
  }
}
