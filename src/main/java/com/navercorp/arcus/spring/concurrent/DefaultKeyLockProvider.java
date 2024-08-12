/*
 * arcus-spring - Arcus as a caching provider for the Spring Cache Abstraction
 * Copyright 2019-2021 JaM2in Co., Ltd.
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

package com.navercorp.arcus.spring.concurrent;

import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DefaultKeyLockProvider implements KeyLockProvider {

  public static final int DEFAULT_EXPONENT_OF_LOCKS = 11;

  private final ReadWriteLock[] mutexes;

  public DefaultKeyLockProvider() {
    this(DEFAULT_EXPONENT_OF_LOCKS);
  }

  public DefaultKeyLockProvider(int exponentOfLocks) {
    if (exponentOfLocks < 0) {
      exponentOfLocks = DEFAULT_EXPONENT_OF_LOCKS;
    }

    int numberOfLocks = (int) Math.pow(2, exponentOfLocks);

    mutexes = new ReadWriteLock[numberOfLocks];
    for (int i = 0; i < numberOfLocks; i++) {
      mutexes[i] = new ReentrantReadWriteLock();
    }
  }

  @Override
  public ReadWriteLock getLockForKey(Object key) {
    return mutexes[selectLock(key)];
  }

  private int selectLock(Object key) {
    return Objects.hashCode(key) & (mutexes.length - 1);
  }

}
