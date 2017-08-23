/*
 *
 *     Copyright 2017 Expedia, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *
 */

package com.expedia.www.haystack.metricpoints.feature.tests.aggregation.rules

import com.expedia.www.haystack.metricpoints.aggregation.rules.MetricRuleEngine
import com.expedia.www.haystack.metricpoints.entities.MetricType.MetricType
import com.expedia.www.haystack.metricpoints.entities.{MetricPoint, MetricType}
import com.expedia.www.haystack.metricpoints.feature.FeatureSpec

class MetricRuleEngineSpec extends FeatureSpec with MetricRuleEngine {

  val DURATION_METRIC_NAME = "duration"
  val SUCCESS_METRIC_NAME = "success-spans"
  val FAILURE_METRIC_NAME = "failure-spans"
  val TOTAL_METRIC_NAME = "total-spans"
  val INVALID_METRIC_NAME = "invalid_metric"
  val SERVICE_NAME = "dummy_service"
  val OPERATION_NAME = "dummy_operation"

  object TagKeys {
    val OPERATION_NAME_KEY = "operationName"
    val SERVICE_NAME_KEY = "serviceName"
  }

  feature("MetricRuleEngine for identifying MetricRule") {
    scenario("should get Histogram MetricType for duration MetricPoint") {

      Given("a duration MetricPoint")
      val keys = Map(TagKeys.OPERATION_NAME_KEY -> OPERATION_NAME,
        TagKeys.SERVICE_NAME_KEY -> SERVICE_NAME)
      val duration = 10
      val startTime = System.currentTimeMillis

      val metricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Metric, keys, duration, startTime)

      When("trying to find matching MetricType")
      val metricType: MetricType = findMatchingMetric(metricPoint)

      Then("should get Histogram MetricType")
      metricType shouldEqual (MetricType.Histogram)
    }

    scenario("should get Aggregate MetricType for Success MetricPoint") {

      Given("a success MetricPoint")
      val keys = Map(TagKeys.OPERATION_NAME_KEY -> OPERATION_NAME,
        TagKeys.SERVICE_NAME_KEY -> SERVICE_NAME)
      val startTime = System.currentTimeMillis

      val metricPoint = MetricPoint(SUCCESS_METRIC_NAME, MetricType.Metric, keys, 1, startTime)

      When("trying to find matching MetricType")
      val metricType: MetricType = findMatchingMetric(metricPoint)

      Then("should get Aggregate MetricType")
      metricType shouldEqual (MetricType.Aggregate)
    }

    scenario("should get Aggregate MetricType for Failure MetricPoint") {

      Given("a failure MetricPoint")
      val keys = Map(TagKeys.OPERATION_NAME_KEY -> OPERATION_NAME,
        TagKeys.SERVICE_NAME_KEY -> SERVICE_NAME)
      val startTime = System.currentTimeMillis

      val metricPoint = MetricPoint(FAILURE_METRIC_NAME, MetricType.Metric, keys, 1, startTime)

      When("trying to find matching MetricType")
      val metricType: MetricType = findMatchingMetric(metricPoint)

      Then("should get Aggregate MetricType")
      metricType shouldEqual (MetricType.Aggregate)
    }

    scenario("should get Aggregate MetricType for Total MetricPoint") {

      Given("a Total MetricPoint")
      val keys = Map(TagKeys.OPERATION_NAME_KEY -> OPERATION_NAME,
        TagKeys.SERVICE_NAME_KEY -> SERVICE_NAME)
      val startTime = System.currentTimeMillis

      val metricPoint = MetricPoint(TOTAL_METRIC_NAME, MetricType.Metric, keys, 1, startTime)

      When("trying to find matching MetricType")
      val metricType: MetricType = findMatchingMetric(metricPoint)

      Then("should get Aggregate MetricType")
      metricType shouldEqual (MetricType.Aggregate)
    }

    scenario("should get MetricType as Metric for invalid MetricPoint") {

      Given("an invalid MetricPoint")
      val keys = Map(TagKeys.OPERATION_NAME_KEY -> OPERATION_NAME,
        TagKeys.SERVICE_NAME_KEY -> SERVICE_NAME)
      val startTime = System.currentTimeMillis

      val metricPoint = MetricPoint(INVALID_METRIC_NAME, MetricType.Metric, keys, 1, startTime)

      When("trying to find matching MetricType")
      val metricType: MetricType = findMatchingMetric(metricPoint)

      Then("should get Metric MetricType")
      metricType shouldEqual (MetricType.Metric)
    }
  }
}