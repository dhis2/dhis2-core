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
package org.hisp.dhis.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.support.KeyHolder;

/**
 * A {@see BatchPreparedStatementSetter} with support for a {@see KeyHolder} This class allows to
 * execute a JDBC batch update operation using a {@see JdbcTemplate} and retrieve the generated
 * primary keys
 *
 * @author Luciano Fiandesio
 */
public abstract class BatchPreparedStatementSetterWithKeyHolder<T>
    implements BatchPreparedStatementSetter {
  private final List<T> beans;

  public BatchPreparedStatementSetterWithKeyHolder(List<T> beans) {
    this.beans = beans;
  }

  @Override
  public void setValues(PreparedStatement ps, int i) throws SQLException {
    setValues(ps, beans.get(i));
  }

  @Override
  public final int getBatchSize() {
    return beans.size();
  }

  public void setPrimaryKey(KeyHolder keyHolder) {
    List<Map<String, Object>> keys = keyHolder.getKeyList();
    for (int i = 0, len = keys.size(); i < len; i++) {
      setPrimaryKey(keys.get(i), beans.get(i));
    }
  }

  protected abstract void setValues(PreparedStatement ps, T bean) throws SQLException;

  protected abstract void setPrimaryKey(Map<String, Object> primaryKey, T bean);
}
