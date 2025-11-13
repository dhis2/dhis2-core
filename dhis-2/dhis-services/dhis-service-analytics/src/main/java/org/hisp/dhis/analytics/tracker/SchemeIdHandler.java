/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.tracker;

import static org.hisp.dhis.common.IdScheme.NAME;

import java.util.LinkedHashSet;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo.Data;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo.Settings;
import org.hisp.dhis.analytics.data.handler.SchemeIdResponseMapper;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.Grid;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemeIdHandler {
  private final SchemeIdResponseMapper schemeIdResponseMapper;

  /**
   * @param grid the {@link Grid}.
   * @param params the {@link EventQueryParams}.
   */
  public void applyScheme(Grid grid, EventQueryParams params) {
    if (params.hasDataIdScheme()) {
      schemeIdResponseMapper.applyOptionAndLegendSetMapping(NAME, grid);
      schemeIdResponseMapper.applyBooleanMapping(params.getDataIdScheme(), grid);
    }

    if (!params.isSkipMeta()) {
      SchemeInfo schemeInfo = new SchemeInfo(schemeSettings(params), schemeData(params));
      schemeIdResponseMapper.applyCustomIdScheme(schemeInfo, grid);
    }
  }

  private Data schemeData(EventQueryParams params) {
    return Data.builder()
        .dataElements(params.getAllDataElements())
        .dimensionalItemObjects(new LinkedHashSet<>(params.getAllDimensionItems()))
        .dataElementOperands(params.getDataElementOperands())
        .options(params.getItemOptions())
        .organizationUnits(params.getOrganisationUnits())
        .program(params.getProgram())
        .programStage(params.getProgramStage())
        .indicators(params.getIndicators())
        .programIndicators(params.getProgramIndicators())
        .build();
  }

  private Settings schemeSettings(EventQueryParams params) {
    return Settings.builder()
        .dataIdScheme(params.getDataIdScheme())
        .outputDataElementIdScheme(params.getOutputDataElementIdScheme())
        .outputDataItemIdScheme(params.getOutputDataItemIdScheme())
        .outputIdScheme(params.getOutputIdScheme())
        .outputOrgUnitIdScheme(params.getOutputOrgUnitIdScheme())
        .outputFormat(params.getOutputFormat())
        .build();
  }
}
