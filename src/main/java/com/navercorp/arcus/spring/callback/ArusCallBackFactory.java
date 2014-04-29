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

public class ArusCallBackFactory {

	public static AsycGetMethod asyncGet(String key) {
		return new AsycGetMethod(key);
	}

	public static SetMethod set(String key, int expSeconds, String value) {
		return new SetMethod(key, expSeconds, value);
	}

	public static DeleteMethod delete(String key) {
		return new DeleteMethod(key);
	}
}
