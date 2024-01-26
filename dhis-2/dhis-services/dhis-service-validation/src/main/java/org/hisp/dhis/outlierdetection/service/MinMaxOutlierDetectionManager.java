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

import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.outlierdetection.OutlierValue;
import org.hisp.dhis.outlierdetection.processor.OutlierSqlStatementProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Manager for database queries related to outlier data detection based on min-max values.
 *
 * @author Lars Helge Overland
 */
@Repository
public class MinMaxOutlierDetectionManager extends AbstractOutlierDetectionManager {
  protected MinMaxOutlierDetectionManager(
      NamedParameterJdbcTemplate jdbcTemplate,
      @Qualifier("minMaxSqlStatementProcessor")
          OutlierSqlStatementProcessor sqlStatementProcessor) {
    super(jdbcTemplate, sqlStatementProcessor);
  }

  /** {@inheritDoc} */
  @Override
  protected RowMapper<OutlierValue> getRowMapper(Calendar calendar, boolean modifiedZ) {
    return (rs, rowNum) -> {
      OutlierValue outlierValue = getOutlierValue(calendar, rs);
      outlierValue.setAbsDev(rs.getDouble("bound_abs_dev"));
      outlierValue.setLowerBound(rs.getDouble("lower_bound"));
      outlierValue.setUpperBound(rs.getDouble("upper_bound"));
      outlierValue.setFollowup(rs.getBoolean("follow_up"));

      return outlierValue;
    };
  }
}
