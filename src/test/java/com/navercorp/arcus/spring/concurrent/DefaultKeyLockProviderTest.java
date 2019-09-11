package com.navercorp.arcus.spring.concurrent;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

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

  static class TestObject {

    private int i;

    TestObject(int i) {
      this.i = i;
    }

    @Override
    public int hashCode() {
      return i;
    }

  }

}
