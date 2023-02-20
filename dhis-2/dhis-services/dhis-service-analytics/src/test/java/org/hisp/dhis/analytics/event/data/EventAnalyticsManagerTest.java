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
package org.hisp.dhis.analytics.event.data;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnitGroup;
import static org.hisp.dhis.DhisConvenienceTest.createPeriod;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramIndicator;
import static org.hisp.dhis.analytics.QueryKey.NV;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.OPTION_SEP;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.hisp.dhis.common.QueryOperator.EQ;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.hisp.dhis.common.QueryOperator.NEQ;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.programindicator.DefaultProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.jdbc.statementbuilder.PostgreSQLStatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class EventAnalyticsManagerTest extends EventAnalyticsTest
{
    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ExecutionPlanStore executionPlanStore;

    private JdbcEventAnalyticsManager subject;

    @Captor
    private ArgumentCaptor<String> sql;

    private final static String TABLE_NAME = "analytics_event";

    private final static String DEFAULT_COLUMNS_WITH_REGISTRATION = "psi,ps,executiondate,storedby,"
        + "createdbydisplayname" + "," + "lastupdatedbydisplayname"
        + ",lastupdated,duedate,enrollmentdate,incidentdate,tei,pi,ST_AsGeoJSON(coalesce(ax.\"psigeometry\",ax.\"pigeometry\",ax.\"teigeometry\",ax.\"ougeometry\"), 6) as geometry,longitude,latitude,ouname,"
        + "oucode,pistatus,psistatus";

    @BeforeEach
    public void setUp()
    {
        StatementBuilder statementBuilder = new PostgreSQLStatementBuilder();
        EventTimeFieldSqlRenderer timeCoordinateSelector = new EventTimeFieldSqlRenderer( statementBuilder );
        ProgramIndicatorService programIndicatorService = mock( ProgramIndicatorService.class );
        DefaultProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder = new DefaultProgramIndicatorSubqueryBuilder(
            programIndicatorService );

        subject = new JdbcEventAnalyticsManager( jdbcTemplate, programIndicatorService,
            programIndicatorSubqueryBuilder, timeCoordinateSelector, executionPlanStore );

        when( jdbcTemplate.queryForRowSet( anyString() ) ).thenReturn( this.rowSet );
    }

    @Test
    void verifyGetEventSqlWithProgramWithNoRegistration()
    {
        mockEmptyRowSet();

        this.programA.setProgramType( ProgramType.WITHOUT_REGISTRATION );

        subject.getEvents( createRequestParams(), createGrid(), 100 );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String expected = "select psi,ps,executiondate,storedby,"
            + "createdbydisplayname" + "," + "lastupdatedbydisplayname"
            + ",lastupdated,duedate,ST_AsGeoJSON(coalesce(ax.\"psigeometry\",ax.\"pigeometry\",ax.\"teigeometry\",ax.\"ougeometry\"), 6) as geometry,"
            + "longitude,latitude,ouname,oucode,pistatus,psistatus,ax.\"monthly\",ax.\"ou\"  from "
            + getTable( programA.getUid() )
            + " as ax where ax.\"monthly\" in ('2000Q1') and ax.\"uidlevel1\" in ('ouabcdefghA') limit 101";

        assertThat( sql.getValue(), is( expected ) );
    }

    @Test
    void verifyGetEventSqlWithOrgUnitTypeDataElement()
    {
        mockEmptyRowSet();

        DataElement dataElement = createDataElement( 'a' );
        QueryItem queryItem = new QueryItem( dataElement, this.programA, null,
            ValueType.ORGANISATION_UNIT, AggregationType.SUM, null );

        subject.getEvents( createRequestParams( queryItem ), createGrid(), 100 );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String expected = "select psi,ps,executiondate,storedby,"
            + "createdbydisplayname" + "," + "lastupdatedbydisplayname"
            + ",lastupdated,duedate,enrollmentdate,"
            + "incidentdate,tei,pi,ST_AsGeoJSON(coalesce(ax.\"psigeometry\",ax.\"pigeometry\",ax.\"teigeometry\",ax.\"ougeometry\"), 6) as geometry,longitude,latitude,ouname,oucode,pistatus,"
            + "psistatus,ax.\"monthly\",ax.\"ou\",\"" + dataElement.getUid() + "_name"
            + "\"  from " + getTable( programA.getUid() )
            + " as ax where ax.\"monthly\" in ('2000Q1') and ax.\"uidlevel1\" in ('ouabcdefghA')"
            + " limit 101";

        assertThat( sql.getValue(), is( expected ) );
    }

    @Test
    void verifyGetEventSqlWithProgram()
    {
        Grid grid = createGrid();
        int unlimited = 0;

        mockEmptyRowSet();

        subject.getEvents( createRequestParams(), grid, unlimited );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String expected = "ax.\"monthly\",ax.\"ou\"  from " + getTable( programA.getUid() )
            + " as ax where ax.\"monthly\" in ('2000Q1') and ax.\"uidlevel1\" in ('ouabcdefghA') ";

        assertSql( expected, sql.getValue() );
        assertTrue( grid.hasLastDataRow() );
    }

    @Test
    void verifyGetEventsSqlWithProgramAndProgramStage()
    {
        mockEmptyRowSet();

        subject.getEvents( createRequestParams( programStage ), createGrid(),
            100 );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String expected = "ax.\"monthly\",ax.\"ou\"  from " + getTable( programA.getUid() )
            + " as ax where ax.\"monthly\" in ('2000Q1') and ax.\"uidlevel1\" in ('ouabcdefghA') and ax.\"ps\" = '"
            + programStage.getUid() + "' limit 101";

        assertSql( expected, sql.getValue() );
    }

    @Test
    void verifyGetEventsWithProgramStageAndNumericDataElement()
    {
        mockEmptyRowSet();

        subject.getEvents( createRequestParams( programStage, ValueType.INTEGER ), createGrid(),
            100 );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String expected = "ax.\"monthly\",ax.\"ou\",ax.\"fWIAEtYVEGk\"  from " + getTable( programA.getUid() )
            + " as ax where ax.\"monthly\" in ('2000Q1') and ax.\"uidlevel1\" in ('ouabcdefghA') and ax.\"ps\" = '"
            + programStage.getUid() + "' limit 101";

        assertSql( expected, sql.getValue() );
    }

    @Test
    void verifyGetEventsWithProgramStageAndNumericDataElementAndFilter()
    {
        mockEmptyRowSet();

        subject.getEvents( createRequestParamsWithFilter( programStage, ValueType.INTEGER ), createGrid(),
            100 );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String expected = "ax.\"monthly\",ax.\"ou\",ax.\"fWIAEtYVEGk\"  from " + getTable( programA.getUid() )
            + " as ax where ax.\"monthly\" in ('2000Q1') and ax.\"uidlevel1\" in ('ouabcdefghA') and ax.\"ps\" = '"
            + programStage.getUid()
            + "' and ax.\"fWIAEtYVEGk\" > '10' limit 101";

        assertSql( expected, sql.getValue() );
    }

    @Test
    void verifyGetEventsWithProgramStatusAndEventStatusParams()
    {
        mockEmptyRowSet();

        subject.getEvents( createRequestParamsWithStatuses(), createGrid(), 100 );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String expected = "ax.\"monthly\",ax.\"ou\"  from " + getTable( programA.getUid() )
            + " as ax where ax.\"monthly\" in ('2000Q1') and ax.\"uidlevel1\" in ('ouabcdefghA')" +
            " and pistatus in ('ACTIVE','COMPLETED') and psistatus in ('SCHEDULE') limit 101";

        assertSql( expected, sql.getValue() );
    }

    @Test
    void verifyGetEventsWithScheduledDateTimeFieldParam()
    {
        mockEmptyRowSet();

        subject.getEvents( createRequestParamsWithTimeField( "SCHEDULED_DATE" ), createGrid(), 100 );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String expected = "ps.\"monthly\",ax.\"ou\"  from " + getTable( programA.getUid() )
            + " as ax left join _dateperiodstructure as ps on cast(ax.\"duedate\" as date) = ps.\"dateperiod\" "
            + "where ps.\"monthly\" in ('2000Q1') and ax.\"uidlevel1\" "
            + "in ('ouabcdefghA') and pistatus in ('ACTIVE','COMPLETED') limit 101";

        assertSql( expected, sql.getValue() );
    }

    @Test
    void verifyGetEventsWithLastUpdatedTimeFieldParam()
    {
        mockEmptyRowSet();

        subject.getEvents( createRequestParamsWithTimeField( "LAST_UPDATED" ), createGrid(), 100 );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String expected = "ax.\"monthly\",ax.\"ou\"  from " + getTable( programA.getUid() ) + " as ax "
            + "where (( ax.\"lastupdated\" >= '2000-01-01' and ax.\"lastupdated\" < '2000-04-01') )and ax.\"uidlevel1\" "
            + "in ('ouabcdefghA') and pistatus in ('ACTIVE','COMPLETED') limit 101";

        assertSql( expected, sql.getValue() );
    }

    @Test
    void verifyGetEventsWithMissingValueEqFilter()
    {
        String expected = "ax.\"fWIAEtYVEGk\" is null";
        testIt( EQ, NV, Collections.singleton(
            ( capturedSql ) -> assertThat( capturedSql, containsString( expected ) ) ) );
    }

    @Test
    void verifyGetEventsWithMissingValueNeqFilter()
    {
        String expected = "ax.\"fWIAEtYVEGk\" is not null";
        testIt( NEQ, NV, Collections.singleton(
            ( capturedSql ) -> assertThat( capturedSql, containsString( expected ) ) ) );
    }

    @Test
    void verifyGetEventsWithMissingValueAndNumericValuesInFilter()
    {
        String numericValues = String.join( OPTION_SEP, "10", "11", "12" );
        String expected = "(ax.\"fWIAEtYVEGk\" in (" + String.join( ",", numericValues.split( OPTION_SEP ) )
            + ") or ax.\"fWIAEtYVEGk\" is null )";
        testIt( IN, numericValues + OPTION_SEP + NV,
            Collections.singleton( ( capturedSql ) -> assertThat( capturedSql, containsString( expected ) ) ) );
    }

    @Test
    void verifyGetEventsWithoutMissingValueAndNumericValuesInFilter()
    {
        String numericValues = String.join( OPTION_SEP, "10", "11", "12" );
        String expected = "ax.\"fWIAEtYVEGk\" in (" + String.join( ",", numericValues.split( OPTION_SEP ) ) + ")";
        testIt( IN, numericValues,
            Collections.singleton( ( capturedSql ) -> assertThat( capturedSql, containsString( expected ) ) ) );
    }

    @Test
    void verifyGetEventsWithOnlyMissingValueInFilter()
    {
        String expected = "ax.\"fWIAEtYVEGk\" is null";
        String unexpected = "(ax.\"fWIAEtYVEGk\" in (";
        testIt( IN, NV, List.of(
            ( capturedSql ) -> assertThat( capturedSql, containsString( expected ) ),
            ( capturedSql ) -> assertThat( capturedSql, not( containsString( unexpected ) ) ) ) );
    }

    private void testIt( QueryOperator operator, String filter, Collection<Consumer<String>> assertions )
    {
        mockEmptyRowSet();

        subject.getEvents(
            createRequestParamsWithFilter( programStage, ValueType.INTEGER, operator, filter ),
            createGrid(),
            100 );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        assertions.forEach( consumer -> consumer.accept( sql.getValue() ) );
    }

    @Test
    void verifyGetEventsWithProgramStageAndTextDataElement()
    {
        mockEmptyRowSet();

        subject.getEvents( createRequestParams( programStage, ValueType.TEXT ), createGrid(),
            100 );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String expected = "ax.\"monthly\",ax.\"ou\",ax.\"fWIAEtYVEGk\"  from " + getTable( programA.getUid() )
            + " as ax where ax.\"monthly\" in ('2000Q1') and ax.\"uidlevel1\" in ('ouabcdefghA') and ax.\"ps\" = '"
            + programStage.getUid() + "' limit 101";

        assertSql( expected, sql.getValue() );
    }

    @Test
    void verifyGetEventsWithProgramStageAndTextDataElementAndFilter()
    {
        mockEmptyRowSet();

        subject.getEvents( createRequestParamsWithFilter( programStage, ValueType.TEXT ), createGrid(), 100 );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String expected = "ax.\"monthly\",ax.\"ou\",ax.\"fWIAEtYVEGk\"  from " + getTable( programA.getUid() )
            + " as ax where ax.\"monthly\" in ('2000Q1') and ax.\"uidlevel1\" in ('ouabcdefghA') and ax.\"ps\" = '"
            + programStage.getUid()
            + "' and ax.\"fWIAEtYVEGk\" > '10' limit 101";

        assertSql( expected, sql.getValue() );
    }

    @Test
    void verifyGetAggregatedEventQuery()
    {
        mockRowSet();

        when( rowSet.getString( "fWIAEtYVEGk" ) ).thenReturn( "2000" );

        Grid resultGrid = subject.getAggregatedEventData( createRequestParams( programStage, ValueType.INTEGER ),
            createGrid(), 200000 );

        assertThat( resultGrid.getRows(), hasSize( 1 ) );
        assertThat( resultGrid.getRow( 0 ), hasSize( 4 ) );
        assertThat( resultGrid.getRow( 0 ).get( 0 ), is( "2000" ) );
        assertThat( resultGrid.getRow( 0 ).get( 1 ), is( "201701" ) );
        assertThat( resultGrid.getRow( 0 ).get( 2 ), is( "Sierra Leone" ) );
        assertThat( resultGrid.getRow( 0 ).get( 3 ), is( 100 ) );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String expected = "select count(ax.\"psi\") as value,ax.\"monthly\",ax.\"ou\",ax.\"fWIAEtYVEGk\" from "
            + getTable( programA.getUid() )
            + " as ax where ax.\"monthly\" in ('2000Q1') and ax.\"uidlevel1\" in ('ouabcdefghA') and ax.\"ps\" = '"
            + programStage.getUid()
            + "' group by ax.\"monthly\", ax.\"ou\", ax.\"fWIAEtYVEGk\" limit 200001";

        assertThat( sql.getValue(), is( expected ) );
    }

    @Test
    void verifyGetAggregatedEventQueryWithFilter()
    {

        when( rowSet.getString( "fWIAEtYVEGk" ) ).thenReturn( "2000" );

        mockRowSet();

        Grid resultGrid = subject.getAggregatedEventData( createRequestParamsWithFilter( programStage, ValueType.TEXT ),
            createGrid(), 200000 );

        assertThat( resultGrid.getRows(), hasSize( 1 ) );
        assertThat( resultGrid.getRow( 0 ), hasSize( 4 ) );
        assertThat( resultGrid.getRow( 0 ).get( 0 ), is( "2000" ) );
        assertThat( resultGrid.getRow( 0 ).get( 1 ), is( "201701" ) );
        assertThat( resultGrid.getRow( 0 ).get( 2 ), is( "Sierra Leone" ) );
        assertThat( resultGrid.getRow( 0 ).get( 3 ), is( 100 ) );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );
        String expected = "select count(ax.\"psi\") as value,ax.\"monthly\",ax.\"ou\",ax.\"fWIAEtYVEGk\" from "
            + getTable( programA.getUid() )
            + " as ax where ax.\"monthly\" in ('2000Q1') and ax.\"uidlevel1\" in ('ouabcdefghA') and ax.\"ps\" = '"
            + programStage.getUid()
            + "' and ax.\"fWIAEtYVEGk\" > '10'"
            + " group by ax.\"monthly\", ax.\"ou\", ax.\"fWIAEtYVEGk\" limit 200001";
        assertThat( sql.getValue(), is( expected ) );
    }

    @Test
    void verifyFirstAggregationTypeSubquery()
    {
        verifyFirstOrLastAggregationTypeSubquery( AnalyticsAggregationType.FIRST );
    }

    @Test
    void verifyLastAggregationTypeSubquery()
    {
        verifyFirstOrLastAggregationTypeSubquery( AnalyticsAggregationType.LAST );
    }

    @Test
    void verifyLastLastOrgUnitAggregationTypeSubquery()
    {
        DataElement deX = createDataElement( 'X' );

        EventQueryParams params = new EventQueryParams.Builder()
            .withOrganisationUnits( List.of( createOrganisationUnit( 'A' ) ) )
            .withPeriods( List.of( createPeriod( "202201" ) ), PeriodTypeEnum.MONTHLY.getName() )
            .addDimension( new BaseDimensionalObject( "jkYhtGth12t", DimensionType.ORGANISATION_UNIT_GROUP_SET,
                "Facility type", List.of( createOrganisationUnitGroup( 'A' ) ) ) )
            .withOrganisationUnitMode( OrganisationUnitSelectionMode.SELECTED )
            .withValue( deX )
            .withProgram( programA )
            .withTableName( getTable( programA.getUid() ) )
            .withAggregationType( new AnalyticsAggregationType( AggregationType.LAST, AggregationType.LAST ) )
            .withAggregateData( true )
            .build();

        subject.getAggregatedEventData( params, createGrid(), 200000 );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String subquery = "from (select \"psi\",\"yearly\",\"deabcdefghX\",\"ou\"," +
            "cast('202201' as text) as \"monthly\",\"jkYhtGth12t\"," +
            "row_number() over (partition by ax.\"ou\",ax.\"jkYhtGth12t\" " +
            "order by ax.\"executiondate\" desc) as pe_rank from analytics_event_prabcdefghA as ax " +
            "where ax.\"executiondate\" >= '2012-01-31' and ax.\"executiondate\" <= '2022-01-31' " +
            "and ax.\"deabcdefghX\" is not null)";

        assertThat( params.isAggregationType( AggregationType.LAST ), is( true ) );
        assertThat( sql.getValue(), containsString( subquery ) );
    }

    @Test
    void verifySortClauseHandlesProgramIndicators()
    {
        Program program = createProgram( 'P' );
        ProgramIndicator piA = createProgramIndicator( 'A', program, ".", "." );
        piA.setUid( "TLKx7vllb1I" );

        ProgramIndicator piB = createProgramIndicator( 'B', program, ".", "." );
        piA.setUid( "CCKx3gllb2P" );

        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        Period peA = PeriodType.getPeriodFromIsoString( "201501" );

        DataElement deA = createDataElement( 'A' );
        deA.setUid( "ZE4cgllb2P" );

        DataQueryParams params = DataQueryParams.newBuilder()
            .withDataType( DataType.NUMERIC )
            .withTableName( "analytics" )
            .withPeriodType( PeriodTypeEnum.QUARTERLY.getName() )
            .withAggregationType( AnalyticsAggregationType.fromAggregationType( AggregationType.DEFAULT ) )
            .addDimension(
                new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.PROGRAM_INDICATOR, getList( piA, piB ) ) )
            .addFilter( new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.DATA_X, getList( peA ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA ) ) )
            .build();

        EventQueryParams.Builder eventQueryParamsBuilder = new EventQueryParams.Builder( params )
            .withProgram( program )
            .addAscSortItem( new QueryItem( piA ) )
            .addDescSortItem( new QueryItem( piB ) )
            .addAscSortItem( new QueryItem( deA ) );

        String sql = subject.getEventsOrEnrollmentsSql( eventQueryParamsBuilder.build(), 100 );

        assertThat( sql, containsString(
            "order by \"" + piA.getUid() + "\" asc nulls last,\"" + deA.getUid() + "\" asc nulls last,\"" + piB.getUid()
                + "\"" ) );
    }

    private void verifyFirstOrLastAggregationTypeSubquery( AnalyticsAggregationType analyticsAggregationType )
    {
        DataElement deU = createDataElement( 'U' );

        EventQueryParams params = new EventQueryParams.Builder( createRequestParamsWithFilter( ValueType.TEXT ) )
            .withValue( deU )
            .withAggregationType( analyticsAggregationType )
            .withAggregateData( true )
            .build();

        subject.getAggregatedEventData( params, createGrid(), 200000 );

        verify( jdbcTemplate ).queryForRowSet( sql.capture() );

        String expectedLastSubquery = "from (select \"psi\",\"yearly\",\"" + deU.getUid()
            + "\",cast('2000Q1' as text) as \"monthly\",\"ou\","
            + "row_number() over (partition by ax.\"ou\",ax.\"ao\" order by ax.\"executiondate\" "
            + (analyticsAggregationType == AnalyticsAggregationType.LAST ? "desc" : "asc") + ") as pe_rank "
            + "from " + getTable( programA.getUid() ) + " as ax where ax.\"executiondate\" >= '1990-03-31' "
            + "and ax.\"executiondate\" <= '2000-03-31' and ax.\"" + deU.getUid() + "\" is not null)";

        assertThat( sql.getValue(), containsString( expectedLastSubquery ) );
    }

    private EventQueryParams createRequestParamsWithFilter( ValueType queryItemValueType )
    {
        EventQueryParams.Builder params = new EventQueryParams.Builder( createRequestParams( queryItemValueType ) );
        QueryItem queryItem = params.build().getItems().get( 0 );
        queryItem.addFilter( new QueryFilter( QueryOperator.GT, "10" ) );

        return params.build();
    }

    private Grid createGrid()
    {
        return new ListGrid()
            .addHeader( new GridHeader(
                "fWIAEtYVEGk", "Mode of discharge", ValueType.TEXT, false, true ) )
            .addHeader( new GridHeader(
                "pe", "Period", ValueType.TEXT, false, true ) )
            .addHeader( new GridHeader(
                "value", "Value", ValueType.NUMBER, false, true ) );
    }

    private void mockRowSet()
    {
        // Simulate one row only
        when( rowSet.next() ).thenReturn( true ).thenReturn( false );

        when( rowSet.getString( "monthly" ) ).thenReturn( "201701" );
        when( rowSet.getString( "ou" ) ).thenReturn( "Sierra Leone" );
        when( rowSet.getInt( "value" ) ).thenReturn( 100 );
    }

    private void assertSql( String expected, String actual )
    {
        expected = "select " + DEFAULT_COLUMNS_WITH_REGISTRATION + "," + expected;

        assertThat( actual, is( expected ) );
    }

    @Override
    String getTableName()
    {
        return TABLE_NAME;
    }
}
