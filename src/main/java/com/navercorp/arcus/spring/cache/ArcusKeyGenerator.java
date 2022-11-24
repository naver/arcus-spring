package com.navercorp.arcus.spring.cache;

import org.springframework.cache.interceptor.KeyGenerator;

public abstract class ArcusKeyGenerator implements KeyGenerator {
  protected static final String DEFAULT_SEPARTOR = ",";

  public Object generate(Object... params) {
    return generate(null, null, params);
  }
}
