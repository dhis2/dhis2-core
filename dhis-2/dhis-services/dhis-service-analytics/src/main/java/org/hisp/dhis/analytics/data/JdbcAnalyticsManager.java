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
package org.hisp.dhis.analytics.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.time.DateUtils.addYears;
import static org.hisp.dhis.analytics.AggregationType.AVERAGE;
import static org.hisp.dhis.analytics.AggregationType.COUNT;
import static org.hisp.dhis.analytics.AggregationType.MAX;
import static org.hisp.dhis.analytics.AggregationType.MIN;
import static org.hisp.dhis.analytics.AggregationType.NONE;
import static org.hisp.dhis.analytics.AggregationType.STDDEV;
import static org.hisp.dhis.analytics.AggregationType.SUM;
import static org.hisp.dhis.analytics.AggregationType.VARIANCE;
import static org.hisp.dhis.analytics.DataQueryParams.LEVEL_PREFIX;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.analytics.DataType.TEXT;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quoteAlias;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.removeLastOr;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsManager;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.MeasureFilter;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.table.PartitionUtils;
import org.hisp.dhis.analytics.util.AnalyticsSqlUtils;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.QueryRuntimeException;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * This class is responsible for producing aggregated data values. It reads data
 * from the analytics table.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Component( "org.hisp.dhis.analytics.AnalyticsManager" )
public class JdbcAnalyticsManager
    implements AnalyticsManager
{
    private static final String COL_APPROVALLEVEL = "approvallevel";

    private static final int LAST_VALUE_YEARS_OFFSET = -10;

    private static final Map<MeasureFilter, String> OPERATOR_SQL_MAP = ImmutableMap.<MeasureFilter, String> builder()
        .put( MeasureFilter.EQ, "=" )
        .put( MeasureFilter.GT, ">" )
        .put( MeasureFilter.GE, ">=" )
        .put( MeasureFilter.LT, "<" )
        .put( MeasureFilter.LE, "<=" )
        .build();

    private final QueryPlanner queryPlanner;

    private final JdbcTemplate jdbcTemplate;

    private final ExecutionPlanStore executionPlanStore;

    public JdbcAnalyticsManager( QueryPlanner queryPlanner,
        @Qualifier( "readOnlyJdbcTemplate" ) JdbcTemplate jdbcTemplate, ExecutionPlanStore executionPlanStore )
    {
        checkNotNull( queryPlanner );
        checkNotNull( jdbcTemplate );
        checkNotNull( executionPlanStore );

        this.queryPlanner = queryPlanner;
        this.jdbcTemplate = jdbcTemplate;
        this.executionPlanStore = executionPlanStore;
    }

    // -------------------------------------------------------------------------
    // AnalyticsManager implementation
    // -------------------------------------------------------------------------

    @Override
    @Async
    public Future<Map<String, Object>> getAggregatedDataValues( DataQueryParams params, AnalyticsTableType tableType,
        int maxLimit )
    {
        assertQuery( params );

        try
        {
            ListMap<DimensionalItemObject, DimensionalItemObject> dataPeriodAggregationPeriodMap = params
                .getDataPeriodAggregationPeriodMap();

            if ( params.isDisaggregation() && params.hasDataPeriodType() )
            {
                params = DataQueryParams.newBuilder( params )
                    .withDataPeriodsForAggregationPeriods( dataPeriodAggregationPeriodMap )
                    .build();

                params = queryPlanner.assignPartitionsFromQueryPeriods( params, tableType );
            }

            String sql = getSelectClause( params );

            sql += getFromClause( params );

            sql += getWhereClause( params, tableType );

            sql += getGroupByClause( params );

            if ( params.hasMeasureCriteria() && params.isDataType( DataType.NUMERIC ) )
            {
                sql += getMeasureCriteriaSql( params );
            }

            log.debug( sql );

            if ( params.analyzeOnly() )
            {
                executionPlanStore.addExecutionPlan( params.getExplainOrderId(), sql );
                return new AsyncResult<>( Maps.newHashMap() );
            }

            Map<String, Object> map;

            try
            {
                map = getKeyValueMap( params, sql, maxLimit );
            }
            catch ( BadSqlGrammarException ex )
            {
                log.info( AnalyticsUtils.ERR_MSG_TABLE_NOT_EXISTING, ex );
                return new AsyncResult<>( Maps.newHashMap() );
            }

            replaceDataPeriodsWithAggregationPeriods( map, params, dataPeriodAggregationPeriodMap );

            return new AsyncResult<>( map );
        }
        catch ( DataAccessResourceFailureException ex )
        {
            log.warn( ErrorCode.E7131.getMessage(), ex );
            throw new QueryRuntimeException( ErrorCode.E7131 );
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

                Assert.notNull( periodKey, String.format( "Period key cannot be null, key: '%s'", key ) );

                List<DimensionalItemObject> periods = dataPeriodAggregationPeriodMap
                    .get( PeriodType.getPeriodFromIsoString( periodKey ) );

                Assert.notNull( periods, String.format( "Period list cannot be null, key: '%s', map: '%s'", key,
                    dataPeriodAggregationPeriodMap.toString() ) );

                Object value = dataValueMap.get( key );

                for ( DimensionalItemObject period : periods )
                {
                    String[] keyCopy = keyArray.clone();

                    keyCopy[periodIndex] = ((Period) period).getIsoDate();

                    String replacementKey = TextUtils.toString( keyCopy, DIMENSION_SEP );

                    if ( dataValueMap.containsKey( replacementKey )
                        && ((Period) period).getPeriodType().spansMultipleCalendarYears() )
                    {
                        Object weightedAverage = AnalyticsUtils.calculateYearlyWeightedAverage(
                            (Double) dataValueMap.get( replacementKey ), (Double) value,
                            AnalyticsUtils.getBaseMonth( ((Period) period).getPeriodType() ) );

                        dataValueMap.put( TextUtils.toString( keyCopy, DIMENSION_SEP ), weightedAverage );
                    }
                    else
                    {
                        dataValueMap.put( TextUtils.toString( keyCopy, DIMENSION_SEP ), value );
                    }
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
     *
     * @param params the {@link DataQueryParams}.
     * @return a SQL select clause.
     */
    private String getSelectClause( DataQueryParams params )
    {
        String sql = "select " + getCommaDelimitedQuotedColumns( params.getDimensions() ) + ", ";

        if ( params.isDataType( TEXT ) )
        {
            sql += params.getValueColumn();
        }
        else // NUMERIC and BOOLEAN
        {
            sql += getNumericValueColumn( params );
        }

        sql += " as value ";

        return sql;
    }

    /**
     * Returns a aggregate clause for the numeric value column.
     *
     * @param params the {@link DataQueryParams}.
     * @return a SQL numeric value column.
     */
    private String getNumericValueColumn( DataQueryParams params )
    {
        String sql;

        AnalyticsAggregationType aggType = params.getAggregationType();

        String valueColumn = params.getValueColumn();

        if ( aggType.isAggregationType( SUM ) && aggType.isPeriodAggregationType( AVERAGE )
            && aggType.isNumericDataType() )
        {
            sql = "sum(daysxvalue) / " + params.getDaysForAvgSumIntAggregation();
        }
        else if ( aggType.isAggregationType( AVERAGE ) && aggType.isNumericDataType() )
        {
            sql = "avg(" + valueColumn + ")";
        }
        else if ( aggType.isAggregationType( AVERAGE ) && aggType.isBooleanDataType() )
        {
            sql = "sum(daysxvalue) / sum(daysno) * 100";
        }
        else if ( aggType.isAggregationType( COUNT ) )
        {
            sql = "count(" + valueColumn + ")";
        }
        else if ( aggType.isAggregationType( STDDEV ) )
        {
            sql = "stddev(" + valueColumn + ")";
        }
        else if ( aggType.isAggregationType( VARIANCE ) )
        {
            sql = "variance(" + valueColumn + ")";
        }
        else if ( aggType.isAggregationType( MIN ) )
        {
            sql = "min(" + valueColumn + ")";
        }
        else if ( aggType.isAggregationType( MAX ) )
        {
            sql = "max(" + valueColumn + ")";
        }
        else if ( aggType.isAggregationType( NONE ) )
        {
            sql = valueColumn;
        }
        else // SUM and no value
        {
            sql = "sum(" + valueColumn + ")";
        }

        return sql;
    }

    /**
     * Generates the from clause of the query SQL.
     *
     * @param params the {@link DataQueryParams}.
     * @return a SQL from clause.
     */
    private String getFromClause( DataQueryParams params )
    {
        String sql = "from ";

        if ( params.getAggregationType().isFirstOrLastPeriodAggregationType() )
        {
            Date earliest = addYears( params.getLatestEndDate(), LAST_VALUE_YEARS_OFFSET );
            sql += getFirstOrLastValueSubquerySql( params, earliest );
        }
        else if ( params.hasPreAggregateMeasureCriteria() && params.isDataType( DataType.NUMERIC ) )
        {
            sql += getPreMeasureCriteriaSubquerySql( params );
        }
        else if ( params.getAggregationType().isLastInPeriodAggregationType() )
        {
            sql += getFirstOrLastValueSubquerySql( params, params.getEarliestStartDate() );
        }
        else
        {
            sql += getFromSourceClause( params );
        }

        return sql + " as " + ANALYTICS_TBL_ALIAS + " ";
    }

    /**
     * Returns the query from source clause. Can be any of table name, partition
     * name or inner select union all query.
     *
     * @param params the {@link DataQueryParams}.
     * @return a SQL from source clause.
     */
    private String getFromSourceClause( DataQueryParams params )
    {
        if ( !params.isSkipPartitioning() && params.hasPartitions() && params.getPartitions().hasOne() )
        {
            Integer partition = params.getPartitions().getAny();

            return PartitionUtils.getPartitionName( params.getTableName(), partition );
        }
        else if ( (!params.isSkipPartitioning() && params.hasPartitions() && params.getPartitions().hasMultiple()) )
        {
            String sql = "(";

            for ( Integer partition : params.getPartitions().getPartitions() )
            {
                String partitionName = PartitionUtils.getPartitionName( params.getTableName(), partition );

                sql += "select ap.* from " + partitionName + " as ap union all ";
            }

            return TextUtils.removeLast( sql, "union all" ) + ")";
        }
        else
        {
            return params.getTableName();
        }
    }

    /**
     * Generates the where clause of the query SQL.
     *
     * @param params the {@link DataQueryParams}.
     * @return a SQL where clause.
     */
    private String getWhereClause( DataQueryParams params, AnalyticsTableType tableType )
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
                String col = quoteAlias( dim.getDimensionName() );

                sql += sqlHelper.whereAnd() + " " + col + " in ("
                    + getQuotedCommaDelimitedString( getUids( dim.getItems() ) ) + ") ";
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
                        String col = quoteAlias( filter.getDimensionName() );

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
                String ouCol = quoteAlias( LEVEL_PREFIX + unit.getLevel() );
                Integer level = params.getDataApprovalLevels().get( unit );

                sql += "(" + ouCol + " = '" + unit.getUid() + "' and " +
                    quoteAlias( COL_APPROVALLEVEL ) + " <= " + level + ") or ";
            }

            sql = removeLastOr( sql ) + ") ";
        }

        // ---------------------------------------------------------------------
        // Restrictions
        // ---------------------------------------------------------------------

        if ( params.isRestrictByOrgUnitOpeningClosedDate() && params.hasStartEndDateRestriction() )
        {
            sql += sqlHelper.whereAnd() + " (" +
                "(" + quoteAlias( "ouopeningdate" ) + " <= '" + getMediumDateString( params.getStartDateRestriction() )
                + "' or " + quoteAlias( "ouopeningdate" ) + " is null) and " +
                "(" + quoteAlias( "oucloseddate" ) + " >= '" + getMediumDateString( params.getEndDateRestriction() )
                + "' or " + quoteAlias( "oucloseddate" ) + " is null)) ";
        }

        if ( params.isRestrictByCategoryOptionStartEndDate() && params.hasStartEndDateRestriction() )
        {
            sql += sqlHelper.whereAnd() + " (" +
                "(" + quoteAlias( "costartdate" ) + " <= '" + getMediumDateString( params.getStartDateRestriction() )
                + "' or " + quoteAlias( "costartdate" ) + " is null) and " +
                "(" + quoteAlias( "coenddate" ) + " >= '" + getMediumDateString( params.getEndDateRestriction() )
                + "' or " + quoteAlias( "coenddate" ) + " is null)) ";
        }

        if ( tableType.hasPeriodDimension() && params.hasStartDate() )
        {
            sql += sqlHelper.whereAnd() + " " +
                quoteAlias( "pestartdate" ) + "  >= '" + getMediumDateString( params.getStartDate() ) + "' ";
        }

        if ( tableType.hasPeriodDimension() && params.hasEndDate() )
        {
            sql += sqlHelper.whereAnd() + " " +
                quoteAlias( "peenddate" ) + " <= '" + getMediumDateString( params.getEndDate() ) + "' ";
        }

        if ( params.isTimely() )
        {
            sql += sqlHelper.whereAnd() + " " + quoteAlias( "timely" ) + " is true ";
        }

        // ---------------------------------------------------------------------
        // Partitions restriction to allow constraint exclusion
        // ---------------------------------------------------------------------

        if ( !params.isSkipPartitioning() && params.hasPartitions() )
        {
            sql += sqlHelper.whereAnd() + " " + quoteAlias( "year" ) + " in (" +
                TextUtils.getCommaDelimitedString( params.getPartitions().getPartitions() ) + ") ";
        }

        // ---------------------------------------------------------------------
        // Period rank restriction to get last value only
        // ---------------------------------------------------------------------

        if ( params.getAggregationType().isFirstOrLastOrLastInPeriodAggregationType() )
        {
            sql += sqlHelper.whereAnd() + " " + quoteAlias( "pe_rank" ) + " = 1 ";
        }

        return sql;
    }

    /**
     * Generates the group by clause of the query SQL.
     *
     * @param params the {@link DataQueryParams}.
     * @return a SQL group by clause.
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
     * Generates a sub query which provides a view of the data where each row is
     * ranked by the start date, then end date of the data value period, latest
     * first. The data is partitioned by data element, org unit, category option
     * combo and attribute option combo. A column {@code pe_rank} defines the
     * rank. Only data for the last 10 years relative to the period end date is
     * included.
     *
     * @param params the {@link DataQueryParams}.
     * @param earliestDate the earliest date for which data is considered.
     * @return a SQL first or last value sub query.
     */
    private String getFirstOrLastValueSubquerySql( DataQueryParams params, Date earliestDate )
    {
        Date latest = params.getLatestEndDate();
        List<String> columns = getFirstOrLastValueSubqueryQuotedColumns( params );
        String fromSourceClause = getFromSourceClause( params ) + " as " + ANALYTICS_TBL_ALIAS;

        String sql = "(select ";

        for ( String col : columns )
        {
            sql += col + ",";
        }

        String order = params.getAggregationType().isFirstPeriodAggregationType() ? "asc" : "desc";

        sql += "row_number() over (" +
            "partition by dx, ou, co, ao " +
            "order by peenddate " + order + ", pestartdate " + order + ") as pe_rank " +
            "from " + fromSourceClause + " " +
            "where pestartdate >= '" + getMediumDateString( earliestDate ) + "' " +
            "and pestartdate <= '" + getMediumDateString( latest ) + "' " +
            "and (value is not null or textvalue is not null))";

        return sql;
    }

    /**
     * Returns quoted names of columns for the {@link AggregationType#LAST} sub
     * query. It is assumed that {@link AggregationType#LAST} type only applies
     * to aggregate data analytics. The period dimension is replaced by the name
     * of the single period in the given query.
     *
     * @param params the {@link DataQueryParams}.
     * @return a SQL clause with quoted columns for a first or last value query.
     */
    private List<String> getFirstOrLastValueSubqueryQuotedColumns( DataQueryParams params )
    {
        Period period = params.getLatestPeriod();

        List<String> cols = Lists.newArrayList( "year", "pestartdate", "peenddate", "oulevel", "daysxvalue", "daysno",
            "value", "textvalue" );

        cols = cols.stream().map( AnalyticsSqlUtils::quote ).collect( Collectors.toList() );

        if ( params.isDataApproval() )
        {
            cols.add( quote( COL_APPROVALLEVEL ) );

            for ( OrganisationUnit unit : params.getDataApprovalLevels().keySet() )
            {
                cols.add( quote( LEVEL_PREFIX + unit.getLevel() ) );
            }
        }

        for ( DimensionalObject dim : params.getDimensionsAndFilters() )
        {
            if ( DimensionType.PERIOD == dim.getDimensionType() && period != null )
            {
                String alias = quote( dim.getDimensionName() );
                String col = "cast('" + period.getDimensionItem() + "' as text) as " + alias;
                cols.add( col );
            }
            else
            {
                cols.add( quote( dim.getDimensionName() ) );
            }
        }

        return cols;
    }

    /**
     * Generates a sub query which provides a filtered view of the data
     * according to the criteria. If not, returns the full view of the
     * partition.
     *
     * @param params the {@link DataQueryParams}.
     * @return a SQL measure sub query.
     */
    private String getPreMeasureCriteriaSubquerySql( DataQueryParams params )
    {
        SqlHelper sqlHelper = new SqlHelper();

        String fromSourceClause = getFromSourceClause( params ) + " as " + ANALYTICS_TBL_ALIAS;

        String sql = "(select * from " + fromSourceClause + " ";

        for ( MeasureFilter filter : params.getPreAggregateMeasureCriteria().keySet() )
        {
            Double criterion = params.getPreAggregateMeasureCriteria().get( filter );

            sql += sqlHelper.whereAnd() + " value " + OPERATOR_SQL_MAP.get( filter ) + " " + criterion + " ";
        }

        sql += ")";

        return sql;
    }

    /**
     * Returns a having clause restricting the result based on the measure
     * criteria.
     *
     * @param params the {@link DataQueryParams}.
     * @return a SQL measure having clause.
     */
    private String getMeasureCriteriaSql( DataQueryParams params )
    {
        SqlHelper sqlHelper = new SqlHelper();

        String sql = " ";

        for ( MeasureFilter filter : params.getMeasureCriteria().keySet() )
        {
            Double criterion = params.getMeasureCriteria().get( filter );

            sql += sqlHelper.havingAnd() + " " + getNumericValueColumn( params ) + " " + OPERATOR_SQL_MAP.get( filter )
                + " " + criterion + " ";
        }

        return sql;
    }

    /**
     * Retrieves data from the database based on the given query and SQL and
     * puts into a value key and value mapping.
     *
     * @param params the {@link DataQueryParams}.
     * @param sql the SQL query.
     * @param maxLimit the max limit of records to return, 0 indicates
     *        unlimited.
     */
    private Map<String, Object> getKeyValueMap( DataQueryParams params, String sql, int maxLimit )
    {
        Map<String, Object> map = new HashMap<>();

        log.debug( String.format( "Analytics SQL: %s", sql ) );

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        int counter = 0;

        while ( rowSet.next() )
        {
            boolean exceedsMaxLimit = maxLimit > 0 && ++counter > maxLimit;

            if ( exceedsMaxLimit )
            {
                throwIllegalQueryEx( ErrorCode.E7128, maxLimit );
            }

            StringBuilder key = new StringBuilder();

            for ( DimensionalObject dim : params.getDimensions() )
            {
                String value = dim.isFixed() ? dim.getDimensionName() : rowSet.getString( dim.getDimensionName() );

                String queryModsId = params.getQueryModsId( dim );

                key.append( value ).append( queryModsId ).append( DIMENSION_SEP );
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
     * Generates a comma-delimited string with the dimension names of the given
     * dimensions where each dimension name is quoted.
     *
     * @param dimensions the collection of {@link Dimension}.
     * @return a comma-delimited string of quoted dimension names.
     */
    private String getCommaDelimitedQuotedColumns( Collection<DimensionalObject> dimensions )
    {
        StringBuilder builder = new StringBuilder();

        if ( dimensions != null && !dimensions.isEmpty() )
        {
            for ( DimensionalObject dimension : dimensions )
            {
                if ( !dimension.isFixed() )
                {
                    builder.append( quoteAlias( dimension.getDimensionName() ) ).append( "," );
                }
            }

            return builder.substring( 0, builder.length() - 1 );
        }

        return builder.toString();
    }

    /**
     * Makes assertions on the query.
     *
     * @param params the data query parameters.
     */
    private void assertQuery( DataQueryParams params )
    {
        Assert.notNull( params.getDataType(), "Data type must be present" );
        Assert.notNull( params.getAggregationType(), "Aggregation type must be present" );
        Assert.isTrue( !(params.getAggregationType().isFirstOrLastPeriodAggregationType() &&
            params.getPeriods().size() > 1),
            "Max one dimension period can be present per query for last period aggregation" );
    }
}
