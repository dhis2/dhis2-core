/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.security.apikey;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@Slf4j
public class DhisApiTokenAuthenticationEntryPoint
    implements AuthenticationEntryPoint, ApplicationContextAware {
  private String realmName;

  private ApplicationContext applicationContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  public void setRealmName(String realmName) {
    this.realmName = realmName;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    _commence(response);

    HandlerExceptionResolver handlerExceptionResolver;
    try {
      handlerExceptionResolver =
          (HandlerExceptionResolver) applicationContext.getBean("handlerExceptionResolver");
      handlerExceptionResolver.resolveException(request, response, null, authException);
    } catch (Exception e) {
      log.error("Could not find a HandlerExceptionResolver bean!");
    }
  }

  public void _commence(HttpServletResponse response) {
    HttpStatus status = HttpStatus.UNAUTHORIZED;
    LinkedHashMap<String, String> parameters = new LinkedHashMap<>();

    if (this.realmName != null) {
      parameters.put("realm", this.realmName);
    }

    String wwwAuthenticate = computeWWWAuthenticateHeaderValue(parameters);

    response.addHeader("WWW-Authenticate", wwwAuthenticate);
    response.setStatus(status.value());
  }

  private static String computeWWWAuthenticateHeaderValue(Map<String, String> parameters) {
    StringBuilder wwwAuthenticate = new StringBuilder();
    wwwAuthenticate.append("ApiToken");

    if (!parameters.isEmpty()) {
      wwwAuthenticate.append(" ");
      int i = 0;

      for (Iterator<Map.Entry<String, String>> iterator = parameters.entrySet().iterator();
          iterator.hasNext();
          ++i) {
        Map.Entry<String, String> entry = iterator.next();

        wwwAuthenticate.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
        if (i != parameters.size() - 1) {
          wwwAuthenticate.append(", ");
        }
      }
    }

    return wwwAuthenticate.toString();
  }
}
