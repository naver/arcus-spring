# ARCUS Cache 기본 사항

ARCUS는 확장된 Key-Value 데이터 모델을 제공한다.
하나의 Key에 하나의 데이터만을 가지는 Simple Key-Value 유형 외에도
하나의 Key에 여러 데이터를 구조화된 형태로 저장하는 Collection 유형을 제공한다.

ARCUS Cache Server의 Key-Value 모델은 아래의 기본 제약 사항을 가진다.

- 기존 Key-Value 모델의 제약 사항
  - Key의 최대 크기는 4000 길이의 문자열이다.
  - Value의 최대 크기는 1MB이다.
- Collection 제약 사항
  - 하나의 Collection이 가지는 최대 Element 개수는 50,000개이다.
  - Collection Element가 저장하는 Value의 최대 크기는 16KB이다.

아래에서 ARCUS Cache를 이해하는 데 있어 기본 사항들을 기술한다.

- [서비스 코드](01-arcus-cache-basics.md#서비스-코드)
- [ARCUS Admin](01-arcus-cache-basics.md#arcus-admin)
- [Cache Key](01-arcus-cache-basics.md#cache-key)
- [Cache Item](01-arcus-cache-basics.md#cache-item)
- [Expiration, Eviction](01-arcus-cache-basics.md#expiration-eviction)


## 서비스 코드

서비스 코드(Service Code)는 ARCUS에서 Cache Cluster를 구분하는 코드이다.
ARCUS Cache Cluster 서비스를 응용들에게 제공한다는 의미에서 "서비스 코드"라는 용어를 사용하게 되었다.

하나의 응용에서 하나 이상의 ARCUS Cache Cluster를 구축하여 사용할 수 있다.
ARCUS Cache Manager 객체는 하나의 ARCUS 서비스 코드만을 가지며, 하나의 ARCUS Cache Cluster에만 접근할 수 있다.
해당 응용이 둘 이상의 ARCUS Cache Cluster를 접근해야 한다면,
각 ARCUS Cache Cluster의 서비스드 코드를 가지는 ARCUS Cache Manager 객체를 따로 생성하여 사용하여야 한다.

## ARCUS Admin

ARCUS Admin은 ZooKeeper를 이용하여 각 서비스 코드에 해당하는 ARCUS Cache Cluster를 관리한다.
특정 서비스 코드에 대한 Cache Server List를 관리하고,
Cache Server 추가 및 삭제에 대해 Cache Server List를 최신 상태로 유지하며,
서비스 코드에 대한 Cache Server List 정보를 ARCUS Client에게 전달한다.
ARCUS Admin은 Highly Available하여야 하므로,
여러 ZooKeeper 서버들을 하나의 ZeeKeeper Ensemble로 구성하여 사용한다.

## Cache Key

Cache Key는 ARCUS Cache에 저장하는 Cache Item을 유일하게 식별한다. Cache key 형식은 아래와 같다.

```
  Cache Key : [<prefix>:]<subkey>
```

- \<prefix\> - Cache key의 앞에 붙는 Namespace이다.
  - Prefix 단위로 Cache Server에 저장된 Key들을 그룹화하여 Flush하거나 통계 정보를 볼 수 있다.
  - Prefix를 생략할 수 있지만, 가급적 사용하길 권한다.
- Delimiter - Prefix와 Subkey를 구분하는 문자로 Default Delimiter는 콜론(`:`)을 사용한다.
- \<subkey\> - 일반적으로 응용에서 사용하는 Key이다.

Prefix와 Subkey는 아래의 명명 규칙을 가진다.

- Prefix는 영문 대소문자, 숫자, 언더바(_), 하이픈(-), 플러스(+), 점(.) 문자만으로 구성될 수 있으며,
  이 중에 하이픈(-)은 prefix 명의 첫번째 문자로 올 수 없다.
- Subkey는 공백(` `) 문자를 가질 수 없으며, 기본적으로 Alphanumeric 문자를 사용하길 권장한다.

## Cache Item

ARCUS Cache는 Simple Key-Value Item 외에 다양한 Collection Item 유형을 가진다.

- Simple Key-Value Item - 기존 Key-Value Item
- Collection Item
  - List Item - 데이터들의 Linked List를 가지는 Item
  - Set Item - 유일한 데이터들의 집합을 가지는 Item
  - Map Item - \<mkey, value\>쌍으로 구성된 데이터 집합을 가지는 Item
  - B+Tree Item - B+Tree Key 기반으로 정렬된 데이터 집합을 가지는 Item

## Expiration, Eviction

각 Cache Item은 Expiration Time 속성을 가진다.
이 값의 설정으로 자동 Expiration을 지정하거나 Expire되지 않도록 지정할 수 있다.

Expiration Time은 2가지 방식으로 설정할 수 있다.
- Expiration Time > 0
  - 지정된 Expiration Time이 지나면 자동으로 Expire된다.
- Expiration Time = 0
  - 시간이 지나도 Expire되지 않는다.
  - 단, 메모리 부족으로 인한 Eviction은 일어날 수 있다.

ARCUS Cache는 Memory Cache이며, 한정된 메모리 공간을 사용하여 데이터를 Caching한다.
메모리 공간이 모두 사용된 상태에서 새로운 Cache Item 저장 요청이 들어오면,
ARCUS Cache는 "Out of Memory" 오류를 내거나
LRU(Least Recently Used) 기반으로 오랫동안 접근되지 않은 Cache Item을 Evict시켜
Available 메모리 공간을 확보한 후에 새로운 Cache Item을 저장한다.
