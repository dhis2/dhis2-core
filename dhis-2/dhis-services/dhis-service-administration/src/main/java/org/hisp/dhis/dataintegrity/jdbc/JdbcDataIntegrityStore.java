/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dataintegrity.jdbc;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.dataintegrity.DataIntegrityCheck;
import org.hisp.dhis.dataintegrity.DataIntegrityDetails;
import org.hisp.dhis.dataintegrity.DataIntegrityDetails.DataIntegrityIssue;
import org.hisp.dhis.dataintegrity.DataIntegrityStore;
import org.hisp.dhis.dataintegrity.DataIntegritySummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the SQL of data integrity checks as plain JDBC statements, so the SQL reaches the database
 * exactly as written in the YAML declarations (no parameter parsing of {@code :} or {@code ?}).
 *
 * <p>Checks run by the data integrity service join its transaction; the read-only @{@link
 * Transactional} here only applies when the store is called outside of one. As the SQL runs over
 * JDBC, it does not see changes still pending in the Hibernate session of that transaction.
 *
 * @author Jan Bernitt
 */
@Repository
@RequiredArgsConstructor
public class JdbcDataIntegrityStore implements DataIntegrityStore {
  private final JdbcTemplate jdbcTemplate;

  @Override
  @Transactional(readOnly = true)
  public DataIntegritySummary querySummary(DataIntegrityCheck check, String sql) {
    Date startTime = new Date();
    Object[] summary = jdbcTemplate.query(sql, rs -> rs.next() ? getRow(rs) : null);
    return new DataIntegritySummary(
        check, startTime, new Date(), null, parseCount(summary), parsePercentage(summary));
  }

  @Override
  @Transactional(readOnly = true)
  public DataIntegrityDetails queryDetails(DataIntegrityCheck check, String sql) {
    Date startTime = new Date();
    List<DataIntegrityIssue> issues =
        jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
              int columns = rs.getMetaData().getColumnCount();
              return new DataIntegrityIssue(
                  getString(rs, columns, 1),
                  getString(rs, columns, 2),
                  getString(rs, columns, 3),
                  getRefs(rs, columns, 4));
            });
    return new DataIntegrityDetails(check, startTime, new Date(), null, List.copyOf(issues));
  }

  private static Object[] getRow(ResultSet rs) throws SQLException {
    Object[] row = new Object[rs.getMetaData().getColumnCount()];
    for (int i = 0; i < row.length; i++) {
      row[i] = rs.getObject(i + 1);
    }
    return row;
  }

  private static String getString(ResultSet rs, int columns, int column) throws SQLException {
    return columns < column ? null : rs.getString(column);
  }

  private static List<String> getRefs(ResultSet rs, int columns, int column) throws SQLException {
    Array refs = columns < column ? null : rs.getArray(column);
    return refs == null ? List.of() : List.of((String[]) refs.getArray());
  }

  private static Double parsePercentage(Object[] row) {
    if (row == null || row.length < 2 || row[1] == null) {
      return null;
    }
    Object value = row[1];
    if (value instanceof String percent) {
      return Double.parseDouble(percent.replace("%", ""));
    }
    return ((Number) value).doubleValue();
  }

  private static int parseCount(Object[] row) {
    if (row == null) {
      return 0;
    }
    return row.length == 0 ? -1 : parseCount(row[0]);
  }

  private static int parseCount(Object value) {
    if (value == null) {
      return 0;
    }
    if (value instanceof String count) {
      return Integer.parseInt(count);
    }
    return ((Number) value).intValue();
  }
}
