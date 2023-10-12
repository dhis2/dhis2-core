/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static java.lang.String.format;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hisp.dhis.common.IdentifiableProperty.UID;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.geojson.GeoJsonImportReport;
import org.hisp.dhis.dxf2.geojson.GeoJsonService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobSchedulerService;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.GeoJsonImportJobParams;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Jan Bernitt
 */
@OpenApi.Tags("data")
@RequestMapping("/organisationUnits")
@RestController
@RequiredArgsConstructor
public class GeoJsonImportController {
  private final GeoJsonService geoJsonService;

  private final JobSchedulerService jobSchedulerService;

  private final JobConfigurationService jobConfigurationService;

  private final CurrentUserService currentUserService;

  @PostMapping(
      value = "/geometry",
      consumes = {"application/geo+json", "application/json"})
  public WebMessage postImport(
      @RequestParam(defaultValue = "true") boolean geoJsonId,
      @RequestParam(required = false) String geoJsonProperty,
      @RequestParam(required = false) String orgUnitProperty,
      @RequestParam(required = false) String attributeId,
      @RequestParam(required = false) boolean dryRun,
      @RequestParam(required = false, defaultValue = "false") boolean async,
      HttpServletRequest request)
      throws IOException, ConflictException, NotFoundException {
    GeoJsonImportJobParams params =
        GeoJsonImportJobParams.builder()
            .attributeId(attributeId)
            .dryRun(dryRun)
            .idType(
                orgUnitProperty == null
                    ? UID
                    : IdentifiableProperty.valueOf(orgUnitProperty.toUpperCase()))
            .orgUnitIdProperty(geoJsonId ? "id" : "properties." + geoJsonProperty)
            .build();

    return runImport(async, params, request);
  }

  private WebMessage runImport(
      boolean async, GeoJsonImportJobParams params, HttpServletRequest request)
      throws ConflictException, NotFoundException, IOException {
    User currentUser = currentUserService.getCurrentUser();
    if (async) {
      JobConfiguration jobConfig = new JobConfiguration(JobType.GEOJSON_IMPORT);
      jobConfig.setJobParameters(params);
      jobConfig.setExecutedBy(currentUser.getUid());
      jobSchedulerService.executeNow(
          jobConfigurationService.create(jobConfig, APPLICATION_JSON, request.getInputStream()));

      return jobConfigurationReport(jobConfig);
    }
    params.setUser(currentUser);
    return toWebMessage(geoJsonService.importGeoData(params, request.getInputStream()));
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @DeleteMapping(value = "/geometry")
  public WebMessage deleteImport(@RequestParam(required = false) String attributeId) {
    return toWebMessage(geoJsonService.deleteGeoData(attributeId));
  }

  @PostMapping(
      value = "/{uid}/geometry",
      consumes = {"application/geo+json", "application/json"})
  public WebMessage postImportSingle(
      @PathVariable("uid") String ou,
      @RequestParam(required = false) String attributeId,
      @RequestParam(required = false) boolean dryRun,
      @RequestBody String geometry,
      @CurrentUser User currentUser) {
    GeoJsonImportJobParams params =
        GeoJsonImportJobParams.builder()
            .user(currentUser)
            .attributeId(attributeId)
            .dryRun(dryRun)
            .orgUnitIdProperty("id")
            .idType(UID)
            .build();

    return toWebMessage(
        geoJsonService.importGeoData(
            params,
            toInputStream(
                format("{\"features\":[{\"id\":\"%s\",\"geometry\":%s}]}", ou, geometry),
                StandardCharsets.UTF_8)));
  }

  @DeleteMapping(value = "/{uid}/geometry")
  public WebMessage deleteImportSingle(
      @PathVariable("uid") String ou,
      @RequestParam(required = false) String attributeId,
      @RequestParam(required = false) boolean dryRun,
      @CurrentUser User currentUser) {
    return postImportSingle(ou, attributeId, dryRun, "null", currentUser);
  }

  private WebMessage toWebMessage(GeoJsonImportReport report) {
    String msg = "Import successful.";
    Status status = Status.OK;
    if (report.getStatus() == ImportStatus.ERROR) {
      msg = "Import failed.";
      status = Status.ERROR;
    } else if (report.getImportCount().getIgnored() > 0) {
      msg = "Import partially successful.";
      status = Status.WARNING;
    }
    return ok(msg).setStatus(status).setResponse(report);
  }
}
