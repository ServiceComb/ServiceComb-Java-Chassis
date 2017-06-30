/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.tests.tracing;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import io.servicecomb.provider.rest.common.RestSchema;
import java.util.Random;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

@RestSchema(schemaId = "someTracedJaxrsRestEndpoint")
@Path("/jaxrs")
public class TracedJaxrsController {
  private static final Logger logger = LoggerFactory.getLogger(TracedJaxrsController.class);
  private final Random random = new Random();

  @Autowired
  private RestTemplate template;

  @GET
  @Path("/bonjour")
  @Produces(TEXT_PLAIN)
  public String bonjour(HttpServletRequest request) throws InterruptedException {
    logger.info("in /bonjour");
    Thread.sleep(random.nextInt(1000));

    request.getRequestURL();
    return "bonjour le " + template.getForObject("http://localhost:8080/jaxrs/monde", String.class);
  }

  @GET
  @Path("/monde")
  @Produces(TEXT_PLAIN)
  public String monde() throws InterruptedException {
    logger.info("in /monde");
    Thread.sleep(random.nextInt(1000));

    return "monde";
  }
}