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
package org.hisp.dhis.analytics.common;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.springframework.util.Assert.hasText;

import java.util.Set;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Component responsible for querying meta information related to DB tables, as well as providing
 * helper methods on top of the meta information.
 *
 * @author maikel arabori
 */
@Component
@AllArgsConstructor
public class TableInfoReader {
  private final JdbcTemplate jdbcTemplate;

  /**
   * Returns the metadata information for the given table.
   *
   * @param tableName the name of the table where the information should be retrieved.
   * @return the populated {@link TableInfo} object.
   * @throws IllegalArgumentException if the argument 'tableName' is null or blank.
   */
  public TableInfo getInfo(@Nonnull String tableName) {
    hasText(tableName, "Param 'tableName' cannot be null/blank");

    String sql =
        "select column_name"
            + " from information_schema.columns"
            + " where table_schema = 'public'"
            + " and table_name = ?";

    Set<String> tableColumns =
        jdbcTemplate.queryForList(sql, String.class, tableName).stream()
            .collect(toUnmodifiableSet());

    return new TableInfo(tableName, tableColumns);
  }

  /**
   * Checks whether the given table contains all set of columns provided.
   *
   * @param table the table where the columns will be checked against.
   * @param columnsToCheck the columns to check (if they exist or not in the given table).
   * @return the set of missing columns, if any, or empty.
   * @throws IllegalArgumentException if the argument is null or blank.
   */
  public Set<String> checkColumnsPresence(@Nonnull String table, Set<String> columnsToCheck) {
    hasText(table, "Param 'table' cannot be null/blank");

    Set<String> columnsInTable = getInfo(table).getColumns();
    return columnsToCheck.stream()
        .filter(c -> !columnsInTable.contains(c))
        .collect(toUnmodifiableSet());
  }

  /**
   * Object representing the information related to the meta information of the respective table.
   */
  @Data
  @AllArgsConstructor
  public static class TableInfo {
    private String name;
    private Set<String> columns;
  }
}
