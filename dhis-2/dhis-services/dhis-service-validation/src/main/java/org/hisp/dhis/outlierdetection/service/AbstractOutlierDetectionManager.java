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
package org.hisp.dhis.outlierdetection.service;

import static org.hisp.dhis.outlierdetection.util.OutlierDetectionUtils.withExceptionHandling;
import static org.hisp.dhis.period.PeriodType.getIsoPeriod;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierValue;
import org.hisp.dhis.outlierdetection.processor.OutlierSqlStatementProcessor;
import org.hisp.dhis.period.PeriodType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * Manager for database queries related to outlier data detection (Z-Score, modified Z-Score,
 * Min-Max values).
 */
@Slf4j
public abstract class AbstractOutlierDetectionManager {
  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final OutlierSqlStatementProcessor sqlStatementProcessor;

  protected AbstractOutlierDetectionManager(
      NamedParameterJdbcTemplate jdbcTemplate, OutlierSqlStatementProcessor sqlStatementProcessor) {
    this.jdbcTemplate = jdbcTemplate;
    this.sqlStatementProcessor = sqlStatementProcessor;
  }

  /**
   * Retrieves all outliers.
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @return list of the OutlierValue instances for api response
   */
  public List<OutlierValue> getOutlierValues(OutlierDetectionRequest request) {
    String sql = sqlStatementProcessor.getSqlStatement(request);
    SqlParameterSource params = sqlStatementProcessor.getSqlParameterSource(request);
    Calendar calendar = PeriodType.getCalendar();
    boolean modifiedZ = request.getAlgorithm() == OutlierDetectionAlgorithm.MOD_Z_SCORE;

    return withExceptionHandling(
            () -> jdbcTemplate.query(sql, params, getRowMapper(calendar, modifiedZ)))
        .orElse(null);
  }

  /**
   * Returns a {@link RowMapper} for {@link OutlierValue}.
   *
   * @param calendar the {@link Calendar}.
   * @return a {@link RowMapper}.
   */
  protected abstract RowMapper<OutlierValue> getRowMapper(
      final Calendar calendar, boolean modifiedZ);

  /**
   * Maps incoming database set into the api response element.
   *
   * @param calendar the {@link Calendar}.
   * @param rs the {@link ResultSet}.
   * @return single OutlierValue instance
   * @throws SQLException
   */
  protected OutlierValue getOutlierValue(Calendar calendar, ResultSet rs) throws SQLException {
    OutlierValue outlier = new OutlierValue();

    String isoPeriod = getIsoPeriod(calendar, rs.getString("pt_name"), rs.getDate("pe_start_date"));

    outlier.setDe(rs.getString("de_uid"));
    outlier.setDeName(rs.getString("de_name"));
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
   * @param outlierValue the {@link OutlierValue}
   * @param rs the {@link ResultSet}
   * @param modifiedZ boolean flag (false means z-score to be applied)
   * @throws SQLException
   */
  protected void addZScoreBasedParamsToOutlierValue(
      OutlierValue outlierValue, ResultSet rs, boolean modifiedZ) throws SQLException {
    if (modifiedZ) {
      outlierValue.setMedian(rs.getDouble("middle_value"));
    } else {
      outlierValue.setMean(rs.getDouble("middle_value"));
    }

    outlierValue.setAbsDev(rs.getDouble("middle_value_abs_dev"));
    outlierValue.setZScore(outlierValue.getStdDev() == 0 ? Double.NaN : rs.getDouble("z_score"));
    outlierValue.setLowerBound(rs.getDouble("lower_bound"));
    outlierValue.setUpperBound(rs.getDouble("upper_bound"));
  }
}
