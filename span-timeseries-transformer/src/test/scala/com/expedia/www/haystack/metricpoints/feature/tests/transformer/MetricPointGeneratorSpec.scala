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
package com.expedia.www.haystack.metricpoints.feature.tests.transformer

import com.expedia.open.tracing.{Process, Span, Tag}
import com.expedia.www.haystack.metricpoints.serde.adapters.MetricTankAdapter
import com.expedia.www.haystack.metricpoints.entities.exceptions.MetricPointCreationException
import com.expedia.www.haystack.metricpoints.entities.{MetricType, TagKeys}
import com.expedia.www.haystack.metricpoints.feature.FeatureSpec
import com.expedia.www.haystack.metricpoints.transformer.{MetricPointGenerator, MetricPointTransformer}


class MetricPointGeneratorSpec extends FeatureSpec with MetricPointGenerator {


  private def getMetricPointTransformers = {
    val ref = classOf[MetricPointGenerator].getInterfaces.count(interface => interface.getInterfaces.headOption.contains(classOf[MetricPointTransformer]))
    ref
  }

  feature("The metricPoint generator must generate metricPoints given a span object") {


    scenario("any valid span object") {
      val operationName = "testSpan"
      val serviceName = "testService"
      Given("a valid span")
      val process = Process.newBuilder().setServiceName(serviceName)
      val span = Span.newBuilder()
        .setDuration(System.currentTimeMillis())
        .setOperationName(operationName)
        .setProcess(process)
        .addTags(Tag.newBuilder().setKey(ERROR_KEY).setVBool(false))
        .build()
      When("its asked to map to metricPoints")
      val metricPoints = mapSpans(span).getOrElse(List())

      Then("the number of metricPoints returned should be equal to the number of metricPoint transformers")
      metricPoints should not be empty
      val metricPointTransformers = getMetricPointTransformers
      metricPoints.size shouldEqual metricPointTransformers
      var metricPointIds = Set[String]()

      Then("each metricPoint should have a unique combination of keys")
      metricPoints.foreach(metricPoint => {
        metricPointIds += metricPoint.getMetricPointKey
      })
      metricPointIds.size shouldEqual metricPointTransformers

      Then("each metricPoint should have the timestamps which is equal to the span timestamp")
      metricPoints.foreach(metricPoint => {
        metricPoint.epochTimeInSeconds shouldEqual span.getStartTime
      })

      Then("each metricPoint should have the metric type as Metric")
      metricPoints.foreach(metricPoint => {
        metricPoint.`type` shouldEqual MetricType.Gauge
      })

    }

    scenario("an invalid span object") {
      val operationName = ""
      val serviceName = ""
      Given("an invalid span")
      val process = Process.newBuilder().setServiceName(serviceName)
      val span = Span.newBuilder()
        .setDuration(System.currentTimeMillis())
        .setOperationName(operationName)
        .setProcess(process)
        .addTags(Tag.newBuilder().setKey(ERROR_KEY).setVBool(false))
        .build()

      When("its asked to map to metricPoints")
      val metricPoints = mapSpans(span)

      Then("It should return a metricPoint creation exception")
      metricPoints.isFailure shouldBe true
      metricPoints.failed.get.isInstanceOf[MetricPointCreationException] shouldBe true
    }


    scenario("a span object with a valid operation Name") {
      val operationName = "testSpan"
      val serviceName = "testService"

      Given("a valid span")
      val process = Process.newBuilder().setServiceName(serviceName)
      val span = Span.newBuilder()
        .setDuration(System.currentTimeMillis())
        .setOperationName(operationName)
        .setProcess(process)
        .addTags(Tag.newBuilder().setKey(ERROR_KEY).setVBool(false))
        .build()

      When("its asked to map to metricPoints")
      val metricPoints = mapSpans(span).getOrElse(List())

      Then("it should create metricPoints with operation name as one its keys")
      metricPoints.map(metricPoint => {
        metricPoint.tags.get(TagKeys.OPERATION_NAME_KEY) should not be None
        metricPoint.tags.get(TagKeys.OPERATION_NAME_KEY) shouldEqual Some(operationName)
      })
    }

    scenario("a span object with a valid service Name") {
      val operationName = "testSpan"
      val serviceName = "testService"

      Given("a valid span")
      val process = Process.newBuilder().setServiceName(serviceName)
      val span = Span.newBuilder()
        .setDuration(System.currentTimeMillis())
        .setOperationName(operationName)
        .setProcess(process)
        .addTags(Tag.newBuilder().setKey(ERROR_KEY).setVBool(false))
        .build()

      When("its asked to map to metricPoints")
      val metricPoints = mapSpans(span).get

      Then("it should create metricPoints with service name as one its keys")
      metricPoints.map(metricPoint => {
        metricPoint.tags.get(TagKeys.SERVICE_NAME_KEY) should not be None
        metricPoint.tags.get(TagKeys.SERVICE_NAME_KEY) shouldEqual Some(serviceName)
      })
    }
  }
}
