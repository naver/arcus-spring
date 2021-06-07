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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

public class DefaultArcusFrontCache extends EhArcusFrontCache {

  public DefaultArcusFrontCache(String name,
                                long maxEntries,
                                boolean copyOnRead,
                                boolean copyOnWrite) {
    super(newCache(name, maxEntries, copyOnRead, copyOnWrite));
  }

  private static Cache newCache(String name,
                                long maxEntries,
                                boolean copyOnRead,
                                boolean copyOnWrite) {
    CacheConfiguration configuration = new CacheConfiguration();
    configuration.setName(name);
    configuration.setMaxEntriesLocalHeap(maxEntries);
    configuration.setCopyOnRead(copyOnRead);
    configuration.setCopyOnWrite(copyOnWrite);
    configuration.memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU);

    Cache cache = new Cache(configuration, null, null);
    CacheManager.getInstance().addCache(cache);

    return cache;
  }

}
