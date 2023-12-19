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

import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.OutlierDetectionAlgorithm;
import org.hisp.dhis.analytics.outlier.OutlierSqlStatementProcessor;
import org.hisp.dhis.analytics.outlier.data.Outlier;
import org.hisp.dhis.calendar.Calendar;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
public class AnalyticsZScoreOutlierManager extends AbstractOutlierManager {
  protected AnalyticsZScoreOutlierManager(
      NamedParameterJdbcTemplate jdbcTemplate,
      @Qualifier("analyticsZScoreSqlStatementProcessor")
          OutlierSqlStatementProcessor sqlStatementProcessor) {
    super(jdbcTemplate, sqlStatementProcessor);
  }

  /** {@inheritDoc} */
  @Override
  protected RowMapper<Outlier> getRowMapper(Calendar calendar, boolean modifiedZ) {
    return (rs, rowNum) -> {
      Outlier outlier = getOutlierValue(calendar, rs);
      addZScoreBasedParamsToOutlierValue(outlier, rs, modifiedZ);

      return outlier;
    };
  }

  /** {@inheritDoc} */
  @Override
  protected void addZScoreBasedParamsToOutlierValue(
      Outlier outlier, ResultSet rs, boolean modifiedZ) throws SQLException {
    if (modifiedZ) {
      outlier.setStdDev(rs.getDouble("mad"));
    } else {
      outlier.setStdDev(rs.getDouble("std_dev"));
    }

    super.addZScoreBasedParamsToOutlierValue(outlier, rs, modifiedZ);
  }
}
