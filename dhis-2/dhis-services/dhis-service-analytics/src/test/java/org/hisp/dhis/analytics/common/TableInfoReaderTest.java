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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Unit tests for {@link TableInfoReader}.
 * @author maikel arabori
 */
@ExtendWith(MockitoExtension.class)
class TableInfoReaderTest {

  @Mock private JdbcTemplate jdbcTemplate;

  private TableInfoReader tableInfoReader;

  @BeforeEach
  public void beforeAll() {
    tableInfoReader = new TableInfoReader(jdbcTemplate);
  }

  @Test
  void testCheckColumnsPresenceWhenAllColumnsArePresent() {
    // Given
    String tableName = "tableName";
    List<String> columns = List.of("col1", "col2");
    String sql =
        "select column_name"
            + " from information_schema.columns"
            + " where table_schema = 'public'"
            + " and table_name = ?";

    // When
    when(jdbcTemplate.queryForList(sql, String.class, tableName)).thenReturn(columns);
    Set<String> absentColumns =
        tableInfoReader.checkColumnsPresence(tableName, new HashSet<>(columns));

    // Then
    assertTrue(absentColumns.isEmpty());
  }

  @Test
  void testCheckColumnsPresenceWhenOneColumnsIsMissing() {
    // Given
    String tableName = "tableName";
    List<String> columns = List.of("col1", "col2");
    Set<String> hasMissing = Set.of("col1", "col3");
    String sql =
        "select column_name"
            + " from information_schema.columns"
            + " where table_schema = 'public'"
            + " and table_name = ?";

    // When
    when(jdbcTemplate.queryForList(sql, String.class, tableName)).thenReturn(columns);
    Set<String> absentColumns = tableInfoReader.checkColumnsPresence(tableName, hasMissing);

    // Then
    assertTrue(absentColumns.contains("col3"));
  }
}
