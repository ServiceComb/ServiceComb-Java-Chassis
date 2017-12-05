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

package io.servicecomb.metrics.core.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.netflix.config.DynamicPropertyFactory;

import io.servicecomb.core.BootListener;
import io.servicecomb.core.definition.SchemaMeta;
import io.servicecomb.core.definition.loader.SchemaLoader;
import io.servicecomb.core.definition.schema.ProducerSchemaFactory;
import io.servicecomb.foundation.common.config.PaaSResourceUtils;
import io.servicecomb.metrics.core.registry.HealthCheckRegistry;
import io.servicecomb.serviceregistry.RegistryUtils;
import io.servicecomb.serviceregistry.api.registry.Microservice;

@Component
public class HealthCheckerBootListener implements BootListener {
  private static final String PUBLISH_ENABLED = "servicecomb.health.publish.enabled";

  private final ProducerSchemaFactory schemaFactory;

  private final SchemaLoader schemaLoader;

  private final HealthCheckRegistry registry;

  @Autowired
  public HealthCheckerBootListener(ProducerSchemaFactory schemaFactory,
      SchemaLoader schemaLoader, HealthCheckRegistry registry) {
    this.schemaFactory = schemaFactory;
    this.schemaLoader = schemaLoader;
    this.registry = registry;
  }

  @Override
  public void onBootEvent(BootEvent event) {
    //inject health provider before ProducerProviderManager init
    if (EventType.BEFORE_PRODUCER_PROVIDER.equals(event.getEventType())) {

      boolean memoryObserverEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty(PUBLISH_ENABLED, false)
          .get();
      if (memoryObserverEnabled) {
        Resource[] resources = PaaSResourceUtils.getResources("servicecomb_internal_health_contract_definition.yaml");
        if (resources.length != 0) {
          Microservice microservice = RegistryUtils.getMicroservice();
          SchemaMeta meta = schemaLoader.registerSchema(microservice.getServiceName(), resources[0]);
          schemaFactory
              .getOrCreateProducerSchema(microservice.getServiceName(), meta.getSchemaId(),
                  DefaultHealthCheckerPublisher.class,
                  new DefaultHealthCheckerPublisher(registry));
        }
      }
    }
  }
}
