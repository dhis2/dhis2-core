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
package org.hisp.dhis.security;

import static org.hisp.dhis.webapi.filter.CustomAuthenticationFilter.PARAM_AUTH_ONLY;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mobile.device.Device;
import org.springframework.mobile.device.DeviceResolver;
import org.springframework.security.web.DefaultRedirectStrategy;

/**
 * @author mortenoh
 */
@Slf4j
public class MappedRedirectStrategy extends DefaultRedirectStrategy {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private Map<String, String> redirectMap = new HashMap<>();

  public Map<String, String> getRedirectMap() {
    return redirectMap;
  }

  public void setRedirectMap(Map<String, String> redirectMap) {
    this.redirectMap = redirectMap;
  }

  private DeviceResolver deviceResolver;

  public void setDeviceResolver(DeviceResolver deviceResolver) {
    this.deviceResolver = deviceResolver;
  }

  // -------------------------------------------------------------------------
  // DefaultRedirectStrategy implementation
  // -------------------------------------------------------------------------

  @Override
  public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url)
      throws IOException {
    // ---------------------------------------------------------------------
    // Check if redirect should be skipped - for cookie authentication only
    // ---------------------------------------------------------------------

    String authOnly = (String) request.getAttribute(PARAM_AUTH_ONLY);

    if ("true".equals(authOnly)) {
      return;
    }

    // ---------------------------------------------------------------------
    // Ignore certain ajax requests
    // ---------------------------------------------------------------------

    for (String key : redirectMap.keySet()) {
      if (url.contains(key)) {
        url = url.replaceFirst(key, redirectMap.get(key));
      }
    }

    // ---------------------------------------------------------------------
    // Redirect to mobile start pages
    // ---------------------------------------------------------------------

    Device device = deviceResolver.resolveDevice(request);

    if ((device.isMobile() || device.isTablet())) {
      url = getRootPath(request) + "/";
    }

    log.debug("Redirecting to " + url);

    super.sendRedirect(request, response, url);
  }

  public String getRootPath(HttpServletRequest request) {
    StringBuilder builder = new StringBuilder();
    builder.append(request.getScheme());

    builder.append("://").append(request.getServerName());

    if (request.getServerPort() != 80 && request.getServerPort() != 443) {
      builder.append(":").append(request.getServerPort());
    }

    builder.append(request.getContextPath());

    return builder.toString();
  }
}
