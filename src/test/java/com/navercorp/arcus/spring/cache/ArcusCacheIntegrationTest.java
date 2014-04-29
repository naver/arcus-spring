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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.navercorp.arcus.spring.cache.ArcusCache;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ArcusCacheConfig.class, loader = AnnotationConfigContextLoader.class)
public class ArcusCacheIntegrationTest {

	@Autowired
	private ArcusCache arcusCache;

	@Before
	public void beforeEach() {
		arcusCache.evict("mykey");
	}

	@Test
	public void testPut() {
		arcusCache.put("mykey", "myvalue");
		assertEquals("myvalue", arcusCache.get("mykey").get());
	}

	@Test
	public void testGet() {
		assertEquals(null, arcusCache.get("mykey"));
		arcusCache.put("mykey", "myvalue");
		assertEquals("myvalue", arcusCache.get("mykey").get());
	}

	@Test
	public void testExternalizableAndSerializable() {
		Foo foo = new Foo();
		foo.setAge(30);

		Bar bar = new Bar();
		bar.setName("someone");
		foo.setBar(bar);

		arcusCache.put("foo", foo);
		Foo loadedBar = (Foo) arcusCache.get("foo").get();
		assertNotNull(loadedBar);
		assertTrue(loadedBar.getAge() == 30);
		assertTrue(loadedBar.getBar().getName().equals("someone"));

		arcusCache.evict("foo");
	}

	@Test
	public void putTheNullValue() {
		arcusCache.put("key", null);

		Object result = arcusCache.get("key");
		assertNull(result);
	}

	@Test
	public void testClear() {
		assertEquals(null, arcusCache.get("mykey"));
		arcusCache.put("mykey", "myvalue");
		assertEquals(null, arcusCache.get("yourkey"));
		arcusCache.put("yourkey", "yourvalue");
		arcusCache.clear();

		assertEquals(null, arcusCache.get("mykey"));
		assertEquals(null, arcusCache.get("yourkey"));
	}

}
