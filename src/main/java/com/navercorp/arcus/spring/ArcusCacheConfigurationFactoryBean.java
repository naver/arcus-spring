package com.navercorp.arcus.spring;

import com.navercorp.arcus.spring.cache.ArcusCacheConfiguration;
import com.navercorp.arcus.spring.cache.front.ArcusFrontCache;

import net.spy.memcached.transcoders.Transcoder;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;

import static com.navercorp.arcus.spring.cache.ArcusCacheConfiguration.DEFAULT_ALLOW_NULL_VALUES;
import static com.navercorp.arcus.spring.cache.ArcusCacheConfiguration.DEFAULT_TIMEOUT_MILLISECONDS;
import static com.navercorp.arcus.spring.cache.ArcusCacheConfiguration.DEFAULT_WANT_TO_GET_EXCEPTION;

/**
 * FactoryBean for creating an {@link ArcusCacheConfiguration} bean in xml configuration.
 */
public class ArcusCacheConfigurationFactoryBean implements FactoryBean<ArcusCacheConfiguration> {

  private String serviceId = "";
  @Nullable
  private String prefix;
  private int expireSeconds;
  private long timeoutMilliSeconds = DEFAULT_TIMEOUT_MILLISECONDS;
  @Nullable
  private Transcoder<Object> operationTranscoder;
  @Nullable
  private ArcusFrontCache arcusFrontCache;
  private int frontExpireSeconds = 5;
  private boolean forceFrontCaching;
  private boolean wantToGetException = DEFAULT_WANT_TO_GET_EXCEPTION;
  private boolean allowNullValues = DEFAULT_ALLOW_NULL_VALUES;

  @Override
  public ArcusCacheConfiguration getObject() {
    ArcusCacheConfiguration arcusCacheConfiguration = new ArcusCacheConfiguration();
    arcusCacheConfiguration.withServiceId(serviceId)
            .withExpireSeconds(expireSeconds)
            .withTimeoutMilliSeconds(timeoutMilliSeconds);
    if (prefix != null) {
      arcusCacheConfiguration.withPrefix(prefix);
    }
    if (operationTranscoder != null) {
      arcusCacheConfiguration.withOperationTranscoder(operationTranscoder);
    }
    if (arcusFrontCache != null) {
      arcusCacheConfiguration.withArcusFrontCache(arcusFrontCache)
              .withFrontExpireSeconds(frontExpireSeconds);
      if (forceFrontCaching) {
        arcusCacheConfiguration.enableForcingFrontCache();
      }
    }
    if (wantToGetException) {
      arcusCacheConfiguration.enableGettingException();
    }
    if (!allowNullValues) {
      arcusCacheConfiguration.disableCachingNullValues();
    }

    return arcusCacheConfiguration;
  }

  @Override
  public Class<?> getObjectType() {
    return ArcusCacheConfiguration.class;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public void setPrefix(@Nullable String prefix) {
    this.prefix = prefix;
  }

  public void setExpireSeconds(int expireSeconds) {
    this.expireSeconds = expireSeconds;
  }

  public void setTimeoutMilliSeconds(long timeoutMilliSeconds) {
    this.timeoutMilliSeconds = timeoutMilliSeconds;
  }

  public void setOperationTranscoder(@Nullable Transcoder<Object> operationTranscoder) {
    this.operationTranscoder = operationTranscoder;
  }

  public void setArcusFrontCache(@Nullable ArcusFrontCache arcusFrontCache) {
    this.arcusFrontCache = arcusFrontCache;
  }

  public void setFrontExpireSeconds(int frontExpireSeconds) {
    this.frontExpireSeconds = frontExpireSeconds;
  }

  public void setForceFrontCaching(boolean forceFrontCaching) {
    this.forceFrontCaching = forceFrontCaching;
  }

  public void setWantToGetException(boolean wantToGetException) {
    this.wantToGetException = wantToGetException;
  }

  public void setAllowNullValues(boolean allowNullValues) {
    this.allowNullValues = allowNullValues;
  }
}
