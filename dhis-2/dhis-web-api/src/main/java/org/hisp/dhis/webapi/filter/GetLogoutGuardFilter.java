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

import static org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig.jsonMapper;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.forbidden;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.webapi.security.CrossOriginGetGuard;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies the {@link CrossOriginGetGuard} to logout requests sent with GET. Runs right before
 * Spring Security's logout filter.
 *
 * <p>Logout with GET is deprecated: POST is the supported method. A GET logout of an authenticated
 * session is rejected with 403 when the browser sent it as a cross-origin navigation or resource
 * load, and logs a deprecation warning otherwise. A GET logout without an authenticated session has
 * no session to end and passes unchanged; this keeps the redirect to the logout URL working after
 * the session was expired by the concurrent session control.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@RequiredArgsConstructor
public class GetLogoutGuardFilter extends OncePerRequestFilter {
  private static final AuthenticationTrustResolver TRUST_RESOLVER =
      new AuthenticationTrustResolverImpl();

  /** Matches logout requests sent with GET. */
  @Nonnull private final RequestMatcher getLogoutRequestMatcher;

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull FilterChain filterChain)
      throws ServletException, IOException {
    if (getLogoutRequestMatcher.matches(request) && isAuthenticated()) {
      if (!CrossOriginGetGuard.isAllowed(request)) {
        CrossOriginGetGuard.logRejected(request);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(
            response.getOutputStream(),
            forbidden(CrossOriginGetGuard.MESSAGE).setErrorCode(ErrorCode.E1006));
        return;
      }
      log.warn(
          "Logout with GET is deprecated and will be removed, use POST instead: GET {}",
          CrossOriginGetGuard.sanitize(request.getRequestURI()));
    }
    filterChain.doFilter(request, response);
  }

  private static boolean isAuthenticated() {
    return TRUST_RESOLVER.isAuthenticated(SecurityContextHolder.getContext().getAuthentication());
  }
}
