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

package com.navercorp.arcus.spring.callback;

import java.util.concurrent.Future;

import net.spy.memcached.ArcusClient;

@Deprecated
public class AsyncLopDeleteByRangeMethod implements ArcusCallBack<Boolean> {
	private String key;
	private int from;
	private int to;

	public AsyncLopDeleteByRangeMethod(String key, int from, int to) {
		this.key = key;
		this.from = from;
		this.to = to;
	}

	@Override
	public Future<Boolean> doInArcus(ArcusClient arcusClient) {
		return arcusClient.asyncLopDelete(key, from, to, true);
	}
}
