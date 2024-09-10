# ARCUS Spring

ARCUS Spring은 Spring Cache Abstraction 인터페이스를 통해 ARCUS Cache를 사용할 수 있는 기능을 제공하는 라이브러리이다.
단, ARCUS Spring의 구현에서 Collection 유형의 캐싱 기능은 제공하지 않고 Simple Key-Value 유형의 캐싱 기능을 제공한다.

## ARCUS Spring 구현

ARCUS Spring에서 제공하는 Spring Cache Abstraction 구현체에 대해 설명한다.

### Spring Framework Version

ARCUS Spring은 [Spring Context 4.3.0.RELEASE](https://mvnrepository.com/artifact/org.springframework/spring-context/4.3.0.RELEASE)을 기준으로 개발되었다.

### ArcusCacheManager

ARCUS Cache를 사용하기 위해 Spring Cache Abstraction의 AbstractCacheManager 클래스를 구현한다.

ARCUS Spring은 내부적으로 ARCUS Client를 설정하기 위해 Admin Address, Service Code, Pool Size, ConnectionFactoryBuilder가 필요하다.

#### 생성자

```java
public ArcusCacheManager(
  String adminAddress,
  String serviceCode,
  ConnectionFactoryBuilder connectionFactoryBuilder,
  int poolSize,
  ArcusCacheConfiguration defaultConfiguration,
  Map<String, ArcusCacheConfiguration> initialCacheConfigs
)
```

- `String adminAddress`
  - [1장에서 설명했던 ARCUS Admin](01-arcus-cache-basics.md#arcus-admin)의 ZooKeeper 주소이다.
- `String serviceCode`
  - [1장에서 설명했던 서비스 코드](01-arcus-cache-basics.md#서비스-코드)이다.
- `ConnectionFactoryBuilder connectionFactoryBuilder`
  - ARCUS Client 객체를 생성할 때 내부적으로 사용되는 객체이다.
- `int poolSize`
  - ARCUS Client는 여러 개의 Connection을 맺어두는 Client Pool 개념을 제공하는데, 이 Client Pool의 크기를 지정한다.
- `ArcusCacheConfiguration defaultConfiguration`
  - initialCacheConfigs에 포함되지 않는 캐시 이름으로 ArcusCache 객체를 찾으려고 할 때, defaultConfiguration에 해당하는 캐시 설정으로 ArcusCache 객체를 새로 만들어 사용한다.
- `Map<String, ArcusCacheConfiguration>  initialCacheConfigs`
  - Key는 캐시 이름, Value는 ArcusCache 객체를 생성할 때 사용하는 캐시 설정 객체로 이루어진 Map 객체이다.
  - 생성되는 ArcusCache 객체는 Key를 캐시 이름으로 사용하며, 그 외의 설정은 Value의 설정을 따른다.

### ArcusCache

ARCUS Client를 통해 캐시 연산을 수행하는 객체로, Spring Cache Abstraction의 Cache 인터페이스를 구현한다.

사용자가 직접 ArcusCache 객체를 생성하는 것이 아니라, ArcusCacheManager에 의해 생성되는 ArcusCache 객체를 사용하게 된다.

ArcusCache 객체는 String 타입의 캐시 이름과 ArcusCacheConfiguration 타입의 캐시 설정을 통해 생성된다.

### ArcusCacheConfiguration

ArcusCache 객체를 생성하기 위해 사용되는 캐시 설정 클래스이다.

ArcusCacheConfiguration 객체를 생성하고 아래 메소드를 통해 속성을 설정한 뒤, ArcusCacheManager 객체를 생성할 때 생성자의 인자로 넘겨주면 된다.
모든 속성을 기본값으로 사용하고 싶다면 인자가 없는 생성자로 생성한 객체를 사용하면 된다.

- `withServiceId(String serviceId)`
  - 다음에 설명할 prefix와 조합하여 캐시 키의 Prefix를 생성할 때 사용되는 serviceId를 지정한다.
  - 인자로 null을 입력할 수 없으며, 보통 배포 단계(test, dev, stage, prod, ...)를 구분하기 위한 문자열에 `-`를 붙여 지정한다. ex) "DEV-"
  - [1장의 Cache Key 설명](01-arcus-cache-basics.md#cache-key)을 참고하여 ARCUS Cache의 캐시 키로 사용할 수 없는 문자열이 포함되지 않도록 해야 한다.
- `withPrefix(String prefix)`
  - serviceId와 조합하여 캐시 키의 Prefix를 생성하는 데 사용된다.
  - 주로 ARCUS Cache에 저장할 객체의 종류(Product, User, Order, ...)를 나타내는 문자열을 지정한다.
  - 이 메서드로 prefix를 지정하지 않을 경우 캐시 키의 Prefix를 생성할 때 serviceId와 현재 캐시 설정 객체를 담은 ArcusCache 객체의 캐시 이름이 사용된다. 
  - 인자로 null을 입력할 수 없으며, [1장의 Cache Key 설명](01-arcus-cache-basics.md#cache-key)을 참고하여 ARCUS Cache의 캐시 키로 사용할 수 없는 문자열이 포함되지 않도록 해야 한다.
- `withExpireSeconds(int expireSeconds)`
  - 캐시 아이템의 Expire Time을 Seconds 단위로 지정한다.
  - 이 메서드로 expireSeconds를 지정하지 않을 경우 0으로 설정된다.
  - 인자로 양수, 0, -1을 제외한 값은 넣을 수 없다.
  - [1장의 Expiration Time 설명](01-arcus-cache-basics.md#expiration-eviction)을 참고하여 지정해야 한다.
- `withTimeoutMilliSeconds(long timeoutMilliSeconds)`
  - ARCUS Client의 비동기 연산에서 사용할 Timeout을 Milliseconds 단위로 설정한다.
  - 이 메서드로 timeoutMilliSeconds를 지정하지 않을 경우 700ms로 설정된다.
  - 인자로 0보다 큰 값만 넣을 수 있다.
- `withOperationTranscoder(Transcoder<Object> operationTranscoder)`
  - ARCUS Client의 연산 결과를 Serialize, Deserialize하는 데 사용되는 Transcoder 객체를 지정한다.
  - 이 메서드로 operationTranscoder를 지정하지 않을 경우 Arcus Client의 기본 Transcoder가 사용된다.
  - 인자로 null을 입력할 수 없다.
- `enableGettingException()` / `disableGettingException()`
  - ARCUS Client의 연산에서 발생하는 예외를 받을지 여부를 설정한다.
  - 기본적으로 disable 상태이며 예외가 발생하면 원본 메서드를 수행하도록 한다. 
  - enable시킬 경우 예외가 발생하면 그대로 반환하므로 직접 상황에 맞게 예외를 처리해주어야 한다.
- `enableCachingNullValues()` / `disableCachingNullValues()`
  - 캐시 아이템의 값으로 null을 허용할지 여부를 설정한다.
  - 기본적으로 enable 상태이며 null 값을 NullValue 객체로 변환하여 캐시에 저장한다.
  - disable 시킬 경우 null 값을 저장하려 하면 예외가 발생한다.

### KeyGenerator

KeyGenerator는 Spring Cache Abstraction에서 캐시 키를 생성할 때 사용되는 객체로, Bean으로 설정할 수 있다. KeyGenerator Bean을 지정하지 않으면 [SimpleKeyGenerator](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/cache/interceptor/SimpleKeyGenerator.html)가 Default KeyGenerator 객체로 지정된다.

ARCUS Spring에서 제공하는 KeyGenerator 구현체는 2가지가 있다.

- SimpleStringKeyGenerator
  - 캐시 대상 메소드가 호출될 때의 매개변수를 조합하여 문자열을 생성한다.
    - 매개변수가 `a`, `b`, `c`일 때 생성되는 문자열은 `a,b,c`이다.
    - 매개변수가 `a,b,c`일 때 생성되는 문자열 또한 `a,b,c`이다.
    - 매개변수가 `a b c`일 때 생성되는 문자열은 `a b c`이다.
  - 아래 이슈를 가진 구현체이다.
    - ARCUS Cache의 키로 사용할 수 없는 공백(` `) 문자가 포함된 문자열을 생성할 수 있다.
    - 매개변수가 달라도(`a`, `b`, `c` 경우와 `a,b,c` 경우), 같은 문자열(`a,b,c`)을 생성한다.
  - 과거에 유일하게 제공하던 구현체로, 하위 호환성을 위해 남겨둔 상태이다.
- StringKeyGenerator
  - SimpleStringKeyGenerator의 문자열 생성 로직에 더해, 다음과 같은 로직을 추가적으로 수행한다.
    - 캐시 키로 사용할 수 없는 공백(` `) 문자를 언더(`_`) 문자로 대체한다.
    - 캐시 대상 메소드가 호출될 때의 매개변수의 해시 값을 맨 뒤에 붙인다.
    - 매개변수가 `a`, `b`, `c`일 때 생성되는 문자열은 `a,b,c317`이다.
    - 매개변수가 `a,b,c`일 때 생성되는 문자열은 `a,b,c291392039`이다.
    - 매개변수가 `a b c`일 때 생성되는 문자열은 `a_b_c291034175`이다.  
  - 생성되는 문자열에 공백(` `) 문자가 없고 해시 값이 붙어 매개변수 조합마다 유일한 문자열을 생성하므로 일반적으로 ARCUS Cache의 캐시 키로 사용하기에 가장 적합한 구현체이다.

### 캐시 키

ARCUS Cache의 캐시 키는 공백(` `) 문자를 허용하지 않고 초기에는 최대 길이가 250인 제약을 가지고 있었다. 이러한 제약을 준수하기 위해 ARCUS Spring에서는 캐시 키를 생성할 때 다음과 같은 처리를 하고 있다.
- ARCUS Spring에서 제공하는 KeyGenerator가 아닌 다른 KeyGenerator 구현체를 사용할 경우, 캐시 키가 예상과 다르게 생성될 수 있다.
  - KeyGenerator로 생성된 문자열에 공백(` `) 문자가 있다면 언더바(`_`) 문자로 변환하며, 변환하기 전의 문자열의 해시 값을 변환한 후의 문자열 뒤에 붙인다.
  - 해시 값을 붙이는 이유는 공백(` `) 문자를 언더바(`_`) 문자로 변한한 경우와 원래 언더바(`_`) 문자가 포함되어 있던 경우를 구분할 수 있는 캐시 키를 생성하기 위함이다.
    - `ARCUS Cache` vs. `ARCUS_Cache`
- 캐시 키의 전체 길이가 250자를 넘으면, KeyGenerator로 생성한 문자열을 MD5 Hash 값을 취하여 16진수 문자열로 만들고, 이 16진수 문자열을 캐시 키를 생성하는 데 사용한다.
  - 이는 ARCUS Spring에서 제공하는 KeyGenerator를 사용하거나 다른 KeyGenerator를 사용하는 경우에도 모두 적용된다.

최종적으로 캐시 키는 ArcusCacheConfiguration 객체에서 설정된 캐시 키의 Prefix와 KeyGenerator에서 생성된 문자열을 콜론(`:`) 문자로 연결하여 생성한다.
- `<serviceId><prefix | cacheName>:<stringOfKeyGenerator>`

### Front Cache

로컬 캐시를 병용하는 Front Cache 기능을 사용할 수 있다.
기본 제공되는 ArcusFrontCache 구현체는 Ehcache를 활용하여, 캐시 아이템을 조회할 때 로컬 캐시를 먼저 조회한다.

로컬 캐시를 활용하는 Front Cache 기능을 사용할 경우, 로컬 캐시 특성 상 데이터의 비일관성이 생길 수 있으므로 주의해야 한다.

Front Cache 기능을 사용하려면 ArcusCacheConfiguration 객체에서 다음의 메서드들을 사용해 설정해야 한다.

- `withArcusFrontCache(ArcusFrontCache arcusFrontCache)`
  - ArcusFrontCache 구현체를 설정한다.
  - 기본 제공되는 DefaultArcusFrontCache를 사용할 수 있다.
  - 기본적으로 비활성화 되어있는 상태이며, 인자로 null 값은 입력할 수 없다.
- `withFrontExpireSeconds(int frontExpireSeconds)`
  - Front Cache의 TTL(TimeToLive)을 설정한다.
- `enableForcingFrontCache()`, `disableForcingFrontCache()`
  - ARCUS 변경 요청(put, delete, clear)의 성공, 실패에 상관 없이 Front Cache를 수행하는지에 대한 여부를 설정한다.
  - 데이터 일관성 문제가 발생하기 쉬우므로 자주 변경되지 않는 데이터에만 사용하는 것이 좋다.
  - 기본적으로 disable 상태이다.

Front Cache를 설정해도, 항상 Front Cache에 저장하지는 않는다. `forceFrontCaching` 여부에 따라 다음과 같이 Front Cache에 저장한다.

| ArcusCache API | ARCUS Cache Result | forceFrontCaching=false | forceFrontCaching=true |
|----------------|--------------------|-------------------------|------------------------|
| get            | Success            | O                       | O                      |
| get            | Failure            | X                       | X                      |
| put            | Success            | O                       | O                      |
| put            | Failure            | X                       | O                      |
| putIfAbsent    | Success            | O                       | O                      |
| putIfAbsent    | Failure            | X                       | X                      |
| evict          | Success            | O                       | O                      |
| evict          | Failure            | O                       | O                      |
| clear          | Success            | O                       | O                      |
| clear          | Failure            | X                       | X                      |

#### DefaultArcusFrontCache

`ArcusFrontCache`는 Front Cache 기능을 사용할 수 있는 인터페이스이다.

사용자가 `ArcusFrontCache`를 구현하는 클래스를 직접 정의할 수 있지만, ARCUS Spring에서는 Ehcache를 활용하는 `DefaultArcusFrontCache` 클래스를 기본으로 제공한다.

##### 생성자

```java
public DefaultArcusFrontCache(
  String name,
  long maxEntries,
  boolean copyOnRead,
  boolean copyOnWrite
)
```

- `String name`
  - 캐시를 유일하게 식별할 수 있는 이름으로, 각 `DefaultArcusFrontCache` 객체마다 서로 다른 값을 가져야 한다.
  - ArcusCache의 캐시 이름과는 중복되어도 된다.
- `long maxEntries`
  - Front Cache에 저장할 수 있는 캐시 아이템의 최대 개수를 설정한다.
- `boolean copyOnRead`
  - Front Cache에서 조회할 때 복사본을 반환할지 여부를 결정한다.
  - false인 경우 캐시 아이템의 원본 참조를 반환하므로, 해당 참조의 필드를 수정하면 Front Cache에 저장된 캐시 아이템의 필드도 변경된다.
- `boolean copyOnWrite`
  - Front Cache에 저장할 때 복사본을 저장할지 여부를 결정한다.
  - false인 경우 캐시 아이템의 원본 참조를 저장하므로, 저장한 뒤 해당 참조의 필드를 수정하면 Front Cache에 저장된 캐시 아이템의 필드도 변경된다.
