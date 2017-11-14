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

package com.expedia.www.haystack.trends.aggregation

import com.codahale.metrics.Meter
import com.expedia.www.haystack.trends.aggregation.metrics.{Metric, MetricFactory}
import com.expedia.www.haystack.trends.commons.entities.MetricPoint
import com.expedia.www.haystack.trends.commons.metrics.MetricsSupport
import com.expedia.www.haystack.trends.entities.Interval.Interval
import com.expedia.www.haystack.trends.entities.TimeWindow

/**
  * This class contains a metric for each time window being computed for a single trend. The number of time windows at any moment is = no. of intervals
  * if incoming metric point lies within the timewindow the metric is updated
  *
  * @param windowedMetricsMap : map containing timewindows and metrics - generally useful when we want to restore the metric state
  * @param metricFactory      factory which is used to create new metrics when requried.
  */
class WindowedMetric private(var windowedMetricsMap: Map[TimeWindow, Metric], metricFactory: MetricFactory) extends MetricsSupport {

  private val disorderedMetricPoints: Meter = metricRegistry.meter("disordered-metricpoints")

  //Todo: Have to add support for watermarking
  private val numberOfWatermarkedWindows = 1

  private var computedMetrics: Map[Long, Metric] = Map[Long, Metric]()


  def getMetricFactory: MetricFactory = {
    metricFactory
  }

  /**
    * function to compute the incoming metric point
    * it updates all the metrics for the windows within which the incoming metric point lies.
    *
    * @param incomingMetricPoint - incoming metric point
    */
  def compute(incomingMetricPoint: MetricPoint): Unit = {
    windowedMetricsMap.foreach(metricTimeWindowTuple => {
      val currentTimeWindow = metricTimeWindowTuple._1
      val currentMetric = metricTimeWindowTuple._2

      val incomingMetricPointTimeWindow = TimeWindow.apply(incomingMetricPoint.epochTimeInSeconds, currentMetric.getMetricInterval)

      compareAndAddMetric(currentTimeWindow, currentMetric, incomingMetricPointTimeWindow, incomingMetricPoint)
    })
  }

  private def compareAndAddMetric(currentTimeWindow: TimeWindow, currentMetric: Metric, incomingMetricPointTimeWindow: TimeWindow, incomingMetricPoint: MetricPoint) = {

    currentTimeWindow.compare(incomingMetricPointTimeWindow) match {

      // compute to existing metric since in current window
      case number if number == 0 => currentMetric.compute(incomingMetricPoint)

      // belongs to next window, lets flush this one to computedMetrics and create a new window
      case number if number < 0 =>
        windowedMetricsMap -= currentTimeWindow
        windowedMetricsMap += (incomingMetricPointTimeWindow -> metricFactory.createMetric(currentMetric.getMetricInterval))
        computedMetrics += currentTimeWindow.endTime -> currentMetric

      // window already closed and we don't support water marking yet
      case _ => disorderedMetricPoints.mark()
    }
  }

  def getComputedMetricPoints: List[MetricPoint] = {
    val metricPoints = computedMetrics.toList.flatMap {
      case (publishTime, metric) => metric.mapToMetricPoints(publishTime)
    }
    computedMetrics = Map()
    metricPoints
  }
}

/**
  * Windowed metric factory which can create a new windowed metric or restore an existing windowed metric
  */
object WindowedMetric {
  def createWindowedMetric(intervals: List[Interval], firstMetricPoint: MetricPoint, metricFactory: MetricFactory): WindowedMetric = {
    val metricsMap = createMetricsForEachInterval(intervals, firstMetricPoint, metricFactory)
    new WindowedMetric(metricsMap, metricFactory)
  }

  def restoreMetric(windowedMetricsMap: Map[TimeWindow, Metric], metricFactory: MetricFactory): WindowedMetric = {
    new WindowedMetric(windowedMetricsMap, metricFactory)
  }

  private def createMetricsForEachInterval(intervals: List[Interval], metricPoint: MetricPoint, metricFactory: MetricFactory): Map[TimeWindow, Metric] = {

    intervals.map(interval => {
      TimeWindow.apply(metricPoint.epochTimeInSeconds, interval) -> metricFactory.createMetric(interval)
    }).toMap
  }
}