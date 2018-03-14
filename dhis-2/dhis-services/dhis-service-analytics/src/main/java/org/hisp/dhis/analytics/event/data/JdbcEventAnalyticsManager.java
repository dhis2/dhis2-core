package org.hisp.dhis.analytics.event.data;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.util.Precision;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.Rectangle;
import org.hisp.dhis.analytics.event.EventAnalyticsManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.*;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.ExpressionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang.time.DateUtils.addYears;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_LATITUDE;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_LONGITUDE;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.*;
import static org.hisp.dhis.system.util.DateUtils.getMediumDateString;

/**
 * TODO could use row_number() and filtering for paging, but not supported on MySQL.
 * 
 * @author Lars Helge Overland
 */
public class JdbcEventAnalyticsManager 
    extends AbstractJdbcEventAnalyticsManager
        implements EventAnalyticsManager
{
    protected static final Log log = LogFactory.getLog( JdbcEventAnalyticsManager.class );
    
    @Override
    public Grid getEvents( EventQueryParams params, Grid grid, int maxLimit )
    {
        List<String> fixedCols = Lists.newArrayList( "psi", "ps", "executiondate", "longitude", "latitude", "ouname", "oucode" );
        
        List<String> selectCols = ListUtils.distinctUnion( fixedCols, getSelectColumns( params ) );

        String sql = "select " + StringUtils.join( selectCols, "," ) + " ";

        // ---------------------------------------------------------------------
        // Criteria
        // ---------------------------------------------------------------------

        sql += getFromClause( params );
        
        sql += getWhereClause( params );
        
        // ---------------------------------------------------------------------
        // Sorting
        // ---------------------------------------------------------------------

        if ( params.isSorting() )
        {
            sql += "order by ";

            for ( DimensionalItemObject item : params.getAsc() )
            {
                sql += statementBuilder.columnQuote( item.getUid() ) + " asc,";
            }

            for  ( DimensionalItemObject item : params.getDesc() )
            {
                sql += statementBuilder.columnQuote( item.getUid() ) + " desc,";
            }

            sql = removeLastComma( sql ) + " ";
        }
        
        // ---------------------------------------------------------------------
        // Paging
        // ---------------------------------------------------------------------

        if ( params.isPaging() )
        {
            sql += "limit " + params.getPageSizeWithDefault() + " offset " + params.getOffset();
        }
        else if ( maxLimit > 0 )
        {
            sql += "limit " + ( maxLimit + 1 );
        }

        // ---------------------------------------------------------------------
        // Grid
        // ---------------------------------------------------------------------

        try
        {
            getEvents( grid, params, sql );
        }
        catch ( BadSqlGrammarException ex )
        {
            log.info( QUERY_ERR_MSG, ex );
        }
        
        return grid;
    }

    private void getEvents( Grid grid, EventQueryParams params, String sql )
    {
        log.debug( String.format( "Analytics event query SQL: %s", sql ) );
        
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        while ( rowSet.next() )
        {
            grid.addRow();
            
            int index = 1;
            
            for ( GridHeader header : grid.getHeaders() )
            {
                if ( ITEM_LONGITUDE.equals( header.getName() ) || ITEM_LATITUDE.equals( header.getName() ) )
                {
                    double val = rowSet.getDouble( index );
                    grid.addValue( Precision.round( val, COORD_DEC ) );
                }
                else if ( Double.class.getName().equals( header.getType() ) && !header.hasLegendSet() )
                {
                    double val = rowSet.getDouble( index );
                    grid.addValue( params.isSkipRounding() ? val : MathUtils.getRounded( val ) );
                }
                else
                {
                    grid.addValue( rowSet.getString( index ) );
                }
                
                index++;
            }
        }
    }

    @Override
    public Grid getEventClusters( EventQueryParams params, Grid grid, int maxLimit )
    {
        String clusterField = params.getCoordinateField();
        String quotedClusterField = statementBuilder.columnQuote( clusterField );
        
        List<String> columns = Lists.newArrayList( "count(psi) as count", 
            "ST_AsText(ST_Centroid(ST_Collect(" + quotedClusterField + "))) as center", "ST_Extent(" + quotedClusterField + ") as extent" );

        columns.add( params.isIncludeClusterPoints() ?
            "array_to_string(array_agg(psi), ',') as points" :
            "case when count(psi) = 1 then array_to_string(array_agg(psi), ',') end as points" );
        
        String sql = "select " + StringUtils.join( columns, "," ) + " ";

        sql += getFromClause( params );
        
        sql += getWhereClause( params );
        
        sql += "group by ST_SnapToGrid(ST_Transform(" + quotedClusterField + ", 3785), " + params.getClusterSize() + ") ";

        log.debug( String.format( "Analytics event cluster SQL: %s", sql ) );
        
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        while ( rowSet.next() )
        {
            grid.addRow()
                .addValue( rowSet.getLong( "count" ) )
                .addValue( rowSet.getString( "center" ) )
                .addValue( rowSet.getString( "extent" ) )
                .addValue( rowSet.getString( "points" ) );         
        }
        
        return grid;
    }

    @Override
    public long getEventCount( EventQueryParams params )
    {
        String sql = "select count(psi) ";

        sql += getFromClause( params );
        
        sql += getWhereClause( params );
        
        long count = 0;
        
        try
        {
            log.debug( "Analytics event count SQL: " + sql );
            
            count = jdbcTemplate.queryForObject( sql, Long.class );
        }
        catch ( BadSqlGrammarException ex )
        {
            log.info( QUERY_ERR_MSG, ex );
        }
        
        return count;
    }
    
    @Override
    public Rectangle getRectangle( EventQueryParams params )
    {
        String clusterField = params.getCoordinateField();
        String quotedClusterField = statementBuilder.columnQuote( clusterField );
                
        String sql = "select count(psi) as " + COL_COUNT + ", ST_Extent(" + quotedClusterField + ") as " + COL_EXTENT + " ";

        sql += getFromClause( params );
        
        sql += getWhereClause( params );
        
        log.debug( String.format( "Analytics event count and extent SQL: %s", sql ) );
        
        Rectangle rectangle = new Rectangle();
        
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );
        
        if ( rowSet.next() )
        {
            Object extent = rowSet.getObject( COL_EXTENT );
            
            rectangle.setCount( rowSet.getLong( COL_COUNT ) );
            rectangle.setExtent( extent != null ? String.valueOf( rowSet.getObject( COL_EXTENT ) ) : null );
        }
        
        return rectangle;
    }
    
    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns a from SQL clause for the given analytics table partition.
     * 
     * @param params the {@link EventQueryParams}.
     */
    protected String getFromClause( EventQueryParams params )
    {
        String sql = " from ";

        if ( params.isAggregateData() && params.hasValueDimension() && params.getAggregationTypeFallback().isLastPeriodAggregationType() )
        {
            sql += getLastValueSubquerySql( params );
        }
        else
        {
            sql += params.getTableName();
        }
        
        return sql + " ";
    }

    /**
     * Returns a from and where SQL clause. If this is a program indicator with non-default boundaries, the relationship 
     * with the reporting period is specified with where conditions on the enrollment or incident dates. If the default 
     * boundaries is used, or the params does not include program indicators, the periods are joined in from the analytics
     * tables the normal way. A where clause can never have a mix of indicators with non-default boundaries and regular 
     * analytics table periods.
     * @param params the {@link EventQueryParams}.
     */
    protected String getWhereClause( EventQueryParams params )
    {
        String sql = "";
        SqlHelper sqlHelper = new SqlHelper();
        
        // ---------------------------------------------------------------------
        // Periods
        // ---------------------------------------------------------------------
        if ( params.hasNonDefaultBoundaries() )
        {
            for ( AnalyticsPeriodBoundary boundary : params.getProgramIndicator().getAnalyticsPeriodBoundaries() )
            {
                sql += sqlHelper.whereAnd() + " " + boundary.getSqlCondition( params.getEarliestStartDate(), params.getLatestEndDate() ) + " ";
            }
        }
        else if ( params.hasStartEndDate() )
        {        
            sql += sqlHelper.whereAnd() + " " + statementBuilder.columnQuote( "executiondate") + " >= '" + getMediumDateString( params.getStartDate() ) + "' ";
            sql += sqlHelper.whereAnd() + " "  + statementBuilder.columnQuote( "executiondate") + " <= '" + getMediumDateString( params.getEndDate() ) + "' ";
        }
        else // Periods
        {
            sql += sqlHelper.whereAnd() + " " + statementBuilder.columnQuote( params.getPeriodType().toLowerCase() ) + " in (" + getQuotedCommaDelimitedString( getUids( params.getDimensionOrFilterItems( PERIOD_DIM_ID ) ) ) + ") ";
        }

        // ---------------------------------------------------------------------
        // Organisation units
        // ---------------------------------------------------------------------

        if ( params.isOrganisationUnitMode( OrganisationUnitSelectionMode.SELECTED ) )
        {
            sql += sqlHelper.whereAnd() + " ou in (" + getQuotedCommaDelimitedString( getUids( params.getDimensionOrFilterItems( ORGUNIT_DIM_ID ) ) ) + ") ";
        }
        else if ( params.isOrganisationUnitMode( OrganisationUnitSelectionMode.CHILDREN ) )
        {
            sql += sqlHelper.whereAnd() + " and ou in (" + getQuotedCommaDelimitedString( getUids( params.getOrganisationUnitChildren() ) ) + ") ";
        }
        else // Descendants
        {
            sql += sqlHelper.whereAnd() + " (";
            
            for ( DimensionalItemObject object : params.getDimensionOrFilterItems( ORGUNIT_DIM_ID ) )
            {
                OrganisationUnit unit = (OrganisationUnit) object;
                sql += statementBuilder.columnQuote( "uidlevel" + unit.getLevel() ) + " = '" + unit.getUid() + "' or ";
            }
            
            sql = removeLastOr( sql ) + ") ";
        }

        // ---------------------------------------------------------------------
        // Organisation unit group sets
        // ---------------------------------------------------------------------

        List<DimensionalObject> dynamicDimensions = params.getDimensionsAndFilters( 
            Sets.newHashSet( DimensionType.ORGANISATION_UNIT_GROUP_SET, DimensionType.CATEGORY ) );
        
        for ( DimensionalObject dim : dynamicDimensions )
        {            
            String col = statementBuilder.columnQuote( dim.getDimensionName() );
            
            sql += sqlHelper.whereAnd() + " " + col + " in (" + getQuotedCommaDelimitedString( getUids( dim.getItems() ) ) + ") ";
        }

        // ---------------------------------------------------------------------
        // Program stage
        // ---------------------------------------------------------------------

        if ( params.hasProgramStage() )
        {
            sql += sqlHelper.whereAnd() + " " + statementBuilder.columnQuote( "ps" ) + " = '" + params.getProgramStage().getUid() + "' ";
        }

        // ---------------------------------------------------------------------
        // Query items and filters
        // ---------------------------------------------------------------------

        for ( QueryItem item : params.getItems() )
        {
            if ( item.hasFilter() )
            {
                for ( QueryFilter filter : item.getFilters() )
                {
                    sql += sqlHelper.whereAnd() + " " + getSelectSql( item, params.getEarliestStartDate(), params.getLatestEndDate() ) +
                        " " + filter.getSqlOperator() + " " + getSqlFilter( filter, item ) + " ";
                }
            }
        }
        
        for ( QueryItem item : params.getItemFilters() )
        {
            if ( item.hasFilter() )
            {
                for ( QueryFilter filter : item.getFilters() )
                {
                    sql += sqlHelper.whereAnd() + " " + getSelectSql( item, params.getEarliestStartDate(), params.getLatestEndDate() ) +
                        " " + filter.getSqlOperator() + " " + getSqlFilter( filter, item ) + " ";
                }
            }
        }

        // ---------------------------------------------------------------------
        // Filter expression
        // ---------------------------------------------------------------------

        if ( params.hasProgramIndicatorDimension() && params.getProgramIndicator().hasFilter() )
        {
            String filter = programIndicatorService.getAnalyticsSQl( params.getProgramIndicator().getFilter(), 
                params.getProgramIndicator(), false, params.getEarliestStartDate(), params.getLatestEndDate() );
            
            String sqlFilter = ExpressionUtils.asSql( filter );
            
            sql += sqlHelper.whereAnd() + " (" + sqlFilter + ") ";
        }
        
        if ( params.hasProgramIndicatorDimension() )
        {
            String anyValueFilter = programIndicatorService.getAnyValueExistsClauseAnalyticsSql( params.getProgramIndicator().getExpression(), params.getProgramIndicator().getAnalyticsType() );
            
            if ( anyValueFilter != null )
            {
                sql += sqlHelper.whereAnd() + " (" + anyValueFilter + ") ";
            }
        }
        
        // ---------------------------------------------------------------------
        // Various filters
        // ---------------------------------------------------------------------

        if ( params.hasProgramStatus() )
        {
            sql += sqlHelper.whereAnd() + " pistatus = '" + params.getProgramStatus().name() + "' ";
        }

        if ( params.hasEventStatus() )
        {
            sql += sqlHelper.whereAnd() + " psistatus = '" + params.getEventStatus().name() + "' ";
        }

        if ( params.isCoordinatesOnly() )
        {
            sql += sqlHelper.whereAnd() + " (longitude is not null and latitude is not null) ";
        }
        
        if ( params.isGeometryOnly() )
        {
            sql += sqlHelper.whereAnd() + " " + statementBuilder.columnQuote( params.getCoordinateField() ) + " is not null ";
        }
        
        if ( params.isCompletedOnly() )
        {
            sql += sqlHelper.whereAnd() + " completeddate is not null ";
        }
        
        if ( params.hasBbox() )
        {
            sql += sqlHelper.whereAnd() + " " + statementBuilder.columnQuote( params.getCoordinateField() ) + " && ST_MakeEnvelope(" + params.getBbox() + ",4326) ";
        }

        // ---------------------------------------------------------------------
        // Partitions restriction to allow constraint exclusion
        // ---------------------------------------------------------------------
        
        if ( !params.isSkipPartitioning() && params.hasPartitions() )
        {
            sql += sqlHelper.whereAnd() + " " + statementBuilder.columnQuote( "yearly" ) + " in (" + 
                TextUtils.getQuotedCommaDelimitedString( params.getPartitions().getPartitions() ) + ") ";
        }

        // ---------------------------------------------------------------------
        // Period rank restriction to get last value only
        // ---------------------------------------------------------------------
        
        if ( params.getAggregationTypeFallback().isLastPeriodAggregationType() )
        {
            sql += sqlHelper.whereAnd() + " " + statementBuilder.columnQuote( "pe_rank" ) + " = 1 ";
        }
        
        return sql;
    }

    /**
     * Generates a sub query which provides a view of the data where each row is
     * ranked by the execution date, latest first. The events are partitioned by 
     * org unit and attribute option combo. A column {@code pe_rank} defines the rank. 
     * Only data for the last 10 years relative to the period end date is included. 
     */
    private String getLastValueSubquerySql( EventQueryParams params )
    {
        Assert.isTrue( params.hasValueDimension(), "Last value aggregation type query must have value dimension" );
        
        Date latest = params.getLatestEndDate();
        Date earliest = addYears( latest, LAST_VALUE_YEARS_OFFSET );
        String valueItem = statementBuilder.columnQuote( params.getValue().getDimensionItem() );
        List<String> columns = getLastValueSubqueryQuotedColumns( params );
        
        String sql = "(select ";
        
        for ( String col : columns )
        {
            sql += col + ",";
        }
        
        sql += 
            "row_number() over (" + 
                "partition by ou, ao " +
                "order by executiondate desc) as pe_rank " +
            "from " + params.getTableName() + " " +
            "where executiondate >= '" + getMediumDateString( earliest ) + "' " +
            "and executiondate <= '" + getMediumDateString( latest ) + "' " +
            "and " + valueItem + " is not null) " +
            "as " + params.getTableName();
        
        return sql;
    }

    /**
     * Returns quoted names of columns for the {@link AggregationType#LAST} sub query.
     * The period dimension is replaced by the name of the single period in the given 
     * query.
     */
    private List<String> getLastValueSubqueryQuotedColumns( EventQueryParams params )
    {
        Period period = params.getLatestPeriod();
        
        String valueItem = params.getValue().getDimensionItem();
        
        List<String> cols = Lists.newArrayList( "yearly", valueItem );

        cols = cols.stream().map( col -> statementBuilder.columnQuote( col ) ).collect( Collectors.toList() );
        
        for ( DimensionalObject dim : params.getDimensionsAndFilters() )
        {            
            if ( DimensionType.PERIOD == dim.getDimensionType() && period != null )
            {
                String alias = statementBuilder.columnQuote( dim.getDimensionName() );
                String col = "cast('" + period.getDimensionItem() + "' as text) as " + alias;
                
                cols.remove( alias ); // Remove column if already present, i.e. "yearly"
                cols.add( col );
            }
            else
            {
                cols.add( statementBuilder.columnQuote( dim.getDimensionName() ) );
            }
        }

        return cols;
    }
}
