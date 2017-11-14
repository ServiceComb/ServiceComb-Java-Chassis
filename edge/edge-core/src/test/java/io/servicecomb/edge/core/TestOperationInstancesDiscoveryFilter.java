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

package io.servicecomb.edge.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.google.common.eventbus.EventBus;

import io.servicecomb.core.CseContext;
import io.servicecomb.core.Invocation;
import io.servicecomb.core.definition.MicroserviceVersionMeta;
import io.servicecomb.core.definition.MicroserviceVersionMetaFactory;
import io.servicecomb.core.definition.OperationMeta;
import io.servicecomb.core.definition.loader.SchemaListenerManager;
import io.servicecomb.core.definition.loader.SchemaLoader;
import io.servicecomb.core.definition.schema.ConsumerSchemaFactory;
import io.servicecomb.foundation.common.utils.BeanUtils;
import io.servicecomb.serviceregistry.RegistryUtils;
import io.servicecomb.serviceregistry.ServiceRegistry;
import io.servicecomb.serviceregistry.api.registry.Microservice;
import io.servicecomb.serviceregistry.api.registry.MicroserviceInstance;
import io.servicecomb.serviceregistry.config.ServiceRegistryConfig;
import io.servicecomb.serviceregistry.consumer.MicroserviceVersionRule;
import io.servicecomb.serviceregistry.consumer.MicroserviceVersions;
import io.servicecomb.serviceregistry.definition.DefinitionConst;
import io.servicecomb.serviceregistry.definition.MicroserviceDefinition;
import io.servicecomb.serviceregistry.discovery.DiscoveryContext;
import io.servicecomb.serviceregistry.discovery.DiscoveryTreeNode;
import io.servicecomb.serviceregistry.registry.LocalServiceRegistry;
import io.servicecomb.swagger.SwaggerUtils;
import io.servicecomb.swagger.generator.core.SwaggerGenerator;
import io.servicecomb.swagger.generator.core.SwaggerGeneratorContext;
import io.servicecomb.swagger.generator.jaxrs.JaxrsSwaggerGeneratorContext;
import mockit.Expectations;
import mockit.Mocked;

public class TestOperationInstancesDiscoveryFilter {
  @Path("/v1")
  private static class V1_0_0 {
    @Path("/add")
    @GET
    public void add() {
    }
  }

  @Path("/v1")
  private static class V1_1_0 {
    @Path("/add")
    @GET
    public void add() {
    }

    @Path("/dec")
    @GET
    public void dec() {
    }
  }

  OperationInstancesDiscoveryFilter filter = new OperationInstancesDiscoveryFilter();

  DiscoveryContext context = new DiscoveryContext();

  SwaggerGeneratorContext swaggerGeneratorContext = new JaxrsSwaggerGeneratorContext();

  EventBus eventBus = new EventBus();

  String appId = "app";

  String microserviceName = "ms";

  ServiceRegistry serviceRegistry =
      new LocalServiceRegistry(eventBus, ServiceRegistryConfig.INSTANCE,
          MicroserviceDefinition.create(appId, "self"));

  @Mocked
  ApplicationContext applicationContext;

  @Mocked
  Invocation invocation;

  OperationMeta latestOperationMeta;

  SchemaLoader schemaLoader = new SchemaLoader();

  ConsumerSchemaFactory consumerSchemaFactory = new ConsumerSchemaFactory();

  DiscoveryTreeNode result;

  @Mocked
  SchemaListenerManager schemaListenerManager;

  @Before
  public void setup() {
    serviceRegistry.init();
    BeanUtils.setContext(applicationContext);

    serviceRegistry.getAppManager().setMicroserviceVersionFactory(new MicroserviceVersionMetaFactory());

    consumerSchemaFactory.setSchemaLoader(schemaLoader);
    CseContext.getInstance().setConsumerSchemaFactory(consumerSchemaFactory);
    CseContext.getInstance().setSchemaListenerManager(schemaListenerManager);

    RegistryUtils.setServiceRegistry(serviceRegistry);
  }

  private void setupOnChange() {
    new Expectations(RegistryUtils.class) {
      {
        invocation.getOperationMeta();
        result = latestOperationMeta;
        invocation.getMicroserviceQualifiedName();
        result = latestOperationMeta.getMicroserviceQualifiedName();
      }
    };

    context.setInputParameters(invocation);
  }

  @After
  public void teardown() {
    RegistryUtils.setServiceRegistry(null);
    BeanUtils.setContext(null);
    CseContext.getInstance().setConsumerSchemaFactory(null);
    CseContext.getInstance().setSchemaListenerManager(null);
  }

  private MicroserviceInstance createInstance(String serviceId) {
    MicroserviceInstance instance = new MicroserviceInstance();
    instance.setInstanceId(UUID.randomUUID().toString());
    instance.setServiceId(serviceId);
    return instance;
  }

  private Microservice regMicroservice(String serviceId, String version, Class<?> schemaCls, int instanceCount) {
    String schemaId = "sid";

    Microservice microservice = new Microservice();
    microservice.setServiceId(serviceId);
    microservice.setAppId(appId);
    microservice.setServiceName(microserviceName);
    microservice.setVersion(version);
    microservice.setSchemas(Arrays.asList(schemaId));
    serviceRegistry.getServiceRegistryClient().registerMicroservice(microservice);

    SwaggerGenerator generator = new SwaggerGenerator(swaggerGeneratorContext, schemaCls);
    String schemaContent = SwaggerUtils.swaggerToString(generator.generate());
    serviceRegistry.getServiceRegistryClient().registerSchema(serviceId, schemaId, schemaContent);

    for (int idx = 0; idx < instanceCount; idx++) {
      MicroserviceInstance instance = createInstance(serviceId);
      serviceRegistry.getServiceRegistryClient().registerMicroserviceInstance(instance);
    }

    return microservice;
  }

  @Test
  public void getOrder() {
    Assert.assertEquals(-10000, filter.getOrder());
  }

  @Test
  public void isGroupingFilter() {
    Assert.assertTrue(filter.isGroupingFilter());
  }

  @Test
  public void discovery_v1_0_0() {
    regMicroservice("1", "1.0.0", V1_0_0.class, 2);

    MicroserviceVersions MicroserviceVersions =
        serviceRegistry.getAppManager().getOrCreateMicroserviceVersions(appId, microserviceName);
    MicroserviceVersions.submitPull();
    MicroserviceVersionRule microserviceVersionRule =
        MicroserviceVersions.getOrCreateMicroserviceVersionRule(DefinitionConst.VERSION_RULE_ALL);
    MicroserviceVersionMeta latestMicroserviceVersionMeta = microserviceVersionRule.getLatestMicroserviceVersion();
    latestOperationMeta = latestMicroserviceVersionMeta.getMicroserviceMeta().ensureFindOperation("sid.add");
    DiscoveryTreeNode parent = new DiscoveryTreeNode().fromCache(microserviceVersionRule.getVersionedCache());

    setupOnChange();

    result = filter.discovery(context, parent);

    Assert.assertEquals(2, result.mapData().size());
    result.mapData().values().forEach(instance -> {
      Assert.assertEquals("1", ((MicroserviceInstance) instance).getServiceId());
    });
  }

  @Test
  public void discovery_v1_1_0_add() {
    regMicroservice("1", "1.0.0", V1_0_0.class, 2);
    regMicroservice("2", "1.1.0", V1_1_0.class, 2);

    MicroserviceVersions MicroserviceVersions =
        serviceRegistry.getAppManager().getOrCreateMicroserviceVersions(appId, microserviceName);
    MicroserviceVersions.submitPull();
    MicroserviceVersionRule microserviceVersionRule =
        MicroserviceVersions.getOrCreateMicroserviceVersionRule(DefinitionConst.VERSION_RULE_ALL);
    MicroserviceVersionMeta latestMicroserviceVersionMeta = microserviceVersionRule.getLatestMicroserviceVersion();
    latestOperationMeta = latestMicroserviceVersionMeta.getMicroserviceMeta().ensureFindOperation("sid.add");
    DiscoveryTreeNode parent = new DiscoveryTreeNode().fromCache(microserviceVersionRule.getVersionedCache());

    setupOnChange();

    result = filter.discovery(context, parent);

    Assert.assertEquals(4, result.mapData().size());
    Set<String> ids = new HashSet<>();
    result.mapData().values().forEach(instance -> {
      ids.add(((MicroserviceInstance) instance).getServiceId());
    });
    Assert.assertThat(ids, Matchers.containsInAnyOrder("1", "2"));
  }

  @Test
  public void discovery_v1_1_0_dec() {
    regMicroservice("1", "1.0.0", V1_0_0.class, 2);
    regMicroservice("2", "1.1.0", V1_1_0.class, 2);

    MicroserviceVersions MicroserviceVersions =
        serviceRegistry.getAppManager().getOrCreateMicroserviceVersions(appId, microserviceName);
    MicroserviceVersions.submitPull();
    MicroserviceVersionRule microserviceVersionRule =
        MicroserviceVersions.getOrCreateMicroserviceVersionRule(DefinitionConst.VERSION_RULE_ALL);
    MicroserviceVersionMeta latestMicroserviceVersionMeta = microserviceVersionRule.getLatestMicroserviceVersion();
    latestOperationMeta = latestMicroserviceVersionMeta.getMicroserviceMeta().ensureFindOperation("sid.dec");
    DiscoveryTreeNode parent = new DiscoveryTreeNode().fromCache(microserviceVersionRule.getVersionedCache());

    setupOnChange();

    result = filter.discovery(context, parent);

    Assert.assertEquals(2, result.mapData().size());
    Set<String> ids = new HashSet<>();
    result.mapData().values().forEach(instance -> {
      ids.add(((MicroserviceInstance) instance).getServiceId());
    });
    Assert.assertThat(ids, Matchers.containsInAnyOrder("2"));
  }
}
