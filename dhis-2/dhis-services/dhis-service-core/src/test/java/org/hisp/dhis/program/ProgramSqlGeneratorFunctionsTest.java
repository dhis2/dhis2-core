/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.jdbc.statementbuilder.PostgreSQLStatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.parser.expression.Parser;
import org.hisp.dhis.parser.expression.ParserException;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.parser.expression.ParserUtils.castString;
import static org.hisp.dhis.program.AnalyticsType.ENROLLMENT;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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

    private ProgramStage programStageA;
    private ProgramStage programStageB;

    private TrackedEntityAttribute attributeA;

    private Date startDate = getDate( 2020, 1, 1 );

    private Date endDate = getDate( 2020, 12, 31 );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private ProgramIndicatorService programIndicatorService;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private TrackedEntityAttributeService attributeService;

    private StatementBuilder statementBuilder;

    @Before
    public void setUp()
    {
        dataElementA = createDataElement( 'A' );
        dataElementA.setDomainType( DataElementDomain.TRACKER );
        dataElementA.setUid( "DataElmentA" );

        dataElementB = createDataElement( 'B' );
        dataElementB.setDomainType( DataElementDomain.TRACKER );
        dataElementB.setUid( "DataElmenB" );

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

        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );
        when( programIndicatorService.getAnalyticsSql( anyString(), eq(programIndicator), eq(startDate), eq(endDate) ) )
            .thenAnswer( i -> test( (String)i.getArguments()[0] ) );
    }

    @Test
    public void testCondition()
    {
        String sql = test( "d2:condition('#{ProgrmStagA.DataElmentA} > 3',10 + 5,3 * 2)" );
        assertThat( sql, is( "case when (coalesce(\"DataElmentA\"::numeric,0) > 3) then 10 + 5 else 3 * 2 end" ) );
    }

    @Test
    public void testCount()
    {
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
        String sql = test( "d2:countIfCondition(#{ProgrmStagA.DataElmentA},'>5')" );
        assertThat( sql, is( "(select count(\"DataElmentA\") " +
            "from analytics_event_Program000A " +
            "where analytics_event_Program000A.pi = ax.pi " +
            "and \"DataElmentA\" is not null and \"DataElmentA\" > 5 and ps = 'ProgrmStagA')" ) );
    }

    @Test
    public void testCountIfValueNumeric()
    {
        String sql = test( "d2:countIfValue(#{ProgrmStagA.DataElmentA},55)");
        assertThat( sql, is( "(select count(\"DataElmentA\") " +
            "from analytics_event_Program000A " +
            "where analytics_event_Program000A.pi = ax.pi " +
            "and \"DataElmentA\" is not null and \"DataElmentA\" = 55 " +
            "and ps = 'ProgrmStagA')" ) );
    }

    @Test
    public void testCountIfValueString()
    {
        dataElementA.setValueType( TEXT );

        String sql = test( "d2:countIfValue(#{ProgrmStagA.DataElmentA},'ABC')");
        assertThat( sql, is( "(select count(\"DataElmentA\") " +
            "from analytics_event_Program000A " +
            "where analytics_event_Program000A.pi = ax.pi " +
            "and \"DataElmentA\" is not null and \"DataElmentA\" = 'ABC' " +
            "and ps = 'ProgrmStagA')" ) );
    }

    @Test
    public void testDaysBetween()
    {
        String sql = test( "d2:daysBetween(#{ProgrmStagA.DataElmentA},#{ProgrmStagB.DataElmentB})" );
        assertThat( sql, is( "(cast(\"DataElmentB\" as date) - cast(\"DataElmentA\" as date))" ) );
    }

    @Test
    public void testHasValueDataElement()
    {
        String sql = test( "d2:hasValue(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "(\"DataElmentA\" is not null)" ) );
    }

    @Test
    public void testHasValueAttribute()
    {
        String sql = test( "d2:hasValue(A{Attribute0A})" );
        assertThat( sql, is( "(\"Attribute0A\" is not null)" ) );
    }

    @Test
    public void testMinutesBetween()
    {
        String sql = test( "d2:minutesBetween(#{ProgrmStagA.DataElmentA},#{ProgrmStagB.DataElmentB})" );
        assertThat( sql, is( "(extract(epoch from (cast(\"DataElmentB\" as timestamp) - cast(\"DataElmentA\" as timestamp))) / 60)" ) );
    }

    @Test
    public void testMonthsBetween()
    {
        String sql = test( "d2:monthsBetween(#{ProgrmStagA.DataElmentA},#{ProgrmStagB.DataElmentB})" );
        assertThat( sql, is( "((date_part('year',age(cast(\"DataElmentB\" as date), cast(\"DataElmentA\"as date)))) * 12 +" +
                "date_part('month',age(cast(\"DataElmentB\" as date), cast(\"DataElmentA\"as date))))" ) );
    }

    @Test
    public void testOizp()
    {
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
        String sql = test( "d2:relationshipCount('Zx7OEwPBUwD')" );
        assertThat( sql, is( "(select count(*) from relationship r " +
            "join relationshiptype rt on r.relationshiptypeid = rt.relationshiptypeid and rt.uid = 'Zx7OEwPBUwD' " +
            "join relationshipitem rifrom on rifrom.relationshipid = r.relationshipid " +
            "join trackedentityinstance tei on rifrom.trackedentityinstanceid = tei.trackedentityinstanceid and tei.uid = ax.tei)" ) );
    }

    @Test
    public void testWeeksBetween()
    {
        String sql = test( "d2:weeksBetween(#{ProgrmStagA.DataElmentA},#{ProgrmStagB.DataElmentB})" );
        assertThat( sql, is( "((cast(\"DataElmentB\" as date) - cast(\"DataElmentA\" as date))/7)" ) );
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
        String sql = test( "d2:zing(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "greatest(0,coalesce(\"DataElmentA\"::numeric,0))" ) );
    }

    @Test
    public void testZpvcOneArg()
    {
        String sql = test( "d2:zpvc(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "nullif(cast((" +
            "case when \"DataElmentA\" >= 0 then 1 else 0 end" +
            ") as double precision),0)" ) );
    }

    @Test
    public void testZpvcTwoArgs()
    {
        String sql = test( "d2:zpvc(#{ProgrmStagA.DataElmentA},#{ProgrmStagA.DataElmentB})" );
        assertThat( sql, is( "nullif(cast((" +
            "case when \"DataElmentA\" >= 0 then 1 else 0 end + " +
            "case when \"DataElmentB\" >= 0 then 1 else 0 end" +
            ") as double precision),0)" ) );
    }

    @Test
    public void testIllegalFunction()
    {
        thrown.expect( ParserException.class );
        test( "d2:zztop(#{ProgrmStagA.DataElmentA})" );
    }

    @Test
    public void testAggAvg()
    {
        String sql = test( "avg(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "avg(coalesce(\"DataElmentA\"::numeric,0))" ) );
    }

    @Test
    public void testAggCount()
    {
        String sql = test( "count(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "count(coalesce(\"DataElmentA\"::numeric,0))" ) );
    }

    @Test
    public void testAggMax()
    {
        String sql = test( "max(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "max(coalesce(\"DataElmentA\"::numeric,0))" ) );
    }

    @Test
    public void testAggMin()
    {
        String sql = test( "min(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "min(coalesce(\"DataElmentA\"::numeric,0))" ) );
    }

    @Test
    public void testAggStddev()
    {
        String sql = test( "stddev(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "stddev(coalesce(\"DataElmentA\"::numeric,0))" ) );
    }

    @Test
    public void testAggSum()
    {
        String sql = test( "sum(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "sum(coalesce(\"DataElmentA\"::numeric,0))" ) );
    }

    @Test
    public void testAggVariance()
    {
        String sql = test( "variance(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "variance(coalesce(\"DataElmentA\"::numeric,0))" ) );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private String test( String expression )
    {
        Set<String> dataElementsAndAttributesIdentifiers = new LinkedHashSet<>();
        dataElementsAndAttributesIdentifiers.add( BASE_UID + "a" );
        dataElementsAndAttributesIdentifiers.add( BASE_UID + "b" );
        dataElementsAndAttributesIdentifiers.add( BASE_UID + "c" );

        ProgramSqlGenerator programSqlGenerator = new ProgramSqlGenerator( programIndicator, startDate, endDate,
            dataElementsAndAttributesIdentifiers, new HashMap<>(), programIndicatorService, statementBuilder,
            dataElementService, attributeService );

        return castString( Parser.visit( expression, programSqlGenerator ) );
    }

    private void setStartEventBoundary()
    {
        Set<AnalyticsPeriodBoundary> boundaries = new HashSet<AnalyticsPeriodBoundary>();
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE, AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD, null, 0 ) );

        setBoundaries( boundaries );
    }

    private void setEndEventBoundary()
    {
        Set<AnalyticsPeriodBoundary> boundaries = new HashSet<AnalyticsPeriodBoundary>();
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE, AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD, null, 0 ) );

        setBoundaries( boundaries );
    }

    private void setStartAndEndEventBoundary()
    {
        Set<AnalyticsPeriodBoundary> boundaries = new HashSet<AnalyticsPeriodBoundary>();
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE, AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD, null, 0 ) );
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE, AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD, null, 0 ) );

        setBoundaries( boundaries );
    }

    private void setAllBoundaries()
    {
        Set<AnalyticsPeriodBoundary> boundaries = new HashSet<AnalyticsPeriodBoundary>();
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE, AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD, null, 0 ) );
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE, AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD, null, 0 ) );
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.ENROLLMENT_DATE, AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD, null, 0 ) );
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.ENROLLMENT_DATE, AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD, null, 0 ) );

        setBoundaries( boundaries );
    }

    private void setBoundaries( Set<AnalyticsPeriodBoundary> boundaries )
    {
        programIndicator.setAnalyticsPeriodBoundaries( boundaries );
        programIndicator.setAnalyticsType( ENROLLMENT );
    }
}