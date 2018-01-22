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

package org.apache.servicecomb.metrics.overwatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.servicecomb.foundation.common.utils.JsonUtils;
import org.apache.servicecomb.metrics.common.ConsumerInvocationMetric;
import org.apache.servicecomb.metrics.common.MetricsDimension;
import org.apache.servicecomb.metrics.common.RegistryMetric;
import org.apache.servicecomb.metrics.overwatch.dto.InstanceStatus;
import org.apache.servicecomb.metrics.overwatch.dto.SystemStatus;
import org.apache.servicecomb.metrics.push.MetricsPusher;
import org.apache.servicecomb.provider.springmvc.reference.RestTemplateBuilder;
import org.apache.servicecomb.serviceregistry.RegistryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.netflix.config.DynamicPropertyFactory;

@Component
public class OverwatchMetricsPublisher implements MetricsPusher {
  private static final Logger logger = LoggerFactory.getLogger(OverwatchMetricsPublisher.class);

  private static final String METRICS_OVERWATCH_ADDRESS = "servicecomb.metrics.overwatch.address";

  private static final String METRICS_OVERWATCH_WINDOW_TIME = "servicecomb.metrics.overwatch.window_time";

  private final String overwatchURL;

  private final RestTemplate template;

  public OverwatchMetricsPublisher() {
    this.overwatchURL = "http://" + DynamicPropertyFactory.getInstance()
        .getStringProperty(METRICS_OVERWATCH_ADDRESS, "localhost:3000").get() + "/stats/";
    this.template = RestTemplateBuilder.create();
  }

  @Override
  public long getWindowTime() {
    return DynamicPropertyFactory.getInstance().getLongProperty(METRICS_OVERWATCH_WINDOW_TIME, 0).get();
  }

  @Override
  public String getServiceName() {
    return RegistryUtils.getMicroservice().getServiceName();
  }

  @Override
  public void push(RegistryMetric metric) {
    SystemStatus systemStatus = convert(metric);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    try {
      HttpEntity request = new HttpEntity<>(JsonUtils.writeValueAsString(systemStatus), headers);
      ResponseEntity<String> result = this.template.postForEntity(this.overwatchURL, request, String.class);
      String content = result.toString();
    } catch (JsonProcessingException e) {
      logger.error("format status error", e);
    }
  }

  private SystemStatus convert(RegistryMetric metric) {
    Map<String, Map<String, Map<String, InstanceStatus>>> allStatus = new HashMap<>();
    Map<String, Map<String, InstanceStatus>> callServiceStatus = new HashMap<>();
    allStatus.put(getServiceName(), callServiceStatus);
    for (Entry<String, ConsumerInvocationMetric> entry : metric.getConsumerMetrics().entrySet()) {
      String callServiceName = entry.getKey().split("\\.")[0];
      Map<String, InstanceStatus> instanceStatus = callServiceStatus
          .computeIfAbsent(callServiceName, s -> new HashMap<>());
      InstanceStatus status = instanceStatus.computeIfAbsent("total", s -> new InstanceStatus(0, 0));
      instanceStatus.put("total", new InstanceStatus(
          (int) (entry.getValue().getConsumerCall().getTpsValue(MetricsDimension.DIMENSION_STATUS,
              MetricsDimension.DIMENSION_STATUS_SUCCESS_FAILED_SUCCESS).getValue() * 60) + status.getRpm(),
          (int) (entry.getValue().getConsumerCall().getTpsValue(MetricsDimension.DIMENSION_STATUS,
              MetricsDimension.DIMENSION_STATUS_SUCCESS_FAILED_FAILED).getValue() * 60) + status.getFpm()));
    }
    return new SystemStatus((int) (System.currentTimeMillis() / 1000), allStatus);
  }
}
