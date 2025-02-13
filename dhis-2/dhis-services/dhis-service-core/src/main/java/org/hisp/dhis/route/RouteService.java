/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.hisp.dhis.config.HibernateEncryptionConfig.AES_128_STRING_ENCRYPTOR;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.hibernate.Hibernate;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.user.UserDetails;
import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Morten Olav Hansen
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RouteService {
  private static final String HEADER_X_FORWARDED_USER = "X-Forwarded-User";

  private final RouteStore routeStore;

  @Qualifier(AES_128_STRING_ENCRYPTOR)
  private final PBEStringCleanablePasswordEncryptor encryptor;

  @Autowired @Getter @Setter private RestTemplate restTemplate;

  private static final Set<String> ALLOWED_REQUEST_HEADERS =
      Set.of(
          "accept",
          "accept-encoding",
          "accept-language",
          "x-requested-with",
          "user-agent",
          "cache-control",
          "if-match",
          "if-modified-since",
          "if-none-match",
          "if-range",
          "if-unmodified-since",
          "x-forwarded-for",
          "x-forwarded-host",
          "x-forwarded-port",
          "x-forwarded-proto",
          "x-forwarded-prefix",
          "forwarded");

  private static final Set<String> ALLOWED_RESPONSE_HEADERS =
      Set.of(
          "content-encoding",
          "content-language",
          "content-length",
          "content-type",
          "expires",
          "cache-control",
          "last-modified",
          "etag");

  @PostConstruct
  public void postConstruct() {
    // Connect timeout
    ConnectionConfig connectionConfig =
        ConnectionConfig.custom().setConnectTimeout(Timeout.ofMilliseconds(5_000)).build();

    // Socket timeout
    SocketConfig socketConfig =
        SocketConfig.custom().setSoTimeout(Timeout.ofMilliseconds(10_000)).build();

    // Connection request timeout
    RequestConfig requestConfig =
        RequestConfig.custom().setConnectionRequestTimeout(Timeout.ofMilliseconds(1_000)).build();

    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setDefaultSocketConfig(socketConfig);
    connectionManager.setDefaultConnectionConfig(connectionConfig);

    org.apache.hc.client5.http.classic.HttpClient httpClient =
        org.apache.hc.client5.http.impl.classic.HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build();

    restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
  }

  /**
   * Retrieves a {@link Route} by UID or code, where the authentication secrets will be decrypted.
   * The route UID or code can be passed as route identifier. Returns null if the route does not
   * exist.
   *
   * @param id the route UID or code.
   * @return {@link Route}.
   */
  public Route getRouteWithDecryptedAuth(@Nonnull String id) {
    Route route = routeStore.getByUidNoAcl(id);

    if (route == null) {
      route = routeStore.getByCodeNoAcl(id);
    }

    if (route == null || route.isDisabled()) {
      return null;
    }

    // prevents Hibernate from persisting updates made to the route object
    route = Hibernate.unproxy(route, Route.class);

    if (route.getAuth() != null) {
      route.setAuth(route.getAuth().decrypt(encryptor::decrypt));
    }

    return route;
  }

  /**
   * Executes the given route and returns the response from the target API.
   *
   * @param route the {@link Route}.
   * @param userDetails the {@link UserDetails} of the current user.
   * @param subPath the sub path.
   * @param request the {@link HttpServletRequest}.
   * @return an {@link ResponseEntity}.
   * @throws IOException
   * @throws BadRequestException
   */
  public ResponseEntity<String> execute(
      Route route, UserDetails userDetails, Optional<String> subPath, HttpServletRequest request)
      throws IOException, BadRequestException {

    HttpHeaders headers = filterRequestHeaders(request);
    route.getHeaders().forEach(headers::add);
    addForwardedUserHeader(userDetails, headers);

    Map<String, List<String>> queryParameters = new HashMap<>();
    request
        .getParameterMap()
        .forEach(
            (key, value) ->
                queryParameters
                    .computeIfAbsent(key, k -> new LinkedList<>())
                    .addAll(Arrays.asList(value)));

    if (route.getAuth() != null) {
      route.getAuth().apply(headers, queryParameters);
    }

    UriComponentsBuilder uriComponentsBuilder =
        UriComponentsBuilder.fromHttpUrl(route.getBaseUrl());

    for (Map.Entry<String, List<String>> queryParameter : queryParameters.entrySet()) {
      uriComponentsBuilder =
          uriComponentsBuilder.queryParam(queryParameter.getKey(), queryParameter.getValue());
    }

    if (subPath.isPresent()) {
      if (!route.allowsSubpaths()) {
        throw new BadRequestException(
            String.format("Route '%s' does not allow sub-paths", route.getId()));
      }
      uriComponentsBuilder.path(subPath.get());
    }

    String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

    HttpEntity<String> entity = new HttpEntity<>(body, headers);

    HttpMethod httpMethod =
        Objects.requireNonNullElse(HttpMethod.valueOf(request.getMethod()), HttpMethod.GET);
    String targetUri = uriComponentsBuilder.toUriString();

    log.info(
        "Sending '{}' '{}' with route '{}' ('{}')",
        httpMethod,
        targetUri,
        route.getName(),
        route.getUid());

    ResponseEntity<String> response =
        restTemplate.exchange(targetUri, httpMethod, entity, String.class);

    HttpHeaders responseHeaders = filterResponseHeaders(response.getHeaders());

    String responseBody = response.getBody();

    log.info(
        "Request '{}' '{}' responded with status '{}' for route '{}' ('{}')",
        httpMethod,
        targetUri,
        response.getStatusCode(),
        route.getName(),
        route.getUid());

    return new ResponseEntity<>(responseBody, responseHeaders, response.getStatusCode());
  }

  /**
   * Adds the user as an HTTP header, if it exists.
   *
   * @param userDetails the {@link UserDetails} of the current user.
   * @param headers the {@link HttpHeaders}.
   */
  private void addForwardedUserHeader(UserDetails userDetails, HttpHeaders headers) {
    if (userDetails != null && StringUtils.hasText(userDetails.getUsername())) {
      log.debug("Route accessed by user: '{}'", userDetails.getUsername());
      headers.add(HEADER_X_FORWARDED_USER, userDetails.getUsername());
    }
  }

  /**
   * Returns the allowed HTTP headers only for the given request.
   *
   * @param request the {@link HttpServletRequest}.
   * @return an {@link HttpHeaders}.
   */
  private HttpHeaders filterRequestHeaders(HttpServletRequest request) {
    return filterHeaders(
        Collections.list(request.getHeaderNames()),
        ALLOWED_REQUEST_HEADERS,
        (String name) -> Collections.list(request.getHeaders(name)));
  }

  /**
   * Returns the allowed HTTP headers only for the given response headers.
   *
   * @param responseHeaders the {@link HttpHeaders}.
   * @return an {@link HttpHeaders}.
   */
  private HttpHeaders filterResponseHeaders(HttpHeaders responseHeaders) {
    return filterHeaders(responseHeaders.keySet(), ALLOWED_RESPONSE_HEADERS, responseHeaders::get);
  }

  /**
   * Filters the given HTTP headers.
   *
   * @param names the header names.
   * @param allowedHeaders the allowed headers.
   * @param valueGetter the function for retrieving the value for a header name.
   * @return an {@link HttpHeaders}.
   */
  private HttpHeaders filterHeaders(
      Iterable<String> names,
      Collection<String> allowedHeaders,
      Function<String, List<String>> valueGetter) {
    HttpHeaders headers = new HttpHeaders();
    names.forEach(
        (String name) -> {
          String lowercaseName = name.toLowerCase();
          if (!allowedHeaders.contains(lowercaseName)) {
            log.debug("Blocked header: '{}'", name);
            return;
          }
          List<String> values = valueGetter.apply(name);
          headers.addAll(name, values);
        });
    return headers;
  }
}
