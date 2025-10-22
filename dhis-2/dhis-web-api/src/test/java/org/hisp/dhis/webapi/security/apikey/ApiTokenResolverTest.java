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
  void testTokenInHeaderAndQueryShouldReject() {
    // Given GET request with token in query string and header
    when(request.getMethod()).thenReturn("GET");
    when(request.getQueryString()).thenReturn("api_token=" + VALID_TOKEN);
    when(request.getHeader("Authorization")).thenReturn("ApiToken " + VALID_TOKEN);

    // When resolving token
    ApiTokenAuthenticationException exception =
        assertThrows(ApiTokenAuthenticationException.class, () -> resolver.resolve(request));

    // Then should throw an exception because token is in both header and query string
    assertTrue(exception.getMessage().contains("Found multiple tokens in the request"));
  }

  @Test
  void testPostRequestWithTokenInQueryStringShouldReject() {
    // Given POST request with token in query string
    when(request.getMethod()).thenReturn("POST");
    when(request.getQueryString()).thenReturn("api_token=" + VALID_TOKEN);
    when(request.getContentType()).thenReturn("text/plain"); // Not form-encoded

    // When resolving token
    ApiTokenAuthenticationException exception =
        assertThrows(ApiTokenAuthenticationException.class, () -> resolver.resolve(request));

    // Then should throw an exception because token is in query string but only form body is allowed
    assertTrue(
        exception
            .getMessage()
            .contains(
                "API token found in URL query string but only form-encoded body parameters are allowed"));
  }

  @Test
  void testPostRequestWithTokenInFormBodyShouldSucceed() {
    // Given POST request with token in form body (proper scenario)
    when(request.getMethod()).thenReturn("POST");
    when(request.getQueryString()).thenReturn(null); // No query string
    when(request.getParameterValues("api_token")).thenReturn(new String[] {VALID_TOKEN});
    when(request.getContentType()).thenReturn("application/x-www-form-urlencoded");

    // When resolving token
    String result = resolver.resolve(request);

    // Then should succeed
    assertNotNull(result);
  }

  @Test
  void testPostRequestWithTokenInBothQueryAndBodyWhenFormBodyAllowed_ShouldReject() {
    // Given POST request with token in both query string and body
    when(request.getMethod()).thenReturn("POST");
    when(request.getQueryString()).thenReturn("api_token=" + VALID_TOKEN);
    when(request.getParameterValues("api_token")).thenReturn(new String[] {VALID_TOKEN});
    when(request.getContentType()).thenReturn("application/x-www-form-urlencoded");

    // When resolving token
    ApiTokenAuthenticationException exception =
        assertThrows(ApiTokenAuthenticationException.class, () -> resolver.resolve(request));

    // Then should throw an exception because token is in both places
    assertTrue(
        exception
            .getMessage()
            .contains(
                "API token found in URL query string but only form-encoded body parameters are allowed"));
  }

  @Test
  void testGetRequestWithTokenInQueryStringWhenQueryAllowed_ShouldSucceed() {
    // Given GET request with token in query string
    when(request.getMethod()).thenReturn("GET");
    when(request.getQueryString()).thenReturn("api_token=" + VALID_TOKEN);

    // When resolving token
    String result = resolver.resolve(request);

    // Then should succeed
    assertNotNull(result);
  }

  @Test
  void testPostRequestWithWrongContentTypeWhenFormBodyAllowed_ShouldCheckForQueryToken() {
    // Given POST request with wrong Content-Type but with token in query string
    when(request.getMethod()).thenReturn("POST");
    when(request.getQueryString()).thenReturn("api_token=" + VALID_TOKEN);
    when(request.getContentType()).thenReturn("application/json");

    // When resolving token
    ApiTokenAuthenticationException exception =
        assertThrows(ApiTokenAuthenticationException.class, () -> resolver.resolve(request));

    // Then should throw an exception because token is in query string
    assertTrue(
        exception
            .getMessage()
            .contains(
                "API token found in URL query string but only form-encoded body parameters are allowed"));
  }

  @Test
  void testInvalidTokenChecksum_ShouldThrowException() {
    // Given GET request with invalid token
    when(request.getMethod()).thenReturn("GET");
    when(request.getQueryString()).thenReturn("api_token=invalid_token");

    // When resolving token
    ApiTokenAuthenticationException exception =
        assertThrows(ApiTokenAuthenticationException.class, () -> resolver.resolve(request));

    // Then should throw checksum validation exception
    assertTrue(exception.getMessage().contains("Checksum validation failed"));
  }

  @Test
  void testMultipleTokensInQuery_ShouldThrowException() {
    // Given GET request with multiple tokens in query string
    when(request.getMethod()).thenReturn("GET");
    when(request.getQueryString())
        .thenReturn("api_token=" + VALID_TOKEN + "&api_token=" + VALID_TOKEN);

    // When resolving token
    ApiTokenAuthenticationException exception =
        assertThrows(ApiTokenAuthenticationException.class, () -> resolver.resolve(request));

    // Then should throw multiple tokens exception
    assertTrue(exception.getMessage().contains("Found multiple Api tokens in the request"));
  }

  @Test
  void testUrlDecodingInQueryString_ShouldWork() {
    // Given GET request with URL-encoded token
    when(request.getMethod()).thenReturn("GET");
    when(request.getQueryString()).thenReturn("api_token=" + VALID_TOKEN.replace("_", "%5F"));

    // When resolving token
    String result = resolver.resolve(request);

    // Then should succeed and properly decode the token
    assertNotNull(result);
  }

  @Test
  void testPutRequestWithFormBodyConfiguration_ShouldReturnNull() {
    // Given PUT request with token
    when(request.getMethod()).thenReturn("PUT");

    // When resolving token
    String result = resolver.resolve(request);

    // Then should return null
    assertNull(result);
  }

  @Test
  void testDeleteRequestWithQueryConfiguration_ShouldReturnNull() {
    // Given DELETE request with token in header
    when(request.getMethod()).thenReturn("DELETE");
    when(request.getHeader("Authorization")).thenReturn("ApiToken " + VALID_TOKEN);

    // When resolving token
    String result = resolver.resolve(request);

    // Then should return valid token from header
    assertNotNull(result);
  }
}
