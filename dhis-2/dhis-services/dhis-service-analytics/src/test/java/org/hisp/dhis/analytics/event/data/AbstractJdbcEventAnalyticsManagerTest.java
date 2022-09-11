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
package org.hisp.dhis.analytics.event.data;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramIndicator;
import static org.hisp.dhis.DhisConvenienceTest.getDate;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.fromAggregationType;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.sql.rowset.RowSetMetaDataImpl;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.programindicator.DefaultProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.jdbc.statementbuilder.PostgreSQLStatementBuilder;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * @author Luciano Fiandesio
 */
public class AbstractJdbcEventAnalyticsManagerTest
    extends
    EventAnalyticsTest
{
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ProgramIndicatorService programIndicatorService;

    private JdbcEventAnalyticsManager subject;

    private Program programA;

    private DataElement dataElementA;

    private Date from = getDate( 2017, 10, 10 );

    private Date to = getDate( 2018, 10, 10 );

    @Before
    public void setUp()
    {
        StatementBuilder statementBuilder = new PostgreSQLStatementBuilder();
        DefaultProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder = new DefaultProgramIndicatorSubqueryBuilder(
            programIndicatorService );
        subject = new JdbcEventAnalyticsManager( jdbcTemplate, statementBuilder, programIndicatorService,
            programIndicatorSubqueryBuilder );

        // data init

        programA = createProgram( 'A' );

        dataElementA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM );
        dataElementA.setUid( "fWIAEtYVEGk" );
    }

    @Test
    public void verifyGetSelectSqlWithProgramIndicator()
    {
        ProgramIndicator programIndicator = createProgramIndicator( 'A', programA, "9.0", null );
        QueryItem item = new QueryItem( programIndicator );

        subject.getSelectSql( item, from, to );

        verify( programIndicatorService ).getAnalyticsSql( programIndicator.getExpression(), NUMERIC, programIndicator,
            from, to );
    }

    @Test
    public void verifyGetSelectSqlWithTextDataElement()
    {
        DimensionalItemObject dio = new BaseDimensionalItemObject( dataElementA.getUid() );

        QueryItem item = new QueryItem( dio );
        item.setValueType( ValueType.TEXT );

        String column = subject.getSelectSql( item, from, to );

        assertThat( column, is( "lower(ax.\"" + dataElementA.getUid() + "\")" ) );
    }

    @Test
    public void verifyGetSelectSqlWithNonTextDataElement()
    {
        DimensionalItemObject dio = new BaseDimensionalItemObject( dataElementA.getUid() );

        QueryItem item = new QueryItem( dio );
        item.setValueType( NUMBER );

        String column = subject.getSelectSql( item, from, to );

        assertThat( column, is( "ax.\"" + dataElementA.getUid() + "\"" ) );
    }

    @Test
    public void verifyGetCoordinateColumn()
    {
        // Given
        DimensionalItemObject dio = new BaseDimensionalItemObject( dataElementA.getUid() );
        QueryItem item = new QueryItem( dio );

        // When
        String column = subject.getCoordinateColumn( item );

        // Then
        String colName = quote( item.getItemName() );

        assertThat( column, is( "'[' || round(ST_X(" + colName + ")::numeric, 6) || ',' || round(ST_Y(" + colName
            + ")::numeric, 6) || ']' as " + colName ) );

        return;
    }

    @Test
    public void verifyGetColumn()
    {
        DimensionalItemObject dio = new BaseDimensionalItemObject( dataElementA.getUid() );

        QueryItem item = new QueryItem( dio );

        String column = subject.getColumn( item );

        assertThat( column, is( "ax.\"" + dataElementA.getUid() + "\"" ) );
    }

    @Override
    String getTableName()
    {
        return "";
    }

    @Test
    public void verifyGetAggregateClauseWithValue()
    {
        DimensionalItemObject dio = new BaseDimensionalItemObject( dataElementA.getUid() );

        EventQueryParams params = new EventQueryParams.Builder( createRequestParams() )
            .withValue( dio )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .build();

        String clause = subject.getAggregateClause( params );

        assertThat( clause, is( "sum(ax.\"fWIAEtYVEGk\")" ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void verifyGetAggregateClauseWithValueFails()
    {
        DimensionalItemObject dio = new BaseDimensionalItemObject( dataElementA.getUid() );

        EventQueryParams params = new EventQueryParams.Builder( createRequestParams() )
            .withValue( dio )
            .withAggregationType( fromAggregationType( AggregationType.CUSTOM ) )
            .build();

        subject.getAggregateClause( params );

    }

    @Test
    public void verifyGetAggregateClauseWithProgramIndicator()
    {
        ProgramIndicator programIndicator = createProgramIndicator( 'A', programA, "9.0", null );
        EventQueryParams params = new EventQueryParams.Builder( createRequestParams() )
            .withProgramIndicator( programIndicator )
            .build();

        when( programIndicatorService.getAnalyticsSql( programIndicator.getExpression(), NUMERIC, programIndicator,
            params.getEarliestStartDate(), params.getLatestEndDate() ) )
                .thenReturn( "select * from table" );

        String clause = subject.getAggregateClause( params );

        assertThat( clause, is( "avg(select * from table)" ) );
    }

    @Test
    public void verifyGetAggregateClauseWithProgramIndicatorAndCustomAggregationType()
    {
        ProgramIndicator programIndicator = createProgramIndicator( 'A', programA, "9.0", null );
        programIndicator.setAggregationType( AggregationType.CUSTOM );

        EventQueryParams params = new EventQueryParams.Builder( createRequestParams() )
            .withProgramIndicator( programIndicator ).build();

        when( programIndicatorService.getAnalyticsSql( programIndicator.getExpression(), NUMERIC, programIndicator,
            params.getEarliestStartDate(), params.getLatestEndDate() ) )
                .thenReturn( "select * from table" );

        String clause = subject.getAggregateClause( params );

        assertThat( clause, is( "(select * from table)" ) );
    }

    @Test
    public void verifyGetAggregateClauseWithEnrollmentDimension()
    {
        ProgramIndicator programIndicator = createProgramIndicator( 'A', programA, "9.0", null );
        programIndicator.setAnalyticsType( AnalyticsType.ENROLLMENT );
        EventQueryParams params = new EventQueryParams.Builder( createRequestParams() )
            .withProgramIndicator( programIndicator )
            .build();

        when( programIndicatorService.getAnalyticsSql( programIndicator.getExpression(), NUMERIC, programIndicator,
            params.getEarliestStartDate(), params.getLatestEndDate() ) )
                .thenReturn( "select * from table" );

        String clause = subject.getAggregateClause( params );

        assertThat( clause, is( "avg(select * from table)" ) );
    }

    @Test
    public void verifyGetColumnsWithAttributeOrgUnitTypeAndCoordinatesReturnsFetchesCoordinatesFromOrgUnite()
    {
        // Given

        DataElement deA = createDataElement( 'A', ValueType.ORGANISATION_UNIT, AggregationType.NONE );
        DimensionalObject periods = new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD,
            newArrayList( MonthlyPeriodType.getPeriodFromIsoString( "201701" ) ) );

        DimensionalObject orgUnits = new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID,
            DimensionType.ORGANISATION_UNIT, "ouA", newArrayList( createOrganisationUnit( 'A' ) ) );

        QueryItem qiA = new QueryItem( deA, null, deA.getValueType(), deA.getAggregationType(), null );

        // When
        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension( periods )
            .addDimension( orgUnits )
            .addItem( qiA )
            .withCoordinateField( deA.getUid() )
            .withSkipData( true )
            .withSkipMeta( false )
            .build();

        final List<String> columns = this.subject.getSelectColumns( params );

        // Then

        assertThat( columns, hasSize( 3 ) );
        assertThat( columns, containsInAnyOrder( "ax.\"pe\"", "ax.\"ou\"",
            "'[' || round(ST_X(ST_Centroid(\"" + deA.getUid() + "_geom"
                + "\"))::numeric, 6) || ',' || round(ST_Y(ST_Centroid(\"" + deA.getUid() + "_geom"
                + "\"))::numeric, 6) || ']' as \"" + deA.getUid() + "_geom" + "\"" ) );
    }

    @Test
    public void verifyGetWhereClauseWithAttributeOrgUnitTypeAndCoordinatesReturnsFetchesCoordinatesFromOrgUnite()
    {
        // Given

        DataElement deA = createDataElement( 'A', ValueType.ORGANISATION_UNIT, AggregationType.NONE );
        DimensionalObject periods = new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD,
            newArrayList( MonthlyPeriodType.getPeriodFromIsoString( "201701" ) ) );

        DimensionalObject orgUnits = new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID,
            DimensionType.ORGANISATION_UNIT, "ouA", newArrayList( createOrganisationUnit( 'A' ) ) );

        QueryItem qiA = new QueryItem( deA, null, deA.getValueType(), deA.getAggregationType(), null );

        // When
        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension( periods )
            .addDimension( orgUnits )
            .addItem( qiA )
            .withCoordinateField( deA.getUid() )
            .withSkipData( true )
            .withSkipMeta( false )
            .withStartDate( new Date() )
            .withEndDate( new Date() )
            // the not null condition is only triggered by this flag (or
            // withGeometry) being true
            .withCoordinatesOnly( true )
            .build();

        final String whereClause = this.subject.getWhereClause( params );

        // Then
        assertThat( whereClause, containsString( "and ax.\"" + deA.getUid() + "_geom" + "\" is not null" ) );
    }

    @Test
    public void testGetWhereClauseWithMultipleOrgUnitDescendantsAtSameLevel()
    {
        // Given
        final DimensionalObject periods = new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID,
            DimensionType.PERIOD,
            newArrayList( MonthlyPeriodType.getPeriodFromIsoString( "201801" ) ) );

        final DimensionalObject multipleOrgUnitsSameLevel = new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID,
            DimensionType.ORGANISATION_UNIT, "uidlevel1", "Level 1",
            newArrayList( createOrganisationUnit( 'A' ), createOrganisationUnit( 'B' ),
                createOrganisationUnit( 'C' ) ) );

        final EventQueryParams params = new EventQueryParams.Builder()
            .addDimension( periods )
            .addDimension( multipleOrgUnitsSameLevel )
            .withSkipData( true )
            .withSkipMeta( false )
            .withStartDate( new Date() )
            .withEndDate( new Date() )
            .build();

        // When
        final String whereClause = this.subject.getWhereClause( params );

        // Then
        assertThat( whereClause,
            containsString(
                "and (ax.\"uidlevel0\" = 'ouabcdefghA' or ax.\"uidlevel0\" = 'ouabcdefghB' or ax.\"uidlevel0\" = 'ouabcdefghC' )" ) );
    }

    @Test
    public void testAddGridValueForDoubleObject()
        throws SQLException
    {
        // Given
        Double doubleObject = 35.5d;
        int index = 1;

        RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
        metaData.setColumnCount( 2 );
        metaData.setColumnName( 1, "col-1" );
        metaData.setColumnName( 2, "col-2" );

        ResultSet resultSet = mock( ResultSet.class );
        when( resultSet.getObject( index ) ).thenReturn( doubleObject );
        when( resultSet.getMetaData() ).thenReturn( metaData );

        EventQueryParams queryParams = new EventQueryParams.Builder()
            .withSkipRounding( false ).build();

        GridHeader header = new GridHeader( "header-1", "column-1", NUMBER, false, false );
        Grid grid = new ListGrid();
        grid.addHeader( header );
        grid.addRow();

        SqlRowSet sqlRowSet = new ResultSetWrappingSqlRowSet( resultSet );

        // When
        subject.addGridValue( grid, header, index, sqlRowSet, queryParams );

        // Then
        assertTrue( grid.getColumn( 0 ).contains( doubleObject ) );
    }

    @Test
    public void testAddGridValueForNull()
        throws SQLException
    {
        // Given
        Double nullObject = null;
        int index = 1;

        RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
        metaData.setColumnCount( 2 );
        metaData.setColumnName( 1, "col-1" );
        metaData.setColumnName( 2, "col-2" );

        ResultSet resultSet = mock( ResultSet.class );
        when( resultSet.getObject( index ) ).thenReturn( nullObject );
        when( resultSet.getMetaData() ).thenReturn( metaData );

        EventQueryParams queryParams = new EventQueryParams.Builder()
            .withSkipRounding( false ).build();

        GridHeader header = new GridHeader( "header-1", "column-1", NUMBER, false, false );
        Grid grid = new ListGrid();
        grid.addHeader( header );
        grid.addRow();

        SqlRowSet sqlRowSet = new ResultSetWrappingSqlRowSet( resultSet );

        // When
        subject.addGridValue( grid, header, index, sqlRowSet, queryParams );

        // Then
        assertTrue( grid.getColumn( 0 ).contains( EMPTY ) );
    }
}
