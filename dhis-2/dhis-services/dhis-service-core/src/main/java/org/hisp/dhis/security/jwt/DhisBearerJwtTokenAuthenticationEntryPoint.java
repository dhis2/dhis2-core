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
package org.hisp.dhis.security.jwt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Spring {@link AuthenticationEntryPoint} used by the OAuth2 resource-server filter chain to signal
 * authentication failure for JWT bearer token requests.
 *
 * <p>On an unauthenticated or failed JWT request this entry point delegates to Spring Security's
 * {@link BearerTokenAuthenticationEntryPoint}, which writes an RFC 6750 compliant {@code
 * WWW-Authenticate: Bearer} challenge to the response. The exact challenge depends on the cause:
 *
 * <ul>
 *   <li>No credentials: a plain {@code Bearer realm="DHIS2"} challenge with HTTP 401.
 *   <li>Invalid or expired token (for example {@code org.springframework.security.oauth2.server
 *       .resource.InvalidBearerTokenException}): {@code error="invalid_token"} with HTTP 401.
 *   <li>Missing or insufficient scopes: {@code error="insufficient_scope"} with HTTP 403.
 * </ul>
 *
 * <p>After emitting the WWW-Authenticate challenge, the entry point also forwards the {@link
 * AuthenticationException} to the Spring MVC {@code HandlerExceptionResolver} so that DHIS2's
 * global exception advice can log and translate the failure consistently with other API errors.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@Slf4j
public class DhisBearerJwtTokenAuthenticationEntryPoint
    implements AuthenticationEntryPoint, ApplicationContextAware {
  private ApplicationContext applicationContext;

  /**
   * Set by the Spring container to supply the {@link ApplicationContext} from which the {@code
   * HandlerExceptionResolver} bean is looked up at request time.
   */
  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  private BearerTokenAuthenticationEntryPoint entryPoint =
      new BearerTokenAuthenticationEntryPoint();

  /**
   * Commence the authentication failure response: write the RFC 6750 {@code WWW-Authenticate:
   * Bearer} challenge (with {@code invalid_token} or {@code insufficient_scope} error codes as
   * applicable) and then forward the exception to the Spring MVC {@code HandlerExceptionResolver}
   * so DHIS2's global exception handling can render a consistent error payload.
   *
   * @param request the current HTTP request
   * @param response the HTTP response to which the challenge is written
   * @param authException the authentication failure that triggered this entry point
   * @throws IOException if writing to the response fails
   * @throws ServletException if the delegate entry point fails
   */
  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    entryPoint.commence(request, response, authException);

    HandlerExceptionResolver handlerExceptionResolver;
    try {
      handlerExceptionResolver =
          (HandlerExceptionResolver) applicationContext.getBean("handlerExceptionResolver");
      handlerExceptionResolver.resolveException(request, response, null, authException);
    } catch (BeansException e) {
      log.error("Could not find a HandlerExceptionResolver bean");
    }
  }
}
