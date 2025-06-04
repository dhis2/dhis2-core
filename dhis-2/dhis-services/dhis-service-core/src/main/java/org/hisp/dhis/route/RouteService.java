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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
  public static final int MAX_TOTAL_HTTP_CONNECTIONS = 500;
  public static final int DEFAULT_MAX_HTTP_CONNECTION_PER_ROUTE = 50;

  private final RouteStore routeStore;

  @Qualifier(AES_128_STRING_ENCRYPTOR)
  private final PBEStringCleanablePasswordEncryptor encryptor;

  @Autowired @Getter @Setter private RestTemplate restTemplate;

  private static final List<String> ALLOWED_REQUEST_HEADERS =
      List.of(
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

  private static final List<String> ALLOWED_RESPONSE_HEADERS =
      List.of(
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
    HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory();
    requestFactory.setConnectionRequestTimeout(1_000);
    requestFactory.setConnectTimeout(5_000);
    requestFactory.setReadTimeout(30_000);
    requestFactory.setBufferRequestBody(true);

    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(MAX_TOTAL_HTTP_CONNECTIONS);
    connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_HTTP_CONNECTION_PER_ROUTE);

    HttpClient httpClient =
        HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .disableCookieManagement()
            .build();

    requestFactory.setHttpClient(httpClient);

    restTemplate.setRequestFactory(requestFactory);
  }

  /**
   * Retrieves a {@link Route} by UID or code, decrypts its password/token and returns it.
   *
   * @param id the UID or code,
   * @return {@link Route}.
   */
  public Route getDecryptedRoute(@Nonnull String id) {
    Route route = routeStore.getByUidNoAcl(id);

    if (route == null) {
      route = routeStore.getByCodeNoAcl(id);
    }

    if (route == null || route.isDisabled()) {
      return null;
    }

    return route;
  }

  public ResponseEntity<String> exec(
      Route route,
      UserDetails currentUserDetails,
      Optional<String> subPath,
      HttpServletRequest request)
      throws IOException, BadRequestException {
    HttpHeaders headers = filterRequestHeaders(request);
    headers.forEach(
        (String name, List<String> values) ->
            log.debug(String.format("Forwarded header %s=%s", name, values.toString())));

    route.getHeaders().forEach(headers::add);

    if (currentUserDetails != null && StringUtils.hasText(currentUserDetails.getUsername())) {
      log.debug("Route accessed by user: '{}'", currentUserDetails.getUsername());
      headers.add("X-Forwarded-User", currentUserDetails.getUsername());
    }

    MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
    request.getParameterMap().forEach((key, value) -> queryParameters.addAll(key, List.of(value)));

    if (route.getAuth() != null) {
      route.getAuth().decrypt(encryptor::decrypt).apply(headers, queryParameters);
    }

    UriComponentsBuilder uriComponentsBuilder =
        UriComponentsBuilder.fromHttpUrl(route.getBaseUrl()).queryParams(queryParameters);

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
        Objects.requireNonNullElse(HttpMethod.resolve(request.getMethod()), HttpMethod.GET);
    String targetUri = uriComponentsBuilder.toUriString();

    log.info(
        "Sending {} {} via route {} ({})", httpMethod, targetUri, route.getName(), route.getUid());

    ResponseEntity<String> response =
        restTemplate.exchange(targetUri, httpMethod, entity, String.class);

    HttpHeaders responseHeaders = filterResponseHeaders(response.getHeaders());

    String responseBody = response.getBody();

    responseHeaders.forEach(
        (String name, List<String> values) ->
            log.debug(String.format("Response header %s=%s", name, values.toString())));
    log.info(
        "Request {} {} responded with HTTP status {} via route {} ({})",
        httpMethod,
        targetUri,
        response.getStatusCode(),
        route.getName(),
        route.getUid());

    return new ResponseEntity<>(responseBody, responseHeaders, response.getStatusCode());
  }

  private HttpHeaders filterHeaders(
      Iterable<String> names,
      List<String> allowedHeaders,
      Function<String, List<String>> valuesGetter) {
    HttpHeaders headers = new HttpHeaders();
    names.forEach(
        (String name) -> {
          String lowercaseName = name.toLowerCase();
          if (!allowedHeaders.contains(lowercaseName)) {
            log.debug("Blocked header: '{}'", name);
            return;
          }
          List<String> values = valuesGetter.apply(name);
          headers.addAll(name, values);
        });
    return headers;
  }

  private HttpHeaders filterRequestHeaders(HttpServletRequest request) {
    return filterHeaders(
        Collections.list(request.getHeaderNames()),
        ALLOWED_REQUEST_HEADERS,
        (String name) -> Collections.list(request.getHeaders(name)));
  }

  private HttpHeaders filterResponseHeaders(HttpHeaders responseHeaders) {
    return filterHeaders(responseHeaders.keySet(), ALLOWED_RESPONSE_HEADERS, responseHeaders::get);
  }
}
