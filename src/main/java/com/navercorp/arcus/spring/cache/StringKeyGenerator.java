/*
 * arcus-spring - Arcus as a caching provider for the Spring Cache Abstraction
 * Copyright 2011-2014 NAVER Corp.
 * Copyright 2014-2021 JaM2in Co., Ltd.
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

package com.navercorp.arcus.spring.cache;

import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;

/**
 * 스프링 Cache의 KeyGenerator 구현체.
 * <p>
 * Arcus의 key 구조는 prefix:subkey 입니다.
 * 여기서 생성하는 키 값은 Arcus key 구조에서 subkey에 해당합니다.
 * </p>
 * <p>
 * 기본적으로 메서드 매개변수를 조합해서 키 값을 생성합니다.
 * </p>
 */
public class StringKeyGenerator implements KeyGenerator {
  private static final String DEFAULT_SEPARTOR = ",";

  @Override
  public Object generate(Object target, Method method, Object... params) {
    int hash = 0;
    StringBuilder keyBuilder = new StringBuilder();
    for (int i = 0, n = params.length; i < n; i++) {
      if (i > 0) {
        keyBuilder.append(DEFAULT_SEPARTOR);
      }
      if (params[i] != null) {
        keyBuilder.append(params[i]);
        hash ^= ArcusStringKey.light_hash(params[i].toString());
      }
    }

    return new ArcusStringKey(keyBuilder.toString().replace(' ', '_') + hash);
  }
}
