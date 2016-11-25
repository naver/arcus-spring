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

/**
 * Created by iceru on 2016. 9. 13..
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/arcus_spring_arcusCache_annotation_test.xml")
public class ArcusCacheWitAnnotationTest {
  @Autowired
  TestService testService;

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

}

interface TestService {
  String cachePopulate(String param1, String param2);

  void cacheEvict(String param1, String param2);
}

class TestServiceImpl implements TestService {
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
