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

import org.junit.Test;

import com.navercorp.arcus.spring.cache.StringKeyGenerator;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StringKeyGeneratorTest {
	StringKeyGenerator keyGenerator = new StringKeyGenerator();

	@Test
	public void testExtract() throws Exception {
		StringBuilder longParam = new StringBuilder();
		for (int i = 0; i < 255; i++) {
			longParam.append(i);
		}
		String key = ((ArcusStringKey) (keyGenerator.generate(null, null, longParam))).getStringKey();
		assertThat(key.length() > 255, is(true));
		System.out.println(key);
	}

	@Test
	public void testDuplicatedKeysWithColons() {
		ArcusStringKey arcusKey1 = (ArcusStringKey) keyGenerator.generate(null, null, "a,b", "c", "de");
		ArcusStringKey arcusKey2 = (ArcusStringKey) keyGenerator.generate(null, null, "a,b", "c,de");

		assertEquals(arcusKey1.getStringKey(), arcusKey2.getStringKey());
		assertTrue(arcusKey1.getHash() != arcusKey2.getHash());
	}

}
