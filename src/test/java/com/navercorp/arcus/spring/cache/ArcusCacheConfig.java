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

package com.navercorp.arcus.spring.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.navercorp.arcus.spring.ArcusClientFactoryBean;

@Deprecated
@Configuration
public class ArcusCacheConfig {

	static String ADMIN = "127.0.0.1:2181";
	static String SERVICE_CODE = "test";
	static String PREFIX = "arcusCache";
	static int POOLSIZE = 4;
	static long TIMEOUT = 100;

	@Bean
	public ArcusClientFactoryBean arcusClientFactory() {
		ArcusClientFactoryBean f = new ArcusClientFactoryBean();

		f.setUrl(ADMIN);
		f.setServiceCode(SERVICE_CODE);
		f.setPoolSize(POOLSIZE);
		f.setTimeoutExceptionThreshold(50);
		f.setMaxReconnectDelay(30);

		return f;
	}

	@Bean
	public ArcusCache arcusCache() throws Exception {
		ArcusCache c = defaultArcusCache();

		c.setName("arcusCache");
		c.setPrefix(PREFIX);
		c.setTimeoutMilliSeconds(TIMEOUT);

		return c;
	}

	private ArcusCache defaultArcusCache() throws Exception {
		ArcusCache c = new ArcusCache();

		c.setServiceId(SERVICE_CODE);
		c.setArcusClient(arcusClientFactory().getObject());
		c.setExpireSeconds(60 * 60 * 12);

		return c;
	}

}
