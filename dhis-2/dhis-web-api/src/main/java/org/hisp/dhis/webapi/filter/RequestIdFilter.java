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

import static org.hisp.dhis.log.MdcKeys.MDC_REQUEST_ID;
import static org.hisp.dhis.log.MdcKeys.MDC_X_REQUEST_ID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that captures the X-Request-ID header and adds it to the Mapped Diagnostic Context (MDC)
 * for logging. Access via {@code %X{requestId}} in log4j2 pattern layouts.
 *
 * @see <a href="https://logback.qos.ch/manual/mdc.html">MDC Documentation</a>
 * @see <a
 *     href="https://logging.apache.org/log4j/2.x/manual/pattern-layout.html#converter-thread-context-map">Pattern
 *     Layout Thread Context Map</a>
 */
@Component("requestIdFilter")
public class RequestIdFilter extends OncePerRequestFilter {

  /** Pattern for valid request IDs: alphanumeric, dash, and underscore, 1-36 characters. */
  private static final Pattern VALID_REQUEST_ID_PATTERN = Pattern.compile("[-_a-zA-Z0-9]{1,36}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String headerValue = request.getHeader("X-Request-ID");
      if (headerValue != null) {
        String sanitized = sanitizeRequestId(headerValue);
        MDC.put(MDC_REQUEST_ID, sanitized);
        MDC.put(MDC_X_REQUEST_ID, sanitized);
      }
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_REQUEST_ID);
      MDC.remove(MDC_X_REQUEST_ID);
    }
  }

  /**
   * Since the request ID is a user provided input that will be used in logs and potentially other
   * places we need to make sure it is secure to be used. Therefore, it is limited to unique
   * identifier patterns such as UUID strings or the UIDs used by DHIS2.
   *
   * <p>A valid ID is alphanumeric (with dash and underscore being allowed too) and has a length
   * between 1 and 36.
   *
   * @param requestId the ID to sanitize
   * @return the sanitized ID or "(illegal)" if the provided ID is invalid
   */
  private static String sanitizeRequestId(String requestId) {
    return VALID_REQUEST_ID_PATTERN.matcher(requestId).matches() ? requestId : "(illegal)";
  }
}
