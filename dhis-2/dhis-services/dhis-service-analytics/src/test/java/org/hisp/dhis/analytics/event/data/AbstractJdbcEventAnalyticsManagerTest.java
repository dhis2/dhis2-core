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

import static com.google.common.collect.Lists.newArrayList;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.programindicator.DefaultProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.jdbc.statementbuilder.PostgreSQLStatementBuilder;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith( MockitoExtension.class )
class AbstractJdbcEventAnalyticsManagerTest extends
    EventAnalyticsTest
{

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ProgramIndicatorService programIndicatorService;

    @Mock
    private ExecutionPlanStore executionPlanStore;

    private JdbcEventAnalyticsManager subject;

    private Program programA;

    private DataElement dataElementA;

    private Date from = getDate( 2017, 10, 10 );

    private Date to = getDate( 2018, 10, 10 );

    @BeforeEach
    public void setUp()
    {
        StatementBuilder statementBuilder = new PostgreSQLStatementBuilder();
        DefaultProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder = new DefaultProgramIndicatorSubqueryBuilder(
            programIndicatorService );
        subject = new JdbcEventAnalyticsManager( jdbcTemplate, statementBuilder, programIndicatorService,
            programIndicatorSubqueryBuilder, new EventTimeFieldSqlRenderer( statementBuilder ), executionPlanStore );

        // data init

        programA = createProgram( 'A' );

        dataElementA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM );
        dataElementA.setUid( "fWIAEtYVEGk" );
    }

    @Test
    void verifyGetSelectSqlWithProgramIndicator()
    {
        ProgramIndicator programIndicator = createProgramIndicator( 'A', programA, "9.0", null );
        QueryItem item = new QueryItem( programIndicator );
        subject.getSelectSql( new QueryFilter(), item, from, to );

        verify( programIndicatorService ).getAnalyticsSql( programIndicator.getExpression(), NUMERIC, programIndicator,
            from, to );
    }

    @Test
    void verifyGetSelectSqlWithTextDataElementIgnoringCase()
    {
        DimensionalItemObject dio = new BaseDimensionalItemObject( dataElementA.getUid() );

        QueryItem item = new QueryItem( dio );
        item.setValueType( ValueType.TEXT );

        QueryFilter queryFilter = new QueryFilter( QueryOperator.IEQ, "IEQ" );

        String column = subject.getSelectSql( queryFilter, item, from, to );

        assertThat( column, is( "lower(ax.\"" + dataElementA.getUid() + "\")" ) );
    }

    @Test
    void verifyGetSelectSqlWithTextDataElement()
    {
        DimensionalItemObject dio = new BaseDimensionalItemObject( dataElementA.getUid() );

        QueryItem item = new QueryItem( dio );
        item.setValueType( ValueType.TEXT );

        QueryFilter queryFilter = new QueryFilter( QueryOperator.EQ, "EQ" );

        String column = subject.getSelectSql( queryFilter, item, from, to );

        assertThat( column, is( "ax.\"" + dataElementA.getUid() + "\"" ) );
    }

    @Test
    void verifyGetSelectSqlWithNonTextDataElement()
    {
        DimensionalItemObject dio = new BaseDimensionalItemObject( dataElementA.getUid() );

        QueryItem item = new QueryItem( dio );
        item.setValueType( ValueType.NUMBER );

        String column = subject.getSelectSql( new QueryFilter(), item, from, to );

        assertThat( column, is( "ax.\"" + dataElementA.getUid() + "\"" ) );
    }

    @Test
    void verifyGetCoordinateColumn()
    {
        // Given
        DimensionalItemObject dio = new BaseDimensionalItemObject( dataElementA.getUid() );
        QueryItem item = new QueryItem( dio );

        // When
        String column = subject.getCoordinateColumn( item ).asSql();

        // Then
        String colName = quote( item.getItemName() );

        assertThat( column, is( "'[' || round(ST_X(" + colName + ")::numeric, 6) || ',' || round(ST_Y(" + colName
            + ")::numeric, 6) || ']' as " + colName ) );

        return;
    }

    @Test
    void verifyGetColumn()
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
    void verifyGetAggregateClauseWithValue()
    {
        DimensionalItemObject dio = new BaseDimensionalItemObject( dataElementA.getUid() );

        EventQueryParams params = new EventQueryParams.Builder( createRequestParams() )
            .withValue( dio )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .build();

        String clause = subject.getAggregateClause( params );

        assertThat( clause, is( "sum(ax.\"fWIAEtYVEGk\")" ) );
    }

    @Test
    void verifyGetAggregateClauseWithValueFails()
    {
        DimensionalItemObject dio = new BaseDimensionalItemObject( dataElementA.getUid() );

        EventQueryParams params = new EventQueryParams.Builder( createRequestParams() )
            .withValue( dio )
            .withAggregationType( fromAggregationType( AggregationType.CUSTOM ) )
            .build();
        assertThrows( IllegalArgumentException.class, () -> subject.getAggregateClause( params ) );
    }

    @Test
    void verifyGetAggregateClauseWithProgramIndicator()
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
    void verifyGetAggregateClauseWithProgramIndicatorAndCustomAggregationType()
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
    void verifyGetAggregateClauseWithEnrollmentDimension()
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
    void verifyGetColumnsWithAttributeOrgUnitTypeAndCoordinatesReturnsFetchesCoordinatesFromOrgUnite()
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

        final List<String> columns = this.subject.getSelectColumns( params, false );

        // Then

        assertThat( columns, hasSize( 3 ) );
        assertThat( columns, containsInAnyOrder( "ax.\"pe\"", "ax.\"ou\"",
            "'[' || round(ST_X(ST_Centroid(\"" + deA.getUid() + "_geom"
                + "\"))::numeric, 6) || ',' || round(ST_Y(ST_Centroid(\"" + deA.getUid() + "_geom"
                + "\"))::numeric, 6) || ']' as \"" + deA.getUid() + "_geom" + "\"" ) );
    }

    @Test
    void verifyGetWhereClauseWithAttributeOrgUnitTypeAndCoordinatesReturnsFetchesCoordinatesFromOrgUnite()
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
    void testGetWhereClauseWithMultipleOrgUnitDescendantsAtSameLevel()
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
                "and ax.\"uidlevel0\" in ('ouabcdefghA','ouabcdefghB','ouabcdefghC')" ) );
    }

    @Test
    void testGeItemNoFiltersSql()
    {
        EventQueryParams queryParams = new EventQueryParams.Builder()
            .addItem( buildQueryItemWithGroupAndFilters( "item", UUID.randomUUID(), Collections.emptyList() ) )
            .build();
        assertEquals( "", subject.getStatementForDimensionsAndFilters( queryParams, new SqlHelper() ) );
    }

    @Test
    void testGetItemSimpleFilterSql()
    {
        EventQueryParams queryParams = new EventQueryParams.Builder()
            .addItem( buildQueryItemWithGroupAndFilters(
                "item",
                UUID.randomUUID(),
                List.of( buildEqQueryFilter( "A" ) ) ) )
            .build();
        String result = subject.getStatementForDimensionsAndFilters( queryParams, new SqlHelper() );
        assertEquals( "where ax.\"item\" = 'A' ", result );
    }

    @Test
    void testGetItemTwoConditionsSameGroupSql()
    {
        EventQueryParams queryParams = new EventQueryParams.Builder()
            .addItem( buildQueryItemWithGroupAndFilters(
                "item",
                UUID.randomUUID(),
                List.of(
                    buildEqQueryFilter( "A" ),
                    buildEqQueryFilter( "B" ) ) ) )
            .build();
        String result = subject.getStatementForDimensionsAndFilters( queryParams, new SqlHelper() );
        assertEquals( "where ax.\"item\" = 'A'  and ax.\"item\" = 'B' ", result );
    }

    @Test
    void testGetItemSameItemTwoConditionsSqlEnhancedConditions()
    {
        EventQueryParams queryParams = new EventQueryParams.Builder()
            .addItem( buildQueryItemWithGroupAndFilters(
                "item",
                UUID.randomUUID(),
                List.of(
                    buildEqQueryFilter( "A" ),
                    buildEqQueryFilter( "B" ) ) ) )
            .withEnhancedConditions( true )
            .build();

        String result = subject.getStatementForDimensionsAndFilters( queryParams, new SqlHelper() );
        assertEquals( "where (ax.\"item\" = 'A'  and ax.\"item\" = 'B' )", result );
    }

    @Test
    void testGetItemTwoConditionsSameGroupSqlEnhancedConditions()
    {
        UUID groupUUID = UUID.randomUUID();
        EventQueryParams queryParams = new EventQueryParams.Builder()
            .addItem( buildQueryItemWithGroupAndFilters(
                "item1",
                groupUUID,
                List.of(
                    buildEqQueryFilter( "A" ) ) ) )
            .addItem( buildQueryItemWithGroupAndFilters(
                "item2",
                groupUUID,
                List.of(
                    buildEqQueryFilter( "B" ) ) ) )
            .withEnhancedConditions( true )
            .build();

        String result = subject.getStatementForDimensionsAndFilters( queryParams, new SqlHelper() );
        assertEquals( "where (ax.\"item1\" = 'A'  or ax.\"item2\" = 'B' )", result );
    }

    @Test
    void testGetItemTwoConditionsDifferentGroupsSqlEnhancedConditions()
    {
        UUID groupUUID1 = UUID.randomUUID();
        UUID groupUUID2 = UUID.randomUUID();
        EventQueryParams queryParams = new EventQueryParams.Builder()
            .addItem( buildQueryItemWithGroupAndFilters(
                "item1",
                groupUUID1,
                List.of( buildEqQueryFilter( "A" ) ) ) )
            .addItem( buildQueryItemWithGroupAndFilters(
                "item2",
                groupUUID1,
                List.of( buildEqQueryFilter( "B" ) ) ) )
            .addItem( buildQueryItemWithGroupAndFilters(
                "item3",
                groupUUID2,
                List.of( buildEqQueryFilter( "C" ) ) ) )
            .addItem( buildQueryItemWithGroupAndFilters(
                "item4",
                groupUUID2,
                List.of( buildEqQueryFilter( "D" ) ) ) )
            .withEnhancedConditions( true )
            .build();

        String result = subject.getStatementForDimensionsAndFilters( queryParams, new SqlHelper() );
        assertTrue( result.contains( "(ax.\"item1\" = 'A'  or ax.\"item2\" = 'B' )" ) );
        assertTrue( result.contains( "(ax.\"item3\" = 'C'  or ax.\"item4\" = 'D' )" ) );
    }

    private QueryFilter buildEqQueryFilter( String filter )
    {
        return new QueryFilter( QueryOperator.EQ, filter );
    }

    private QueryItem buildQueryItem( String item )
    {
        return new QueryItem( new BaseDimensionalItemObject( item ) );
    }

    private QueryItem buildQueryItemWithGroupAndFilters( String item, UUID groupUUID, Collection<QueryFilter> filters )
    {
        QueryItem queryItem = buildQueryItem( item );
        queryItem.setGroupUUID( groupUUID );
        queryItem.setFilters( new ArrayList<>( filters ) );
        return queryItem;
    }
}
