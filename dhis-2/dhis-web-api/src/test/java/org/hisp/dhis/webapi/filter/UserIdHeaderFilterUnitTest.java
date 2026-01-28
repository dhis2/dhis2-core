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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserIdHeaderFilterUnitTest {
  private static final String HEADER_NAME = "X-User-ID";
  private static final String ENCRYPTION_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
  private static final String HEADER_VERSION_PREFIX = "v1.";
  private static final int GCM_IV_LENGTH_BYTES = 12;
  private static final int GCM_TAG_LENGTH_BITS = 128;

  @Mock private DhisConfigurationProvider dhisConfigurationProvider;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldNotAddHeaderWhenDisabled() throws Exception {
    UserIdFilter filter = init(false);
    withAuthenticatedUser();

    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse res = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(req, res, chain);

    verify(res, never()).addHeader(eq(HEADER_NAME), anyString());
  }

  @Test
  void shouldNotAddHeaderWhenNoAuthenticatedUser() throws Exception {
    UserIdFilter filter = init(true);
    SecurityContextHolder.clearContext();
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse res = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(req, res, chain);

    verify(res, never()).addHeader(eq(HEADER_NAME), anyString());
  }

  @Test
  void shouldAddHeaderWhenAuthenticatedUser() throws Exception {

    String userUID = "uid123";
    UserIdFilter filter = init(true);
    UserDetails userDetails = withAuthenticatedUser();
    when(userDetails.getUid()).thenReturn(userUID);

    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse res = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(req, res, chain);

    ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);
    verify(res).addHeader(eq(HEADER_NAME), headerValueCaptor.capture());
    String headerValue = headerValueCaptor.getValue();
    org.junit.jupiter.api.Assertions.assertTrue(headerValue.startsWith(HEADER_VERSION_PREFIX));
    org.junit.jupiter.api.Assertions.assertEquals(userUID, decryptUserIDHeaderValue(headerValue));
  }

  private UserIdFilter init(boolean enabled) {
    when(dhisConfigurationProvider.isEnabled(LOGGING_USER_ID_HEADER_ENABLED)).thenReturn(enabled);
    when(dhisConfigurationProvider.getProperty(LOGGING_USER_ID_ENCRYPTION_KEY))
        .thenReturn(ENCRYPTION_KEY);
    return new UserIdFilter(dhisConfigurationProvider);
  }

  private UserDetails withAuthenticatedUser() {
    UserDetails userDetails = mock(UserDetails.class);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            userDetails, "pw", List.of((GrantedAuthority) () -> "ALL"));
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    return userDetails;
  }

  private String decryptUserIDHeaderValue(String headerValue) throws GeneralSecurityException {
    String token = headerValue.substring(HEADER_VERSION_PREFIX.length());
    byte[] combined = Base64.getUrlDecoder().decode(token);
    byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
    byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH_BYTES];
    System.arraycopy(combined, 0, iv, 0, iv.length);
    System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    SecretKeySpec key = new SecretKeySpec(Base64.getDecoder().decode(ENCRYPTION_KEY), "AES");
    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
    byte[] plaintext = cipher.doFinal(ciphertext);
    return new String(plaintext, StandardCharsets.UTF_8);
  }
}
