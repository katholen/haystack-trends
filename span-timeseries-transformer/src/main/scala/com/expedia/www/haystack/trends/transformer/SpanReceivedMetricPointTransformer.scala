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
package com.expedia.www.haystack.trends.transformer

import com.expedia.open.tracing.Span
import com.expedia.www.haystack.commons.entities.{MetricPoint, MetricType}

/**
  * This transformer is responsible to generate the total-spans gauge metric
  */
trait SpanReceivedMetricPointTransformer extends MetricPointTransformer {
  private val spanReceivedMetricPoints = metricRegistry.meter("metricpoint.span.received")

  val TOTAL_METRIC_NAME = "received-span"

  override def mapSpan(span: Span, serviceOnlyFlag: Boolean): List[MetricPoint] = {
    spanReceivedMetricPoints.mark()
    var metricPoints = List(MetricPoint(TOTAL_METRIC_NAME, MetricType.Gauge, createCommonMetricTags(span), 1, getDataPointTimestamp(span)))

    if (serviceOnlyFlag) {
      metricPoints = metricPoints :+ MetricPoint(TOTAL_METRIC_NAME, MetricType.Gauge, createServiceOnlyMetricTags(span), 1, getDataPointTimestamp(span))
    }

    metricPoints
  }
}

object SpanReceivedMetricPointTransformer extends SpanReceivedMetricPointTransformer
