/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.servicecomb.swagger.invocation.springmvc.response;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.springframework.http.ResponseEntity;

import io.servicecomb.swagger.invocation.response.ResponseMapperFactorys;
import io.servicecomb.swagger.invocation.response.producer.ProducerResponseMapper;
import io.servicecomb.swagger.invocation.response.producer.ProducerResponseMapperFactory;

public class SpringmvcProducerResponseMapperFactory implements ProducerResponseMapperFactory {
  @Override
  public boolean isMatch(Type swaggerType, Type producerType) {
    if (!ParameterizedType.class.isAssignableFrom(producerType.getClass())) {
      return false;
    }

    return ((ParameterizedType) producerType).getRawType().equals(ResponseEntity.class);
  }

  @Override
  public ProducerResponseMapper createResponseMapper(ResponseMapperFactorys<ProducerResponseMapper> factorys,
      Type swaggerType, Type producerType) {
    Type realProducerType = ((ParameterizedType) producerType).getActualTypeArguments()[0];
    ProducerResponseMapper realMapper = factorys.createResponseMapper(swaggerType, realProducerType);
    return new SpringmvcProducerResponseMapper(realMapper);
  }
}
