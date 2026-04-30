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
package org.hisp.dhis.webapi.security.csp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that emits the default {@code Content-Security-Policy} plus {@code
 * X-Content-Type-Options} on every response that flows through the filter chain. {@code
 * X-Frame-Options} is added only when CSP is disabled (legacy fallback); when CSP is enabled the
 * {@code frame-ancestors} directive supersedes it.
 *
 * <p>Necessary because {@link CspInterceptor} only fires when {@code DispatcherServlet} resolves a
 * {@link org.springframework.web.method.HandlerMethod}, leaving 404 responses, static-resource
 * handlers and pre-{@code DispatcherServlet} rejections without security headers. This filter sets
 * the baseline; for handler-mapped requests {@link CspInterceptor} overrides {@code
 * Content-Security-Policy} with the annotation-driven policy via {@code response.setHeader}.
 *
 * @see CspInterceptor
 * @author Morten Svanaes
 */
public class CspBaselineFilter extends OncePerRequestFilter {

  private final CspPolicyService cspPolicyService;

  public CspBaselineFilter(CspPolicyService cspPolicyService) {
    this.cspPolicyService = cspPolicyService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    HttpHeaders baseline = cspPolicyService.getDefaultSecurityHeaders();
    baseline.forEach(
        (name, values) -> {
          if (!values.isEmpty()) {
            response.setHeader(name, values.get(0));
          }
        });
    chain.doFilter(request, response);
  }
}
