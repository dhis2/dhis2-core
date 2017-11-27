package org.hisp.dhis.analytics.data;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.AnalyticsManager;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.MeasureFilter;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import static org.hisp.dhis.analytics.AnalyticsAggregationType.AVERAGE_BOOL;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.AVERAGE_INT;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.AVERAGE_INT_DISAGGREGATION;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.AVERAGE_SUM_INT;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.COUNT;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.MAX;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.MIN;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.NONE;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.STDDEV;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.VARIANCE;
import static org.hisp.dhis.analytics.DataQueryParams.LEVEL_PREFIX;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.analytics.DataType.TEXT;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.removeLastOr;
import static org.hisp.dhis.system.util.DateUtils.getMediumDateString;

/**
 * This class is responsible for producing aggregated data values. It reads data
 * from the analytics table.
 *
 * @author Lars Helge Overland
 */
public class JdbcAnalyticsManager
    implements AnalyticsManager
{
    private static final Log log = LogFactory.getLog( JdbcAnalyticsManager.class );

    private static final String COL_APPROVALLEVEL = "approvallevel";

    private static final Map<MeasureFilter, String> OPERATOR_SQL_MAP = ImmutableMap.<MeasureFilter, String>builder()
        .put( MeasureFilter.EQ, "=" )
        .put( MeasureFilter.GT, ">" )
        .put( MeasureFilter.GE, ">=" )
        .put( MeasureFilter.LT, "<" )
        .put( MeasureFilter.LE, "<=" )
        .build();
    
    @Resource( name = "readOnlyJdbcTemplate" )
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StatementBuilder statementBuilder;

    // -------------------------------------------------------------------------
    // AnalyticsManager implementation
    // -------------------------------------------------------------------------

    @Override
    @Async
    public Future<Map<String, Object>> getAggregatedDataValues( DataQueryParams params, int maxLimit )
    {
        try
        {
            ListMap<DimensionalItemObject, DimensionalItemObject> dataPeriodAggregationPeriodMap = 
                params.getDataPeriodAggregationPeriodMap();

            if ( params.isDisaggregation() && params.hasDataPeriodType() )
            {
                params = DataQueryParams.newBuilder( params )
                    .withDataPeriodsForAggregationPeriods( dataPeriodAggregationPeriodMap )
                    .build();
            }

            String sql = getSelectClause( params );

            sql += getFromClause( params );
            
            sql += getWhereClause( params );

            sql += getGroupByClause( params );

            if ( params.isDataType( DataType.NUMERIC ) && !params.getMeasureCriteria().isEmpty() )
            {
                sql += getMeasureCriteriaSql( params );
            }

            log.debug( sql );

            Map<String, Object> map = null;

            try
            {
                map = getKeyValueMap( params, sql, maxLimit );
            }
            catch ( BadSqlGrammarException ex )
            {
                log.info( "Query failed, likely because the requested analytics table does not exist", ex );
                return new AsyncResult<>( Maps.newHashMap() );
            }

            replaceDataPeriodsWithAggregationPeriods( map, params, dataPeriodAggregationPeriodMap );

            return new AsyncResult<>( map );
        }
        catch ( RuntimeException ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            throw ex;
        }
    }

    @Override
    public void replaceDataPeriodsWithAggregationPeriods( Map<String, Object> dataValueMap, 
        DataQueryParams params, ListMap<DimensionalItemObject, DimensionalItemObject> dataPeriodAggregationPeriodMap )
    {
        if ( params.isDisaggregation() )
        {
            int periodIndex = params.getPeriodDimensionIndex();

            if ( periodIndex == -1 )
            {
                return; // Period is filter, nothing to replace
            }

            Set<String> keys = new HashSet<>( dataValueMap.keySet() );
            
            for ( String key : keys )
            {
                String[] keyArray = key.split( DIMENSION_SEP );
                
                String periodKey = keyArray[periodIndex];

                Assert.notNull( periodKey, "Period key cannot be null" );

                List<DimensionalItemObject> periods = dataPeriodAggregationPeriodMap.get( PeriodType.getPeriodFromIsoString( periodKey ) );

                Assert.notNull( periods, dataPeriodAggregationPeriodMap.toString() );

                Object value = dataValueMap.get( key );

                for ( DimensionalItemObject period : periods )
                {
                    String[] keyCopy = keyArray.clone();
                    keyCopy[periodIndex] = ((Period) period).getIsoDate();
                    dataValueMap.put( TextUtils.toString( keyCopy, DIMENSION_SEP ), value );
                }

                dataValueMap.remove( key );
            }
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Generates the select clause of the query SQL.
     */
    private String getSelectClause( DataQueryParams params )
    {
        String sql = "select " + getCommaDelimitedQuotedColumns( params.getDimensions() ) + ", ";

        if ( params.isDataType( TEXT ) )
        {
            sql += "textvalue";
        }
        else // NUMERIC
        {
            sql += getNumericValueColumn( params );
        }

        sql += " as value ";

        return sql;
    }

    /**
     * Returns a aggregate clause for the numeric value column.
     */
    private String getNumericValueColumn( DataQueryParams params )
    {
        String sql = "";

        if ( params.isAggregationType( AVERAGE_SUM_INT ) )
        {
            sql = "sum(daysxvalue) / " + params.getDaysForAvgSumIntAggregation();
        }
        else if ( params.isAggregationType( AVERAGE_INT ) || params.isAggregationType( AVERAGE_INT_DISAGGREGATION ) )
        {
            sql = "avg(value)";
        }
        else if ( params.isAggregationType( AVERAGE_BOOL ) )
        {
            sql = "sum(daysxvalue) / sum(daysno) * 100";
        }
        else if ( params.isAggregationType( COUNT ) )
        {
            sql = "count(value)";
        }
        else if ( params.isAggregationType( STDDEV ) )
        {
            sql = "stddev(value)";
        }
        else if ( params.isAggregationType( VARIANCE ) )
        {
            sql = "variance(value)";
        }
        else if ( params.isAggregationType( MIN ) )
        {
            sql = "min(value)";
        }
        else if ( params.isAggregationType( MAX ) )
        {
            sql = "max(value)";
        }
        else if ( params.isAggregationType( NONE ) )
        {
            sql = "value";
        }
        else // SUM, AVERAGE_SUM_INT_DISAGGREGATION and null
        {
            sql = "sum(value)";
        }

        return sql;
    }

    /**
     * Generates the from clause of the query SQL.
     */
    private String getFromClause( DataQueryParams params )
    {
        String sql = "from ";
        
        if ( params.hasPreAggregateMeasureCriteria() && params.isDataType( DataType.NUMERIC ) )
        {
            sql += getPreMeasureCriteriaSubquerySql( params );
        }
        else
        {
            sql += params.getTableName();
        }
        
        return sql + " ";
    }

    /**
     * Generates the where clause of the query SQL.
     */
    private String getWhereClause( DataQueryParams params )
    {
        SqlHelper sqlHelper = new SqlHelper();
        
        String sql = "";

        // ---------------------------------------------------------------------
        // Dimensions
        // ---------------------------------------------------------------------

        for ( DimensionalObject dim : params.getDimensions() )
        {
            if ( !dim.getItems().isEmpty() && !dim.isFixed() )
            {
                String col = statementBuilder.columnQuote( dim.getDimensionName() );

                sql += sqlHelper.whereAnd() + " " + col + " in (" + getQuotedCommaDelimitedString( getUids( dim.getItems() ) ) + ") ";
            }
        }

        // ---------------------------------------------------------------------
        // Filters
        // ---------------------------------------------------------------------

        ListMap<String, DimensionalObject> filterMap = params.getDimensionFilterMap();

        for ( String dimension : filterMap.keySet() )
        {
            List<DimensionalObject> filters = filterMap.get( dimension );

            if ( DimensionalObjectUtils.anyDimensionHasItems( filters ) )
            {
                sql += sqlHelper.whereAnd() + " ( ";

                for ( DimensionalObject filter : filters )
                {
                    if ( filter.hasItems() )
                    {
                        String col = statementBuilder.columnQuote( filter.getDimensionName() );

                        sql += col + " in (" + getQuotedCommaDelimitedString( getUids( filter.getItems() ) ) + ") or ";
                    }
                }

                sql = removeLastOr( sql ) + ") ";
            }
        }

        // ---------------------------------------------------------------------
        // Data approval
        // ---------------------------------------------------------------------

        if ( params.isDataApproval() )
        {
            sql += sqlHelper.whereAnd() + " ( ";

            for ( OrganisationUnit unit : params.getDataApprovalLevels().keySet() )
            {
                String ouCol = LEVEL_PREFIX + unit.getLevel();
                Integer level = params.getDataApprovalLevels().get( unit );

                sql += "(" + ouCol + " = '" + unit.getUid() + "' and " + COL_APPROVALLEVEL + " <= " + level + ") or ";
            }

            sql = removeLastOr( sql ) + ") ";
        }

        // ---------------------------------------------------------------------
        // Restrictions
        // ---------------------------------------------------------------------
        
        if ( params.isRestrictByOrgUnitOpeningClosedDate() && params.hasStartEndDate() )
        {
            sql += sqlHelper.whereAnd() + " (" +
                "(" + statementBuilder.columnQuote( "ouopeningdate") + " <= '" + getMediumDateString( params.getStartDate() ) + "' or " + statementBuilder.columnQuote( "ouopeningdate" ) + " is null) and " +
                "(" + statementBuilder.columnQuote( "oucloseddate" ) + " >= '" + getMediumDateString( params.getEndDate() ) + "' or " + statementBuilder.columnQuote( "oucloseddate" ) + " is null)) ";
        }
        
        if ( params.isRestrictByCategoryOptionStartEndDate() && params.hasStartEndDate() )
        {
            sql += sqlHelper.whereAnd() + " (" +
                "(" + statementBuilder.columnQuote( "costartdate" ) + " <= '" + getMediumDateString( params.getStartDate() ) + "' or " + statementBuilder.columnQuote( "costartdate" ) + " is null) and " +
                "(" + statementBuilder.columnQuote( "coenddate" ) + " >= '" + getMediumDateString( params.getEndDate() ) + "' or " + statementBuilder.columnQuote( "coenddate" ) +  " is null)) ";
        }

        if ( !params.isRestrictByOrgUnitOpeningClosedDate() && !params.isRestrictByCategoryOptionStartEndDate() && params.hasStartEndDate() )
        {
            sql += sqlHelper.whereAnd() + " " + statementBuilder.columnQuote( "pestartdate" ) + "  >= '" + getMediumDateString( params.getStartDate() ) + "' ";
            sql += "and " + statementBuilder.columnQuote( "peenddate" ) + " <= '" + getMediumDateString( params.getEndDate() ) + "' ";
        }

        if ( params.isTimely() )
        {
            sql += sqlHelper.whereAnd() + " timely is true ";
        }

        // ---------------------------------------------------------------------
        // Partitions restriction to allow constraint exclusion
        // ---------------------------------------------------------------------
        
        if ( !params.isSkipPartitioning() && params.hasPartitions() )
        {            
            sql += sqlHelper.whereAnd() + " " + statementBuilder.columnQuote( "yearly" ) + " in (" + 
                TextUtils.getQuotedCommaDelimitedString( params.getPartitions().getPartitions() ) + ") ";
        }
        
        return sql;
    }

    /**
     * Generates the group by clause of the query SQL.
     */
    private String getGroupByClause( DataQueryParams params )
    {
        String sql = "";

        if ( params.isAggregation() )
        {
            sql = "group by " + getCommaDelimitedQuotedColumns( params.getDimensions() );
        }

        return sql;
    }
    
    /**
     * Generates a query which provides a filtered view of the data according 
     * to the criteria. If not, returns the full view of the partition.
     */
    private String getPreMeasureCriteriaSubquerySql( DataQueryParams params )
    {
        SqlHelper sqlHelper = new SqlHelper();

        String sql = "(select * from " + params.getTableName() + " ";

        for ( MeasureFilter filter : params.getPreAggregateMeasureCriteria().keySet() )
        {
            Double criterion = params.getPreAggregateMeasureCriteria().get( filter );

            sql += sqlHelper.whereAnd() + " value " + OPERATOR_SQL_MAP.get( filter ) + " " + criterion + " ";

        }

        sql += ") as " + params.getTableName();

        return sql;
    }

    /**
     * Returns a HAVING clause restricting the result based on the measure criteria.
     */
    private String getMeasureCriteriaSql( DataQueryParams params )
    {
        SqlHelper sqlHelper = new SqlHelper();
        
        String sql = " ";

        for ( MeasureFilter filter : params.getMeasureCriteria().keySet() )
        {
            Double criterion = params.getMeasureCriteria().get( filter );

            sql += sqlHelper.havingAnd() + " " + getNumericValueColumn( params ) + " " + OPERATOR_SQL_MAP.get( filter ) + " " + criterion + " ";
        }

        return sql;
    }

    /**
     * Retrieves data from the database based on the given query and SQL and puts
     * into a value key and value mapping.
     */
    private Map<String, Object> getKeyValueMap( DataQueryParams params, String sql, int maxLimit )
    {
        Map<String, Object> map = new HashMap<>();

        log.debug( String.format( "Analytics SQL: %s", sql ) );

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        int counter = 0;
        
        while ( rowSet.next() )
        {
            if ( maxLimit > 0 && ++counter > maxLimit )
            {
                throw new IllegalQueryException( "Query result set exceeds max limit: " + maxLimit );
            }
            
            StringBuilder key = new StringBuilder();

            for ( DimensionalObject dim : params.getDimensions() )
            {
                String value = dim.isFixed() ? dim.getDimensionName() : rowSet.getString( dim.getDimensionName() );
                
                key.append( value ).append( DIMENSION_SEP );
            }

            key.deleteCharAt( key.length() - 1 );

            if ( params.isDataType( TEXT ) )
            {
                String value = rowSet.getString( VALUE_ID );

                map.put( key.toString(), value );
            }
            else // NUMERIC
            {
                Double value = rowSet.getDouble( VALUE_ID );

                map.put( key.toString(), value );
            }
        }

        return map;
    }

    /**
     * Generates a comma-delimited string based on the dimension names of the
     * given dimensions where each dimension name is quoted.
     */
    private String getCommaDelimitedQuotedColumns( Collection<DimensionalObject> dimensions )
    {
        final StringBuilder builder = new StringBuilder();

        if ( dimensions != null && !dimensions.isEmpty() )
        {
            for ( DimensionalObject dimension : dimensions )
            {
                if ( !dimension.isFixed() )
                {
                    builder.append( statementBuilder.columnQuote( dimension.getDimensionName() ) ).append( "," );
                }
            }

            return builder.substring( 0, builder.length() - 1 );
        }

        return builder.toString();
    }
}
