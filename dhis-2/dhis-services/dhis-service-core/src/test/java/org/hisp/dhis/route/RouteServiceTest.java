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
package org.hisp.dhis.route;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Properties;
import java.util.Random;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.test.config.TestDhisConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RouteServiceTest {

  private String protocolUnderTest;

  @BeforeEach
  void beforeEach() {
    String[] protocols = {"http", "https"};
    protocolUnderTest = protocols[new Random().nextInt(protocols.length)];
  }

  @Test
  void testPostConstructThrowsExceptionWhenRouteRemoteServerAllowedEntryHasNonHttpProtocol() {
    Properties properties = new Properties();
    properties.setProperty(
        ConfigurationKey.ROUTE_REMOTE_SERVERS_ALLOWED.getKey(), "ftp://foo.org/");
    DhisConfigurationProvider dhisConfigurationProvider =
        new TestDhisConfigurationProvider(properties);

    RouteService routeService = new RouteService(null, null, dhisConfigurationProvider, null, null);
    assertThrows(IllegalStateException.class, routeService::postConstruct);
  }

  @Test
  void testPostConstructThrowsExceptionWhenRouteRemoteServerAllowedEntryHasUrlPath() {
    Properties properties = new Properties();
    properties.setProperty(
        ConfigurationKey.ROUTE_REMOTE_SERVERS_ALLOWED.getKey(), protocolUnderTest + "://*.org/foo");
    DhisConfigurationProvider dhisConfigurationProvider =
        new TestDhisConfigurationProvider(properties);

    RouteService routeService = new RouteService(null, null, dhisConfigurationProvider, null, null);
    assertThrows(IllegalStateException.class, routeService::postConstruct);
  }

  @Test
  void testValidateRoutePassesWhenRouteUrlProtocolIsHttpsGivenDefaultRouteRemoteServerAllowedList()
      throws ConflictException {
    Properties properties = new Properties();
    properties.setProperty(
        ConfigurationKey.ROUTE_REMOTE_SERVERS_ALLOWED.getKey(),
        ConfigurationKey.ROUTE_REMOTE_SERVERS_ALLOWED.getDefaultValue());
    DhisConfigurationProvider dhisConfigurationProvider =
        new TestDhisConfigurationProvider(properties);

    RouteService routeService = new RouteService(null, null, dhisConfigurationProvider, null, null);
    routeService.postConstruct();

    Route route = new Route();
    route.setUrl("https://stub");
    routeService.validateRoute(route);
  }

  @ParameterizedTest
  @CsvSource({
    "172.17.0.1:8080,172.17.0.1:8080/foo/**,true",
    "172.17.0.1:8080,172.17.0.1/foo/**,false",
    "172.17.0.1,172.17.0.1:8080/foo/**,false",
    "172.17.0.1,172.17.0.1/foo/**,true",
    "192.168.*.*,192.169.0.1,false",
    "*.org,stub.com,false",
    "192.168.*.*,192.168.0.1,true",
    "*.org,stub.org,true",
    ",stub,false"
  })
  void testValidateRoute(String routeRemoteServersAllowed, String routeUrl, boolean isValid)
      throws ConflictException {
    Properties properties = new Properties();
    properties.setProperty(
        ConfigurationKey.ROUTE_REMOTE_SERVERS_ALLOWED.getKey(),
        protocolUnderTest + "://" + routeRemoteServersAllowed);
    DhisConfigurationProvider dhisConfigurationProvider =
        new TestDhisConfigurationProvider(properties);

    RouteService routeService = new RouteService(null, null, dhisConfigurationProvider, null, null);
    routeService.postConstruct();

    Route route = new Route();
    route.setUrl(protocolUnderTest + "://" + routeUrl);
    if (isValid) {
      routeService.validateRoute(route);
    } else {
      assertThrows(ConflictException.class, () -> routeService.validateRoute(route));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "x-custom-header",
      "x-api-version", 
      "x-request-id",
      "custom-header",
      "my-header-123",
  })
  void testValidateResponseHeader_ValidHeaders_ShouldPass(String headerName) {
    Properties properties = new Properties();
    DhisConfigurationProvider dhisConfigurationProvider = new TestDhisConfigurationProvider(properties);
    RouteService routeService = new RouteService(null, null, dhisConfigurationProvider, null, null);

    assertDoesNotThrow(() -> routeService.validateResponseHeader(headerName));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "authorization",
      "www-authenticate",
      "proxy-authenticate", 
      "proxy-authorization",
      "set-cookie",
      "cookie",
      "x-forwarded-user",
      "x-auth-token",
      "x-api-key",
      "server",
      "x-powered-by"
  })
  void testValidateResponseHeader_DangerousHeaders_ShouldReject(String headerName) {
    Properties properties = new Properties();
    DhisConfigurationProvider dhisConfigurationProvider = new TestDhisConfigurationProvider(properties);
    RouteService routeService = new RouteService(null, null, dhisConfigurationProvider, null, null);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class, 
        () -> routeService.validateResponseHeader(headerName)
    );
    assert(exception.getMessage().contains("blacklisted for security reasons"));
  }

  @Test
  void testValidateResponseHeader_CaseInsensitiveDangerousHeaders_ShouldReject() {
    Properties properties = new Properties();
    DhisConfigurationProvider dhisConfigurationProvider = new TestDhisConfigurationProvider(properties);
    
    RouteService routeService = new RouteService(null, null, dhisConfigurationProvider, null, null);
    assertThrows(IllegalArgumentException.class, () -> routeService.validateResponseHeader("AUTHORIZATION"));
    assertThrows(IllegalArgumentException.class, () -> routeService.validateResponseHeader("Set-Cookie"));
    assertThrows(IllegalArgumentException.class, () -> routeService.validateResponseHeader("X-API-KEY"));
  }

}
