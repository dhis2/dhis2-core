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
package org.hisp.dhis.security;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary diagnostics holder to capture request details from the login app HTML serving path.
 * This allows /api/loginConfig to report what headers were seen when the login HTML was last
 * served. TODO: Remove after debugging proxy header issue.
 */
public class LoginAppDiagnostics {
  private static final ConcurrentHashMap<String, String> lastRenderInfo = new ConcurrentHashMap<>();

  public static void capture(
      String baseUrl,
      String serverName,
      String xForwardedHost,
      String xForwardedProto,
      String xForwardedPort,
      String scheme,
      int serverPort,
      String requestClass) {
    lastRenderInfo.put("renderBaseUrl", baseUrl);
    lastRenderInfo.put("renderServerName", serverName);
    lastRenderInfo.put("renderXForwardedHost", str(xForwardedHost));
    lastRenderInfo.put("renderXForwardedProto", str(xForwardedProto));
    lastRenderInfo.put("renderXForwardedPort", str(xForwardedPort));
    lastRenderInfo.put("renderScheme", scheme);
    lastRenderInfo.put("renderServerPort", String.valueOf(serverPort));
    lastRenderInfo.put("renderRequestClass", requestClass);
    lastRenderInfo.put("renderTimestamp", java.time.Instant.now().toString());
  }

  public static ConcurrentHashMap<String, String> getLastRenderInfo() {
    return lastRenderInfo;
  }

  private static String str(String value) {
    return value != null ? value : "(null)";
  }
}
