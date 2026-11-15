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
package org.hisp.dhis.webapi.filter;

import static org.hisp.dhis.external.conf.ConfigurationKey.CROSS_ORIGIN_PROTECTION_ENABLED;
import static org.hisp.dhis.external.conf.ConfigurationKey.CROSS_ORIGIN_PROTECTION_LOG_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.render.DefaultRenderService;
import org.hisp.dhis.schema.SchemaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests for the rules of {@link CrossOriginRequestFilter}, its 403 response and its
 * configuration modes.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class CrossOriginRequestFilterTest {

  /** Origin of a request to {@code http://localhost:8080}. */
  private static final String SELF_ORIGIN = "http://localhost:8080";

  private static final String ALLOWLISTED_ORIGIN = "http://app.example.org";

  private static final String FOREIGN_ORIGIN = "http://evil.example";

  private static final List<String> UNSAFE_METHODS = List.of("POST", "PUT", "PATCH", "DELETE");

  private static final List<String> SAFE_METHODS = List.of("GET", "OPTIONS");

  private enum OriginHeader {
    ABSENT(null),
    SELF(SELF_ORIGIN),
    ALLOWLISTED(ALLOWLISTED_ORIGIN),
    FOREIGN(FOREIGN_ORIGIN),
    NULL("null");

    private final String value;

    OriginHeader(String value) {
      this.value = value;
    }
  }

  /**
   * All combinations of Origin header, method and {@code Sec-Fetch-Site} header ({@code null} means
   * absent), with the expected outcome derived from the documented rules.
   */
  static Stream<Arguments> ruleMatrix() {
    List<String> methods = new ArrayList<>(UNSAFE_METHODS);
    methods.addAll(SAFE_METHODS);
    List<String> fetchSites = Arrays.asList(null, "same-origin", "none", "same-site", "cross-site");

    List<Arguments> arguments = new ArrayList<>();
    for (OriginHeader origin : OriginHeader.values()) {
      for (String method : methods) {
        for (String fetchSite : fetchSites) {
          arguments.add(
              Arguments.of(origin, method, fetchSite, isAllowedByRules(origin, method, fetchSite)));
        }
      }
    }
    return arguments.stream();
  }

  private static boolean isAllowedByRules(OriginHeader origin, String method, String fetchSite) {
    return switch (origin) {
      case SELF, ALLOWLISTED -> true;
      case FOREIGN -> false;
      case NULL -> "same-origin".equals(fetchSite);
      case ABSENT ->
          !UNSAFE_METHODS.contains(method)
              || fetchSite == null
              || fetchSite.equals("same-origin")
              || fetchSite.equals("none");
    };
  }

  @ParameterizedTest(name = "Origin {0}, {1}, Sec-Fetch-Site {2} -> allowed {3}")
  @MethodSource("ruleMatrix")
  void appliesOriginAndFetchMetadataRules(
      OriginHeader origin, String method, String fetchSite, boolean expectedAllowed)
      throws ServletException, IOException {
    CrossOriginRequestFilter filter = filter("on", "off");
    MockHttpServletRequest request = request(method, origin.value, fetchSite);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    if (expectedAllowed) {
      assertNotNull(chain.getRequest(), "allowed request must continue down the filter chain");
      assertEquals(200, response.getStatus());
    } else {
      assertNull(chain.getRequest(), "rejected request must not reach the endpoint");
      assertEquals(403, response.getStatus());
    }
  }

  @Test
  void rejectedRequestGetsForbiddenWebMessage() throws Exception {
    CrossOriginRequestFilter filter = filter("on", "off");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request("POST", FOREIGN_ORIGIN, "cross-site"), response, new MockFilterChain());

    assertEquals(403, response.getStatus());
    assertEquals("application/json", response.getContentType());
    JsonNode body = JacksonObjectMapperConfig.jsonMapper.readTree(response.getContentAsString());
    assertEquals("Forbidden", body.get("httpStatus").asText());
    assertEquals(403, body.get("httpStatusCode").asInt());
    assertEquals("ERROR", body.get("status").asText());
    assertEquals(CrossOriginRequestFilter.REJECTED_MESSAGE, body.get("message").asText());
  }

  @Test
  void forwardedProtoDecidesSelfOrigin() throws ServletException, IOException {
    CrossOriginRequestFilter filter = filter("on", "off");
    MockHttpServletRequest request = request("POST", "https://localhost:8080", "same-origin");
    request.addHeader("X-Forwarded-Proto", "https");
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertNotNull(chain.getRequest(), "TLS-terminating proxy setting X-Forwarded-Proto must pass");
  }

  @ParameterizedTest
  @ValueSource(strings = {"true", "TRUE", "on", "ON"})
  void logOnlyModeLetsRejectedRequestThrough(String logOnly) throws ServletException, IOException {
    CrossOriginRequestFilter filter = filter("on", logOnly);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request("POST", FOREIGN_ORIGIN, "cross-site"), response, chain);

    assertNotNull(chain.getRequest(), "log-only mode must not block the request");
    assertEquals(200, response.getStatus());
  }

  @ParameterizedTest(name = "Origin null, Sec-Fetch-Site {0}")
  @MethodSource("nullOriginFetchSites")
  void logOnlyModeLetsNullOriginThrough(String fetchSite) throws ServletException, IOException {
    CrossOriginRequestFilter filter = filter("on", "on");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request("POST", "null", fetchSite), response, chain);

    assertNotNull(chain.getRequest(), "log-only mode must not block the request");
    assertEquals(200, response.getStatus());
  }

  static Stream<String> nullOriginFetchSites() {
    return Stream.of(null, "same-origin", "none", "same-site", "cross-site");
  }

  @ParameterizedTest(name = "Origin {0}, Sec-Fetch-Site same-origin -> allowed {1}")
  @MethodSource("nullOriginSpellings")
  void nullOriginSpellingsWithSameOriginFetchSite(String origin, boolean expectedAllowed)
      throws ServletException, IOException {
    CrossOriginRequestFilter filter = filter("on", "off");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request("POST", origin, "same-origin"), response, chain);

    assertEquals(expectedAllowed, chain.getRequest() != null);
    assertEquals(expectedAllowed ? 200 : 403, response.getStatus());
  }

  static Stream<Arguments> nullOriginSpellings() {
    return Stream.of(
        Arguments.of("null", true),
        Arguments.of("NULL", true),
        Arguments.of("null ", false),
        Arguments.of("nulls", false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"false", "off", "OFF"})
  void logOnlyModeOffEnforces(String logOnly) throws ServletException, IOException {
    CrossOriginRequestFilter filter = filter("on", logOnly);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request("POST", FOREIGN_ORIGIN, "cross-site"), response, chain);

    assertNull(chain.getRequest());
    assertEquals(403, response.getStatus());
  }

  @ParameterizedTest
  @ValueSource(strings = {"true", "TRUE", "on", "ON"})
  void enabledSpellingsEnforce(String enabled) throws ServletException, IOException {
    CrossOriginRequestFilter filter = filter(enabled, "off");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request("POST", FOREIGN_ORIGIN, "cross-site"), response, chain);

    assertNull(chain.getRequest());
    assertEquals(403, response.getStatus());
  }

  @ParameterizedTest
  @ValueSource(strings = {"false", "FALSE", "off", "OFF"})
  void disabledFilterLetsEveryRequestThrough(String enabled) throws ServletException, IOException {
    CrossOriginRequestFilter filter = filter(enabled, "off");
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(
        request("POST", FOREIGN_ORIGIN, "cross-site"), new MockHttpServletResponse(), chain);

    assertNotNull(chain.getRequest());
  }

  @ParameterizedTest(name = "log-only {0}: Origin {2}, Sec-Fetch-Site {3} -> {1}, {4}")
  @MethodSource("rejections")
  void rejectionIncrementsCounterForModeAndReason(
      String logOnly, String mode, String origin, String fetchSite, String reason)
      throws ServletException, IOException {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    CrossOriginRequestFilter filter = filter("on", logOnly, registry);

    filter.doFilter(
        request("POST", origin, fetchSite), new MockHttpServletResponse(), new MockFilterChain());

    assertEquals(1.0, rejectedCount(registry, mode, reason));
    assertEquals(1.0, totalRejectedCount(registry), "exactly one series must be incremented");
  }

  static Stream<Arguments> rejections() {
    List<Arguments> arguments = new ArrayList<>();
    for (String[] mode : new String[][] {{"off", "enforce"}, {"on", "log_only"}}) {
      arguments.add(
          Arguments.of(mode[0], mode[1], FOREIGN_ORIGIN, "cross-site", "origin_not_allowed"));
      arguments.add(Arguments.of(mode[0], mode[1], "null", null, "origin_not_allowed"));
      arguments.add(
          Arguments.of(mode[0], mode[1], null, "cross-site", "cross_site_without_origin"));
    }
    return arguments.stream();
  }

  @ParameterizedTest(name = "Origin {0}, {1}, Sec-Fetch-Site {2}")
  @MethodSource("allowedRequests")
  void allowedRequestDoesNotIncrementCounter(String origin, String method, String fetchSite)
      throws ServletException, IOException {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    CrossOriginRequestFilter filter = filter("on", "off", registry);

    filter.doFilter(
        request(method, origin, fetchSite), new MockHttpServletResponse(), new MockFilterChain());

    assertEquals(0.0, totalRejectedCount(registry));
  }

  static Stream<Arguments> allowedRequests() {
    return Stream.of(
        Arguments.of(SELF_ORIGIN, "POST", "same-origin"),
        Arguments.of(ALLOWLISTED_ORIGIN, "POST", "cross-site"),
        Arguments.of("null", "POST", "same-origin"),
        Arguments.of(null, "POST", null),
        Arguments.of(null, "GET", "cross-site"));
  }

  @ParameterizedTest(name = "log-only {0} -> mode {1}")
  @MethodSource("modes")
  void countersAreRegisteredAtZeroWithOnlyModeAndReasonTags(String logOnly, String mode) {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();

    filter("on", logOnly, registry);

    Collection<Counter> counters =
        registry.find(CrossOriginRequestFilter.REJECTED_COUNTER).counters();
    assertEquals(2, counters.size());
    for (Counter counter : counters) {
      assertEquals(0.0, counter.count());
      assertEquals(mode, counter.getId().getTag("mode"));
      assertEquals(
          Set.of("mode", "reason"),
          counter.getId().getTags().stream().map(Tag::getKey).collect(Collectors.toSet()),
          "tags must stay low-cardinality: never origin, path or user");
    }
    assertEquals(
        Set.of("origin_not_allowed", "cross_site_without_origin"),
        counters.stream().map(c -> c.getId().getTag("reason")).collect(Collectors.toSet()));
  }

  static Stream<Arguments> modes() {
    return Stream.of(Arguments.of("off", "enforce"), Arguments.of("on", "log_only"));
  }

  @Test
  void disabledFilterRegistersNoCounter() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();

    filter("off", "off", registry);

    assertTrue(registry.find(CrossOriginRequestFilter.REJECTED_COUNTER).counters().isEmpty());
  }

  private static double rejectedCount(MeterRegistry registry, String mode, String reason) {
    return registry
        .get(CrossOriginRequestFilter.REJECTED_COUNTER)
        .tag("mode", mode)
        .tag("reason", reason)
        .counter()
        .count();
  }

  private static double totalRejectedCount(MeterRegistry registry) {
    return registry.find(CrossOriginRequestFilter.REJECTED_COUNTER).counters().stream()
        .mapToDouble(Counter::count)
        .sum();
  }

  private static CrossOriginRequestFilter filter(String enabled, String logOnly) {
    return filter(enabled, logOnly, null);
  }

  private static CrossOriginRequestFilter filter(
      String enabled, String logOnly, MeterRegistry registry) {
    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    when(config.isEnabled(any())).thenCallRealMethod();
    when(config.getProperty(CROSS_ORIGIN_PROTECTION_ENABLED)).thenReturn(enabled);
    when(config.getProperty(CROSS_ORIGIN_PROTECTION_LOG_ONLY)).thenReturn(logOnly);

    ConfigurationService configurationService = mock(ConfigurationService.class);
    when(configurationService.isCorsWhitelisted(ALLOWLISTED_ORIGIN)).thenReturn(true);

    DefaultRenderService renderService =
        new DefaultRenderService(
            JacksonObjectMapperConfig.jsonMapper,
            JacksonObjectMapperConfig.xmlMapper,
            mock(SchemaService.class));

    return new CrossOriginRequestFilter(
        config, new AllowedOriginMatcher(configurationService), renderService, registry);
  }

  private static MockHttpServletRequest request(String method, String origin, String fetchSite) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/constants");
    request.setServerName("localhost");
    request.setServerPort(8080);
    if (origin != null) {
      request.addHeader("Origin", origin);
    }
    if (fetchSite != null) {
      request.addHeader("Sec-Fetch-Site", fetchSite);
    }
    return request;
  }
}
