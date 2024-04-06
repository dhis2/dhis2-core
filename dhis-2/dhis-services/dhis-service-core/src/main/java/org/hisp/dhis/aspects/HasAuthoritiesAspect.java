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
