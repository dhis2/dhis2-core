/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpServletRequestPaths {
  private static final Pattern API_VERSION = Pattern.compile("(/api/(\\d+)?/)");

  /** Configured fallback from server.base.url in dhis.conf */
  private static volatile String configuredFallbackBaseUrl;

  /** Learned fallback from the first request that had X-Forwarded-Host headers */
  private static volatile String learnedFallbackBaseUrl;

  private HttpServletRequestPaths() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Set a fallback base URL (from server.base.url in dhis.conf) to use when X-Forwarded-Host is not
   * present. This handles cases like internal RequestDispatcher forwards where proxy headers are
   * lost.
   */
  public static void setFallbackBaseUrl(String url) {
    if (url != null && !url.isEmpty()) {
      // Strip trailing slash for consistency
      configuredFallbackBaseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
  }

  public static String getApiPath(HttpServletRequest request) {
    Matcher matcher = API_VERSION.matcher(request.getRequestURI());
    String version = "";

    if (matcher.find()) {
      version = "/" + matcher.group(2);
    }

    return getContextPath(request) + request.getServletPath() + "/api" + version;
  }

  public static String getServletPath(HttpServletRequest request) {
    return getContextPath(request) + request.getServletPath();
  }

  public static String getContextPath(HttpServletRequest request) {
    boolean hasForwardedHeaders = hasForwardedHeaders(request);

    if (!hasForwardedHeaders) {
      String fallback = getFallbackBaseUrl();
      if (fallback != null) {
        return fallback;
      }
    }

    String scheme = resolveScheme(request);
    String hostname = resolveHostname(request, hasForwardedHeaders);
    int port = resolvePort(request);

    StringBuilder builder = new StringBuilder();
    builder.append(scheme).append("://").append(hostname);

    if (port != 80 && port != 443) {
      builder.append(":").append(port);
    }

    builder.append(request.getContextPath());

    String result = builder.toString();

    if (hasForwardedHeaders && learnedFallbackBaseUrl == null) {
      learnedFallbackBaseUrl = result;
    }

    return result;
  }

  private static boolean hasForwardedHeaders(HttpServletRequest request) {
    String xForwardedHost = request.getHeader("X-Forwarded-Host");
    return xForwardedHost != null && !xForwardedHost.isEmpty();
  }

  /** Returns the configured or learned fallback base URL, or null if none is available. */
  private static String getFallbackBaseUrl() {
    if (configuredFallbackBaseUrl != null) {
      return configuredFallbackBaseUrl;
    }
    return learnedFallbackBaseUrl;
  }

  /** Uses X-Forwarded-Proto if it's a valid HTTP scheme, otherwise falls back to request scheme. */
  private static String resolveScheme(HttpServletRequest request) {
    String xForwardedProto = request.getHeader("X-Forwarded-Proto");
    if (xForwardedProto != null
        && (xForwardedProto.equalsIgnoreCase("http")
            || xForwardedProto.equalsIgnoreCase("https"))) {
      return xForwardedProto;
    }
    return request.getScheme();
  }

  /**
   * Extracts the hostname from X-Forwarded-Host (stripping any port suffix), or falls back to the
   * server name when no forwarded headers are present. Handles IPv6 addresses in bracket notation.
   */
  private static String resolveHostname(HttpServletRequest request, boolean hasForwardedHeaders) {
    if (!hasForwardedHeaders) {
      return request.getServerName();
    }

    String host = request.getHeader("X-Forwarded-Host");

    // IPv6 with port, e.g. "[::1]:8080" — strip the port after the closing bracket
    if (host.contains("]:")) {
      return host.substring(0, host.indexOf("]:") + 1);
    }

    // IPv4 or hostname with port, e.g. "example.com:8080" — strip the port
    if (host.contains(":") && !host.startsWith("[")) {
      return host.substring(0, host.indexOf(":"));
    }

    return host;
  }

  /** Uses X-Forwarded-Port if parseable, otherwise falls back to the server port. */
  private static int resolvePort(HttpServletRequest request) {
    String xForwardedPort = request.getHeader("X-Forwarded-Port");
    try {
      return Integer.parseInt(xForwardedPort);
    } catch (NumberFormatException e) {
      return request.getServerPort();
    }
  }
}
