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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the PrometheusTextBuilder class.
 *
 * @author Jason P. Pickering
 */
class PrometheusTextBuilderTest {
  @Test
  void getMetricsReturnsEmptyStringWhenNoMetrics() {
    PrometheusTextBuilder builder = new PrometheusTextBuilder();
    assertEquals("", builder.getMetrics());
  }

  @Test
  void helpLineAppendsHelpText() {
    PrometheusTextBuilder builder = new PrometheusTextBuilder();
    builder.addHelp("test_metric", "This is a test metric");
    assertEquals("# HELP test_metric This is a test metric\n", builder.getMetrics());
  }

  @Test
  void typeLineAppendsTypeText() {
    PrometheusTextBuilder builder = new PrometheusTextBuilder();
    builder.addType("test_metric", "counter");
    assertEquals("# TYPE test_metric counter\n", builder.getMetrics());
  }

  @Test
  void updateMetricsFromMapAppendsMetrics() {
    PrometheusTextBuilder builder = new PrometheusTextBuilder();
    Map<String, Integer> map = Map.of("key1", 1);
    builder.addMetrics(map, "test_metric", "key", "Test help", "gauge");
    String expected =
        """
        # HELP test_metric Test help
        # TYPE test_metric gauge
        test_metric{key="key1"} 1
        """;
    assertEquals(expected, builder.getMetrics());
  }
}
