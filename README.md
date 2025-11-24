## arcus-spring

Arcus as a caching provider for the Spring Cache Abstraction.

arcus-spring provides following functionalities.

- ArcusClientFactoryBean: Lifecycle management such as creating or deleting an ArcusClient object.
- ArcusCacheManager: AbstractCacheManager implementation of Spring Cache for managing ArcusCache.
- ArcusCacheConfiguration: Configuration class representing properties of ArcusCache
- ArcusCache: Spring Cache implementation for Arcus.
- StringKeyGenerator, SimpleStringKeyGenerator: KeyGenerator implementation of Spring Cache for generating ArcusStringKey.
- ArcusStringKey: Arcus subkey class with hash and string key without prefix.
- Spring 4.3 cache abstract support.

## Getting Started

See [Getting Started Guide](docs/getting-started.md) for detailed instructions on how to use arcus-spring.

## Front Cache

You can use the front cache to provide fast responsiveness of cache requests. The front cache takes precedence over ARCUS and performs cache requests. To enable this feature, create an implementation of the `ArcusFrontCache` interface and set it to the `ArcusCacheConfiguration`.

### Configuration

```java
@Bean
public ArcusCacheConfiguration testCacheConfig() {
  return new ArcusCacheConfiguration()
      .withServiceId("TEST-")
      .withPrefix("PRODUCT")
      .withExpireSeconds(60)
      .withTimeoutMilliSeconds(800)
      /* front cache configuration */
      .withArcusFrontCache(testArcusFrontCache())
      .withFrontExpireSeconds(120)
      .enableForcingFrontCache();
      /* front cache configuration */
}

@Bean
public ArcusCacheConfiguration devCacheConfig() {
  return new ArcusCacheConfiguration()
      .withServiceId("DEV-")
      .withPrefix("PRODUCT")
      .withExpireSeconds(120)
      .withTimeoutMilliSeconds(800)
      /* front cache configuration */
      .withArcusFrontCache(devArcusFrontCache())
      .withFrontExpireSeconds(240)
      .enableForcingFrontCache();
      /* front cache configuration */
}

@Bean
public ArcusFrontCache testArcusFrontCache() {
    return new DefaultArcusFrontCache("test" /*name*/, 10000 /*maxEntries*/, false /*copyOnRead*/, false /*copyOnWrite*/);
}

@Bean
public ArcusFrontCache devArcusFrontCache() {
    return new DefaultArcusFrontCache("dev" /*name*/, 20000 /*maxEntries*/, false /*copyOnRead*/, false /*copyOnWrite*/);
}
```

The properties added to the `ArcusCacheConfiguration` class related to Front Cache are as follows.

- `withArcusFrontCache(ArcusFrontCache arcusFrontCache)`
  - Set the ArcusFrontCache object to enable Front Caching.
  - The DefaultArcusFrontCache class provided by Arcus Spring can be used.
  - Front caching is disabled by default and arguments cannot be set to null.
- `withFrontExpireSeconds(int frontExpireSeconds)`
  - Set Front Cache TTL(TimeToLive).
- `enableForcingFrontCache()`, `disableForcingFrontCache()`
  - Set whether to perform Front Cache regardless of success or failure of ARCUS change request(put, delete, clear).
  - It is prone to data consistency issues, so we recommend using it only for data that doesn't change frequently.
  - It is disabled by default.

Front Caching is not always performed. It is performed depending on the attribute of `forceFrontCaching` property and the result of the ARCUS request.

| ArcusCache API | ARCUS Result | forceFrontCaching=false | forceFrontCaching=true |
|-------------|----------------------|-------------------------|------------------------|
| get         | success              | O                       | O                      |
| get         | failure              | X                       | X                      |
| put         | success              | O                       | O                      |
| put         | failure              | X                       | O                      |
| putIfAbsent | success              | O                       | O                      |
| putIfAbsent | failure              | X                       | X                      |
| evict       | success              | O                       | O                      |
| evict       | failure              | O                       | O                      |
| clear       | success              | O                       | O                      |
| clear       | failure              | X                       | X                      |

### DefaultArcusFrontCache

`ArcusFrontCache` consists of a simple interface for Front Cache. You can implement and use the `ArcusFrontCache` interface, or you can use the `DefaultArcusFrontCache` implementation provided by default in the library. Four options are required to use DefaultArcusFrontCache.

- name 
  - Cache name. It must be unique each time an instance is created.
- maxEntries 
  - The maximum number of items that can be stored in the front cache.
- copyOnRead 
  - Whether the Front Cache should copy elements it returns.  
- copyOnWrite
  - Whether the Front Cache should copy elements it gets.
  
## Issues

If you find a bug, please report it via the GitHub issues page.

https://github.com/naver/arcus-spring/issues

## License

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
