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
package org.hisp.dhis.outlierdetection.processor;

import static org.hisp.dhis.outlierdetection.OutliersSqlParam.DATA_ELEMENT_IDS;
import static org.hisp.dhis.outlierdetection.OutliersSqlParam.END_DATE;
import static org.hisp.dhis.outlierdetection.OutliersSqlParam.MAX_RESULTS;
import static org.hisp.dhis.outlierdetection.OutliersSqlParam.START_DATE;

import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public class AnalyticsMinMaxSqlStatementProcessor extends OutlierSqlStatementProcessor {
  @Override
  public String getSqlStatement(OutlierDetectionRequest request) {
    final String ouPathClause = getOrgUnitPathClause(request.getOrgUnits());

    return "select ax.de_uid, "
        + "       ax.ou_uid, "
        + "       ax.coc_uid, "
        + "       ax.aoc_uid, "
        + "       ax.de_name, "
        + "       ax.ou_name, "
        + "       ax.coc_name, "
        + "       ax.aoc_name, "
        + "       ax.pestartdate                                               as pe_start_date, "
        + "       ax.pt_name, "
        + "       ax.value::double precision                                   as value, "
        + "       ax.follow_up                                                 as follow_up, "
        + "       least(abs(ax.value::double precision - ax.min_value), "
        + "             abs(ax.value::double precision - ax.max_value))        as bound_abs_dev, "
        + "       ax.min_value                                                 as lower_bound, "
        + "       ax.max_value                                                 as upper_bound "
        + "from analytics ax "
        + "where ax.dataelementid in (:"
        + DATA_ELEMENT_IDS.getKey()
        + ") "
        + "  and "
        + ouPathClause
        + " "
        + "  and ax.pestartdate >= :"
        + START_DATE.getKey()
        + " "
        + "  and ax.peenddate <= :"
        + END_DATE.getKey()
        + " "
        + "  and (ax.value::double precision < ax.min_value or ax.value::double precision > ax.max_value) "
        + "order by bound_abs_dev desc "
        + "limit :"
        + MAX_RESULTS.getKey()
        + ";";
  }

  @Override
  public SqlParameterSource getSqlParameterSource(OutlierDetectionRequest request) {
    return new MapSqlParameterSource()
        .addValue(DATA_ELEMENT_IDS.getKey(), request.getDataElementIds())
        .addValue(START_DATE.getKey(), request.getStartDate())
        .addValue(END_DATE.getKey(), request.getEndDate())
        .addValue(MAX_RESULTS.getKey(), request.getMaxResults());
  }
}
