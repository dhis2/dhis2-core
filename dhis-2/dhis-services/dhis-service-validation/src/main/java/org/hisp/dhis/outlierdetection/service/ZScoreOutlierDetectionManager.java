/*
 * Copyright (c) 2004-2022, University of Oslo
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

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierValue;
import org.hisp.dhis.outlierdetection.processor.AnalyticsZScoreSqlStatementProcessor;
import org.hisp.dhis.outlierdetection.processor.IOutlierSqlStatementProcessor;
import org.hisp.dhis.outlierdetection.processor.ZScoreSqlStatementProcessor;
import org.hisp.dhis.period.PeriodType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

/**
 * Manager for database queries related to outlier data detection based on z-score.
 *
 * <p>This both implements the {@link OutlierDetectionAlgorithm#Z_SCORE} and {@link
 * OutlierDetectionAlgorithm#MOD_Z_SCORE}. Usual z-score uses the mean as middle value whereas the
 * modified z-score uses the median as middle value or more mathematically correct as the
 * <em>measure of central tendency</em>.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@RequiredArgsConstructor
@Repository
public class ZScoreOutlierDetectionManager {
  private final NamedParameterJdbcTemplate jdbcTemplate;

  /**
   * Returns a list of outlier data values based on z-score for the given request.
   *
   * @param request the instance of {@link OutlierDetectionRequest}.
   * @return a list of {@link OutlierValue}.
   */
  public List<OutlierValue> getOutlierValues(OutlierDetectionRequest request) {

    final IOutlierSqlStatementProcessor statementFactory =  request.hasDataStartEndDate() ?
            new ZScoreSqlStatementProcessor(): new AnalyticsZScoreSqlStatementProcessor();

    final String sql = statementFactory.getSqlStatement(request);
    final SqlParameterSource params = statementFactory.getSqlParameterSource(request);
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
  private RowMapper<OutlierValue> getRowMapper(final Calendar calendar, boolean modifiedZ) {
    return (rs, rowNum) -> {
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
      if (modifiedZ) {
        outlier.setMedian(rs.getDouble("middle_value"));
      } else {
        outlier.setMean(rs.getDouble("middle_value"));
      }
      outlier.setStdDev(rs.getDouble("std_dev"));
      outlier.setAbsDev(rs.getDouble("middle_value_abs_dev"));
      outlier.setZScore(rs.getDouble("z_score"));
      outlier.setLowerBound(rs.getDouble("lower_bound"));
      outlier.setUpperBound(rs.getDouble("upper_bound"));
      outlier.setFollowup(rs.getBoolean("follow_up"));

      return outlier;
    };
  }
}
