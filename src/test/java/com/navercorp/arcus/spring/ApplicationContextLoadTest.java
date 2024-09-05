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

package com.navercorp.arcus.spring;

import com.navercorp.arcus.spring.cache.ArcusCache;

import net.spy.memcached.ArcusClientPool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("/arcus_spring_basic_context_test.xml")
public class ApplicationContextLoadTest {

  @Autowired
  private ApplicationContext context;

  @Test
  public void contextLoaded() {
    assertNotNull(context);

    ArcusClientPool client = context.getBean(ArcusClientPool.class);
    CacheManager cacheManager = context.getBean(CacheManager.class);
    Cache cache = cacheManager.getCache("testCache");

    assertNotNull(client);
    assertNotNull(cacheManager);
    assertInstanceOf(ArcusCache.class, cache);
    assertSame(cache.getNativeCache(), client);
  }

}
