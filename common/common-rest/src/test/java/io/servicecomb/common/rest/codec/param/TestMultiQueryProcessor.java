/*
 * Copyright 2017 Huawei Technologies Co., Ltd
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

package io.servicecomb.common.rest.codec.param;

import javax.servlet.http.HttpServletRequest;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.type.TypeFactory;

import io.servicecomb.common.rest.codec.QueryProcessorFactory.MultiQueryProcessor;
import mockit.Expectations;
import mockit.Mocked;

public class TestMultiQueryProcessor {
  @Mocked
  HttpServletRequest request;

  private ParamValueProcessor createProcessor(String name, Class<?> type) {
    return new MultiQueryProcessor(name, TypeFactory.defaultInstance().constructType(type));
  }

  @Test
  public void testGetValueNormal() throws Exception {
    new Expectations() {
      {
        request.getParameter("name");
        result = "value";
      }
    };

    ParamValueProcessor processor = createProcessor("name", String.class);
    Object value = processor.getValue(request);
    Assert.assertEquals("value", value);
  }

  @Test
  public void testGetValueContainerType() throws Exception {
    new Expectations() {
      {
        request.getParameterValues("name");
        result = new String[] {"v1,v2,v3"};
      }
    };

    ParamValueProcessor processor = createProcessor("name", String[].class);
    String[] value = (String[]) processor.getValue(request);
    Assert.assertThat(value, Matchers.arrayContaining("v1,v2,v3"));
  }

  @Test
  public void testGetProcessorType() {
    ParamValueProcessor processor = createProcessor("name", String.class);
    Assert.assertEquals("query", processor.getProcessorType());
  }
}
