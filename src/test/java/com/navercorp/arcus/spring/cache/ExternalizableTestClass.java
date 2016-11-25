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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ExternalizableTestClass implements Externalizable {

  int age;

  SerializableTestClass serializable;

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  public SerializableTestClass getSerializable() {
    return serializable;
  }

  public void setSerializable(SerializableTestClass SerializableTestClass) {
    this.serializable = SerializableTestClass;
  }

  @Override
  public void writeExternal(ObjectOutput objectOutput) throws IOException {
    objectOutput.writeInt(age);
    objectOutput.writeObject(serializable);
  }

  @Override
  public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
    this.age = objectInput.readInt();
    this.serializable = (SerializableTestClass) objectInput.readObject();
  }
}
