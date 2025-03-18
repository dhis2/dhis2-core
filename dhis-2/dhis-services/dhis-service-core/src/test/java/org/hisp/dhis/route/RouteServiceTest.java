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
package org.hisp.dhis.route;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Properties;
import java.util.Random;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.test.config.TestDhisConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

  @Test
  void testValidateRouteFailsWhenRouteUrlProtocolIsHttpsGivenEmptyRouteRemoteServerAllowedList() {
    Properties properties = new Properties();
    properties.setProperty(ConfigurationKey.ROUTE_REMOTE_SERVERS_ALLOWED.getKey(), "");
    DhisConfigurationProvider dhisConfigurationProvider =
        new TestDhisConfigurationProvider(properties);

    RouteService routeService = new RouteService(null, null, dhisConfigurationProvider, null, null);
    routeService.postConstruct();

    Route route = new Route();
    route.setUrl("https://stub");
    assertThrows(ConflictException.class, () -> routeService.validateRoute(route));
  }

  @Test
  void testValidateRouteFailsWhenRouteUrlProtocolIsHttpGivenEmptyRouteRemoteServerAllowedList() {
    Properties properties = new Properties();
    properties.setProperty(ConfigurationKey.ROUTE_REMOTE_SERVERS_ALLOWED.getKey(), "");
    DhisConfigurationProvider dhisConfigurationProvider =
        new TestDhisConfigurationProvider(properties);

    RouteService routeService = new RouteService(null, null, dhisConfigurationProvider, null, null);
    routeService.postConstruct();

    Route route = new Route();
    route.setUrl("http://stub");
    assertThrows(ConflictException.class, () -> routeService.validateRoute(route));
  }

  @Test
  void
      testValidateRoutePassesWhenRouteUrlMatchesGivenRouteRemoteServerAllowedWildcardDomainNameEntry()
          throws ConflictException {
    Properties properties = new Properties();
    properties.setProperty(
        ConfigurationKey.ROUTE_REMOTE_SERVERS_ALLOWED.getKey(), protocolUnderTest + "://*.org");
    DhisConfigurationProvider dhisConfigurationProvider =
        new TestDhisConfigurationProvider(properties);

    RouteService routeService = new RouteService(null, null, dhisConfigurationProvider, null, null);
    routeService.postConstruct();

    Route route = new Route();
    route.setUrl(protocolUnderTest + "://stub.org");
    routeService.validateRoute(route);
  }

  @Test
  void
      testValidateRoutePassesWhenRouteUrlMatchesGivenRouteRemoteServerAllowedWildcardIpAddressEntry()
          throws ConflictException {
    Properties properties = new Properties();
    properties.setProperty(
        ConfigurationKey.ROUTE_REMOTE_SERVERS_ALLOWED.getKey(),
        protocolUnderTest + "://192.168.*.*");
    DhisConfigurationProvider dhisConfigurationProvider =
        new TestDhisConfigurationProvider(properties);

    RouteService routeService = new RouteService(null, null, dhisConfigurationProvider, null, null);
    routeService.postConstruct();

    Route route = new Route();
    route.setUrl(protocolUnderTest + "://192.168.0.1");
    routeService.validateRoute(route);
  }

  @Test
  void
      testValidateRouteFailsWhenRouteUrlDoesNotMatchGivenRouteRemoteServerAllowedWildcardDomainNameEntry() {
    Properties properties = new Properties();
    properties.setProperty(
        ConfigurationKey.ROUTE_REMOTE_SERVERS_ALLOWED.getKey(), protocolUnderTest + "://*.org");
    DhisConfigurationProvider dhisConfigurationProvider =
        new TestDhisConfigurationProvider(properties);

    RouteService routeService = new RouteService(null, null, dhisConfigurationProvider, null, null);
    routeService.postConstruct();

    Route route = new Route();
    route.setUrl(protocolUnderTest + "://stub.com");
    assertThrows(ConflictException.class, () -> routeService.validateRoute(route));
  }

  @Test
  void
      testValidateRouteFailsWhenRouteUrlDoesNotMatchGivenRouteRemoteServerAllowedWildcardIpAddressEntry() {
    Properties properties = new Properties();
    properties.setProperty(
        ConfigurationKey.ROUTE_REMOTE_SERVERS_ALLOWED.getKey(),
        protocolUnderTest + "://192.168.*.*");
    DhisConfigurationProvider dhisConfigurationProvider =
        new TestDhisConfigurationProvider(properties);

    RouteService routeService = new RouteService(null, null, dhisConfigurationProvider, null, null);
    routeService.postConstruct();

    Route route = new Route();
    route.setUrl(protocolUnderTest + "://192.169.0.1");
    assertThrows(ConflictException.class, () -> routeService.validateRoute(route));
  }
}
