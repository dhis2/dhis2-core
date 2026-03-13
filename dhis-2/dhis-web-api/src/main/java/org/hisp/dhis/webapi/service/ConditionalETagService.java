/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.service;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.strip;
import static org.apache.commons.lang3.StringUtils.trim;

import com.google.common.net.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.ETagVersionService;
import org.hisp.dhis.user.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service for conditional ETag caching with DML-observer-based invalidation. ETags are validated
 * before heavy computations, returning 304 Not Modified when possible.
 *
 * <p>ETag inputs include user UID, build revision, time window, all-cache version, and
 * entity-type/composite version. The raw ETag is SHA-256 hashed to prevent leaking internals.
 *
 * @author Morten Svanæs
 */
@Slf4j
@Service
public class ConditionalETagService {

  private final ETagVersionService eTagVersionService;

  /** Build revision loaded once at startup. Changes on server upgrade, invalidating all ETags. */
  private final String buildRevision;

  /** Clock for time window calculation — overridable in tests. */
  private final Clock clock;

  @Autowired
  public ConditionalETagService(ETagVersionService eTagVersionService) {
    this(eTagVersionService, Clock.systemUTC());
  }

  /** Test constructor that accepts a custom clock for deterministic TTL window testing. */
  ConditionalETagService(ETagVersionService eTagVersionService, Clock clock) {
    this.eTagVersionService = eTagVersionService;
    this.clock = clock;
    this.buildRevision = loadBuildRevision();
  }

  private static String loadBuildRevision() {
    try (var resource =
        ConditionalETagService.class.getClassLoader().getResourceAsStream("build.properties")) {
      if (resource != null) {
        Properties props = new Properties();
        props.load(resource);
        String revision = props.getProperty("build.revision", "unknown");
        log.info(
            "ETag cache: using build revision '{}' for cache invalidation on upgrade", revision);
        return revision;
      }
    } catch (IOException e) {
      log.warn("Could not load build.properties for ETag cache revision", e);
    }
    return "unknown";
  }

  /**
   * Checks if ETag caching is enabled.
   *
   * @return true if enabled, false otherwise
   */
  public boolean isEnabled() {
    return eTagVersionService.isEnabled();
  }

  /**
   * Generates an ETag for the given user using the all-cache version. The ETag format is: {@code
   * {userUid}-{buildRevision}-{timeWindow}-{allCacheVersion}}
   *
   * <p><b>Note:</b> Prefer using {@link #generateETag(UserDetails, Class)} with a specific entity
   * type for more granular cache invalidation.
   *
   * @param userDetails the current user details
   * @return the generated ETag value (without quotes)
   */
  public String generateETag(@Nonnull UserDetails userDetails) {
    String userUid = userDetails.getUid();
    long timeWindow = calculateTimeWindow();
    long allCacheVersion = eTagVersionService.getAllCacheVersion();

    return hashETag(
        String.format("%s-%s-%d-%d", userUid, buildRevision, timeWindow, allCacheVersion));
  }

  /**
   * Generates an ETag for the given user and entity type. The result is SHA-256 hashed.
   *
   * @param userDetails the current user details
   * @param entityType the entity class (e.g., OrganisationUnit.class)
   * @return the generated hashed ETag value (without quotes)
   */
  public String generateETag(@Nonnull UserDetails userDetails, @Nonnull Class<?> entityType) {
    String userUid = userDetails.getUid();
    String entityTypeName = entityType.getName();
    long timeWindow = calculateTimeWindow();
    long allCacheVersion = eTagVersionService.getAllCacheVersion();
    long entityTypeVersion = eTagVersionService.getEntityTypeVersion(entityType);

    return hashETag(
        String.format(
            "%s-%s-%s-%d-%d-%d",
            userUid,
            buildRevision,
            entityTypeName,
            timeWindow,
            allCacheVersion,
            entityTypeVersion));
  }

  /**
   * Generates a composite ETag for endpoints that depend on multiple entity types. The result is
   * SHA-256 hashed.
   *
   * @param userDetails the current user details
   * @param entityTypes the entity classes this endpoint depends on
   * @return the generated hashed ETag value (without quotes)
   */
  public String generateETag(
      @Nonnull UserDetails userDetails, @Nonnull Collection<Class<?>> entityTypes) {
    String userUid = userDetails.getUid();
    long timeWindow = calculateTimeWindow();
    long allCacheVersion = eTagVersionService.getAllCacheVersion();
    // Sort by class name for deterministic iteration order.
    StringJoiner versionParts = new StringJoiner(".");
    entityTypes.stream()
        .sorted(Comparator.comparing(Class::getName))
        .forEach(
            type ->
                versionParts.add(
                    type.getName() + "=" + eTagVersionService.getEntityTypeVersion(type)));
    return hashETag(
        String.format(
            "%s-%s-c-%d-%d-%s", userUid, buildRevision, timeWindow, allCacheVersion, versionParts));
  }

  /** Hashes the raw ETag value using SHA-256. */
  private static String hashETag(String rawETag) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawETag.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  /**
   * Calculates the current time window by dividing epoch millis by the TTL duration. All servers
   * with synced clocks compute the same bucket, ensuring consistent ETags in clustered deployments.
   */
  private long calculateTimeWindow() {
    int ttlMinutes = eTagVersionService.getTtlMinutes();
    long ttlMillis = TimeUnit.MINUTES.toMillis(ttlMinutes);
    return clock.millis() / ttlMillis;
  }

  /**
   * Checks if the request's If-None-Match header matches the current ETag. If it matches, the
   * cached response is still valid.
   *
   * @param request the HTTP request
   * @param currentETag the current ETag value (without quotes)
   * @return true if the cached response is still valid (304 should be returned)
   */
  public boolean checkNotModified(
      @Nonnull HttpServletRequest request, @Nonnull String currentETag) {
    String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);

    if (isBlank(ifNoneMatch)) {
      return false;
    }

    String strippedIfNoneMatch = stripETagValue(ifNoneMatch);
    return currentETag.equals(strippedIfNoneMatch);
  }

  /**
   * Strips ETag formatting (W/ prefix and quotes) from the header value.
   *
   * @param value the header value
   * @return the stripped value
   */
  private String stripETagValue(String value) {
    value = removeStart(trim(value), "W/");
    value = strip(value, "\"");
    return value;
  }

  /**
   * Returns a {@link ResponseEntity} with conditional ETag caching using the global version.
   *
   * <p><b>Note:</b> Prefer using {@link #withConditionalETagCaching(UserDetails,
   * HttpServletRequest, Class, Supplier)} with a specific entity type for more granular cache
   * invalidation.
   *
   * @param <T> the response body type
   * @param userDetails the current user details
   * @param request the HTTP request
   * @param bodySupplier the supplier for the response body (only called if ETag doesn't match)
   * @return a ResponseEntity with either 304 Not Modified or 200 OK with the body
   */
  public <T> ResponseEntity<T> withConditionalETagCaching(
      @Nonnull UserDetails userDetails,
      @Nonnull HttpServletRequest request,
      @Nonnull Supplier<T> bodySupplier) {

    if (!isEnabled()) {
      return ResponseEntity.ok().body(bodySupplier.get());
    }

    String currentETag = generateETag(userDetails);
    return executeWithETag(userDetails, request, currentETag, bodySupplier);
  }

  /**
   * Returns a {@link ResponseEntity} with conditional ETag caching for a specific entity type. If
   * the request's ETag matches the current ETag, returns 304 Not Modified without executing the
   * body supplier. Otherwise, executes the body supplier and returns 200 OK with the new ETag.
   *
   * <p>This provides granular cache invalidation - changes to one entity type won't invalidate
   * caches for other entity types.
   *
   * @param <T> the response body type
   * @param userDetails the current user details
   * @param request the HTTP request
   * @param entityType the entity class for granular versioning (e.g., OrganisationUnit.class)
   * @param bodySupplier the supplier for the response body (only called if ETag doesn't match)
   * @return a ResponseEntity with either 304 Not Modified or 200 OK with the body
   */
  public <T> ResponseEntity<T> withConditionalETagCaching(
      @Nonnull UserDetails userDetails,
      @Nonnull HttpServletRequest request,
      @Nonnull Class<?> entityType,
      @Nonnull Supplier<T> bodySupplier) {

    if (!isEnabled()) {
      return ResponseEntity.ok().body(bodySupplier.get());
    }

    String currentETag = generateETag(userDetails, entityType);
    return executeWithETag(userDetails, request, currentETag, bodySupplier);
  }

  private <T> ResponseEntity<T> executeWithETag(
      UserDetails userDetails,
      HttpServletRequest request,
      String currentETag,
      Supplier<T> bodySupplier) {

    if (checkNotModified(request, currentETag)) {
      log.debug("ETag match - returning 304 Not Modified for user {}", userDetails.getUid());
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
          .cacheControl(CacheControl.noCache().cachePrivate().mustRevalidate())
          .eTag(quote(currentETag))
          .header(HttpHeaders.VARY, "Cookie", "Authorization")
          .build();
    }

    log.debug("ETag mismatch - executing body supplier for user {}", userDetails.getUid());
    T body = bodySupplier.get();

    return ResponseEntity.ok()
        .cacheControl(CacheControl.noCache().cachePrivate().mustRevalidate())
        .eTag(quote(currentETag))
        .header(HttpHeaders.VARY, "Cookie", "Authorization")
        .body(body);
  }

  /**
   * Checks if the request should return 304 Not Modified using the global version.
   *
   * <p><b>Note:</b> Prefer using {@link #checkNotModifiedResponse(UserDetails, HttpServletRequest,
   * Class)} with a specific entity type for more granular cache invalidation.
   *
   * @param <T> the response body type
   * @param userDetails the current user details
   * @param request the HTTP request
   * @return Optional containing 304 response if ETag matches, empty Optional otherwise
   */
  public <T> java.util.Optional<ResponseEntity<T>> checkNotModifiedResponse(
      @Nonnull UserDetails userDetails, @Nonnull HttpServletRequest request) {

    if (!isEnabled()) {
      return java.util.Optional.empty();
    }

    String currentETag = generateETag(userDetails);
    return checkNotModifiedWithETag(userDetails, request, currentETag);
  }

  /**
   * Checks if the request should return 304 Not Modified for a specific entity type. If so, returns
   * an Optional containing the 304 response. Otherwise, returns empty Optional and the caller
   * should proceed with computing the response.
   *
   * <p>This provides granular cache invalidation - changes to one entity type won't invalidate
   * caches for other entity types.
   *
   * @param <T> the response body type
   * @param userDetails the current user details
   * @param request the HTTP request
   * @param entityType the entity class for granular versioning (e.g., OrganisationUnit.class)
   * @return Optional containing 304 response if ETag matches, empty Optional otherwise
   */
  public <T> java.util.Optional<ResponseEntity<T>> checkNotModifiedResponse(
      @Nonnull UserDetails userDetails,
      @Nonnull HttpServletRequest request,
      @Nonnull Class<?> entityType) {

    if (!isEnabled()) {
      return java.util.Optional.empty();
    }

    String currentETag = generateETag(userDetails, entityType);
    return checkNotModifiedWithETag(userDetails, request, currentETag);
  }

  private <T> java.util.Optional<ResponseEntity<T>> checkNotModifiedWithETag(
      UserDetails userDetails, HttpServletRequest request, String currentETag) {
    if (checkNotModified(request, currentETag)) {
      log.debug("ETag match - returning 304 Not Modified for user {}", userDetails.getUid());
      ResponseEntity<T> response =
          ResponseEntity.status(HttpStatus.NOT_MODIFIED)
              .cacheControl(CacheControl.noCache().cachePrivate().mustRevalidate())
              .eTag(quote(currentETag))
              .header(HttpHeaders.VARY, "Cookie", "Authorization")
              .build();
      return java.util.Optional.of(response);
    }

    return java.util.Optional.empty();
  }

  /**
   * Sets ETag headers on an existing ResponseEntity builder using the global version.
   *
   * <p><b>Note:</b> Prefer using {@link #setETagHeaders(UserDetails, HttpServletResponse, Class)}
   * with a specific entity type for more granular cache invalidation.
   *
   * @param userDetails the current user details
   * @param response the HTTP response to set headers on
   */
  public void setETagHeaders(
      @Nonnull UserDetails userDetails, @Nonnull HttpServletResponse response) {
    if (!isEnabled()) {
      return;
    }

    String currentETag = generateETag(userDetails);
    setETagHeaders(response, currentETag);
  }

  /**
   * Sets ETag headers on an existing ResponseEntity builder for a specific entity type. Use this
   * when building streaming responses where you need to set headers before the body is streamed.
   *
   * <p>This provides granular cache invalidation - changes to one entity type won't invalidate
   * caches for other entity types.
   *
   * @param userDetails the current user details
   * @param response the HTTP response to set headers on
   * @param entityType the entity class for granular versioning (e.g., OrganisationUnit.class)
   */
  public void setETagHeaders(
      @Nonnull UserDetails userDetails,
      @Nonnull HttpServletResponse response,
      @Nonnull Class<?> entityType) {
    if (!isEnabled()) {
      return;
    }

    String currentETag = generateETag(userDetails, entityType);
    setETagHeaders(response, currentETag);
  }

  /**
   * Sets ETag headers on response for a composite endpoint that depends on multiple entity types.
   * Use this when building streaming responses for composite endpoints.
   *
   * @param userDetails the current user details
   * @param response the HTTP response to set headers on
   * @param entityTypes the entity classes this endpoint depends on
   */
  public void setETagHeaders(
      @Nonnull UserDetails userDetails,
      @Nonnull HttpServletResponse response,
      @Nonnull Collection<Class<?>> entityTypes) {
    if (!isEnabled()) {
      return;
    }
    String currentETag = generateETag(userDetails, entityTypes);
    setETagHeaders(response, currentETag);
  }

  /**
   * Sets ETag headers on response using an already computed ETag value. Use this when the ETag was
   * determined earlier in the request lifecycle and must be reused consistently.
   *
   * @param response the HTTP response to set headers on
   * @param currentETag the already computed ETag value (without quotes)
   */
  public void setETagHeaders(@Nonnull HttpServletResponse response, @Nonnull String currentETag) {
    response.setHeader(HttpHeaders.ETAG, quote(currentETag));
    response.setHeader(HttpHeaders.VARY, "Cookie, Authorization");

    Collection<String> headers = response.getHeaders(HttpHeaders.CACHE_CONTROL);
    if (headers.isEmpty()) {
      response.setHeader(
          HttpHeaders.CACHE_CONTROL,
          CacheControl.noCache().cachePrivate().mustRevalidate().getHeaderValue());
    } else {
      log.warn("Cache-Control header already set: {}", String.join(",", headers));
    }
  }

  /**
   * Quotes the ETag value for the HTTP header.
   *
   * @param etag the ETag value
   * @return the quoted ETag value
   */
  private String quote(String etag) {
    return "\"" + etag + "\"";
  }
}
