/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.importSummary;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.scheduling.JobType.COMPLETE_DATA_SET_REGISTRATION_IMPORT;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataEntryKey;
import org.hisp.dhis.datavalue.DataEntryService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistrationExchangeService;
import org.hisp.dhis.dxf2.dataset.ExportParams;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobExecutionService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.webapi.webdomain.CompleteDataSetRegQueryParams;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Halvdan Hoem Grelland <halvdan@dhis2.org>
 */
@OpenApi.EntityType(CompleteDataSetRegistration.class)
@OpenApi.Document(
    entity = CompleteDataSetRegistration.class,
    classifiers = {"team:platform", "purpose:data"})
@Controller
@RequestMapping("/api/completeDataSetRegistrations")
@RequiredArgsConstructor
public class CompleteDataSetRegistrationController {

  private final CompleteDataSetRegistrationService registrationService;

  private final DataEntryService dataEntryService;

  private final IdentifiableObjectManager manager;

  private final OrganisationUnitService organisationUnitService;

  private final InputUtils inputUtils;

  private final CompleteDataSetRegistrationExchangeService registrationExchangeService;

  private final JobExecutionService jobExecutionService;

  // -------------------------------------------------------------------------
  // GET
  // -------------------------------------------------------------------------

  @GetMapping(produces = CONTENT_TYPE_JSON)
  public void getCompleteRegistrationsJson(
      CompleteDataSetRegQueryParams queryParams, IdSchemes idSchemes, HttpServletResponse response)
      throws IOException {
    response.setContentType(CONTENT_TYPE_JSON);
    ExportParams params = getExportParams(queryParams, idSchemes);
    registrationExchangeService.writeCompleteDataSetRegistrationsJson(
        params, response.getOutputStream());
  }

  @GetMapping(produces = CONTENT_TYPE_XML)
  public void getCompleteRegistrationsXml(
      CompleteDataSetRegQueryParams queryParams, IdSchemes idSchemes, HttpServletResponse response)
      throws IOException {
    response.setContentType(CONTENT_TYPE_XML);
    ExportParams params = getExportParams(queryParams, idSchemes);
    registrationExchangeService.writeCompleteDataSetRegistrationsXml(
        params, response.getOutputStream());
  }

  // -------------------------------------------------------------------------
  // POST
  // -------------------------------------------------------------------------

  @PostMapping(consumes = CONTENT_TYPE_XML, produces = CONTENT_TYPE_XML)
  @ResponseBody
  public WebMessage postCompleteRegistrationsXml(
      ImportOptions importOptions, HttpServletRequest request)
      throws IOException, ConflictException {
    if (importOptions.isAsync()) {
      return asyncImport(importOptions, APPLICATION_XML, request);
    }
    ImportSummary summary =
        registrationExchangeService.saveCompleteDataSetRegistrationsXml(
            request.getInputStream(), importOptions);
    summary.setImportOptions(importOptions);
    return importSummary(summary);
  }

  @PostMapping(consumes = CONTENT_TYPE_JSON, produces = CONTENT_TYPE_JSON)
  @ResponseBody
  public WebMessage postCompleteRegistrationsJson(
      ImportOptions importOptions, HttpServletRequest request)
      throws IOException, ConflictException {
    if (importOptions.isAsync()) {
      return asyncImport(importOptions, APPLICATION_JSON, request);
    }
    ImportSummary summary =
        registrationExchangeService.saveCompleteDataSetRegistrationsJson(
            request.getInputStream(), importOptions);
    summary.setImportOptions(importOptions);
    return importSummary(summary);
  }

  // -------------------------------------------------------------------------
  // DELETE
  // -------------------------------------------------------------------------

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteCompleteDataSetRegistration(
      @RequestParam Set<String> ds,
      @RequestParam String pe,
      @RequestParam String ou,
      @RequestParam(required = false) String cc,
      @RequestParam(required = false) String cp,
      @RequestParam(required = false) boolean multiOu)
      throws WebMessageException, ConflictException {
    Set<DataSet> dataSets = new HashSet<>(manager.getByUid(DataSet.class, ds));

    if (dataSets.size() != ds.size()) {
      throw new WebMessageException(conflict("Illegal data set identifier in this list: " + ds));
    }

    Period period = PeriodType.getPeriodFromIsoString(pe);

    if (period == null) {
      throw new WebMessageException(conflict("Illegal period identifier: " + pe));
    }

    OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit(ou);

    if (organisationUnit == null) {
      throw new WebMessageException(conflict("Illegal organisation unit identifier: " + ou));
    }

    CategoryOptionCombo attributeOptionCombo = inputUtils.getAttributeOptionCombo(cc, cp, false);

    if (attributeOptionCombo == null) {
      return;
    }

    // ---------------------------------------------------------------------
    // Check locked status
    // ---------------------------------------------------------------------
    List<String> lockedDataSets = new ArrayList<>();

    Collection<OrganisationUnit> orgUnitsToCheck =
        multiOu ? organisationUnit.getChildren() : List.of(organisationUnit);
    for (DataSet dataSet : dataSets) {
      UID de =
          UID.of(
              dataSet
                  .getDataElements()
                  .iterator()
                  .next()); // de is not relevant but required, so we use any
      for (OrganisationUnit orgUnit : orgUnitsToCheck) {
        DataEntryKey key =
            new DataEntryKey(de, UID.of(orgUnit), null, UID.of(attributeOptionCombo), pe);
        if (!dataEntryService.getEntryStatus(UID.of(dataSet), key).isOpen()) {
          lockedDataSets.add(dataSet.getUid());
          break;
        }
      }
    }

    if (!lockedDataSets.isEmpty()) {
      throw new WebMessageException(
          conflict("Locked Data set(s) : " + StringUtils.join(lockedDataSets, ", ")));
    }

    // ---------------------------------------------------------------------
    // Un-register as completed data set
    // ---------------------------------------------------------------------

    Set<OrganisationUnit> orgUnits = new HashSet<>();
    orgUnits.add(organisationUnit);

    if (multiOu) {
      orgUnits.addAll(organisationUnit.getChildren());
    }

    unRegisterCompleteDataSet(dataSets, period, orgUnits, attributeOptionCombo);
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private WebMessage asyncImport(
      ImportOptions importOptions, MimeType mimeType, HttpServletRequest request)
      throws IOException, ConflictException {

    JobConfiguration jobConfig = new JobConfiguration(COMPLETE_DATA_SET_REGISTRATION_IMPORT);

    jobConfig.setJobParameters(importOptions);
    jobConfig.setExecutedBy(CurrentUserUtil.getCurrentUserDetails().getUid());
    jobExecutionService.executeOnceNow(jobConfig, mimeType, request.getInputStream());

    return jobConfigurationReport(jobConfig);
  }

  private void unRegisterCompleteDataSet(
      Set<DataSet> dataSets,
      Period period,
      Set<OrganisationUnit> orgUnits,
      CategoryOptionCombo attributeOptionCombo) {
    List<CompleteDataSetRegistration> registrations = new ArrayList<>();

    for (OrganisationUnit unit : orgUnits) {
      for (DataSet dataSet : dataSets) {
        if (unit.getDataSets().contains(dataSet)) {
          CompleteDataSetRegistration registration =
              registrationService.getCompleteDataSetRegistration(
                  dataSet, period, unit, attributeOptionCombo);

          if (registration != null) {
            registrations.add(registration);
          }
        }
      }
    }
    if (!registrations.isEmpty()) {
      registrationService.deleteCompleteDataSetRegistrations(registrations);
    }
  }

  private ExportParams getExportParams(CompleteDataSetRegQueryParams params, IdSchemes idSchemes) {
    return registrationExchangeService.paramsFromUrl(
        params.getDataSet(),
        params.getOrgUnit(),
        params.getOrgUnitGroup(),
        params.getPeriod(),
        params.getStartDate(),
        params.getEndDate(),
        params.isChildren(),
        params.getCreated(),
        params.getCreatedDuration(),
        params.getLimit(),
        idSchemes);
  }
}
