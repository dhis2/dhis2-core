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

package org.hisp.dhis.program.variable;

import org.hisp.dhis.api.util.DateUtils;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.jdbc.statementbuilder.PostgreSQLStatementBuilder;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorVariable;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.CoreMatchers.*;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramIndicator;
import static org.hisp.dhis.program.ProgramIndicatorVariable.*;
import static org.junit.Assert.*;

/**
 * @author Luciano Fiandesio
 */
public class ProgramIndicatorVariableToSqlStrategyTest
{

    private StatementBuilder statementBuilder;

    @Before
    public void setUp()
    {
        this.statementBuilder = new PostgreSQLStatementBuilder();
    }

    private final String SQL_CASE_NOT_NULL = "case when \"%s\" is not null then 1 else 0 end";

    private final String SQL_CASE_VALUE = "case when \"%s\" >= 0 then 1 else 0 end";

    private ProgramIndicatorVariableToSqlStrategy getStrategy( ProgramIndicatorVariable programIndicatorVariable )
    {
        return ProgramIndicatorVariableToSqlStrategy.getStrategy( programIndicatorVariable, statementBuilder );
    }

    @Test
    public void verifyCurrentDateStrategy()
    {
        String var = getStrategy( VAR_CURRENT_DATE ).resolve( null, AnalyticsType.EVENT, null, null, null );

        String date = DateUtils.getLongDateString();

        assertThat( var, startsWith( "'" + date.substring( 0, 13 ) ) );
        assertThat( var, endsWith( "'" ) );
    }

    @Test
    public void verifyCreationDateStrategyWithEvent()
    {
        ProgramIndicator programIndicator = createProgramIndicator( 'a', AnalyticsType.EVENT, null, null, null );

        String var = getStrategy( VAR_CREATION_DATE ).resolve( null, AnalyticsType.EVENT, programIndicator, null,
            null );

        assertThat( var, is( "created" ) );
    }

    @Test
    public void verifyCreationDateStrategyWithEnrollment()
    {
        Program program = createProgram( 'p' );
        program.setUid( "9999999" );
        ProgramIndicator programIndicator = createProgramIndicator( 'a', AnalyticsType.ENROLLMENT, program, null,
            null );
        String var = getStrategy( VAR_CREATION_DATE ).resolve( null, AnalyticsType.ENROLLMENT, programIndicator, null,
            null );
        assertThat( var, is(
            "(select created from analytics_event_9999999 where analytics_event_9999999.pi = ax.pi and created is not null order by executiondate desc limit 1 )" ) );
    }

    @Test
    public void verifyValueCountStrategy()
    {

        String expression = "#{chG8sINMf11.yD5mUKAm3aK} + #{chG8sINMf11.UaGD9u0kaur} - A{y1Bhi6xHtVk}";

        String var = getStrategy( VAR_VALUE_COUNT ).resolve( expression, AnalyticsType.EVENT, null, null, null );

        assertThat( var,
            is( "nullif(cast((" + sqlCase( SQL_CASE_NOT_NULL, "UaGD9u0kaur" ) + " + "
                + sqlCase( SQL_CASE_NOT_NULL, "yD5mUKAm3aK" ) + " + " + sqlCase( SQL_CASE_NOT_NULL, "y1Bhi6xHtVk" )
                + ") as double precision),0)" ) );
    }

    @Test
    public void verifyValueCountStrategyWithEnrollment()
    {
        String expression = "#{chG8sINMf11.yD5mUKAm3aK} + #{chG8sINMf11.UaGD9u0kaur} - A{y1Bhi6xHtVk}";

        String var = getStrategy( VAR_VALUE_COUNT ).resolve( expression, AnalyticsType.ENROLLMENT, null, null, null );

        assertThat( var,
            is( "nullif(cast((" + sqlCase( SQL_CASE_NOT_NULL, "chG8sINMf11_UaGD9u0kaur" ) + " + "
                + sqlCase( SQL_CASE_NOT_NULL, "chG8sINMf11_yD5mUKAm3aK" ) + " + "
                + sqlCase( SQL_CASE_NOT_NULL, "y1Bhi6xHtVk" ) + ") as double precision),0)" ) );
    }

    @Test
    public void verifyZeroPositionValueStrategy()
    {
        String expression = "#{chG8sINMf11.yD5mUKAm3aK} + #{chG8sINMf11.UaGD9u0kaur} - A{y1Bhi6xHtVk}";

        String var = getStrategy( VAR_ZERO_POS_VALUE_COUNT ).resolve( expression, AnalyticsType.EVENT, null, null,
            null );

        assertThat( var,
            is( "nullif(cast((" + sqlCase( SQL_CASE_VALUE, "UaGD9u0kaur" ) + " + "
                + sqlCase( SQL_CASE_VALUE, "yD5mUKAm3aK" ) + " + " + sqlCase( SQL_CASE_VALUE, "y1Bhi6xHtVk" )
                + ") as double precision),0)" ) );
    }

    @Test
    public void verifyEventCountStrategy()
    {

        String var = getStrategy( VAR_EVENT_COUNT ).resolve( null, AnalyticsType.EVENT, null, null, null );
        assertThat( var, is( "distinct psi" ) );
    }

    @Test
    public void verifyProgramNameStrategy()
    {
        String var = getStrategy( VAR_PROGRAM_STAGE_NAME ).resolve( null, AnalyticsType.EVENT, null, null, null );
        assertThat( var, is( "(select name from programstage where uid = ps)" ) );
    }

    @Test
    public void verifyProgramNameStrategyWithEnrollment()
    {
        String var = getStrategy( VAR_PROGRAM_STAGE_NAME ).resolve( null, AnalyticsType.ENROLLMENT, null, null, null );

        assertThat( var, is( "''" ) );
    }

    @Test
    public void verifyProgramIdStrategy()
    {
        String var = getStrategy( VAR_PROGRAM_STAGE_ID ).resolve( null, AnalyticsType.EVENT, null, null, null );

        assertThat( var, is( "ps" ) );
    }

    @Test
    public void verifyProgramIdStrategyWithEnrollment()
    {
        String var = getStrategy( VAR_PROGRAM_STAGE_ID ).resolve( null, AnalyticsType.ENROLLMENT, null, null, null );

        assertThat( var, is( "''" ) );
    }

    @Test
    public void verifyDateStrategy()
        throws ParseException
    {

        Date startDate = toDate( "10/03/2018" );
        Date endDate = toDate( "10/03/2019" );

        String var = getStrategy( VAR_ANALYTICS_PERIOD_START ).resolve( null, AnalyticsType.ENROLLMENT, null, startDate,
            endDate );

        assertThat( var, is( "'2018-03-10'" ) );

        var = getStrategy( VAR_ANALYTICS_PERIOD_END ).resolve( null, AnalyticsType.ENROLLMENT, null, startDate,
            endDate );

        assertThat( var, is( "'2019-03-10'" ) );
    }

    @Test
    public void verifyDefaultStrategy()
    {
        String var = getStrategy( ProgramIndicatorVariable.getFromVariableName("missing") ).resolve( null, AnalyticsType.EVENT, null, null, null );

        assertThat( var, is( nullValue() ) );
    }

    private String sqlCase( String template, String id )
    {
        return String.format( template, id );
    }

    private Date toDate( String date )
        throws ParseException
    {
        return new SimpleDateFormat( "dd/MM/yyyy" ).parse( date );
    }

}