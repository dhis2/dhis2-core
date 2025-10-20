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
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.AuditableEntity;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.user.User;
import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Morten Olav Hansen
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RouteService {
  private static final String HEADER_X_FORWARDED_USER = "X-Forwarded-User";

  protected static final int MAX_TOTAL_HTTP_CONNECTIONS = 500;
  protected static final int DEFAULT_MAX_HTTP_CONNECTION_PER_ROUTE = 50;

  private final RouteStore routeStore;

  @Qualifier(AES_128_STRING_ENCRYPTOR)
  private final PBEStringCleanablePasswordEncryptor encryptor;

  @Getter @Setter private CloseableHttpClient httpClient;

  private final AuditManager auditManager;

  protected static final Set<String> ALLOWED_REQUEST_HEADERS =
      Set.of(
          "accept",
          "accept-encoding",
          "accept-language",
          "x-requested-with",
          "user-agent",
          "cache-control",
          "content-type",
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
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(MAX_TOTAL_HTTP_CONNECTIONS);
    connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_HTTP_CONNECTION_PER_ROUTE);

    httpClient =
        HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .disableCookieManagement()
            .build();
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

  public void validateRoute(Route route) throws ConflictException {
    URL url;
    try {
      url = new URL(route.getUrl());
    } catch (MalformedURLException e) {
      throw new ConflictException("Malformed route URL");
    }

    if (!(url.getProtocol().equalsIgnoreCase("http")
        || url.getProtocol().equalsIgnoreCase("https"))) {
      throw new ConflictException("Route URL scheme must be either http or https");
    }

    if (route.getResponseTimeoutSeconds() < 1 || route.getResponseTimeoutSeconds() > 60) {
      throw new ConflictException(
          "Route response timeout must be greater than 0 seconds and less than or equal to 60 seconds");
    }
  }

  /**
   * Executes the given route and returns the response from the target API.
   *
   * @param route the {@link Route}.
   * @param user the {@link User} of the current user.
   * @param subPath the sub path.
   * @param request the {@link HttpServletRequest}.
   * @return an {@link ResponseEntity}.
   * @throws IOException
   * @throws BadRequestException
   */
  public ResponseEntity<byte[]> execute(
      Route route, User user, Optional<String> subPath, HttpServletRequest request)
      throws IOException, BadRequestException, ServletException {

    HttpHeaders headers = filterRequestHeaders(request);
    route.getHeaders().forEach(headers::add);
    addForwardedUserHeader(user, headers);

    MultiValueMap<String, String> queryParameters = getQueryParams(request);
    if (route.getAuth() != null) {
      route.getAuth().decrypt(encryptor::decrypt).apply(headers, queryParameters);
    }
    HttpMethod httpMethod =
        Objects.requireNonNullElse(HttpMethod.resolve(request.getMethod()), HttpMethod.GET);

    UriComponentsBuilder uriComponentsBuilder = createRequestPathBuilder(route, subPath);
    String upstreamUrlWithoutQueryParams = uriComponentsBuilder.build().toUriString();
    String upstreamUrl = createRequestUrl(uriComponentsBuilder.cloneBuilder(), queryParameters);

    log.debug(
        "Sending '{}' '{}' with route '{}' ('{}')",
        httpMethod,
        upstreamUrlWithoutQueryParams,
        route.getName(),
        route.getUid());

    RestTemplate restTemplate = newRestTemplate(route);

    ResponseEntity<byte[]> response;
    try {
      if (request instanceof MultipartHttpServletRequest) {
        response = postMultipartBody(upstreamUrl, headers, request, restTemplate);
      } else {
        response = exchange(upstreamUrl, headers, httpMethod, request, restTemplate);
      }
    } catch (RestClientResponseException e) {
      response =
          new ResponseEntity<>(
              e.getResponseBodyAsByteArray(), e.getResponseHeaders(), e.getRawStatusCode());
    }

    audit(user, route, httpMethod, upstreamUrlWithoutQueryParams, response);

    HttpHeaders responseHeaders = filterResponseHeaders(response.getHeaders());
    responseHeaders.forEach(
        (String name, List<String> values) ->
            log.debug("Response header {}={}", name, values.toString()));

    log.info(
        "Request {} {} responded with HTTP status {} via route {} ({})",
        httpMethod,
        upstreamUrlWithoutQueryParams,
        response.getStatusCode(),
        route.getName(),
        route.getUid());

    return new ResponseEntity<>(response.getBody(), responseHeaders, response.getStatusCode());
  }

  protected ResponseEntity<byte[]> postMultipartBody(
      String upstreamUrl,
      HttpHeaders headers,
      HttpServletRequest request,
      RestTemplate restTemplate)
      throws ServletException, IOException {
    MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
    for (Part part : request.getParts()) {
      multipartBodyBuilder.part(part.getName(), part.getInputStream().readAllBytes());
    }

    for (Map.Entry<String, MultipartFile> file :
        ((MultipartHttpServletRequest) request).getFileMap().entrySet()) {
      MultipartBodyBuilder.PartBuilder partBuilder =
          multipartBodyBuilder.part(file.getKey(), file.getValue().getResource());
      if (file.getValue().getContentType() != null) {
        partBuilder.contentType(MediaType.valueOf(file.getValue().getContentType()));
      }
      if (file.getValue().getOriginalFilename() != null) {
        partBuilder.filename(file.getValue().getOriginalFilename());
      }
    }
    MultiValueMap<String, HttpEntity<?>> multipartBody = multipartBodyBuilder.build();
    HttpEntity<MultiValueMap<String, HttpEntity<?>>> httpEntity =
        new HttpEntity<>(multipartBody, headers);

    return restTemplate.postForEntity(upstreamUrl, httpEntity, byte[].class);
  }

  protected ResponseEntity<byte[]> exchange(
      String upstreamUrl,
      HttpHeaders headers,
      HttpMethod httpMethod,
      HttpServletRequest request,
      RestTemplate restTemplate)
      throws IOException {
    HttpEntity<InputStreamResource> entity =
        new HttpEntity<>(new InputStreamResource(request.getInputStream()), headers);
    return restTemplate.exchange(upstreamUrl, httpMethod, entity, byte[].class);
  }

  protected void audit(
      User user,
      Route route,
      HttpMethod httpMethod,
      String upstreamUrl,
      ResponseEntity<byte[]> response) {
    Audit.AuditBuilder auditBuilder =
        Audit.builder()
            .auditScope(AuditScope.API)
            .createdBy(user.getUsername())
            .auditType(AuditType.SECURITY)
            .data("");

    RouteRunApiAuditEntry auditEntry = new RouteRunApiAuditEntry();
    auditEntry.setSource("Route Run");
    auditEntry.setRouteId(route.getUid());
    auditEntry.setHttpMethod(httpMethod.name());
    auditEntry.setUpstreamUrl(upstreamUrl);

    if (response.getStatusCode().isError()) {
      auditEntry.setSuccessful(false);

      AuditableEntity auditableEntity =
          new AuditableEntity(RouteRunApiAuditEntry.class, auditEntry);

      Audit audit =
          auditBuilder
              .createdAt(LocalDateTime.now())
              .attributes(
                  auditManager.collectAuditAttributes(auditEntry, RouteRunApiAuditEntry.class))
              .auditableEntity(auditableEntity)
              .build();
      auditManager.send(audit);
    } else {
      auditEntry.setSuccessful(true);

      AuditableEntity auditableEntity =
          new AuditableEntity(RouteRunApiAuditEntry.class, auditEntry);

      Audit audit =
          auditBuilder
              .createdAt(LocalDateTime.now())
              .attributes(
                  auditManager.collectAuditAttributes(auditEntry, RouteRunApiAuditEntry.class))
              .auditableEntity(auditableEntity)
              .build();
      auditManager.send(audit);
    }
  }

  protected UriComponentsBuilder createRequestPathBuilder(Route route, Optional<String> subPath)
      throws BadRequestException {
    UriComponentsBuilder uriComponentsBuilder =
        UriComponentsBuilder.fromUriString(route.getBaseUrl());
    uriComponentsBuilder.path(getSubPath(route, subPath));

    return uriComponentsBuilder;
  }

  protected String createRequestUrl(
      UriComponentsBuilder uriComponentsBuilder, Map<String, List<String>> queryParameters) {
    for (Map.Entry<String, List<String>> queryParameter : queryParameters.entrySet()) {
      uriComponentsBuilder =
          uriComponentsBuilder.queryParam(queryParameter.getKey(), queryParameter.getValue());
    }

    return uriComponentsBuilder.build().toUriString();
  }

  protected RestTemplate newRestTemplate(Route route) {
    HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory();
    requestFactory.setConnectionRequestTimeout(1_000);
    requestFactory.setConnectTimeout(5_000);
    requestFactory.setReadTimeout(
        (int) Duration.of(route.getResponseTimeoutSeconds(), ChronoUnit.SECONDS).toMillis());
    requestFactory.setBufferRequestBody(true);
    requestFactory.setHttpClient(httpClient);

    return new RestTemplate(requestFactory);
  }

  protected MultiValueMap<String, String> getQueryParams(HttpServletRequest request) {
    if (request.getQueryString() != null) {
      return UriComponentsBuilder.fromUriString("?" + request.getQueryString())
          .build()
          .getQueryParams();
    } else {
      return new LinkedMultiValueMap<>();
    }
  }

  protected String getSubPath(Route route, Optional<String> subPath) throws BadRequestException {
    if (subPath.isPresent()) {
      if (!route.allowsSubpaths()) {
        throw new BadRequestException(
            String.format("Route '%s' does not allow sub-paths", route.getId()));
      }
      return subPath.get();
    } else {
      return "";
    }
  }

  /**
   * Adds the user as an HTTP header, if it exists.
   *
   * @param user the {@link User} of the current user.
   * @param headers the {@link HttpHeaders}.
   */
  private void addForwardedUserHeader(User user, HttpHeaders headers) {
    if (user != null && StringUtils.hasText(user.getUsername())) {
      log.debug("Route accessed by user: '{}'", user.getUsername());
      headers.add(HEADER_X_FORWARDED_USER, user.getUsername());
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
