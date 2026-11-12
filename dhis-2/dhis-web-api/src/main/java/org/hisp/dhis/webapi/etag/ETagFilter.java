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
package org.hisp.dhis.webapi.etag;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * The servlet filter of the API ETag mechanism, mapped to {@code /api/*} in {@code
 * DhisWebApiWebAppInitializer}. It works in two tiers:
 *
 * <ol>
 *   <li>Smart ETags: when {@code ETagConditionalInterceptor} covers a request, the response carries
 *       the version-based ETag before its body is written. Spring's response wrapper then streams
 *       the body without buffering, and the smart ETag stands (a smart 304 has no body).
 *   <li>Body-hash fallback: every other GET that answers 2xx without {@code no-store} gets an MD5
 *       ETag of its body and a 304 for a matching {@code If-None-Match}. This covers endpoints the
 *       interceptor does not map, deep {@code fields=} requests, controllers that bypass the
 *       response advice, and every request while the smart tier is off (for example when DHIS2
 *       clustering or Redis cache invalidation forces it off).
 * </ol>
 *
 * <p>Requests whose URI matches {@link #ENDPOINTS} bypass both tiers' buffering and get no
 * body-hash ETag: they stream large payloads (file and image resources, data value sets, metadata
 * version snapshots) where buffering costs more than the ETag saves.
 *
 * <p>Replaces {@code ExcludableShallowEtagHeaderFilter} with the same exclusions.
 *
 * @author Lars Helge Overland
 * @author Halvdan Hoem Grelland
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Component(ETagFilter.BEAN_NAME)
public class ETagFilter extends ShallowEtagHeaderFilter {

  /** Bean name used by the {@code DelegatingFilterProxy} registration. */
  public static final String BEAN_NAME = "etagFilter";

  private static final String UID_REGEXP = "[a-zA-Z][a-zA-Z0-9]{10}";

  private static final String ENDPOINTS =
      "/api/(\\d{2}/)?dataValueSets|"
          + "/api/(\\d{2}/)?dataValues|"
          + "/api/(\\d{2}/)?fileResources|"
          + "/api/(\\d{2}/)?dataEntry/metadata|"
          + "/api/(\\d{2}/)?tracker/events/"
          + UID_REGEXP
          + "/dataValues/"
          + UID_REGEXP
          + "/(file|image)|"
          + "/api/(\\d{2}/)?tracker/trackedEntities/"
          + UID_REGEXP
          + "/attributes/"
          + UID_REGEXP
          + "/(file|image)|"
          // Metadata version snapshots are not suitable for ETag buffering.
          // Ideally, versions are only requested once per remote client.
          + "/api/(\\d{2}/)?metadata/version/.+/data(\\.gz)?";

  private static final Pattern EXCLUDE_PATTERN = Pattern.compile(ENDPOINTS);

  @PostConstruct
  void logConfiguration() {
    log.debug("ETagFilter registered; URIs matching '{}' bypass ETag buffering", ENDPOINTS);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (EXCLUDE_PATTERN.matcher(request.getRequestURI()).find()) {
      filterChain.doFilter(request, response);
    } else {
      super.doFilterInternal(request, response, filterChain);
    }
  }
}
