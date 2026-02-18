/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static org.hisp.dhis.external.conf.ConfigurationKey.STATIC_ASSETS_CACHE_MAX_AGE;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter which applies cache headers to static assets like JS/CSS/images when enabled.
 *
 * @author Jason P. Pickering
 */
@Component
public class StaticAssetsCacheFilter extends OncePerRequestFilter {
  // Match asset paths with '/dhis-web-' or '/apps' that end with common static extensions.
  public static final String ASSET_PATH_REGEX =
      "\\/(dhis-web-|apps).*(\\.(js|css|png|jpg|jpeg|gif|svg|webp|ico|woff2?|ttf|eot|map|properties))$";
  public static final Pattern ASSET_PATH_PATTERN = Pattern.compile(ASSET_PATH_REGEX);

  private final long maxAgeSeconds;

  public StaticAssetsCacheFilter(DhisConfigurationProvider dhisConfig) {
    this.maxAgeSeconds = parseMaxAgeSeconds(dhisConfig.getProperty(STATIC_ASSETS_CACHE_MAX_AGE));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (maxAgeSeconds > 0 && HttpMethod.GET.matches(request.getMethod())) {
      String uri = request.getRequestURI();
      Matcher matcher = ASSET_PATH_PATTERN.matcher(uri);
      if (matcher.find()) {
        ContextUtils.setCacheControl(
            response, CacheControl.maxAge(maxAgeSeconds, TimeUnit.SECONDS).cachePublic());
      }
    }

    chain.doFilter(request, response);
  }

  private static long parseMaxAgeSeconds(String value) {
    if (value == null || value.isBlank()) {
      return 0;
    }
    try {
      return Math.max(0, Long.parseLong(value));
    } catch (NumberFormatException ex) {
      return 0;
    }
  }
}
