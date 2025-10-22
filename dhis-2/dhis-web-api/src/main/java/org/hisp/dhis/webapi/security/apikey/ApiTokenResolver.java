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
package org.hisp.dhis.webapi.security.apikey;

import static org.hisp.dhis.security.apikey.ApiKeyTokenGenerator.isValidTokenChecksum;

import com.google.common.net.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import lombok.Setter;
import org.apache.commons.lang3.Strings;
import org.hisp.dhis.security.apikey.ApiKeyTokenGenerator;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Setter
public final class ApiTokenResolver {

  private static final Pattern AUTHORIZATION_PATTERN =
      Pattern.compile("^ApiToken (?<token>[a-z0-9-._~+/]+=*)$", Pattern.CASE_INSENSITIVE);

  public static final String HEADER_TOKEN_KEY_PREFIX = "apitoken";
  public static final String REQUEST_PARAMETER_NAME = "api_token";
  public static final String CHECKSUM_VALIDATION_FAILED = "Checksum validation failed";

  private String bearerTokenHeaderName = HttpHeaders.AUTHORIZATION;

  @CheckForNull
  public String resolve(HttpServletRequest request) {
    char[] headerToken = extractTokenFromHeader(request);
    char[] parameterToken = extractTokenFromParameters(request);

    if (validateHeaderToken(headerToken, parameterToken)) return hashToken(headerToken);
    if (validateParameterToken(request, parameterToken)) return hashToken(parameterToken);

    return null;
  }

  private char[] extractTokenFromHeader(HttpServletRequest request) {
    String authorization = request.getHeader(this.bearerTokenHeaderName);
    if (!Strings.CI.startsWith(authorization, HEADER_TOKEN_KEY_PREFIX)) {
      return new char[0];
    }

    Matcher matcher = AUTHORIZATION_PATTERN.matcher(authorization);
    if (!matcher.matches()) {
      throw new ApiTokenAuthenticationException(
          ApiTokenErrors.invalidRequest("Api token is malformed"));
    }

    return matcher.group("token").toCharArray();
  }

  private static boolean validateHeaderToken(
      char[] authorizationHeaderToken, char[] parameterToken) {
    if (authorizationHeaderToken.length > 0) {
      validateChecksum(authorizationHeaderToken);

      if (parameterToken.length > 0) {
        throw new ApiTokenAuthenticationException(
            ApiTokenErrors.invalidRequest("Found multiple tokens in the request"));
      }
      return true;
    }

    return false;
  }

  private char[] extractTokenFromParameters(HttpServletRequest request) {
    if ("POST".equals(request.getMethod())) {
      return extractTokenFromFormBody(request);
    } else if ("GET".equals(request.getMethod())) {
      return extractTokenFromQueryString(request);
    }
    return new char[0];
  }

  private static char[] extractTokenFromQueryString(HttpServletRequest request) {
    String queryString = request.getQueryString();
    if (queryString == null || queryString.isEmpty()) {
      return new char[0];
    }

    // Parse query string manually to avoid mixing with body parameters
    String[] pairs = queryString.split("&");
    String tokenValue = null;
    int tokenCount = 0;

    for (String pair : pairs) {
      String[] keyValue = pair.split("=", 2);
      if (keyValue.length == 2 && REQUEST_PARAMETER_NAME.equals(keyValue[0])) {
        tokenValue = keyValue[1];
        tokenCount++;
      }
    }

    if (tokenCount == 0) {
      return new char[0];
    }

    if (tokenCount == 1) {
      return URLDecoder.decode(tokenValue, StandardCharsets.UTF_8).toCharArray();
    }

    throw new ApiTokenAuthenticationException(
        ApiTokenErrors.invalidRequest("Found multiple Api tokens in the request"));
  }

  private static char[] extractTokenFromFormBody(HttpServletRequest request) {
    // Validate Content-Type for form-encoded requests
    String contentType = request.getContentType();
    if (contentType == null
        || !contentType.toLowerCase().startsWith("application/x-www-form-urlencoded")) {
      // If not form-encoded, check if token is in query string (which should be rejected)
      if (request.getQueryString() != null
          && request.getQueryString().contains(REQUEST_PARAMETER_NAME)) {
        throw new ApiTokenAuthenticationException(
            ApiTokenErrors.invalidRequest(
                "API token found in URL query string but only form-encoded body parameters are allowed"));
      }
      return new char[0];
    }

    // For form-encoded requests, we need to extract only from the body, not query string
    String[] allValues = request.getParameterValues(REQUEST_PARAMETER_NAME);
    if (allValues == null || allValues.length == 0) {
      return new char[0];
    }

    // Check if token is also in query string (which should be rejected)
    String queryString = request.getQueryString();
    if (queryString != null && queryString.contains(REQUEST_PARAMETER_NAME)) {
      throw new ApiTokenAuthenticationException(
          ApiTokenErrors.invalidRequest(
              "API token found in URL query string but only form-encoded body parameters are allowed"));
    }

    if (allValues.length == 1) {
      return allValues[0].toCharArray();
    }

    throw new ApiTokenAuthenticationException(
        ApiTokenErrors.invalidRequest("Found multiple Api tokens in the request"));
  }

  private boolean validateParameterToken(HttpServletRequest request, char[] parameterToken) {
    if (parameterToken.length > 0) {
      String method = request.getMethod();
      if ("POST".equals(method) || "GET".equals(method)) {
        validateChecksum(parameterToken);
        return true;
      }
      throw new ApiTokenAuthenticationException(
          ApiTokenErrors.invalidRequest(
              "API token source not allowed for this request method and configuration"));
    }
    return false;
  }

  private static void validateChecksum(char[] token) {
    try {
      if (!isValidTokenChecksum(token)) {
        throw new ApiTokenAuthenticationException(
            ApiTokenErrors.invalidRequest(CHECKSUM_VALIDATION_FAILED));
      }
    } catch (ApiTokenAuthenticationException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiTokenAuthenticationException(
          ApiTokenErrors.invalidRequest(CHECKSUM_VALIDATION_FAILED));
    }
  }

  private static String hashToken(char[] tokenInHeader) {
    try {
      return ApiKeyTokenGenerator.hashToken(tokenInHeader);
    } catch (Exception e) {
      throw new ApiTokenAuthenticationException(
          ApiTokenErrors.invalidRequest("Could not hash token"));
    }
  }
}
