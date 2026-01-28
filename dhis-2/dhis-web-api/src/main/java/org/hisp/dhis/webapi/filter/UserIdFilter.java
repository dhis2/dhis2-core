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

import static org.hisp.dhis.external.conf.ConfigurationKey.LOGGING_USER_ID_ENCRYPTION_KEY;
import static org.hisp.dhis.external.conf.ConfigurationKey.LOGGING_USER_ID_HEADER_ENABLED;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that adds an encrypted user identifier to the response headers for authenticated users.
 * The header value is reversible using the configured key, allowing downstream systems to correlate
 * requests to a user.
 *
 * <p>Enabled when {@code logging.user_id_header.enabled} is true and {@code
 * logging.user_id_encryption_key} is set.
 */
@Slf4j
@Component("userIdHeaderFilter")
public class UserIdFilter extends OncePerRequestFilter {
  private static final String HEADER_NAME = "X-User-ID";
  private static final String MDC_KEY = "userUid";
  private static final String HEADER_VERSION_PREFIX = "v1.";
  private static final int GCM_IV_LENGTH_BYTES = 12;
  private static final int GCM_TAG_LENGTH_BITS = 128;
  private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
  private static final int KEY_LENGTH_BYTES = 32;
  private static final long CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(5);
  private static final int MAX_CACHE_SIZE = 10_000;
  private static final ConcurrentHashMap<String, CacheEntry> HEADER_CACHE =
      new ConcurrentHashMap<>();

  private final boolean enabled;
  private final byte[] keyBytes;
  private final SecureRandom secureRandom = new SecureRandom();

  public UserIdFilter(DhisConfigurationProvider dhisConfig) {
    boolean configEnabled = dhisConfig.isEnabled(LOGGING_USER_ID_HEADER_ENABLED);
    String token = dhisConfig.getProperty(LOGGING_USER_ID_ENCRYPTION_KEY);
    if (configEnabled && (token == null || token.isBlank())) {
      log.warn(
          "Session ID header logging enabled, but logging.session_id_encryption_key is not set.");
      this.enabled = false;
      this.keyBytes = new byte[0];
    } else {
      byte[] decodedKey = token == null ? new byte[0] : decodeKey(token);
      if (decodedKey.length != KEY_LENGTH_BYTES) {
        log.warn(
            "User ID header logging enabled, but logging.user_id_encryption_key must be a base64-encoded 32-byte key.");
        this.enabled = false;
        this.keyBytes = new byte[0];
      } else {
        this.enabled = configEnabled;
        this.keyBytes = decodedKey;
      }
    }
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String headerValue = null;
    if (enabled && CurrentUserUtil.hasCurrentUser()) {
      try {
        UserDetails userDetails = CurrentUserUtil.getCurrentUserDetails();
        headerValue = getOrCreateHeaderValue(userDetails.getUid());
        response.addHeader(HEADER_NAME, headerValue);
        MDC.put(MDC_KEY, headerValue);
      } catch (GeneralSecurityException ex) {
        log.error("Failed to encrypt user header payload", ex);
      }
    }

    try {
      chain.doFilter(request, response);
    } finally {
      if (headerValue != null) {
        MDC.remove(MDC_KEY);
      }
    }
  }

  private String encrypt(String payload) throws GeneralSecurityException {
    byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
    secureRandom.nextBytes(iv);
    Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
    SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
    byte[] ciphertext = cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    byte[] combined = new byte[iv.length + ciphertext.length];
    System.arraycopy(iv, 0, combined, 0, iv.length);
    System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
    return HEADER_VERSION_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
  }

  private String getOrCreateHeaderValue(String cacheKey) throws GeneralSecurityException {
    long now = System.currentTimeMillis();
    CacheEntry cached = HEADER_CACHE.get(cacheKey);
    if (cached != null && cached.expiresAtMillis > now) {
      return cached.headerValue;
    }

    if (cached != null) {
      HEADER_CACHE.remove(cacheKey, cached);
    }

    String headerValue = encrypt(cacheKey);
    CacheEntry entry = new CacheEntry(headerValue, now + CACHE_TTL_MILLIS);
    HEADER_CACHE.put(cacheKey, entry);

    if (HEADER_CACHE.size() > MAX_CACHE_SIZE) {
      HEADER_CACHE.entrySet().removeIf(e -> e.getValue().expiresAtMillis <= now);
      if (HEADER_CACHE.size() > MAX_CACHE_SIZE) {
        HEADER_CACHE.clear();
      }
    }

    return headerValue;
  }

  private static byte[] decodeKey(String token) {
    try {
      return Base64.getDecoder().decode(token);
    } catch (IllegalArgumentException ex) {
      return new byte[0];
    }
  }

  private record CacheEntry(String headerValue, long expiresAtMillis) {}
}
