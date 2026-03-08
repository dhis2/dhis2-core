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
package org.hisp.dhis.webapi.mvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.webapi.service.ConditionalETagService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interceptor that provides automatic conditional ETag caching for metadata API endpoints.
 *
 * <p>Cacheable endpoints are determined by schema lookup: if the URL path maps to a metadata entity
 * type via {@link SchemaService}, the endpoint gets per-entity-type ETag caching.
 *
 * <p>Non-schema endpoints (e.g. {@code /api/me}, {@code /api/configuration}) are handled via a
 * static composite endpoint map that defines their entity-type dependencies.
 *
 * @author Morten Svanæs
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConditionalETagInterceptor implements HandlerInterceptor {

  private static final String ETAG_ATTR = ConditionalETagInterceptor.class.getName() + ".etag";
  private static final String ENTITY_TYPE_ATTR =
      ConditionalETagInterceptor.class.getName() + ".entityType";

  private static final String COMPOSITE_TYPES_ATTR =
      ConditionalETagInterceptor.class.getName() + ".compositeTypes";

  /**
   * Non-CRUD endpoints mapped to the entity types they depend on. When any of these entity types
   * has its version bumped, the endpoint's composite ETag changes, invalidating the cache.
   */
  private static final Map<String, Set<Class<?>>> COMPOSITE_ENDPOINTS =
      Map.of(
          "me",
          Set.of(
              User.class,
              UserRole.class,
              UserGroup.class,
              OrganisationUnit.class,
              DataApprovalLevel.class),
          "configuration",
          Set.of(Configuration.class));

  /** Version-prefix pattern: matches /api/41/ or /api/ */
  private static final Pattern PATH_PATTERN = Pattern.compile("/api/(?:\\d{2}/)?([\\w]+)");

  private final ConditionalETagService conditionalETagService;
  private final SchemaService schemaService;

  @Override
  public boolean preHandle(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull Object handler) {

    if (!conditionalETagService.isEnabled()) {
      return true;
    }

    String method = request.getMethod();
    if (!"GET".equals(method) && !"HEAD".equals(method)) {
      return true;
    }

    if (!CurrentUserUtil.hasCurrentUser()) {
      return true;
    }

    String resourceName = extractResourceName(request.getRequestURI());
    if (resourceName == null) {
      return true;
    }

    UserDetails userDetails = CurrentUserUtil.getCurrentUserDetails();

    // Check composite endpoint map first (non-CRUD endpoints with multi-type dependencies)
    Set<Class<?>> compositeTypes = COMPOSITE_ENDPOINTS.get(resourceName);
    if (compositeTypes != null) {
      String currentETag = conditionalETagService.generateETag(userDetails, compositeTypes);
      if (conditionalETagService.checkNotModified(request, currentETag)) {
        log.debug("ETag match for composite endpoint {} - returning 304", request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        conditionalETagService.setETagHeaders(userDetails, response, compositeTypes);
        return false;
      }
      request.setAttribute(ETAG_ATTR, currentETag);
      request.setAttribute(COMPOSITE_TYPES_ATTR, compositeTypes);
      return true;
    }

    // Only metadata endpoints backed by a Schema get ETag caching.
    Schema schema = schemaService.getSchemaByPluralName(resourceName);
    if (schema == null || !schema.isMetadata()) {
      return true;
    }

    Class<?> entityType = schema.getKlass();
    String currentETag = conditionalETagService.generateETag(userDetails, entityType);

    // Check If-None-Match
    if (conditionalETagService.checkNotModified(request, currentETag)) {
      log.debug("ETag match for {} - returning 304", request.getRequestURI());
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      conditionalETagService.setETagHeaders(userDetails, response, entityType);
      return false;
    }

    // Store ETag for postHandle to set headers
    request.setAttribute(ETAG_ATTR, currentETag);
    request.setAttribute(ENTITY_TYPE_ATTR, entityType);
    return true;
  }

  @Override
  public void postHandle(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull Object handler,
      ModelAndView modelAndView) {

    String currentETag = (String) request.getAttribute(ETAG_ATTR);
    if (currentETag == null) {
      return;
    }

    if (!response.isCommitted() && isSuccessStatus(response.getStatus())) {
      UserDetails userDetails = CurrentUserUtil.getCurrentUserDetails();
      @SuppressWarnings("unchecked")
      Collection<Class<?>> compositeTypes =
          (Collection<Class<?>>) request.getAttribute(COMPOSITE_TYPES_ATTR);
      if (compositeTypes != null) {
        conditionalETagService.setETagHeaders(userDetails, response, compositeTypes);
      } else {
        Class<?> entityType = (Class<?>) request.getAttribute(ENTITY_TYPE_ATTR);
        if (entityType != null) {
          conditionalETagService.setETagHeaders(userDetails, response, entityType);
        }
      }
    }
  }

  /**
   * Extracts the resource name (first path segment after /api/ and optional version prefix). e.g.
   * "/api/41/organisationUnits/abc123" → "organisationUnits", "/api/me" → "me"
   */
  static String extractResourceName(String uri) {
    if (uri == null) return null;
    Matcher matcher = PATH_PATTERN.matcher(uri);
    return matcher.find() ? matcher.group(1) : null;
  }

  private static boolean isSuccessStatus(int status) {
    return status >= 200 && status < 300;
  }

  /** Visible for testing. Returns the composite entity types for a resource name, or null. */
  static Set<Class<?>> getCompositeEndpointTypes(String resourceName) {
    return COMPOSITE_ENDPOINTS.get(resourceName);
  }
}
