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
package org.hisp.dhis.sql;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Implementation of the {@link org.hisp.dhis.sql.SQL.QueryAPI} as a {@link
 * org.springframework.jdbc.core.JdbcTemplate}.
 *
 * @author Jan Bernitt
 */
class JdbcNativeSQL {

  record JdbcQuery(
      NamedParameterJdbcTemplate impl, String sql, Map<String, SqlParameterValue> params)
      implements SQL.Query {

    @Override
    public SQL.Query setParameter(@Nonnull SQL.Param param) {
      int type =
          switch (param.type()) {
            case BOOLEAN -> Types.BOOLEAN;
            case INTEGER -> Types.INTEGER;
            case LONG -> Types.BIGINT;
            case STRING -> Types.VARCHAR;
            case DATE -> Types.TIMESTAMP;
            case LONG_ARRAY, STRING_ARRAY -> Types.ARRAY;
          };
      String elemType =
          switch (param.type()) {
            case LONG_ARRAY -> "bigint";
            case STRING_ARRAY -> "varchar";
            default -> null;
          };
      SqlParameterValue value = new SqlParameterValue(type, elemType, param.value());
      params.put(param.name(), value);
      return this;
    }

    @Override
    public SQL.Query setLimit(int n) {
      params.put("_limit", new SqlParameterValue(Types.INTEGER, n));
      return this;
    }

    @Override
    public SQL.Query setOffset(int n) {
      params.put("_offset", new SqlParameterValue(Types.INTEGER, n));
      return this;
    }

    @Override
    public <T> Stream<T> stream(@Nonnull Class<T> of) {
      @SuppressWarnings("unchecked")
      RowMapper<T> mapper =
          of == Object[].class
              ? (RowMapper<T>) new ObjectArrayRowMapper()
              : new BeanPropertyRowMapper<>(of);
      return stream(mapper);
    }

    @Override
    public <T> Stream<T> stream(Function<SQL.Row, T> map) {
      return stream(new RowAdapterRowMapper<>(map));
    }

    private <T> Stream<T> stream(RowMapper<T> mapper) {
      String extSql = sql;
      if (params.containsKey("_limit")) extSql += "\nLIMIT :_limit";
      if (params.containsKey("_offset")) extSql += "\nOFFSET :_offset";
      return impl.queryForStream(extSql, params, mapper);
    }

    @Override
    public int count() {
      return impl.queryForObject(sql, params, Integer.class);
    }
  }

  private static final class ObjectArrayRowMapper implements RowMapper<Object[]> {

    private Object[] row;

    @Nonnull
    @Override
    public Object[] mapRow(@Nonnull ResultSet rs, int rowNum) throws SQLException {
      if (row == null) row = new Object[rs.getMetaData().getColumnCount()];
      for (int i = 0; i < row.length; i++) row[i] = rs.getObject(i + 1);
      return row;
    }
  }

  /**
   * @implNote The {@link ResultSet} API is tricky when it comes to SQL NULL and what it maps to for
   *     the different getters. The intent is to map SQL NULL to Java {@code null}.
   */
  private record RowAdapterRowMapper<T>(Function<SQL.Row, T> map) implements RowMapper<T> {

    @Nonnull
    @Override
    public T mapRow(@Nonnull ResultSet rs, int rowNum) {
      return map.apply(
          new SQL.Row() {
            @Override
            public Object getObject(int index) {
              try {
                return rs.getObject(index + 1);
              } catch (SQLException e) {
                throw new UnsupportedOperationException(e);
              }
            }

            @Override
            public Object[] getArray(int index) {
              try {
                Array array = rs.getArray(index + 1);
                return array == null ? null : (Object[]) array.getArray();
              } catch (SQLException e) {
                throw new UnsupportedOperationException(e);
              }
            }
          });
    }
  }
}
