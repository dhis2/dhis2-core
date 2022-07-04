/*
 * Copyright (c) 2004-2022, University of Oslo
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
import static org.hamcrest.Matchers.startsWith;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.program.AnalyticsType.ENROLLMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jim Grace
 */
class ProgramIndicatorServiceVariableTest extends IntegrationTestBase
{

    @Autowired
    private ProgramIndicatorService programIndicatorService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    private Program programA;

    private ProgramIndicator piA;

    private ProgramIndicator piB;

    private Date startDate = new GregorianCalendar( 2020, Calendar.JANUARY, 1 ).getTime();

    private Date endDate = new GregorianCalendar( 2020, Calendar.JANUARY, 31 ).getTime();

    @Override
    public void setUpTest()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        programA = createProgram( 'A', new HashSet<>(), organisationUnit );
        programA.setUid( "Program000A" );
        programService.addProgram( programA );

        piA = createProgramIndicator( 'A', programA, "20", null );
        programA.getProgramIndicators().add( piA );
        piB = createProgramIndicator( 'B', programA, "70", null );
        piB.setAnalyticsType( ENROLLMENT );
        programA.getProgramIndicators().add( piB );

        TrackedEntityAttribute teaA = createTrackedEntityAttribute( 'A' );
        teaA.setUid( "TEAttribute" );
        attributeService.addTrackedEntityAttribute( teaA );
    }

    private String getSql( String expression )
    {
        piA.setExpression( expression );

        return programIndicatorService.getAnalyticsSql( expression, NUMERIC, piA, startDate, endDate );
    }

    private String getSqlEnrollment( String expression )
    {
        piB.setExpression( expression );

        return programIndicatorService.getAnalyticsSql( expression, NUMERIC, piB, startDate, endDate );
    }

    // -------------------------------------------------------------------------
    // Program variables tests (in alphabetical order)
    // -------------------------------------------------------------------------
    @Test
    void testAnalyticsPeriodEnd()
    {
        assertEquals( "'2020-01-31'", getSql( "V{analytics_period_end}" ) );
        assertEquals( "'2020-01-31'", getSqlEnrollment( "V{analytics_period_end}" ) );
    }

    @Test
    void testAnalyticsPeriodStart()
    {
        assertEquals( "'2020-01-01'", getSql( "V{analytics_period_start}" ) );
        assertEquals( "'2020-01-01'", getSqlEnrollment( "V{analytics_period_start}" ) );
    }

    @Test
    void testCreationDate()
    {
        assertEquals( "created", getSql( "V{creation_date}" ) );
        assertEquals(
            "(select created from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and created is not null and executiondate < cast( '2020-02-01' as date ) and executiondate >= cast( '2020-01-01' as date ) order by executiondate desc limit 1 )",
            getSqlEnrollment( "V{creation_date}" ) );
    }

    @Test
    void testCurrentDate()
    {
        String today = "'" + DateUtils.getLongDateString().substring( 0, 10 );
        assertThat( getSql( "V{current_date}" ), startsWith( today ) );
        assertThat( getSqlEnrollment( "V{current_date}" ), startsWith( today ) );
    }

    @Test
    void testDueDate()
    {
        assertEquals( "duedate", getSql( "V{due_date}" ) );
        assertEquals(
            "(select duedate from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and duedate is not null and executiondate < cast( '2020-02-01' as date ) and executiondate >= cast( '2020-01-01' as date ) order by executiondate desc limit 1 )",
            getSqlEnrollment( "V{due_date}" ) );
    }

    @Test
    void testEnrollmentCount()
    {
        assertEquals( "distinct pi", getSql( "V{enrollment_count}" ) );
        assertEquals( "pi", getSqlEnrollment( "V{enrollment_count}" ) );
    }

    @Test
    void testEnrollmentDate()
    {
        assertEquals( "enrollmentdate", getSql( "V{enrollment_date}" ) );
        assertEquals( "enrollmentdate", getSqlEnrollment( "V{enrollment_date}" ) );
    }

    @Test
    void testEnrollmentStatus()
    {
        assertEquals( "pistatus", getSql( "V{enrollment_status}" ) );
        assertEquals( "enrollmentstatus", getSqlEnrollment( "V{enrollment_status}" ) );
    }

    @Test
    void testEventStatus()
    {
        assertEquals( "psistatus", getSql( "V{event_status}" ) );
        assertEquals(
            "(select psistatus from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and psistatus is not null and executiondate < cast( '2020-02-01' as date ) and executiondate >= cast( '2020-01-01' as date ) order by executiondate desc limit 1 )",
            getSqlEnrollment( "V{event_status}" ) );
    }

    @Test
    void testEventStatusWithComparison()
    {
        assertEquals( "psistatus = 'SCHEDULE'", getSql( "V{event_status} == 'SCHEDULE'" ) );
    }

    @Test
    void testEventCount()
    {
        assertEquals( "case when psistatus in ('ACTIVE', 'COMPLETED') then 1 end", getSql( "V{event_count}" ) );
        assertEquals( "case when psistatus in ('ACTIVE', 'COMPLETED') then 1 end",
            getSqlEnrollment( "V{event_count}" ) );
    }

    @Test
    void testScheduledEventCount()
    {
        assertEquals( " case when psistatus = 'SCHEDULE' then 1 end ", getSql( "V{scheduled_event_count}" ) );
        assertEquals( " case when psistatus = 'SCHEDULE' then 1 end ",
            getSqlEnrollment( "V{scheduled_event_count}" ) );
    }

    @Test
    void testExecutionDate()
    {
        assertEquals( "executiondate", getSql( "V{execution_date}" ) );
        assertEquals(
            "(select executiondate from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and executiondate is not null and executiondate < cast( '2020-02-01' as date ) and executiondate >= cast( '2020-01-01' as date )  and psistatus IN ('COMPLETED', 'ACTIVE') order by executiondate desc limit 1 )",
            getSqlEnrollment( "V{execution_date}" ) );
    }

    @Test
    void testEventDate()
    {
        assertEquals( "executiondate", getSql( "V{event_date}" ) );
        assertEquals(
            "(select executiondate from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and executiondate is not null and executiondate < cast( '2020-02-01' as date ) and executiondate >= cast( '2020-01-01' as date )  and psistatus IN ('COMPLETED', 'ACTIVE') order by executiondate desc limit 1 )",
            getSqlEnrollment( "V{event_date}" ) );
    }

    @Test
    void testScheduledDate()
    {
        assertEquals( "duedate", getSql( "V{scheduled_date}" ) );
        assertEquals(
            "(select duedate from analytics_event_Program000A where analytics_event_Program000A.pi = ax.pi and duedate is not null and executiondate < cast( '2020-02-01' as date ) and executiondate >= cast( '2020-01-01' as date )  and psistatus = 'SCHEDULE' order by executiondate desc limit 1 )",
            getSqlEnrollment( "V{scheduled_date}" ) );
    }

    @Test
    void testIncidentDate()
    {
        assertEquals( "incidentdate", getSql( "V{incident_date}" ) );
        assertEquals( "incidentdate", getSqlEnrollment( "V{incident_date}" ) );
    }

    @Test
    void testOrgUnitCount()
    {
        assertEquals( "distinct ou", getSql( "V{org_unit_count}" ) );
        assertEquals( "distinct ou", getSqlEnrollment( "V{org_unit_count}" ) );
    }

    @Test
    void testProgramStageId()
    {
        assertEquals( "ps", getSql( "V{program_stage_id}" ) );
        assertEquals( "''", getSqlEnrollment( "V{program_stage_id}" ) );
    }

    @Test
    void testProgramStageName()
    {
        assertEquals( "(select name from programstage where uid = ps)", getSql( "V{program_stage_name}" ) );
        assertEquals( "''", getSqlEnrollment( "V{program_stage_name}" ) );
    }

    @Test
    void testSyncDate()
    {
        assertEquals( "lastupdated", getSql( "V{sync_date}" ) );
        assertEquals( "lastupdated", getSqlEnrollment( "V{sync_date}" ) );
    }

    @Test
    void testTeiCount()
    {
        assertEquals( "distinct tei", getSql( "V{tei_count}" ) );
        assertEquals( "distinct tei", getSqlEnrollment( "V{tei_count}" ) );
    }

    @Test
    @Disabled( "Actual result has 'double precision' instead of 'double'" )
    void testValueCount()
    {
        assertEquals( "0", getSql( "V{value_count}" ) );
        assertEquals( "0", getSqlEnrollment( "V{value_count}" ) );

        assertEquals(
            "coalesce(\"TEAttribute\",'') + nullif(cast((case when \"TEAttribute\" is not null then 1 else 0 end) as double),0)",
            getSql( "A{TEAttribute} + V{value_count}" ) );
        assertEquals(
            "coalesce(\"TEAttribute\",'') + nullif(cast((case when \"TEAttribute\" is not null then 1 else 0 end) as double),0)",
            getSqlEnrollment( "A{TEAttribute} + V{value_count}" ) );
    }

    @Test
    @Disabled( "Actual result has 'double precision' instead of 'double'" )
    void testZeroPosValueCount()
    {
        assertEquals( "0", getSql( "V{zero_pos_value_count}" ) );
        assertEquals( "0", getSqlEnrollment( "V{zero_pos_value_count}" ) );

        assertEquals(
            "coalesce(\"TEAttribute\",'') + nullif(cast((case when \"TEAttribute\" >= 0 then 1 else 0 end) as double),0)",
            getSql( "A{TEAttribute} + V{zero_pos_value_count}" ) );
        assertEquals(
            "coalesce(\"TEAttribute\",'') + nullif(cast((case when \"TEAttribute\" >= 0 then 1 else 0 end) as double),0)",
            getSqlEnrollment( "A{TEAttribute} + V{zero_pos_value_count}" ) );
    }
}
