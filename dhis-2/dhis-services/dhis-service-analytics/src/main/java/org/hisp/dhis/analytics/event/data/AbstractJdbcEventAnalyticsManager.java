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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.analytics.DataQueryParams.NUMERATOR_DENOMINATOR_PROPERTIES_COUNT;
import static org.hisp.dhis.analytics.SortOrder.ASC;
import static org.hisp.dhis.analytics.SortOrder.DESC;
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.OU_GEOMETRY_COL_SUFFIX;
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.OU_NAME_COL_SUFFIX;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.DATE_PERIOD_STRUCT_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ORG_UNIT_STRUCT_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quoteAlias;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_INDICATOR;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.hisp.dhis.system.util.MathUtils.getRounded;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.InQueryFilter;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryRuntimeException;
import org.hisp.dhis.common.Reference;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

/**
 * @author Markus Bekken
 */
@Slf4j
public abstract class AbstractJdbcEventAnalyticsManager
{
    protected static final String COL_COUNT = "count";

    protected static final String COL_EXTENT = "extent";

    protected static final int COORD_DEC = 6;

    protected static final int LAST_VALUE_YEARS_OFFSET = -10;

    protected final JdbcTemplate jdbcTemplate;

    protected final StatementBuilder statementBuilder;

    protected final ProgramIndicatorService programIndicatorService;

    protected final ProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder;

    protected final ExecutionPlanStore executionPlanStore;

    public AbstractJdbcEventAnalyticsManager( @Qualifier( "readOnlyJdbcTemplate" ) JdbcTemplate jdbcTemplate,
        StatementBuilder statementBuilder, ProgramIndicatorService programIndicatorService,
        ProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder, ExecutionPlanStore executionPlanStore )
    {
        checkNotNull( jdbcTemplate );
        checkNotNull( statementBuilder );
        checkNotNull( programIndicatorService );
        checkNotNull( programIndicatorSubqueryBuilder );
        checkNotNull( executionPlanStore );

        this.jdbcTemplate = jdbcTemplate;
        this.statementBuilder = statementBuilder;
        this.programIndicatorService = programIndicatorService;
        this.programIndicatorSubqueryBuilder = programIndicatorSubqueryBuilder;
        this.executionPlanStore = executionPlanStore;
    }

    /**
     * Returns an SQL paging clause.
     *
     * @param params the {@link EventQueryParams}.
     */
    private String getPagingClause( EventQueryParams params, int maxLimit )
    {
        String sql = "";

        if ( params.isPaging() )
        {
            sql += "limit " + params.getPageSizeWithDefault() + " offset " + params.getOffset();
        }
        else if ( maxLimit > 0 )
        {
            sql += "limit " + (maxLimit + 1);
        }

        return sql;
    }

    /**
     * Returns an SQL sort clause.
     *
     * @param params the {@link EventQueryParams}.
     */
    private String getSortClause( EventQueryParams params )
    {
        String sql = "";

        if ( params.isSorting() )
        {
            sql += "order by " + getSortColumns( params, ASC ) + getSortColumns( params, DESC );

            sql = TextUtils.removeLastComma( sql ) + " ";
        }

        return sql;
    }

    private String getSortColumns( EventQueryParams params, SortOrder order )
    {
        String sql = "";

        for ( QueryItem item : order == ASC ? params.getAsc() : params.getDesc() )
        {
            if ( item.getItem().getDimensionItemType() == PROGRAM_INDICATOR )
            {
                sql += quote( item.getItem().getUid() );
            }
            else if ( item.getItem().getDimensionItemType() == DATA_ELEMENT )
            {
                if ( item.hasRepeatableStageParams() )
                {
                    sql += quote( item.getRepeatableStageParams().getDimension() );
                }
                else if ( item.getProgramStage() != null )
                {
                    sql += quote( item.getProgramStage().getUid() + "." + item.getItem().getUid() );
                }
                else
                {
                    sql += quote( item.getItem().getUid() );
                }
            }
            else
            {
                sql += quoteAlias( item.getItem().getUid() );
            }

            sql += order == ASC ? " asc," : " desc,";
        }

        return sql;
    }

    /**
     * Returns the dynamic select column names to use in a group by clause.
     * Dimensions come first and query items second. Program indicator
     * expressions are converted to SQL expressions. When grouping with
     * non-default analytics period boundaries, all periods are skipped in the
     * group clause, as non default boundaries is defining their own period
     * groups within their where clause.
     */
    private List<String> getGroupByColumnNames( EventQueryParams params )
    {
        return getSelectColumns( params, true );
    }

    /**
     * Returns the dynamic select columns. Dimensions come first and query items
     * second. Program indicator expressions are converted to SQL expressions.
     * In the case of non-default
     * boundaries{@link EventQueryParams#hasNonDefaultBoundaries}, the period is
     * hard coded into the select statement with "(isoPeriod) as (periodType)".
     */
    protected List<String> getSelectColumns( EventQueryParams params )
    {
        return getSelectColumns( params, false );
    }

    /**
     * Returns the dynamic select columns. Dimensions come first and query items
     * second. Program indicator expressions are converted to SQL expressions.
     * In the case of non-default
     * boundaries{@link EventQueryParams#hasNonDefaultBoundaries}, the period is
     * hard coded into the select statement with "(isoPeriod) as (periodType)".
     *
     * @param isGroupByClause used to avoid grouping by period when using
     *        non-default boundaries where the column content would be hard
     *        coded. Used by the group-by calls.
     */
    private List<String> getSelectColumns( EventQueryParams params, boolean isGroupByClause )
    {
        List<String> columns = Lists.newArrayList();

        for ( DimensionalObject dimension : params.getDimensions() )
        {
            if ( isGroupByClause && dimension.getDimensionType() == DimensionType.PERIOD
                && params.hasNonDefaultBoundaries() )
            {
                continue;
            }

            if ( !params.hasNonDefaultBoundaries() || dimension.getDimensionType() != DimensionType.PERIOD )
            {
                String alias = getAlias( params, dimension.getDimensionType() );

                columns.add( quote( alias, dimension.getDimensionName() ) );
            }
            else if ( params.hasSinglePeriod() )
            {
                Period period = (Period) params.getPeriods().get( 0 );
                columns.add( statementBuilder.encode( period.getIsoDate() ) + " as " +
                    period.getPeriodType().getName() );
            }
            else if ( !params.hasPeriods() && params.hasFilterPeriods() )
            {
                // Assuming same period type for all period filters, as the
                // query planner splits into one query per period type

                Period period = (Period) params.getFilterPeriods().get( 0 );
                columns.add( statementBuilder.encode( period.getIsoDate() ) + " as " +
                    period.getPeriodType().getName() );
            }
            else
            {
                throw new IllegalStateException(
                    "Program indicator with non-default boundary expects queries to have exactly one period, or no periods and a period filter" );
            }
        }

        for ( QueryItem queryItem : params.getItems() )
        {
            if ( queryItem.isProgramIndicator() )
            {
                ProgramIndicator in = (ProgramIndicator) queryItem.getItem();

                String asClause = " as " + quote( in.getUid() );

                if ( queryItem.hasRelationshipType() )
                {
                    columns.add( programIndicatorSubqueryBuilder.getAggregateClauseForProgramIndicator( in,
                        queryItem.getRelationshipType(), getAnalyticsType(), params.getEarliestStartDate(),
                        params.getLatestEndDate() ) + asClause );
                }
                else
                {
                    columns.add( programIndicatorSubqueryBuilder.getAggregateClauseForProgramIndicator( in,
                        getAnalyticsType(), params.getEarliestStartDate(), params.getLatestEndDate() ) + asClause );
                }

            }
            else if ( ValueType.COORDINATE == queryItem.getValueType() )
            {
                columns.add( getCoordinateColumn( queryItem ) );
            }
            else if ( ValueType.ORGANISATION_UNIT == queryItem.getValueType() )
            {
                if ( queryItem.getItem().getUid().equals( params.getCoordinateField() ) )
                {
                    columns.add( getCoordinateColumn( queryItem, OU_GEOMETRY_COL_SUFFIX ) );
                }
                else
                {
                    columns.add( getColumn( queryItem, OU_NAME_COL_SUFFIX ) );
                }
            }
            else if ( queryItem.getValueType() == ValueType.NUMBER && !isGroupByClause )
            {
                columns.add( "coalesce(" + getColumn( queryItem ) + ", null) as " + queryItem.getItemName() );
            }
            else
            {
                columns.add( getColumn( queryItem ) );
            }
        }

        return columns;
    }

    public Grid getAggregatedEventData( EventQueryParams params, Grid grid, int maxLimit )
    {
        String countClause = getAggregateClause( params );

        String sql = TextUtils.removeLastComma( "select " + countClause + " as value," +
            StringUtils.join( getSelectColumns( params ), "," ) + " " );

        // ---------------------------------------------------------------------
        // Criteria
        // ---------------------------------------------------------------------

        sql += getFromClause( params );

        sql += getWhereClause( params );

        // ---------------------------------------------------------------------
        // Group by
        // ---------------------------------------------------------------------

        List<String> selectColumnNames = getGroupByColumnNames( params );

        if ( selectColumnNames.size() > 0 )
        {
            sql += "group by " + StringUtils.join( selectColumnNames, "," ) + " ";
        }

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
            sql += "limit " + (maxLimit + 1);
        }

        // ---------------------------------------------------------------------
        // Grid
        // ---------------------------------------------------------------------

        try
        {
            if ( params.analyzeOnly() )
            {
                executionPlanStore.addExecutionPlan( params.getExplainOrderId(), sql );
            }
            else
            {
                getAggregatedEventData( grid, params, sql );
            }
        }
        catch ( BadSqlGrammarException ex )
        {
            log.info( AnalyticsUtils.ERR_MSG_TABLE_NOT_EXISTING, ex );
        }
        catch ( DataAccessResourceFailureException ex )
        {
            log.warn( ErrorCode.E7131.getMessage(), ex );
            throw new QueryRuntimeException( ErrorCode.E7131, ex );
        }

        return grid;
    }

    private void getAggregatedEventData( Grid grid, EventQueryParams params, String sql )
    {
        log.debug( "Analytics enrollment aggregate SQL: " + sql );

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        while ( rowSet.next() )
        {
            grid.addRow();

            if ( params.isAggregateData() )
            {
                if ( params.hasValueDimension() )
                {
                    String itemId = params.getProgram().getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP
                        + params.getValue().getUid();
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

                    String itemName = rowSet.getString( queryItem.getItemName() );
                    String itemValue = params.isCollapseDataDimensions()
                        ? QueryItemHelper.getCollapsedDataItemValue( queryItem, itemName )
                        : itemName;

                    if ( params.getOutputIdScheme() == null || params.getOutputIdScheme() == IdScheme.NAME )
                    {
                        grid.addValue( itemValue );
                    }
                    else
                    {
                        String value = null;

                        String itemOptionValue = QueryItemHelper.getItemOptionValue( itemValue, params );

                        if ( itemOptionValue != null && !itemOptionValue.trim().isEmpty() )
                        {
                            value = itemOptionValue;
                        }
                        else
                        {
                            String legendItemValue = QueryItemHelper.getItemLegendValue( itemValue, params );

                            if ( legendItemValue != null && !legendItemValue.trim().isEmpty() )
                            {
                                value = legendItemValue;
                            }
                        }

                        grid.addValue( value == null ? itemValue : value );
                    }
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
                grid.addNullValues( NUMERATOR_DENOMINATOR_PROPERTIES_COUNT );
            }
        }
    }

    /**
     * Returns the count clause based on value dimension and output type.
     *
     * @param params the {@link EventQueryParams}.
     *
     *        TODO include output type if aggregation type is count
     */
    protected String getAggregateClause( EventQueryParams params )
    {
        EventOutputType outputType = params.getOutputType();

        if ( params.hasValueDimension() ) // TODO && isNumeric
        {
            Assert.isTrue( params.getAggregationTypeFallback().getAggregationType().isAggregatable(),
                "Event query aggregation type must be aggregatable" );

            String function = params.getAggregationTypeFallback().getAggregationType().getValue();

            String expression = quoteAlias( params.getValue().getUid() );

            return function + "(" + expression + ")";
        }
        else if ( params.hasProgramIndicatorDimension() )
        {
            String function = params.getProgramIndicator().getAggregationTypeFallback().getValue();

            function = TextUtils.emptyIfEqual( function, AggregationType.CUSTOM.getValue() );

            String expression = programIndicatorService.getAnalyticsSql( params.getProgramIndicator().getExpression(),
                params.getProgramIndicator(), params.getEarliestStartDate(), params.getLatestEndDate() );

            return function + "(" + expression + ")";
        }
        else
        {
            if ( params.hasEnrollmentProgramIndicatorDimension() )
            {
                if ( EventOutputType.TRACKED_ENTITY_INSTANCE.equals( outputType ) && params.isProgramRegistration() )
                {
                    return "count(distinct tei)";
                }
                else // EVENT
                {
                    return "count(pi)";
                }
            }
            else
            {
                if ( EventOutputType.TRACKED_ENTITY_INSTANCE.equals( outputType ) && params.isProgramRegistration() )
                {
                    return "count(distinct " + quoteAlias( "tei" ) + ")";
                }
                else if ( EventOutputType.ENROLLMENT.equals( outputType ) )
                {
                    if ( params.hasEnrollmentProgramIndicatorDimension() )
                    {
                        return "count(" + quoteAlias( "pi" ) + ")";
                    }
                    return "count(distinct " + quoteAlias( "pi" ) + ")";
                }
                else // EVENT
                {
                    return "count(" + quoteAlias( "psi" ) + ")";
                }
            }
        }
    }

    /**
     * Creates a coordinate base column "selector" for the given item name. The
     * item is expected to be of type Coordinate.
     *
     * @param item the {@link QueryItem}
     * @return the column select statement for the given item
     */
    protected String getCoordinateColumn( final QueryItem item )
    {
        final String colName = quote( item.getItemName() );

        return "'[' || round(ST_X(" + colName + ")::numeric, 6) || ',' || round(ST_Y(" + colName
            + ")::numeric, 6) || ']' as " + colName;
    }

    /**
     * Creates a coordinate base column "selector" for the given item name. The
     * item is expected to be of type Coordinate.
     *
     * @param item the {@link QueryItem}
     * @param suffix the suffix to append to the item id
     * @return the column select statement for the given item
     */
    protected String getCoordinateColumn( final QueryItem item, final String suffix )
    {
        final String colName = quote( item.getItemId() + suffix );

        String stCentroidFunction = "";

        if ( ValueType.ORGANISATION_UNIT == item.getValueType() )
        {
            stCentroidFunction = "ST_Centroid";
        }

        return "'[' || round(ST_X(" + stCentroidFunction + "(" + colName + "))::numeric, 6) || ',' || round(ST_Y("
            + stCentroidFunction + "(" + colName + "))::numeric, 6) || ']' as " + colName;
    }

    /**
     * Creates a column "selector" for the given item name. The suffix will be
     * appended as part of the item name.
     *
     * @param item
     * @return the the column select statement for the given item
     */
    protected String getColumn( final QueryItem item, final String suffix )
    {
        return quote( item.getItemName() + suffix );
    }

    /**
     * Returns an encoded column name
     *
     * @param item the {@link QueryItem}.
     */
    protected String getColumn( QueryItem item )
    {
        return quoteAlias( item.getItemName() );
    }

    /**
     * Returns an SQL to select the expression or column of the item. If the
     * item is a program indicator, the program indicator expression is
     * returned; if the item is a data element, the item column name is
     * returned.
     *
     * @param item the {@link QueryItem}.
     */
    protected String getSelectSql( QueryFilter filter, QueryItem item, Date startDate, Date endDate )
    {
        if ( item.isProgramIndicator() )
        {
            ProgramIndicator programIndicator = (ProgramIndicator) item.getItem();
            return programIndicatorService.getAnalyticsSql( programIndicator.getExpression(), programIndicator,
                startDate, endDate );
        }
        else
        {
            return filter.getSqlFilterColumn( getColumn( item ), item.getValueType() );
        }
    }

    /**
     * Returns the filter value for the given query item.
     *
     * @param filter the {@link QueryFilter}.
     * @param item the {@link QueryItem}.
     */
    protected String getSqlFilter( QueryFilter filter, QueryItem item )
    {
        String encodedFilter = statementBuilder.encode( filter.getFilter(), false );

        return item.getSqlFilter( filter, encodedFilter );
    }

    /**
     * Returns the analytics table alias for the organisation unit dimension.
     *
     * @param params the event query parameters.
     */
    protected String getOrgUnitAlias( EventQueryParams params )
    {
        return params.hasOrgUnitField() ? ORG_UNIT_STRUCT_ALIAS : ANALYTICS_TBL_ALIAS;
    }

    /**
     * Returns the analytics table alias.
     *
     * @param params the event query parameters.
     * @param dimensionType the dimension type.
     */
    private String getAlias( EventQueryParams params, DimensionType dimensionType )
    {
        if ( params.hasTimeField() && DimensionType.PERIOD == dimensionType )
        {
            return DATE_PERIOD_STRUCT_ALIAS;
        }
        else if ( params.hasOrgUnitField() && DimensionType.ORGANISATION_UNIT == dimensionType )
        {
            return ORG_UNIT_STRUCT_ALIAS;
        }
        else
        {
            return ANALYTICS_TBL_ALIAS;
        }
    }

    /**
     * Template method that generates a SQL query for retrieving Events or
     * Enrollments
     *
     * @param params an {@see EventQueryParams} to drive the query generation
     * @param maxLimit max number of hits returned
     *
     * @return a SQL query
     */
    String getEventsOrEnrollmentsSql( EventQueryParams params, int maxLimit )
    {

        String sql = getSelectClause( params );

        sql += getFromClause( params );

        sql += getWhereClause( params );

        sql += getSortClause( params );

        sql += getPagingClause( params, maxLimit );

        return sql;
    }

    /**
     * Wraps the provided interface around a common exception handling strategy
     *
     * @param r a {@see Runnable} interface containing the code block to execute
     *        and wrap around the exception handling
     */
    void withExceptionHandling( Runnable r )
    {
        try
        {
            r.run();
        }
        catch ( BadSqlGrammarException ex )
        {
            log.info( AnalyticsUtils.ERR_MSG_TABLE_NOT_EXISTING, ex );
        }
        catch ( DataAccessResourceFailureException ex )
        {
            log.warn( ErrorCode.E7131.getMessage(), ex );
            throw new QueryRuntimeException( ErrorCode.E7131, ex );
        }
    }

    protected void addGridValue( Grid grid, GridHeader header, int index, SqlRowSet sqlRowSet, EventQueryParams params )
    {
        if ( Double.class.getName().equals( header.getType() ) && !header.hasLegendSet() )
        {
            double val = sqlRowSet.getDouble( index );

            if ( Double.isNaN( val ) )
            {
                grid.addValue( "" );
            }
            else
            {
                grid.addValue( params.isSkipRounding() ? val : MathUtils.getRounded( val ) );
            }
        }
        else if ( header.getValueType() == ValueType.REFERENCE )
        {
            String json = sqlRowSet.getString( index );

            ObjectMapper mapper = new ObjectMapper();

            try
            {
                JsonNode jsonNode = mapper.readTree( json );

                String uid = UUID.randomUUID().toString();

                Reference referenceNode = new Reference( uid, jsonNode );

                grid.addValue( uid );

                grid.addReference( referenceNode );
            }
            catch ( Exception e )
            {
                grid.addValue( json );
            }
        }
        else
        {
            grid.addValue( StringUtils.trimToNull( sqlRowSet.getString( index ) ) );
        }
    }

    /**
     * Return SQL string bnased on Items a its params
     *
     * @param params a {@see EventQueryParams}
     * @param hlp a {@see SqlHelper}
     */
    protected String getItemsSql( EventQueryParams params, SqlHelper hlp )
    {
        List<String> repeatableStagesSqlList = new ArrayList<>();

        StringBuilder sbItemsSql = new StringBuilder();

        params.getItems()
            .stream()
            .filter( QueryItem::hasFilter )
            .forEach( item -> item.getFilters()
                .forEach( filter -> {
                    String field = getSelectSql( filter, item, params.getEarliestStartDate(),
                        params.getLatestEndDate() );

                    if ( IN.equals( filter.getOperator() ) )
                    {
                        InQueryFilter inQueryFilter = new InQueryFilter( field,
                            statementBuilder.encode( filter.getFilter(), false ), item.isText() );
                        sbItemsSql.append( hlp.whereAnd() )
                            .append( " " )
                            .append( inQueryFilter.getSqlFilter() );
                    }
                    else if ( item.hasRepeatableStageParams() )
                    {
                        repeatableStagesSqlList.add( field + " " + filter.getSqlOperator() + " "
                            + getSqlFilter( filter, item ) );
                    }
                    else
                    {
                        sbItemsSql.append( hlp.whereAnd() )
                            .append( " " )
                            .append( field )
                            .append( " " )
                            .append( filter.getSqlOperator() )
                            .append( " " )
                            .append( getSqlFilter( filter, item ) )
                            .append( " " );
                    }
                } ) );

        if ( !repeatableStagesSqlList.isEmpty() )
        {
            sbItemsSql.append( hlp.whereAnd() )
                .append( " (" )
                .append( String.join( " or ", repeatableStagesSqlList ) )
                .append( ")" );
        }

        return sbItemsSql.toString();
    }

    /**
     * Returns a select SQL clause for the given query.
     *
     * @param params the {@link EventQueryParams}.
     */
    protected abstract String getSelectClause( EventQueryParams params );

    /**
     * Generates the SQL for the from-clause. Generally this means which
     * analytics table to get data from.
     *
     * @param params the {@link EventQueryParams} that define what is going to
     *        be queried.
     * @return SQL to add to the analytics query.
     */
    protected abstract String getFromClause( EventQueryParams params );

    /**
     * Generates the SQL for the where-clause. Generally this means adding
     * filters, grouping and ordering to the SQL.
     *
     * @param params the {@link EventQueryParams} that defines the details of
     *        the filters, grouping and ordering.
     * @return SQL to add to the analytics query.
     */
    protected abstract String getWhereClause( EventQueryParams params );

    /**
     * Returns the relevant {@link AnalyticsType}.
     *
     * @return the {@link AnalyticsType}.
     */
    protected abstract AnalyticsType getAnalyticsType();
}
