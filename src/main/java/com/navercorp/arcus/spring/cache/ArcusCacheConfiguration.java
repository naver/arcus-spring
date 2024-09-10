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

import javax.annotation.Nullable;

import net.spy.memcached.transcoders.Transcoder;

import org.springframework.util.Assert;

@SuppressWarnings("DeprecatedIsStillUsed")
public class ArcusCacheConfiguration {

  static final long DEFAULT_TIMEOUT_MILLISECONDS = 700L;
  @Deprecated
  static final boolean DEFAULT_WANT_TO_GET_EXCEPTION = false;
  static final boolean DEFAULT_ALLOW_NULL_VALUES = true;

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
  @Deprecated
  private boolean wantToGetException = DEFAULT_WANT_TO_GET_EXCEPTION;
  private boolean allowNullValues = DEFAULT_ALLOW_NULL_VALUES;

  public ArcusCacheConfiguration() {
  }

  public ArcusCacheConfiguration withServiceId(String serviceId) {
    Assert.notNull(serviceId, "ServiceId must not be null.");
    this.serviceId = serviceId;
    return this;
  }

  public ArcusCacheConfiguration withPrefix(String prefix) {
    Assert.notNull(prefix, "Prefix must not be null.");
    this.prefix = prefix;
    return this;
  }

  public ArcusCacheConfiguration withExpireSeconds(int expireSeconds) {
    Assert.isTrue(expireSeconds > -2, "ExpireSeconds must be positive integer, 0, or -1.");
    this.expireSeconds = expireSeconds;
    return this;
  }

  public ArcusCacheConfiguration withTimeoutMilliSeconds(long timeoutMilliSeconds) {
    Assert.isTrue(timeoutMilliSeconds > 0, "TimeoutMilliSeconds must be larger than 0.");
    this.timeoutMilliSeconds = timeoutMilliSeconds;
    return this;
  }

  public ArcusCacheConfiguration withOperationTranscoder(Transcoder<Object> operationTranscoder) {
    Assert.notNull(operationTranscoder, "OperationTranscoder must not be null.");
    this.operationTranscoder = operationTranscoder;
    return this;
  }

  public ArcusCacheConfiguration withArcusFrontCache(ArcusFrontCache arcusFrontCache) {
    Assert.notNull(arcusFrontCache, "ArcusFrontCache must not be null.");
    this.arcusFrontCache = arcusFrontCache;
    return this;
  }

  public ArcusCacheConfiguration withFrontExpireSeconds(int frontExpireSeconds) {
    Assert.isTrue(frontExpireSeconds > -1, "FrontExpireSeconds must not be negative integer.");
    this.frontExpireSeconds = frontExpireSeconds;
    return this;
  }

  public ArcusCacheConfiguration enableForcingFrontCache() {
    this.forceFrontCaching = true;
    return this;
  }

  public ArcusCacheConfiguration disableForcingFrontCache() {
    this.forceFrontCaching = false;
    return this;
  }

  @Deprecated
  public ArcusCacheConfiguration enableGettingException() {
    this.wantToGetException = true;
    return this;
  }

  @Deprecated
  public ArcusCacheConfiguration disableGettingException() {
    this.wantToGetException = false;
    return this;
  }

  public ArcusCacheConfiguration enableCachingNullValues() {
    this.allowNullValues = true;
    return this;
  }

  public ArcusCacheConfiguration disableCachingNullValues() {
    this.allowNullValues = false;
    return this;
  }

  public String getServiceId() {
    return serviceId;
  }

  @Deprecated
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  @Nullable
  public String getPrefix() {
    return prefix;
  }

  @Deprecated
  public void setPrefix(@Nullable String prefix) {
    this.prefix = prefix;
  }

  public int getExpireSeconds() {
    return expireSeconds;
  }

  @Deprecated
  public void setExpireSeconds(int expireSeconds) {
    this.expireSeconds = expireSeconds;
  }

  public int getFrontExpireSeconds() {
    return frontExpireSeconds;
  }

  @Deprecated
  public void setFrontExpireSeconds(int frontExpireSeconds) {
    this.frontExpireSeconds = frontExpireSeconds;
  }

  public long getTimeoutMilliSeconds() {
    return timeoutMilliSeconds;
  }

  @Deprecated
  public void setTimeoutMilliSeconds(long timeoutMilliSeconds) {
    Assert.isTrue(timeoutMilliSeconds > 0, "TimeoutMilliSeconds must be larger than 0.");
    this.timeoutMilliSeconds = timeoutMilliSeconds;
  }

  @Nullable
  public Transcoder<Object> getOperationTranscoder() {
    return operationTranscoder;
  }

  @Deprecated
  public void setOperationTranscoder(@Nullable Transcoder<Object> operationTranscoder) {
    this.operationTranscoder = operationTranscoder;
  }

  @Nullable
  public ArcusFrontCache getArcusFrontCache() {
    return arcusFrontCache;
  }

  @Deprecated
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

  @Deprecated
  public void setForceFrontCaching(boolean forceFrontCaching) {
    this.forceFrontCaching = forceFrontCaching;
  }

  public boolean isAllowNullValues() {
    return this.allowNullValues;
  }

  @Deprecated
  public void setAllowNullValues(boolean allowNullValues) {
    this.allowNullValues = allowNullValues;
  }

}
