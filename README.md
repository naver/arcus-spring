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

### Dependency

The artifact for arcus-spring is in the central Maven repository. To use it, add the following dependency.

#### Maven (pom.xml)

```
<dependencies>
    <dependency>
        <groupId>com.navercorp.arcus</groupId>
        <artifactId>arcus-spring</artifactId>
        <version>1.11.5</version>
    </dependency>
</dependencies>
```

#### Gradle (build.gradle)
```
dependencies {
    compile 'com.navercorp.arcus:arcus-spring:1.11.5'
}
```

### KeyGenerator

Arcus-spring provides two types of key generator. These two are StringKeyGenerator and SimpleStringKeyGenerator.

- StringKeyGenerator: it generate the key by combining the parameters and hashcode of the parameters.
It's because the key can have invalid characters. This generator replace invalid characters to valid characters.
But if the generator do that, even though the keys are different, they can be the same. So the generator adds hashcode to distinguish the keys.
- SimpleStringKeyGenerator: it generate the key simply by combining the parameters. So this generator can generate invalid keys or duplicate keys.

For example, when the parameters are 'a', 'b', 'c', StringKeyGenerator creates the key 'a,b,c317' and SimpleStringKeyGenerator creates the key 'a,b,c'.

### Configuration
Spring Cache configuration is required before using arcus-spring. Create ArcusCacheManager and StringKeyGenerator with the following configuration.

#### XML
```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:cache="http://www.springframework.org/schema/cache"
       xmlns:util="http://www.springframework.org/schema/util"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/cache
                           http://www.springframework.org/schema/cache/spring-cache.xsd
	                       http://www.springframework.org/schema/util
                           http://www.springframework.org/schema/util/spring-util.xsd">

    <cache:annotation-driven
        key-generator="arcusKeyGenerator"
        cache-manager="arcusCacheManager"/>

    <bean id="arcusKeyGenerator"
        class="com.navercorp.arcus.spring.cache.StringKeyGenerator"/>

    <bean id="arcusCacheManager" class="com.navercorp.arcus.spring.cache.ArcusCacheManager">
        <constructor-arg name="adminAddress" value="127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183"/>
        <constructor-arg name="serviceCode" value="test"/>
        <constructor-arg name="connectionFactoryBuilder">
            <bean class="net.spy.memcached.ConnectionFactoryBuilder"/>
        </constructor-arg>
        <constructor-arg name="poolSize" value="8"/>
        <!-- default cache configuration (missing cache) -->
        <constructor-arg name="defaultConfiguration" ref="defaultCacheConfig"/>
        <!-- a map of cache configuration (key=cache name, value=cache configuration) -->
        <constructor-arg name="initialCacheConfigs">
            <map>
                <entry key="testCache">
                    <bean parent="defaultCacheConfig">
                        <property name="serviceId" value="TEST-"/>
                        <property name="prefix" value="PRODUCT"/>
                        <property name="expireSeconds" value="60"/>
                    </bean>
                </entry>
                <entry key="devCache">
                    <bean parent="defaultCacheConfig">
                        <property name="serviceId" value="DEV-">
                        <property name="prefix" value="PRODUCT"/>
                        <property name="expireSeconds" value="120"/>
                    </bean>
                </entry>
            </map>
        </constructor-arg>
    </bean>

    <bean id="defaultCacheConfig" class="com.navercorp.arcus.spring.cache.ArcusCacheConfiguration">
        <property name="prefix" value="DEFAULT"/>
        <property name="expireSeconds" value="60"/>
        <property name="timeoutMilliSeconds" value="800"/>
    </bean>

</beans>
```

#### Java
```
@Configuration
@EnableCaching
public class ArcusConfiguration extends CachingConfigurerSupport {

    private static String ADMIN_ADDRESS = "127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183";
    private static String SERVICE_CODE = "test";
    private static int POOL_SIZE = 8;

    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return new StringKeyGenerator();
    }

    @Bean
    @Override
    public CacheManager cacheManager() {
        return new ArcusCacheManager(
            ADMIN_ADDRESS,
            SERVICE_CODE,
            new ConnectionFactoryBuilder(),
            POOL_SIZE,
            /* default cache configuration (missing cache) */
            defaultCacheConfig(),
            /* a map of cache configuration (key=cache name, value=cache configuration) */
            initialCacheConfig()
        );
    }

    @Bean
    public ArcusCacheConfiguration defaultCacheConfig() {
        ArcusCacheConfiguration defaultCacheConfig = new ArcusCacheConfiguration();
        defaultCacheConfig.setPrefix("DEFAULT");
        defaultCacheConfig.setExpireSeconds(60);
        defaultCacheConfig.setTimeoutMilliSeconds(800);
        return defaultCacheConfig;
    }

    @Bean
    public Map<String, ArcusCacheConfiguration> initialCacheConfig() {
        Map<String, ArcusCacheConfiguration> initialCacheConfig = new HashMap<>();
        initialCacheConfig.put("testCache", testCacheConfig());
        initialCacheConfig.put("devCache", devCacheConfig());
        return initialCacheConfig;
    }

    @Bean
    public ArcusCacheConfiguration testCacheConfig() {
        ArcusCacheConfiguration cacheConfig = new ArcusCacheConfiguration();
        cacheConfig.setServiceId("TEST-");
        cacheConfig.setPrefix("PRODUCT");
        cacheConfig.setExpireSeconds(60);
        cacheConfig.setTimeoutMilliSeconds(800);
        return cacheConfig;
    }

    @Bean
    public ArcusCacheConfiguration devCacheConfig() {
        ArcusCacheConfiguration cacheConfig = new ArcusCacheConfiguration();
        cacheConfig.setServiceId("DEV-");
        cacheConfig.setPrefix("PRODUCT");
        cacheConfig.setExpireSeconds(120);
        cacheConfig.setTimeoutMilliSeconds(800);
        return cacheConfig;
    }

}
```

### Example

Apply the cache using the key(cacheNames) stored in the initialCacheConfig map of ArcusCacheManager you created with XML or Java configuration. 

```
@Service
public class ProductService {

    /*
        using the "testCache" cache with 60 expire seconds and "TEST-PRODUCT" prefix.
    */
    @Cacheable(cacheNames = "testCache", key="#id")
    public Product getProduct_TestCache(int id) {
        return new Product(id);
    }

    /*
        using the "devCache" cache with 120 expire seconds and "DEV-PRODUCT" prefix.
    */
    @Cacheable(cacheNames = "devCache", key="#id")
    public Product getProduct_DevCache(int id) {
        return new Product(id);
    }

    /*
        In ArcusCacheManger, missing cache is loaded with default cache configuration.
        so, the below code uses the default cache with 60 expire seconds and "DEFAULT" prefix.
    */
    @Cacheable(cacheNames = "missingCache", key="#id")
    public Product getProduct_DefaultCache(int id) {
        return new Product(id);
    }

}
```

## Issues

If you find a bug, please report it via the GitHub issues page.

https://github.com/naver/arcus-spring/issues

## License

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
