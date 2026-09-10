/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.export.timeout;

import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A {@link JdbcTemplate} that bounds every statement it issues by the remaining budget of the
 * tracker export request on the current thread. Only tracker export reads go through it, which is
 * what keeps the timeout off every other product.
 *
 * <p>Spring's own {@code DataSourceUtils.applyTimeout} cannot be reused: it reads a thread-bound
 * {@code ConnectionHolder} that the parallel branches of {@code TrackedEntityAggregate} never see,
 * and its fallback is a fixed timeout rather than a shrinking budget.
 */
public class DeadlineAwareJdbcTemplate extends JdbcTemplate {

  public DeadlineAwareJdbcTemplate(DataSource dataSource) {
    super(dataSource);
  }

  @Override
  protected void applyStatementSettings(Statement stmt) throws SQLException {
    // super sets fetch size, max rows, and a query timeout of its own via DataSourceUtils: from a
    // @Transactional(timeout=N) on this thread, or from setQueryTimeout on this bean. Neither
    // applies today, but calling super first means that if one ever does, it cannot replace our
    // shrinking budget with a fixed value.
    super.applyStatementSettings(stmt);

    Deadline deadline = DeadlineHolder.get();
    if (deadline == null) {
      return; // feature off, or a background job rather than a request
    }

    // fail fast rather than arming a 1s timeout per remaining statement only to fail anyway
    DeadlineHolder.checkNotExpired();

    stmt.setQueryTimeout(deadline.remainingSecondsCeil());
  }

  /**
   * Turns a query cancelled by our own {@code setQueryTimeout} into a {@link
   * DeadlineExceededException} so it reaches the client as 504 rather than a generic 500. pgjdbc
   * cancels the statement, which PostgreSQL reports as SQLSTATE 57014 and Spring translates to
   * {@link QueryTimeoutException}.
   *
   * <p>Only translated when this thread had a deadline, so a cancel from any other source keeps its
   * usual translation.
   */
  @Override
  protected DataAccessException translateException(String task, String sql, SQLException ex) {
    DataAccessException translated = super.translateException(task, sql, ex);
    Deadline deadline = DeadlineHolder.get();
    if (translated instanceof QueryTimeoutException && deadline != null) {
      return new DeadlineExceededException(deadline.budget(), ex);
    }
    return translated;
  }
}
