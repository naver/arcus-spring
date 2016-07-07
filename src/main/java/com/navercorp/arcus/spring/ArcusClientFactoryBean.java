/*
 * arcus-spring - Arcus as a caching provider for the Spring Cache Abstraction
 * Copyright 2011-2014 NAVER Corp.
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

package com.navercorp.arcus.spring;

import net.spy.memcached.ArcusClient;
import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.DefaultConnectionFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import net.spy.memcached.transcoders.Transcoder;

public class ArcusClientFactoryBean implements FactoryBean<ArcusClientPool>,
		DisposableBean, InitializingBean {

	private ArcusClientPool client;
	private String url;
	private String serviceCode;
	private int poolSize = 4;
	private int frontCacheExpireTime = DefaultConnectionFactory.DEFAULT_FRONTCACHE_EXPIRETIME;
	private int maxFrontCacheElements = DefaultConnectionFactory.DEFAULT_MAX_FRONTCACHE_ELEMENTS;
	private boolean frontCacheCopyOnRead = DefaultConnectionFactory.DEFAULT_FRONT_CACHE_COPY_ON_READ;
	private boolean frontCacheCopyOnWrite = DefaultConnectionFactory.DEFAULT_FRONT_CACHE_COPY_ON_WRITE;
	private int timeoutExceptionThreshold = 100;
	private long maxReconnectDelay = DefaultConnectionFactory.DEFAULT_MAX_RECONNECT_DELAY;

	/**
	 * global transcoder for key/value store.
	 */
	private Transcoder<Object> globalTranscoder;

	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

	public void setFrontCacheExpireTime(int frontCacheExpireTime) {
		this.frontCacheExpireTime = frontCacheExpireTime;
	}

	public void setMaxFrontCacheElements(int maxFrontCacheElements) {
		this.maxFrontCacheElements = maxFrontCacheElements;
	}

	public void setFrontCacheCopyOnRead(boolean copyOnRead) {
		this.frontCacheCopyOnRead = copyOnRead;
	}

	public void setFrontCacheCopyOnWrite(boolean copyOnWrite) {
		this.frontCacheCopyOnWrite = copyOnWrite;
	}

	public void setGlobalTranscoder(Transcoder<Object> tc) {
		this.globalTranscoder = tc;
	}

	public void setTimeoutExceptionThreshold(int timeoutExceptionThreshold) {
		this.timeoutExceptionThreshold = timeoutExceptionThreshold;
	}

	public void setMaxReconnectDelay(long maxReconnectDelay) {
		this.maxReconnectDelay = maxReconnectDelay;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setServiceCode(String serviceCode) {
		this.serviceCode = serviceCode;
	}

	@Override
	public void destroy() throws Exception {
		if (this.client != null) {
			this.client.shutdown();
		}
	}

	@Override
	public ArcusClientPool getObject() throws Exception {
		ConnectionFactoryBuilder cfb = new ConnectionFactoryBuilder();
		cfb.setFrontCacheExpireTime(frontCacheExpireTime);
		cfb.setTimeoutExceptionThreshold(timeoutExceptionThreshold);
		cfb.setFrontCacheCopyOnRead(frontCacheCopyOnRead);
		cfb.setFrontCacheCopyOnWrite(frontCacheCopyOnWrite);
		cfb.setMaxReconnectDelay(maxReconnectDelay);
		if (maxFrontCacheElements > 0) {
			cfb.setMaxFrontCacheElements(maxFrontCacheElements);
		}
		if (globalTranscoder != null) {
			cfb.setTranscoder(globalTranscoder);
		}
		client = ArcusClient.createArcusClientPool(url, serviceCode, cfb,
				poolSize);
		return client;
	}

	@Override
	public Class<?> getObjectType() {
		return ArcusClient.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.url, "Url property must be provided.");
		Assert.notNull(this.serviceCode,
				"ServiceCode property must be provided.");
		Assert.isTrue(this.poolSize > 0,
				"PoolSize property must be larger than 0.");
		Assert.isTrue(this.timeoutExceptionThreshold > 0,
				"TimeoutExceptionThreshold must be larger than 0.");
		Assert.isTrue(this.maxReconnectDelay > 0,
				"MaxReconnectDelay must be larger than 0.");
	}
}
