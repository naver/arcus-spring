package com.navercorp.arcus.spring.concurrent;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings("WeakerAccess")
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
    if (key == null) {
      return 0;
    }
    return key.hashCode() & (mutexes.length - 1);
  }

}
