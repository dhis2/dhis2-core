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
import static org.hisp.dhis.security.utils.CspConstants.CONTENT_SECURITY_POLICY_HEADER_NAME;
import static org.hisp.dhis.security.utils.CspConstants.DEFAULT_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.FRAME_ANCESTORS_DEFAULT_CSP;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.webapi.security.CspPolicyHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class CspFilter extends OncePerRequestFilter {
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
    try {
      if (!enabled) {
        // If CSP is not enabled, just set X-Frame-Options to SAMEORIGIN for clickjacking protection
        // and proceed
        res.addHeader("X-Frame-Options", "SAMEORIGIN");

        chain.doFilter(req, res);
        return;
      }

      // Check if a custom CSP policy was set via @CustomCsp annotation
      String customCspPolicy = CspPolicyHolder.getCspPolicy();
      String cspPolicy;

      if (customCspPolicy != null) {
        // Use the custom CSP policy from the controller
        cspPolicy = customCspPolicy;
      } else {
        // Use strict default policy for all other endpoints
        cspPolicy = DEFAULT_CSP_POLICY;
      }

      // Set the base CSP policy
      res.addHeader(CONTENT_SECURITY_POLICY_HEADER_NAME, cspPolicy);

      // Add frame-ancestors CSP rule based on CORS whitelist
      setFrameAncestorsCspRule(res);

      // Add additional security headers
      // Always set X-Content-Type-Options to nosniff to prevent MIME type sniffing
      res.addHeader("X-Content-Type-Options", "nosniff");

      chain.doFilter(req, res);
    } finally {
      // Clean up the ThreadLocal to prevent memory leaks
      CspPolicyHolder.clear();
    }
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
}
