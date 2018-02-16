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

package com.expedia.www.haystack.trends.feature.tests.aggregation

import com.expedia.www.haystack.trends.aggregation.TrendMetric
import com.expedia.www.haystack.trends.aggregation.metrics.{CountMetric, CountMetricFactory, HistogramMetric, HistogramMetricFactory}
import com.expedia.www.haystack.trends.commons.entities.Interval.Interval
import com.expedia.www.haystack.trends.commons.entities.{Interval, MetricPoint, MetricType}
import com.expedia.www.haystack.trends.commons.metrics.MetricsRegistries
import com.expedia.www.haystack.trends.feature.FeatureSpec

class TrendMetricSpec extends FeatureSpec {

  val TOTAL_METRIC_NAME = "total-spans"
  val INVALID_METRIC_NAME = "invalid_metric"
  val SERVICE_NAME = "dummy_service"
  val OPERATION_NAME = "dummy_operation"
  val keys = Map(TagKeys.OPERATION_NAME_KEY -> OPERATION_NAME,
    TagKeys.SERVICE_NAME_KEY -> SERVICE_NAME)

  object TagKeys {
    val OPERATION_NAME_KEY = "operationName"
    val SERVICE_NAME_KEY = "serviceName"
  }

  feature("Creating a TrendMetric") {

    scenario("should get Histogram aggregated MetricPoints post watermarked metrics") {
      val DURATION_METRIC_NAME = "duration"

      Given("some duration MetricPoints")
      val intervals: List[Interval] = List(Interval.ONE_MINUTE)
      val currentTime = 1
      val expectedMetric: HistogramMetric = new HistogramMetric(Interval.ONE_MINUTE)
      val firstMetricPoint: MetricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, keys, 1, currentTime)

      When("creating a WindowedMetric and passing first MetricPoint")
      val trendMetric: TrendMetric = TrendMetric.createTrendMetric(intervals, firstMetricPoint, HistogramMetricFactory)
      trendMetric.compute(firstMetricPoint)
      expectedMetric.compute(firstMetricPoint)

      Then("should return 0 MetricPoints if we try to get within (watermark + 1) metrics")
      trendMetric.getComputedMetricPoints.size shouldBe 0
      var i = TrendMetric.trendMetricConfig(intervals.head)._1
      while (i > 0) {
        val secondMetricPoint: MetricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, keys, 2, currentTime + intervals.head.timeInSeconds * i)
        trendMetric.compute(secondMetricPoint)
        trendMetric.getComputedMetricPoints.size shouldEqual 0
        i = i - 1
      }

      When("adding another MetricPoint after watermark")
      val metricPointAfterWatermark: MetricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, keys, 10, currentTime + intervals.head.timeInSeconds * (TrendMetric.trendMetricConfig(intervals.head)._1 + 1))
      trendMetric.compute(metricPointAfterWatermark)
      val aggMetrics = trendMetric.getComputedMetricPoints
      aggMetrics.size shouldEqual 1 * 7   // HistogramMetric

      Then("values for histogram should same as expected")
      expectedMetric.getRunningHistogram.getMean shouldEqual aggMetrics.find(metricPoint => metricPoint.getMetricPointKey(true).contains("mean")).get.value
      expectedMetric.getRunningHistogram.getMaxValue shouldEqual aggMetrics.find(metricPoint => metricPoint.getMetricPointKey(true).contains("max")).get.value
      expectedMetric.getRunningHistogram.getMinValue shouldEqual aggMetrics.find(metricPoint => metricPoint.getMetricPointKey(true).contains("min")).get.value
      expectedMetric.getRunningHistogram.getValueAtPercentile(99) shouldEqual aggMetrics.find(metricPoint => metricPoint.getMetricPointKey(true).contains("*_99")).get.value
      expectedMetric.getRunningHistogram.getValueAtPercentile(95) shouldEqual aggMetrics.find(metricPoint => metricPoint.getMetricPointKey(true).contains("*_95")).get.value
      expectedMetric.getRunningHistogram.getValueAtPercentile(50) shouldEqual aggMetrics.find(metricPoint => metricPoint.getMetricPointKey(true).contains("*_50")).get.value
    }

    scenario("should get count aggregated MetricPoint post watermarked metrics") {
      val COUNT_METRIC_NAME = "span-received"

      Given("some count MetricPoints")
      val intervals: List[Interval] = List(Interval.ONE_MINUTE, Interval.FIVE_MINUTE)
      val currentTime = 1

      val firstMetricPoint: MetricPoint = MetricPoint(COUNT_METRIC_NAME, MetricType.Gauge, keys, 1, currentTime)
      val trendMetric = TrendMetric.createTrendMetric(intervals, firstMetricPoint, CountMetricFactory)
      trendMetric.compute(firstMetricPoint)
      val expectedMetric: CountMetric = new CountMetric(Interval.FIVE_MINUTE)
      expectedMetric.compute(firstMetricPoint)

      var i = TrendMetric.trendMetricConfig(intervals.last)._1
      while (i > 0) {
        val secondMetricPoint: MetricPoint = MetricPoint(COUNT_METRIC_NAME, MetricType.Gauge, keys, 1, currentTime + intervals.last.timeInSeconds * i)
        trendMetric.compute(secondMetricPoint)
        i = i - 1
      }

      When("adding another MetricPoint after watermark")
      val metricPointAfterWatermark: MetricPoint = MetricPoint(COUNT_METRIC_NAME, MetricType.Gauge, keys, 10, currentTime + intervals.last.timeInSeconds * (TrendMetric.trendMetricConfig(intervals.head)._1 + 1))
      trendMetric.compute(metricPointAfterWatermark)
      val aggMetrics = trendMetric.getComputedMetricPoints

      Then("values for count should same as expected")
      expectedMetric.getCurrentCount shouldEqual aggMetrics.find(metricPoint => metricPoint.getMetricPointKey(true).contains("FiveMinute")).get.value
    }

    scenario("jmx metric (metricpoints.invalid) should be set for invalid MetricPoints") {
      val DURATION_METRIC_NAME = "duration"
      val validMetricPoint: MetricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, keys, 10, currentTimeInSecs)
      val intervals: List[Interval] = List(Interval.ONE_MINUTE, Interval.FIFTEEN_MINUTE)
      val trendMetric: TrendMetric = TrendMetric.createTrendMetric(intervals, validMetricPoint, HistogramMetricFactory)
      val metricsRegistry = MetricsRegistries.metricRegistry

      Given("metric points with invalid values")
      val negativeValueMetricPoint: MetricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, keys, -1, currentTimeInSecs)
      val zeroValueMetricPoint: MetricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, keys, 0, currentTimeInSecs)

      When("computing a negative value MetricPoint")
      trendMetric.compute(negativeValueMetricPoint)

      Then("metric for invalid value should get incremented")
      metricsRegistry.getMeters.get("metricpoints.invalid").getCount shouldEqual 1

      When("computing a zero value MetricPoint")
      trendMetric.compute(zeroValueMetricPoint)     //to prevent hdr histogram from breaking

      Then("metric for invalid value should get incremented")
      metricsRegistry.getMeters.get("metricpoints.invalid").getCount shouldEqual 2
    }
  }
}
