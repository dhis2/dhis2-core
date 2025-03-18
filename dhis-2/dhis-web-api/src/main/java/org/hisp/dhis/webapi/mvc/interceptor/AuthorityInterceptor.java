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
package org.hisp.dhis.webapi.mvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.RequiresAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor which checks that the {@link org.hisp.dhis.user.User} has any one of {@link
 * org.hisp.dhis.security.Authorities} which are passed in {@link RequiresAuthority} as an arg.
 *
 * <p>Throws {@link AccessDeniedException} if {@link org.hisp.dhis.user.User} does not have any of
 * the passed-in {@link org.hisp.dhis.security.Authorities}. The exception message includes the
 * required {@link org.hisp.dhis.security.Authorities} for the endpoint.
 *
 * <p>{@link Authorities#ALL} is automatically added to the check, as having this Authority allows
 * access to all methods by default.
 */
@Component
public class AuthorityInterceptor implements HandlerInterceptor {

  /**
   * Checks if the User has the required authority. Checks and executes at the method level first
   * (matching Spring convention). If no method-level annotation then check at the class level.
   *
   * @param request http request
   * @param response http response
   * @param handler request handler
   * @return boolean, indicating whether to proceed through the request chain or not
   */
  @Override
  public boolean preHandle(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull Object handler) {

    if (!(handler instanceof HandlerMethod)) {
      return true;
    }

    HandlerMethod handlerMethod = (HandlerMethod) handler;
    // check if RequiresAuthority is at method level
    if (handlerMethod.hasMethodAnnotation(RequiresAuthority.class)) {
      RequiresAuthority requiresMethodAuthority =
          handlerMethod.getMethodAnnotation(RequiresAuthority.class);
      if (requiresMethodAuthority != null)
        return checkForRequiredAuthority(requiresMethodAuthority);
    }

    // heck if RequiresAuthority is at method level
    if (handlerMethod.getBeanType().isAnnotationPresent(RequiresAuthority.class)) {
      RequiresAuthority requiresClassAuthority =
          handlerMethod.getBeanType().getAnnotation(RequiresAuthority.class);
      return checkForRequiredAuthority(requiresClassAuthority);
    }
    return true;
  }

  private boolean checkForRequiredAuthority(RequiresAuthority requiresAuthority) {
    // include 'ALL' authority in required authorities
    List<Authorities> requiredAuthorities = new ArrayList<>(List.of(Authorities.ALL));
    requiredAuthorities.addAll(List.of(requiresAuthority.anyOf()));

    // get user authorities
    final SecurityContext securityContext = SecurityContextHolder.getContext();
    Authentication authentication = securityContext.getAuthentication();
    if (authentication == null)
      throw new SessionAuthenticationException("Error trying to get user authentication details");

    Collection<? extends GrantedAuthority> userAuthorities = authentication.getAuthorities();

    // check if user has any of the required authorities passed in
    if (requiredAuthorities.stream()
        .noneMatch(
            reqAuth ->
                userAuthorities.stream()
                    .anyMatch(userAuth -> reqAuth.toString().equals(userAuth.getAuthority())))) {
      throw new AccessDeniedException(
          "Access is denied, requires one Authority from [%s]"
              .formatted(StringUtils.join(requiresAuthority.anyOf(), ", ")));
    }
    return true;
  }
}
