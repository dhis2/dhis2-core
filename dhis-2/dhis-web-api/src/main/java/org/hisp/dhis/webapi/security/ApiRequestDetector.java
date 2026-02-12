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
package org.hisp.dhis.webapi.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Detects whether an HTTP request originates from an API client (SPA, mobile app, script) as
 * opposed to a browser navigation. Used by authentication entry points to decide between returning
 * a 401 JSON response or redirecting to the login page.
 */
public class ApiRequestDetector {

  private ApiRequestDetector() {}

  /**
   * Returns {@code true} if the request appears to come from an API client rather than a browser
   * navigating to a page.
   *
   * <p>Detection heuristics (any match returns true):
   *
   * <ol>
   *   <li>{@code X-Requested-With: XMLHttpRequest} header (jQuery-era convention, backward compat)
   *   <li>Request path starts with {@code /api/} or equals {@code /api} (most robust signal)
   *   <li>{@code Accept} header contains {@code application/json} or {@code application/xml} but
   *       does NOT contain {@code text/html} (catches non-/api/ JSON consumers without false
   *       positives from browser navigation which sends {@code Accept: text/html,...})
   * </ol>
   */
  public static boolean isApiRequest(HttpServletRequest request) {
    // 1. Legacy XMLHttpRequest check (backward compat)
    if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
      return true;
    }

    // 2. Path-based detection — the most reliable signal for /api/ calls
    String path = request.getRequestURI().substring(request.getContextPath().length());
    if (path.equals("/api") || path.startsWith("/api/")) {
      return true;
    }

    // 3. Accept header detection — catches non-/api/ JSON/XML consumers
    String accept = request.getHeader("Accept");
    if (accept != null) {
      boolean wantsStructuredData =
          accept.contains("application/json") || accept.contains("application/xml");
      boolean wantsHtml = accept.contains("text/html");
      if (wantsStructuredData && !wantsHtml) {
        return true;
      }
    }

    return false;
  }
}
