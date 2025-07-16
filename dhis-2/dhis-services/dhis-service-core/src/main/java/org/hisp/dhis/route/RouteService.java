/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.hisp.dhis.config.HibernateEncryptionConfig.AES_128_STRING_ENCRYPTOR;

import io.netty.handler.timeout.ReadTimeoutException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
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
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadGatewayException;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.TextUtils;
import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClientRequest;

/**
 * @author Morten Olav Hansen
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RouteService {
  private static final String HEADER_X_FORWARDED_USER = "X-Forwarded-User";

  private static final Pattern HTTP_OR_HTTPS_PATTERN = Pattern.compile("^(https?:).*");

  private final ApplicationContext applicationContext;

  private final RouteStore routeStore;

  private final DhisConfigurationProvider configuration;

  @Qualifier(AES_128_STRING_ENCRYPTOR)
  private final PBEStringCleanablePasswordEncryptor encryptor;

  private final ClientHttpConnector clientHttpConnector;

  private DataBufferFactory dataBufferFactory;

  private WebClient webClient;

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

  private final List<String> allowedRouteRegexRemoteServers = new ArrayList<>();

  @PostConstruct
  public void postConstruct() {
    String routeRemoteServersAllowed =
        configuration.getProperty(ConfigurationKey.ROUTE_REMOTE_SERVERS_ALLOWED).strip();
    if (!routeRemoteServersAllowed.isEmpty()) {
      for (String host : routeRemoteServersAllowed.split(",")) {
        validateHost(host);
        allowedRouteRegexRemoteServers.add(TextUtils.createRegexFromGlob(host));
      }
    }

    webClient = WebClient.builder().clientConnector(clientHttpConnector).build();
    dataBufferFactory = new DefaultDataBufferFactory();
  }

  protected void validateHost(String host) {
    if (!(HTTP_OR_HTTPS_PATTERN.matcher(host).matches())) {
      throw new IllegalStateException(
          "Allowed route URL scheme must be either http or https: " + host);
    }
    if ((host.startsWith("http:"))) {
      log.warn("Allowed route URL is insecure: {}. You should change the protocol to HTTPS", host);
    }
    if ((host.equals("https://*"))) {
      log.warn(
          "Default allowed route URL {} is vulnerable to server-side request forgery (SSRF) attacks. You should further restrict the default allowed route URL such that it contains no wildcards",
          host);
    } else if (host.contains("*")) {
      log.warn(
          "Allowed route URL is vulnerable to server-side request forgery (SSRF) attacks: {}. You should further restrict the allowed route URL such that it contains no wildcards",
          host);
    }

    URL url;
    try {
      url = new URL(host);
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
    if (org.apache.commons.lang3.StringUtils.isNotEmpty(url.getPath())) {
      throw new IllegalStateException("Allowed route URL must not have a path: " + host);
    }
  }

  /**
   * Retrieves a {@link Route} by UID or code, where the authentication secrets will be decrypted.
   * The route UID or code can be passed as route identifier. Returns null if the route does not
   * exist.
   *
   * @param id the route UID or code.
   * @return {@link Route}.
   */
  public Route getRoute(@Nonnull String id) {
    Route route = routeStore.getByUidNoAcl(id);

    if (route == null) {
      route = routeStore.getByCodeNoAcl(id);
    }

    if (route == null || route.isDisabled()) {
      return null;
    }

    return route;
  }

  protected boolean isRouteUrlAllowed(URL routeUrl) {
    String routeAddress =
        routeUrl.getProtocol()
            + "://"
            + routeUrl.getHost()
            + (routeUrl.getPort() > -1 ? ":" + routeUrl.getPort() : "");
    for (String regexRemoteServer : allowedRouteRegexRemoteServers) {
      if (routeAddress.matches(regexRemoteServer)) {
        return true;
      }
    }
    return false;
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

    if (!isRouteUrlAllowed(url)) {
      throw new ConflictException("Route URL is not permitted");
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
   * @param userDetails the {@link UserDetails} of the current user.
   * @param subPath the sub path.
   * @param request the {@link HttpServletRequest}.
   * @return an {@link ResponseEntity}.
   * @throws IOException
   * @throws BadRequestException
   */
  public ResponseEntity<ResponseBodyEmitter> execute(
      Route route, UserDetails userDetails, Optional<String> subPath, HttpServletRequest request)
      throws BadGatewayException {

    try {
      if (!isRouteUrlAllowed(new URL(route.getBaseUrl()))) {
        return new ResponseEntity<>(HttpStatusCode.valueOf(503));
      }
    } catch (MalformedURLException e) {
      log.error(e.getMessage(), e);
      throw new BadGatewayException("An unexpected error occurred");
    }

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

    applyAuthScheme(route, headers, queryParameters);
    String targetUri = createTargetUri(route, subPath, queryParameters);
    HttpMethod httpMethod =
        Objects.requireNonNullElse(HttpMethod.valueOf(request.getMethod()), HttpMethod.GET);
    WebClient.RequestHeadersSpec<?> requestHeadersSpec =
        buildRequestSpec(headers, httpMethod, targetUri, route, request);

    log.debug(
        "Sending '{}' '{}' with route '{}' ('{}')",
        httpMethod,
        targetUri,
        route.getName(),
        route.getUid());

    ResponseEntity<Flux<DataBuffer>> responseEntityFlux = retrieve(requestHeadersSpec);

    log.debug(
        "Request '{}' '{}' responded with status '{}' for route '{}' ('{}')",
        httpMethod,
        targetUri,
        responseEntityFlux.getStatusCode(),
        route.getName(),
        route.getUid());

    return new ResponseEntity<>(
        emitResponseBody(responseEntityFlux),
        filterResponseHeaders(responseEntityFlux.getHeaders()),
        responseEntityFlux.getStatusCode());
  }

  protected ResponseEntity<Flux<DataBuffer>> retrieve(
      WebClient.RequestHeadersSpec<?> requestHeadersSpec) {
    WebClient.ResponseSpec responseSpec =
        requestHeadersSpec
            .retrieve()
            .onStatus(httpStatusCode -> true, clientResponse -> Mono.empty());

    return responseSpec
        .toEntityFlux(DataBuffer.class)
        .onErrorReturn(
            throwable -> throwable.getCause() instanceof ReadTimeoutException,
            new ResponseEntity<>(HttpStatus.GATEWAY_TIMEOUT))
        .block();
  }

  protected String createTargetUri(
      Route route, Optional<String> subPath, Map<String, List<String>> queryParameters)
      throws BadGatewayException {
    UriComponentsBuilder uriComponentsBuilder =
        UriComponentsBuilder.fromHttpUrl(route.getBaseUrl());

    for (Map.Entry<String, List<String>> queryParameter : queryParameters.entrySet()) {
      uriComponentsBuilder =
          uriComponentsBuilder.queryParam(queryParameter.getKey(), queryParameter.getValue());
    }

    uriComponentsBuilder.path(getSubPath(route, subPath));

    return uriComponentsBuilder.toUriString();
  }

  protected WebClient.RequestHeadersSpec<?> buildRequestSpec(
      HttpHeaders headers,
      HttpMethod httpMethod,
      String targetUri,
      Route route,
      HttpServletRequest request)
      throws BadGatewayException {

    final Flux<DataBuffer> requestBodyFlux;
    try {
      requestBodyFlux =
          DataBufferUtils.read(
                  new InputStreamResource(request.getInputStream()), dataBufferFactory, 8192)
              .doOnNext(DataBufferUtils.releaseConsumer());
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new BadGatewayException("An error occurred while reading the upstream response");
    }

    WebClient.RequestHeadersSpec<?> requestHeadersSpec =
        webClient
            .method(httpMethod)
            .uri(targetUri)
            .httpRequest(
                clientHttpRequest -> {
                  Object nativeRequest = clientHttpRequest.getNativeRequest();
                  if (nativeRequest instanceof HttpClientRequest httpClientRequest) {
                    httpClientRequest.responseTimeout(
                        Duration.ofSeconds(route.getResponseTimeoutSeconds()));
                  }
                })
            .body(requestBodyFlux, DataBuffer.class);

    for (Map.Entry<String, List<String>> header : headers.entrySet()) {
      requestHeadersSpec =
          requestHeadersSpec.header(header.getKey(), header.getValue().toArray(new String[0]));
    }

    return requestHeadersSpec;
  }

  protected ResponseBodyEmitter emitResponseBody(
      ResponseEntity<Flux<DataBuffer>> responseEntityFlux) {
    ResponseBodyEmitter responseBodyEmitter =
        new ResponseBodyEmitter(Duration.ofMinutes(5).toMillis());

    if (responseEntityFlux.hasBody()) {
      responseEntityFlux
          .getBody()
          .subscribe(
              dataBuffer -> {
                try (DataBuffer.ByteBufferIterator byteBufferIterator =
                    dataBuffer.readableByteBuffers()) {
                  byte[] bytes;
                  ByteBuffer byteBuffer;
                  while (byteBufferIterator.hasNext()) {
                    byteBuffer = byteBufferIterator.next();
                    bytes = new byte[byteBuffer.limit()];
                    byteBuffer.get(bytes);
                    responseBodyEmitter.send(bytes);
                  }
                } catch (IOException e) {
                  log.error(e.getMessage(), e);
                  throw new RuntimeException(e);
                } finally {
                  DataBufferUtils.release(dataBuffer);
                }
              },
              responseBodyEmitter::completeWithError,
              responseBodyEmitter::complete);
    } else {
      responseBodyEmitter.complete();
    }

    return responseBodyEmitter;
  }

  protected void applyAuthScheme(
      Route route, Map<String, List<String>> headers, Map<String, List<String>> queryParameters)
      throws BadGatewayException {
    if (route.getAuth() != null) {
      try {
        route
            .getAuth()
            .decrypt(encryptor::decrypt)
            .apply(applicationContext, headers, queryParameters);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        throw new BadGatewayException("An error occurred during authentication");
      }
    }
  }

  protected String getSubPath(Route route, Optional<String> subPath) throws BadGatewayException {
    if (subPath.isPresent()) {
      if (!route.allowsSubpaths()) {
        throw new BadGatewayException(
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
