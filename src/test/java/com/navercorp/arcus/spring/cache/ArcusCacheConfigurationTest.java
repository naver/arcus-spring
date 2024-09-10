package com.navercorp.arcus.spring.cache;

import net.spy.memcached.ArcusClient;
import net.spy.memcached.ArcusClientPool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcusCacheConfigurationTest {

  @Test
  void createValidCacheKey() {
    ArcusCacheConfiguration config = new ArcusCacheConfiguration()
            .withServiceId("TEST-")
            .withPrefix("test1");
    ArcusClientPool clientPool = ArcusClient.createArcusClientPool("localhost:2181", "test", 8);
    ArcusCache arcusCache = new ArcusCache("cache1", clientPool, config);

    assertTrue(arcusCache.createArcusKey("key1").startsWith("TEST-test1:key1"));
  }

  @Test
  void failWhenSettingInvalidValueInConfigMethod() {
    ArcusCacheConfiguration config = new ArcusCacheConfiguration();

    assertThrows(IllegalArgumentException.class, () -> config.withServiceId(null));
    assertThrows(IllegalArgumentException.class, () -> config.withPrefix(null));
    assertThrows(IllegalArgumentException.class, () -> config.withExpireSeconds(-30));
    assertThrows(IllegalArgumentException.class, () -> config.withTimeoutMilliSeconds(-1));
    assertThrows(IllegalArgumentException.class, () -> config.withArcusFrontCache(null));
    assertThrows(IllegalArgumentException.class, () -> config.withOperationTranscoder(null));
  }

}
