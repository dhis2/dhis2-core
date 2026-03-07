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
package org.hisp.dhis.webapi.security.csp;

import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_ENABLED;
import static org.hisp.dhis.security.utils.CspConstants.CONTENT_SECURITY_POLICY_HEADER_NAME;
import static org.hisp.dhis.security.utils.CspConstants.DEFAULT_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.FRAME_ANCESTORS_DEFAULT_CSP;
import static org.hisp.dhis.security.utils.CspConstants.USER_UPLOADED_CONTENT_CSP_POLICY;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/** Service that encapsulates CSP decision logic and caching for CSP-related settings. */
@Slf4j
@Component
public class CspPolicyService {
  private final DhisConfigurationProvider dhisConfig;

  private final ConfigurationService configurationService;

  private final Cache<Set<String>> corsWhitelistCache;

  private static final String CORS_WHITELIST_KEY = "corsWhitelist";

  public CspPolicyService(
      DhisConfigurationProvider dhisConfig,
      ConfigurationService configurationService,
      CacheProvider cacheProvider) {
    this.dhisConfig = dhisConfig;
    this.configurationService = configurationService;
    this.corsWhitelistCache = cacheProvider.createCorsWhitelistCache();
  }

  public String constructCustomCspPolicy(String customPolicy) {
    StringBuilder policyBuilder = new StringBuilder();

    if (customPolicy != null && !customPolicy.trim().isEmpty()) {
      policyBuilder.append(customPolicy.trim());
      if (!customPolicy.trim().endsWith(";")) {
        policyBuilder.append("; ");
      } else {
        policyBuilder.append(" ");
      }
    }

    policyBuilder.append(getFrameAncestorsCspDirective());

    return policyBuilder.toString();
  }

  public String constructDefaultCspPolicy() {
    return constructCustomCspPolicy(getDefaultCspPolicy());
  }

  public String constructUserUploadedContentCspPolicy() {
    return constructCustomCspPolicy(getUserUploadedContentCspPolicy());
  }

  private String getFrameAncestorsCspDirective() {
    Set<String> corsWhitelist = getCorsWhitelist();
    if (corsWhitelist != null && !corsWhitelist.isEmpty()) {
      return FRAME_ANCESTORS_DEFAULT_CSP + " " + String.join(" ", corsWhitelist) + ";";
    } else {
      return FRAME_ANCESTORS_DEFAULT_CSP + ";";
    }
  }

  /** Apply security headers including CSP to the provided response based on the handler. */
  public HttpHeaders getSecurityHeaders(String cspPolicy) {
    HttpHeaders headers = new HttpHeaders();

    if (isCspEnabled()) {
      if (cspPolicy == null || cspPolicy.trim().isEmpty()) {
        cspPolicy = getDefaultCspPolicy();
      }
      if (!cspPolicy.endsWith(";")) {
        cspPolicy = cspPolicy + ";";
      }
      headers.set(getContentSecurityPolicyHeaderName(), cspPolicy);
    }

    headers.set("X-Content-Type-Options", "nosniff");
    headers.set("X-Frame-Options", "SAMEORIGIN");

    log.debug("Applied CSP policy " + cspPolicy + " and standard security headers for response");
    return headers;
  }

  /*
   * Helper methods to retrieve CSP policies and settings
   */

  private Set<String> getCorsWhitelist() {
    return corsWhitelistCache.get(
        CORS_WHITELIST_KEY, key -> configurationService.getConfiguration().getCorsWhitelist());
  }

  private boolean isCspEnabled() {
    return dhisConfig.isEnabled(CSP_ENABLED);
  }

  private String getDefaultCspPolicy() {
    return DEFAULT_CSP_POLICY;
  }

  private String getContentSecurityPolicyHeaderName() {
    return CONTENT_SECURITY_POLICY_HEADER_NAME;
  }

  private String getUserUploadedContentCspPolicy() {
    return USER_UPLOADED_CONTENT_CSP_POLICY;
  }
}
