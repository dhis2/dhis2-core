/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.outlier.service;

import static java.lang.Double.NaN;
import static org.apache.commons.math3.util.Precision.round;
import static org.hisp.dhis.analytics.OutlierDetectionAlgorithm.MODIFIED_Z_SCORE;
import static org.hisp.dhis.analytics.outlier.OutlierHelper.withExceptionHandling;
import static org.hisp.dhis.period.PeriodType.getIsoPeriod;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.OutlierDetectionAlgorithm;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.outlier.OutlierSqlStatementProcessor;
import org.hisp.dhis.analytics.outlier.data.Outlier;
import org.hisp.dhis.analytics.outlier.data.OutlierRequest;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.ExecutionPlan;
import org.hisp.dhis.period.PeriodType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

/**
 * Manager for database queries related to outlier data detection based on z-score and modified
 * z-score.
 *
 * <p>This both implements the {@link OutlierDetectionAlgorithm#Z_SCORE} and {@link
 * OutlierDetectionAlgorithm#MODIFIED_Z_SCORE}. Usual z-score uses the mean as middle value whereas
 * the modified z-score uses the median as middle value or more mathematically correct as the
 * <em>measure of central tendency</em>.
 */
@Repository
@RequiredArgsConstructor
public class AnalyticsZScoreOutlierDetector {
  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Qualifier("analyticsZScoreSqlStatementProcessor")
  private final OutlierSqlStatementProcessor sqlStatementProcessor;

  private final ExecutionPlanStore executionPlanStore;

  /**
   * Retrieves all outliers.
   *
   * @param request the {@link OutlierRequest}.
   * @return list of the {@link Outlier} instances for api response.
   */
  public List<Outlier> getOutliers(OutlierRequest request) {
    String sql = sqlStatementProcessor.getSqlStatement(request);
    SqlParameterSource params = sqlStatementProcessor.getSqlParameterSource(request);
    Calendar calendar = PeriodType.getCalendar();
    boolean modifiedZ = request.getAlgorithm() == MODIFIED_Z_SCORE;

    return withExceptionHandling(
            () ->
                jdbcTemplate.query(
                    sql, params, getRowMapper(calendar, modifiedZ, request.isSkipRounding())))
        .orElse(List.of());
  }

  public List<ExecutionPlan> getExecutionPlans(OutlierRequest request) {
    String sql = sqlStatementProcessor.getPlainSqlStatement(request);
    executionPlanStore.addExecutionPlan(request.getExplainOrderId(), sql);

    return executionPlanStore.getExecutionPlans(request.getExplainOrderId());
  }

  /**
   * Returns a {@link RowMapper} for {@link Outlier}.
   *
   * @param calendar the {@link Calendar}.
   * @param modifiedZ boolean flag (false means z-score to be applied).
   * @param skipRounding indicates the rounding of the floating point types
   * @return a {@link RowMapper}.
   */
  private RowMapper<Outlier> getRowMapper(
      Calendar calendar, boolean modifiedZ, boolean skipRounding) {
    return (rs, rowNum) -> {
      Outlier outlier = getOutlier(calendar, rs);
      addZScoreBasedParamsToOutlier(outlier, rs, modifiedZ, skipRounding);

      return outlier;
    };
  }

  /**
   * Maps incoming database set into the api response element.
   *
   * @param calendar the {@link Calendar}.
   * @param rs the {@link ResultSet}.
   * @return single {@link Outlier} instance.
   * @throws SQLException
   */
  private Outlier getOutlier(Calendar calendar, ResultSet rs) throws SQLException {
    String isoPeriod = getIsoPeriod(calendar, rs.getString("pt_name"), rs.getDate("pe_start_date"));

    Outlier outlier = new Outlier();
    outlier.setDx(rs.getString("de_uid"));
    outlier.setPe(isoPeriod);
    outlier.setOu(rs.getString("ou_uid"));
    outlier.setCoc(rs.getString("coc_uid"));
    outlier.setAoc(rs.getString("aoc_uid"));
    outlier.setValue(rs.getDouble("value"));

    return outlier;
  }

  /**
   * The values for outlier identification are added to OutlierValue instance.
   *
   * @param outlier the {@link Outlier}.
   * @param rs the {@link ResultSet}.
   * @param modifiedZ boolean flag (false means z-score to be applied).
   * @param skipRounding indicates the rounding of the floating point types
   * @throws SQLException
   */
  private void addZScoreBasedParamsToOutlier(
      Outlier outlier, ResultSet rs, boolean modifiedZ, boolean skipRounding) throws SQLException {
    final int scale = skipRounding ? 10 : 2;
    if (modifiedZ) {
      outlier.setMedian(round(rs.getDouble("middle_value"), scale));
      outlier.setStdDev(round(rs.getDouble("mad"), scale));
    } else {
      outlier.setMean(round(rs.getDouble("middle_value"), scale));
      outlier.setStdDev(round(rs.getDouble("std_dev"), scale));
    }

    outlier.setAbsDev(round(rs.getDouble("middle_value_abs_dev"), scale));
    outlier.setZScore(round(outlier.getStdDev() == 0 ? NaN : rs.getDouble("z_score"), scale));
    outlier.setLowerBound(round(rs.getDouble("lower_bound"), scale));
    outlier.setUpperBound(round(rs.getDouble("upper_bound"), scale));
  }
}
