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
package org.hisp.dhis.aspects;

import java.util.Arrays;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hisp.dhis.security.annotations.HasAuthorities;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Aspect that gets invoked wherever the {@link HasAuthorities} is used. Checks that the {@link
 * org.hisp.dhis.user.User} has whatever {@link org.hisp.dhis.security.Authorities} are passed in
 * {@link HasAuthorities}.
 *
 * <p>Throws {@link AccessDeniedException} if {@link org.hisp.dhis.user.User} does not have the
 * required {@link org.hisp.dhis.security.Authorities}
 */
@Slf4j
@Aspect
@Component
public class HasAuthoritiesAspect {

  @Before("@annotation(requiredAuthorities)")
  public void hasAuthorities(final HasAuthorities requiredAuthorities) {
    log.info("checking required auth in HasAuthoritiesAspect");

    final SecurityContext securityContext = SecurityContextHolder.getContext();
    Authentication authentication = securityContext.getAuthentication();

    String name = authentication.getName();
    log.info("current user {}", name);

    Collection<? extends GrantedAuthority> userAuthorities = authentication.getAuthorities();
    log.info("current user authorities {}", userAuthorities);

    if (Arrays.stream(requiredAuthorities.authorities())
        .noneMatch(
            reqAuth ->
                userAuthorities.stream()
                    .anyMatch(userAuth -> reqAuth.name().equals(userAuth.getAuthority())))) {
      log.warn("user does not have the required authority");
      throw new AccessDeniedException("required authority missing");
    }
    log.info("user has the required authority");
  }
}
