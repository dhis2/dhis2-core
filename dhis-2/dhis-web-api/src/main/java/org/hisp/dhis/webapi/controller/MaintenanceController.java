/*
 * Copyright (c) 2004-2022, University of Oslo
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
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.importSummaries;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.security.Authorities.ALL;
import static org.hisp.dhis.security.Authorities.F_PERFORM_MAINTENANCE;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionComboGenerateService;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.maintenance.MaintenanceService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.security.RequiresAuthority;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Document(
    entity = Server.class,
    classifiers = {"team:platform", "purpose:support"})
@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
@RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
public class MaintenanceController {

  private final MaintenanceService maintenanceService;
  private final ResourceTableService resourceTableService;
  private final AnalyticsTableGenerator analyticsTableGenerator;
  private final OrganisationUnitService organisationUnitService;
  private final DataElementService dataElementService;
  private final List<AnalyticsTableService> analyticsTableService;
  private final AppManager appManager;
  private final CategoryService categoryService;
  private final CategoryOptionComboGenerateService categoryOptionComboGenerateService;

  @RequestMapping(
      value = "/analyticsTablesClear",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseStatus(HttpStatus.OK)
  public void clearAnalyticsTables() {
    analyticsTableService.forEach(AnalyticsTableService::dropTables);
  }

  @RequestMapping(
      value = "/analyticsTablesAnalyze",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage analyzeAnalyticsTables() {
    analyticsTableService.forEach(AnalyticsTableService::analyzeAnalyticsTables);
    return WebMessageUtils.ok();
  }

  @RequestMapping(
      value = "/expiredInvitationsClear",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage clearExpiredInvitations() {
    maintenanceService.removeExpiredInvitations();
    return WebMessageUtils.ok();
  }

  @RequestMapping(
      value = "/ouPathsUpdate",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage forceUpdatePaths() {
    organisationUnitService.forceUpdatePaths();
    return WebMessageUtils.ok();
  }

  @RequestMapping(
      value = "/periodPruning",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage prunePeriods() {
    maintenanceService.prunePeriods();
    return WebMessageUtils.ok();
  }

  @RequestMapping(
      value = "/zeroDataValueRemoval",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage deleteZeroDataValues() {
    maintenanceService.deleteZeroDataValues();
    return WebMessageUtils.ok();
  }

  @RequestMapping(
      value = "/softDeletedDataValueRemoval",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage deleteSoftDeletedDataValues() {
    maintenanceService.deleteSoftDeletedDataValues();
    return WebMessageUtils.ok();
  }

  @RequestMapping(
      value = "/softDeletedEventRemoval",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage deleteSoftDeletedEvents() {
    maintenanceService.deleteSoftDeletedEvents();
    return WebMessageUtils.ok();
  }

  @RequestMapping(
      value = "/softDeletedRelationshipRemoval",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage deleteSoftDeletedRelationships() {
    maintenanceService.deleteSoftDeletedRelationships();
    return WebMessageUtils.ok();
  }

  @RequestMapping(
      value = "/softDeletedEnrollmentRemoval",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage deleteSoftDeletedEnrollments() {
    maintenanceService.deleteSoftDeletedEnrollments();
    return WebMessageUtils.ok();
  }

  @RequestMapping(
      value = "/softDeletedTrackedEntityRemoval",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage deleteSoftDeletedTrackedEntities() {
    maintenanceService.deleteSoftDeletedTrackedEntities();
    return WebMessageUtils.ok();
  }

  @RequestMapping(
      value = "/sqlViewsCreate",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage createSqlViews() {
    resourceTableService.createAllSqlViews(JobProgress.noop());
    return WebMessageUtils.ok();
  }

  @RequestMapping(
      value = "/sqlViewsDrop",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage dropSqlViews() {
    resourceTableService.dropAllSqlViews(JobProgress.noop());
    return WebMessageUtils.ok();
  }

  @RequestMapping(
      value = "/categoryOptionComboUpdate",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage updateCategoryOptionCombos() {
    categoryOptionComboGenerateService.addAndPruneAllOptionCombos();
    return WebMessageUtils.ok();
  }

  @RequestMapping(
      value = "/categoryOptionComboUpdate/categoryCombo/{uid}",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseBody
  public WebMessage updateCategoryOptionCombos(@PathVariable String uid) {
    CategoryCombo categoryCombo = categoryService.getCategoryCombo(uid);

    if (categoryCombo == null) {
      return conflict("CategoryCombo does not exist: " + uid);
    }

    return importSummaries(
        categoryOptionComboGenerateService.addAndPruneOptionCombosWithSummary(categoryCombo));
  }

  @RequestMapping(
      value = {"/cacheClear", "/cache"},
      method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage clearCache() {
    maintenanceService.clearApplicationCaches();
    return WebMessageUtils.ok();
  }

  @RequestMapping(
      value = "/dataPruning/organisationUnits/{uid}",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @RequiresAuthority(anyOf = ALL)
  @ResponseBody
  public WebMessage pruneDataByOrganisationUnit(@PathVariable String uid) {
    OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit(uid);

    if (organisationUnit == null) {
      return conflict("Organisation unit does not exist: " + uid);
    }

    return maintenanceService.pruneData(organisationUnit)
        ? ok("Data was pruned successfully")
        : conflict("Data could not be pruned");
  }

  @RequestMapping(
      value = "/dataPruning/dataElements/{uid}",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @RequiresAuthority(anyOf = ALL)
  @ResponseBody
  public WebMessage pruneDataByDataElement(@PathVariable String uid) {
    DataElement dataElement = dataElementService.getDataElement(uid);

    if (dataElement == null) {
      return conflict("Data element does not exist: " + uid);
    }

    return maintenanceService.pruneData(dataElement)
        ? ok("Data was pruned successfully")
        : conflict("Data could not be pruned");
  }

  @GetMapping("/appReload")
  @ResponseBody
  public WebMessage appReload() {
    appManager.reloadApps();
    return ok("Apps reloaded");
  }

  @RequestMapping(method = {RequestMethod.PUT, RequestMethod.POST})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage performMaintenance(
      @RequestParam(required = false) boolean analyticsTableClear,
      @RequestParam(required = false) boolean analyticsTableAnalyze,
      @RequestParam(required = false) boolean expiredInvitationsClear,
      @RequestParam(required = false) boolean ouPathsUpdate,
      @RequestParam(required = false) boolean periodPruning,
      @RequestParam(required = false) boolean zeroDataValueRemoval,
      @RequestParam(required = false) boolean softDeletedDataValueRemoval,
      @RequestParam(required = false) boolean softDeletedRelationshipRemoval,
      @RequestParam(required = false) boolean softDeletedEventRemoval,
      @RequestParam(required = false) boolean softDeletedEnrollmentRemoval,
      @RequestParam(required = false) boolean softDeletedTrackedEntityRemoval,
      @RequestParam(required = false) boolean sqlViewsDrop,
      @RequestParam(required = false) boolean sqlViewsCreate,
      @RequestParam(required = false) boolean categoryOptionComboUpdate,
      @RequestParam(required = false) boolean cacheClear,
      @RequestParam(required = false) boolean appReload,
      @RequestParam(required = false) boolean resourceTableUpdate) {
    if (analyticsTableClear) {
      clearAnalyticsTables();
    }

    if (analyticsTableAnalyze) {
      analyzeAnalyticsTables();
    }

    if (expiredInvitationsClear) {
      clearExpiredInvitations();
    }

    if (ouPathsUpdate) {
      forceUpdatePaths();
    }

    if (periodPruning) {
      prunePeriods();
    }

    if (zeroDataValueRemoval) {
      deleteZeroDataValues();
    }

    if (softDeletedDataValueRemoval) {
      deleteSoftDeletedDataValues();
    }

    if (softDeletedRelationshipRemoval) {
      deleteSoftDeletedRelationships();
    }

    if (softDeletedEventRemoval) {
      deleteSoftDeletedEvents();
    }

    if (softDeletedEnrollmentRemoval) {
      deleteSoftDeletedEnrollments();
    }

    if (softDeletedTrackedEntityRemoval) {
      deleteSoftDeletedTrackedEntities();
    }

    if (sqlViewsDrop) {
      dropSqlViews();
    }

    if (sqlViewsCreate) {
      createSqlViews();
    }

    if (categoryOptionComboUpdate) {
      updateCategoryOptionCombos();
    }

    if (cacheClear) {
      clearCache();
    }

    if (appReload) {
      appManager.reloadApps();
    }

    if (resourceTableUpdate) {
      analyticsTableGenerator.generateResourceTables(JobProgress.noop());
    }

    return WebMessageUtils.ok();
  }
}
