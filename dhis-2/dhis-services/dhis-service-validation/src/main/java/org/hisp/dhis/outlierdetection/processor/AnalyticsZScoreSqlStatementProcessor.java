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

import static org.hisp.dhis.outlierdetection.OutliersSqlParamName.DATA_ELEMENT_IDS;
import static org.hisp.dhis.outlierdetection.OutliersSqlParamName.END_DATE;
import static org.hisp.dhis.outlierdetection.OutliersSqlParamName.MAX_RESULTS;
import static org.hisp.dhis.outlierdetection.OutliersSqlParamName.START_DATE;
import static org.hisp.dhis.outlierdetection.OutliersSqlParamName.THRESHOLD;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.outlierdetection.Order;
import org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutliersSqlParamName;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

@Component
@Qualifier("AnalyticsZScoreSqlProcessor")
public class AnalyticsZScoreSqlStatementProcessor extends AbstractOutlierSqlStatementProcessor {

  @Override
  public String getSqlStatement(OutlierDetectionRequest request) {
    if (request == null) {
      return StringUtils.EMPTY;
    }

    String ouPathClause = getOrgUnitPathClause(request.getOrgUnits());

    boolean modifiedZ = request.getAlgorithm() == OutlierDetectionAlgorithm.MOD_Z_SCORE;

    String middleValue = modifiedZ ? " ax.percentile_middle_value" : " ax.avg_middle_value";

    String order =
        request.getOrderBy() == Order.MEAN_ABS_DEV
            ? "middle_value_abs_dev"
            : request.getOrderBy().getKey();
    String thresholdParam = OutliersSqlParamName.THRESHOLD.getKey();

    return "select * from (select "
        + "ax.dataelementid, "
        + "ax.de_uid, "
        + "ax.ou_uid, "
        + "ax.coc_uid, "
        + "ax.aoc_uid, "
        + "ax.de_name, "
        + "ax.ou_name, "
        + "ax.coc_name, "
        + "ax.aoc_name, "
        + "ax.value, "
        + "ax.pestartdate as pe_start_date, "
        + "ax.pt_name, "
        + middleValue
        + " as middle_value, "
        + "ax.std_dev as std_dev, "
        + "abs(ax.value::double precision - "
        + middleValue
        + ") as middle_value_abs_dev, "
        + "(case when ax.std_dev = 0 then 0 "
        + "      else abs(ax.value::double precision - "
        + middleValue
        + " ) / ax.std_dev "
        + "       end) as z_score, "
        + middleValue
        + " - (ax.std_dev * :"
        + thresholdParam
        + ") as lower_bound, "
        + middleValue
        + " + (ax.std_dev * :"
        + thresholdParam
        + ") as upper_bound "
        + "from analytics ax "
        + "where dataelementid in  (:"
        + DATA_ELEMENT_IDS.getKey()
        + ") "
        + "and "
        + ouPathClause
        + " "
        + "and ax.pestartdate >= :"
        + START_DATE.getKey()
        + " "
        + "and ax.peenddate <= :"
        + END_DATE.getKey()
        + ") t1 "
        + "where t1.z_score > :"
        + thresholdParam
        + " "
        + "order by "
        + order
        + " desc "
        + "limit :"
        + MAX_RESULTS.getKey()
        + " ";
  }

  @Override
  public SqlParameterSource getSqlParameterSource(OutlierDetectionRequest request) {
    return new MapSqlParameterSource()
        .addValue(THRESHOLD.getKey(), request.getThreshold())
        .addValue(DATA_ELEMENT_IDS.getKey(), request.getDataElementIds())
        .addValue(START_DATE.getKey(), request.getStartDate())
        .addValue(END_DATE.getKey(), request.getEndDate())
        .addValue(MAX_RESULTS.getKey(), request.getMaxResults());
  }

  @Override
  protected String getOrganisationUnitAlias() {
    return "ax";
  }
}
