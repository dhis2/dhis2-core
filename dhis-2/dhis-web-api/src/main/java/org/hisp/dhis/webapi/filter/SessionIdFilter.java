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

import static org.hisp.dhis.external.conf.ConfigurationKey.LOGGING_SESSION_ID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that adds a hashed version of the Session ID to the Mapped Diagnostic Context (MDC) for
 * authenticated users. This allows correlating multiple requests from the same user session. Access
 * via {@code %X{sessionId}} in log4j2 pattern layouts.
 *
 * <p>The session ID is hashed using SHA-256 and base64-encoded for security. Only enabled when
 * {@code logging.session_id} is true.
 *
 * @author Luciano Fiandesio
 * @see <a href="https://logback.qos.ch/manual/mdc.html">MDC Documentation</a>
 * @see <a
 *     href="https://logging.apache.org/log4j/2.x/manual/pattern-layout.html#converter-thread-context-map">Pattern
 *     Layout Thread Context Map</a>
 */
@Slf4j
@Component
public class SessionIdFilter extends OncePerRequestFilter {
  private static final String SESSION_ID_KEY = "sessionId";

  /** The hash algorithm to use. */
  private static final String HASH_ALGO = "SHA-256";

  private static final String IDENTIFIER_PREFIX = "ID";

  private final boolean enabled;

  public SessionIdFilter(DhisConfigurationProvider dhisConfig) {
    this.enabled = dhisConfig.isEnabled(LOGGING_SESSION_ID);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    if (enabled) {
      try {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
            && authentication.isAuthenticated()
            && !authentication.getPrincipal().equals("anonymousUser")) {

          MDC.put(SESSION_ID_KEY, IDENTIFIER_PREFIX + hashToBase64(req.getSession().getId()));
        }
      } catch (NoSuchAlgorithmException e) {
        log.error(String.format("Invalid Hash algorithm provided (%s)", HASH_ALGO), e);
      }
    }

    chain.doFilter(req, res);
  }

  static String hashToBase64(String sessionId) throws NoSuchAlgorithmException {
    byte[] data = sessionId.getBytes();
    MessageDigest digester = MessageDigest.getInstance(HASH_ALGO);
    digester.update(data);
    return Base64.getEncoder().encodeToString(digester.digest());
  }
}
