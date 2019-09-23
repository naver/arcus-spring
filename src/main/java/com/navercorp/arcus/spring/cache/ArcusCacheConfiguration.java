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
  public void afterPropertiesSet() throws Exception {
    Assert.isTrue(timeoutMilliSeconds > 0, "TimeoutMilliSeconds must be larger than 0.");
  }

}
