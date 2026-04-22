/*
 * Copyright (c) 2004-2022, University of Oslo
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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * Subclass of {@link org.springframework.web.filter.ShallowEtagHeaderFilter} which skips ETag
 * generation for URIs matching {@link #ENDPOINTS}.
 *
 * <p>Registered as a Spring bean and wired into the servlet filter chain via a {@code
 * DelegatingFilterProxy} in {@code DhisWebApiWebAppInitializer}, mapped to {@code /api/*}.
 *
 * <p>Requests whose URI matches {@link #ENDPOINTS} bypass the underlying {@link
 * ShallowEtagHeaderFilter} and proceed down the chain without response buffering or ETag
 * computation. This is used to skip endpoints that stream large payloads (e.g. file/image resources
 * and data value sets) where the cost of buffering the response to compute an MD5 outweighs the
 * benefit.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>{@code GET /api/dataElementGroups} — <em>not</em> excluded; response gets an ETag.
 *   <li>{@code GET /api/dataValues} — excluded; no ETag header added.
 *   <li>{@code GET /api/41/fileResources/abc123} — excluded (the {@code (\d{2}/)?} segment matches
 *       the optional API version prefix).
 *   <li>{@code GET /api/tracker/events/RkV9CZzmV2E/dataValues/q33Wv8jNvFA/file} — excluded.
 * </ul>
 *
 * @author Lars Helge Overland
 * @author Halvdan Hoem Grelland
 */
@Slf4j
@Component
public class ExcludableShallowEtagHeaderFilter extends ShallowEtagHeaderFilter {
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
          + "/(file|image)";

  private static final Pattern EXCLUDE_PATTERN = Pattern.compile(ENDPOINTS);

  @PostConstruct
  void logConfiguration() {
    log.debug(
        "ExcludableShallowEtagHeaderFilter registered; URIs matching '{}' will bypass ETag generation",
        ENDPOINTS);
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
