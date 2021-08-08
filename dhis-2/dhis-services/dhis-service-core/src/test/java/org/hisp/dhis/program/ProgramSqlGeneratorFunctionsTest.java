/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.program;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.antlr.AntlrParserUtils.castString;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.parser.expression.ParserUtils.DEFAULT_SAMPLE_PERIODS;
import static org.hisp.dhis.parser.expression.ParserUtils.ITEM_GET_DESCRIPTIONS;
import static org.hisp.dhis.parser.expression.ParserUtils.ITEM_GET_SQL;
import static org.hisp.dhis.program.AnalyticsType.ENROLLMENT;
import static org.hisp.dhis.program.DefaultProgramIndicatorService.PROGRAM_INDICATOR_ITEMS;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.antlr.AntlrExprLiteral;
import org.hisp.dhis.antlr.Parser;
import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.antlr.literal.DefaultLiteral;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.jdbc.statementbuilder.PostgreSQLStatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItemMethod;
import org.hisp.dhis.parser.expression.literal.SqlLiteral;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Jim Grace
 */
public class ProgramSqlGeneratorFunctionsTest
    extends DhisConvenienceTest
{
    private ProgramIndicator programIndicator;

    private Program programA;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataElement dataElementC;

    private DataElement dataElementD;

    private ProgramStage programStageA;

    private ProgramStage programStageB;

    private TrackedEntityAttribute attributeA;

    private RelationshipType relTypeA;

    private Date startDate = getDate( 2020, 1, 1 );

    private Date endDate = getDate( 2020, 12, 31 );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ProgramIndicatorService programIndicatorService;

    @Mock
    private ConstantService constantService;

    @Mock
    private ProgramStageService programStageService;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private TrackedEntityAttributeService attributeService;

    @Mock
    private RelationshipTypeService relationshipTypeService;

    private StatementBuilder statementBuilder;

    @Before
    public void setUp()
    {
        dataElementA = createDataElement( 'A' );
        dataElementA.setDomainType( DataElementDomain.TRACKER );
        dataElementA.setUid( "DataElmentA" );

        dataElementB = createDataElement( 'B' );
        dataElementB.setDomainType( DataElementDomain.TRACKER );
        dataElementB.setUid( "DataElmentB" );

        dataElementC = createDataElement( 'C' );
        dataElementC.setDomainType( DataElementDomain.TRACKER );
        dataElementC.setUid( "DataElmentC" );
        dataElementC.setValueType( ValueType.DATE );

        dataElementD = createDataElement( 'D' );
        dataElementD.setDomainType( DataElementDomain.TRACKER );
        dataElementD.setUid( "DataElmentD" );
        dataElementD.setValueType( ValueType.DATE );

        attributeA = createTrackedEntityAttribute( 'A', ValueType.NUMBER );
        attributeA.setUid( "Attribute0A" );

        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );

        programStageA = new ProgramStage( "StageA", programA );
        programStageA.setSortOrder( 1 );
        programStageA.setUid( "ProgrmStagA" );

        programStageB = new ProgramStage( "StageB", programA );
        programStageB.setSortOrder( 2 );
        programStageB.setUid( "ProgrmStagB" );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( programStageA );
        programStages.add( programStageB );

        programA = createProgram( 'A', new HashSet<>(), organisationUnit );
        programA.setUid( "Program000A" );
        programA.setProgramStages( programStages );

        statementBuilder = new PostgreSQLStatementBuilder();

        programIndicator = new ProgramIndicator();
        programIndicator.setProgram( programA );
        programIndicator.setAnalyticsType( AnalyticsType.EVENT );

        relTypeA = new RelationshipType();
        relTypeA.setUid( "RelatnTypeA" );
    }

    @Test
    public void testCondition()
    {
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( programIndicatorService.getAnalyticsSql( anyString(), eq( programIndicator ), eq( startDate ),
            eq( endDate ) ) )
                .thenAnswer( i -> test( (String) i.getArguments()[0] ) );

        String sql = test( "d2:condition('#{ProgrmStagA.DataElmentA} > 3',10 + 5,3 * 2)" );
        assertThat( sql, is( "case when (coalesce(\"DataElmentA\"::numeric,0) > 3) then 10 + 5 else 3 * 2 end" ) );
    }

    @Test
    public void testCount()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );

        String sql = test( "d2:count(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "(select count(\"DataElmentA\") " +
            "from analytics_event_Program000A " +
            "where analytics_event_Program000A.pi = ax.pi " +
            "and \"DataElmentA\" is not null and \"DataElmentA\" is not null " +
            "and ps = 'ProgrmStagA')" ) );
    }

    @Test
    public void testCountWithStartEventBoundary()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );

        setStartEventBoundary();

        String sql = test( "d2:count(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "(select count(\"DataElmentA\") " +
            "from analytics_event_Program000A " +
            "where analytics_event_Program000A.pi = ax.pi " +
            "and \"DataElmentA\" is not null and \"DataElmentA\" is not null " +
            "and executiondate < cast( '2021-01-01' as date ) " +
            "and ps = 'ProgrmStagA')" ) );
    }

    @Test
    public void testCountWithEndEventBoundary()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );

        setEndEventBoundary();

        String sql = test( "d2:count(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "(select count(\"DataElmentA\") " +
            "from analytics_event_Program000A " +
            "where analytics_event_Program000A.pi = ax.pi " +
            "and \"DataElmentA\" is not null and \"DataElmentA\" is not null " +
            "and executiondate >= cast( '2020-01-01' as date ) " +
            "and ps = 'ProgrmStagA')" ) );
    }

    @Test
    public void testCountWithStartAndEndEventBoundary()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );

        setStartAndEndEventBoundary();

        String sql = test( "d2:count(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "(select count(\"DataElmentA\") " +
            "from analytics_event_Program000A " +
            "where analytics_event_Program000A.pi = ax.pi " +
            "and \"DataElmentA\" is not null and \"DataElmentA\" is not null " +
            "and executiondate < cast( '2021-01-01' as date ) and executiondate >= cast( '2020-01-01' as date ) " +
            "and ps = 'ProgrmStagA')" ) );
    }

    @Test
    public void testCountIfCondition()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );
        when( programIndicatorService.getAnalyticsSql( anyString(), eq( programIndicator ), eq( startDate ),
            eq( endDate ) ) )
                .thenAnswer( i -> test( (String) i.getArguments()[0] ) );

        String sql = test( "d2:countIfCondition(#{ProgrmStagA.DataElmentA},'>5')" );
        assertThat( sql, is( "(select count(\"DataElmentA\") " +
            "from analytics_event_Program000A " +
            "where analytics_event_Program000A.pi = ax.pi " +
            "and \"DataElmentA\" is not null and \"DataElmentA\" > 5 and ps = 'ProgrmStagA')" ) );
    }

    @Test
    public void testCountIfValueNumeric()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );

        String sql = test( "d2:countIfValue(#{ProgrmStagA.DataElmentA},55)" );
        assertThat( sql, is( "(select count(\"DataElmentA\") " +
            "from analytics_event_Program000A " +
            "where analytics_event_Program000A.pi = ax.pi " +
            "and \"DataElmentA\" is not null and \"DataElmentA\" = 55 " +
            "and ps = 'ProgrmStagA')" ) );
    }

    @Test
    public void testCountIfValueString()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );

        dataElementA.setValueType( TEXT );

        String sql = test( "d2:countIfValue(#{ProgrmStagA.DataElmentA},'ABC')" );
        assertThat( sql, is( "(select count(\"DataElmentA\") " +
            "from analytics_event_Program000A " +
            "where analytics_event_Program000A.pi = ax.pi " +
            "and \"DataElmentA\" is not null and \"DataElmentA\" = 'ABC' " +
            "and ps = 'ProgrmStagA')" ) );
    }

    @Test
    public void testDaysBetween()
    {
        when( dataElementService.getDataElement( dataElementC.getUid() ) ).thenReturn( dataElementC );
        when( dataElementService.getDataElement( dataElementD.getUid() ) ).thenReturn( dataElementD );
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( programStageService.getProgramStage( programStageB.getUid() ) ).thenReturn( programStageB );

        String sql = test( "d2:daysBetween(#{ProgrmStagA.DataElmentC},#{ProgrmStagB.DataElmentD})" );
        assertThat( sql, is( "(cast(\"DataElmentD\" as date) - cast(\"DataElmentC\" as date))" ) );
    }

    @Test
    public void testHasValueDataElement()
    {

        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );

        String sql = test( "d2:hasValue(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "(\"DataElmentA\" is not null)" ) );
    }

    @Test
    public void testHasValueAttribute()
    {
        when( attributeService.getTrackedEntityAttribute( attributeA.getUid() ) ).thenReturn( attributeA );

        String sql = test( "d2:hasValue(A{Attribute0A})" );
        assertThat( sql, is( "(\"Attribute0A\" is not null)" ) );
    }

    @Test
    public void testMinutesBetween()
    {
        when( dataElementService.getDataElement( dataElementC.getUid() ) ).thenReturn( dataElementC );
        when( dataElementService.getDataElement( dataElementD.getUid() ) ).thenReturn( dataElementD );
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( programStageService.getProgramStage( programStageB.getUid() ) ).thenReturn( programStageB );

        String sql = test( "d2:minutesBetween(#{ProgrmStagA.DataElmentC},#{ProgrmStagB.DataElmentD})" );
        assertThat( sql, is(
            "(extract(epoch from (cast(\"DataElmentD\" as timestamp) - cast(\"DataElmentC\" as timestamp))) / 60)" ) );
    }

    @Test
    public void testMonthsBetween()
    {
        when( dataElementService.getDataElement( dataElementC.getUid() ) ).thenReturn( dataElementC );
        when( dataElementService.getDataElement( dataElementD.getUid() ) ).thenReturn( dataElementD );
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( programStageService.getProgramStage( programStageB.getUid() ) ).thenReturn( programStageB );

        String sql = test( "d2:monthsBetween(#{ProgrmStagA.DataElmentC},#{ProgrmStagB.DataElmentD})" );

        assertThat( sql,
            is( "((date_part('year',age(cast(\"DataElmentD\" as date), cast(\"DataElmentC\" as date)))) * 12 + " +
                "date_part('month',age(cast(\"DataElmentD\" as date), cast(\"DataElmentC\" as date))))" ) );
    }

    @Test
    public void testOizp()
    {
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );

        String sql = test( "66 + d2:oizp(#{ProgrmStagA.DataElmentA} + 4)" );
        assertThat( sql, is( "66 + coalesce(case when \"DataElmentA\" + 4 >= 0 then 1 else 0 end, 0)" ) );
    }

    @Test
    public void testRelationshipCountWithNoRelationshipId()
    {
        String sql = test( "d2:relationshipCount()" );
        assertThat( sql, is( "(select count(*) from relationship r " +
            "join relationshipitem rifrom on rifrom.relationshipid = r.relationshipid " +
            "join trackedentityinstance tei on rifrom.trackedentityinstanceid = tei.trackedentityinstanceid and tei.uid = ax.tei)" ) );
    }

    @Test
    public void testRelationshipCountWithRelationshipId()
    {
        when( relationshipTypeService.getRelationshipType( relTypeA.getUid() ) ).thenReturn( relTypeA );

        String sql = test( "d2:relationshipCount('RelatnTypeA')" );
        assertThat( sql, is( "(select count(*) from relationship r " +
            "join relationshiptype rt on r.relationshiptypeid = rt.relationshiptypeid and rt.uid = 'RelatnTypeA' " +
            "join relationshipitem rifrom on rifrom.relationshipid = r.relationshipid " +
            "join trackedentityinstance tei on rifrom.trackedentityinstanceid = tei.trackedentityinstanceid and tei.uid = ax.tei)" ) );
    }

    @Test
    public void testWeeksBetween()
    {
        when( dataElementService.getDataElement( dataElementC.getUid() ) ).thenReturn( dataElementC );
        when( dataElementService.getDataElement( dataElementD.getUid() ) ).thenReturn( dataElementD );
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( programStageService.getProgramStage( programStageB.getUid() ) ).thenReturn( programStageB );

        String sql = test( "d2:weeksBetween(#{ProgrmStagA.DataElmentC},#{ProgrmStagB.DataElmentD})" );
        assertThat( sql, is( "((cast(\"DataElmentD\" as date) - cast(\"DataElmentC\" as date))/7)" ) );
    }

    @Test
    public void testYearsBetween()
    {
        String sql = test( "d2:yearsBetween(V{enrollment_date}, V{analytics_period_start})" );
        assertThat( sql, is( "(date_part('year',age(cast('2020-01-01' as date), cast(enrollmentdate as date))))" ) );
    }

    @Test
    public void testYearsBetweenWithProgramStage()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );

        programIndicator.setAnalyticsType( ENROLLMENT );

        String sql = test( "d2:yearsBetween(V{enrollment_date}, PS_EVENTDATE:ProgrmStagA)" );
        assertThat( sql, is( "(date_part('year',age(cast((select executiondate from analytics_event_Program000A " +
            "where analytics_event_Program000A.pi = ax.pi and executiondate is not null " +
            "and ps = 'ProgrmStagA' " +
            "order by executiondate desc limit 1 ) as date), cast(enrollmentdate as date))))" ) );
    }

    @Test
    public void testYearsBetweenWithProgramStageAndBoundaries()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );

        setAllBoundaries();

        String sql = test( "d2:yearsBetween(V{enrollment_date}, PS_EVENTDATE:ProgrmStagA) < 1" );
        assertThat( sql, is( "(date_part('year',age(cast((select executiondate from analytics_event_Program000A " +
            "where analytics_event_Program000A.pi = ax.pi and executiondate is not null " +
            "and executiondate < cast( '2021-01-01' as date ) and executiondate >= cast( '2020-01-01' as date ) " +
            "and ps = 'ProgrmStagA' " +
            "order by executiondate desc limit 1 ) as date), cast(enrollmentdate as date)))) < 1" ) );
    }

    @Test
    public void testZing()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );

        String sql = test( "d2:zing(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "greatest(0,coalesce(\"DataElmentA\"::numeric,0))" ) );
    }

    @Test
    public void testZpvcOneArg()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );

        String sql = test( "d2:zpvc(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "nullif(cast((" +
            "case when \"DataElmentA\" >= 0 then 1 else 0 end" +
            ") as double precision),0)" ) );
    }

    @Test
    public void testZpvcTwoArgs()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );
        when( dataElementService.getDataElement( dataElementB.getUid() ) ).thenReturn( dataElementB );

        String sql = test( "d2:zpvc(#{ProgrmStagA.DataElmentA},#{ProgrmStagA.DataElmentB})" );
        assertThat( sql, is( "nullif(cast((" +
            "case when \"DataElmentA\" >= 0 then 1 else 0 end + " +
            "case when \"DataElmentB\" >= 0 then 1 else 0 end" +
            ") as double precision),0)" ) );
    }

    @Test
    public void testLog()
    {
        String sql = test( "log(V{enrollment_count})" );
        assertThat( sql, is( "ln(distinct pi)" ) );

        sql = test( "log(V{event_count},3)" );
        assertThat( sql, is( "log(3,psi)" ) );
    }

    @Test
    public void testLog10()
    {
        String sql = test( "log10(V{org_unit_count})" );
        assertThat( sql, is( "log(distinct ou)" ) );
    }

    @Test
    public void testIllegalFunction()
    {
        assertThrows( ParserException.class,
            () -> test( "d2:zztop(#{ProgrmStagA.DataElmentA})" ) );
    }

    @Test
    public void testVectorAvg()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );

        String sql = test( "avg(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "avg(coalesce(\"DataElmentA\"::numeric,0))" ) );
    }

    @Test
    public void testVectorCount()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );

        String sql = test( "count(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "count(coalesce(\"DataElmentA\"::numeric,0))" ) );
    }

    @Test
    public void testVectorMax()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );

        String sql = test( "max(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "max(coalesce(\"DataElmentA\"::numeric,0))" ) );
    }

    @Test
    public void testVectorMin()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );

        String sql = test( "min(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "min(coalesce(\"DataElmentA\"::numeric,0))" ) );
    }

    @Test
    public void testVectorStddev()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );

        String sql = test( "stddev(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "stddev_samp(coalesce(\"DataElmentA\"::numeric,0))" ) );
    }

    @Test
    public void testVectorSum()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );

        String sql = test( "sum(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "sum(coalesce(\"DataElmentA\"::numeric,0))" ) );
    }

    @Test
    public void testVectorVariance()
    {
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );

        String sql = test( "variance(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "variance(coalesce(\"DataElmentA\"::numeric,0))" ) );
    }

    @Test
    public void testCompareStrings()
    {
        String sql = test( "'a' < \"b\"" );
        assertThat( sql, is( "'a' < 'b'" ) );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private String test( String expression )
    {
        test( expression, new DefaultLiteral(), ITEM_GET_DESCRIPTIONS );

        return castString( test( expression, new SqlLiteral(), ITEM_GET_SQL ) );
    }

    private Object test( String expression, AntlrExprLiteral exprLiteral,
        ExpressionItemMethod itemMethod )
    {
        Set<String> dataElementsAndAttributesIdentifiers = new LinkedHashSet<>();
        dataElementsAndAttributesIdentifiers.add( BASE_UID + "a" );
        dataElementsAndAttributesIdentifiers.add( BASE_UID + "b" );
        dataElementsAndAttributesIdentifiers.add( BASE_UID + "c" );

        CommonExpressionVisitor visitor = CommonExpressionVisitor.newBuilder()
            .withItemMap( PROGRAM_INDICATOR_ITEMS )
            .withItemMethod( itemMethod )
            .withConstantMap( constantService.getConstantMap() )
            .withProgramIndicatorService( programIndicatorService )
            .withProgramStageService( programStageService )
            .withDataElementService( dataElementService )
            .withAttributeService( attributeService )
            .withRelationshipTypeService( relationshipTypeService )
            .withStatementBuilder( statementBuilder )
            .withI18n( new I18n( null, null ) )
            .withSamplePeriods( DEFAULT_SAMPLE_PERIODS )
            .buildForProgramIndicatorExpressions();

        visitor.setExpressionLiteral( exprLiteral );
        visitor.setProgramIndicator( programIndicator );
        visitor.setReportingStartDate( startDate );
        visitor.setReportingEndDate( endDate );
        visitor.setDataElementAndAttributeIdentifiers( dataElementsAndAttributesIdentifiers );

        return Parser.visit( expression, visitor );
    }

    private void setStartEventBoundary()
    {
        Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD, null, 0 ) );

        setBoundaries( boundaries );
    }

    private void setEndEventBoundary()
    {
        Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD, null, 0 ) );

        setBoundaries( boundaries );
    }

    private void setStartAndEndEventBoundary()
    {
        Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD, null, 0 ) );
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD, null, 0 ) );

        setBoundaries( boundaries );
    }

    private void setAllBoundaries()
    {
        Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD, null, 0 ) );
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD, null, 0 ) );
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.ENROLLMENT_DATE,
            AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD, null, 0 ) );
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.ENROLLMENT_DATE,
            AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD, null, 0 ) );

        setBoundaries( boundaries );
    }

    private void setBoundaries( Set<AnalyticsPeriodBoundary> boundaries )
    {
        programIndicator.setAnalyticsPeriodBoundaries( boundaries );
        programIndicator.setAnalyticsType( ENROLLMENT );
    }
}