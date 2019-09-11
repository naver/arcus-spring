package com.navercorp.arcus.spring.concurrent;

import java.util.concurrent.locks.ReadWriteLock;

public interface KeyLockProvider {

  ReadWriteLock getLockForKey(Object key);

}
