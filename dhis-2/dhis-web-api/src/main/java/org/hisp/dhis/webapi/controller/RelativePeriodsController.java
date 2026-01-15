/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey;
import org.hisp.dhis.common.Maturity;
import org.hisp.dhis.period.DateField;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.period.RelativePeriods;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Resolves a single relative period into its ISO period identifiers.
 *
 * @author Jason P. Pickering
 */
@Maturity(Maturity.Classification.BETA)
@RestController
@RequestMapping("/api/relativePeriods")
public class RelativePeriodsController {

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  public List<String> getRelativePeriods(
      @RequestParam RelativePeriodEnum relativePeriod,
      @RequestParam(required = false) Date date,
      @RequestParam(required = false) AnalyticsFinancialYearStartKey financialYearStart) {

    Date resolvedDate = date != null ? date : new Date();
    AnalyticsFinancialYearStartKey financialYearStartKey =
        financialYearStart != null
            ? financialYearStart
            : AnalyticsFinancialYearStartKey.FINANCIAL_YEAR_OCTOBER;

    return RelativePeriods.getRelativePeriodsFromEnum(
            relativePeriod,
            DateField.withDefaults().withDate(resolvedDate),
            null,
            false,
            financialYearStartKey)
        .stream()
        .map(PeriodDimension::getIsoDate)
        .toList();
  }
}
