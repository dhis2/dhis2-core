/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.security.csp.app;

import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_ENABLED;
import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_REPORT_ENABLED;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.stereotype.Component;

/**
 * Points app entrypoint policies at {@value #REPORT_PATH} with the app key and entrypoint as query
 * parameters, while {@code csp.enabled} and {@code csp.report.enabled} are on. Only {@code
 * report-uri} is emitted: browsers ignore {@code report-uri} when {@code report-to} is present, and
 * Chromium delivers Reporting API reports only to potentially trustworthy (HTTPS) origins.
 *
 * @author Morten Svanæs
 */
@Component
@RequiredArgsConstructor
public class DefaultCspReportUriProvider implements CspReportUriProvider {
  public static final String REPORT_PATH = "/api/security/csp/report";

  private final DhisConfigurationProvider config;

  @Override
  @CheckForNull
  public String reportUri(String contextPath, String appKey, String entrypoint) {
    if (!config.isEnabled(CSP_ENABLED) || !config.isEnabled(CSP_REPORT_ENABLED)) {
      return null;
    }
    return (contextPath == null ? "" : contextPath)
        + REPORT_PATH
        + "?app="
        + URLEncoder.encode(appKey, StandardCharsets.UTF_8)
        + "&ep="
        + URLEncoder.encode(entrypoint, StandardCharsets.UTF_8);
  }
}
