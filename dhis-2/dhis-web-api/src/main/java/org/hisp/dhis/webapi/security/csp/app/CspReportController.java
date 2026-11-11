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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unauthenticated intake of browser CSP violation reports (the {@code report-uri} of app entrypoint
 * policies). Bodies over {@value #MAX_BYTES} bytes are refused with 413 before parsing; everything
 * else answers 204, including reports that are dropped (unknown app, extension noise, rate limits,
 * reporting disabled), counted by the {@code dhis_csp_reports_dropped_total} metric. Accepted
 * reports only update the in-memory deduplication of {@link CspViolationService}.
 *
 * @author Morten Svanæs
 */
@Slf4j
@OpenApi.Document(
    entity = App.class,
    classifiers = {"team:platform", "purpose:support"})
@RestController
@RequestMapping("/api/security/csp")
public class CspReportController {
  public static final int MAX_BYTES = 8 * 1024;

  static final String DROPPED_METRIC = "dhis.csp.reports.dropped";

  private static final ObjectMapper MAPPER = JacksonObjectMapperConfig.staticJsonMapper();

  private final CspViolationService violationService;
  private final CspReportRateLimiter rateLimiter;
  private final AppManager appManager;
  private final MeterRegistry meterRegistry;
  private final Map<String, Counter> dropCounters = new ConcurrentHashMap<>();

  public CspReportController(
      CspViolationService violationService,
      CspReportRateLimiter rateLimiter,
      AppManager appManager,
      ObjectProvider<MeterRegistry> meterRegistry) {
    this.violationService = violationService;
    this.rateLimiter = rateLimiter;
    this.appManager = appManager;
    this.meterRegistry = meterRegistry.getIfAvailable();
  }

  @PostMapping(
      value = "/report",
      consumes = {"application/csp-report", "application/reports+json", "application/json"})
  public ResponseEntity<Void> report(
      @RequestParam(value = "app", required = false) String app,
      @RequestParam(value = "ep", required = false) String entrypoint,
      HttpServletRequest request)
      throws IOException {
    if (request.getContentLengthLong() > MAX_BYTES) {
      drop("too_large");
      return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
    }
    byte[] body = readAtMost(request.getInputStream(), MAX_BYTES + 1);
    if (body.length > MAX_BYTES) {
      drop("too_large");
      return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
    }
    if (!violationService.isReportingEnabled()) {
      drop("disabled");
      return ResponseEntity.noContent().build();
    }
    String limited = rateLimiter.tryAcquire(request.getRemoteAddr());
    if (limited != null) {
      drop(limited);
      return ResponseEntity.noContent().build();
    }
    String appKey = resolveAppKey(app, request.getContextPath());
    if (appKey == null || !CspEntrypoints.isValid(entrypoint)) {
      drop("unknown_app");
      return ResponseEntity.noContent().build();
    }
    List<CspReportParser.Raw> reports;
    try {
      reports = CspReportParser.parse(body, request.getContentType(), MAPPER);
    } catch (IOException e) {
      drop("invalid");
      return ResponseEntity.noContent().build();
    }
    if (reports.isEmpty()) {
      drop("invalid");
    }
    for (CspReportParser.Raw raw : reports) {
      CspReportParser.Outcome outcome = CspReportParser.normalize(raw, appKey, entrypoint);
      if (outcome.report() != null) {
        violationService.record(outcome.report());
      } else {
        drop(outcome.dropReason());
      }
    }
    return ResponseEntity.noContent().build();
  }

  private String resolveAppKey(String app, String contextPath) {
    if (app == null || app.isBlank()) {
      return null;
    }
    if (CspViolationService.PLATFORM_APP_KEY.equals(app)) {
      return app;
    }
    for (App installed : appManager.getAllApps(contextPath)) {
      if (app.equals(installed.getKey())) {
        return installed.getKey();
      }
    }
    return null;
  }

  private void drop(String reason) {
    log.debug("Dropped CSP report: {}", reason);
    if (meterRegistry != null) {
      dropCounters
          .computeIfAbsent(
              reason,
              r ->
                  Counter.builder(DROPPED_METRIC)
                      .description("CSP violation reports that were not recorded")
                      .tag("reason", r)
                      .register(meterRegistry))
          .increment();
    }
  }

  private static byte[] readAtMost(InputStream in, int limit) throws IOException {
    return in.readNBytes(limit);
  }
}
