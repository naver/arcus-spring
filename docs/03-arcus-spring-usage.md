# ARCUS Spring 사용법

예제를 통해 ARCUS Spring 기본 사용법을 알아본다.

## 의존성 설치

### Maven (pom.xml)

```xml
<dependencies>
  <dependency>
    <groupId>com.jam2in.arcus</groupId>
    <artifactId>arcus-spring</artifactId>
    <version>1.13.4</version>
  </dependency>
</dependencies>
```

### Gradle (build.gradle)
#### version 7.0 before
```groovy
dependencies {
  compile 'com.jam2in.arcus:arcus-spring:1.13.4'
}
```
#### version 7.0 or later
```groovy
dependencies {
  implementation 'com.jam2in.arcus:arcus-spring:1.13.4'
}
```

## Bean 설정

Spring Cache Abstraction을 통해 ARCUS Cache를 사용하려면, 다음과 같이 ArcusCacheManager 객체와 ArcusCacheConfiguration 객체를 생성하여 CacheManager Bean을 등록한다.

### XML
```xml
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
                        <property name="timeoutMilliSeconds" value="800"/>
                    </bean>
                </entry>
                <entry key="devCache">
                    <bean parent="defaultCacheConfig">
                        <property name="serviceId" value="DEV-"/>
                        <property name="prefix" value="PRODUCT"/>
                        <property name="expireSeconds" value="120"/>
                        <property name="timeoutMilliSeconds" value="800"/>
                    </bean>
                </entry>
            </map>
        </constructor-arg>
    </bean>

    <bean id="defaultCacheConfig" class="com.navercorp.arcus.spring.cache.ArcusCacheConfiguration">
        <property name="serviceId" value=""/>
        <property name="expireSeconds" value="240"/>
        <property name="timeoutMilliSeconds" value="800"/>
    </bean>

</beans>
```

### Java
```java
@Configuration
@EnableCaching
public class ArcusConfiguration extends CachingConfigurerSupport {
  // Not need to extend CachingConfigurerSupport since Spring 6.0
  // because CachingConfigurerSupport is deprecated since Spring 6.0
  // Just register the beans below since Spring 6.0
  
  private static String ADMIN_ADDRESS = "127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183";
  private static String SERVICE_CODE = "test";
  private static int POOL_SIZE = 8;

  @Bean
  public KeyGenerator keyGenerator() {
    return new StringKeyGenerator();
  }

  @Bean
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
    defaultCacheConfig.setServiceId("");
    defaultCacheConfig.setExpireSeconds(240);
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

## 캐싱

캐싱 기능을 사용하려면 아래와 같이 `@Cacheable` 어노테이션을 사용한다.

### @Cacheable

#### testCache

```java
@Cacheable(cacheNames = "testCache", key="#id")
public Product getProduct_TestCache(int id) {
  return new Product(id);
}
```

- 캐시 이름이 `testCache`인 캐시 설정에 따라 60초의 Expire Time을 갖는 캐시 아이템을 `TEST-PRODUCT` Prefix로 저장

#### devCache

```java
@Cacheable(cacheNames = "devCache", key="#id")
public Product getProduct_DevCache(int id) {
  return new Product(id);
}
```

- 캐시 이름이 `devCache`인 캐시 설정에 따라 120초의 Expire Time을 갖는 캐시 아이템을 `DEV-PRODUCT` Prefix로 저장

#### missingCache

```java
@Cacheable(cacheNames = "missingCache", key="#id")
public Product getProduct_DefaultCache(int id) {
  return new Product(id);
}
```

- 캐시 이름이 `missingCache`인 캐시 설정은 존재하지 않으므로, defaultCacheConfig()에서 생성한 캐시 설정에 따라 240초의 Expire Time을 갖는 캐시 아이템을 `missingCache` Prefix로 저장

### CacheManager

일반적으로 `@Cacheable` 어노테이션을 사용하지만, CacheManager Bean을 주입 받아 직접 사용할 수 있다.
CacheManager Bean을 직접 사용하는 예시는 다음과 같다.

#### testCache

```java
@Autowired
private CacheManager cacheManager;

@Autowired
private KeyGenerator keyGenerator;

public Product getProduct_TestCache(int id) {
  Cache testCache = cacheManager.getCache("testCache");
  Object key = keyGenerator.generate(null, null, id);
  Product product = testCache.get(key, Product.class);

  if (product == null) {
    product = new Product(id);
    testCache.put(key, product);
  }
  
  return product;
}
```

- 캐시 이름이 `testCache`인 캐시 설정에 따라 60초의 Expire Time을 갖는 캐시 아이템을 `TEST-PRODUCT` Prefix로 저장

## Front Cache

Front Cache 기능을 사용하려면 다음과 같이 ArcusCacheConfiguration 객체에 Front Cache 관련 설정을 추가한다.

Front Cache 기능에 대한 설명은 [2장](02-arcus-spring-concept.md#front-cache)을 참고한다.

```java
@Bean
public ArcusCacheConfiguration testCacheConfig() {
  ArcusCacheConfiguration cacheConfig = new ArcusCacheConfiguration();
  cacheConfig.setServiceId("TEST-");
  cacheConfig.setPrefix("PRODUCT");
  cacheConfig.setExpireSeconds(60);
  cacheConfig.setTimeoutMilliSeconds(800);
  /* front cache configuration */
  cacheConfig.setArcusFrontCache(testArcusFrontCache());
  cacheConfig.setFrontExpireSeconds(120);
  cacheConfig.setForceFrontCaching(false);
  /* front cache configuration */
  return cacheConfig;
}

@Bean
public ArcusCacheConfiguration devCacheConfig() {
  ArcusCacheConfiguration cacheConfig = new ArcusCacheConfiguration();
  cacheConfig.setServiceId("DEV-");
  cacheConfig.setPrefix("PRODUCT");
  cacheConfig.setExpireSeconds(120);
  cacheConfig.setTimeoutMilliSeconds(800);
  /* front cache configuration */
  cacheConfig.setArcusFrontCache(devArcusFrontCache());
  cacheConfig.setFrontExpireSeconds(240);
  cacheConfig.setForceFrontCaching(true);
  /* front cache configuration */
  return cacheConfig;
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
