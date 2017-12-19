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

package io.servicecomb.swagger.generator.core.processor.parameter;

import io.servicecomb.swagger.generator.core.OperationGenerator;
import io.servicecomb.swagger.generator.core.ParameterAnnotationProcessor;
import io.servicecomb.swagger.generator.core.utils.ParamUtils;
import io.swagger.annotations.ApiParam;
import io.swagger.models.parameters.AbstractSerializableParameter;

public abstract class AbstractParameterProcessor<T extends AbstractSerializableParameter<?>>
    implements ParameterAnnotationProcessor {
  @Override
  public void process(Object annotation, OperationGenerator operationGenerator, int paramIdx) {
    T parameter = createParameter();

    fillParameter(annotation, operationGenerator, paramIdx, parameter);

    operationGenerator.addProviderParameter(parameter);
  }

  protected void fillParameter(Object annotation, OperationGenerator operationGenerator, int paramIdx,
      T parameter) {
    setParameterType(operationGenerator, paramIdx, parameter);
    setParameterName(annotation, operationGenerator, paramIdx, parameter);
  }

  protected void setParameterType(OperationGenerator operationGenerator, int paramIdx,
      T parameter) {
    ParamUtils.setParameterType(operationGenerator.getSwagger(),
        operationGenerator.getProviderMethod(),
        paramIdx,
        parameter);
  }

  protected void setParameterName(Object annotation, OperationGenerator operationGenerator, int paramIdx,
      T parameter) {
    String paramName = getAnnotationParameterName(annotation);
    paramName = ParamUtils.getParameterName(paramName, operationGenerator.getProviderMethod(), paramIdx);
    parameter.setName(paramName);
    parameter.setCollectionFormat(getAnnotationParameterColFmt(annotation));
  }

  protected abstract T createParameter();

  protected abstract String getAnnotationParameterName(Object annotation);

  protected String getAnnotationParameterColFmt(Object annotation) {
    return null;
  }
}
