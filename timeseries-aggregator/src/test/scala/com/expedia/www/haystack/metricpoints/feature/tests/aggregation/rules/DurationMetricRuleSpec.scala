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

import com.expedia.www.haystack.metricpoints.aggregation.metrics.AggregationType
import com.expedia.www.haystack.metricpoints.aggregation.metrics.AggregationType.AggregationType
import com.expedia.www.haystack.metricpoints.aggregation.rules.{DurationMetricRule, MetricRuleEngine}
import com.expedia.www.haystack.metricpoints.entities.{MetricPoint, MetricType}
import com.expedia.www.haystack.metricpoints.feature.FeatureSpec

class DurationMetricRuleSpec extends FeatureSpec with DurationMetricRule {

  val DURATION_METRIC_NAME = "duration"
  val SERVICE_NAME = "dummy_service"
  val OPERATION_NAME = "dummy_operation"

  object TagKeys {
    val OPERATION_NAME_KEY = "operationName"
    val SERVICE_NAME_KEY = "serviceName"
  }

  feature("DurationMetricRule for identifying MetricRule") {

    scenario("should get Histogram AggregationType for duration MetricPoint") {

      Given("a duration MetricPoint")
      val keys = Map(TagKeys.OPERATION_NAME_KEY -> OPERATION_NAME,
        TagKeys.SERVICE_NAME_KEY -> SERVICE_NAME)
      val duration = 10
      val startTime = System.currentTimeMillis

      val metricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, keys, duration, startTime)

      When("trying to find matching AggregationType")
      val aggregationType: AggregationType = isMatched(metricPoint)

      Then("should get Histogram AggregationType")
      aggregationType shouldEqual AggregationType.Histogram
    }
  }
}