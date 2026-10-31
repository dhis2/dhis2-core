/*
 * Copyright (c) 2004-2026, University of Oslo
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
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.configuration.ConfigurationService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Decides whether a browser {@code Origin} may talk to this server: it must be the server's own
 * origin or match an entry on the CORS allowlist ({@link ConfigurationService#isCorsWhitelisted}).
 *
 * <p>{@link DhisCorsProcessor} uses it to decide which origins get CORS response headers, and
 * {@link CrossOriginRequestFilter} uses it to decide which requests to reject, so both always apply
 * the same rule.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class AllowedOriginMatcher {

  /**
   * The serialized origin browsers send for opaque origins (sandboxed frames, {@code file:} and
   * {@code data:} documents, privacy-sensitive redirects). It never identifies a trusted site.
   */
  static final String NULL_ORIGIN = "null";

  private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

  private final ConfigurationService configurationService;

  /**
   * @param request the current request, used to compute the server's own origin
   * @param origin the value of the request's {@code Origin} header
   * @return true if the origin is the server's own origin or is on the CORS allowlist; false for a
   *     missing, empty or {@code null} origin
   */
  public boolean isAllowed(HttpServletRequest request, @CheckForNull String origin) {
    if (StringUtils.isEmpty(origin) || NULL_ORIGIN.equalsIgnoreCase(origin)) {
      return false;
    }

    return getSelfOrigin(request).equals(origin) || configurationService.isCorsWhitelisted(origin);
  }

  /**
   * Computes the origin (scheme, host and port) the request was addressed to. The scheme is taken
   * from the {@code X-Forwarded-Proto} header when a reverse proxy sets it; host and port always
   * come from the request itself. The port is omitted when it is the default for the scheme.
   *
   * @param request the current request
   * @return the server's own origin, for example {@code https://dhis2.example.org}
   */
  public String getSelfOrigin(HttpServletRequest request) {
    UriComponentsBuilder uriBuilder =
        ServletUriComponentsBuilder.fromContextPath(new HttpServletRequestEncodingWrapper(request))
            .replacePath("");

    String forwardedProto = request.getHeader(X_FORWARDED_PROTO);

    if (!StringUtils.isEmpty(forwardedProto)) {
      uriBuilder.scheme(forwardedProto);
    }

    return uriBuilder.build().toString();
  }

  /**
   * Simple HttpServletRequestWrapper implementation that makes sure that the query string is
   * properly encoded.
   */
  private static class HttpServletRequestEncodingWrapper extends HttpServletRequestWrapper {
    HttpServletRequestEncodingWrapper(HttpServletRequest request) {
      super(request);
    }

    @Override
    public String getQueryString() {
      String queryString = super.getQueryString();

      if (!StringUtils.isEmpty(queryString)) {
        return URLEncoder.encode(queryString, StandardCharsets.UTF_8);
      }

      return queryString;
    }
  }
}
