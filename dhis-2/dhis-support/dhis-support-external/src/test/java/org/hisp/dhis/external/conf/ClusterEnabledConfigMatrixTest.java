/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.external.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;
import java.util.stream.Stream;
import org.hisp.dhis.external.location.DefaultLocationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Exercises the real {@link DefaultDhisConfigurationProvider#isClusterEnabled()} AND of {@code
 * cluster.members} and {@code cluster.hostname}. Half-configured clusters must not report enabled
 * (they also fail open for the ETag multi-node gate unless redis invalidation is on).
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ClusterEnabledConfigMatrixTest {

  private DefaultDhisConfigurationProvider config;

  @BeforeEach
  void setUp() {
    System.setProperty("dhis2.home", "src/test/resources");
    DefaultLocationManager locationManager = DefaultLocationManager.getDefault();
    locationManager.init();
    config = new DefaultDhisConfigurationProvider(locationManager);
    config.init();
    // Clear any cluster keys that may exist in the shared test dhis.conf.
    Properties props = config.getProperties();
    props.remove(ConfigurationKey.CLUSTER_MEMBERS.getKey());
    props.remove(ConfigurationKey.CLUSTER_HOSTNAME.getKey());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("clusterMatrix")
  @DisplayName("isClusterEnabled requires both members and hostname non-blank")
  void isClusterEnabledMatrix(String caseName, String members, String hostname, boolean expected) {
    Properties props = config.getProperties();
    applyKey(props, ConfigurationKey.CLUSTER_MEMBERS.getKey(), members);
    applyKey(props, ConfigurationKey.CLUSTER_HOSTNAME.getKey(), hostname);

    assertEquals(expected, config.isClusterEnabled(), caseName);
  }

  /**
   * @param members null = key absent; "" = blank value; non-empty = set
   * @param hostname null = key absent; "" = blank value; non-empty = set
   */
  static Stream<Arguments> clusterMatrix() {
    return Stream.of(
        Arguments.of("neither (keys missing)", null, null, false),
        Arguments.of("neither (keys blank)", "", "", false),
        Arguments.of("members-only (hostname missing)", "node1:4001", null, false),
        Arguments.of("members-only (hostname blank)", "node1:4001", "", false),
        Arguments.of("hostname-only (members missing)", null, "node1", false),
        Arguments.of("hostname-only (members blank)", "", "node1", false),
        Arguments.of("members blank + hostname set", "  ", "node1", false),
        Arguments.of("members set + hostname whitespace", "node1:4001", "  ", false),
        Arguments.of("both set", "node1:4001", "node1", true),
        Arguments.of("both set with multiple members", "n1:4001,n2:4001", "n1", true));
  }

  private static void applyKey(Properties props, String key, String value) {
    if (value == null) {
      props.remove(key);
    } else {
      props.setProperty(key, value);
    }
  }
}
