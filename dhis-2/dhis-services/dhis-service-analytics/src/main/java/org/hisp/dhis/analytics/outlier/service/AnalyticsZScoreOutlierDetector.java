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

import static java.lang.Double.NaN;
import static org.hisp.dhis.analytics.OutlierDetectionAlgorithm.MOD_Z_SCORE;
import static org.hisp.dhis.analytics.outlier.OutlierHelper.withExceptionHandling;
import static org.hisp.dhis.period.PeriodType.getIsoPeriod;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.OutlierDetectionAlgorithm;
import org.hisp.dhis.analytics.outlier.OutlierSqlStatementProcessor;
import org.hisp.dhis.analytics.outlier.data.Outlier;
import org.hisp.dhis.analytics.outlier.data.OutlierRequest;
import org.hisp.dhis.calendar.Calendar;
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
 * OutlierDetectionAlgorithm#MOD_Z_SCORE}. Usual z-score uses the mean as middle value whereas the
 * modified z-score uses the median as middle value or more mathematically correct as the
 * <em>measure of central tendency</em>.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AnalyticsZScoreOutlierDetector {
  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Qualifier("analyticsZScoreSqlStatementProcessor")
  private final OutlierSqlStatementProcessor sqlStatementProcessor;

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
    boolean modifiedZ = request.getAlgorithm() == MOD_Z_SCORE;

    return withExceptionHandling(
            () -> jdbcTemplate.query(sql, params, getRowMapper(calendar, modifiedZ)))
        .orElse(List.of());
  }

  /**
   * Returns a {@link RowMapper} for {@link Outlier}.
   *
   * @param calendar the {@link Calendar}.
   * @return a {@link RowMapper}.
   */
  private RowMapper<Outlier> getRowMapper(Calendar calendar, boolean modifiedZ) {
    return (rs, rowNum) -> {
      Outlier outlier = getOutlier(calendar, rs);
      addZScoreBasedParamsToOutlier(outlier, rs, modifiedZ);

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
    outlier.setDxName(rs.getString("de_name"));
    outlier.setPe(isoPeriod);
    outlier.setOu(rs.getString("ou_uid"));
    outlier.setOuName(rs.getString("ou_name"));
    outlier.setCoc(rs.getString("coc_uid"));
    outlier.setCocName(rs.getString("coc_name"));
    outlier.setAoc(rs.getString("aoc_uid"));
    outlier.setAocName(rs.getString("aoc_name"));
    outlier.setValue(rs.getDouble("value"));

    return outlier;
  }

  /**
   * The values for outlier identification are added to OutlierValue instance.
   *
   * @param outlier the {@link Outlier}.
   * @param rs the {@link ResultSet}.
   * @param modifiedZ boolean flag (false means z-score to be applied).
   * @throws SQLException
   */
  private void addZScoreBasedParamsToOutlier(Outlier outlier, ResultSet rs, boolean modifiedZ)
      throws SQLException {

    if (modifiedZ) {
      outlier.setMedian(rs.getDouble("middle_value"));
      outlier.setStdDev(rs.getDouble("mad"));
    } else {
      outlier.setMean(rs.getDouble("middle_value"));
      outlier.setStdDev(rs.getDouble("std_dev"));
    }

    outlier.setAbsDev(rs.getDouble("middle_value_abs_dev"));
    outlier.setZScore(outlier.getStdDev() == 0 ? NaN : rs.getDouble("z_score"));
    outlier.setLowerBound(rs.getDouble("lower_bound"));
    outlier.setUpperBound(rs.getDouble("upper_bound"));
  }
}
