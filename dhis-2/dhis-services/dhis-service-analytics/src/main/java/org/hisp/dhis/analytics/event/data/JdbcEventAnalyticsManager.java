package org.hisp.dhis.analytics.event.data;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.removeLastComma;
import static org.hisp.dhis.commons.util.TextUtils.removeLastOr;
import static org.hisp.dhis.commons.util.TextUtils.trimEnd;
import static org.hisp.dhis.system.util.DateUtils.getMediumDateString;
import static org.hisp.dhis.system.util.MathUtils.getRounded;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsUtils;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.Rectangle;
import org.hisp.dhis.analytics.event.EventAnalyticsManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.ExpressionUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.google.common.collect.Lists;

/**
 * TODO could use row_number() and filtering for paging, but not supported on MySQL.
 * 
 * @author Lars Helge Overland
 */
public class JdbcEventAnalyticsManager
    implements EventAnalyticsManager
{
    private static final Log log = LogFactory.getLog( JdbcEventAnalyticsManager.class );
    
    private static final String QUERY_ERR_MSG = "Query failed, likely because the requested analytics table does not exist";
    private static final String ITEM_NAME_SEP = ": ";
    private static final String NA = "[N/A]";
    private static final String COL_COUNT = "count";
    private static final String COL_EXTENT = "extent";

    @Resource( name = "readOnlyJdbcTemplate" )
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StatementBuilder statementBuilder;
    
    @Autowired
    private ProgramIndicatorService programIndicatorService;
    
    // -------------------------------------------------------------------------
    // EventAnalyticsManager implementation
    // -------------------------------------------------------------------------

    @Override
    public Grid getAggregatedEventData( EventQueryParams params, Grid grid, int maxLimit )
    {
        String countClause = getAggregateClause( params );
        
        String sql = "select " + countClause + " as value," + StringUtils.join( getSelectColumns( params ), "," ) + " ";

        // ---------------------------------------------------------------------
        // Criteria
        // ---------------------------------------------------------------------

        sql += getFromWhereClause( params, Lists.newArrayList( "psi" ) );

        // ---------------------------------------------------------------------
        // Group by
        // ---------------------------------------------------------------------

        sql += "group by " + StringUtils.join( getSelectColumns( params ), "," ) + " ";

        // ---------------------------------------------------------------------
        // Sort order
        // ---------------------------------------------------------------------

        if ( params.hasSortOrder() )
        {
            sql += "order by value " + params.getSortOrder().toString().toLowerCase() + " ";
        }
        
        // ---------------------------------------------------------------------
        // Limit, add one to max to enable later check against max limit
        // ---------------------------------------------------------------------

        if ( params.hasLimit() )
        {
            sql += "limit " + params.getLimit();
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
            getAggregatedEventData( grid, params, sql );
        }
        catch ( BadSqlGrammarException ex )
        {
            log.info( QUERY_ERR_MSG, ex );
        }
        
        return grid;
    }
    
    private void getAggregatedEventData( Grid grid, EventQueryParams params, String sql )
    {
        log.debug( "Analytics event aggregate SQL: " + sql );
        
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        while ( rowSet.next() )
        {            
            grid.addRow();

            if ( params.isAggregateData() )
            {
                if ( params.hasValueDimension() )
                {
                    String itemId = params.getProgram().getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + params.getValue().getUid();
                    grid.addValue( itemId );
                }
                else if ( params.hasProgramIndicatorDimension() )
                {
                    grid.addValue( params.getProgramIndicator().getUid() );
                }                
            }
            else
            {
                for ( QueryItem queryItem : params.getItems() )
                {
                    String itemValue = rowSet.getString( queryItem.getItemName() );
                    String gridValue = params.isCollapseDataDimensions() ? getCollapsedDataItemValue( params, queryItem, itemValue ) : itemValue;
                    grid.addValue( gridValue );
                }
            }
            
            for ( DimensionalObject dimension : params.getDimensions() )
            {
                String dimensionValue = rowSet.getString( dimension.getDimensionName() );
                grid.addValue( dimensionValue );
            }
            
            if ( params.hasValueDimension() )
            {
                double value = rowSet.getDouble( "value" );
                grid.addValue( params.isSkipRounding() ? value : getRounded( value ) );
            }
            else if ( params.hasProgramIndicatorDimension() )
            {
                double value = rowSet.getDouble( "value" );
                ProgramIndicator indicator = params.getProgramIndicator();
                grid.addValue( AnalyticsUtils.getRoundedValue( params, indicator.getDecimals(), value ) );
            }
            else
            {
                int value = rowSet.getInt( "value" );
                grid.addValue( value );
            }
            
            if ( params.isIncludeNumDen() )
            {
                grid.addNullValues( 3 );
            }
        }
    }
    
    @Override
    public Grid getEvents( EventQueryParams params, Grid grid, int maxLimit )
    {
        List<String> fixedCols = Lists.newArrayList( "psi", "ps", "executiondate", "longitude", "latitude", "ouname", "oucode" );
        
        List<String> selectCols = ListUtils.distinctUnion( fixedCols, getSelectColumns( params ) );
        
        String sql = "select " + StringUtils.join( selectCols, "," ) + " ";

        // ---------------------------------------------------------------------
        // Criteria
        // ---------------------------------------------------------------------

        sql += getFromWhereClause( params, fixedCols );
        
        // ---------------------------------------------------------------------
        // Sorting
        // ---------------------------------------------------------------------

        if ( params.isSorting() )
        {
            sql += "order by ";
        
            for ( String item : params.getAsc() )
            {
                sql += statementBuilder.columnQuote( item ) + " asc,";
            }
            
            for  ( String item : params.getDesc() )
            {
                sql += statementBuilder.columnQuote( item ) + " desc,";
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
        log.debug( "Analytics event query SQL: " + sql );
        
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        while ( rowSet.next() )
        {
            grid.addRow();
            
            int index = 1;
            
            for ( GridHeader header : grid.getHeaders() )
            {
                if ( Double.class.getName().equals( header.getType() ) && !header.hasLegendSet() )
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
        params.setGeometryOnly( true );
        
        List<String> columns = Lists.newArrayList( "count(psi) as count", 
            "ST_AsText(ST_Centroid(ST_Collect(geom))) as center", "ST_Extent(geom) as extent" );

        columns.add( params.isIncludeClusterPoints() ?
            "array_to_string(array_agg(psi), ',') as points" :
            "case when count(psi) = 1 then array_to_string(array_agg(psi), ',') end as points" );
        
        String sql = "select " + StringUtils.join( columns, "," ) + " ";
        
        sql += getFromWhereClause( params, Lists.newArrayList( "psi", "geom" ) );
        
        sql += "group by ST_SnapToGrid(ST_Transform(geom, 3785), " + params.getClusterSize() + ") ";

        log.debug( "Analytics event cluster SQL: " + sql );
        
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        while ( rowSet.next() )
        {
            grid.addRow();
            
            grid.addValue( rowSet.getLong( "count" ) );
            grid.addValue( rowSet.getString( "center" ) );
            grid.addValue( rowSet.getString( "extent" ) );
            grid.addValue( rowSet.getString( "points" ) );         
        }
        
        return grid;
    }

    @Override
    public long getEventCount( EventQueryParams params )
    {
        String sql = "select count(psi) ";
        
        sql += getFromWhereClause( params, Lists.newArrayList( "psi" ) );
        
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
        params.setGeometryOnly( true );
        
        String sql = "select count(psi) as " + COL_COUNT + ", ST_Extent(geom) AS " + COL_EXTENT + " ";
        
        sql += getFromWhereClause( params, Lists.newArrayList( "psi", "geom" ) );

        log.debug( "Analytics event count and extent SQL: " + sql );
        
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
     * Returns the count clause based on value dimension and output type.
     * 
     * TODO include output type if aggregation type is count
     */
    private String getAggregateClause( EventQueryParams params )
    {
        EventOutputType outputType = params.getOutputType();
        
        if ( params.hasValueDimension() ) // && isNumeric
        {
            String function = params.getAggregationTypeFallback().getValue();
            
            String expression = statementBuilder.columnQuote( params.getValue().getUid() );
            
            return function + "(" + expression + ")";
        }
        else if ( params.hasProgramIndicatorDimension() )
        {
            String function = params.getProgramIndicator().getAggregationTypeFallback().getValue();
            
            function = TextUtils.emptyIfEqual( function, AggregationType.CUSTOM.getValue() );
            
            String expression = programIndicatorService.getAnalyticsSQl( params.getProgramIndicator().getExpression() );
            
            return function + "(" + expression + ")";
            
            //TODO check if expression is valid and safe SQL
        }
        else
        {
            if ( EventOutputType.TRACKED_ENTITY_INSTANCE.equals( outputType ) && params.isProgramRegistration() )
            {
                return "count(distinct tei)";
            }
            else if ( EventOutputType.ENROLLMENT.equals( outputType ) )
            {
                return "count(distinct pi)";
            }
            else // EVENT
            {
                return "count(psi)";
            }
        }
    }

    /**
     * Returns columns based on value dimension and output type.
     */
    private List<String> getAggregateColumns( EventQueryParams params )
    {
        EventOutputType outputType = params.getOutputType();
        
        if ( params.hasValueDimension() )
        {
            return Lists.newArrayList( statementBuilder.columnQuote( params.getValue().getUid() ) );
        }
        else if ( params.hasProgramIndicatorDimension() )
        {
            Set<String> uids = ProgramIndicator.getDataElementAndAttributeIdentifiers( params.getProgramIndicator().getExpression() );
            
            return uids.stream().map( uid -> statementBuilder.columnQuote( uid ) ).collect( Collectors.toList() );
        }
        else
        {
            if ( EventOutputType.TRACKED_ENTITY_INSTANCE.equals( outputType ) && params.isProgramRegistration() )
            {
                return Lists.newArrayList( statementBuilder.columnQuote( "tei" ) );
            }
            else if ( EventOutputType.ENROLLMENT.equals( outputType ) )
            {
                return Lists.newArrayList( statementBuilder.columnQuote( "pi" ) );
            }
        }
        
        return Lists.newArrayList();
    }
    
    /**
     * Returns the dynamic select columns. Dimensions come first and query items
     * second. Program indicator expressions are converted to SQL expressions.
     */
    private List<String> getSelectColumns( EventQueryParams params )
    {
        List<String> columns = Lists.newArrayList();
        
        for ( DimensionalObject dimension : params.getDimensions() )
        {
            columns.add( statementBuilder.columnQuote( dimension.getDimensionName() ) );
        }
        
        for ( QueryItem queryItem : params.getItems() )
        {
            if ( queryItem.isProgramIndicator() )
            {
                ProgramIndicator in = (ProgramIndicator) queryItem.getItem();
                
                String asClause = " as " + statementBuilder.columnQuote( in.getUid() );
                
                columns.add( "(" + programIndicatorService.getAnalyticsSQl( in.getExpression() ) + ")" + asClause );
            }
            else
            {
                columns.add( statementBuilder.columnQuote( queryItem.getItemName() ) );
            }
        }
        
        return columns;
    }

    /**
     * Returns the dynamic select columns. Dimensions come first and query items
     * second. Program indicator expressions are exploded into attributes and
     * data element identifiers.
     */
    private List<String> getPartitionSelectColumns( EventQueryParams params )
    {
        List<String> columns = Lists.newArrayList();
        
        for ( DimensionalObject dimension : params.getDimensions() )
        {
            columns.add( statementBuilder.columnQuote( dimension.getDimensionName() ) );
        }
        
        for ( QueryItem queryItem : params.getItems() )
        {
            if ( queryItem.isProgramIndicator() )
            {
                ProgramIndicator in = (ProgramIndicator) queryItem.getItem();
                
                Set<String> uids = ProgramIndicator.getDataElementAndAttributeIdentifiers( in.getExpression() );
                
                for ( String uid : uids )
                {
                    columns.add( statementBuilder.columnQuote( uid ) );
                }
            }
            else
            {
                columns.add( statementBuilder.columnQuote( queryItem.getItemName() ) );
            }
        }
        
        return columns;
    }
    
    /**
     * Returns a from and where SQL clause.
     * 
     * @param params the event query parameters.
     * @param fixedColumns the list of fixed column names to include.
     */
    private String getFromWhereClause( EventQueryParams params, List<String> fixedColumns )
    {
        if ( params.spansMultiplePartitions() )
        {
            return getFromWhereMultiplePartitionsClause( params, fixedColumns );
        }
        else
        {
            return getFromWhereSinglePartitionClause( params, params.getPartitions().getSinglePartition() );
        }
    }

    /**
     * Returns a from and where SQL clause for all partitions part of the given
     * query parameters.
     * 
     * @param params the event query parameters.
     * @param fixedColumns the list of fixed column names to include.
     */
    private String getFromWhereMultiplePartitionsClause( EventQueryParams params, List<String> fixedColumns )
    {
        List<String> cols = ListUtils.distinctUnion( fixedColumns, getAggregateColumns( params ), getPartitionSelectColumns( params ) );
        
        String selectCols = StringUtils.join( cols, "," );
        
        String sql = "from (";
        
        for ( String partition : params.getPartitions().getPartitions() )
        {
            sql += "select " + selectCols + " ";
            
            sql += getFromWhereSinglePartitionClause( params, partition );
            
            sql += "union all ";
        }

        sql = trimEnd( sql, "union all ".length() ) + ") as data ";
        
        return sql;
    }

    /**
     * Returns a from and where SQL clause for the given analytics table 
     * partition.
     * 
     * @param params the event query parameters.
     * @param partition the partition name.
     */
    private String getFromWhereSinglePartitionClause( EventQueryParams params, String partition )
    {
        String sql = "from " + partition + " ";

        // ---------------------------------------------------------------------
        // Periods
        // ---------------------------------------------------------------------

        if ( params.hasStartEndDate() )
        {        
            sql += "where executiondate >= '" + getMediumDateString( params.getStartDate() ) + "' ";
            sql += "and executiondate <= '" + getMediumDateString( params.getEndDate() ) + "' ";
        }
        else // Periods
        {
            sql += "where " + params.getPeriodType() + " in (" + getQuotedCommaDelimitedString( getUids( params.getDimensionOrFilterItems( PERIOD_DIM_ID ) ) ) + ") ";
        }

        // ---------------------------------------------------------------------
        // Organisation units
        // ---------------------------------------------------------------------

        if ( params.isOrganisationUnitMode( OrganisationUnitSelectionMode.SELECTED ) )
        {
            sql += "and ou in (" + getQuotedCommaDelimitedString( getUids( params.getDimensionOrFilterItems( ORGUNIT_DIM_ID ) ) ) + ") ";
        }
        else if ( params.isOrganisationUnitMode( OrganisationUnitSelectionMode.CHILDREN ) )
        {
            sql += "and ou in (" + getQuotedCommaDelimitedString( getUids( params.getOrganisationUnitChildren() ) ) + ") ";
        }
        else // Descendants
        {
            sql += "and (";
            
            for ( DimensionalItemObject object : params.getDimensionOrFilterItems( ORGUNIT_DIM_ID ) )
            {
                OrganisationUnit unit = (OrganisationUnit) object;
                sql += "uidlevel" + unit.getLevel() + " = '" + unit.getUid() + "' or ";
            }
            
            sql = removeLastOr( sql ) + ") ";
        }

        // ---------------------------------------------------------------------
        // Organisation unit group sets
        // ---------------------------------------------------------------------

        for ( DimensionalObject dim : params.getDimensionsAndFilters( DimensionType.ORGANISATION_UNIT_GROUP_SET ) )
        {            
            String col = statementBuilder.columnQuote( dim.getDimensionName() );
                
            sql += "and " + col + " in (" + getQuotedCommaDelimitedString( getUids( dim.getItems() ) ) + ") ";
        }

        // ---------------------------------------------------------------------
        // Program stage
        // ---------------------------------------------------------------------

        if ( params.hasProgramStage() )
        {
            sql += "and ps = '" + params.getProgramStage().getUid() + "' ";
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
                    sql += "and " + getColumn( item ) + " " + filter.getSqlOperator() + " " + getSqlFilter( filter, item ) + " ";
                }
            }
        }
        
        for ( QueryItem item : params.getItemFilters() )
        {
            if ( item.hasFilter() )
            {
                for ( QueryFilter filter : item.getFilters() )
                {
                    sql += "and " + getColumn( item ) + " " + filter.getSqlOperator() + " " + getSqlFilter( filter, item ) + " ";
                }
            }
        }

        // ---------------------------------------------------------------------
        // Filter expression
        // ---------------------------------------------------------------------

        if ( params.hasProgramIndicatorDimension() && params.getProgramIndicator().hasFilter() )
        {
            String filter = programIndicatorService.getAnalyticsSQl( params.getProgramIndicator().getFilter(), false );
            
            String sqlFilter = ExpressionUtils.asSql( filter );
            
            sql += "and (" + sqlFilter + ") ";
        }
        
        if ( params.hasProgramIndicatorDimension() )
        {
            String anyValueFilter = programIndicatorService.getAnyValueExistsClauseAnalyticsSql( params.getProgramIndicator().getExpression() );
            
            if ( anyValueFilter != null )
            {
                sql += "and (" + anyValueFilter + ") ";
            }
        }
        
        // ---------------------------------------------------------------------
        // Various filters
        // ---------------------------------------------------------------------

        if ( params.isCoordinatesOnly() )
        {
            sql += "and (longitude is not null and latitude is not null) ";
        }
        
        if ( params.isGeometryOnly() )
        {
            sql += "and geom is not null ";
        }
        
        if ( params.isCompletedOnly() )
        {
            sql += "and completeddate is not null ";
        }
        
        if ( params.hasBbox() )
        {
            sql += "and geom && ST_MakeEnvelope(" + params.getBbox() + ",4326)";
        }

        return sql;
    }
    
    /**
     * Returns an encoded column name wrapped in lower directive if not numeric
     * or boolean.
     */
    private String getColumn( QueryItem item )
    {
        String col = statementBuilder.columnQuote( item.getItemName() );
        
        return item.isText() ? "lower(" + col + ")" : col;
    }
    
    /**
     * Returns the filter value for the given query item.
     */
    private String getSqlFilter( QueryFilter filter, QueryItem item )
    {
        String encodedFilter = statementBuilder.encode( filter.getFilter(), false );
        
        String sqlFilter = filter.getSqlFilter( encodedFilter );
        
        return item.isText() ? sqlFilter.toLowerCase() : sqlFilter;
    }

    /**
     * Returns an item value for the given query, query item and value. Assumes that
     * data dimensions are collapsed for the given query. Returns the short name
     * of the given query item followed by the item value. If the given query item
     * has a legend set, the item value is treated as an id and substituted with
     * the matching legend name. If the given query item has an option set, the 
     * item value is treated as a code and substituted with the matching option 
     * name.
     */
    private String getCollapsedDataItemValue( EventQueryParams params, QueryItem item, String itemValue )
    {
        String value = item.getItem().getDisplayShortName() + ITEM_NAME_SEP;
        
        Legend legend = null;
        Option option = null;
        
        if ( item.hasLegendSet() && ( legend = item.getLegendSet().getLegendByUid( itemValue ) ) != null )
        {
            return value + legend.getDisplayName();
        }        
        else if ( item.hasOptionSet() && ( option = item.getOptionSet().getOptionByCode( itemValue ) ) != null )
        {
            return value + option.getDisplayName();
        }
        else
        {
            itemValue = StringUtils.defaultString( itemValue, NA );
            
            return value + itemValue;
        }
    }
}
