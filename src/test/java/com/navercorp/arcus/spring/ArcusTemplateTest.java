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

import java.util.concurrent.TimeUnit;

import net.spy.memcached.ArcusClientPool;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.junit.Assert.assertThat;

@Deprecated
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/arcus_spring_arcusTemplete_test.xml")
public class ArcusTemplateTest {

  @Autowired
  private ArcusClientPool client;
  private ArcusTemplate arcus;

  @Before
  public void setUp() {
    arcus = new ArcusTemplate(client.getClient());
  }

  @Test
  public void valueShouldBeSetAndGot() {
    // given
    String key = "sample:testKey";
    int expireTime = 300;
    String value = "setTest";

    // when
    Boolean worked = arcus.execute(set(key, expireTime, value));

    String valueGot = (String) arcus.execute(asyncGet(key));

    assertThat(worked, is(true));
    assertThat(valueGot, is(value));
  }

  @Test
  public void valueShouldBeSetAndDelete() {
    // given
    String key = "sample:testKey";
    int expireTime = 300;
    String value = "setAndDeleteTest";

    Boolean setWorked = arcus.execute(createSetMethod(key, expireTime, value));
    assertThat(setWorked, is(true));

    // when
    Boolean deleteWorked = arcus.execute(delete(key));

    // then
    assertThat(deleteWorked, is(true));
    String valueGot = (String) arcus.execute(createAsyncGetMethod(key));
    assertThat(valueGot, is(nullValue()));
  }

  @Test
  public void valueShouldSetAndExpired() throws Exception {
    // given
    String key = "sample:testKey";
    String value = "expireTest";

    // when
    arcus.execute(createSetMethod(key, 1, value));

    // then
    TimeUnit.SECONDS.sleep(3); // cache가 expired 될 때까지 기다림
    String valueGot = (String) arcus.execute(createAsyncGetMethod(key));
    assertThat(valueGot, is(nullValue()));
  }

  // There is NO way to avoid deprecated warnings when import deprecated classes.
  // Use full class path to avoid them.
  private com.navercorp.arcus.spring.callback.SetMethod set(String key, int expireTime, String value) {
    return com.navercorp.arcus.spring.callback.ArusCallBackFactory.set(key, expireTime, value);
  }

  private com.navercorp.arcus.spring.callback.AsycGetMethod asyncGet(String key) {
    return com.navercorp.arcus.spring.callback.ArusCallBackFactory.asyncGet(key);
  }

  private com.navercorp.arcus.spring.callback.DeleteMethod delete(String key) {
    return com.navercorp.arcus.spring.callback.ArusCallBackFactory.delete(key);
  }

  private com.navercorp.arcus.spring.callback.SetMethod createSetMethod(String key, int expireTime, String value) {
    return new com.navercorp.arcus.spring.callback.SetMethod(key, expireTime, value);
  }

  private com.navercorp.arcus.spring.callback.AsycGetMethod createAsyncGetMethod(String key) {
    return new com.navercorp.arcus.spring.callback.AsycGetMethod(key);
  }
}
