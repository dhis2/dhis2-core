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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary diagnostics holder to capture request details from the login app HTML serving path.
 * This allows /api/loginConfig to report what headers were seen when the login HTML was last
 * served. TODO: Remove after debugging proxy header issue.
 */
public class LoginAppDiagnostics {
  private static final ConcurrentHashMap<String, String> lastRenderInfo = new ConcurrentHashMap<>();

  public static void capture(HttpServletRequest request, String baseUrl) {
    lastRenderInfo.put("renderBaseUrl", baseUrl);
    lastRenderInfo.put("renderServerName", request.getServerName());
    lastRenderInfo.put("renderXForwardedHost", str(request.getHeader("X-Forwarded-Host")));
    lastRenderInfo.put("renderXForwardedProto", str(request.getHeader("X-Forwarded-Proto")));
    lastRenderInfo.put("renderXForwardedPort", str(request.getHeader("X-Forwarded-Port")));
    lastRenderInfo.put("renderScheme", request.getScheme());
    lastRenderInfo.put("renderServerPort", String.valueOf(request.getServerPort()));
    lastRenderInfo.put("renderRequestClass", request.getClass().getName());
    lastRenderInfo.put("renderDispatcherType", String.valueOf(request.getDispatcherType()));
    lastRenderInfo.put("renderTimestamp", java.time.Instant.now().toString());
    lastRenderInfo.put(
        "renderUsedFallback",
        (request.getHeader("X-Forwarded-Host") == null
                || request.getHeader("X-Forwarded-Host").isEmpty())
            ? "true"
            : "false");

    // Unwrap the request wrapper chain to find where headers disappear
    List<String> wrapperChain = new ArrayList<>();
    HttpServletRequest r = request;
    int depth = 0;
    while (r instanceof HttpServletRequestWrapper wrapper) {
      String headerAtLayer = str(r.getHeader("X-Forwarded-Host"));
      wrapperChain.add("[" + depth + "] " + r.getClass().getName() + " -> XFH=" + headerAtLayer);
      lastRenderInfo.put("wrapperLayer_" + depth, r.getClass().getSimpleName());
      lastRenderInfo.put("wrapperLayer_" + depth + "_XFH", headerAtLayer);
      r = (HttpServletRequest) wrapper.getRequest();
      depth++;
    }
    // Innermost (unwrapped) request
    String innermostHeader = str(r.getHeader("X-Forwarded-Host"));
    wrapperChain.add(
        "[" + depth + "] " + r.getClass().getName() + " -> XFH=" + innermostHeader);
    lastRenderInfo.put("wrapperLayer_" + depth, r.getClass().getSimpleName());
    lastRenderInfo.put("wrapperLayer_" + depth + "_XFH", innermostHeader);
    lastRenderInfo.put("wrapperDepth", String.valueOf(depth));
    lastRenderInfo.put("wrapperChainSummary", String.join(" | ", wrapperChain));
  }

  public static ConcurrentHashMap<String, String> getLastRenderInfo() {
    return lastRenderInfo;
  }

  private static String str(String value) {
    return value != null ? value : "(null)";
  }
}
