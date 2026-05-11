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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramStage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.Test;

/**
 * Pins the SQL produced by {@link EnrollmentEventSubqueryBuilder} so the extraction from {@code
 * JdbcEnrollmentAnalyticsManager.getColumn} and {@code getCoordinateColumn} preserves
 * byte-identical output.
 */
class EnrollmentEventSubqueryBuilderTest {

  private final PostgreSqlAnalyticsSqlBuilder sqlBuilder = new PostgreSqlAnalyticsSqlBuilder();

  private final EnrollmentEventSubqueryBuilder subject =
      new EnrollmentEventSubqueryBuilder(sqlBuilder, new ProgramStageOffsetSqlBuilder(sqlBuilder));

  private final Program program = programWithUid("prog123ABCD");
  private final ProgramStage programStage = stageWithUid("stagABCDEFG", false);
  private final ProgramStage repeatableStage = stageWithUid("rstgABCDEFG", true);
  private final DataElement dataElement = dataElementWithUid("deUidABCDEF");

  @Test
  void coordinateSubqueryReturnsEmptyWhenItemHasNoProgram() {
    QueryItem item = new QueryItem(dataElement);
    item.setValueType(ValueType.COORDINATE);

    assertSame(ColumnAndAlias.EMPTY, subject.renderCoordinateSubquery(item));
  }

  @Test
  void coordinateSubqueryWithoutProgramStage() {
    QueryItem item = new QueryItem(dataElement);
    item.setValueType(ValueType.COORDINATE);
    item.setProgram(program);

    String sql = subject.renderCoordinateSubquery(item).asSql();

    assertEquals(
        "(select '[' || round(ST_X((\"deUidABCDEF\"))::numeric, 6) || ',' "
            + "|| round(ST_Y((\"deUidABCDEF\"))::numeric, 6) || ']' as \"deUidABCDEF\" "
            + "from analytics_event_prog123ABCD "
            + "where analytics_event_prog123ABCD.enrollment = ax.enrollment "
            + "and \"deUidABCDEF\" is not null  "
            + "order by occurreddate desc, created desc  limit 1 )",
        sql);
  }

  @Test
  void coordinateSubqueryWithProgramStage() {
    QueryItem item = new QueryItem(dataElement);
    item.setValueType(ValueType.COORDINATE);
    item.setProgram(program);
    item.setProgramStage(programStage);

    String sql = subject.renderCoordinateSubquery(item).asSql();

    assertEquals(
        "(select '[' || round(ST_X((\"deUidABCDEF\"))::numeric, 6) || ',' "
            + "|| round(ST_Y((\"deUidABCDEF\"))::numeric, 6) || ']' as \"deUidABCDEF\" "
            + "from analytics_event_prog123ABCD "
            + "where analytics_event_prog123ABCD.enrollment = ax.enrollment "
            + "and \"deUidABCDEF\" is not null and ps = 'stagABCDEFG'  "
            + "order by occurreddate desc, created desc  limit 1 )",
        sql);
  }

  @Test
  void coordinateSubqueryWithOrgUnitValueTypeUsesStCentroid() {
    QueryItem item = new QueryItem(dataElement);
    item.setValueType(ValueType.ORGANISATION_UNIT);
    item.setProgram(program);
    item.setProgramStage(programStage);

    String sql = subject.renderCoordinateSubquery(item).asSql();

    assertEquals(
        "(select '[' || round(ST_X(ST_Centroid(\"deUidABCDEF\"))::numeric, 6) || ',' "
            + "|| round(ST_Y(ST_Centroid(\"deUidABCDEF\"))::numeric, 6) || ']' as \"deUidABCDEF\" "
            + "from analytics_event_prog123ABCD "
            + "where analytics_event_prog123ABCD.enrollment = ax.enrollment "
            + "and \"deUidABCDEF\" is not null and ps = 'stagABCDEFG'  "
            + "order by occurreddate desc, created desc  limit 1 )",
        sql);
  }

  @Test
  void valueSubqueryForSingleStageDataElement() {
    QueryItem item = new QueryItem(dataElement);
    item.setProgram(program);
    item.setProgramStage(programStage);

    String sql = subject.renderValueSubquery(item, "");

    assertEquals(
        "(select \"deUidABCDEF\" as \"stagABCDEFG.deUidABCDEF\" "
            + "from analytics_event_prog123ABCD "
            + "where analytics_event_prog123ABCD.eventstatus != 'SCHEDULE' "
            + "and analytics_event_prog123ABCD.enrollment = ax.enrollment "
            + "and \"deUidABCDEF\" is not null and ps = 'stagABCDEFG' "
            + "order by occurreddate desc, created desc  limit 1 )",
        sql);
  }

  @Test
  void valueSubqueryForRepeatableStageWithoutDateBounds() {
    QueryItem item = new QueryItem(dataElement);
    item.setProgram(program);
    item.setProgramStage(repeatableStage);
    RepeatableStageParams params = new RepeatableStageParams();
    params.setIndex(0);
    item.setRepeatableStageParams(params);

    String sql = subject.renderValueSubquery(item, "");

    assertEquals(
        "(select \"deUidABCDEF\" "
            + "from analytics_event_prog123ABCD "
            + "where analytics_event_prog123ABCD.eventstatus != 'SCHEDULE' "
            + "and analytics_event_prog123ABCD.enrollment = ax.enrollment "
            + "and ps = 'rstgABCDEFG' "
            + "order by occurreddate desc, created desc  limit 1 )",
        sql);
  }

  @Test
  void valueSubqueryForRepeatableStageWithDateBounds() {
    QueryItem item = new QueryItem(dataElement);
    item.setProgram(program);
    item.setProgramStage(repeatableStage);
    RepeatableStageParams params = new RepeatableStageParams();
    params.setIndex(0);
    params.setStartDate(DateUtils.parseDate("2024-01-15"));
    params.setEndDate(DateUtils.parseDate("2024-12-31"));
    item.setRepeatableStageParams(params);

    String sql = subject.renderValueSubquery(item, "");

    assertEquals(
        "(select \"deUidABCDEF\" "
            + "from analytics_event_prog123ABCD "
            + "where analytics_event_prog123ABCD.eventstatus != 'SCHEDULE' "
            + "and analytics_event_prog123ABCD.enrollment = ax.enrollment "
            + "and ps = 'rstgABCDEFG'  and occurreddate >= '2024-01-15'  "
            + "and occurreddate <= '2024-12-31' "
            + "order by occurreddate desc, created desc  limit 1 )",
        sql);
  }

  private static Program programWithUid(String uid) {
    Program program = createProgram('A');
    program.setUid(uid);
    return program;
  }

  private static ProgramStage stageWithUid(String uid, boolean repeatable) {
    ProgramStage stage = createProgramStage('A', 0);
    stage.setUid(uid);
    stage.setRepeatable(repeatable);
    return stage;
  }

  private static DataElement dataElementWithUid(String uid) {
    DataElement de = createDataElement('A');
    de.setUid(uid);
    return de;
  }
}
