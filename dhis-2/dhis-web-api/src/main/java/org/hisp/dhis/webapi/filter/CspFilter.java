/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_ENABLED;
import static org.hisp.dhis.security.utils.CspConstants.EXTERNAL_STATIC_CONTENT_URL_PATTERNS;
import static org.hisp.dhis.security.utils.CspConstants.SCRIPT_SOURCE_DEFAULT;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class CspFilter extends OncePerRequestFilter {
  public static final String CONTENT_SECURITY_POLICY_HEADER_NAME = "Content-Security-Policy";
  public static final String FRAME_ANCESTORS_DEFAULT_CSP = "frame-ancestors 'self'";

  private final boolean enabled;

  private final ConfigurationService configurationService;

  /** Cache for CORS whitelist to avoid DB lookups on every request. Expires after 5 minutes. */
  private final Cache<Set<String>> corsWhitelistCache;

  public CspFilter(
      DhisConfigurationProvider dhisConfig,
      ConfigurationService configurationService,
      CacheProvider cacheProvider) {
    this.enabled = dhisConfig.isEnabled(CSP_ENABLED);
    this.configurationService = configurationService;
    this.corsWhitelistCache = cacheProvider.createCorsWhitelistCache();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String url = req.getRequestURL().toString();

    if (!enabled) {
      res.addHeader("X-Frame-Options", "SAMEORIGIN");
      chain.doFilter(req, res);
      return;
    }

    if (isUploadedContentInsideApi(url)) {
      res.addHeader(CONTENT_SECURITY_POLICY_HEADER_NAME, SCRIPT_SOURCE_DEFAULT);
    }

    setFrameAncestorsCspRule(res);

    chain.doFilter(req, res);
  }

  private void setFrameAncestorsCspRule(HttpServletResponse res) {
    Set<String> corsWhitelist = getCorsWhitelist();
    if (!corsWhitelist.isEmpty()) {
      String corsAllowedOrigins = String.join(" ", corsWhitelist);
      res.addHeader(
          CONTENT_SECURITY_POLICY_HEADER_NAME,
          FRAME_ANCESTORS_DEFAULT_CSP + " " + corsAllowedOrigins + ";");
    } else {
      res.addHeader(CONTENT_SECURITY_POLICY_HEADER_NAME, FRAME_ANCESTORS_DEFAULT_CSP + ";");
    }
  }

  /**
   * Returns the cached CORS whitelist, refreshing from the database if the cache has expired (older
   * than 5 minutes) or is not yet initialized.
   *
   * @return the CORS whitelist Set
   */
  private Set<String> getCorsWhitelist() {
    return corsWhitelistCache.get(
        "CORS_WHITELIST", key -> configurationService.getConfiguration().getCorsWhitelist());
  }

  private boolean isUploadedContentInsideApi(String requestURI) {
    for (Pattern pattern : EXTERNAL_STATIC_CONTENT_URL_PATTERNS) {
      if (pattern.matcher(requestURI).matches()) {
        return true;
      }
    }
    return false;
  }
}
