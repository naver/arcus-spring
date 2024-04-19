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

import javax.annotation.Nullable;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

public class EhArcusFrontCache implements ArcusFrontCache {

  private final Cache cache;

  public EhArcusFrontCache(String name) {
    this(name, CacheManager.getInstance());
  }

  public EhArcusFrontCache(String name, CacheManager cacheManager) {
    Cache cache = cacheManager.getCache(name);

    if (cache == null) {
      cacheManager.addCache(name);
      cache = cacheManager.getCache(name);
    }

    this.cache = cache;
  }

  public EhArcusFrontCache(Cache cache) {
    this.cache = cache;
  }

  @Nullable
  @Override
  public Object get(String key) {
    Element element = cache.get(key);
    return element != null ? element.getObjectValue() : null;
  }

  @Override
  public void set(String key, @Nullable Object value, int expireTime) {
    Element element = new Element(key, value);
    element.setTimeToLive(expireTime);
    element.setTimeToIdle(0);
    cache.put(element);
  }

  @Override
  public void delete(String key) {
    cache.remove(key);
  }

  @Override
  public void clear() {
    cache.removeAll();
  }

}
