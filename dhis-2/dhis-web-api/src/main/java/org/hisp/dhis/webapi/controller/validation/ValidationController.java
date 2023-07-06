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
package org.hisp.dhis.webapi.controller.validation;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;

import java.util.ArrayList;
import javax.servlet.http.HttpServletResponse;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.validation.ValidationAnalysisParams;
import org.hisp.dhis.validation.ValidationService;
import org.hisp.dhis.validation.ValidationSummary;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping(value = "/validation")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class ValidationController {
  @Autowired private ValidationService validationService;

  @Autowired private DataSetService dataSetService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private CategoryService categoryService;

  @Autowired private SchedulingManager schedulingManager;

  @GetMapping("/dataSet/{ds}")
  public @ResponseBody ValidationSummary validate(
      @PathVariable String ds,
      @RequestParam String pe,
      @RequestParam String ou,
      @RequestParam(required = false) String aoc,
      HttpServletResponse response,
      Model model)
      throws WebMessageException {
    DataSet dataSet = dataSetService.getDataSet(ds);

    if (dataSet == null) {
      throw new WebMessageException(conflict("Data set does not exist: " + ds));
    }

    Period period = PeriodType.getPeriodFromIsoString(pe);

    if (period == null) {
      throw new WebMessageException(conflict("Period does not exist: " + pe));
    }

    OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(ou);

    if (orgUnit == null) {
      throw new WebMessageException(conflict("Organisation unit does not exist: " + ou));
    }

    CategoryOptionCombo attributeOptionCombo = categoryService.getCategoryOptionCombo(aoc);

    if (attributeOptionCombo == null) {
      attributeOptionCombo = categoryService.getDefaultCategoryOptionCombo();
    }

    ValidationSummary summary = new ValidationSummary();

    ValidationAnalysisParams params =
        validationService
            .newParamsBuilder(dataSet, orgUnit, period)
            .withAttributeOptionCombo(attributeOptionCombo)
            .build();

    summary.setValidationRuleViolations(
        new ArrayList<>(validationService.validationAnalysis(params)));
    summary.setCommentRequiredViolations(
        validationService.validateRequiredComments(dataSet, period, orgUnit, attributeOptionCombo));

    return summary;
  }

  @RequestMapping(
      value = "/sendNotifications",
      method = {RequestMethod.PUT, RequestMethod.POST})
  @PreAuthorize("hasRole('ALL') or hasRole('M_dhis-web-app-management')")
  @ResponseBody
  public WebMessage runValidationNotificationsTask() {
    JobConfiguration validationResultNotification =
        new JobConfiguration(
            "validation result notification from validation controller",
            JobType.VALIDATION_RESULTS_NOTIFICATION,
            "",
            null);
    validationResultNotification.setInMemoryJob(true);
    validationResultNotification.setUid(CodeGenerator.generateUid());

    schedulingManager.executeNow(validationResultNotification);

    return ok("Initiated validation result notification");
  }
}
