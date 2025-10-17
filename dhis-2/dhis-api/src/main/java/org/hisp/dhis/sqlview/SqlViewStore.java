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
package org.hisp.dhis.sqlview;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.TransactionMode;

/**
 * @author Dang Duy Hieu
 */
public interface SqlViewStore extends IdentifiableObjectStore<SqlView> {
  String ID = SqlViewStore.class.getName();

  String createViewTable(SqlView sqlView);

  void dropViewTable(SqlView sqlView);

  /**
   * This method will use the appropriate jdbcTemplate depending how DHIS2 has been setup.<br>
   *
   * <p>If DHIS2 has been set up using Postgres read replica, then the readOnlyJdbcTemplate will be
   * used for the reads, otherwise the normal jdbcTemplate will be used for all reads/writes.
   *
   * @param grid the {@link Grid} to populate with the results of the sql query.
   * @param sql the sql query to execute.
   * @param args the args for the query.
   * @param transactionMode the {@link TransactionMode} to use for the query.
   */
  void populateSqlViewGrid(Grid grid, String sql, Object[] args, TransactionMode transactionMode);

  boolean refreshMaterializedView(SqlView sqlView);
}
