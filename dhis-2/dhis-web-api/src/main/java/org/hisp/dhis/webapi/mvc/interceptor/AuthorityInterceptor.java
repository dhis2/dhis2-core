/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.mvc.interceptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.RequiresAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor which checks that the {@link org.hisp.dhis.user.User} has any one of {@link
 * org.hisp.dhis.security.Authorities} which are passed in {@link RequiresAuthority} as an arg.
 *
 * <p>Throws {@link AccessDeniedException} if {@link org.hisp.dhis.user.User} does not have any of
 * the passed-in {@link org.hisp.dhis.security.Authorities}.
 *
 * <p>{@link Authorities#ALL} is automatically added to the check, as having this Authority allows
 * access to all methods by default.
 */
@Slf4j
@Component
public class AuthorityInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull Object handler) {
    HandlerMethod handlerMethod = (HandlerMethod) handler;

    if (!handlerMethod.hasMethodAnnotation(RequiresAuthority.class)) {
      return true;
    }

    RequiresAuthority requiresAuthority = handlerMethod.getMethodAnnotation(RequiresAuthority.class);
    if (requiresAuthority != null) {
      // include 'ALL' authority in required authorities
      List<Authorities> requiredAuthorities = new ArrayList<>(List.of(Authorities.ALL));
      requiredAuthorities.addAll(List.of(requiresAuthority.anyOf()));

      // get user authorities
      final SecurityContext securityContext = SecurityContextHolder.getContext();
      Authentication authentication = securityContext.getAuthentication();
      Collection<? extends GrantedAuthority> userAuthorities = authentication.getAuthorities();

      // check if user has any of the required authorities passed in
      if (requiredAuthorities.stream()
          .noneMatch(
              reqAuth ->
                  userAuthorities.stream()
                      .anyMatch(userAuth -> reqAuth.name().equals(userAuth.getAuthority())))) {
        throw new AccessDeniedException("Access is denied");
      }
    }
    return true;
  }
}
