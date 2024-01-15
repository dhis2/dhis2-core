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
package org.hisp.dhis.analytics.outlier.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.analytics.OutlierDetectionAlgorithm.MODIFIED_Z_SCORE;
import static org.hisp.dhis.analytics.outlier.Order.MEAN_ABS_DEV;
import static org.hisp.dhis.analytics.outlier.data.OutlierSqlParams.DATA_ELEMENT_IDS;
import static org.hisp.dhis.analytics.outlier.data.OutlierSqlParams.END_DATE;
import static org.hisp.dhis.analytics.outlier.data.OutlierSqlParams.MAX_RESULTS;
import static org.hisp.dhis.analytics.outlier.data.OutlierSqlParams.START_DATE;
import static org.hisp.dhis.analytics.outlier.data.OutlierSqlParams.THRESHOLD;

import java.util.stream.Collectors;
import org.hisp.dhis.analytics.OutlierDetectionAlgorithm;
import org.hisp.dhis.analytics.outlier.OutlierHelper;
import org.hisp.dhis.analytics.outlier.OutlierSqlStatementProcessor;
import org.hisp.dhis.analytics.outlier.data.OutlierRequest;
import org.hisp.dhis.commons.util.TextUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * The class is related to outlier data detection based on z-score and modified z-score. The
 * analytics tables are used for it.
 *
 * <p>This both implements the {@link OutlierDetectionAlgorithm#Z_SCORE} and {@link
 * OutlierDetectionAlgorithm#MODIFIED_Z_SCORE}. Usual z-score uses the mean as middle value whereas
 * the modified z-score uses the median as middle value or more mathematically correct as the
 * <em>measure of central tendency</em>.
 */
@Component
public class AnalyticsZScoreSqlStatementProcessor implements OutlierSqlStatementProcessor {

  /**
   * The function retries the sql statement for inspection of outliers. Following scores are in use:
   *
   * <p>Z-Score abs(xi – μ) / σ where: xi: A single data value μ: The mean of the dataset σ: The
   * standard deviation of the dataset
   *
   * <p>Modified z-score = 0.6745 * abs(xi – x̃) / MAD where: xi: A single data value x̃: The median
   * of the dataset MAD: The median absolute deviation of the dataset 0.6745: conversion factor
   * (0.75 percentiles) *
   *
   * @param request the instance of {@link OutlierRequest}.
   * @return sql statement for the outlier detection and related data
   */
  @Override
  public String getSqlStatement(OutlierRequest request) {
    return getSqlStatement(request, true);
  }

  /**
   * The function retries the sql statement for inspection of outliers. Following scores are in use:
   *
   * <p>Z-Score abs(xi – μ) / σ where: xi: A single data value μ: The mean of the dataset σ: The
   * standard deviation of the dataset
   *
   * <p>Modified z-score = 0.6745 * abs(xi – x̃) / MAD where: xi: A single data value x̃: The median
   * of the dataset MAD: The median absolute deviation of the dataset 0.6745: conversion factor
   * (0.75 percentiles) *
   *
   * @param request the instance of {@link OutlierRequest}.
   * @return sql statement for the outlier detection and related data
   */
  @Override
  public String getPlainSqlStatement(OutlierRequest request) {
    return getSqlStatement(request, false);
  }

  /**
   * To avoid the sql injection and decrease the load of the database engine (query plan caching)
   * the named params are in use.
   *
   * @param request the instance of {@link OutlierRequest}.
   * @return named params for parametrized sql query
   */
  @Override
  public SqlParameterSource getSqlParameterSource(OutlierRequest request) {
    MapSqlParameterSource sqlParameterSource =
        new MapSqlParameterSource()
            .addValue(THRESHOLD.getKey(), request.getThreshold())
            .addValue(DATA_ELEMENT_IDS.getKey(), request.getDataElementIds())
            .addValue(MAX_RESULTS.getKey(), request.getMaxResults());

    if (request.hasStartEndDate()) {
      sqlParameterSource
          .addValue(START_DATE.getKey(), request.getStartDate())
          .addValue(END_DATE.getKey(), request.getEndDate());
    } else if (request.hasPeriods()) {
      for (int i = 0; i < request.getPeriods().size(); i++) {
        sqlParameterSource
            .addValue(START_DATE.getKey() + i, request.getPeriods().get(i).getStartDate())
            .addValue(END_DATE.getKey() + i, request.getPeriods().get(i).getEndDate());
      }
    }

    return sqlParameterSource;
  }

  /**
   * The method retrieves the sql query
   *
   * @param request the {@link OutlierRequest}.
   * @param withParams indicates the parametrized sql query
   * @return string with sql query
   */
  private String getSqlStatement(OutlierRequest request, boolean withParams) {
    if (request == null) {
      return EMPTY;
    }

    String ouPathClause = OutlierHelper.getOrgUnitPathClause(request.getOrgUnits(), "ax");

    boolean modifiedZ = request.getAlgorithm() == MODIFIED_Z_SCORE;

    String middleValue = modifiedZ ? " ax.percentile_middle_value" : " ax.avg_middle_value";

    String order =
        request.getOrderBy() == MEAN_ABS_DEV
            ? "middle_value_abs_dev"
            : request.getOrderBy().getColumnName();

    String thresholdParam =
        withParams ? ":" + THRESHOLD.getKey() : Double.toString(request.getThreshold());

    String sql =
        "select * from (select "
            + "ax.dx as de_uid, "
            + "ax.ou as ou_uid, "
            + "ax.co as coc_uid, "
            + "ax.ao as aoc_uid, "
            + "ax.value, "
            + "ax.pestartdate as pe_start_date, "
            + "ax.petype as pt_name, "
            + middleValue
            + " as middle_value, "
            + "ax.std_dev, "
            + "ax.mad, "
            + "abs(ax.value::double precision - "
            + middleValue
            + ") as middle_value_abs_dev, ";
    if (modifiedZ) {
      sql +=
          "(case when ax.mad = 0 then 0 "
              + "      else 0.6745 * abs(ax.value::double precision - "
              + middleValue
              + " ) / ax.mad "
              + "       end) as z_score, ";
    } else {
      sql +=
          "(case when ax.std_dev = 0 then 0 "
              + "      else abs(ax.value::double precision - "
              + middleValue
              + " ) / ax.std_dev "
              + "       end) as z_score, ";
    }
    sql +=
        middleValue
            + " - (ax.std_dev * "
            + thresholdParam
            + ") as lower_bound, "
            + middleValue
            + " + (ax.std_dev * "
            + thresholdParam
            + ") as upper_bound "
            + "from analytics ax "
            + "where dataelementid in  ("
            + (withParams
                ? ":" + DATA_ELEMENT_IDS.getKey()
                : request.getDataElementIds().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(",")))
            + ") "
            + "and "
            + ouPathClause
            + getPeriodSqlSnippet(request, withParams)
            + ") t1 "
            + "where t1.z_score > "
            + thresholdParam
            + " order by "
            + order
            + " "
            + request.getSortOrder().getValue()
            + " limit "
            + (withParams ? ":" + MAX_RESULTS.getKey() : request.getMaxResults())
            + " ";

    return sql;
  }

  /**
   * The method retrieves the sql snippet of the period dedicated sql predicate
   *
   * @param request the {@link OutlierRequest}.
   * @param withParams indicates the parametrized sql query
   * @return period part of the where clause
   */
  private String getPeriodSqlSnippet(OutlierRequest request, boolean withParams) {
    if (request.hasStartEndDate()) {
      return " and ax.pestartdate >= "
          + (withParams ? ":" + START_DATE.getKey() : "'" + request.getStartDate() + "'")
          + " and ax.peenddate <= "
          + (withParams ? ":" + END_DATE.getKey() : "'" + request.getEndDate() + "'");
    }

    if (!request.hasPeriods()) {
      return EMPTY;
    }

    String sql = "";
    for (int i = 0; i < request.getPeriods().size(); i++) {
      sql +=
          " ax.pestartdate >= "
              + (withParams
                  ? ":" + START_DATE.getKey() + i
                  : "'" + request.getPeriods().get(i).getStartDateString() + "'")
              + " and ax.peenddate <= "
              + (withParams
                  ? ":" + END_DATE.getKey() + i
                  : "'" + request.getPeriods().get(i).getEndDateString() + "'")
              + " or ";
    }

    sql = " and (" + TextUtils.removeLastOr(sql) + ")";

    return sql;
  }
}
