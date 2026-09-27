/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.web.tomcat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.RemoteIpValve;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ForwardHeadersTest {
  @ParameterizedTest
  @CsvSource({
    ",,false",
    ",native,true",
    "none,native,false",
    "native,none,true",
    "NaTiVe,,true",
    "NONE,,false",
    ",NATIVE,true",
    "none,framework,false"
  })
  void resolvesStrategyWithPropertyPrecedence(
      String property, String environment, boolean expected) {
    assertEquals(expected, Main.useForwardHeaders(property, environment));
  }

  @ParameterizedTest
  @ValueSource(strings = {"framework", "invalid", "", " native "})
  void rejectsUnsupportedStrategies(String value) {
    assertThrows(IllegalArgumentException.class, () -> Main.useForwardHeaders(value, "native"));
    assertThrows(IllegalArgumentException.class, () -> Main.useForwardHeaders(null, value));
  }

  @Test
  void leavesPipelineUnchangedWhenDisabled() {
    Tomcat tomcat = new Tomcat();
    var pipeline = tomcat.getEngine().getPipeline();
    var valves = pipeline.getValves();
    Main.configureForwardHeaders(tomcat, false, "127\\.0\\.0\\.1");
    assertArrayEquals(valves, pipeline.getValves());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"127\\.0\\.0\\.1", "192\\.0\\.2\\.1", ""})
  void installsNativeValveWithSelectedProxyTrust(String proxies) {
    Tomcat tomcat = new Tomcat();
    Main.configureForwardHeaders(tomcat, true, proxies);
    var valves =
        Arrays.stream(tomcat.getEngine().getPipeline().getValves())
            .filter(RemoteIpValve.class::isInstance)
            .map(RemoteIpValve.class::cast)
            .toList();
    assertEquals(1, valves.size());
    RemoteIpValve valve = valves.get(0);
    assertEquals("X-Forwarded-For", valve.getRemoteIpHeader());
    assertEquals("X-Forwarded-Proto", valve.getProtocolHeader());
    assertEquals("https", valve.getProtocolHeaderHttpsValue());
    assertEquals("X-Forwarded-Port", valve.getPortHeader());
    assertEquals("X-Forwarded-Host", valve.getHostHeader());
    // Tomcat treats an empty value as no internal proxies, so no proxy is trusted.
    assertEquals(
        proxies == null
            ? new RemoteIpValve().getInternalProxies()
            : proxies.isEmpty() ? null : proxies,
        valve.getInternalProxies());
  }
}
