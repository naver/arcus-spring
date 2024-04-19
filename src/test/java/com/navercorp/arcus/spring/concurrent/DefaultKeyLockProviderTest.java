/*
 * arcus-spring - Arcus as a caching provider for the Spring Cache Abstraction
 * Copyright 2019 JaM2in Co., Ltd.
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

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultKeyLockProviderTest {

  private int count = 0;

  @Test
  public void testArrayIndexOufOfBoundsExceptionNotThrown() {
    Set<Integer> hashCodeSet = new HashSet<Integer>();

    int exponentOfLocks = DefaultKeyLockProvider.DEFAULT_EXPONENT_OF_LOCKS + 1;
    int numberOfLocks = (int) Math.pow(2, exponentOfLocks);

    DefaultKeyLockProvider provider = new DefaultKeyLockProvider(exponentOfLocks);

    hashCodeSet.add(provider.getLockForKey(null).hashCode());
    
    for (int i = 0; i < numberOfLocks * 2; i++) {
      hashCodeSet.add(provider.getLockForKey(new TestObject(i)).hashCode());
    }

    assertEquals(hashCodeSet.size(), numberOfLocks);
  }

  @Test
  public void testConcurrency() throws InterruptedException {
    final DefaultKeyLockProvider provider = new DefaultKeyLockProvider();
    final TestObject key = new TestObject(0);
    final int maxCount = 10000;
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        for (int i = 0; i < maxCount; i++) {
          provider.getLockForKey(key).writeLock().lock();
          count++;
          provider.getLockForKey(key).writeLock().unlock();
        }
      }
    };

    Thread[] threads = new Thread[] { new Thread(runnable), new Thread(runnable), new Thread(runnable) };
    for (Thread thread : threads) {
      thread.start();
    }
    for (Thread thread : threads) {
      thread.join();
    }

    assertEquals(count, maxCount * threads.length);
  }

  private static class TestObject {

    private final int i;

    private TestObject(int i) {
      this.i = i;
    }

    @Override
    public int hashCode() {
      return i;
    }

  }

}
