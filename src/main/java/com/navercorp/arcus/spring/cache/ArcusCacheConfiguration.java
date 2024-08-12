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

package com.navercorp.arcus.spring.cache;

import com.navercorp.arcus.spring.cache.front.ArcusFrontCache;

import net.spy.memcached.transcoders.Transcoder;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

@SuppressWarnings("DeprecatedIsStillUsed")
public class ArcusCacheConfiguration implements InitializingBean {

  private String serviceId = "";
  @Nullable
  private String prefix;
  private int expireSeconds;
  private int frontExpireSeconds;
  private long timeoutMilliSeconds = ArcusCache.DEFAULT_TIMEOUT_MILLISECONDS;
  @Nullable
  private Transcoder<Object> operationTranscoder;
  @Nullable
  private ArcusFrontCache arcusFrontCache;
  @Deprecated
  private boolean wantToGetException = ArcusCache.DEFAULT_WANT_TO_GET_EXCEPTION;
  private boolean forceFrontCaching;
  private boolean allowNullValues = ArcusCache.DEFAULT_ALLOW_NULL_VALUES;

  public ArcusCacheConfiguration() {
    this.serviceId = this.getServiceIdPlaceholder();
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  @Nullable
  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(@Nullable String prefix) {
    this.prefix = prefix;
  }

  public int getExpireSeconds() {
    return expireSeconds;
  }

  public void setExpireSeconds(int expireSeconds) {
    this.expireSeconds = expireSeconds;
  }

  public int getFrontExpireSeconds() {
    return frontExpireSeconds;
  }

  public void setFrontExpireSeconds(int frontExpireSeconds) {
    this.frontExpireSeconds = frontExpireSeconds;
  }

  public long getTimeoutMilliSeconds() {
    return timeoutMilliSeconds;
  }

  public void setTimeoutMilliSeconds(long timeoutMilliSeconds) {
    this.timeoutMilliSeconds = timeoutMilliSeconds;
  }

  @Nullable
  public Transcoder<Object> getOperationTranscoder() {
    return operationTranscoder;
  }

  public void setOperationTranscoder(@Nullable Transcoder<Object> operationTranscoder) {
    this.operationTranscoder = operationTranscoder;
  }

  @Nullable
  public ArcusFrontCache getArcusFrontCache() {
    return arcusFrontCache;
  }

  public void setArcusFrontCache(@Nullable ArcusFrontCache arcusFrontCache) {
    this.arcusFrontCache = arcusFrontCache;
  }

  @Deprecated
  public boolean isWantToGetException() {
    return wantToGetException;
  }

  @Deprecated
  public void setWantToGetException(boolean wantToGetException) {
    this.wantToGetException = wantToGetException;
  }

  public boolean isForceFrontCaching() {
    return forceFrontCaching;
  }

  public void setForceFrontCaching(boolean forceFrontCaching) {
    this.forceFrontCaching = forceFrontCaching;
  }

  public boolean isAllowNullValues() {
    return this.allowNullValues;
  }

  public void setAllowNullValues(boolean allowNullValues) {
    this.allowNullValues = allowNullValues;
  }

  private String getServiceIdPlaceholder() {
    return ArcusCacheManager.getStringFieldPlaceholder("serviceId", this);
  }

  @Override
  public void afterPropertiesSet() {
    Assert.isTrue(!getServiceIdPlaceholder().equals(serviceId), "ServiceId must be set.");
    Assert.isTrue(timeoutMilliSeconds > 0, "TimeoutMilliSeconds must be larger than 0.");
  }

}
