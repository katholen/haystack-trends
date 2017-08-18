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
package com.expedia.www.haystack.metricpoints.serde

import java.util

import com.expedia.open.tracing.Span
import com.expedia.www.haystack.metricpoints.metrics.MetricsSupport
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}

object SpanSerde extends Serde[Span] with MetricsSupport {

  private val spanSerdeMeter = metricRegistry.meter("span.serde.failure")

  override def configure(configs: util.Map[String, _], b: Boolean): Unit = ()

  override def close(): Unit = ()


  def serializer: Serializer[Span] = {
    new Serializer[Span] {
      override def configure(configs: util.Map[String, _], b: Boolean): Unit = ()

      override def close(): Unit = ()

      override def serialize(topic: String, obj: Span): Array[Byte] = if(obj != null) obj.toByteArray else null
    }
  }

  def deserializer: Deserializer[Span] = {
    new Deserializer[Span] {
      override def configure(configs: util.Map[String, _], b: Boolean): Unit = ()

      override def close(): Unit = ()

      override def deserialize(topic: String, data: Array[Byte]): Span = performDeserialize(data)

      /**
        * converts the binary protobuf bytes into Span object
        * @param data serialized bytes of Span
        * @return
        */
      private def performDeserialize(data: Array[Byte]): Span = {
        try {
          if(data == null || data.length == 0) null else Span.parseFrom(data)
        } catch {
          case _: Exception =>
            /* may be log and add metric */
            spanSerdeMeter.mark()
            null
        }
      }
    }
  }
}