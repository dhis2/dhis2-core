/*
 * Copyright (c) 2004-2025, University of Oslo
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

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.http.server.PathContainer;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * OpenEntityManagerInViewFilter that excludes specific URL patterns from having an open Hibernate
 * session throughout the request lifecycle.
 *
 * <p>Open Session In View (OSIV) keeps a Hibernate EntityManager and database connection open for
 * the entire HTTP request duration, from controller entry through view rendering and response
 * serialization. This allows lazy-loaded entity relationships to be accessed anywhere in the
 * request processing chain, even outside explicit transaction boundaries.
 *
 * <p><b>OSIV should NOT be relied upon</b> due to:
 *
 * <ul>
 *   <li><b>Long connection hold times:</b> Database connections are held for the entire request
 *       duration, even when no longer needed for data access (e.g., during JSON serialization)
 *   <li><b>Connection pool exhaustion:</b> With limited connection pools, long-running requests can
 *       block all connections, causing timeouts and degraded performance. Each concurrent request
 *       holds a connection, limiting scalability and concurrent user capacity
 *   <li><b>Unclear transaction boundaries:</b> Code outside {@code @Transactional} methods can
 *       still trigger database queries, making it unclear where data access occurs
 * </ul>
 *
 * <h2>Current DHIS2 Dependencies on OSIV</h2>
 *
 * <p><b>Controllers extending AbstractFullReadOnlyController rely on OSIV</b> because their field
 * filtering and JSON serialization access lazy-loaded collections on detached entities. Removing
 * OSIV causes {@code LazyInitializationException} for these endpoints.
 *
 * <p><b>New code should avoid OSIV dependency:</b> Use DTOs, explicit JOIN FETCH queries, or
 * projections to load required data within transaction boundaries instead of relying on OSIV.
 *
 * <h2>Excluded Endpoints</h2>
 *
 * <p>This filter excludes specific endpoints that don't require database access or manage their own
 * transactions explicitly, improving performance by avoiding unnecessary connection usage.
 */
public class ConditionalOpenEntityManagerInViewFilter extends OpenEntityManagerInViewFilter {

  private static final PathPatternParser PARSER = new PathPatternParser();
  private static final List<PathPattern> EXCLUDE_PATTERNS =
      Stream.of(
              "/api/tracker/**",
              "/api/ping",
              "/api/metrics",
              "/api/system/ping",
              "/api/potentialDuplicates")
          .map(PARSER::parse)
          .toList();

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // Strip context path to get application-relative path for pattern matching
    String path = request.getRequestURI().substring(request.getContextPath().length());
    PathContainer pathContainer = PathContainer.parsePath(path);

    for (PathPattern pattern : EXCLUDE_PATTERNS) {
      if (pattern.matches(pathContainer)) {
        return true;
      }
    }
    return false;
  }
}
