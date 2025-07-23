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
package org.hisp.dhis.external.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.external.location.DefaultLocationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class DhisConfigurationProviderTest {

  private DefaultDhisConfigurationProvider configProvider;

  @BeforeEach
  public void setup() {
    System.setProperty("dhis2.home", "src/test/resources");
    DefaultLocationManager locationManager = DefaultLocationManager.getDefault();
    locationManager.init();
    configProvider = new DefaultDhisConfigurationProvider(locationManager);
    configProvider.init();
  }

  @Test
  void isOn() {
    assertTrue(DhisConfigurationProvider.isOn("on"));
    assertTrue(DhisConfigurationProvider.isOn("ON"));
    assertTrue(DhisConfigurationProvider.isOn("true"));
    assertTrue(DhisConfigurationProvider.isOn("TRUE"));
    assertFalse(DhisConfigurationProvider.isOn("off"));
    assertFalse(DhisConfigurationProvider.isOn("OFF"));
    assertFalse(DhisConfigurationProvider.isOn("false"));
    assertFalse(DhisConfigurationProvider.isOn("FALSE"));
    assertFalse(DhisConfigurationProvider.isOn(""));
    assertFalse(DhisConfigurationProvider.isOn(null));
  }

  @Test
  void getDefault() {
    assertEquals("hikari", configProvider.getProperty(ConfigurationKey.DB_POOL_TYPE));
  }

  @Test
  void isEnabled() {
    assertFalse(configProvider.isEnabled(ConfigurationKey.REDIS_ENABLED));
    assertFalse(configProvider.isEnabled(ConfigurationKey.MONITORING_API_ENABLED));
    assertTrue(configProvider.isEnabled(ConfigurationKey.ENABLE_QUERY_LOGGING));
    assertFalse(configProvider.isEnabled(ConfigurationKey.METHOD_QUERY_LOGGING_ENABLED));
  }

  @Test
  void getIntProperty() {
    assertEquals(80, configProvider.getIntProperty(ConfigurationKey.CONNECTION_POOL_MAX_SIZE));
    assertEquals(10, configProvider.getIntProperty(ConfigurationKey.CONNECTION_POOL_MIN_SIZE));
  }

  @Test
  @DisplayName("remote servers retrieved from config should have expected values")
  void getMetaDataSyncRemoteServersAllowedTest() {
    // given there are 2 remote servers in the test config allowed list
    // when we retrieve the remote servers allowed
    List<String> remoteServersAllowed = configProvider.getMetaDataSyncRemoteServersAllowed();

    // then it should contain the expected values
    assertNotNull(remoteServersAllowed);
    assertEquals(2, remoteServersAllowed.size());
    assertTrue(
        remoteServersAllowed.containsAll(
            List.of("https://validtesturl.com/", "https://validtesturl2.com/")));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " ", "https://invalidurl.com/fail"})
  @DisplayName("invalid URLs should return false")
  void invalidUrlTest(String url) {
    // given there are 2 remote servers in the test config allowed list
    // when we check if an invalid url is in the allowed list
    boolean urlIsAllowed = configProvider.isMetaDataSyncRemoteServerAllowed(url);

    // then it should be false
    assertFalse(urlIsAllowed);
  }

  @Test
  @DisplayName("a valid url which is in the allowed list returns true")
  void validUrlInAllowedListTest() {
    // given there are 2 remote servers in the test config allowed list
    // when we check if a valid url is in the allowed list
    boolean urlIsAllowed =
        configProvider.isMetaDataSyncRemoteServerAllowed(
            "https://validtesturl.com/success/with/extra/path");

    // then it should be true
    assertTrue(urlIsAllowed);
  }
}
