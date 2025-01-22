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
 * A simple utility class to build Prometheus text format metrics. Note that there is no validation
 * of the input, so the user should make sure that the input is correct. The Prometheus text format
 * is documented here: https://prometheus.io/docs/instrumenting/exposition_formats/
 *
 * @author Jason P. Pickering
 */
public class PrometheusTextBuilder {

  private StringBuilder metrics = new StringBuilder();

  public void helpLine(String metricName, String help) {
    metrics.append("# HELP ").append(metricName).append(" ").append(help).append("%n");
  }

  public void typeLine(String metricName, String type) {
    metrics.append("# TYPE ").append(metricName).append(" ").append(type).append("%n");
  }

  public void updateMetricsFromMap(
      Map<?, ?> map, String metricName, String keyName, String help, String type) {
    helpLine(metricName, help);
    typeLine(metricName, type);
    map.forEach(
        (key, value) ->
            metrics.append("%s{%s=\"%s\" } %s%n".formatted(metricName, keyName, key, value)));
  }

  public void appendStaticKeyValue(String metricName, String key, String value) {
    if (value != null) {
      metrics
          .append(metricName)
          .append("{key=\"")
          .append(key)
          .append("\", value=\"")
          .append(value)
          .append("\"} 1%n");
    }
  }

  public String getMetrics() {
    return metrics.toString();
  }
}
