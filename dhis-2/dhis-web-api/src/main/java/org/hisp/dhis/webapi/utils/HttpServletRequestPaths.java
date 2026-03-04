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
   * Set a fallback base URL (from server.base.url in dhis.conf) to use when X-Forwarded-Host is
   * not present. This handles cases like internal RequestDispatcher forwards where proxy headers are
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
    String xForwardedHost = request.getHeader("X-Forwarded-Host");
    boolean hasForwardedHeaders = xForwardedHost != null && !xForwardedHost.isEmpty();

    // When X-Forwarded-Host is absent (e.g., internal RequestDispatcher forwards where proxy
    // headers are lost), use the best available fallback:
    // 1. Configured server.base.url from dhis.conf (explicit config takes priority)
    // 2. Learned base URL from a previous request that had forwarded headers
    if (!hasForwardedHeaders) {
      if (configuredFallbackBaseUrl != null) {
        return configuredFallbackBaseUrl;
      }
      if (learnedFallbackBaseUrl != null) {
        return learnedFallbackBaseUrl;
      }
    }

    StringBuilder builder = new StringBuilder();
    String xForwardedProto = request.getHeader("X-Forwarded-Proto");
    String xForwardedPort = request.getHeader("X-Forwarded-Port");

    if (xForwardedProto != null
        && (xForwardedProto.equalsIgnoreCase("http")
            || xForwardedProto.equalsIgnoreCase("https"))) {
      builder.append(xForwardedProto);
    } else {
      builder.append(request.getScheme());
    }

    if (!hasForwardedHeaders) {
      xForwardedHost = request.getServerName();
    } else {
      // X-Forwarded-Host can be "host:port" - extract hostname for URL building
      if (xForwardedHost.contains("]:")) {
        xForwardedHost = xForwardedHost.substring(0, xForwardedHost.indexOf("]:") + 1);
      } else if (xForwardedHost.contains(":") && !xForwardedHost.startsWith("[")) {
        xForwardedHost = xForwardedHost.substring(0, xForwardedHost.indexOf(":"));
      }
    }
    builder.append("://").append(xForwardedHost);

    int port;

    try {
      port = Integer.parseInt(xForwardedPort);
    } catch (NumberFormatException e) {
      port = request.getServerPort();
    }

    if (port != 80 && port != 443) {
      builder.append(":").append(port);
    }

    builder.append(request.getContextPath());

    String result = builder.toString();

    // Learn the base URL from the first request that has forwarded headers, so we can
    // use it as a fallback for subsequent requests that lack them (e.g., internal forwards).
    if (hasForwardedHeaders && learnedFallbackBaseUrl == null) {
      learnedFallbackBaseUrl = result;
    }

    return result;
  }
}
