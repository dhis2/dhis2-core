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

import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.junit.Assert.*;

import java.util.*;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.relationship.RelationshipType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jim Grace
 */
public class ProgramIndicatorServiceD2FunctionTest
    extends DhisSpringTest
{
    @Autowired
    private ProgramIndicatorService programIndicatorService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    private ProgramStage psA;

    private ProgramStage psB;

    private Program programA;

    private Program programB;

    private DataElement deA;

    private DataElement deB;

    private DataElement deD;

    private ProgramIndicator piA;

    private ProgramIndicator piB;

    private RelationshipType relationshipTypeA;

    private Date newDate = new GregorianCalendar( 2020, Calendar.JANUARY, 9 ).getTime();

    @Override
    public void setUpTest()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        // ---------------------------------------------------------------------
        // Program
        // ---------------------------------------------------------------------

        programA = createProgram( 'A', new HashSet<>(), organisationUnit );
        programA.setUid( "Program000A" );
        programService.addProgram( programA );

        psA = new ProgramStage( "StageA", programA );
        psA.setSortOrder( 1 );
        psA.setUid( "ProgrmStagA" );
        programStageService.saveProgramStage( psA );

        psB = new ProgramStage( "StageB", programA );
        psB.setSortOrder( 2 );
        psB.setUid( "ProgrmStagB" );
        programStageService.saveProgramStage( psB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( psA );
        programStages.add( psB );
        programA.setProgramStages( programStages );
        programService.updateProgram( programA );

        programB = createProgram( 'B', new HashSet<>(), organisationUnit );
        programB.setUid( "Program000B" );
        programService.addProgram( programB );

        // ---------------------------------------------------------------------
        // Program Stage DE
        // ---------------------------------------------------------------------

        deA = createDataElement( 'A' );
        deA.setDomainType( DataElementDomain.TRACKER );
        deA.setUid( "DataElmentA" );

        deB = createDataElement( 'B' );
        deB.setDomainType( DataElementDomain.TRACKER );
        deB.setUid( "DataElmentB" );

        deD = createDataElement( 'D' );
        deD.setDomainType( DataElementDomain.TRACKER );
        deD.setUid( "DataElmentD" );
        deD.setValueType( ValueType.DATE );

        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deD );

        ProgramStageDataElement stageDataElementA = new ProgramStageDataElement( psA, deA, false, 1 );
        ProgramStageDataElement stageDataElementB = new ProgramStageDataElement( psA, deB, false, 2 );
        ProgramStageDataElement stageDataElementD = new ProgramStageDataElement( psA, deD, false, 3 );
        ProgramStageDataElement stageDataElementE = new ProgramStageDataElement( psB, deA, false, 1 );
        ProgramStageDataElement stageDataElementF = new ProgramStageDataElement( psB, deB, false, 2 );

        programStageDataElementService.addProgramStageDataElement( stageDataElementA );
        programStageDataElementService.addProgramStageDataElement( stageDataElementB );
        programStageDataElementService.addProgramStageDataElement( stageDataElementD );
        programStageDataElementService.addProgramStageDataElement( stageDataElementE );
        programStageDataElementService.addProgramStageDataElement( stageDataElementF );

        // ---------------------------------------------------------------------
        // ProgramIndicator
        // ---------------------------------------------------------------------

        piA = createProgramIndicator( 'A', programA, "20", null );
        programA.getProgramIndicators().add( piA );

        piB = createProgramIndicator( 'B', programA, "70", null );
        piB.setAnalyticsType( AnalyticsType.ENROLLMENT );
        programA.getProgramIndicators().add( piB );

        // ---------------------------------------------------------------------
        // RelationshipType
        // ---------------------------------------------------------------------

        relationshipTypeA = createRelationshipType( 'A' );
        relationshipTypeA.setUid( "RelatioTypA" );
    }

    private String getSql( String expression )
    {
        return programIndicatorService.getAnalyticsSql( expression, NUMERIC, piA, newDate, newDate );
    }

    private String getSqlEnrollment( String expression )
    {
        return programIndicatorService.getAnalyticsSql( expression, NUMERIC, piB, newDate, newDate );
    }

    // -------------------------------------------------------------------------
    // D2 function tests (in alphabetical order)
    // -------------------------------------------------------------------------

    @Test
    public void testD2Condition()
    {
        assertEquals(
            "case when ((\"DataElmentA\" is not null)) then 1 + 4 else nullif(cast((case when \"DataElmentB\" >= 0 then 1 else 0 end) as double precision),0) end",
            getSql(
                "d2:condition( 'd2:hasValue(#{ProgrmStagA.DataElmentA})', 1+4, d2:zpvc(#{Program000B.DataElmentB}) )" ) );

        assertEquals(
            "case when (((select \"DataElmentA\" from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentA\" is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 ) is not null)) "
                +
                "then 1 + 4 else nullif(cast((case when (select \"DataElmentB\" from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentB\" is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'Program000B' order by executiondate desc limit 1 ) >= 0 then 1 else 0 end) as double precision),0) end",
            getSqlEnrollment(
                "d2:condition( \"d2:hasValue(#{ProgrmStagA.DataElmentA})\", 1+4, d2:zpvc(#{Program000B.DataElmentB}) )" ) );
    }

    @Test
    public void testD2Count()
    {
        assertEquals(
            "(select count(\"DataElmentA\") from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentA\" is not null and \"DataElmentA\" is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
            getSql( "d2:count(#{ProgrmStagA.DataElmentA})" ) );

        assertEquals(
            "(select count(\"DataElmentA\") from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentA\" is not null and \"DataElmentA\" is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
            getSqlEnrollment( "d2:count(#{ProgrmStagA.DataElmentA})" ) );
    }

    @Test
    public void testD2CountIfCondition()
    {
        assertEquals(
            "(select count(\"DataElmentA\") from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentA\" is not null and \"DataElmentA\" >= coalesce(\"DataElmentB\"::numeric,0) and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
            getSql( "d2:countIfCondition( #{ProgrmStagA.DataElmentA}, ' >= #{Program000B.DataElmentB}')" ) );

        assertEquals(
            "(select count(\"DataElmentA\") from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentA\" is not null and \"DataElmentA\" >= coalesce("
                +
                "(select \"DataElmentB\" from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentB\" is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'Program000B' order by executiondate desc limit 1 )::numeric,0) "
                +
                "and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
            getSqlEnrollment(
                "d2:countIfCondition( #{ProgrmStagA.DataElmentA}, \" >= #{Program000B.DataElmentB}\")" ) );
    }

    @Test
    public void testD2CountIfValue()
    {
        assertEquals(
            "(select count(\"DataElmentA\") from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentA\" is not null and \"DataElmentA\" = 10 and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
            getSql( "d2:countIfValue(#{ProgrmStagA.DataElmentA}, 10)" ) );

        assertEquals(
            "(select count(\"DataElmentA\") from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentA\" is not null and \"DataElmentA\" = 10 and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
            getSqlEnrollment( "d2:countIfValue(#{ProgrmStagA.DataElmentA}, 10)" ) );
    }

    @Test
    public void testD2DaysBetween()
    {
        assertEquals( "(cast(executiondate as date) - cast(\"DataElmentD\" as date))",
            getSql( "d2:daysBetween(#{ProgrmStagA.DataElmentD}, PS_EVENTDATE:ProgrmStagA)" ) );

        assertEquals(
            "(cast((select executiondate from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and executiondate is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 ) as date) "
                +
                "- cast((select \"DataElmentD\" from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentD\" is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 ) as date))",
            getSqlEnrollment( "d2:daysBetween(#{ProgrmStagA.DataElmentD}, PS_EVENTDATE:ProgrmStagA)" ) );
    }

    @Test
    public void testD2HasValue()
    {
        assertEquals( "(\"DataElmentA\" is not null)",
            getSql( "d2:hasValue(#{ProgrmStagA.DataElmentA})" ) );

        assertEquals(
            "((select \"DataElmentA\" from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentA\" is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 ) is not null)",
            getSqlEnrollment( "d2:hasValue(#{ProgrmStagA.DataElmentA})" ) );
    }

    @Test
    public void testD2MaxValue()
    {
        assertEquals( "\"DataElmentA\"",
            getSql( "d2:maxValue(#{ProgrmStagA.DataElmentA})" ) );

        assertEquals(
            "(select max(\"DataElmentA\") from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
            getSqlEnrollment( "d2:maxValue(#{ProgrmStagA.DataElmentA})" ) );

        assertEquals( "\"executiondate\"",
            getSql( "d2:maxValue(PS_EVENTDATE:ProgrmStagA)" ) );

        assertEquals(
            "(select max(\"executiondate\") from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
            getSqlEnrollment( "d2:maxValue(PS_EVENTDATE:ProgrmStagA)" ) );
    }

    @Test
    public void testD2MinutesBetween()
    {
        assertEquals(
            "(extract(epoch from (cast(executiondate as timestamp) - cast(\"DataElmentD\" as timestamp))) / 60)",
            getSql( "d2:minutesBetween(#{ProgrmStagA.DataElmentD}, PS_EVENTDATE:ProgrmStagA)" ) );

        assertEquals(
            "(extract(epoch from (cast((select executiondate from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and executiondate is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 ) as timestamp) "
                +
                "- cast((select \"DataElmentD\" from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentD\" is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 ) as timestamp))) / 60)",
            getSqlEnrollment( "d2:minutesBetween(#{ProgrmStagA.DataElmentD}, PS_EVENTDATE:ProgrmStagA)" ) );
    }

    @Test
    public void testD2MinValue()
    {
        assertEquals( "\"DataElmentA\"",
            getSql( "d2:minValue(#{ProgrmStagA.DataElmentA})" ) );

        assertEquals(
            "(select min(\"DataElmentA\") from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
            getSqlEnrollment( "d2:minValue(#{ProgrmStagA.DataElmentA})" ) );

        assertEquals( "\"executiondate\"",
            getSql( "d2:minValue(PS_EVENTDATE:ProgrmStagA)" ) );

        assertEquals(
            "(select min(\"executiondate\") from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
            getSqlEnrollment( "d2:minValue(PS_EVENTDATE:ProgrmStagA)" ) );
    }

    @Test
    public void testD2MonthsBetween()
    {
        assertEquals(
            "((date_part('year',age(cast(executiondate as date), cast(\"DataElmentD\" as date)))) * 12 + date_part('month',age(cast(executiondate as date), cast(\"DataElmentD\" as date))))",
            getSql( "d2:monthsBetween(#{ProgrmStagA.DataElmentD}, PS_EVENTDATE:ProgrmStagA)" ) );

        assertEquals(
            "((date_part('year',age(cast((select executiondate from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and executiondate is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 ) as date), "
                +
                "cast((select \"DataElmentD\" from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentD\" is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 ) as date)))) "
                +
                "* 12 + date_part('month',age(cast((select executiondate from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and executiondate is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 ) as date), "
                +
                "cast((select \"DataElmentD\" from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentD\" is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 ) as date))))",
            getSqlEnrollment( "d2:monthsBetween(#{ProgrmStagA.DataElmentD}, PS_EVENTDATE:ProgrmStagA)" ) );
    }

    @Test
    public void testD2Oizp()
    {
        assertEquals(
            "((date_part('year',age(cast(executiondate as date), cast(\"DataElmentA\" as date)))) * 12 + date_part('month',age(cast(executiondate as date), cast(\"DataElmentA\" as date))))",
            getSql( "d2:monthsBetween(#{ProgrmStagA.DataElmentA}, PS_EVENTDATE:ProgrmStagA)" ) );

        assertEquals( "coalesce(case when \"DataElmentA\" >= 0 then 1 else 0 end, 0)",
            getSql( "d2:oizp(#{ProgrmStagA.DataElmentA})" ) );

        assertEquals(
            "coalesce(case when (select \"DataElmentA\" from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentA\" is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 ) >= 0 then 1 else 0 end, 0)",
            getSqlEnrollment( "d2:oizp(#{ProgrmStagA.DataElmentA})" ) );
    }

    @Test
    public void testD2RelationshipCount()
    {
        assertEquals(
            "(select count(*) from relationship r join relationshipitem rifrom on rifrom.relationshipid = r.relationshipid join trackedentityinstance tei on rifrom.trackedentityinstanceid = tei.trackedentityinstanceid and tei.uid = ax.tei)",
            getSql( "d2:relationshipCount()" ) );

        assertEquals(
            "(select count(*) from relationship r join relationshipitem rifrom on rifrom.relationshipid = r.relationshipid join trackedentityinstance tei on rifrom.trackedentityinstanceid = tei.trackedentityinstanceid and tei.uid = ax.tei)",
            getSqlEnrollment( "d2:relationshipCount()" ) );

        assertEquals(
            "(select count(*) from relationship r join relationshiptype rt on r.relationshiptypeid = rt.relationshiptypeid and rt.uid = 'RelatioTypA' join relationshipitem rifrom on rifrom.relationshipid = r.relationshipid join trackedentityinstance tei on rifrom.trackedentityinstanceid = tei.trackedentityinstanceid and tei.uid = ax.tei)",
            getSql( "d2:relationshipCount('RelatioTypA')" ) );

        assertEquals(
            "(select count(*) from relationship r join relationshiptype rt on r.relationshiptypeid = rt.relationshiptypeid and rt.uid = 'RelatioTypA' join relationshipitem rifrom on rifrom.relationshipid = r.relationshipid join trackedentityinstance tei on rifrom.trackedentityinstanceid = tei.trackedentityinstanceid and tei.uid = ax.tei)",
            getSqlEnrollment( "d2:relationshipCount('RelatioTypA')" ) );
    }

    @Test
    public void testD2WeeksBetween()
    {
        assertEquals( "((cast(executiondate as date) - cast(\"DataElmentA\" as date))/7)",
            getSql( "d2:weeksBetween(#{ProgrmStagA.DataElmentA}, PS_EVENTDATE:ProgrmStagA)" ) );

        assertEquals(
            "((cast((select executiondate from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and executiondate is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 ) as date) "
                +
                "- cast((select \"DataElmentD\" from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentD\" is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 ) as date))/7)",
            getSqlEnrollment( "d2:weeksBetween(#{ProgrmStagA.DataElmentD}, PS_EVENTDATE:ProgrmStagA)" ) );
    }

    @Test
    public void testD2YearsBetween()
    {
        assertEquals( "(date_part('year',age(cast(executiondate as date), cast(\"DataElmentA\" as date))))",
            getSql( "d2:yearsBetween(#{ProgrmStagA.DataElmentA}, PS_EVENTDATE:ProgrmStagA)" ) );

        assertEquals( "(date_part('year',age(cast((select executiondate from analytics_event_Program000A " +
            "where analytics_event_Program000A.pi = ax.pi and executiondate is not null and executiondate < " +
            "cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 ) as date), "
            +
            "cast((select \"DataElmentD\" from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentD\" is not null and executiondate < "
            +
            "cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 ) as date))))",
            getSqlEnrollment( "d2:yearsBetween(#{ProgrmStagA.DataElmentD}, PS_EVENTDATE:ProgrmStagA)" ) );
    }

    @Test
    public void testD2Zing()
    {
        assertEquals( "greatest(0,coalesce(\"DataElmentA\"::numeric,0) + 5)",
            getSql( "d2:zing(#{ProgrmStagA.DataElmentA} + 5)" ) );

        assertEquals(
            "greatest(0,coalesce((select \"DataElmentA\" from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentA\" is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 )::numeric,0) + 5)",
            getSqlEnrollment( "d2:zing(#{ProgrmStagA.DataElmentA} + 5)" ) );
    }

    @Test
    public void testD2Zpvc()
    {
        assertEquals(
            "nullif(cast((case when \"DataElmentA\" >= 0 then 1 else 0 end + case when \"DataElmentB\" >= 0 then 1 else 0 end) as double precision),0)",
            getSql( "d2:zpvc(#{ProgrmStagA.DataElmentA},#{ProgrmStagB.DataElmentB})" ) );

        assertEquals(
            "nullif(cast((case when (select \"DataElmentA\" from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentA\" is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 ) >= 0 then 1 else 0 end "
                +
                "+ case when (select \"DataElmentB\" from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and \"DataElmentB\" is not null and executiondate < cast( '2020-01-10' as date ) and executiondate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagB' order by executiondate desc limit 1 ) >= 0 then 1 else 0 end) as double precision),0)",
            getSqlEnrollment( "d2:zpvc(#{ProgrmStagA.DataElmentA},#{ProgrmStagB.DataElmentB})" ) );
    }
}
