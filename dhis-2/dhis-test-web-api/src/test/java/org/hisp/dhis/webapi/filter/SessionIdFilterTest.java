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

import static org.hisp.dhis.external.conf.ConfigurationKey.LOGGING_SESSION_ID_HEADER_ENABLED;
import static org.hisp.dhis.log.MdcKeys.MDC_SESSION_ID;
import static org.hisp.dhis.webapi.filter.SessionIdFilter.hashToBase64;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class SessionIdFilterTest {
  @Mock private DhisConfigurationProvider dhisConfigurationProvider;

  private SessionIdFilter subject;

  @BeforeEach
  void setUp() {
    MDC.clear();
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void testMdcNotSetWhenUnauthenticated() throws Exception {
    init(false);
    String[] capturedMdc = {null};
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse res = mock(HttpServletResponse.class);
    FilterChain chain = (r, p) -> capturedMdc[0] = MDC.get(MDC_SESSION_ID);

    subject.doFilter(req, res, chain);

    assertNull(capturedMdc[0]);
    assertNull(MDC.get(MDC_SESSION_ID));
  }

  @Test
  void testMdcSetWhenAuthenticated() throws Exception {
    withAuthenticatedUser();
    init(false);

    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse res = mock(HttpServletResponse.class);
    HttpSession session = mock(HttpSession.class);
    when(req.getSession()).thenReturn(session);
    when(session.getId()).thenReturn("ABCDEFGHILMNO");

    String[] capturedMdc = {null};
    FilterChain chain = (r, p) -> capturedMdc[0] = MDC.get(MDC_SESSION_ID);

    subject.doFilter(req, res, chain);

    assertEquals("ID" + hashToBase64("ABCDEFGHILMNO"), capturedMdc[0]);
    assertNull(MDC.get(MDC_SESSION_ID));
  }

  @Test
  void testHeaderNotAddedWhenDisabled() throws Exception {
    withAuthenticatedUser();
    init(false);

    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse res = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    HttpSession session = mock(HttpSession.class);
    when(req.getSession()).thenReturn(session);
    when(session.getId()).thenReturn("ABCDEFGHILMNO");

    subject.doFilter(req, res, chain);

    verify(res, never())
        .addHeader(
            org.mockito.ArgumentMatchers.eq("X-Session-ID"),
            org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void testHeaderAddedWhenEnabled() throws Exception {
    withAuthenticatedUser();
    init(true);

    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse res = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    HttpSession session = mock(HttpSession.class);
    when(req.getSession()).thenReturn(session);
    when(session.getId()).thenReturn("ABCDEFGHILMNO");

    subject.doFilter(req, res, chain);

    verify(res).addHeader("X-Session-ID", "ID" + hashToBase64("ABCDEFGHILMNO"));
  }

  private void withAuthenticatedUser() {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            "admin", "admin", List.of((GrantedAuthority) () -> "ALL"));
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
  }

  private void init(boolean headerEnabled) {
    when(dhisConfigurationProvider.isEnabled(LOGGING_SESSION_ID_HEADER_ENABLED))
        .thenReturn(headerEnabled);
    subject = new SessionIdFilter(dhisConfigurationProvider);
  }
}
