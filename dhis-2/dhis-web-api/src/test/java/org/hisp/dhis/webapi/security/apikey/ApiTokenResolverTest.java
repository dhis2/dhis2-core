/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.webapi.security.apikey;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiTokenResolverTest {

  @Mock private HttpServletRequest request;

  private ApiTokenResolver resolver;

  // Valid test token with proper checksum
  private static final String VALID_TOKEN =
      "d2p_lyt5j9WqXaVzJsUhvGJ1w0Ea5GL4jkszazPSWo5HWLFC1QnPCi";

  @BeforeEach
  void setUp() {
    resolver = new ApiTokenResolver();
  }

  @Test
  void testValidTokenInAuthorizationHeader_ShouldSucceed() {
    // Given request with valid token in Authorization header
    when(request.getHeader("Authorization")).thenReturn("ApiToken " + VALID_TOKEN);

    // When resolving token
    String result = resolver.resolve(request);

    // Then should return hashed token
    assertNotNull(result);
  }

  @Test
  void testMissingAuthorizationHeader_ShouldReturnNull() {
    // Given request without Authorization header
    when(request.getHeader("Authorization")).thenReturn(null);

    // When resolving token
    String result = resolver.resolve(request);

    // Then should return null
    assertNull(result);
  }

  @Test
  void testWrongHeaderPrefix_ShouldReturnNull() {
    // Given request with wrong header prefix
    when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);

    // When resolving token
    String result = resolver.resolve(request);

    // Then should return null (not an API token)
    assertNull(result);
  }

  @Test
  void testMalformedAuthorizationHeader_ShouldThrowException() {
    // Given request with malformed Authorization header (missing space)
    when(request.getHeader("Authorization")).thenReturn("ApiToken" + VALID_TOKEN);

    // When resolving token
    ApiTokenAuthenticationException exception =
        assertThrows(ApiTokenAuthenticationException.class, () -> resolver.resolve(request));

    // Then should throw exception for malformed token
    assertTrue(exception.getMessage().contains("Api token is malformed"));
  }

  @Test
  void testEmptyAuthorizationHeader_ShouldReturnNull() {
    // Given request with empty Authorization header
    when(request.getHeader("Authorization")).thenReturn("");

    // When resolving token
    String result = resolver.resolve(request);

    // Then should return null
    assertNull(result);
  }

  @Test
  void testCaseInsensitiveHeaderPrefix_ShouldSucceed() {
    // Given request with lowercase token prefix
    when(request.getHeader("Authorization")).thenReturn("apitoken " + VALID_TOKEN);

    // When resolving token
    String result = resolver.resolve(request);

    // Then should succeed (case insensitive)
    assertNotNull(result);
  }

  @Test
  void testGetRequestWithHeaderToken_ShouldSucceed() {
    // Given request with token in header
    when(request.getHeader("Authorization")).thenReturn("ApiToken " + VALID_TOKEN);

    // When resolving token
    String result = resolver.resolve(request);

    // Then should return valid token from header
    assertNotNull(result);
  }
}
