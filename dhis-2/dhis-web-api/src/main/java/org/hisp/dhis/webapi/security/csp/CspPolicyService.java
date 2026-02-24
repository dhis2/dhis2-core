/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 */
package org.hisp.dhis.webapi.security.csp;

import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_ENABLED;
import static org.hisp.dhis.security.utils.CspConstants.FRAME_ANCESTORS_DEFAULT_CSP;
import static org.hisp.dhis.security.utils.CspConstants.CONTENT_SECURITY_POLICY_HEADER_NAME;
import static org.hisp.dhis.security.utils.CspConstants.DEFAULT_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.USER_UPLOADED_CONTENT_CSP_POLICY;

import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Service that encapsulates CSP decision logic and caching for CSP-related settings.
 */
@Slf4j
@Component
public class CspPolicyService {
  private final DhisConfigurationProvider dhisConfig;

  private final ConfigurationService configurationService;

  private final Cache<Set<String>> corsWhitelistCache;

  private static final String CORS_WHITELIST_KEY = "corsWhitelist";

  public CspPolicyService(DhisConfigurationProvider dhisConfig, ConfigurationService configurationService, CacheProvider cacheProvider) {
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

  /**
   * Apply security headers including CSP to the provided response based on the handler.
   */
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

    log.info("Applied CSP policy " + cspPolicy + " and standard security headers for response");
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
