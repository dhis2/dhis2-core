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
import static org.hamcrest.Matchers.startsWith;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.antlr.AntlrParserUtils.castString;
import static org.hisp.dhis.parser.expression.ParserUtils.DEFAULT_SAMPLE_PERIODS;
import static org.hisp.dhis.parser.expression.ParserUtils.ITEM_GET_SQL;
import static org.hisp.dhis.program.DefaultProgramIndicatorService.PROGRAM_INDICATOR_ITEMS;
import static org.junit.Assert.assertThrows;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.antlr.AntlrExprLiteral;
import org.hisp.dhis.antlr.Parser;
import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.antlr.literal.DefaultLiteral;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.jdbc.statementbuilder.PostgreSQLStatementBuilder;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.util.DateUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

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

    private Date startDate = getDate( 2018, 1, 1 );

    private Date endDate = getDate( 2018, 12, 31 );

    @Mock
    private ProgramIndicatorService programIndicatorService;

    @Mock
    private ProgramStageService programStageService;

    private StatementBuilder statementBuilder;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private TrackedEntityAttributeService attributeService;

    @Mock
    private RelationshipTypeService relationshipTypeService;

    private CommonExpressionVisitor subject;

    private ProgramIndicator eventIndicator;

    private ProgramIndicator enrollmentIndicator;

    @Before
    public void setUp()
    {
        statementBuilder = new PostgreSQLStatementBuilder();

        eventIndicator = new ProgramIndicator();
        eventIndicator.setAnalyticsType( AnalyticsType.EVENT );

        enrollmentIndicator = makeEnrollmentProgramIndicator();
    }

    @Test
    public void testAnalyticsPeriodEndVariable()
    {
        String sql = castString( test( "V{analytics_period_end}", new DefaultLiteral(), eventIndicator ) );
        assertThat( sql, is( "'2018-12-31'" ) );
    }

    @Test
    public void testAnalyticsPeriodStartVariable()
    {
        String sql = castString( test( "V{analytics_period_start}", new DefaultLiteral(), eventIndicator ) );
        assertThat( sql, is( "'2018-01-01'" ) );
    }

    @Test
    public void testCreationDateForEnrollment()
    {
        String sql = castString( test( "V{creation_date}", new DefaultLiteral(), enrollmentIndicator ) );
        assertThat( sql,
            is( "(select created from analytics_event_" + enrollmentIndicator.getProgram().getUid()
                + " where analytics_event_"
                + enrollmentIndicator.getProgram().getUid()
                + ".pi = ax.pi and created is not null order by executiondate desc limit 1 )" ) );
    }

    @Test
    public void testCreationDateForEvent()
    {
        String sql = castString( test( "V{creation_date}", new DefaultLiteral(), eventIndicator ) );
        assertThat( sql, is( "created" ) );
    }

    @Test
    public void testCompletedDateForEnrollment()
    {
        String sql = castString( test( "V{completed_date}", new DefaultLiteral(), enrollmentIndicator ) );
        assertThat( sql, is( "completeddate" ) );
    }

    @Test
    public void testCompletedDateForEvent()
    {
        String sql = castString( test( "V{completed_date}", new DefaultLiteral(), eventIndicator ) );
        assertThat( sql, is( "completeddate" ) );
    }

    @Test
    public void testCurrentDateForEvent()
    {
        String sql = castString( test( "V{current_date}", new DefaultLiteral(), eventIndicator ) );
        String date = DateUtils.getLongDateString();

        assertThat( sql, startsWith( "'" + date.substring( 0, 13 ) ) );
    }

    @Test
    public void testDueDate()
    {
        String sql = castString( test( "V{due_date}", new DefaultLiteral(), eventIndicator ) );
        assertThat( sql, is( "duedate" ) );
    }

    @Test
    public void testEnrollmentCount()
    {
        String sql = castString( test( "V{enrollment_count}", new DefaultLiteral(), eventIndicator ) );
        assertThat( sql, is( "distinct pi" ) );
    }

    @Test
    public void testEnrollmentDate()
    {
        String sql = castString( test( "V{enrollment_date}", new DefaultLiteral(), eventIndicator ) );
        assertThat( sql, is( "enrollmentdate" ) );
    }

    @Test
    public void testEnrollmentStatus()
    {
        String sql = castString( test( "V{enrollment_status}", new DefaultLiteral(), eventIndicator ) );
        assertThat( sql, is( "pistatus" ) );
    }

    @Test
    public void testEventCount()
    {
        String sql = castString( test( "V{event_count}", new DefaultLiteral(), eventIndicator ) );
        assertThat( sql, is( "psi" ) );
    }

    @Test
    public void testExecutionDate()
    {
        String sql = castString( test( "V{execution_date}", new DefaultLiteral(), eventIndicator ) );
        assertThat( sql, is( "executiondate" ) );
    }

    @Test
    public void testEventDate()
    {
        String sql = castString( test( "V{event_date}", new DefaultLiteral(), eventIndicator ) );
        assertThat( sql, is( "executiondate" ) );
    }

    @Test
    public void testIncidentDate()
    {
        String sql = castString( test( "V{incident_date}", new DefaultLiteral(), eventIndicator ) );
        assertThat( sql, is( "incidentdate" ) );
    }

    @Test
    public void testProgramStageId()
    {
        String sql = castString( test( "V{program_stage_id}", new DefaultLiteral(), eventIndicator ) );
        assertThat( sql, is( "ps" ) );
    }

    @Test
    public void testProgramStageIdForEnrollment()
    {
        String sql = castString( test( "V{program_stage_id}", new DefaultLiteral(), enrollmentIndicator ) );
        assertThat( sql, is( "''" ) );
    }

    @Test
    public void testProgramStageName()
    {
        String sql = castString( test( "V{program_stage_name}", new DefaultLiteral(), eventIndicator ) );
        assertThat( sql, is( "(select name from programstage where uid = ps)" ) );
    }

    @Test
    public void testProgramStageNameForEnrollment()
    {
        String sql = castString( test( "V{program_stage_name}", new DefaultLiteral(), enrollmentIndicator ) );
        assertThat( sql, is( "''" ) );
    }

    @Test
    public void testSyncDate()
    {
        String sql = castString( test( "V{sync_date}", new DefaultLiteral(), enrollmentIndicator ) );
        assertThat( sql, is( "lastupdated" ) );
    }

    @Test
    public void testOrgUnitCount()
    {
        String sql = castString( test( "V{org_unit_count}", new DefaultLiteral(), enrollmentIndicator ) );
        assertThat( sql, is( "distinct ou" ) );
    }

    @Test
    public void testTeiCount()
    {
        String sql = castString( test( "V{tei_count}", new DefaultLiteral(), eventIndicator ) );
        assertThat( sql, is( "distinct tei" ) );
    }

    @Test
    public void testValueCount()
    {
        String sql = castString( test( "V{value_count}", new DefaultLiteral(), eventIndicator ) );

        assertThat( sql,
            is( "nullif(cast((" + sqlCase( SQL_CASE_NOT_NULL, BASE_UID + "a" ) + " + "
                + sqlCase( SQL_CASE_NOT_NULL, BASE_UID + "b" ) + " + " + sqlCase( SQL_CASE_NOT_NULL, BASE_UID + "c" )
                + ") as double precision),0)" ) );
    }

    @Test
    public void testZeroPosValueCount()
    {
        String sql = castString( test( "V{zero_pos_value_count}", new DefaultLiteral(), eventIndicator ) );

        assertThat( sql,
            is( "nullif(cast((" + sqlCase( SQL_CASE_VALUE, BASE_UID + "a" ) + " + "
                + sqlCase( SQL_CASE_VALUE, BASE_UID + "b" ) + " + " + sqlCase( SQL_CASE_VALUE, BASE_UID + "c" )
                + ") as double precision),0)" ) );
    }

    @Test
    public void testInvalidVariable()
    {
        assertThrows( ParserException.class,
            () -> test( "V{undefined_variable}", new DefaultLiteral(), eventIndicator ) );
    }

    private Object test( String expression, AntlrExprLiteral exprLiteral, ProgramIndicator programIndicator )
    {
        Set<String> dataElementsAndAttributesIdentifiers = new LinkedHashSet<>();
        dataElementsAndAttributesIdentifiers.add( BASE_UID + "a" );
        dataElementsAndAttributesIdentifiers.add( BASE_UID + "b" );
        dataElementsAndAttributesIdentifiers.add( BASE_UID + "c" );

        subject = CommonExpressionVisitor.newBuilder()
            .withItemMap( PROGRAM_INDICATOR_ITEMS )
            .withItemMethod( ITEM_GET_SQL )
            .withDataType( NUMERIC )
            .withConstantMap( new HashMap<>() )
            .withProgramIndicatorService( programIndicatorService )
            .withProgramStageService( programStageService )
            .withDataElementService( dataElementService )
            .withAttributeService( attributeService )
            .withRelationshipTypeService( relationshipTypeService )
            .withStatementBuilder( statementBuilder )
            .withI18n( new I18n( null, null ) )
            .withSamplePeriods( DEFAULT_SAMPLE_PERIODS )
            .buildForProgramIndicatorExpressions();

        subject.setExpressionLiteral( exprLiteral );
        subject.setProgramIndicator( programIndicator );
        subject.setReportingStartDate( startDate );
        subject.setReportingEndDate( endDate );
        subject.setDataElementAndAttributeIdentifiers( dataElementsAndAttributesIdentifiers );

        return Parser.visit( expression, subject );
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