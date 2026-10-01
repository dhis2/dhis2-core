/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.table;

import static org.hisp.dhis.analytics.AnalyticsStringUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.analytics.table.util.ColumnMapper;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbstractEventJdbcTableManagerTest {

  @Mock private AnalyticsTableSettings settings;

  @Spy private SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  @InjectMocks private ColumnMapper manager;

  @Test
  void testToCommaSeparated() {
    List<Column> columns =
        List.of(
            new Column("dx", DataType.VARCHAR_255),
            new Column("pe", DataType.VARCHAR_255),
            new Column("value", DataType.DOUBLE));

    String expected =
        """
        "dx","pe","value\"""";

    assertEquals(expected, toCommaSeparated(columns, col -> manager.quote(col.getName())));
  }

  @Test
  void testGetCastExpression() {
    String expected =
        """
        case when eventdatavalues #>> '{GieVkTxp4HH, value}' ~* '^(-?[0-9]+)(\\.[0-9]+)?$' \
        then cast(eventdatavalues #>> '{GieVkTxp4HH, value}' as double precision) \
        end""";

    String actual =
        manager.getCastExpression(
            "eventdatavalues #>> '{GieVkTxp4HH, value}'",
            "'^(-?[0-9]+)(\\.[0-9]+)?$'",
            "double precision");

    assertEquals(expected, actual);
  }

  @Test
  void testGetSelectExpressionNumber() {
    String expected =
        """
        case when eventdatavalues #>> '{GieVkTxp4HH, value}' ~* '^(-?[0-9]+)(\\.[0-9]+)?$' \
        then cast(eventdatavalues #>> '{GieVkTxp4HH, value}' as double precision) \
        end""";

    String actual =
        manager.getColumnExpression(ValueType.NUMBER, "eventdatavalues #>> '{GieVkTxp4HH, value}'");

    assertEquals(expected, actual);
  }

  @Test
  void testGetSelectExpressionBoolean() {
    String expected =
        """
        case when eventdatavalues #>> '{Xl3voRRcmpo, value}' = 'true' then 1 \
        when eventdatavalues #>> '{Xl3voRRcmpo, value}' = 'false' then 0 \
        else null end""";

    String actual =
        manager.getColumnExpression(
            ValueType.BOOLEAN, "eventdatavalues #>> '{Xl3voRRcmpo, value}'");

    assertEquals(expected, actual);
  }

  @Test
  void testGetSelectExpressionDate() {
    String expected =
        """
        case when eventdatavalues #>> '{AL04Wbutskk, value}' ~* '^[0-9]{4}-[0-9]{2}-[0-9]{2}(\\s|T)?(([0-9]{2}:)([0-9]{2}:)?([0-9]{2}))?(|.([0-9]{3})|.([0-9]{3})Z)?$' \
        then cast(eventdatavalues #>> '{AL04Wbutskk, value}' as timestamp) \
        end""";

    String actual =
        manager.getColumnExpression(ValueType.DATE, "eventdatavalues #>> '{AL04Wbutskk, value}'");

    assertEquals(expected, actual);
  }

  @Test
  void testGetSelectExpressionText() {
    String expected =
        """
        eventdatavalues #>> '{FwUzmc49Pcr, value}'""";

    String actual =
        manager.getColumnExpression(ValueType.TEXT, "eventdatavalues #>> '{FwUzmc49Pcr, value}'");

    assertEquals(expected, actual);
  }

  @Test
  void testGetSelectExpressionGeometry() {
    when(manager.isGeospatialSupport()).thenReturn(true);

    String expected =
        """
        ST_GeomFromGeoJSON('{"type":"Point", "coordinates":' || (eventdatavalues #>> '{C6bh7GevJfH, value}') || ', "crs":{"type":"name", "properties":{"name":"EPSG:4326"}}}')""";

    String actual =
        manager.getColumnExpression(
            ValueType.GEOJSON, "eventdatavalues #>> '{C6bh7GevJfH, value}'");

    assertEquals(expected, actual);
  }

  // ---------------------------------------------------------------------------
  // collectColumnJoinClauses
  // ---------------------------------------------------------------------------

  @Test
  void testCollectColumnJoinClauses_emptyColumnList() {
    assertEquals("", AbstractEventJdbcTableManager.collectColumnJoinClauses(List.of()));
  }

  @Test
  void testCollectColumnJoinClauses_noColumnDeclaresJoinClause() {
    AnalyticsTableColumn col =
        AnalyticsTableColumn.builder()
            .name("foo")
            .dataType(DataType.TEXT)
            .selectExpression("ev.foo")
            .build();

    assertEquals("", AbstractEventJdbcTableManager.collectColumnJoinClauses(List.of(col)));
  }

  @Test
  void testCollectColumnJoinClauses_singleClause() {
    String join = "left join organisationunit ou_x on ou_x.uid = ev.foo";
    AnalyticsTableColumn col =
        AnalyticsTableColumn.builder()
            .name("foo_name")
            .dataType(DataType.TEXT)
            .selectExpression("ou_x.name as foo_name")
            .joinClause(join)
            .build();

    assertEquals(join, AbstractEventJdbcTableManager.collectColumnJoinClauses(List.of(col)));
  }

  @Test
  void testCollectColumnJoinClauses_distinctClausesJoinedWithSpace() {
    AnalyticsTableColumn a =
        AnalyticsTableColumn.builder()
            .name("a_name")
            .dataType(DataType.TEXT)
            .selectExpression("ou_a.name as a_name")
            .joinClause("left join organisationunit ou_a on ou_a.uid = ev.a")
            .build();
    AnalyticsTableColumn b =
        AnalyticsTableColumn.builder()
            .name("b_name")
            .dataType(DataType.TEXT)
            .selectExpression("ou_b.name as b_name")
            .joinClause("left join organisationunit ou_b on ou_b.uid = ev.b")
            .build();

    String expected =
        "left join organisationunit ou_a on ou_a.uid = ev.a "
            + "left join organisationunit ou_b on ou_b.uid = ev.b";

    assertEquals(expected, AbstractEventJdbcTableManager.collectColumnJoinClauses(List.of(a, b)));
  }

  @Test
  void testCollectColumnJoinClauses_deduplicatesIdenticalClauses() {
    String join = "left join organisationunit ou_x on ou_x.uid = ev.foo";
    AnalyticsTableColumn first =
        AnalyticsTableColumn.builder()
            .name("foo_name")
            .dataType(DataType.TEXT)
            .selectExpression("ou_x.name as foo_name")
            .joinClause(join)
            .build();
    AnalyticsTableColumn second =
        AnalyticsTableColumn.builder()
            .name("foo_geometry")
            .dataType(DataType.TEXT)
            .selectExpression("ou_x.geometry as foo_geometry")
            .joinClause(join)
            .build();

    assertEquals(
        join, AbstractEventJdbcTableManager.collectColumnJoinClauses(List.of(first, second)));
  }

  @Test
  void testCollectColumnJoinClauses_filtersNullAndBlankClauses() {
    AnalyticsTableColumn nullClause =
        AnalyticsTableColumn.builder()
            .name("a")
            .dataType(DataType.TEXT)
            .selectExpression("ev.a")
            .build();
    AnalyticsTableColumn emptyClause =
        AnalyticsTableColumn.builder()
            .name("b")
            .dataType(DataType.TEXT)
            .selectExpression("ev.b")
            .joinClause("")
            .build();
    AnalyticsTableColumn whitespaceClause =
        AnalyticsTableColumn.builder()
            .name("c")
            .dataType(DataType.TEXT)
            .selectExpression("ev.c")
            .joinClause("   ")
            .build();
    AnalyticsTableColumn realClause =
        AnalyticsTableColumn.builder()
            .name("d_name")
            .dataType(DataType.TEXT)
            .selectExpression("ou_d.name as d_name")
            .joinClause("left join organisationunit ou_d on ou_d.uid = ev.d")
            .build();

    assertEquals(
        "left join organisationunit ou_d on ou_d.uid = ev.d",
        AbstractEventJdbcTableManager.collectColumnJoinClauses(
            List.of(nullClause, emptyClause, whitespaceClause, realClause)));
  }
}
