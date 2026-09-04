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
package org.hisp.dhis.webapi.mvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.tracker.export.timeout.Deadline;
import org.hisp.dhis.tracker.export.timeout.DeadlineHolder;
import org.hisp.dhis.tracker.export.timeout.TrackerExportTimeout;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Gives every tracker export request a {@link Deadline} so its queries are bounded by {@code
 * tracker.export.timeout}.
 *
 * <p>Scoped by {@link #PATH_PATTERNS} and GET so no other product ever gets a deadline. Both are
 * needed: the paths exclude {@code /api/tracker/ownership} and the import job endpoints, GET
 * excludes the note POSTs which live in {@code TrackerImportController} under the same prefixes.
 *
 * <p>Cleared in {@link #afterCompletion} whatever the outcome, since Tomcat reuses worker threads.
 */
@Component
@RequiredArgsConstructor
public class TrackerExportDeadlineInterceptor implements HandlerInterceptor {

  /** The six export controllers' request mappings. */
  public static final List<String> PATH_PATTERNS =
      List.of(
          "/api/tracker/trackedEntities/**",
          "/api/tracker/enrollments/**",
          "/api/tracker/events/**",
          "/api/tracker/trackerEvents/**",
          "/api/tracker/singleEvents/**",
          "/api/tracker/relationships/**");

  private final TrackerExportTimeout timeout;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!HttpMethod.GET.matches(request.getMethod())) {
      return true;
    }

    // null when the timeout is disabled, which leaves the request unbounded
    DeadlineHolder.set(timeout.newDeadline());
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    DeadlineHolder.clear();
  }
}
