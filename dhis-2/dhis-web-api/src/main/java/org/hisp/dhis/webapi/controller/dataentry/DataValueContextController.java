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
package org.hisp.dhis.webapi.controller.dataentry;

import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.datavalue.DataExportInputParams;
import org.hisp.dhis.datavalue.DataExportPipeline;
import org.hisp.dhis.datavalue.DataExportValue;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueChangelogEntry;
import org.hisp.dhis.datavalue.DataValueChangelogService;
import org.hisp.dhis.datavalue.DataValueQueryParams;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.webapi.webdomain.datavalue.DataValueContextDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@OpenApi.Document(
    entity = DataValue.class,
    classifiers = {"team:platform", "purpose:data-entry"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dataEntry")
public class DataValueContextController {

  private final DataValueChangelogService dataValueChangelogService;
  private final DataExportPipeline dataExportPipeline;
  private final PeriodService periodService;

  @GetMapping("/dataValueContext")
  public DataValueContextDto getChangeLog(DataValueQueryParams params) throws ConflictException {
    // treat "" as null for CC + CP (this endpoint only for backwards compatibility)
    if ("".equals(params.getCp())) params.setCp(null);
    if ("".equals(params.getCc())) params.setCc(null);

    List<DataValueChangelogEntry> entries = dataValueChangelogService.getChangelogEntries(params);

    Set<String> periods =
        periodService.getPeriods(PeriodType.getPeriodFromIsoString(params.getPe()), 13).stream()
            .map(Period::getIsoDate)
            .collect(toSet());

    List<DataExportValue> dataValues =
        dataExportPipeline.exportAsList(
            DataExportInputParams.builder()
                .dataElement(Set.of(params.getDe()))
                .period(periods)
                .orgUnit(Set.of(params.getOu()))
                .categoryOptionCombo(Set.of(params.getCo()))
                .attributeCombo(params.getCc())
                .attributeOptions(params.getCp() == null ? null : Set.of(params.getCp().split(";")))
                .orderByPeriod(true)
                .build(),
            Function.identity());

    return new DataValueContextDto().setAudits(entries).setHistory(dataValues);
  }
}
