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

import static org.hisp.dhis.period.PeriodType.getIsoPeriod;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierValue;
import org.hisp.dhis.outlierdetection.processor.OutlierSqlStatementProcessor;
import org.hisp.dhis.period.PeriodType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@Slf4j
public abstract class AbstractOutlierDetectionManager {
  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final OutlierSqlStatementProcessor sqlStatementProcessor;

  protected AbstractOutlierDetectionManager(
      NamedParameterJdbcTemplate jdbcTemplate, OutlierSqlStatementProcessor sqlStatementProcessor) {
    this.jdbcTemplate = jdbcTemplate;
    this.sqlStatementProcessor = sqlStatementProcessor;
  }

  public List<OutlierValue> getOutlierValues(OutlierDetectionRequest request) {

    final String sql = sqlStatementProcessor.getSqlStatement(request);
    final SqlParameterSource params = sqlStatementProcessor.getSqlParameterSource(request);
    final Calendar calendar = PeriodType.getCalendar();
    final boolean modifiedZ = request.getAlgorithm() == OutlierDetectionAlgorithm.MOD_Z_SCORE;

    try {
      return jdbcTemplate.query(sql, params, getRowMapper(calendar, modifiedZ));
    } catch (DataIntegrityViolationException ex) {
      // Casting non-numeric data to double, catching exception is faster
      // than filtering

      log.error(ErrorCode.E2208.getMessage(), ex);

      throw new IllegalQueryException(ErrorCode.E2208);
    }
  }

  /**
   * Returns a {@link RowMapper} for {@link OutlierValue}.
   *
   * @param calendar the {@link Calendar}.
   * @return a {@link RowMapper}.
   */
  protected abstract RowMapper<OutlierValue> getRowMapper(
      final Calendar calendar, boolean modifiedZ);

  protected OutlierValue getOutlierValue(Calendar calendar, ResultSet rs) throws SQLException {
    final OutlierValue outlier = new OutlierValue();

    final String isoPeriod =
        getIsoPeriod(calendar, rs.getString("pt_name"), rs.getDate("pe_start_date"));

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

  protected void addZScoreBasedParamsToOutlierValue(
      OutlierValue outlierValue, ResultSet rs, boolean modifiedZ) throws SQLException {
    if (modifiedZ) {
      outlierValue.setMedian(rs.getDouble("middle_value"));
    } else {
      outlierValue.setMean(rs.getDouble("middle_value"));
    }
    outlierValue.setStdDev(rs.getDouble("std_dev"));
    outlierValue.setAbsDev(rs.getDouble("middle_value_abs_dev"));
    outlierValue.setZScore(rs.getDouble("z_score"));
    outlierValue.setLowerBound(rs.getDouble("lower_bound"));
    outlierValue.setUpperBound(rs.getDouble("upper_bound"));
  }
}
