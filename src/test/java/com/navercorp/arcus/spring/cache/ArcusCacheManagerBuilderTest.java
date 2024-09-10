package com.navercorp.arcus.spring.cache;

import java.util.Collections;

import net.spy.memcached.ArcusClient;
import net.spy.memcached.ArcusClientPool;

import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArcusCacheManagerBuilderTest {

  private final ArcusClientPool arcusClientPool = ArcusClient.createArcusClientPool("localhost:2181", "test", 4);

  @Test
  void testMissingCacheMadeByDefaultCacheConfig() {
    ArcusCacheConfiguration configuration = new ArcusCacheConfiguration().withServiceId("TEST-");
    ArcusCacheManager cm = ArcusCacheManager.builder(arcusClientPool).cacheDefaults(configuration).build();
    cm.afterPropertiesSet();

    ArcusCache missingCache = (ArcusCache) cm.getMissingCache("new-cache");
    assertNotNull(missingCache);
    assertEquals(configuration.getServiceId(), missingCache.getCacheConfiguration().getServiceId());
    assertEquals(configuration.getPrefix(), missingCache.getCacheConfiguration().getPrefix());
    assertEquals(configuration.getExpireSeconds(), missingCache.getCacheConfiguration().getExpireSeconds());
  }

  @Test
  void testSettingDifferentDefaultCacheConfiguration() {
    ArcusCacheConfiguration withPrefix = new ArcusCacheConfiguration().withPrefix("prefix");
    ArcusCacheConfiguration withoutPrefix = new ArcusCacheConfiguration();

    ArcusCacheManager cm = ArcusCacheManager.builder(arcusClientPool)
            .cacheDefaults(withPrefix)
            .initialCacheNames(Collections.singleton("first-cache"))
            .cacheDefaults(withoutPrefix)
            .initialCacheNames(Collections.singleton("second-cache"))
            .build();

    cm.afterPropertiesSet();

    ArcusCache firstCache = (ArcusCache) cm.getCache("first-cache");
    assertNotNull(firstCache);
    assertEquals(withPrefix.getPrefix(), firstCache.getCacheConfiguration().getPrefix());
    ArcusCache secondCache = (ArcusCache) cm.getCache("second-cache");
    assertNotNull(secondCache);
    assertNull(withoutPrefix.getPrefix());
    assertEquals(withoutPrefix.getPrefix(), secondCache.getCacheConfiguration().getPrefix());
  }

  @Test
  void testTransactionAwareCacheManager() {
    Cache cache = ArcusCacheManager.builder(arcusClientPool)
            .transactionAware()
            .build()
            .getCache("decorated-cache");

    assertInstanceOf(TransactionAwareCacheDecorator.class, cache);
  }

  @Test
  void testArcusClientNull() {
    assertThrows(IllegalStateException.class, () -> ArcusCacheManager.builder(null).build());
  }
}
