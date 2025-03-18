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

  private HttpServletRequestPaths() {
    throw new IllegalStateException("Utility class");
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

    builder.append("://").append(request.getServerName());

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

    return builder.toString();
  }
}
