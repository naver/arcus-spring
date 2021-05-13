/*
 * arcus-spring - Arcus as a caching provider for the Spring Cache Abstraction
 * Copyright 2016-2021 JaM2in Co., Ltd.
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

public class ArcusStringKey {
  public static int light_hash(String str) {
    int hash = 7;
    for (int i = 0; i < str.length(); i++) {
      hash = hash * 31 + str.charAt(i);
    }
    return hash;
  }

  private final String stringKey;

  public ArcusStringKey(String key) {
    this.stringKey = key;
  }

  public String getStringKey() {
    return stringKey;
  }
}
