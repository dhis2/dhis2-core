/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.webapi.utils;

import java.util.Map;

/**
 * A simple utility class to build Prometheus text format metrics. The Prometheus text format is
 * documented here: https://prometheus.io/docs/instrumenting/exposition_formats/
 *
 * @author Jason P. Pickering
 */
public class PrometheusTextBuilder {

  private StringBuilder metrics = new StringBuilder();

  public void helpLine(String metricName, String help) {
    metrics.append(String.format("# HELP %s %s%n", metricName, help));
  }

  /**
   * Appends a Prometheus metric type line to the metrics.
   *
   * @param metricName the name of the metric
   * @param type the type of the metric (e.g., counter, gauge)
   */
  public void typeLine(String metricName, String type) {
    metrics.append(String.format("# TYPE %s %s%n", metricName, type));
  }

  /**
   * Transform a Map<String, ?> into a Prometheus text format metric. Note that the key is assumed
   * to be a string, and the value should be a number which is capable of being converted to a
   * string.
   *
   * @param map the map containing the metrics data
   * @param metricName the name of the metric
   * @param keyName the name of the key in the metric
   * @param help the help text for the metric
   * @param type the type of the metric
   */
  public void updateMetricsFromMap(
      Map<?, ?> map, String metricName, String keyName, String help, String type) {
    helpLine(metricName, help);
    typeLine(metricName, type);
    map.forEach(
        (key, value) ->
            metrics.append("%s{%s=\"%s\"} %s%n".formatted(metricName, keyName, key, value)));
  }

  /**
   * Appends a static key-value pair to the Prometheus metrics. This is most useful for representing
   * labels in the Prometheus text format such as build time, version, etc. This will produce a
   * metric with a static value of 1.
   *
   * @param metricName the name of the metric
   * @param key the key for the metric
   * @param value the value for the metric
   */
  public void appendStaticKeyValue(String metricName, String key, String value) {
    if (value != null) {
      metrics.append(String.format("%s{key=\"%s\", value=\"%s\"} 1%n", metricName, key, value));
    }
  }

  /**
   * Returns the Prometheus metrics as a string.
   *
   * @return the metrics in Prometheus text format
   */
  public String getMetrics() {
    return metrics.toString();
  }
}
