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

package io.servicecomb.metrics.sample.writefile;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.servicecomb.metrics.sample.writefile.config.FileWriterManager;
import io.servicecomb.serviceregistry.RegistryUtils;
import io.servicecomb.serviceregistry.api.registry.Microservice;

@Component
public class MetricsFileOutput {

  private final FileWriterManager fileWriterManager;

  private final String filePrefix;

  @Autowired
  public MetricsFileOutput(FileWriterManager fileWriterManager) {
    this.fileWriterManager = fileWriterManager;
    Microservice microservice = RegistryUtils.getMicroservice();
    filePrefix = microservice.getAppId() + "." + microservice.getServiceName() + ".";
  }

  public void output(Map<String, String> metrics) {
    for (String metricName : metrics.keySet()) {
      fileWriterManager.write(metricName, filePrefix, metrics.get(metricName));
    }
  }
}
