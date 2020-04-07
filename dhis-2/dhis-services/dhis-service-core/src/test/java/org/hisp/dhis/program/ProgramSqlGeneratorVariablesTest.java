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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.jdbc.statementbuilder.PostgreSQLStatementBuilder;
import org.hisp.dhis.parser.expression.InternalParserException;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.util.DateUtils;
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
import static org.hamcrest.Matchers.startsWith;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Luciano Fiandesio
 */
public class ProgramSqlGeneratorVariablesTest
    extends DhisConvenienceTest
{
    private BeanRandomizer beanRandomizer = new BeanRandomizer();

    private final String SQL_CASE_NOT_NULL = "case when \"%s\" is not null then 1 else 0 end";

    private final String SQL_CASE_VALUE = "case when \"%s\" >= 0 then 1 else 0 end";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Date startDate = getDate( 2018, 1, 1 );

    private Date endDate = getDate( 2018, 12, 31 );

    @Mock
    private ProgramIndicatorService programIndicatorService;

    private StatementBuilder statementBuilder;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private TrackedEntityAttributeService trackedEntityAttributeService;

    private ProgramSqlGenerator subject;

    @Mock
    private ParserRuleContext parserRuleContext;

    @Before
    public void setUp()
    {
        statementBuilder = new PostgreSQLStatementBuilder();

        ProgramIndicator programIndicator = new ProgramIndicator();

        programIndicator.setAnalyticsType( AnalyticsType.EVENT );

        initSubject( programIndicator );
    }

    @Test
    public void testAnalyticsPeriodEndVariable()
    {
        String sql = subject.visitProgramVariable( mockContext( V_ANALYTICS_PERIOD_END ) );
        assertThat( sql, is( "'2018-12-31'" ) );
    }

    @Test
    public void testAnalyticsPeriodStartVariable()
    {
        String sql = subject.visitProgramVariable( mockContext( V_ANALYTICS_PERIOD_START ) );
        assertThat( sql, is( "'2018-01-01'" ) );
    }

    @Test
    public void testCreationDateForEnrollment()
    {
        ProgramIndicator pi = makeEnrollmentProgramIndicator();
        initSubject( pi );

        String sql = subject.visitProgramVariable( mockContext( V_CREATION_DATE ) );
        assertThat( sql,
            is( "(select created from analytics_event_" + pi.getProgram().getUid() + " where analytics_event_"
                + pi.getProgram().getUid()
                + ".pi = ax.pi and created is not null order by executiondate desc limit 1 )" ) );
    }

    @Test
    public void testCreationDateForEvent()
    {
        String sql = subject.visitProgramVariable( mockContext( V_CREATION_DATE ) );

        assertThat( sql, is( "created" ) );
    }

    @Test
    public void testCompletedDateForEnrollment()
    {
        ProgramIndicator pi = makeEnrollmentProgramIndicator();
        initSubject( pi );

        String sql = (String) subject.visit( mockContext( V_COMPLETED_DATE ) );
        assertThat( sql,
            is( "(select completeddate from analytics_event_" + pi.getProgram().getUid() + " where analytics_event_"
                    + pi.getProgram().getUid()
                    + ".pi = ax.pi and completeddate is not null order by executiondate desc limit 1 )" ) );
    }

    @Test
    public void testCompletedDateForEvent()
    {
        String sql = (String) subject.visit( mockContext( V_COMPLETED_DATE ) );

        assertThat( sql, is( "completeddate" ) );
    }

    @Test
    public void testCurrentDateForEvent()
    {
        String sql = subject.visitProgramVariable( mockContext( V_CURRENT_DATE ) );
        String date = DateUtils.getLongDateString();

        // Only test the first part of the Date (up to the hour)
        assertThat( sql, startsWith( "'" + date.substring( 0, 13 ) ) );
    }

    @Test
    public void testDueDate()
    {
        String sql = subject.visitProgramVariable( mockContext( V_DUE_DATE ) );

        assertThat( sql, is( "duedate" ) );
    }

    @Test
    public void testEnrollmentCount()
    {
        String sql = subject.visitProgramVariable( mockContext( V_ENROLLMENT_COUNT ) );

        assertThat( sql, is( "distinct pi" ) );
    }

    @Test
    public void testEnrollmentDate()
    {
        String sql = subject.visitProgramVariable( mockContext( V_ENROLLMENT_DATE ) );

        assertThat( sql, is( "enrollmentdate" ) );
    }

    @Test
    public void testEnrollmentStatus()
    {
        String sql = subject.visitProgramVariable( mockContext( V_ENROLLMENT_STATUS ) );

        assertThat( sql, is( "enrollmentstatus" ) );
    }

    @Test
    public void testEventCount()
    {
        String sql = subject.visitProgramVariable( mockContext( V_EVENT_COUNT ) );

        assertThat( sql, is( "distinct psi" ) );
    }

    @Test
    public void testExecutionDate()
    {
        String sql = subject.visitProgramVariable( mockContext( V_EXECUTION_DATE ) );

        assertThat( sql, is( "executiondate" ) );
    }

    @Test
    public void testEventDate()
    {
        String sql = subject.visitProgramVariable( mockContext( V_EVENT_DATE ) );

        assertThat( sql, is( "executiondate" ) );
    }

    @Test
    public void testIncidentDate()
    {
        String sql = subject.visitProgramVariable( mockContext( V_INCIDENT_DATE ) );

        assertThat( sql, is( "incidentdate" ) );
    }

    @Test
    public void testProgramStageId()
    {
        String sql = subject.visitProgramVariable( mockContext( V_PROGRAM_STAGE_ID ) );

        assertThat( sql, is( "ps" ) );
    }

    @Test
    public void testProgramStageIdForEnrollment()
    {
        initSubject( makeEnrollmentProgramIndicator() );

        String sql = subject.visitProgramVariable( mockContext( V_PROGRAM_STAGE_ID ) );

        assertThat( sql, is( "''" ) );
    }

    @Test
    public void testProgramStageName()
    {
        String sql = subject.visitProgramVariable( mockContext( V_PROGRAM_STAGE_NAME ) );

        assertThat( sql, is( "(select name from programstage where uid = ps)" ) );
    }

    @Test
    public void testProgramStageNameForEnrollment()
    {
        initSubject( makeEnrollmentProgramIndicator() );

        String sql = subject.visitProgramVariable( mockContext( V_PROGRAM_STAGE_NAME ) );

        assertThat( sql, is( "''" ) );
    }

    @Test
    public void testSyncDate()
    {
        initSubject( makeEnrollmentProgramIndicator() );

        String sql = subject.visitProgramVariable( mockContext( V_SYNC_DATE ) );

        assertThat( sql, is( "lastupdated" ) );
    }

    @Test
    public void testOrgUnitCount()
    {
        initSubject( makeEnrollmentProgramIndicator() );

        String sql = subject.visitProgramVariable( mockContext( V_ORG_UNIT_COUNT ) );

        assertThat( sql, is( "distinct ou" ) );
    }

    @Test
    public void testTeiCount()
    {
        initSubject( makeEnrollmentProgramIndicator() );

        String sql = subject.visitProgramVariable( mockContext( V_TEI_COUNT ) );

        assertThat( sql, is( "distinct tei" ) );
    }

    @Test
    public void testValueCount()
    {
        String sql = subject.visitProgramVariable( mockContext( V_VALUE_COUNT ) );

        assertThat( sql,
            is( "nullif(cast((" + sqlCase( SQL_CASE_NOT_NULL, BASE_UID + "a" ) + " + "
                + sqlCase( SQL_CASE_NOT_NULL, BASE_UID + "b" ) + " + " + sqlCase( SQL_CASE_NOT_NULL, BASE_UID + "c" )
                + ") as double precision),0)" ) );
    }

    @Test
    public void testZeroPosValueCount()
    {
        String sql = subject.visitProgramVariable( mockContext( V_ZERO_POS_VALUE_COUNT ) );

        assertThat( sql,
            is( "nullif(cast((" + sqlCase( SQL_CASE_VALUE, BASE_UID + "a" ) + " + "
                + sqlCase( SQL_CASE_VALUE, BASE_UID + "b" ) + " + " + sqlCase( SQL_CASE_VALUE, BASE_UID + "c" )
                + ") as double precision),0)" ) );
    }

    @Test
    public void testInvalidVariable()
    {
        thrown.expect( InternalParserException.class );
        subject.visitProgramVariable( mockContext( 129839128 ) );
    }

    private ExpressionParser.ProgramVariableContext mockContext( int programIndicatorVariable )
    {
        ExpressionParser.ProgramVariableContext programVariableContext = new ExpressionParser.ProgramVariableContext(
            parserRuleContext, 1 );

        Token token = mock( Token.class );

        when( token.getType() ).thenReturn( programIndicatorVariable );
        programVariableContext.var = token;

        return programVariableContext;
    }

    private void initSubject( ProgramIndicator programIndicator )
    {
        Set<String> dataElementsAndAttributesIdentifiers = new LinkedHashSet<>();
        dataElementsAndAttributesIdentifiers.add( BASE_UID + "a" );
        dataElementsAndAttributesIdentifiers.add( BASE_UID + "b" );
        dataElementsAndAttributesIdentifiers.add( BASE_UID + "c" );

        this.subject = new ProgramSqlGenerator( programIndicator, startDate, endDate,
            dataElementsAndAttributesIdentifiers, new HashMap<>(), programIndicatorService, statementBuilder,
            dataElementService, trackedEntityAttributeService );
    }

    private ProgramIndicator makeEnrollmentProgramIndicator()
    {
        Program program = beanRandomizer.randomObject( Program.class );
        ProgramIndicator programIndicator = createProgramIndicator( 'A', AnalyticsType.ENROLLMENT, program, "", "" );

        programIndicator.setProgram( program );

        return programIndicator;
    }

    private String sqlCase( String template, String id )
    {
        return String.format( template, id );
    }

}