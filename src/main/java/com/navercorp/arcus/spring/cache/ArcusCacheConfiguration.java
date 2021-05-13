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

package com.navercorp.arcus.spring.cache;

import net.spy.memcached.transcoders.Transcoder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class ArcusCacheConfiguration implements InitializingBean {

  private String serviceId;
  private String prefix;
  private int expireSeconds;
  private int timeoutMilliSeconds;
  private Transcoder<Object> operationTranscoder;

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public int getExpireSeconds() {
    return expireSeconds;
  }

  public void setExpireSeconds(int expireSeconds) {
    this.expireSeconds = expireSeconds;
  }

  public int getTimeoutMilliSeconds() {
    return timeoutMilliSeconds;
  }

  public void setTimeoutMilliSeconds(int timeoutMilliSeconds) {
    this.timeoutMilliSeconds = timeoutMilliSeconds;
  }

  public Transcoder<Object> getOperationTranscoder() {
    return operationTranscoder;
  }

  public void setOperationTranscoder(Transcoder<Object> operationTranscoder) {
    this.operationTranscoder = operationTranscoder;
  }

  @Override
  public void afterPropertiesSet() {
    Assert.isTrue(timeoutMilliSeconds > 0, "TimeoutMilliSeconds must be larger than 0.");
  }

}
