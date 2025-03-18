/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
  public static final String GAUGE = "gauge";

  public void addHelp(String metricName, String help) {
    metrics.append(String.format("# HELP %s %s%n", metricName, help));
  }

  /**
   * Appends a Prometheus metric type line to the metrics.
   *
   * @param metricName the name of the metric
   */
  public void addType(String metricName) {
    addType(metricName, GAUGE);
  }

  /**
   * Appends a Prometheus metric type line to the metrics.
   *
   * @param metricName the name of the metric
   * @param type the type of the metric (e.g., counter, gauge)
   */
  public void addType(String metricName, String type) {
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
   */
  public void addMetrics(Map<?, ?> map, String metricName, String keyName, String help) {
    addMetrics(map, metricName, keyName, help, GAUGE);
  }

  /**
   * Transform a Map<String, ?> into a Prometheus text format metric. Note that the key is assumed
   * to be a string, and the value should be a number which is capable of being converted to a
   * string. The type of the metric is assumed to be a gauge by default.
   *
   * @param map the map containing the metrics data
   * @param metricName the name of the metric
   * @param keyName the name of the key in the metric
   * @param help the help text for the metric
   * @param type the type of the metric
   */
  public void addMetrics(
      Map<?, ?> map, String metricName, String keyName, String help, String type) {
    addHelp(metricName, help);
    addType(metricName, type);
    map.forEach(
        (key, value) ->
            metrics.append("%s{%s=\"%s\"} %s%n".formatted(metricName, keyName, key, value)));
  }

  /**
   * Transform a Map<String, ?> into a Prometheus text format metric. This method allows for the
   * specification of a label map, which will be used to generate the labels for the metric. Note
   * that the key is assumed to be a string, and the value should be a complete value string which
   * is capable of being imported into a Prometheus time series.
   *
   * @param map the map containing the metrics data
   * @param metricName the name of the metric
   * @param labelMap the map containing the labels
   * @param help the help text for the metric
   * @param type the type of the metric
   */
  public void updateMetricsWithLabelsMap(
      Map<?, ?> map, String metricName, Map<String, String> labelMap, String help, String type) {
    addHelp(metricName, help);
    addType(metricName, type);
    map.forEach(
        (key, value) -> {
          String thisLabel = labelMap.get(key);
          if (thisLabel == null) {
            return;
          }
          metrics.append("%s{%s} %s%n".formatted(metricName, thisLabel, value));
        });
  }

  /**
   * Returns the Prometheus metrics as a string.
   *
   * @return the metrics in Prometheus text format
   */
  public String getMetrics() {
    return metrics.toString();
  }

  /**
   * Appends a formatted string to the Prometheus metrics. This is not checked for correctness, so
   * be sure you know what you are doing before using this method.
   *
   * @param format the formatted string to append
   */
  public void append(String format) {
    metrics.append(format);
  }
}
