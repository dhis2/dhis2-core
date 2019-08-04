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

package org.hisp.dhis.analytics.event.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.DhisConvenienceTest.getDate;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hisp.dhis.analytics.QueryValidator;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.jdbc.statementbuilder.PostgreSQLStatementBuilder;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * @author Luciano Fiandesio
 */
public class EnrollmentAnalyticsManagerTest extends EventAnalyticsTest
{
    private JdbcEnrollmentAnalyticsManager subject;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private StatementBuilder statementBuilder;

    @Mock
    private ProgramIndicatorService programIndicatorService;

    @Mock
    private SystemSettingManager systemSettingManager;

    @Mock
    private QueryValidator queryValidator;

    @Mock
    private SqlRowSet rowSet;

    @Captor
    private ArgumentCaptor<String> sql;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private String DEFAULT_COLUMNS = "pi,tei,enrollmentdate,incidentdate,ST_AsGeoJSON(pigeometry),longitude,latitude,ouname,oucode";
    private final String TABLE_NAME = "analytics_enrollment";

    @Before
    public void setUp()
    {
        when( jdbcTemplate.queryForRowSet( anyString() ) ).thenReturn( this.rowSet );

        statementBuilder = new PostgreSQLStatementBuilder();

        subject = new JdbcEnrollmentAnalyticsManager( jdbcTemplate, statementBuilder, programIndicatorService );
    }

    @Test
    public void verifyWithProgramAndStartEndDate()
    {
        EventQueryParams params = new EventQueryParams.Builder( createRequestParams() )
            .withStartDate( getDate( 2017, 1, 1 ) )
            .withEndDate( getDate( 2017, 12, 31 ) )
            .build();

        subject.getEnrollments( params, new ListGrid(), 10000);

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String expected = "ax.\"monthly\",ax.\"ou\"  from " + getTable( programA.getUid() )
            + " as ax where enrollmentdate >= '2017-01-01' and enrollmentdate <= '2017-12-31' and (uidlevel0 = 'ouabcdefghA' ) limit 10001";

        assertSql( expected, sql.getValue() );

    }

    @Test
    public void verifyWithProgramStageAndNumericDataElement()
    {
        verifyWithProgramStageAndNumericDataElement( ValueType.NUMBER );
    }

    @Test
    public void verifyWithProgramStageAndTextDataElement()
    {
        verifyWithProgramStageAndNumericDataElement( ValueType.TEXT );
    }

    private  void verifyWithProgramStageAndNumericDataElement( ValueType valueType ) {

        EventQueryParams params = createRequestParams( this.programStage, valueType );

        subject.getEnrollments( params, new ListGrid(), 100);

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String subSelect = "(select \"fWIAEtYVEGk\" from analytics_event_" + programA.getUid()  + " where analytics_event_"
            + programA.getUid()  + ".pi = ax.pi and \"fWIAEtYVEGk\" is not null and ps = '"
            + programStage.getUid() + "' order by executiondate desc limit 1 )";

        String expected = "ax.\"monthly\",ax.\"ou\"," + subSelect + "  from " + getTable( programA.getUid() )
            + " as ax where ax.\"monthly\" in ('2000Q1') and (uidlevel0 = 'ouabcdefghA' ) "
            + "and ps = '" + programStage.getUid() + "' limit 101";

        assertSql( expected, sql.getValue()   );
    }

    @Test
    public void verifyWithProgramStageAndTextualDataElementAndFilter() {

        EventQueryParams params = createRequestParamsWithFilter( programStage, ValueType.TEXT );

        subject.getEnrollments( params, new ListGrid(), 10000 );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String subSelect = "(select \"fWIAEtYVEGk\" from analytics_event_" + programA.getUid()  + " where analytics_event_"
            + programA.getUid()  + ".pi = ax.pi and \"fWIAEtYVEGk\" is not null and ps = '"
            + programStage.getUid() + "' order by executiondate desc limit 1 )";

        String expected = "ax.\"monthly\",ax.\"ou\"," + subSelect + "  from " + getTable( programA.getUid() )
            + " as ax where ax.\"monthly\" in ('2000Q1') and (uidlevel0 = 'ouabcdefghA' ) "
            + "and ps = '" + programStage.getUid() + "' and lower(" + subSelect + ") > '10' limit 10001";

        assertSql( expected, sql.getValue() );
    }

    @Test
    public void verifyWithProgramStageAndNumericDataElementAndFilter2() {

        EventQueryParams params = createRequestParamsWithFilter( programStage, ValueType.NUMBER );

        subject.getEnrollments( params, new ListGrid(), 10000 );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String subSelect = "(select \"fWIAEtYVEGk\" from analytics_event_" + programA.getUid()  + " where analytics_event_"
            + programA.getUid()  + ".pi = ax.pi and \"fWIAEtYVEGk\" is not null and ps = '"
            + programStage.getUid() + "' order by executiondate desc limit 1 )";

        String expected = "ax.\"monthly\",ax.\"ou\"," + subSelect + "  from " + getTable( programA.getUid() )
            + " as ax where ax.\"monthly\" in ('2000Q1') and (uidlevel0 = 'ouabcdefghA' ) "
            + "and ps = '" + programStage.getUid() + "' and " + subSelect + " > '10' limit 10001";

        assertSql( expected, sql.getValue() );
    }


    @Override
    String getTableName()
    {
        return this.TABLE_NAME;
    }

    private void assertSql( String actual, String expected )
    {
        assertThat( "select " + DEFAULT_COLUMNS + "," + actual, is( expected ) );
    }
}
