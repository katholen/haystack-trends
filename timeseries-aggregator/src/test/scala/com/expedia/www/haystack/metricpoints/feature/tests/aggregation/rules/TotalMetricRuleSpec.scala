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
import com.expedia.www.haystack.metricpoints.aggregation.rules.TotalMetricRule
import com.expedia.www.haystack.metricpoints.entities.{MetricPoint, MetricType}
import com.expedia.www.haystack.metricpoints.feature.FeatureSpec

class TotalMetricRuleSpec extends FeatureSpec with TotalMetricRule {

  val TOTAL_METRIC_NAME = "total-spans"
  val SERVICE_NAME = "dummy_service"
  val OPERATION_NAME = "dummy_operation"

  object TagKeys {
    val OPERATION_NAME_KEY = "operationName"
    val SERVICE_NAME_KEY = "serviceName"
  }

  feature("TotalMetricRule for identifying MetricRule") {

    scenario("should get Aggregate AggregationType for Total MetricPoint") {

      Given("a Total MetricPoint")
      val keys = Map(TagKeys.OPERATION_NAME_KEY -> OPERATION_NAME,
        TagKeys.SERVICE_NAME_KEY -> SERVICE_NAME)
      val startTime = System.currentTimeMillis

      val metricPoint = MetricPoint(TOTAL_METRIC_NAME, MetricType.Gauge, keys, 1, startTime)

      When("trying to find matching AggregationType")
      val aggregationType: AggregationType = isMatched(metricPoint)

      Then("should get Aggregate AggregationType")
      aggregationType shouldEqual AggregationType.Count
    }

  }
}