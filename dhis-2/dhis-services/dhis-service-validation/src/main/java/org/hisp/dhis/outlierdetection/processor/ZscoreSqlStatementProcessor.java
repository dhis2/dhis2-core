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
import static org.hisp.dhis.outlierdetection.OutliersSqlParamName.DATA_END_DATE;
import static org.hisp.dhis.outlierdetection.OutliersSqlParamName.DATA_START_DATE;
import static org.hisp.dhis.outlierdetection.OutliersSqlParamName.END_DATE;
import static org.hisp.dhis.outlierdetection.OutliersSqlParamName.MAX_RESULTS;
import static org.hisp.dhis.outlierdetection.OutliersSqlParamName.START_DATE;
import static org.hisp.dhis.outlierdetection.OutliersSqlParamName.THRESHOLD;

import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.outlierdetection.Order;
import org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.util.OutlierDetectionUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

@Component
public class ZscoreSqlStatementProcessor implements OutlierSqlStatementProcessor {

  @Override
  public String getSqlStatement(OutlierDetectionRequest request) {
    if (request == null) {
      return StringUtils.EMPTY;
    }

    String ouPathClause = OutlierDetectionUtils.getOrgUnitPathClause(request.getOrgUnits(), "ou");
    String dataStartDateClause = getDataStartDateClause(request.getDataStartDate());
    String dataEndDateClause = getDataEndDateClause(request.getDataEndDate());

    boolean modifiedZ = request.getAlgorithm() == OutlierDetectionAlgorithm.MOD_Z_SCORE;
    String middleStatsCalc =
        modifiedZ
            ? "percentile_cont(0.5) within group(order by dv.value::double precision)"
            : "avg(dv.value::double precision)";

    String order =
        request.getOrderBy() == Order.MEAN_ABS_DEV
            ? "middle_value_abs_dev"
            : request.getOrderBy().getKey();

    String thresholdParam = THRESHOLD.getKey();

    return "select dvs.de_uid, dvs.ou_uid, dvs.coc_uid, dvs.aoc_uid, "
        + "dvs.de_name, dvs.ou_name, dvs.coc_name, dvs.aoc_name, dvs.value, dvs.follow_up, "
        + "dvs.pe_start_date, dvs.pt_name, "
        + "stats.middle_value as middle_value, "
        + "stats.std_dev as std_dev, "
        + "abs(dvs.value::double precision - stats.middle_value) as middle_value_abs_dev, "
        + "abs(dvs.value::double precision - stats.middle_value) / stats.std_dev as z_score, "
        + "stats.middle_value - (stats.std_dev * :"
        + thresholdParam
        + ") as lower_bound, "
        + "stats.middle_value + (stats.std_dev * :"
        + thresholdParam
        + ") as upper_bound "
        +
        // Data value query
        "from ("
        + "select dv.dataelementid, dv.sourceid, dv.periodid, "
        + "dv.categoryoptioncomboid, dv.attributeoptioncomboid, "
        + "de.uid as de_uid, ou.uid as ou_uid, coc.uid as coc_uid, aoc.uid as aoc_uid, "
        + "de.name as de_name, ou.name as ou_name, coc.name as coc_name, aoc.name as aoc_name, "
        + "pe.startdate as pe_start_date, pt.name as pt_name, "
        + "dv.value as value, dv.followup as follow_up "
        + "from datavalue dv "
        + "inner join dataelement de on dv.dataelementid = de.dataelementid "
        + "inner join categoryoptioncombo coc on dv.categoryoptioncomboid = coc.categoryoptioncomboid "
        + "inner join categoryoptioncombo aoc on dv.attributeoptioncomboid = aoc.categoryoptioncomboid "
        + "inner join period pe on dv.periodid = pe.periodid "
        + "inner join periodtype pt on pe.periodtypeid = pt.periodtypeid "
        + "inner join organisationunit ou on dv.sourceid = ou.organisationunitid "
        + "where dv.dataelementid in (:"
        + DATA_ELEMENT_IDS.getKey()
        + ") "
        + "and pe.startdate >= :"
        + START_DATE.getKey()
        + " "
        + "and pe.enddate <= :"
        + END_DATE.getKey()
        + " "
        + "and "
        + ouPathClause
        + " "
        + "and dv.deleted is false"
        + ") as dvs "
        +
        // Mean or Median and std dev mapping query
        "inner join ("
        + "select dv.dataelementid as dataelementid, dv.sourceid as sourceid, "
        + "dv.categoryoptioncomboid as categoryoptioncomboid, "
        + "dv.attributeoptioncomboid as attributeoptioncomboid, "
        + middleStatsCalc
        + " as middle_value, "
        + "stddev_pop(dv.value::double precision) as std_dev "
        + "from datavalue dv "
        + "inner join period pe on dv.periodid = pe.periodid "
        + "inner join organisationunit ou on dv.sourceid = ou.organisationunitid "
        + "where dv.dataelementid in (:"
        + DATA_ELEMENT_IDS.getKey()
        + ") "
        + dataStartDateClause
        + dataEndDateClause
        + "and "
        + ouPathClause
        + " "
        + "and dv.deleted is false "
        + "group by dv.dataelementid, dv.sourceid, dv.categoryoptioncomboid, dv.attributeoptioncomboid"
        + ") as stats "
        +
        // Query join
        "on dvs.dataelementid = stats.dataelementid "
        + "and dvs.sourceid = stats.sourceid "
        + "and dvs.categoryoptioncomboid = stats.categoryoptioncomboid "
        + "and dvs.attributeoptioncomboid = stats.attributeoptioncomboid "
        + "where stats.std_dev != 0.0 "
        +
        // Filter on z-score threshold
        "and (abs(dvs.value::double precision - stats.middle_value) / stats.std_dev) >= :"
        + thresholdParam
        + " "
        +
        // Order and limit
        "order by "
        + order
        + " desc "
        + "limit :"
        + MAX_RESULTS.getKey()
        + ";";
  }

  @Override
  public SqlParameterSource getSqlParameterSource(OutlierDetectionRequest request) {
    return new MapSqlParameterSource()
        .addValue(THRESHOLD.getKey(), request.getThreshold())
        .addValue(DATA_ELEMENT_IDS.getKey(), request.getDataElementIds())
        .addValue(START_DATE.getKey(), request.getStartDate())
        .addValue(END_DATE.getKey(), request.getEndDate())
        .addValue(DATA_START_DATE.getKey(), request.getDataStartDate())
        .addValue(DATA_END_DATE.getKey(), request.getDataEndDate())
        .addValue(MAX_RESULTS.getKey(), request.getMaxResults());
  }

  private String getDataStartDateClause(Date dataStartDate) {
    return dataStartDate != null
        ? "and pe.startdate >= :" + DATA_START_DATE.getKey() + " "
        : StringUtils.EMPTY;
  }

  private String getDataEndDateClause(Date dataStartDate) {
    return dataStartDate != null
        ? "and pe.enddate <= :" + DATA_END_DATE.getKey() + " "
        : StringUtils.EMPTY;
  }
}
