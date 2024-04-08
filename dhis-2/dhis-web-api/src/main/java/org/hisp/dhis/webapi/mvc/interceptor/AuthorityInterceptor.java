package org.hisp.dhis.webapi.mvc.interceptor;

import java.util.Arrays;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.security.HasAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AuthorityInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull Object handler) {
    HandlerMethod handlerMethod = (HandlerMethod) handler;

    if (!handlerMethod.hasMethodAnnotation(HasAuthority.class)) {
      return true;
    }

    // get user authorities
    final SecurityContext securityContext = SecurityContextHolder.getContext();
    Authentication authentication = securityContext.getAuthentication();
    Collection<? extends GrantedAuthority> userAuthorities = authentication.getAuthorities();

    HasAuthority hasAuthority = handlerMethod.getMethodAnnotation(HasAuthority.class);

    // check if user has any of the required authorities passed in
    if (Arrays.stream(hasAuthority.anyOf())
        .noneMatch(
            reqAuth ->
                userAuthorities.stream()
                    .anyMatch(userAuth -> reqAuth.name().equals(userAuth.getAuthority())))) {
      log.info(
          "User {} does not have the required authority for method {}",
          authentication.getName(),
          ((HandlerMethod) handler).getMethod().getName());
      throw new AccessDeniedException("Access is denied");
    }
    return true;
  }
}
