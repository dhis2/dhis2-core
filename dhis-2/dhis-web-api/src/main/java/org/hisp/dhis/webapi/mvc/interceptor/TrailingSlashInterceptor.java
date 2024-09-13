package org.hisp.dhis.webapi.mvc.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class TrailingSlashInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {

    String requestURI = request.getRequestURI();
    String contextPath = request.getContextPath();

    // Ignore API paths
    if (requestURI.startsWith(contextPath + "/api")) {
      return true;
    }

    // Check if the path does not end with '/' or contains .
    if (!requestURI.endsWith("/") && !requestURI.contains(".")) {
      String queryString = request.getQueryString();
      String redirectURI = requestURI + "/";
      if (queryString != null) {
        redirectURI += "?" + queryString;
      }
      response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
      response.setHeader("Location", redirectURI);
      return false;
    }

    return true;
  }
}
