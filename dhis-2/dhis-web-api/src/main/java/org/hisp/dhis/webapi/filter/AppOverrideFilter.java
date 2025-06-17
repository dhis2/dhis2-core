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
package org.hisp.dhis.webapi.filter;

import static java.util.regex.Pattern.compile;

import com.google.common.base.Strings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.appmanager.AppManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Austin McGee <austin@dhis2.org>
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AppOverrideFilter extends OncePerRequestFilter {

  /* To be removed in favor of dynamic ClassPath loading, see BundledAppStorageService */
  @Deprecated(forRemoval = true)
  private static final Set<String> BUNDLED_APPS =
      Set.of(
          "aggregate-data-entry",
          "approval",
          "app-management",
          "cache-cleaner",
          "capture",
          "dashboard",
          "data-administration",
          "data-visualizer",
          "data-quality",
          "datastore",
          "event-reports",
          "event-visualizer",
          "global-shell",
          "import-export",
          "interpretation",
          "line-listing",
          "login",
          "maintenance",
          "maps",
          "menu-management",
          "messaging",
          "pivot",
          "reports",
          "scheduler",
          "settings",
          "sms-configuration",
          "translations",
          "usage-analytics",
          "user",
          "user-profile");

  public static final String APP_PATH_PATTERN_STRING =
      "^/" + AppManager.BUNDLED_APP_PREFIX + "(" + String.join("|", BUNDLED_APPS) + ")(/?.*)";

  public static final Pattern APP_PATH_PATTERN = compile(APP_PATH_PATTERN_STRING);

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull FilterChain chain)
      throws IOException, ServletException {

    String pathInfo = request.getPathInfo();
    Matcher m = APP_PATH_PATTERN.matcher(Strings.nullToEmpty(pathInfo));
    if (m.find()) {
      String appName = m.group(1);
      String resourcePath = m.group(2);

      String destinationPath = "/" + AppManager.INSTALLED_APP_PREFIX + appName + resourcePath;

      log.debug(
          "AppOverrideFilter :: Matched for path {} ({} | {}) => {}",
          pathInfo,
          appName,
          resourcePath,
          destinationPath);

      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null
          && authentication.isAuthenticated()
          && !authentication.getPrincipal().equals("anonymousUser")) {
        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(destinationPath);
        dispatcher.forward(request, response);
        return;
      }
    }

    chain.doFilter(request, response);
  }
}
