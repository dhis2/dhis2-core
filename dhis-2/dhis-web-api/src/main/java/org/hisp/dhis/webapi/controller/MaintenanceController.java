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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.importSummaries;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;

import java.util.List;
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryManager;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.util.CategoryUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.maintenance.MaintenanceService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping(value = MaintenanceController.RESOURCE_PATH)
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class MaintenanceController {
  public static final String RESOURCE_PATH = "/maintenance";

  @Autowired private MaintenanceService maintenanceService;

  @Autowired private ResourceTableService resourceTableService;

  @Autowired private AnalyticsTableGenerator analyticsTableGenerator;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private DataElementService dataElementService;

  @Autowired private List<AnalyticsTableService> analyticsTableService;

  @Autowired private CategoryManager categoryManager;

  @Autowired private CategoryUtils categoryUtils;

  @Autowired private AppManager appManager;

  @Autowired private CategoryService categoryService;

  @RequestMapping(
      value = "/analyticsTablesClear",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void clearAnalyticsTables() {
    analyticsTableService.forEach(AnalyticsTableService::dropTables);
  }

  @RequestMapping(
      value = "/analyticsTablesAnalyze",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void analyzeAnalyticsTables() {
    analyticsTableService.forEach(AnalyticsTableService::analyzeAnalyticsTables);
  }

  @RequestMapping(
      value = "/expiredInvitationsClear",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void clearExpiredInvitations() {
    maintenanceService.removeExpiredInvitations();
  }

  @RequestMapping(
      value = "/ouPathsUpdate",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void forceUpdatePaths() {
    organisationUnitService.forceUpdatePaths();
  }

  @RequestMapping(
      value = "/periodPruning",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void prunePeriods() {
    maintenanceService.prunePeriods();
  }

  @RequestMapping(
      value = "/zeroDataValueRemoval",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteZeroDataValues() {
    maintenanceService.deleteZeroDataValues();
  }

  @RequestMapping(
      value = "/softDeletedDataValueRemoval",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteSoftDeletedDataValues() {
    maintenanceService.deleteSoftDeletedDataValues();
  }

  @RequestMapping(
      value = "/softDeletedProgramStageInstanceRemoval",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteSoftDeletedProgramStageInstances() {
    maintenanceService.deleteSoftDeletedProgramStageInstances();
  }

  @RequestMapping(
      value = "/softDeletedRelationshipRemoval",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteSoftDeletedRelationships() {
    maintenanceService.deleteSoftDeletedRelationships();
  }

  @RequestMapping(
      value = "/softDeletedProgramInstanceRemoval",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteSoftDeletedProgramInstances() {
    maintenanceService.deleteSoftDeletedProgramInstances();
  }

  @RequestMapping(
      value = "/softDeletedTrackedEntityInstanceRemoval",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteSoftDeletedTrackedEntityInstances() {
    maintenanceService.deleteSoftDeletedTrackedEntityInstances();
  }

  @RequestMapping(
      value = "/sqlViewsCreate",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void createSqlViews() {
    resourceTableService.createAllSqlViews(NoopJobProgress.INSTANCE);
  }

  @RequestMapping(
      value = "/sqlViewsDrop",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void dropSqlViews() {
    resourceTableService.dropAllSqlViews(NoopJobProgress.INSTANCE);
  }

  @RequestMapping(
      value = "/categoryOptionComboUpdate",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateCategoryOptionCombos() {
    categoryManager.addAndPruneAllOptionCombos();
  }

  @RequestMapping(
      value = "/categoryOptionComboUpdate/categoryCombo/{uid}",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseBody
  public WebMessage updateCategoryOptionCombos(@PathVariable String uid) {
    CategoryCombo categoryCombo = categoryService.getCategoryCombo(uid);

    if (categoryCombo == null) {
      return conflict("CategoryCombo does not exist: " + uid);
    }

    return importSummaries(categoryUtils.addAndPruneOptionCombos(categoryCombo));
  }

  @RequestMapping(
      value = {"/cacheClear", "/cache"},
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void clearCache() {
    maintenanceService.clearApplicationCaches();
  }

  @RequestMapping(
      value = "/dataPruning/organisationUnits/{uid}",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL')")
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
  @PreAuthorize("hasRole('ALL')")
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
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseBody
  public WebMessage appReload() {
    appManager.reloadApps();
    return ok("Apps reloaded");
  }

  @RequestMapping(method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void performMaintenance(
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
      @RequestParam(required = false) boolean softDeletedTrackedEntityInstanceRemoval,
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
      deleteSoftDeletedProgramStageInstances();
    }

    if (softDeletedEnrollmentRemoval) {
      deleteSoftDeletedProgramInstances();
    }

    if (softDeletedTrackedEntityInstanceRemoval) {
      deleteSoftDeletedTrackedEntityInstances();
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
      analyticsTableGenerator.generateResourceTables(NoopJobProgress.INSTANCE);
    }
  }
}
