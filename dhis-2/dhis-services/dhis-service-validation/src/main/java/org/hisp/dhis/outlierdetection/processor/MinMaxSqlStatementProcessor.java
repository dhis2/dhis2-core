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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.util.OutlierDetectionUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

@Component
public class MinMaxSqlStatementProcessor implements OutlierSqlStatementProcessor {

  /**
   * The function retries the sql statement for inspection of outliers. Min and max values are
   * inspected.
   *
   * @param request the instance of {@link OutlierDetectionRequest}.
   * @return sql statement for the outlier detection and related data
   */
  @Override
  public String getSqlStatement(OutlierDetectionRequest request) {
    if (request == null) {
      return StringUtils.EMPTY;
    }

    String ouPathClause = OutlierDetectionUtils.getOrgUnitPathClause(request.getOrgUnits(), "ou");

    return "select de.uid as de_uid, ou.uid as ou_uid, coc.uid as coc_uid, aoc.uid as aoc_uid, "
        + "de.name as de_name, ou.name as ou_name, coc.name as coc_name, aoc.name as aoc_name, "
        + "pe.startdate as pe_start_date, pt.name as pt_name, "
        + "dv.value::double precision as value, dv.followup as follow_up, "
        + "least(abs(dv.value::double precision - mm.minimumvalue), "
        + "abs(dv.value::double precision - mm.maximumvalue)) as bound_abs_dev, "
        + "mm.minimumvalue as lower_bound, "
        + "mm.maximumvalue as upper_bound "
        + "from datavalue dv "
        + "inner join dataelement de on dv.dataelementid = de.dataelementid "
        + "inner join categoryoptioncombo coc on dv.categoryoptioncomboid = coc.categoryoptioncomboid "
        + "inner join categoryoptioncombo aoc on dv.attributeoptioncomboid = aoc.categoryoptioncomboid "
        + "inner join period pe on dv.periodid = pe.periodid "
        + "inner join periodtype pt on pe.periodtypeid = pt.periodtypeid "
        + "inner join organisationunit ou on dv.sourceid = ou.organisationunitid "
        +
        // Min-max value join
        "inner join minmaxdataelement mm on (dv.dataelementid = mm.dataelementid "
        + "and dv.sourceid = mm.sourceid and dv.categoryoptioncomboid = mm.categoryoptioncomboid) "
        + "where dv.dataelementid in (:data_element_ids) "
        + "and pe.startdate >= :start_date "
        + "and pe.enddate <= :end_date "
        + "and "
        + ouPathClause
        + " "
        + "and dv.deleted is false "
        +
        // Filter for values outside the min-max range
        "and (dv.value::double precision < mm.minimumvalue or dv.value::double precision > mm.maximumvalue) "
        +
        // Order and limit
        "order by bound_abs_dev desc "
        + "limit :max_results;";
  }

  /**
   * To avoid the sql injection and decrease the load of the database engine (query plan caching)
   * the named params are in use.
   *
   * @param request the instance of {@link OutlierDetectionRequest}.
   * @return named params for parametrized sql query
   */
  @Override
  public SqlParameterSource getSqlParameterSource(OutlierDetectionRequest request) {
    return new MapSqlParameterSource()
        .addValue(DATA_ELEMENT_IDS.getKey(), request.getDataElementIds())
        .addValue(START_DATE.getKey(), request.getStartDate())
        .addValue(END_DATE.getKey(), request.getEndDate())
        .addValue(MAX_RESULTS.getKey(), request.getMaxResults());
  }
}
