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
package org.hisp.dhis.webapi.security;

import jakarta.servlet.http.HttpServletRequest;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.feedback.ForbiddenException;

/**
 * Same-origin GET guard for endpoints that change server state on GET or HEAD.
 *
 * <p>A request fails the guard when all of the following hold:
 *
 * <ul>
 *   <li>the method is {@code GET} or {@code HEAD},
 *   <li>the {@code Sec-Fetch-Site} header is present and is neither {@code same-origin} nor {@code
 *       none},
 *   <li>the request has no {@code Origin} header.
 * </ul>
 *
 * <p>Browsers never send {@code Origin} on navigations or on resource loads such as images and
 * scripts, so such requests coming from another site fail the guard. A CORS-mode fetch carries
 * {@code Origin}; the CORS filter has already checked that origin against the server origin and the
 * CORS allowlist before the request gets here, so cross-origin fetches from allowlisted apps pass.
 * Clients that send no Fetch Metadata headers (Android apps, scripts, browsers on plain-HTTP
 * deployments) pass as well.
 *
 * <p>Use {@link RejectCrossOriginGet} on handler methods, or call {@link #check} for side effects
 * that depend on request parameters.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public final class CrossOriginGetGuard {
  public static final String SEC_FETCH_SITE = "Sec-Fetch-Site";

  public static final String SEC_FETCH_MODE = "Sec-Fetch-Mode";

  public static final String SEC_FETCH_DEST = "Sec-Fetch-Dest";

  public static final String ORIGIN = "Origin";

  public static final String MESSAGE =
      "Request rejected: this endpoint changes server state and does not accept GET requests from"
          + " cross-origin navigations or resource loads. Send the request from the same origin,"
          + " or use POST where the endpoint supports it.";

  private static final int MAX_LOGGED_VALUE_LENGTH = 200;

  private CrossOriginGetGuard() {
    throw new UnsupportedOperationException("util");
  }

  /**
   * @param request the current request
   * @return true when the request passes the guard, false when it is a GET or HEAD request that a
   *     browser sent as a cross-origin navigation or resource load
   */
  public static boolean isAllowed(@Nonnull HttpServletRequest request) {
    if (!isGetOrHead(request)) {
      return true;
    }
    String site = request.getHeader(SEC_FETCH_SITE);
    if (site == null || "same-origin".equals(site) || "none".equals(site)) {
      return true;
    }
    return request.getHeader(ORIGIN) != null;
  }

  /**
   * Throws when the request fails the guard and logs the rejection.
   *
   * @param request the current request
   * @throws ForbiddenException when the request fails the guard
   */
  public static void check(@Nonnull HttpServletRequest request) throws ForbiddenException {
    if (!isAllowed(request)) {
      logRejected(request);
      throw new ForbiddenException(MESSAGE);
    }
  }

  /** Logs a rejected request with the headers needed to diagnose it. */
  public static void logRejected(@Nonnull HttpServletRequest request) {
    log.warn(
        "Rejected cross-origin {} {} without Origin header: Sec-Fetch-Site={}, Sec-Fetch-Mode={},"
            + " Sec-Fetch-Dest={}",
        request.getMethod(),
        sanitize(request.getRequestURI()),
        sanitize(request.getHeader(SEC_FETCH_SITE)),
        sanitize(request.getHeader(SEC_FETCH_MODE)),
        sanitize(request.getHeader(SEC_FETCH_DEST)));
  }

  /**
   * Makes a client-supplied value safe for a single log line: replaces control characters and
   * truncates long values.
   */
  @Nonnull
  public static String sanitize(@CheckForNull String value) {
    if (value == null) {
      return "-";
    }
    String clean = value.replaceAll("\\p{Cntrl}", "_");
    return clean.length() > MAX_LOGGED_VALUE_LENGTH
        ? clean.substring(0, MAX_LOGGED_VALUE_LENGTH) + "..."
        : clean;
  }

  private static boolean isGetOrHead(HttpServletRequest request) {
    String method = request.getMethod();
    return "GET".equals(method) || "HEAD".equals(method);
  }
}
