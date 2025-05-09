/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.program;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.antlr.AntlrParserUtils.castString;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.parser.expression.ExpressionItem.ITEM_GET_DESCRIPTIONS;
import static org.hisp.dhis.parser.expression.ExpressionItem.ITEM_GET_SQL;
import static org.hisp.dhis.program.AnalyticsType.ENROLLMENT;
import static org.hisp.dhis.program.variable.vEventCount.DEFAULT_COUNT_CONDITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.antlr.AntlrExprLiteral;
import org.hisp.dhis.antlr.Parser;
import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.antlr.literal.DefaultLiteral;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItemMethod;
import org.hisp.dhis.parser.expression.ProgramExpressionParams;
import org.hisp.dhis.parser.expression.literal.SqlLiteral;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jim Grace
 */
@ExtendWith(MockitoExtension.class)
class ProgramSqlGeneratorFunctionsTest extends TestBase {
  private ProgramIndicator programIndicator;

  private Program programA;

  private DataElement dataElementA;

  private DataElement dataElementB;

  private DataElement dataElementC;

  private DataElement dataElementD;

  private DataElement dataElementE;

  private DataElement dataElementF;

  private ProgramStage programStageA;

  private ProgramStage programStageB;

  private TrackedEntityAttribute attributeA;

  private RelationshipType relTypeA;

  private final Date startDate = getDate(2020, 1, 1);

  private final Date endDate = getDate(2020, 12, 31);

  @Mock private IdentifiableObjectManager idObjectManager;

  @Mock private ProgramIndicatorService programIndicatorService;

  @Mock private ProgramStageService programStageService;

  @Mock private DimensionService dimensionService;

  @Spy private PostgreSqlBuilder sqlBuilder;

  @BeforeEach
  public void setUp() {
    dataElementA = createDataElement('A');
    dataElementA.setDomainType(DataElementDomain.TRACKER);
    dataElementA.setUid("DataElmentA");

    dataElementB = createDataElement('B');
    dataElementB.setDomainType(DataElementDomain.TRACKER);
    dataElementB.setUid("DataElmentB");

    dataElementC = createDataElement('C');
    dataElementC.setDomainType(DataElementDomain.TRACKER);
    dataElementC.setUid("DataElmentC");
    dataElementC.setValueType(ValueType.DATE);

    dataElementD = createDataElement('D');
    dataElementD.setDomainType(DataElementDomain.TRACKER);
    dataElementD.setUid("DataElmentD");
    dataElementD.setValueType(ValueType.DATE);

    dataElementE = createDataElement('E');
    dataElementE.setDomainType(DataElementDomain.TRACKER);
    dataElementE.setUid("DataElmentE");
    dataElementE.setValueType(ValueType.BOOLEAN);

    dataElementF = createDataElement('F');
    dataElementF.setDomainType(DataElementDomain.TRACKER);
    dataElementF.setUid("DataElmentF");
    dataElementF.setValueType(ValueType.TEXT);

    attributeA = createTrackedEntityAttribute('A', ValueType.NUMBER);
    attributeA.setUid("Attribute0A");

    OrganisationUnit organisationUnit = createOrganisationUnit('A');

    programStageA = new ProgramStage("StageA", programA);
    programStageA.setSortOrder(1);
    programStageA.setUid("ProgrmStagA");

    programStageB = new ProgramStage("StageB", programA);
    programStageB.setSortOrder(2);
    programStageB.setUid("ProgrmStagB");

    Set<ProgramStage> programStages = new HashSet<>();
    programStages.add(programStageA);
    programStages.add(programStageB);

    programA = createProgram('A', new HashSet<>(), organisationUnit);
    programA.setUid("Program000A");
    programA.setProgramStages(programStages);

    programIndicator = new ProgramIndicator();
    programIndicator.setProgram(programA);
    programIndicator.setAnalyticsType(AnalyticsType.EVENT);

    relTypeA = new RelationshipType();
    relTypeA.setUid("RelatnTypeA");
  }

  @Test
  void testIsIn() {
    assertEquals("'A' in ('A','B','C')", test("is('A' in 'A','B','C')"));
    assertEquals("1::numeric in (1::numeric,2::numeric,3::numeric)", test("is( 1 in 1, 2, 3 )"));
  }

  @Test
  void testCondition() {
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(programIndicatorService.getAnalyticsSql(
            anyString(), any(DataType.class), eq(programIndicator), eq(startDate), eq(endDate)))
        .thenAnswer(i -> test((String) i.getArguments()[0], (DataType) i.getArguments()[1]));

    String sql = test("d2:condition('#{ProgrmStagA.DataElmentA} > 3',10 + 5,3 * 2)");
    assertThat(
        sql,
        is(
            "case when (coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end::numeric,0) "
                + "> 3::numeric) then 10::numeric + 5::numeric else 3::numeric * 2::numeric end"));
  }

  @Test
  void testConditionWithBooleanAsBoolean() {
    when(idObjectManager.get(DataElement.class, dataElementE.getUid())).thenReturn(dataElementE);
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(programIndicatorService.getAnalyticsSql(
            anyString(), any(DataType.class), eq(programIndicator), eq(startDate), eq(endDate)))
        .thenAnswer(i -> test((String) i.getArguments()[0], (DataType) i.getArguments()[1]));

    String sql = test("d2:condition('#{ProgrmStagA.DataElmentE}',10 + 5,3 * 2)");
    assertThat(
        sql,
        is(
            "case when (coalesce("
                + "case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentE\" else null end::numeric != 0,false)) "
                + "then 10::numeric + 5::numeric else 3::numeric * 2::numeric end"));
  }

  @Test
  void testConditionWithBooleanAsNumeric() {
    when(idObjectManager.get(DataElement.class, dataElementE.getUid())).thenReturn(dataElementE);
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(programIndicatorService.getAnalyticsSql(
            anyString(), any(DataType.class), eq(programIndicator), eq(startDate), eq(endDate)))
        .thenAnswer(i -> test((String) i.getArguments()[0], (DataType) i.getArguments()[1]));

    String sql = test("d2:condition('#{ProgrmStagA.DataElmentE} > 0',10 + 5,3 * 2)");
    assertThat(
        sql,
        is(
            "case when (coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentE\" else null end::numeric,0) "
                + "> 0::numeric) then 10::numeric + 5::numeric else 3::numeric * 2::numeric end"));
  }

  @Test
  void testContains() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementF.getUid())).thenReturn(dataElementF);

    String sql1 = test("if(contains(#{ProgrmStagA.DataElmentF},'abc'),1,2)");
    assertThat(
        sql1,
        is(
            " case when (position('abc' in coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentF\" else null end::text,''))>0"
                + ") then 1::numeric else 2::numeric end"));

    String sql2 = test("if(contains(#{ProgrmStagA.DataElmentF},'abc','def'),1,2)");
    assertThat(
        sql2,
        is(
            " case when (position('abc' in coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentF\" else null end::text,''))>0"
                + " and position('def' in coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentF\" else null end::text,''))>0"
                + ") then 1::numeric else 2::numeric end"));

    String sql3 = test("if(contains(#{ProgrmStagA.DataElmentF},'abc','def','ghi'),1,2)");
    assertThat(
        sql3,
        is(
            " case when (position('abc' in coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentF\" else null end::text,''))>0"
                + " and position('def' in coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentF\" else null end::text,''))>0"
                + " and position('ghi' in coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentF\" else null end::text,''))>0"
                + ") then 1::numeric else 2::numeric end"));
  }

  @Test
  void testContainsItems() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementF.getUid())).thenReturn(dataElementF);

    String sql1 = test("if(containsItems(#{ProgrmStagA.DataElmentF},'abc'),1,2)");
    assertThat(
        sql1,
        is(
            " case when (regexp_split_to_array(coalesce(case when ax.\"ps\" = 'ProgrmStagA' "
                + "then \"DataElmentF\" else null end::text,''),',') "
                + "@> ARRAY['abc']) then 1::numeric else 2::numeric end"));

    String sql2 = test("if(containsItems(#{ProgrmStagA.DataElmentF},'abc','def'),1,2)");
    assertThat(
        sql2,
        is(
            " case when (regexp_split_to_array(coalesce(case when ax.\"ps\" = 'ProgrmStagA' "
                + "then \"DataElmentF\" else null end::text,''),',') "
                + "@> ARRAY['abc','def']) then 1::numeric else 2::numeric end"));

    String sql3 = test("if(containsItems(#{ProgrmStagA.DataElmentF},'abc','def','ghi'),1,2)");
    assertThat(
        sql3,
        is(
            " case when (regexp_split_to_array(coalesce(case when ax.\"ps\" = 'ProgrmStagA' "
                + "then \"DataElmentF\" else null end::text,''),',') "
                + "@> ARRAY['abc','def','ghi']) then 1::numeric else 2::numeric end"));
  }

  @Test
  void testCount() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);

    String sql = test("d2:count(#{ProgrmStagA.DataElmentA})");
    assertThat(
        normalize(sql),
        is(
            normalize(
                """
                (select count("DataElmentA")
                from analytics_event_Program000A
                where analytics_event_Program000A.enrollment = ax.enrollment
                and "DataElmentA" is not null
                and "DataElmentA" is not null
                and ps = 'ProgrmStagA')
                """)));
  }

  @Test
  void testCountWithStartEventBoundary() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);

    setStartEventBoundary();

    String sql = test("d2:count(#{ProgrmStagA.DataElmentA})");
    assertThat(
        normalize(sql),
        is(
            normalize(
                "(select count(\"DataElmentA\") "
                    + "from analytics_event_Program000A "
                    + "where analytics_event_Program000A.enrollment = ax.enrollment "
                    + "and \"DataElmentA\" is not null and \"DataElmentA\" is not null "
                    + "and occurreddate < cast( '2021-01-01' as date ) "
                    + "and ps = 'ProgrmStagA')")));
  }

  @Test
  void testCountWithEndEventBoundary() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);

    setEndEventBoundary();

    String sql = test("d2:count(#{ProgrmStagA.DataElmentA})");
    assertThat(
        normalize(sql),
        is(
            normalize(
                "(select count(\"DataElmentA\") "
                    + "from analytics_event_Program000A "
                    + "where analytics_event_Program000A.enrollment = ax.enrollment "
                    + "and \"DataElmentA\" is not null and \"DataElmentA\" is not null "
                    + "and occurreddate >= cast( '2020-01-01' as date ) "
                    + "and ps = 'ProgrmStagA')")));
  }

  @Test
  void testCountWithStartAndEndEventBoundary() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);

    setStartAndEndEventBoundary();

    String sql = test("d2:count(#{ProgrmStagA.DataElmentA})");
    assertThat(
        normalize(sql),
        is(
            normalize(
                "(select count(\"DataElmentA\") "
                    + "from analytics_event_Program000A "
                    + "where analytics_event_Program000A.enrollment = ax.enrollment "
                    + "and \"DataElmentA\" is not null and \"DataElmentA\" is not null "
                    + "and occurreddate < cast( '2021-01-01' as date ) and occurreddate >= cast( '2020-01-01' as date ) "
                    + "and ps = 'ProgrmStagA')")));
  }

  @Test
  void testCountIfCondition() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);
    when(programIndicatorService.getAnalyticsSql(
            anyString(), any(DataType.class), eq(programIndicator), eq(startDate), eq(endDate)))
        .thenAnswer(i -> test((String) i.getArguments()[0], (DataType) i.getArguments()[1]));

    String sql = test("d2:countIfCondition(#{ProgrmStagA.DataElmentA},'>5')");
    assertThat(
        normalize(sql),
        is(
            normalize(
                "(select count(\"DataElmentA\") "
                    + "from analytics_event_Program000A "
                    + "where analytics_event_Program000A.enrollment = ax.enrollment "
                    + "and \"DataElmentA\" is not null and \"DataElmentA\"::numeric > 5::numeric and ps = 'ProgrmStagA')")));
  }

  @Test
  void testCountIfConditionWithBooleanAsNumeric() {
    // Note: A boolean within a comparison should be treated as numeric.
    // PostgreSQL allows comparision of a text column with a numeric value.

    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);
    when(idObjectManager.get(DataElement.class, dataElementE.getUid())).thenReturn(dataElementE);
    when(programIndicatorService.getAnalyticsSql(
            anyString(), any(DataType.class), eq(programIndicator), eq(startDate), eq(endDate)))
        .thenAnswer(i -> test((String) i.getArguments()[0], (DataType) i.getArguments()[1]));

    String sql =
        test("d2:countIfCondition(#{ProgrmStagA.DataElmentA},'>#{ProgrmStagA.DataElmentE}')");
    assertThat(
        normalize(sql),
        is(
            normalize(
                "(select count(\"DataElmentA\") "
                    + "from analytics_event_Program000A "
                    + "where analytics_event_Program000A.enrollment = ax.enrollment "
                    + "and \"DataElmentA\" is not null and \"DataElmentA\"::numeric > "
                    + "coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentE\" else null end::numeric,0) "
                    + "and ps = 'ProgrmStagA')")));
  }

  @Test
  void testCountIfValueNumeric() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);

    String sql = test("d2:countIfValue(#{ProgrmStagA.DataElmentA},55)");
    assertThat(
        normalize(sql),
        is(
            normalize(
                "(select count(\"DataElmentA\") "
                    + "from analytics_event_Program000A "
                    + "where analytics_event_Program000A.enrollment = ax.enrollment "
                    + "and \"DataElmentA\" is not null and \"DataElmentA\" = 55::numeric "
                    + "and ps = 'ProgrmStagA')")));
  }

  @Test
  void testCountIfValueString() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);

    dataElementA.setValueType(TEXT);

    String sql = test("d2:countIfValue(#{ProgrmStagA.DataElmentA},'ABC')");
    assertThat(
        normalize(sql),
        is(
            normalize(
                "(select count(\"DataElmentA\") "
                    + "from analytics_event_Program000A "
                    + "where analytics_event_Program000A.enrollment = ax.enrollment "
                    + "and \"DataElmentA\" is not null and \"DataElmentA\" = 'ABC' "
                    + "and ps = 'ProgrmStagA')")));
  }

  @Test
  void testDaysBetween() {
    when(idObjectManager.get(DataElement.class, dataElementC.getUid())).thenReturn(dataElementC);
    when(idObjectManager.get(DataElement.class, dataElementD.getUid())).thenReturn(dataElementD);
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);

    String sql = test("d2:daysBetween(#{ProgrmStagA.DataElmentC},#{ProgrmStagA.DataElmentD})");
    assertThat(
        sql,
        is(
            "(cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentD\" else null end as date) - "
                + "cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentC\" else null end as date))"));
  }

  @Test
  void testHasValueDataElement() {
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);

    String sql = test("d2:hasValue(#{ProgrmStagA.DataElmentA})");
    assertThat(
        sql,
        is("(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end is not null)"));
  }

  @Test
  void testHasValueAttribute() {
    when(idObjectManager.get(TrackedEntityAttribute.class, attributeA.getUid()))
        .thenReturn(attributeA);

    String sql = test("d2:hasValue(A{Attribute0A})");
    assertThat(sql, is("(\"Attribute0A\" is not null)"));
  }

  @Test
  void testMinutesBetween() {
    when(idObjectManager.get(DataElement.class, dataElementC.getUid())).thenReturn(dataElementC);
    when(idObjectManager.get(DataElement.class, dataElementD.getUid())).thenReturn(dataElementD);
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);

    String sql = test("d2:minutesBetween(#{ProgrmStagA.DataElmentC},#{ProgrmStagA.DataElmentD})");
    assertThat(
        sql,
        is(
            "(extract(epoch from (cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentD\" else null end as timestamp) "
                + "- cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentC\" else null end as timestamp))) / 60)"));
  }

  @Test
  void testMonthsBetween() {
    when(idObjectManager.get(DataElement.class, dataElementC.getUid())).thenReturn(dataElementC);
    when(idObjectManager.get(DataElement.class, dataElementD.getUid())).thenReturn(dataElementD);
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);

    String sql = test("d2:monthsBetween(#{ProgrmStagA.DataElmentC},#{ProgrmStagA.DataElmentD})");

    assertThat(
        sql,
        is(
            "((date_part('year',age(cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentD\" else null end as date), "
                + "cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentC\" else null end as date)))) * 12 + "
                + "date_part('month',age(cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentD\" else null end as date), "
                + "cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentC\" else null end as date))))"));
  }

  @Test
  void testOizp() {
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);

    String sql = test("66 + d2:oizp(#{ProgrmStagA.DataElmentA} + 4)");
    assertThat(
        sql,
        is(
            "66::numeric + coalesce(case when case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end + 4::numeric >= 0 then 1 else 0 end, 0)"));
  }

  @Test
  void testRelationshipCountWithNoRelationshipId() {
    String sql = test("d2:relationshipCount()");
    assertThat(
        sql,
        is(
            """
             (select sum(relationship_count)
              from analytics_rs_relationship arr
              where arr.trackedentityid = ax.trackedentity)
             """));
  }

  @Test
  void testRelationshipCountWithRelationshipId() {
    when(idObjectManager.get(RelationshipType.class, relTypeA.getUid())).thenReturn(relTypeA);

    String sql = test("d2:relationshipCount('RelatnTypeA')");
    assertThat(
        sql,
        is(
            """
                     (select relationship_count
                      from analytics_rs_relationship arr
                      where arr.trackedentityid = ax.trackedentity and relationshiptypeuid = 'RelatnTypeA')
                     """));
  }

  @Test
  void testWeeksBetween() {
    when(idObjectManager.get(DataElement.class, dataElementC.getUid())).thenReturn(dataElementC);
    when(idObjectManager.get(DataElement.class, dataElementD.getUid())).thenReturn(dataElementD);
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);

    String sql = test("d2:weeksBetween(#{ProgrmStagA.DataElmentC},#{ProgrmStagA.DataElmentD})");
    assertThat(
        sql,
        is(
            "((cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentD\" else null end as date) - "
                + "cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentC\" else null end as date)) / 7)"));
  }

  @Test
  void testYearsBetween() {
    String sql = test("d2:yearsBetween(V{enrollment_date}, V{analytics_period_start})");
    assertThat(
        sql,
        is("(date_part('year',age(cast('2020-01-01' as date), cast(enrollmentdate as date))))"));
  }

  @Test
  void testYearsBetweenWithProgramStage() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);

    programIndicator.setAnalyticsType(ENROLLMENT);

    String sql = test("d2:yearsBetween(V{enrollment_date}, PS_EVENTDATE:ProgrmStagA)");
    assertThat(
        sql,
        is(
            "(date_part('year',age(cast((select occurreddate from analytics_event_Program000A "
                + "where analytics_event_Program000A.enrollment = ax.enrollment and occurreddate is not null "
                + "and ps = 'ProgrmStagA' "
                + "order by occurreddate desc limit 1 ) as date), cast(enrollmentdate as date))))"));
  }

  @Test
  void testYearsBetweenWithProgramStageAndBoundaries() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);

    setAllBoundaries();

    String sql = test("d2:yearsBetween(V{enrollment_date}, PS_EVENTDATE:ProgrmStagA) < 1");
    assertThat(
        sql,
        is(
            "(date_part('year',age(cast((select occurreddate from analytics_event_Program000A "
                + "where analytics_event_Program000A.enrollment = ax.enrollment and occurreddate is not null "
                + "and occurreddate < cast( '2021-01-01' as date ) and occurreddate >= cast( '2020-01-01' as date ) "
                + "and ps = 'ProgrmStagA' "
                + "order by occurreddate desc limit 1 ) as date), cast(enrollmentdate as date)))) < 1::numeric"));
  }

  @Test
  void testZing() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);

    String sql = test("d2:zing(#{ProgrmStagA.DataElmentA})");
    assertThat(
        sql,
        is(
            "greatest(0,coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end::numeric,0))"));
  }

  @Test
  void testZpvcOneArg() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);

    String sql = test("d2:zpvc(#{ProgrmStagA.DataElmentA})");
    assertThat(
        sql,
        is(
            "nullif(cast(("
                + "case when case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end >= 0 then 1 else 0 end"
                + ") as double precision),0)"));
  }

  @Test
  void testZpvcTwoArgs() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);
    when(idObjectManager.get(DataElement.class, dataElementB.getUid())).thenReturn(dataElementB);

    String sql = test("d2:zpvc(#{ProgrmStagA.DataElmentA},#{ProgrmStagA.DataElmentB})");
    assertThat(
        sql,
        is(
            "nullif(cast(("
                + "case when case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end >= 0 then 1 else 0 end + "
                + "case when case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentB\" else null end >= 0 then 1 else 0 end"
                + ") as double precision),0)"));
  }

  @Test
  void testLog() {
    String sql = test("log(V{enrollment_count})");
    assertThat(sql, is("ln(distinct enrollment)"));

    sql = test("log(V{event_count},3)");
    assertThat(sql, is("log(3::numeric,case " + DEFAULT_COUNT_CONDITION + " end)"));
  }

  @Test
  void testLog10() {
    String sql = test("log10(V{org_unit_count})");
    assertThat(sql, is("log(distinct ou)"));
  }

  @Test
  void testIllegalFunction() {
    assertThrows(ParserException.class, () -> test("d2:zztop(#{ProgrmStagA.DataElmentA})"));
  }

  @Test
  void testVectorAvg() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);

    String sql = test("avg(#{ProgrmStagA.DataElmentA})");
    assertThat(
        sql,
        is(
            "avg(coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end::numeric,0))"));
  }

  @Test
  void testVectorCount() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);

    String sql = test("count(#{ProgrmStagA.DataElmentA})");
    assertThat(
        sql,
        is(
            "count(coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end::numeric,0))"));

    String sql2 = test("count(distinct #{ProgrmStagA.DataElmentA})");
    assertThat(
        sql2,
        is(
            "count(distinct coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end::numeric,0))"));
  }

  @Test
  void testVectorMax() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);

    String sql = test("max(#{ProgrmStagA.DataElmentA})");
    assertThat(
        sql,
        is(
            "max(coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end::numeric,0))"));
  }

  @Test
  void testVectorMin() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);

    String sql = test("min(#{ProgrmStagA.DataElmentA})");
    assertThat(
        sql,
        is(
            "min(coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end::numeric,0))"));
  }

  @Test
  void testVectorStddev() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);

    String sql = test("stddev(#{ProgrmStagA.DataElmentA})");
    assertThat(
        sql,
        is(
            "stddev_samp(coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end::numeric,0))"));
  }

  @Test
  void testVectorSum() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);

    String sql = test("sum(#{ProgrmStagA.DataElmentA})");
    assertThat(
        sql,
        is(
            "sum(coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end::numeric,0))"));
  }

  @Test
  void testVectorVariance() {
    when(programStageService.getProgramStage(programStageA.getUid())).thenReturn(programStageA);
    when(idObjectManager.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);

    String sql = test("variance(#{ProgrmStagA.DataElmentA})");
    assertThat(
        sql,
        is(
            "variance(coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end::numeric,0))"));
  }

  @Test
  void testCompareStrings() {
    String sql = test("'a' < \"b\"");
    assertThat(sql, is("'a' < 'b'"));
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private String test(String expression) {
    return test(expression, NUMERIC);
  }

  private String test(String expression, DataType dataType) {
    test(expression, new DefaultLiteral(), ITEM_GET_DESCRIPTIONS, dataType);

    return castString(
        test(expression, new SqlLiteral(new PostgreSqlBuilder()), ITEM_GET_SQL, dataType));
  }

  private Object test(
      String expression,
      AntlrExprLiteral exprLiteral,
      ExpressionItemMethod itemMethod,
      DataType dataType) {
    Set<String> dataElementsAndAttributesIdentifiers = new LinkedHashSet<>();
    dataElementsAndAttributesIdentifiers.add(BASE_UID + "a");
    dataElementsAndAttributesIdentifiers.add(BASE_UID + "b");
    dataElementsAndAttributesIdentifiers.add(BASE_UID + "c");

    ExpressionParams params = ExpressionParams.builder().dataType(dataType).build();

    ProgramExpressionParams progParams =
        ProgramExpressionParams.builder()
            .programIndicator(programIndicator)
            .reportingStartDate(startDate)
            .reportingEndDate(endDate)
            .dataElementAndAttributeIdentifiers(dataElementsAndAttributesIdentifiers)
            .build();

    CommonExpressionVisitor visitor =
        CommonExpressionVisitor.builder()
            .idObjectManager(idObjectManager)
            .dimensionService(dimensionService)
            .programIndicatorService(programIndicatorService)
            .programStageService(programStageService)
            .i18nSupplier(() -> new I18n(null, null))
            .itemMap(new ExpressionMapBuilder().getExpressionItemMap())
            .itemMethod(itemMethod)
            .params(params)
            .progParams(progParams)
            .sqlBuilder(new PostgreSqlBuilder())
            .build();

    visitor.setExpressionLiteral(exprLiteral);

    return Parser.visit(expression, visitor);
  }

  private void setStartEventBoundary() {
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    boundaries.add(
        new AnalyticsPeriodBoundary(
            AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD,
            null,
            0));

    setBoundaries(boundaries);
  }

  private void setEndEventBoundary() {
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    boundaries.add(
        new AnalyticsPeriodBoundary(
            AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD,
            null,
            0));

    setBoundaries(boundaries);
  }

  private void setStartAndEndEventBoundary() {
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    boundaries.add(
        new AnalyticsPeriodBoundary(
            AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD,
            null,
            0));
    boundaries.add(
        new AnalyticsPeriodBoundary(
            AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD,
            null,
            0));

    setBoundaries(boundaries);
  }

  private void setAllBoundaries() {
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    boundaries.add(
        new AnalyticsPeriodBoundary(
            AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD,
            null,
            0));
    boundaries.add(
        new AnalyticsPeriodBoundary(
            AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD,
            null,
            0));
    boundaries.add(
        new AnalyticsPeriodBoundary(
            AnalyticsPeriodBoundary.ENROLLMENT_DATE,
            AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD,
            null,
            0));
    boundaries.add(
        new AnalyticsPeriodBoundary(
            AnalyticsPeriodBoundary.ENROLLMENT_DATE,
            AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD,
            null,
            0));

    setBoundaries(boundaries);
  }

  private void setBoundaries(Set<AnalyticsPeriodBoundary> boundaries) {
    programIndicator.setAnalyticsPeriodBoundaries(boundaries);
    programIndicator.setAnalyticsType(ENROLLMENT);
  }

  private String normalize(String sql) {
    return sql.replaceAll("\\s+", " ").trim();
  }
}
