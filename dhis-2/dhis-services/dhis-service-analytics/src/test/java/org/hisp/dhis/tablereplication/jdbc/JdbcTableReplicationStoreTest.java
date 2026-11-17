/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.tablereplication.jdbc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.db.sql.DorisSqlBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tests the per tracked entity sync statements of {@link JdbcTableReplicationStore} against the
 * Doris SQL dialect.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class JdbcTableReplicationStoreTest {

  private static final Table TABLE =
      new Table(
          "trackedentityattributevalue",
          List.of(
              new Column("trackedentityid", DataType.BIGINT, Nullable.NOT_NULL),
              new Column("trackedentityattributeid", DataType.BIGINT, Nullable.NOT_NULL),
              new Column("value", DataType.TEXT, Nullable.NULL)),
          List.of("trackedentityid", "trackedentityattributeid"));

  @Mock private JdbcTemplate analyticsJdbcTemplate;

  @Mock private JdbcTemplate postgresJdbcTemplate;

  private JdbcTableReplicationStore store;

  @BeforeEach
  void setUp() {
    store =
        new JdbcTableReplicationStore(
            analyticsJdbcTemplate,
            postgresJdbcTemplate,
            new DorisSqlBuilder("pg_dhis", "postgresql.jar"));
  }

  @Test
  void deleteRowsUsesLiteralIdList() {
    store.deleteRows(TABLE, "trackedentityid", List.of(5L, 7L));

    verify(analyticsJdbcTemplate)
        .execute("delete from `trackedentityattributevalue` where `trackedentityid` in (5,7);");
  }

  @Test
  void replicateRowsReadsOnlyGivenTrackedEntitiesFromCatalog() {
    store.replicateRows(TABLE, "trackedentityid", List.of(5L, 7L));

    verify(analyticsJdbcTemplate)
        .execute(
            """
            insert into `trackedentityattributevalue` \
            (`trackedentityid`,`trackedentityattributeid`,`value`) \
            select `trackedentityid`,`trackedentityattributeid`,`value` \
            from pg_dhis.public.`trackedentityattributevalue` \
            where `trackedentityid` in (5,7);""");
  }

  @Test
  void changedTrackedEntitiesQueryDoesNotFilterDeleted() {
    Date since = new Date();

    store.getTrackedEntityIdsUpdatedSince(since, 10);

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(postgresJdbcTemplate).queryForList(sql.capture(), eq(Long.class), eq(since), eq(10));
    assertTrue(sql.getValue().contains("lastupdated >= ?"));
    assertTrue(sql.getValue().contains("limit ?"));
    assertFalse(sql.getValue().contains("deleted"));
    verifyNoInteractions(analyticsJdbcTemplate);
  }

  @Test
  void emptyIdListExecutesNothing() {
    store.deleteRows(TABLE, "trackedentityid", List.of());
    store.replicateRows(TABLE, "trackedentityid", List.of());

    verifyNoInteractions(analyticsJdbcTemplate);
  }

  @Test
  void tableExistsQueriesAnalyticsDatabase() {
    store.tableExists(TABLE);

    verify(analyticsJdbcTemplate).queryForList(any(String.class));
    verifyNoInteractions(postgresJdbcTemplate);
  }
}
