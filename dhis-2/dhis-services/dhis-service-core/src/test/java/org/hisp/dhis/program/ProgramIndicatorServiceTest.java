package org.hisp.dhis.program;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import static org.hamcrest.CoreMatchers.is;
import static org.hisp.dhis.program.ProgramIndicator.*;
import static org.hisp.dhis.program.ProgramIndicatorVariable.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.api.util.DateUtils;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.jdbc.statementbuilder.PostgreSQLStatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Chau Thu Tran
 * @author Luciano Fiandesio
 */
public class ProgramIndicatorServiceTest extends DhisConvenienceTest
{
    private static final String COL_QUOTE = "\"";

    private ProgramIndicatorService subject;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private ProgramStageService programStageService;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private ConstantService constantService;

    @Mock
    private ProgramIndicatorStore programIndicatorStore;

    @Mock
    private TrackedEntityAttributeService trackedEntityAttributeService;

    private PostgreSQLStatementBuilder statementBuilder = new PostgreSQLStatementBuilder();

    @Mock
    private IdentifiableObjectStore<ProgramIndicatorGroup> identifiableObjectStore;

    @Mock
    private I18nManager i18nManager;

    @Mock
    private I18n i18n;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private ProgramStage psA;

    private ProgramStage psB;

    private Program programA;

    private Program programB;

    private DataElement deA;

    private DataElement deB;

    private TrackedEntityAttribute atA;

    private TrackedEntityAttribute atB;

    private ProgramIndicator indicatorA;

    private ProgramIndicator indicatorB;

    private ProgramIndicator indicatorC;

    private ProgramIndicator indicatorD;

    private ProgramIndicator indicatorE;

    private ProgramIndicator indicatorF;

    private ProgramIndicator indicatorG;

    private Constant constantA;

    @Before
    public void setUpTest()
    {

        subject = new DefaultProgramIndicatorService(programIndicatorStore,
                programStageService,
                dataElementService,
                trackedEntityAttributeService,
                constantService, statementBuilder, identifiableObjectStore, i18nManager);
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        // ---------------------------------------------------------------------
        // Program
        // ---------------------------------------------------------------------

        programA = createProgram( 'A', new HashSet<>(), organisationUnit );

        psA = new ProgramStage( "StageA", programA );
        psA.setUid( "EZq9VbPWgML");
        psA.setSortOrder( 1 );

        psB = new ProgramStage( "StageB", programA );
        psB.setUid( "EZq9VbPWgMX");
        psB.setSortOrder( 2 );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( psA );
        programStages.add( psB );
        programA.setProgramStages( programStages );

        programB = createProgram( 'B', new HashSet<>(), organisationUnit );

        // ---------------------------------------------------------------------
        // Program Stage DE
        // ---------------------------------------------------------------------

        deA = createDataElement( 'A' );
        deA.setDomainType( DataElementDomain.TRACKER );

        deB = createDataElement( 'B' );
        deB.setDomainType( DataElementDomain.TRACKER );

        // ---------------------------------------------------------------------
        // TrackedEntityAttribute
        // ---------------------------------------------------------------------

        atA = createTrackedEntityAttribute( 'A', ValueType.NUMBER );
        atB = createTrackedEntityAttribute( 'B', ValueType.NUMBER );

        // ---------------------------------------------------------------------
        // Constant
        // ---------------------------------------------------------------------

        constantA = createConstant( 'A', 7.0 );

        // ---------------------------------------------------------------------
        // ProgramIndicator
        // ---------------------------------------------------------------------

        String expressionA = "( d2:daysBetween(" + KEY_PROGRAM_VARIABLE + "{" + VAR_ENROLLMENT_DATE.getVariableName() + "}, " + KEY_PROGRAM_VARIABLE + "{"
                + VAR_INCIDENT_DATE.getVariableName() + "}) )  / " + ProgramIndicator.KEY_CONSTANT + "{" + constantA.getUid() + "}";
        indicatorA = createProgramIndicator( 'A', programA, expressionA, null );
        programA.getProgramIndicators().add( indicatorA );

        indicatorB = createProgramIndicator( 'B', programA, "70", null );
        programA.getProgramIndicators().add( indicatorB );

        indicatorC = createProgramIndicator( 'C', programA, "0", null );
        programA.getProgramIndicators().add( indicatorC );

        String expressionD = "0 + A + 4 + " + ProgramIndicator.KEY_PROGRAM_VARIABLE + "{" + VAR_INCIDENT_DATE.getVariableName() + "}";
        indicatorD = createProgramIndicator( 'D', programB, expressionD, null );

        String expressionE = KEY_DATAELEMENT + "{" + psA.getUid() + "." + deA.getUid() + "} + " + KEY_DATAELEMENT + "{"
            + psB.getUid() + "." + deA.getUid() + "} - " + KEY_ATTRIBUTE + "{" + atA.getUid() + "} + " + KEY_ATTRIBUTE
            + "{" + atB.getUid() + "}";
        String filterE = KEY_DATAELEMENT + "{" + psA.getUid() + "." + deA.getUid() + "} + " + KEY_ATTRIBUTE + "{" + atA.getUid() + "} > 10";
        indicatorE = createProgramIndicator( 'E', programB, expressionE, filterE );

        String expressionF = KEY_DATAELEMENT + "{" + psA.getUid() + "." + deA.getUid() + "}";
        String filterF = KEY_DATAELEMENT + "{" + psA.getUid() + "." + deA.getUid() + "} > " +
            KEY_ATTRIBUTE + "{" + atA.getUid() + "}";
        indicatorF = createProgramIndicator( 'F', AnalyticsType.ENROLLMENT, programB, expressionF, filterF );
        indicatorF.getAnalyticsPeriodBoundaries().add( new AnalyticsPeriodBoundary(AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD, PeriodType.getByNameIgnoreCase( "daily" ), 10) );

        String expressionG = KEY_DATAELEMENT + getVariableExpression( VAR_TEI_COUNT );
        String filterG = "d2:daysBetween(" + getVariableExpression(VAR_ENROLLMENT_DATE) + "," + getVariableExpression(VAR_EVENT_DATE) + ") > 90";
        indicatorG = createProgramIndicator( 'F', AnalyticsType.ENROLLMENT, programB, expressionG, filterG );
        indicatorG.getAnalyticsPeriodBoundaries().add( new AnalyticsPeriodBoundary(AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD, PeriodType.getByNameIgnoreCase( "monthly" ), -6) );
    }

    // -------------------------------------------------------------------------
    // CRUD tests
    // -------------------------------------------------------------------------

    @Test
    public void testAddProgramIndicator()
    {

        subject.addProgramIndicator( indicatorA );
        verify( programIndicatorStore ).save( indicatorA );

        subject.getProgramIndicator( 1 );
        verify( programIndicatorStore ).get( 1 );

    }

    @Test
    public void testDeleteProgramIndicator()
    {
        subject.deleteProgramIndicator( indicatorB );
        verify( programIndicatorStore ).delete( indicatorB );
    }

    @Test
    public void testUpdateProgramIndicator()
    {
        subject.updateProgramIndicator( indicatorB );
        verify( programIndicatorStore ).update( indicatorB );
    }

    @Test
    public void testGetProgramIndicatorByName()
    {
        subject.getProgramIndicator( "IndicatorA" );
        verify( programIndicatorStore ).getByName( "IndicatorA" );
    }

    @Test
    public void testGetAllProgramIndicators()
    {
        subject.getAllProgramIndicators();
        verify( programIndicatorStore ).getAll( );
    }

    // -------------------------------------------------------------------------
    // Logic tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetExpressionDescriptionReturnsNullOnNullExpression()
    {
        assertNull( subject.getExpressionDescription( null ) );
    }

    @Test
    public void testGetExpressionKeyDataElement()
    {

        when( programStageService.getProgramStage( "OXXcwl6aPCQ" ) ).thenReturn( createProgramStage( 'A', 1 ) );
        when( dataElementService.getDataElement( "GCyeKSqlpdk" ) ).thenReturn( createDataElement( 'B' ) );

        String expected = subject.getExpressionDescription( "#{OXXcwl6aPCQ.GCyeKSqlpdk}" );

        assertThat( expected, is( "ProgramStageA.DataElementB" ) );
    }

    @Test
    public void testGetExpressionKeyAttribute()
    {
        when( trackedEntityAttributeService.getTrackedEntityAttribute( "gAyeKSqlpdk" ) )
                .thenReturn( createTrackedEntityAttribute( 'A' ) );

        String expected = subject.getExpressionDescription( "A{gAyeKSqlpdk}" );

        assertThat( expected, is( "AttributeA" ) );
    }

    @Test
    public void testGetExpressionConstant()
    {
        when( constantService.getConstant( "Gfd3ppDfq8E" ) )
                .thenReturn( createConstant( 'A' , 10.0) );

        String expected = subject.getExpressionDescription( "C{Gfd3ppDfq8E}" );

        assertThat( expected, is( "ConstantA" ) );
    }

    @Test
    public void testGetExpressionVariable()
    {
        when(i18nManager.getI18n()).thenReturn(i18n);
        when(i18n.getString("event_date")).thenReturn("Event Date");

        String expected = subject.getExpressionDescription( "V{event_date}" );

        assertThat( expected, is( "Event Date" ) );
    }


    @Test
    public void testGetAnyValueExistsFilterEventAnalyticsSQl()
    {
        String expected = "\"GCyeKSqlpdk\" is not null or \"gAyeKSqlpdk\" is not null";
        String expression = "#{OXXcwl6aPCQ.GCyeKSqlpdk} - A{gAyeKSqlpdk}";

        assertEquals( expected, subject.getAnyValueExistsClauseAnalyticsSql( expression, AnalyticsType.EVENT ) );
    }

    @Test
    public void testGetAnyValueExistsFilterEnrollmentAnalyticsSQl()
    {
        String expected = "\"gAyeKSqlpdk\" is not null or \"OXXcwl6aPCQ_GCyeKSqlpdk\" is not null";
        String expression = "#{OXXcwl6aPCQ.GCyeKSqlpdk} - A{gAyeKSqlpdk}";

        assertEquals( expected, subject.getAnyValueExistsClauseAnalyticsSql( expression, AnalyticsType.ENROLLMENT ) );
    }

    @Test
    public void testGetAnalyticsSQl()
    {
        Date d1 = new Date();
        Date d2 = new Date();

        String expected = "coalesce(\"" + deA.getUid() + "\"::numeric,0) + coalesce(\"" + atA.getUid() + "\"::numeric,0) > 10";

        String expression = "#{F3nQiJpaaSW." + deA.getUid() +"} + A{" + atA.getUid() + "} > 10";

        assertEquals( expected, subject.getAnalyticsSQl( expression, indicatorE, d1, d2 ) );
    }

    @Test
    public void testGetAnalyticsSQlRespectMissingValues()
    {
        Date d1 = new Date();
        Date d2 = new Date();

        String expected = "\"" + deA.getUid() + "\" + \"" + atA.getUid() + "\" > 10";
        String expression = "#{F3nQiJpaaSW." + deA.getUid() + "} + A{" + atA.getUid() + "} > 10";

        assertEquals( expected, subject.getAnalyticsSQl( expression, indicatorE, false, d1, d2 ) );
    }

    @Test
    public void testGetAnalyticsWithVariables()
    {
        String expression = "d2:zing(#{OXXcwl6aPCQ.EZq9VbPWgML}) + " + "#{OXXcwl6aPCQ.GCyeKSqlpdk} + "
                + getVariableExpression( VAR_ZERO_POS_VALUE_COUNT );

        String expected = "coalesce(case when \"EZq9VbPWgML\" < 0 then 0 else \"EZq9VbPWgML\" end, 0) + "
                + "coalesce(\"GCyeKSqlpdk\"::numeric,0) + "
                + "nullif(cast((case when \"EZq9VbPWgML\" >= 0 then 1 else 0 end + case when \"GCyeKSqlpdk\" >= 0 then 1 else 0 end) as double precision),0)";

        assertEquals( expected, getAnalyticsSQl( expression ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsZingA()
    {
        String col = COL_QUOTE + deA.getUid() + COL_QUOTE;
        String expressionElement = "#{" + psA.getUid() + "." + deA.getUid() + "}";

        String expected = "coalesce(case when " + col + " < 0 then 0 else " + col + " end, 0)";
        String expression = "d2:zing(" + expressionElement + ")";

        assertEquals( expected, getAnalyticsSQl( expression ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsZingB()
    {
        String expected =
            "coalesce(case when \"EZq9VbPWgML\" < 0 then 0 else \"EZq9VbPWgML\" end, 0) + " +
                "coalesce(case when \"GCyeKSqlpdk\" < 0 then 0 else \"GCyeKSqlpdk\" end, 0) + " +
                "coalesce(case when \"hsCmEqBcU23\" < 0 then 0 else \"hsCmEqBcU23\" end, 0)";

        String expression =
            "d2:zing(#{OXXcwl6aPCQ.EZq9VbPWgML}) + " +
                "d2:zing(#{OXXcwl6aPCQ.GCyeKSqlpdk}) + " +
                "d2:zing(#{OXXcwl6aPCQ.hsCmEqBcU23})";

        assertEquals( expected, getAnalyticsSQl( expression ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsOizp()
    {
        String col = COL_QUOTE + deA.getUid() + COL_QUOTE;
        String expressionElement = "#{" + psA.getUid() + "." + deA.getUid() + "}";

        String expected = "coalesce(case when " + col + " >= 0 then 1 else 0 end, 0)";
        String expression = "d2:oizp(" + expressionElement + ")";

        assertEquals( expected, getAnalyticsSQl( expression ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsZpvc()
    {
        String expected =
            "nullif(cast((" +
                "case when \"EZq9VbPWgML\" >= 0 then 1 else 0 end + " +
                "case when \"GCyeKSqlpdk\" >= 0 then 1 else 0 end" +
                ") as double precision),0)";

        String expression = "d2:zpvc(#{OXXcwl6aPCQ.EZq9VbPWgML},#{OXXcwl6aPCQ.GCyeKSqlpdk})";

        assertEquals( expected, getAnalyticsSQl( expression ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsDaysBetween()
    {
        String col1 = COL_QUOTE + deA.getUid() + COL_QUOTE;
        String col2 = COL_QUOTE + deB.getUid() + COL_QUOTE;
        String expressionElement1 = "#{" + psA.getUid() + "." + deA.getUid() + "}";
        String expressionElement2 = "#{" + psB.getUid() + "." + deB.getUid() + "}";


        String expected = "(cast(" + col2 + " as date) - cast(" + col1 + " as date))";
        String expression = "d2:daysBetween(" + expressionElement1 + "," + expressionElement2 + ")";


        assertEquals( expected, subject.getAnalyticsSQl( expression, createProgramIndicator( 'X', programA, expression, null ), false, new Date(), new Date() ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsCondition()
    {
        String col1 = COL_QUOTE + deA.getUid() + COL_QUOTE;
        String expressionElement = "#{" + psA.getUid() + "." + deA.getUid() + "}";

        String expected = "case when (" + col1 + " > 3) then 10 else 5 end";
        String expression = "d2:condition('" + expressionElement + " > 3',10,5)";

        assertEquals( expected, getAnalyticsSQl( expression ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsComposite()
    {
        String expected =
            "coalesce(case when \"EZq9VbPWgML\" < 0 then 0 else \"EZq9VbPWgML\" end, 0) + " +
                "(cast(\"kts5J79K9gA\" as date) - cast(\"GCyeKSqlpdk\" as date)) + " +
                "case when (\"GCyeKSqlpdk\" > 70) then 100 else 50 end + " +
                "case when (\"HihhUWBeg7I\" < 30) then 20 else 100 end";

        String expression =
            "d2:zing(#{OXXcwl6aPCQ.EZq9VbPWgML}) + " +
                "d2:daysBetween(#{OXXcwl6aPCQ.GCyeKSqlpdk},#{OXXcwl6aPCQ.kts5J79K9gA}) + " +
                "d2:condition(\"#{OXXcwl6aPCQ.GCyeKSqlpdk} > 70\",100,50) + " +
                "d2:condition('#{OXXcwl6aPCQ.HihhUWBeg7I} < 30',20,100)";

        assertEquals( expected, subject.getAnalyticsSQl( expression, createProgramIndicator( 'X', programA, expression, null ), false, new Date(), new Date() ) );
    }

    @Test( expected = IllegalStateException.class )
    public void testGetAnalyticsSqlWithFunctionsInvalid()
    {
        String col = COL_QUOTE + deA.getUid() + COL_QUOTE;
        String expressionElement = "#{" + psA.getUid() + "." + deA.getUid() + "}";

        String expected = "case when " + col + " >= 0 then 1 else " + col + " end";
        String expression = "d2:xyza(" + expressionElement + ")";

        assertEquals( expected, getAnalyticsSQl( expression ) );
    }

    @Test
    public void testGetAnalyticsSqlWithVariables()
    {
        String expected = "coalesce(\"EZq9VbPWgML\"::numeric,0) + (executiondate - enrollmentdate)";
        String expression = "#{OXXcwl6aPCQ.EZq9VbPWgML} + (" + getVariableExpression( VAR_EXECUTION_DATE ) + " - "
            + getVariableExpression( VAR_ENROLLMENT_DATE ) + ")";

        assertEquals( expected, getAnalyticsSQl( expression ) );
    }

    @Test
    public void testVariableEventCreationDateIsParsed()
    {
        String expression = "#{OXXcwl6aPCQ.EZq9VbPWgML} + (" + getVariableExpression( VAR_CREATION_DATE ) + ")";
        String expected = "coalesce(\"EZq9VbPWgML\"::numeric,0) + (created)";

        assertEquals( expected, getAnalyticsSQl( expression ) );
    }

    @Test
    public void testVariableEventSyncDateIsParsed()
    {
        String expression = "#{OXXcwl6aPCQ.EZq9VbPWgML} + (" + getVariableExpression(VAR_SYNC_DATE) + ")";
        String expected = "coalesce(\"EZq9VbPWgML\"::numeric,0) + (lastupdated)";

        assertEquals( expected, getAnalyticsSQl( expression ) );
    }

    @Test
    public void testIsEmptyFilter()
    {
        String expected = "coalesce(\"EZq9VbPWgML\",'') == '' ";
        String filter = "#{OXXcwl6aPCQ.EZq9VbPWgML} == ''";

        assertEquals( expected, getAnalyticsSQl( filter ) );
    }

    @Test
    public void testIsZeroFilter()
    {
        String expected = "coalesce(\"EZq9VbPWgML\"::numeric,0) == 0 ";
        String filter = "#{OXXcwl6aPCQ.EZq9VbPWgML} == 0";

        assertEquals( expected, subject.getAnalyticsSQl( filter, createProgramIndicator( 'X', AnalyticsType.EVENT, programA, null, filter ), new Date(), new Date() ) );
    }

    @Test
    public void testIsZeroOrEmptyFilter()
    {
        String expected = "coalesce(\"GCyeKSqlpdk\"::numeric,0) == 1 or " +
            "(coalesce(\"GCyeKSqlpdk\",'') == '' and " +
            "coalesce(\"kts5J79K9gA\"::numeric,0) == 0 )";

        String filter = "#{OXXcwl6aPCQ.GCyeKSqlpdk} == 1 or " +
            "(#{OXXcwl6aPCQ.GCyeKSqlpdk}  == ''   and A{kts5J79K9gA}== 0)";
        String actual = subject.getAnalyticsSQl( filter, createProgramIndicator( 'X', AnalyticsType.EVENT, programA, null, filter ), true, new Date(), new Date() );
        assertEquals( expected, actual );
    }

    @Test
    public void testEnrollmentIndicatorWithEventBoundaryExpression()
    {
        String expected = "coalesce((select \"" + deA.getUid() + "\" from analytics_event_" + programB.getUid() + " " +
            "where analytics_event_" + indicatorF.getProgram().getUid() +
            ".pi = ax.pi and \"" + deA.getUid() + "\" is not null " +
            "and executiondate < cast( '2018-03-11' as date ) and "+
            "ps = '" + psA.getUid() + "' order by executiondate desc limit 1 )::numeric,0)";

        Date reportingStartDate = new GregorianCalendar(2018, Calendar.FEBRUARY, 1).getTime();
        Date reportingEndDate = new GregorianCalendar(2018, Calendar.FEBRUARY, 28).getTime();

        String actual = subject.getAnalyticsSQl( indicatorF.getExpression(), indicatorF, true, reportingStartDate, reportingEndDate );

        assertEquals( expected, actual );
    }

    @Test
    public void testEnrollmentIndicatorWithEventBoundaryFilter()
    {
        String expected = "(select \"" + deA.getUid() + "\" from analytics_event_" + programB.getUid() + " " +
            "where analytics_event_" + indicatorF.getProgram().getUid() + ".pi " +
            "= ax.pi and \"" + deA.getUid() + "\" is not null and executiondate < cast( '2018-03-11' as date ) and " +
            "ps = '" + psA.getUid() + "' order by executiondate desc limit 1 ) > \"" + atA.getUid() + "\"";
        Date reportingStartDate = new GregorianCalendar(2018, Calendar.FEBRUARY, 1).getTime();
        Date reportingEndDate = new GregorianCalendar(2018, Calendar.FEBRUARY, 28).getTime();
        String actual = subject.getAnalyticsSQl( indicatorF.getFilter(), indicatorF, false, reportingStartDate, reportingEndDate );
        assertEquals( expected, actual );
    }

    @Test
    public void testDateFunctions()
    {
        String expected = "(date_part('year',age(cast( '2016-01-01' as date), cast(enrollmentdate as date)))) < 1 " +
            "and (date_part('year',age(cast( '2016-12-31' as date), cast(enrollmentdate as date)))) >= 1";

        String filter = "d2:yearsBetween(V{enrollment_date}, V{analytics_period_start}) < 1 " +
            "and d2:yearsBetween(V{enrollment_date}, V{analytics_period_end}) >= 1";

        String actual = subject.getAnalyticsSQl( filter, createProgramIndicator( 'X', programA, filter, null ), true, DateUtils.parseDate( "2016-01-01" ) , DateUtils.parseDate( "2016-12-31" ) );

        assertEquals( expected, actual );
    }

    @Test
    public void testDateFunctionsWithProgramStageDateArguments()
    {

        String expected = "(date_part('year',age(cast((select executiondate from analytics_event_" + programA.getUid() + " " +
            "where analytics_event_" + programA.getUid() + ".pi = ax.pi and executiondate is not null and ps = '" + psA.getUid() + "' " +
            "order by executiondate desc limit 1 ) as date), cast(enrollmentdate as date)))) < 1 and " +
            "(date_part('month',age(cast((select executiondate from analytics_event_" + programA.getUid() +" where " +
            "analytics_event_" + programA.getUid() + ".pi = ax.pi and executiondate is not null and ps = '" + psB.getUid() + "' order " +
            "by executiondate desc limit 1 ) as date), cast((select executiondate from analytics_event_" + programA.getUid() + " " +
            "where analytics_event_" + programA.getUid() + ".pi = ax.pi and executiondate is not null and ps = '" + psA.getUid() + "' order " +
            "by executiondate desc limit 1 ) as date)))) > 10";

        String filter = "d2:yearsBetween(V{enrollment_date}, PS_EVENTDATE:" + psA.getUid() + ") < 1 " +
            "and d2:monthsBetween(PS_EVENTDATE:" + psA.getUid() + ", PS_EVENTDATE:" + psB.getUid() + ") > 10";

        String actual = subject.getAnalyticsSQl( filter, createProgramIndicator( 'X', AnalyticsType.ENROLLMENT, programA, filter, null ), true, DateUtils.parseDate( "2016-01-01" ) , DateUtils.parseDate( "2016-12-31" ) );

        assertEquals( expected, actual );
    }
    
    @Test
    public void testDateFunctionsWithProgramStageDateArgumentsAndBoundaries()
    {
        String expected = "(date_part('year',age(cast((select executiondate from analytics_event_" + programA.getUid() + " where analytics_event_" +
            programA.getUid() + ".pi = ax.pi and executiondate is not null and executiondate < cast( '2017-01-01' as date ) and executiondate >= " +
            "cast( '2016-01-01' as date ) and ps = '" + psA.getUid() + "' order by executiondate desc limit 1 ) as date), cast(enrollmentdate as date)))) < 1";

        String filter = "d2:yearsBetween(V{enrollment_date}, PS_EVENTDATE:" + psA.getUid() + ") < 1";
        ProgramIndicator programIndicator = createProgramIndicator( 'X', AnalyticsType.ENROLLMENT, programA, filter, null );
        Set<AnalyticsPeriodBoundary> boundaries = new HashSet<AnalyticsPeriodBoundary>();
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE, AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD, null, 0 ) );
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE, AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD, null, 0 ) );
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.ENROLLMENT_DATE, AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD, null, 0 ) );
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.ENROLLMENT_DATE, AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD, null, 0 ) );
        programIndicator.setAnalyticsPeriodBoundaries( boundaries );

        String actual = subject.getAnalyticsSQl( filter, programIndicator, true, DateUtils.parseDate( "2016-01-01" ) , DateUtils.parseDate( "2016-12-31" ) );

        assertEquals( expected, actual );
    }

    @Test
    public void testExpressionIsValid()
    {
        when( constantService.getConstant( constantA.getUid() ) ).thenReturn( createConstant( 'A', 10.0 ) );

        assertEquals( ProgramIndicator.VALID, subject.expressionIsValid( indicatorB.getExpression() ) );
        assertEquals( ProgramIndicator.VALID, subject.expressionIsValid( indicatorA.getExpression() ) );
        assertEquals( ProgramIndicator.EXPRESSION_NOT_VALID, subject.expressionIsValid( indicatorD.getExpression() ) );
    }

    @Test
    public void testExpressionWithFunctionIsValid()
    {
        when( programStageService.getProgramStage( "EZq9VbPWgML" ) ).thenReturn( createProgramStage( 'A', 1 ) );
        when( dataElementService.getDataElement( "deabcdefghA" ) ).thenReturn( createDataElement( 'B' ) );

        String exprA = "#{" + psA.getUid() + "." + deA.getUid() + "}";
        String exprB = "d2:zing(#{" + psA.getUid() + "." + deA.getUid() + "})";
        String exprC = "d2:condition('#{" + psA.getUid() + "." + deA.getUid() + "} > 10',2,1)";

        assertEquals( ProgramIndicator.VALID, subject.expressionIsValid( exprA ) );
        assertEquals( ProgramIndicator.VALID, subject.expressionIsValid( exprB ) );
        assertEquals( ProgramIndicator.VALID, subject.expressionIsValid( exprC ) );
    }

    @Test
    public void testFilterIsValid()
    {
        when( programStageService.getProgramStage( "EZq9VbPWgML" ) ).thenReturn( createProgramStage( 'A', 1 ) );
        when( dataElementService.getDataElement( "deabcdefghA" ) ).thenReturn( createDataElement( 'B' ) );

        TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute( 'A' );
        trackedEntityAttribute.setValueType( ValueType.BOOLEAN );

        when( trackedEntityAttributeService.getTrackedEntityAttribute( anyString() ) )
                .thenReturn( trackedEntityAttribute );
        when( trackedEntityAttributeService.getTrackedEntityAttribute( "invaliduid" ) ).thenReturn( null );

        String filterA = KEY_DATAELEMENT + "{" + psA.getUid() + "." + deA.getUid() + "}  - " + KEY_ATTRIBUTE + "{" + atA.getUid() + "} > 10";
        String filterB = KEY_ATTRIBUTE + "{" + atA.getUid() + "} == " + KEY_DATAELEMENT + "{" + psA.getUid() + "." + deA.getUid() + "} - 5";
        String filterC = KEY_ATTRIBUTE + "{invaliduid} == 100";
        String filterD = KEY_ATTRIBUTE + "{" + atA.getUid() + "} + 200";

        assertEquals( ProgramIndicator.VALID, subject.filterIsValid( filterA ) );
        assertEquals( ProgramIndicator.VALID, subject.filterIsValid( filterB ) );
        assertEquals( ProgramIndicator.INVALID_IDENTIFIERS_IN_EXPRESSION, subject.filterIsValid( filterC ) );
        assertEquals( ProgramIndicator.FILTER_NOT_EVALUATING_TO_TRUE_OR_FALSE, subject.filterIsValid( filterD ) );
    }

    @Test
    public void testd2relationshipCountFilter()
    {
        String expected = "(select count(*) from relationship r join relationshipitem rifrom on rifrom.relationshipid = r.relationshipid join " +
            "trackedentityinstance tei on rifrom.trackedentityinstanceid = tei.trackedentityinstanceid and tei.uid = ax.tei)";

        String filter = "d2:relationshipCount()";
        String actual = subject.getAnalyticsSQl( filter, createProgramIndicator( 'X', programA, filter, null ), true, DateUtils.parseDate( "2016-01-01" ) , DateUtils.parseDate( "2016-12-31" ) );
        assertEquals( expected, actual );
    }

    @Test
    public void testd2relationshipCountForOneRelationshipTypeFilter()
    {
        String expected = "(select count(*) from relationship r join relationshiptype rt on r.relationshiptypeid = rt.relationshiptypeid and " +
            "rt.uid = 'Zx7OEwPBUwD' join relationshipitem rifrom on rifrom.relationshipid = r.relationshipid " +
            "join trackedentityinstance tei on rifrom.trackedentityinstanceid = tei.trackedentityinstanceid and tei.uid = ax.tei)";

        String filter = "d2:relationshipCount('Zx7OEwPBUwD')";
        String actual = subject.getAnalyticsSQl( filter, createProgramIndicator( 'X', programA, filter, null ), true, DateUtils.parseDate( "2016-01-01" ) , DateUtils.parseDate( "2016-12-31" ) );
        assertEquals( expected, actual );
    }

    @Test
    public void testEventDateEnrollment()
    {
        Date reportingStartDate = new GregorianCalendar(2018, Calendar.FEBRUARY, 1).getTime();
        Date reportingEndDate = new GregorianCalendar(2018, Calendar.FEBRUARY, 28).getTime();

        String expectedFilter = "(cast((select executiondate from analytics_event_"
            + indicatorG.getProgram().getUid() + " where analytics_event_"
            + indicatorG.getProgram().getUid() + ".pi = ax.pi and executiondate"
            + " is not null and executiondate < cast( '2017-09-01' as date ) "
            + "order by executiondate desc limit 1 ) as date) - cast(enrollmentdate as date)) > 90";

        
        String actualFilter = subject.getAnalyticsSQl( indicatorG.getFilter(), indicatorG, false, reportingStartDate, reportingEndDate );
        assertEquals( expectedFilter, actualFilter );
    }

    @Test
    public void testIsVariableValid()
    {
        for ( ProgramIndicatorVariable piv : ProgramIndicatorVariable.values() )
        {
            if ( !piv.equals( VAR_UNDEFINED ) )
            {
                assertEquals( "Invalid expression: " + getVariableExpression( piv ), ProgramIndicator.VALID,
                    subject.expressionIsValid( getVariableExpression( piv ) ) );
            }
        }
    }

    private String getAnalyticsSQl( String expression )
    {
        return subject.getAnalyticsSQl( expression, createProgramIndicator( 'X', programA, expression, null ),
            new Date(), new Date() );
    }

    private String getVariableExpression(ProgramIndicatorVariable varName )
    {

        return "V{" + varName.getVariableName() + "}";
    }
}