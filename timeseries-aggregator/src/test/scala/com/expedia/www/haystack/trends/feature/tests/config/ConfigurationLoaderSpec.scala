/*
 *  Copyright 2017 Expedia, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.expedia.www.haystack.trends.feature.tests.config

import com.expedia.www.haystack.commons.entities.encoders.PeriodReplacementEncoder
import com.expedia.www.haystack.trends.config.ProjectConfiguration
import com.expedia.www.haystack.trends.feature.FeatureSpec

class ConfigurationLoaderSpec extends FeatureSpec {

  feature("Configuration loader") {

    scenario("should load the health status config from base.conf") {

      Given("A config file at base config file containing config for health status file path")
      val healthStatusFilePath = "/app/isHealthy"

      When("When the configuration is loaded in project configuration")
      val projectConfig = new ProjectConfiguration()

      Then("the healthStatusFilePath should be correct")
      projectConfig.healthStatusFilePath shouldEqual healthStatusFilePath
    }

    scenario("should load the metric point enable period replacement config from base.conf") {

      Given("A config file at base config file containing config for enable period replacement")
      val enableMetricPointPeriodReplacement = true

      When("When the configuration is loaded in project configuration")
      val projectConfig = new ProjectConfiguration()

      Then("the encoder should be correct")
      projectConfig.encoder shouldBe an[PeriodReplacementEncoder]
    }

    scenario("should load the kafka config from base.conf") {

      Given("A config file at base config file containing kafka ")

      When("When the configuration is loaded in project configuration")
      val projectConfig = new ProjectConfiguration()

      Then("It should create the write configuration object based on the file contents")
      val kafkaConfig = projectConfig.kafkaConfig
      kafkaConfig.consumeTopic shouldBe "metricpoints"
    }


    scenario("should override configuration based on environment variable") {


      Given("A config file at base config file containing config for kafka")

      When("When the configuration is loaded in project configuration")
      val projectConfig = new ProjectConfiguration()

      Then("It should override the configuration object based on the environment variable if it exists")

      val kafkaProduceTopic = sys.env.getOrElse("HAYSTACK_PROP_KAFKA_PRODUCER_TOPIC", "mdm")
      val kafkaConfig = projectConfig.kafkaConfig
      kafkaConfig.producerConfig.topic shouldBe kafkaProduceTopic
    }

    scenario("should load the state store configs from base.conf") {

      Given("A config file at base config file containing kafka ")

      When("When the configuration is loaded in project configuration")
      val projectConfig = new ProjectConfiguration()

      Then("It should create the write configuration object based on the file contents")
      val stateStoreConfigs = projectConfig.stateStoreConfig
      projectConfig.enableStateStoreLogging shouldBe true
      projectConfig.loggingDelayInSeconds shouldBe 60
      stateStoreConfigs("cleanup.policy") shouldBe "compact,delete"
      stateStoreConfigs("retention.ms") shouldBe "14400000"
    }

    scenario("should load the external kafka configs from base.conf") {

      Given("A config file at base config file containing kafka ")

      When("When the configuration is loaded in project configuration")
      val projectConfig = new ProjectConfiguration()

      Then("It should create the write configuration object based on the file contents")
      projectConfig.kafkaConfig.producerConfig.enableExternalKafka shouldBe true
      projectConfig.kafkaConfig.producerConfig.props.get.getProperty("bootstrap.servers") shouldBe "kafkasvc:9092"
    }
  }
}
