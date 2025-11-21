# ARCUS Spring 사용 시 주의사항

## spring-devtools 사용 시 버그

ARCUS Spring와 spring-devtools를 활용해 Spring 앱을 개발하다 보면 다음과 같은 에러 메세지가 출력될 수 있다.

```
Cached value is not of required type [com.example.Product]: com.example.Product@3c212a06
```

저장된 캐시 아이템의 타입과 변환하려는 타입이 서로 다르다는 뜻이다.
이 현상은 다음과 같이 재현할 수 있다.

1. 사용자 정의 클래스 타입의 캐시 아이템 저장
2. spring-devtools에 의해 사용자 정의 클래스 리로드
3. 캐시 아이템 조회

spring-devtools를 통해 Spring 앱을 개발할 때, 처음 앱 구동 시에는 `BaseClassLoader`에 의해 클래스가 로드된다.
이후 spring-devtools에 의해 클래스가 리로드 될 때는 `RestartClassLoader`에 의해 클래스가 로드된다.

즉, 저장된 캐시 아이템의 타입을 로드한 클래스 로더와 변환하고자 하는 타입을 로드한 클래스 로더가 달라질 수 있다.

C++로 구현된 JDK에서 두 클래스가 동일한지 여부는 두 클래스 객체의 주소 값을 비교하여 판별하는데, 클래스 로더가 달라졌기 때문에 실제로는 동일한 클래스여도 주소 값이 달라져 다른 클래스로 인식하게 된다. 
이에 따라 저장된 캐시 아이템의 타입과 변환하려는 타입이 서로 다르다고 인식하게 되어 오류가 발생하는 것이다.

## 캐시 키에 hash 값을 붙이지 않으려면 SimpleStringKeyGenerator 사용

ArcusCache 클래스의 캐시 키 생성 로직 중 일부는 다음과 같다.

```java
if (key instanceof ArcusStringKey) {
  keyString = ((ArcusStringKey) key).getStringKey();
} else if (key instanceof Integer) {
  keyString = key.toString();
} else {
  keyString = key.toString();
  int hash = ArcusStringKey.light_hash(keyString);
  keyString = keyString.replace(' ', '_') + hash;
}
```

다음과 같이 캐시 아이템을 조회할 때, 캐시 키에 hash 값이 항상 붙게 된다.

```java
long id = 1;
Cache arcusCache = cacheManager.getCache("devCache");

Product product = arcusCache.get(id, Product.class);
```

이는 캐시 키로 넘겨준 인자가 ArcusStringKey 타입도 아니고, Integer 타입도 아니기 때문이다.
이러한 현상을 피하기 위해서는 SimpleStringKeyGenerator를 사용해야 한다.

```java
long id = 1;
KeyGenerator keyGenerator = new SimpleStringKeyGenerator();
Object key = keyGenerator.generate(null, null, id);
Cache arcusCache = cacheManager.getCache("devCache");

Product product = arcusCache.get(key, Product.class);
```

단, SimpleStringKeyGenerator를 통해 생성된 캐시 키는 공백(` `) 문자가 들어갈 수 있으며, 이 경우 ARCUS Cache 연산이 정상적으로 동작하지 않을 수 있다.
이 경우 ArcusStringKey 객체를 반환하는 KeyGenerator 구현체를 새로 만들어 사용하면 된다.

```java
KeyGenerator customKeyGenerator = new KeyGenerator() {
  private static final String DEFAULT_SEPARTOR = ",";

  @Override
  public Object generate(Object target, Method method, Object... params) {
    StringBuilder keyBuilder = new StringBuilder();
    for (int i = 0, n = params.length; i < n; i++) {
      if (i > 0) {
        keyBuilder.append(DEFAULT_SEPARTOR);
      }
      if (params[i] != null) {
        keyBuilder.append(params[i]);
      }
    }
    return new ArcusStringKey(keyBuilder.toString().replace(' ', '_'));
  }
};
```
