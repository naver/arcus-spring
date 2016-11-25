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

import com.navercorp.arcus.spring.callback.AsycGetMethod;
import com.navercorp.arcus.spring.callback.SetMethod;
import net.spy.memcached.ArcusClientPool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.TimeUnit;

import static com.navercorp.arcus.spring.callback.ArusCallBackFactory.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@Deprecated
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/arcus_spring_arcusTemplete_test.xml")
public class ArcusTemplateTest {

  @Autowired
  private ArcusClientPool client;
  String key = "sample:testKey";
  ArcusTemplate arcus;

  @Before
  public void setUp() {
    arcus = new ArcusTemplate(client.getClient());
  }

  @Test
  public void valueShouldBeSetAndGot() throws Exception {
    // given
    String value = "setTest";

    // when
    Boolean worked = arcus.execute(set(key, 300, value));

    String valueGot = (String) arcus.execute(asyncGet(key));

    assertThat(worked, is(true));
    assertThat(valueGot, is(value));
  }

  @Test
  public void valueShouldBeSetAndDelete() throws Exception {
    // given
    String value = "setAndDeleteTest";
    Boolean setWorked = arcus.execute(new SetMethod(key, 300, value));
    assertThat(setWorked, is(true));

    // when
    Boolean deleteWorked = arcus.execute(delete(key));

    // then
    assertThat(deleteWorked, is(true));
    String valueGot = (String) arcus.execute(new AsycGetMethod(key));
    assertThat(valueGot, is(nullValue()));
  }

  @Test
  public void valueShouldSetAndExpired() throws Exception {
    // given
    final String value = "expireTest";

    // when
    arcus.execute(new SetMethod(key, 1, value));

    // then
    TimeUnit.SECONDS.sleep(3); // cache가 expired 될 때까지 기다림
    String valueGot = (String) arcus.execute(new AsycGetMethod(key));
    assertThat(valueGot, is(nullValue()));
  }
}
