/*
 * arcus-spring - Arcus as a caching provider for the Spring Cache Abstraction
 * Copyright 2020 JaM2in JaM2in Co., Ltd.
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

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import org.springframework.cache.interceptor.KeyGenerator;

public class SimpleStringKeyGenerator implements KeyGenerator {
  private static final String DEFAULT_SEPARATOR = ",";

  @Override
  public Object generate(@Nullable Object target, @Nullable Method method, Object... params) {
    return generateKey(params);
  }

  public static ArcusStringKey generateKey(Object... params) {
    StringBuilder keyBuilder = new StringBuilder();
    for (int i = 0, n = params.length; i < n; i++) {
      if (i > 0) {
        keyBuilder.append(DEFAULT_SEPARATOR);
      }
      if (params[i] != null) {
        keyBuilder.append(params[i]);
      }
    }
    return new ArcusStringKey(keyBuilder.toString());
  }
}
