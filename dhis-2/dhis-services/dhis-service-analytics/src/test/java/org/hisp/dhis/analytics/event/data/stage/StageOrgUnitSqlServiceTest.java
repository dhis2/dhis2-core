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
package org.hisp.dhis.analytics.event.data.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.OrgUnitField;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.ColumnAndAlias;
import org.hisp.dhis.analytics.event.data.OrganisationUnitResolver;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;

class StageOrgUnitSqlServiceTest {
  @Test
  void shouldUseStageOuCteContextForSelectColumn() {
    TestResolver resolver = new TestResolver();
    resolver.stageOuCteContext =
        new OrganisationUnitResolver.StageOuCteContext("\"uidlevel4\"", "", "");
    EventQueryParams params = new EventQueryParams.Builder().build();
    QueryItem item = createStageOuItem();

    DefaultStageOrgUnitSqlService subject =
        new DefaultStageOrgUnitSqlService(resolver, new PostgreSqlAnalyticsSqlBuilder());

    ColumnAndAlias column = subject.selectColumn(item, params, false);

    assertEquals("\"uidlevel4\"", column.getColumn());
    assertEquals("ou", column.getAlias());
  }

  @Test
  void shouldUseRawColumnForGroupBySelectColumn() {
    TestResolver resolver = new TestResolver();
    resolver.stageOuCteContext =
        new OrganisationUnitResolver.StageOuCteContext("\"uidlevel5\"", "", "");
    EventQueryParams params = new EventQueryParams.Builder().build();
    QueryItem item = createStageOuItem();

    DefaultStageOrgUnitSqlService subject =
        new DefaultStageOrgUnitSqlService(resolver, new PostgreSqlAnalyticsSqlBuilder());

    ColumnAndAlias column = subject.selectColumn(item, params, true);

    assertEquals("\"uidlevel5\"", column.getColumn());
    assertEquals("\"uidlevel5\"", column.asSql());
  }

  @Test
  void shouldGenerateWhereClauseWithUidLevelsAndProgramStage() {
    TestResolver resolver = new TestResolver();
    EventQueryParams params =
        new EventQueryParams.Builder().withOrgUnitField(new OrgUnitField(null)).build();
    QueryItem item = createStageOuItem();

    OrganisationUnit ouLevel2 = new OrganisationUnit();
    ouLevel2.setUid("ouL2AAAAAAA");
    OrganisationUnit ouLevel3 = new OrganisationUnit();
    ouLevel3.setUid("ouL3BBBBBBB");
    resolver.orgUnitsByLevel = Map.of(2, List.of(ouLevel2), 3, List.of(ouLevel3));

    DefaultStageOrgUnitSqlService subject =
        new DefaultStageOrgUnitSqlService(resolver, new PostgreSqlAnalyticsSqlBuilder());

    String whereClause = subject.whereClause(item, params, AnalyticsType.EVENT);

    assertTrue(whereClause.startsWith("("));
    assertTrue(whereClause.contains("ax.\"uidlevel2\" in ('ouL2AAAAAAA')"));
    assertTrue(whereClause.contains("ax.\"uidlevel3\" in ('ouL3BBBBBBB')"));
    assertTrue(whereClause.contains("ax.\"ps\" = 's1234567890'"));
  }

  @Test
  void shouldReturnEmptyWhereClauseWhenNoOrgUnitsResolved() {
    TestResolver resolver = new TestResolver();
    EventQueryParams params =
        new EventQueryParams.Builder().withOrgUnitField(new OrgUnitField(null)).build();
    QueryItem item = createStageOuItem();

    DefaultStageOrgUnitSqlService subject =
        new DefaultStageOrgUnitSqlService(resolver, new PostgreSqlAnalyticsSqlBuilder());

    assertEquals("", subject.whereClause(item, params, AnalyticsType.EVENT));
  }

  private QueryItem createStageOuItem() {
    DataElement dataElement = new DataElement();
    dataElement.setUid("ou");
    QueryItem item = new QueryItem(dataElement);
    ProgramStage programStage = new ProgramStage();
    programStage.setUid("s1234567890");
    item.setProgramStage(programStage);
    return item;
  }

  private static class TestResolver extends OrganisationUnitResolver {
    private Map<Integer, List<OrganisationUnit>> orgUnitsByLevel = Map.of();
    private StageOuCteContext stageOuCteContext = new StageOuCteContext("\"ou\"", "", "");

    private TestResolver() {
      super(null, null, null, new PostgreSqlAnalyticsSqlBuilder());
    }

    @Override
    public Map<Integer, List<OrganisationUnit>> resolveOrgUnitsGroupedByLevel(
        EventQueryParams params, QueryItem item) {
      return orgUnitsByLevel;
    }

    @Override
    public StageOuCteContext buildStageOuCteContext(QueryItem item, EventQueryParams params) {
      return stageOuCteContext;
    }
  }
}
