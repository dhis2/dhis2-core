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

import static org.hisp.dhis.security.Authorities.F_SYSTEM_SETTING;
import static org.hisp.dhis.security.Authorities.M_DHIS_WEB_APP_MANAGEMENT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.security.RequiresAuthority;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Per-app, per-entrypoint CSP overrides and the deduplicated violation reports (prototype). Changes
 * take effect on the next request of the app; no restart.
 *
 * @author Morten Svanæs
 */
@OpenApi.Document(
    entity = App.class,
    classifiers = {"team:platform", "purpose:support"})
@RestController
@RequestMapping("/api/security/csp")
@RequiredArgsConstructor
public class CspAppOverrideController {
  private final CspAppOverrideService service;
  private final CspViolationService violationService;

  @GetMapping(value = "/apps", produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = {F_SYSTEM_SETTING, M_DHIS_WEB_APP_MANAGEMENT})
  public CspAppOverrideService.Summary getApps(HttpServletRequest request) {
    return service.summary(request.getContextPath());
  }

  @GetMapping(value = "/apps/{appKey}", produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = {F_SYSTEM_SETTING, M_DHIS_WEB_APP_MANAGEMENT})
  public CspAppOverrideService.Detail getApp(
      @PathVariable("appKey") String appKey, HttpServletRequest request) throws NotFoundException {
    return service.detail(appKey, request.getContextPath());
  }

  @PutMapping(
      value = "/apps/{appKey}",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = F_SYSTEM_SETTING)
  public ResponseEntity<Object> putApp(
      @PathVariable("appKey") String appKey,
      @RequestBody CspAppOverride.Request body,
      HttpServletRequest request)
      throws NotFoundException {
    try {
      Map<String, Object> saved = service.save(appKey, body, request.getContextPath());
      return ResponseEntity.ok(saved);
    } catch (CspAppOverrideService.RejectedException e) {
      return ResponseEntity.status(e.getStatus()).body(e.getBody());
    }
  }

  @PostMapping(
      value = "/apps/{appKey}/validate",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = {F_SYSTEM_SETTING, M_DHIS_WEB_APP_MANAGEMENT})
  public CspAppOverrideService.ValidateResponse validateApp(
      @PathVariable("appKey") String appKey,
      @RequestBody CspAppOverride.Request body,
      HttpServletRequest request)
      throws NotFoundException {
    return service.validate(appKey, body, request.getContextPath());
  }

  @DeleteMapping("/apps/{appKey}")
  @RequiresAuthority(anyOf = F_SYSTEM_SETTING)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteApp(@PathVariable("appKey") String appKey, HttpServletRequest request)
      throws NotFoundException {
    service.delete(appKey, request.getContextPath());
  }

  public record Violations(@JsonProperty List<CspViolationService.Violation> violations) {}

  @GetMapping(value = "/reports", produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = {F_SYSTEM_SETTING, M_DHIS_WEB_APP_MANAGEMENT})
  public Violations getReports(@RequestParam(value = "app", required = false) String app) {
    return new Violations(violationService.list(app));
  }

  @GetMapping(value = "/apps/{appKey}/reports", produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = {F_SYSTEM_SETTING, M_DHIS_WEB_APP_MANAGEMENT})
  public Violations getAppReports(@PathVariable("appKey") String appKey, HttpServletRequest request)
      throws NotFoundException {
    String key = reportKey(appKey, request);
    return new Violations(violationService.list(key));
  }

  @DeleteMapping("/apps/{appKey}/reports")
  @RequiresAuthority(anyOf = F_SYSTEM_SETTING)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteAppReports(@PathVariable("appKey") String appKey, HttpServletRequest request)
      throws NotFoundException {
    violationService.clear(reportKey(appKey, request));
  }

  private String reportKey(String appKey, HttpServletRequest request) throws NotFoundException {
    return CspViolationService.PLATFORM_APP_KEY.equals(appKey)
        ? appKey
        : service.requireApp(appKey, request.getContextPath()).getKey();
  }
}
