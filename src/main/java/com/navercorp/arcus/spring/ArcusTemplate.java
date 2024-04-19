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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.memcached.ArcusClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class ArcusTemplate {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ArcusClient arcusClient;

  public ArcusTemplate(ArcusClient arcusClient) {
    this.arcusClient = arcusClient;
  }

  // There is NO way to avoid deprecated warnings when import deprecated classes.
  // Use full class path to avoid them.
  public <T> T execute(final com.navercorp.arcus.spring.callback.ArcusCallBack<T> methodCall) {
    return executeAndHandleException(methodCall, new FutureGetter<T>() {
      public T get(Future<T> future) throws InterruptedException,
              ExecutionException {
        return future.get();
      }
    });
  }

  public <T> T execute(final com.navercorp.arcus.spring.callback.ArcusCallBack<T> methodCall,
                       final long timeout,
                       final TimeUnit unit) {
    return executeAndHandleException(methodCall, new FutureGetter<T>() {
      public T get(Future<T> future) throws InterruptedException,
              ExecutionException, TimeoutException {
        return future.get(timeout, unit);
      }
    });
  }

  private <T> T executeAndHandleException(final com.navercorp.arcus.spring.callback.ArcusCallBack<T> methodCall,
                                          FutureGetter<T> futureGetter) {
    T arcusResponse = null;
    Future<T> link = null;
    try {
      link = methodCall.doInArcus(arcusClient);
      arcusResponse = futureGetter.get(link);
    } catch (Exception e) {
      if (link != null) {
        link.cancel(true);
      }
      logger.error(e.getMessage());
    }
    return arcusResponse;
  }

  private interface FutureGetter<T> {
    T get(Future<T> future) throws InterruptedException,
            ExecutionException, TimeoutException;
  }
}
