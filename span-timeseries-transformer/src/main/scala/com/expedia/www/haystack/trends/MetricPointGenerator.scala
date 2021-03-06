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
package com.expedia.www.haystack.trends

import com.expedia.open.tracing.Span
import com.expedia.www.haystack.commons.entities.MetricPoint
import com.expedia.www.haystack.commons.metrics.MetricsSupport
import com.expedia.www.haystack.trends.exceptions.SpanValidationException
import com.expedia.www.haystack.trends.transformer.MetricPointTransformer

import scala.util.{Failure, Success, Try}

trait MetricPointGenerator extends MetricsSupport {

  private val SpanValidationErrors = metricRegistry.meter("span.validation.failure")
  private val metricPointGenerationTimer = metricRegistry.timer("metricpoint.generation.time")

  /**
    * This function is responsible for generating all the metric points which can be created given a span
    *
    * @param transformers list of transformers to be applied
    * @param blacklistedServices list of services which are blacklisted - typically the ones which have a lot of operation names and can result in many unique trends
    * @param span         incoming span
    * @return try of either a list of generated metric points or a validation exception
    */
  def generateMetricPoints(blacklistedServices: List[String])(transformers: Seq[MetricPointTransformer])(span: Span, serviceOnlyFlag: Boolean): Try[List[MetricPoint]] = {
    val context = metricPointGenerationTimer.time()
    val metricPoints = {
      validate(blacklistedServices)(span).map { validatedSpan =>
        transformers.flatMap(transformer => transformer.mapSpan(validatedSpan, serviceOnlyFlag)).toList
      }
    }
    context.close()
    metricPoints
  }

  /**
    * This function validates a span and makes sure that the span has the necessary data to generate meaningful metrics
    * This layer is supposed to do generic validations which would impact all the transformers.
    * Validation specific to the transformer can be done in the transformer itself
    *
    * @param span incoming span
    * @return Try object which should return either the span as is or a validation exception
    */
  private def validate(blackListedServices: List[String])(span: Span): Try[Span] = {
    if (span.getServiceName.isEmpty || span.getOperationName.isEmpty || blackListedServices.contains(span.getServiceName)) {
      SpanValidationErrors.mark()
      Failure(new SpanValidationException)
    } else {
      Success(span)
    }

  }

}





