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
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.ETagService;
import org.hisp.dhis.user.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
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

  private final ETagService eTagVersionService;

  /** Build revision loaded once at startup. Changes on server upgrade, invalidating all ETags. */
  private final String buildRevision;

  /** Clock for time window calculation — overridable in tests. */
  private final Clock clock;

  @Autowired
  public ConditionalETagService(ETagService eTagVersionService) {
    this(eTagVersionService, Clock.systemUTC());
  }

  /** Test constructor that accepts a custom clock for deterministic TTL window testing. */
  ConditionalETagService(ETagService eTagVersionService, Clock clock) {
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

  /**
   * Generates an ETag for a named-key endpoint. Named-key endpoints may depend on a mix of entity
   * types and non-entity named version keys (e.g. {@code "installedApps"}).
   *
   * @param userDetails the current user details
   * @param entityTypes entity classes this endpoint depends on (may be empty)
   * @param namedKeys non-entity version key names this endpoint depends on (may be empty)
   * @return the generated hashed ETag value (without quotes)
   */
  public String generateETag(
      @Nonnull UserDetails userDetails,
      @Nonnull Collection<Class<?>> entityTypes,
      @Nonnull Collection<String> namedKeys) {
    String userUid = userDetails.getUid();
    long timeWindow = calculateTimeWindow();
    long allCacheVersion = eTagVersionService.getAllCacheVersion();

    StringJoiner versionParts = new StringJoiner(".");
    entityTypes.stream()
        .sorted(Comparator.comparing(Class::getName))
        .forEach(
            type ->
                versionParts.add(
                    type.getName() + "=" + eTagVersionService.getEntityTypeVersion(type)));
    namedKeys.stream()
        .sorted()
        .forEach(key -> versionParts.add(key + "=" + eTagVersionService.getNamedVersion(key)));

    return hashETag(
        String.format(
            "%s-%s-n-%d-%d-%s", userUid, buildRevision, timeWindow, allCacheVersion, versionParts));
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

  private CacheControl buildCacheControl() {
    int staleSeconds = eTagVersionService.getStaleWhileRevalidateSeconds();
    CacheControl cc = CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate();
    if (staleSeconds > 0) {
      cc = cc.staleWhileRevalidate(staleSeconds, TimeUnit.SECONDS);
    }
    return cc;
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
   * Sets ETag headers on response using an already computed ETag value. Use this when the ETag was
   * determined earlier in the request lifecycle and must be reused consistently.
   *
   * @param response the HTTP response to set headers on
   * @param currentETag the already computed ETag value (without quotes)
   */
  public void setETagHeaders(@Nonnull HttpServletResponse response, @Nonnull String currentETag) {
    response.setHeader(HttpHeaders.ETAG, quote(currentETag));
    response.setHeader(HttpHeaders.VARY, "Cookie, Authorization");

    String cacheControlValue = buildCacheControl().getHeaderValue();
    Collection<String> existing = response.getHeaders(HttpHeaders.CACHE_CONTROL);
    if (!existing.isEmpty()) {
      log.debug(
          "Overwriting pre-existing Cache-Control header: {} -> {}",
          String.join(",", existing),
          cacheControlValue);
    }
    response.setHeader(HttpHeaders.CACHE_CONTROL, cacheControlValue);
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
