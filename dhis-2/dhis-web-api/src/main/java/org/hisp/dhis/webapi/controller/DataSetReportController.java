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
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_HTML;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datasetreport.DataSetReportService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Stian Sandvold
 */
@Controller
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class DataSetReportController {
  private static final String RESOURCE_PATH = "/dataSetReport";

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  @Autowired private DataSetReportService dataSetReportService;

  @Autowired private DataSetService dataSetService;

  @Autowired private PeriodService periodService;

  @Autowired private ContextUtils contextUtils;

  @Autowired IdentifiableObjectManager idObjectManager;

  @GetMapping(value = RESOURCE_PATH + "/custom", produces = CONTENT_TYPE_HTML)
  public @ResponseBody String getCustomDataSetReport(
      HttpServletResponse response,
      @RequestParam String ou,
      @RequestParam String ds,
      @RequestParam List<String> pe,
      @RequestParam(required = false) Set<String> filter,
      @RequestParam(required = false) boolean selectedUnitOnly)
      throws Exception {
    OrganisationUnit orgUnit = getAndValidateOrgUnit(ou);
    DataSet dataSet = getAndValidateDataSet(ds);
    List<Period> periods = getAndValidatePeriods(pe);

    if (!dataSet.getFormType().isCustom()) {
      throw new WebMessageException(
          conflict("Data set form type must be 'custom': " + dataSet.getFormType()));
    }

    contextUtils.configureResponse(
        response, CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING);

    return dataSetReportService.getCustomDataSetReport(
        dataSet, periods, orgUnit, filter, selectedUnitOnly);
  }

  @GetMapping(value = RESOURCE_PATH, produces = APPLICATION_JSON_VALUE)
  public @ResponseBody List<Grid> getDataSetReportAsJson(
      HttpServletResponse response,
      @RequestParam String ou,
      @RequestParam String ds,
      @RequestParam List<String> pe,
      @RequestParam(required = false) Set<String> filter,
      @RequestParam(required = false) boolean selectedUnitOnly)
      throws Exception {
    OrganisationUnit orgUnit = getAndValidateOrgUnit(ou);
    DataSet dataSet = getAndValidateDataSet(ds);
    List<Period> periods = getAndValidatePeriods(pe);

    contextUtils.configureResponse(
        response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.RESPECT_SYSTEM_SETTING);
    return dataSetReportService.getDataSetReportAsGrid(
        dataSet, periods, orgUnit, filter, selectedUnitOnly);
  }

  @GetMapping(RESOURCE_PATH + ".xls")
  public void getDataSetReportAsExcel(
      HttpServletResponse response,
      @RequestParam String ou,
      @RequestParam String ds,
      @RequestParam List<String> pe,
      @RequestParam(required = false) Set<String> filter,
      @RequestParam(required = false) boolean selectedUnitOnly)
      throws Exception {
    OrganisationUnit orgUnit = getAndValidateOrgUnit(ou);
    DataSet dataSet = getAndValidateDataSet(ds);
    List<Period> periods = getAndValidatePeriods(pe);

    contextUtils.configureResponse(
        response, ContextUtils.CONTENT_TYPE_EXCEL, CacheStrategy.RESPECT_SYSTEM_SETTING);
    List<Grid> grids =
        dataSetReportService.getDataSetReportAsGrid(
            dataSet, periods, orgUnit, filter, selectedUnitOnly);
    GridUtils.toXls(grids, response.getOutputStream());
  }

  @GetMapping(RESOURCE_PATH + ".pdf")
  public void getDataSetReportAsPdf(
      HttpServletResponse response,
      @RequestParam String ds,
      @RequestParam List<String> pe,
      @RequestParam String ou,
      @RequestParam(required = false) Set<String> filter,
      @RequestParam(required = false) boolean selectedUnitOnly)
      throws Exception {
    OrganisationUnit orgUnit = getAndValidateOrgUnit(ou);
    DataSet dataSet = getAndValidateDataSet(ds);
    List<Period> periods = getAndValidatePeriods(pe);

    contextUtils.configureResponse(
        response, ContextUtils.CONTENT_TYPE_PDF, CacheStrategy.RESPECT_SYSTEM_SETTING);
    List<Grid> grids =
        dataSetReportService.getDataSetReportAsGrid(
            dataSet, periods, orgUnit, filter, selectedUnitOnly);
    GridUtils.toPdf(grids, response.getOutputStream());
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private OrganisationUnit getAndValidateOrgUnit(String ou) throws WebMessageException {
    OrganisationUnit orgUnit = idObjectManager.get(OrganisationUnit.class, ou);

    if (orgUnit == null) {
      throw new WebMessageException(conflict("Organisation unit does not exist: " + ou));
    }

    return orgUnit;
  }

  private DataSet getAndValidateDataSet(String ds) throws WebMessageException {
    DataSet dataSet = dataSetService.getDataSet(ds);

    if (dataSet == null) {
      throw new WebMessageException(conflict("Data set does not exist: " + ds));
    }

    return dataSet;
  }

  private List<Period> getAndValidatePeriods(List<String> pe) throws WebMessageException {
    List<Period> periods = new ArrayList<>();

    for (String p : pe) {
      Period period = PeriodType.getPeriodFromIsoString(p);

      if (period == null) {
        throw new WebMessageException(conflict("Period does not exist: " + pe));
      }

      periods.add(periodService.reloadPeriod(period));
    }

    return periods;
  }
}
