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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.hisp.dhis.analytics.AggregationType.CUSTOM;
import static org.hisp.dhis.analytics.AggregationType.NONE;
import static org.hisp.dhis.analytics.DataQueryParams.NUMERATOR_DENOMINATOR_PROPERTIES_COUNT;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.analytics.QueryKey.NV;
import static org.hisp.dhis.analytics.SortOrder.ASC;
import static org.hisp.dhis.analytics.SortOrder.DESC;
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.OU_GEOMETRY_COL_SUFFIX;
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.OU_NAME_COL_SUFFIX;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.DATE_PERIOD_STRUCT_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.encode;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quoteAlias;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_INDICATOR;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.hisp.dhis.common.RequestTypeAware.EndpointItem.ENROLLMENT;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;
import static org.hisp.dhis.system.util.MathUtils.getRounded;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.InQueryFilter;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryRuntimeException;
import org.hisp.dhis.common.Reference;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.option.Option;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Markus Bekken
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractJdbcEventAnalyticsManager
{
    protected static final String COL_COUNT = "count";

    protected static final String COL_EXTENT = "extent";

    protected static final int COORD_DEC = 6;

    protected static final int LAST_VALUE_YEARS_OFFSET = -10;

    private static final String COL_VALUE = "value";

    private static final String AND = " and ";

    private static final String OR = " or ";

    private static final String LIMIT = "limit";

    private static final Collector<CharSequence, ?, String> OR_JOINER = joining( OR, "(", ")" );

    private static final Collector<CharSequence, ?, String> AND_JOINER = joining( AND );

    @Qualifier( "readOnlyJdbcTemplate" )
    protected final JdbcTemplate jdbcTemplate;

    protected final ProgramIndicatorService programIndicatorService;

    protected final ProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder;

    protected final ExecutionPlanStore executionPlanStore;

    /**
     * Returns a SQL paging clause.
     *
     * @param params the {@link EventQueryParams}.
     * @param maxLimit the configurable max limit of records.
     */
    private String getPagingClause( EventQueryParams params, int maxLimit )
    {
        String sql = "";

        if ( params.isPaging() )
        {
            int limit = params.isTotalPages() ? params.getPageSizeWithDefault()
                : params.getPageSizeWithDefault() + 1;

            sql += LIMIT + " " + limit + " offset " + params.getOffset();
        }
        else if ( maxLimit > 0 )
        {
            sql += LIMIT + " " + (maxLimit + 1);
        }

        return sql;
    }

    /**
     * Returns a SQL sort clause.
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
                sql += getSortColumnForDataElementDimensionType( item );
            }
            else
            {
                // Query returns UIDs but we want sorting on name or shortName
                // depending on the display property for OUGS and COGS
                sql += Optional.ofNullable( extract( params.getDimensions(), item.getItem() ) )
                    .filter( this::isSupported )
                    .filter( DimensionalObject::hasItems )
                    .map( dim -> toCase( dim, quote( item.getItem().getUid() ), params.getDisplayProperty() ) )
                    .orElse( quote( item.getItem().getUid() ) );
            }

            sql += order == ASC ? " asc nulls last," : " desc nulls last,";
        }

        return sql;
    }

    private String getSortColumnForDataElementDimensionType( QueryItem item )
    {
        if ( ValueType.ORGANISATION_UNIT == item.getValueType() )
        {
            return quote( item.getItemName() + OU_NAME_COL_SUFFIX );
        }

        if ( item.hasRepeatableStageParams() )
        {
            return quote( item.getRepeatableStageParams().getDimension() );
        }

        if ( item.getProgramStage() != null )
        {
            return quote( item.getProgramStage().getUid() + "." + item.getItem().getUid() );
        }

        return quote( item.getItem().getUid() );
    }

    /**
     * Builds a CASE statement to use in sorting, mapping each OUGS and COGS
     * identifiers into its name or short name.
     */
    private String toCase( DimensionalObject dimension, String quotedAlias, DisplayProperty displayProperty )
    {
        return dimension.getItems().stream()
            .map( dio -> toWhenEntry( dio, quotedAlias, displayProperty ) )
            .collect( Collectors.joining( " ", "(CASE ", " ELSE '' END)" ) );
    }

    /**
     * Builds a WHEN statement based on the given {@link DimensionalItemObject}.
     */
    private String toWhenEntry( DimensionalItemObject item, String quotedAlias, DisplayProperty dp )
    {
        return "WHEN " +
            quotedAlias + "=" + encode( item.getUid() ) +
            " THEN " + (dp == DisplayProperty.NAME
                ? encode( item.getName() )
                : encode( item.getShortName() ));
    }

    private boolean isSupported( DimensionalObject dimension )
    {
        return dimension.getDimensionType() == DimensionType.ORGANISATION_UNIT_GROUP_SET
            || dimension.getDimensionType() == DimensionType.CATEGORY_OPTION_GROUP_SET;
    }

    private DimensionalObject extract( List<DimensionalObject> dimensions, DimensionalItemObject item )
    {
        return dimensions.stream()
            .filter( dimensionalObject -> dimensionalObject.getUid().equals( item.getUid() ) )
            .findFirst()
            .orElse( null );
    }

    /**
     * Returns the dynamic select column names to use in a group by clause.
     * Dimensions come first and query items second. Program indicator
     * expressions are converted to SQL expressions. When grouping with
     * non-default analytics period boundaries, all periods are skipped in the
     * group clause, as non default boundaries is defining their own period
     * groups within their where clause.
     */
    protected List<String> getGroupByColumnNames( EventQueryParams params, boolean isAggregated )
    {
        return getSelectColumns( params, true, isAggregated );
    }

    /**
     * Returns the dynamic select columns. Dimensions come first and query items
     * second. Program indicator expressions are converted to SQL expressions.
     * In the case of non-default boundaries
     * {@link EventQueryParams#hasNonDefaultBoundaries}, the period is
     * hard-coded into the select statement with "(isoPeriod) as (periodType)".
     */
    protected List<String> getSelectColumns( EventQueryParams params, boolean isAggregated )
    {
        return getSelectColumns( params, false, isAggregated );
    }

    /**
     * Returns the dynamic select columns. Dimensions come first and query items
     * second.
     *
     * @param params the {@link EventQueryParams}.
     * @param isGroupByClause used to avoid grouping by period when using
     *        non-default boundaries where the column content would be fixed.
     *        Used by the group by calls.
     */
    private List<String> getSelectColumns( EventQueryParams params, boolean isGroupByClause, boolean isAggregated )
    {
        List<String> columns = new ArrayList<>();

        addDimensionSelectColumns( columns, params, isGroupByClause );
        addItemSelectColumns( columns, params, isGroupByClause, isAggregated );

        return columns;
    }

    /**
     * Adds the dynamic dimension select columns. Program indicator expressions
     * are converted to SQL expressions. In the case of non-default boundaries
     * {@link EventQueryParams#hasNonDefaultBoundaries}, the period is
     * hard-coded into the select statement with "(isoPeriod) as (periodType)".
     * <p>
     * If the first/last subquery is used then one query will be done for each
     * period, and the period will not be present in the query, so add it to the
     * select columns and skip it in the group by columns.
     */
    private void addDimensionSelectColumns( List<String> columns, EventQueryParams params, boolean isGroupByClause )
    {
        for ( DimensionalObject dimension : params.getDimensions() )
        {
            if ( isGroupByClause && dimension.getDimensionType() == DimensionType.PERIOD
                && params.hasNonDefaultBoundaries() )
            {
                continue;
            }

            if ( dimension.getDimensionType() == DimensionType.PERIOD &&
                params.getAggregationTypeFallback().isFirstOrLastPeriodAggregationType() )
            {
                if ( !isGroupByClause )
                {
                    String alias = quote( dimension.getDimensionName() );
                    columns.add( "cast('" + params.getLatestPeriod().getDimensionItem() + "' as text) as " + alias );
                }
            }
            else if ( !params.hasNonDefaultBoundaries() || dimension.getDimensionType() != DimensionType.PERIOD )
            {
                columns.add( getTableAndColumn( params, dimension, isGroupByClause ) );
            }
            else if ( params.hasSinglePeriod() )
            {
                Period period = (Period) params.getPeriods().get( 0 );
                columns.add( encode( period.getIsoDate() ) + " as " +
                    period.getPeriodType().getName() );
            }
            else if ( !params.hasPeriods() && params.hasFilterPeriods() )
            {
                // Assuming same period type for all period filters, as the
                // query planner splits into one query per period type

                Period period = (Period) params.getFilterPeriods().get( 0 );
                columns.add( encode( period.getIsoDate() ) + " as " +
                    period.getPeriodType().getName() );
            }
            else
            {
                throw new IllegalStateException( "Program indicator non-default boundary query must have " +
                    "exactly one period, or no periods and a period filter" );
            }
        }
    }

    private void addItemSelectColumns( List<String> columns, EventQueryParams params, boolean isGroupByClause,
        boolean isAggregated )
    {
        for ( QueryItem queryItem : params.getItems() )
        {
            ColumnAndAlias columnAndAlias = getColumnAndAlias( queryItem, params, isGroupByClause, isAggregated );

            columns.add( columnAndAlias.asSql() );

            // asked for row context if allowed and needed
            if ( rowContextAllowedAndNeeded( params, queryItem ) )
            {
                String column = " exists (" + columnAndAlias.column + ")";
                String alias = columnAndAlias.alias + ".exists";
                columns.add( (new ColumnAndAlias( column, alias )).asSql() );
            }
        }
    }

    /**
     * Eligibility of enrollment request for grid row context
     *
     * @param params
     * @param queryItem
     * @return true when eligible for row context
     */
    private boolean rowContextAllowedAndNeeded( EventQueryParams params, QueryItem queryItem )
    {
        return params.getEndpointItem() == ENROLLMENT && params.isRowContext() && queryItem.hasProgramStage()
            && queryItem.getProgramStage().getRepeatable()
            && queryItem.hasRepeatableStageParams();
    }

    private ColumnAndAlias getColumnAndAlias( QueryItem queryItem, EventQueryParams params, boolean isGroupByClause,
        boolean isAggregated )
    {
        if ( queryItem.isProgramIndicator() )
        {
            ProgramIndicator in = (ProgramIndicator) queryItem.getItem();

            String asClause = in.getUid();
            String programIndicatorSubquery;

            if ( queryItem.hasRelationshipType() )
            {
                programIndicatorSubquery = programIndicatorSubqueryBuilder.getAggregateClauseForProgramIndicator( in,
                    queryItem.getRelationshipType(), getAnalyticsType(), params.getEarliestStartDate(),
                    params.getLatestEndDate() );
            }
            else
            {
                programIndicatorSubquery = programIndicatorSubqueryBuilder.getAggregateClauseForProgramIndicator( in,
                    getAnalyticsType(), params.getEarliestStartDate(), params.getLatestEndDate() );
            }

            return ColumnAndAlias.ofColumnAndAlias( programIndicatorSubquery, asClause );
        }
        else if ( ValueType.COORDINATE == queryItem.getValueType() )
        {
            return getCoordinateColumn( queryItem );
        }
        else if ( ValueType.ORGANISATION_UNIT == queryItem.getValueType() )
        {
            if ( params.getCoordinateFields().stream().anyMatch( f -> queryItem.getItem().getUid().equals( f ) ) )
            {
                return getCoordinateColumn( queryItem, OU_GEOMETRY_COL_SUFFIX );
            }
            else
            {
                return ColumnAndAlias.ofColumn( getColumn( queryItem, OU_NAME_COL_SUFFIX ) );
            }
        }
        else if ( queryItem.getValueType() == ValueType.NUMBER && !isGroupByClause )
        {
            ColumnAndAlias columnAndAlias = getColumnAndAlias( queryItem, isAggregated, queryItem.getItemName() );

            return ColumnAndAlias.ofColumnAndAlias(
                columnAndAlias.getColumn(),
                defaultIfNull( columnAndAlias.getAlias(), queryItem.getItemName() ) );
        }
        else if ( queryItem.isText() && !isGroupByClause && hasOrderByClauseForQueryItem( queryItem, params ) )
        {
            return getColumnAndAliasWithNullIfFunction( queryItem );
        }
        else
        {
            return getColumnAndAlias( queryItem, isGroupByClause, "" );
        }
    }

    /**
     * The method create a ColumnAndAlias object with nullif sql function. toSql
     * function of class will return f.e. nullif(select 'w75KJ2mc4zz' from...,
     * '') as 'w75KJ2mc4zz'
     *
     * @param queryItem the {@link QueryItem}.
     * @return the {@link ColumnAndAlias} {@link ColumnWithNullIfAndAlias}
     */
    private ColumnAndAlias getColumnAndAliasWithNullIfFunction( QueryItem queryItem )
    {
        String column = getColumn( queryItem );

        if ( queryItem.hasProgramStage() && queryItem.getItem().getDimensionItemType() == DATA_ELEMENT )
        {
            return ColumnWithNullIfAndAlias.ofColumnWithNullIfAndAlias( column,
                queryItem.getProgramStage().getUid() + "." + queryItem.getItem().getUid() );
        }

        return ColumnWithNullIfAndAlias.ofColumnWithNullIfAndAlias( column, queryItem.getItem().getUid() );
    }

    private boolean hasOrderByClauseForQueryItem( QueryItem queryItem, EventQueryParams params )
    {
        List<QueryItem> orderByColumns = getDistinctOrderByColumns( params );

        return orderByColumns.contains( queryItem );
    }

    private ColumnAndAlias getColumnAndAlias( QueryItem queryItem, boolean isGroupByClause, String aliasIfMissing )
    {
        String column = getColumn( queryItem );

        if ( !isGroupByClause )
        {
            return ColumnAndAlias.ofColumnAndAlias( column, getAlias( queryItem ).orElse( aliasIfMissing ) );
        }

        return ColumnAndAlias.ofColumn( column );
    }

    protected Optional<String> getAlias( QueryItem queryItem )
    {
        return Optional.of( queryItem )
            .filter( QueryItem::hasProgramStage )
            .filter( QueryItem::hasRepeatableStageParams )
            .map( QueryItem::getRepeatableStageParams )
            .map( RepeatableStageParams::getDimension );
    }

    public Grid getAggregatedEventData( EventQueryParams params, Grid grid, int maxLimit )
    {
        String aggregateClause = getAggregateClause( params );

        String sql = TextUtils.removeLastComma( "select " + aggregateClause + " as value," +
            StringUtils.join( getSelectColumns( params, true ), "," ) + " " );

        // ---------------------------------------------------------------------
        // Criteria
        // ---------------------------------------------------------------------

        sql += getFromClause( params );

        sql += getWhereClause( params );

        sql += getGroupByClause( params );

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
            sql += LIMIT + " " + params.getLimit();
        }
        else if ( maxLimit > 0 )
        {
            sql += LIMIT + " " + (maxLimit + 1);
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
            throw new QueryRuntimeException( ErrorCode.E7131 );
        }

        return grid;
    }

    /**
     * Returns a group by SQL clause.
     *
     * @param params the {@link EventQueryParams}.
     * @return a group by SQL clause.
     */
    private String getGroupByClause( EventQueryParams params )
    {
        String sql = "";

        if ( params.isAggregation() )
        {
            List<String> selectColumnNames = getGroupByColumnNames( params, true );

            if ( isNotEmpty( selectColumnNames ) )
            {
                sql += "group by " + getCommaDelimitedString( selectColumnNames ) + " ";
            }
        }

        return sql;
    }

    private void getAggregatedEventData( Grid grid, EventQueryParams params, String sql )
    {
        log.debug( "Event analytics aggregate SQL: '{}'", sql );

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        while ( rowSet.next() )
        {
            grid.addRow();

            if ( params.isAggregateData() )
            {
                if ( params.hasValueDimension() )
                {
                    String itemId = params.getProgram().getUid() +
                        COMPOSITE_DIM_OBJECT_PLAIN_SEP + params.getValue().getUid();
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

                    ColumnAndAlias columnAndAlias = getColumnAndAlias( queryItem, params, false, true );
                    String alias = columnAndAlias.getAlias();

                    if ( isEmpty( alias ) )
                    {
                        alias = queryItem.getItemName();
                    }

                    String itemName = rowSet.getString( alias );
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
                if ( params.hasTextValueDimension() )
                {
                    String value = rowSet.getString( COL_VALUE );
                    grid.addValue( value );
                }
                else // Numeric
                {
                    double value = rowSet.getDouble( COL_VALUE );
                    grid.addValue( params.isSkipRounding() ? value : getRounded( value ) );
                }
            }
            else if ( params.hasProgramIndicatorDimension() )
            {
                double value = rowSet.getDouble( COL_VALUE );
                ProgramIndicator indicator = params.getProgramIndicator();
                grid.addValue( AnalyticsUtils.getRoundedValue( params, indicator.getDecimals(), value ) );
            }
            else
            {
                int value = rowSet.getInt( COL_VALUE );
                grid.addValue( value );
            }

            if ( params.isIncludeNumDen() )
            {
                grid.addNullValues( NUMERATOR_DENOMINATOR_PROPERTIES_COUNT );
            }
        }
    }

    /**
     * Returns the aggregate clause based on value dimension and output type.
     *
     * @param params the {@link EventQueryParams}.
     */
    protected String getAggregateClause( EventQueryParams params )
    {
        // TODO include output type if aggregation type is count

        EventOutputType outputType = params.getOutputType();

        AggregationType aggregationType = params.getAggregationTypeFallback().getAggregationType();

        String function = (aggregationType == NONE || aggregationType == CUSTOM)
            ? ""
            : aggregationType.getValue();

        if ( !params.isAggregation() )
        {
            return quoteAlias( params.getValue().getUid() );
        }
        else if ( params.getAggregationTypeFallback().isFirstOrLastPeriodAggregationType()
            && params.hasEventProgramIndicatorDimension() )
        {
            return function + "(value)";
        }
        else if ( params.hasNumericValueDimension() )
        {
            String expression = quoteAlias( params.getValue().getUid() );

            return function + "(" + expression + ")";
        }
        else if ( params.hasProgramIndicatorDimension() )
        {
            String expression = programIndicatorService.getAnalyticsSql( params.getProgramIndicator().getExpression(),
                NUMERIC, params.getProgramIndicator(), params.getEarliestStartDate(), params.getLatestEndDate() );

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
     * @param item the {@link QueryItem}.
     * @return the column select statement for the given item.
     */
    protected ColumnAndAlias getCoordinateColumn( QueryItem item )
    {
        String colName = item.getItemName();

        return ColumnAndAlias
            .ofColumnAndAlias(
                "'[' || round(ST_X(" + quote( colName ) + ")::numeric, 6) || ',' || round(ST_Y(" + quote( colName )
                    + ")::numeric, 6) || ']'",
                getAlias( item ).orElse( colName ) );
    }

    /**
     * Creates a coordinate base column "selector" for the given item name. The
     * item is expected to be of type Coordinate.
     *
     * @param item the {@link QueryItem}.
     * @param suffix the suffix to append to the item id.
     * @return the column select statement for the given item.
     */
    protected ColumnAndAlias getCoordinateColumn( QueryItem item, String suffix )
    {
        String colName = item.getItemId() + suffix;

        String stCentroidFunction = "";

        if ( ValueType.ORGANISATION_UNIT == item.getValueType() )
        {
            stCentroidFunction = "ST_Centroid";
        }

        return ColumnAndAlias.ofColumnAndAlias( "'[' || round(ST_X(" + stCentroidFunction +
            "(" + quote( colName ) + "))::numeric, 6) || ',' || round(ST_Y(" + stCentroidFunction + "(" +
            quote( colName ) + "))::numeric, 6) || ']'", colName );
    }

    /**
     * Creates a column selector for the given item name. The suffix will be
     * appended as part of the item name.
     *
     * @param item the {@link QueryItem}.
     * @param suffix the suffix.
     * @return the the column select statement for the given item.
     */
    protected String getColumn( QueryItem item, String suffix )
    {
        return quote( item.getItemName() + suffix );
    }

    /**
     * Returns an encoded column name.
     *
     * @param item the {@link QueryItem}.
     */
    protected String getColumn( QueryItem item )
    {
        return quoteAlias( item.getItemName() );
    }

    /**
     * Returns a SQL statement to select the expression or column of the item.
     * If the item is a program indicator, the program indicator expression is
     * returned; if the item is a data element, the item column name is
     * returned.
     *
     * @param filter the {@link QueryFilter}.
     * @param item the {@link QueryItem}.
     * @param startDate the start date.
     * @param endDate the end date.
     */
    protected String getSelectSql( QueryFilter filter, QueryItem item, Date startDate, Date endDate )
    {
        if ( item.isProgramIndicator() )
        {
            ProgramIndicator programIndicator = (ProgramIndicator) item.getItem();

            return programIndicatorService.getAnalyticsSql( programIndicator.getExpression(), NUMERIC,
                programIndicator, startDate, endDate );
        }
        else
        {
            return filter.getSqlFilterColumn( getColumn( item ), item.getValueType() );
        }
    }

    protected String getSelectSql( QueryFilter filter, QueryItem item, EventQueryParams params )
    {
        if ( item.isProgramIndicator() )
        {
            return getColumnAndAlias( item, params, false, false ).getColumn();
        }
        else
        {
            return filter.getSqlFilterColumn( getColumn( item ), item.getValueType() );
        }
    }

    /**
     * Returns a filter string.
     *
     * @param filter the filter string.
     * @param item the {@link QueryItem}.
     * @return a filter string.
     */
    private String getFilter( String filter, QueryItem item )
    {
        try
        {
            if ( !NV.equals( filter ) && item.getValueType() == ValueType.DATETIME )
            {
                return DateFormatUtils.format(
                    DateUtils.parseDate( filter,
                        // known formats
                        "yyyy-MM-dd'T'HH.mm",
                        "yyyy-MM-dd'T'HH.mm.ss" ),
                    // postgres format
                    "yyyy-MM-dd HH:mm:ss" );
            }
        }
        catch ( ParseException pe )
        {
            throwIllegalQueryEx( ErrorCode.E7135, filter );
        }

        return filter;
    }

    /**
     * Returns the queryFilter value for the given query item.
     *
     * @param queryFilter the {@link QueryFilter}.
     * @param item the {@link QueryItem}.
     */
    protected String getSqlFilter( QueryFilter queryFilter, QueryItem item )
    {
        String filter = getFilter( queryFilter.getFilter(), item );
        String encodedFilter = encode( filter, false );

        return item.getSqlFilter( queryFilter, encodedFilter, true );
    }

    /**
     * Returns the analytics table alias and column.
     *
     * @param params the {@link EventQueryParams}.
     * @param dimension the {@link DimensionalObject}.
     * @param isGroupByClause don't add a column alias if present.
     */
    private String getTableAndColumn( EventQueryParams params, DimensionalObject dimension, boolean isGroupByClause )
    {
        String col = dimension.getDimensionName();

        if ( params.hasTimeField() && DimensionType.PERIOD == dimension.getDimensionType() )
        {
            return quote( DATE_PERIOD_STRUCT_ALIAS, col );
        }
        else if ( DimensionType.ORGANISATION_UNIT == dimension.getDimensionType() )
        {
            return params.getOrgUnitField().getOrgUnitStructCol( col, getAnalyticsType(), isGroupByClause );
        }
        else if ( DimensionType.ORGANISATION_UNIT_GROUP_SET == dimension.getDimensionType() )
        {
            return params.getOrgUnitField().getOrgUnitGroupSetCol( col, getAnalyticsType(), isGroupByClause );
        }
        else
        {
            return quote( ANALYTICS_TBL_ALIAS, col );
        }
    }

    /**
     * Template method that generates a SQL query for retrieving events or
     * enrollments.
     *
     * @param params the {@link EventQueryParams} to drive the query generation.
     * @param maxLimit max number of records to return.
     * @return a SQL query.
     */
    protected String getEventsOrEnrollmentsSql( EventQueryParams params, int maxLimit )
    {
        String sql = getSelectClause( params );

        sql += getFromClause( params );

        sql += getWhereClause( params );

        sql += getSortClause( params );

        sql += getPagingClause( params, maxLimit );

        return sql;
    }

    /**
     * Wraps the provided interface around a common exception handling strategy.
     *
     * @param runnable the {@link Runnable} containing the code block to execute
     *        and wrap around the exception handling.
     */
    protected void withExceptionHandling( Runnable runnable )
    {
        try
        {
            runnable.run();
        }
        catch ( BadSqlGrammarException ex )
        {
            log.info( AnalyticsUtils.ERR_MSG_TABLE_NOT_EXISTING, ex );
        }
        catch ( DataAccessResourceFailureException ex )
        {
            log.warn( ErrorCode.E7131.getMessage(), ex );
            throw new QueryRuntimeException( ErrorCode.E7131 );
        }
    }

    /**
     * Adds a value from the given row set to the grid.
     *
     * @param grid the {@link Grid}.
     * @param header the {@link GridHeader}.
     * @param index the row set index.
     * @param sqlRowSet the {@link SqlRowSet}.
     * @param params the {@link EventQueryParams}.
     */
    protected void addGridValue( Grid grid, GridHeader header, int index, SqlRowSet sqlRowSet, EventQueryParams params )
    {
        if ( Double.class.getName().equals( header.getType() ) && !header.hasLegendSet() )
        {
            Object value = sqlRowSet.getObject( index );

            boolean isDouble = value instanceof Double;

            if ( value == null || (isDouble && Double.isNaN( (Double) value )) )
            {
                grid.addValue( EMPTY );
            }
            else if ( isDouble && !Double.isNaN( (Double) value ) )
            {
                addGridDoubleTypeValue( (Double) value, grid, header, params );
            }
            else if ( value instanceof BigDecimal )
            {
                // toPlainString method prevents scientific notation (3E+2)
                grid.addValue( ((BigDecimal) value).stripTrailingZeros().toPlainString() );
            }
            else
            {
                grid.addValue( StringUtils.trimToNull( sqlRowSet.getString( index ) ) );
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
     * Double value type will be added into the grid. There is special handling
     * for Option Set (Type numeric)/Option. The code in grid/meta info and
     * related value in row has to be the same (FE request) if possible. The
     * string interpretation of code coming from Option/Code can vary from
     * Option/value (double) fetched from database ("1" vs "1.0") By the
     * equality (both are converted to double) of both the Option/Code is used
     * as a value.
     *
     * @param value the value.
     * @param grid the {@link Grid}.
     * @param header the {@link GridHeader}.
     * @param params the {@link EventQueryParams}.
     */
    private void addGridDoubleTypeValue( Double value, Grid grid, GridHeader header, EventQueryParams params )
    {
        if ( header.hasOptionSet() )
        {
            Optional<Option> option = header.getOptionSetObject().getOptions().stream()
                .filter( o -> NumberUtils.isCreatable( o.getCode() ) &&
                    MathUtils.isEqual( NumberUtils.createDouble( o.getCode() ), value ) )
                .findFirst();

            if ( option.isPresent() )
            {
                grid.addValue( option.get().getCode() );
            }
            else
            {
                grid.addValue( params.isSkipRounding() ? value : MathUtils.getRoundedObject( value ) );
            }
        }
        else
        {
            grid.addValue( params.isSkipRounding() ? value : MathUtils.getRoundedObject( value ) );
        }
    }

    /**
     * Returns a SQL where clause string for query items and query item filters.
     *
     * @param params the {@link EventQueryParams}.
     * @param helper the {@link SqlHelper}.
     */
    protected String getQueryItemsAndFiltersWhereClause( EventQueryParams params, SqlHelper helper )
    {
        if ( params.isEnhancedCondition() )
        {
            return getItemsSqlForEnhancedConditions( params, helper );
        }

        // Creates a map grouping query items referring to repeatable stages and
        // those referring to non-repeatable stages. This is for enrollment
        // only, event query items are treated as non-repeatable.
        Map<Boolean, List<QueryItem>> itemsByRepeatableFlag = Stream.concat(
            params.getItems().stream(), params.getItemFilters().stream() )
            .filter( QueryItem::hasFilter )
            .collect( groupingBy(
                queryItem -> queryItem.hasRepeatableStageParams() && params.getEndpointItem() == ENROLLMENT ) );

        // Groups repeatable conditions based on PSI.DEID
        Map<String, List<String>> repeatableConditionsByIdentifier = asSqlCollection( itemsByRepeatableFlag.get( true ),
            params )
                .collect( groupingBy( IdentifiableSql::getIdentifier, mapping( IdentifiableSql::getSql, toList() ) ) );

        // Joins each group with OR
        Collection<String> orConditions = repeatableConditionsByIdentifier.values()
            .stream()
            .map( sameGroup -> joinSql( sameGroup, OR_JOINER ) )
            .collect( toList() );

        // Non-repeatable conditions
        Collection<String> andConditions = asSqlCollection( itemsByRepeatableFlag.get( false ), params )
            .map( IdentifiableSql::getSql )
            .collect( toList() );

        if ( orConditions.isEmpty() && andConditions.isEmpty() )
        {
            return StringUtils.EMPTY;
        }

        return helper.whereAnd() + " " + joinSql( Stream.concat(
            orConditions.stream(),
            andConditions.stream() ), AND_JOINER );

    }

    /**
     * Joins a stream of conditions using given join function. Returns empty
     * string if collection is empty.
     */
    private String joinSql( Stream<String> conditions, Collector<CharSequence, ?, String> joiner )
    {
        return joinSql( conditions.collect( toList() ), joiner );
    }

    private String getItemsSqlForEnhancedConditions( EventQueryParams params, SqlHelper hlp )
    {
        Map<UUID, String> sqlConditionByGroup = Stream.concat(
            params.getItems().stream(), params.getItemFilters().stream() )
            .filter( QueryItem::hasFilter )
            .collect(
                groupingBy( QueryItem::getGroupUUID, mapping( queryItem -> toSql( queryItem, params ), OR_JOINER ) ) );

        if ( sqlConditionByGroup.values().isEmpty() )
        {
            return "";
        }

        return hlp.whereAnd() + " " + String.join( AND, sqlConditionByGroup.values() );
    }

    /**
     * Joins a collection of conditions using given join function, returns empty
     * string if collection is empty
     */
    private String joinSql( Collection<String> conditions, Collector<CharSequence, ?, String> joiner )
    {
        if ( !conditions.isEmpty() )
        {
            return conditions.stream().collect( joiner );
        }

        return "";
    }

    /**
     * Returns a collection of IdentifiableSql, each representing SQL for given
     * queryItems together with its identifier.
     */
    private Stream<IdentifiableSql> asSqlCollection( List<QueryItem> queryItems, EventQueryParams params )
    {
        return emptyIfNull( queryItems ).stream()
            .map( queryItem -> toIdentifiableSql( queryItem, params ) );
    }

    /**
     * Converts given queryItem into {@link IdentifiableSql} joining its filters
     * using AND.
     */
    private IdentifiableSql toIdentifiableSql( QueryItem queryItem, EventQueryParams params )
    {
        return IdentifiableSql.builder()
            .identifier( getIdentifier( queryItem ) )
            .sql( toSql( queryItem, params ) )
            .build();
    }

    /**
     * Converts given queryItem into SQL joining its filters using AND.
     */
    private String toSql( QueryItem queryItem, EventQueryParams params )
    {
        return queryItem.getFilters().stream()
            .map( filter -> toSql( queryItem, filter, params ) )
            .collect( joining( AND ) );
    }

    /**
     * Returns PSID.ITEM_ID of given queryItem.
     */
    private String getIdentifier( QueryItem queryItem )
    {
        String programStageId = Optional.of( queryItem )
            .map( QueryItem::getProgramStage )
            .map( IdentifiableObject::getUid )
            .orElse( "" );
        return programStageId + "." + queryItem.getItem().getUid();
    }

    @Getter
    @Builder
    private static class IdentifiableSql
    {
        private final String identifier;

        private final String sql;
    }

    /**
     * Creates a SQL statement for a single filter inside a query item.
     *
     * @param item the {@link QueryItem}.
     * @param filter the {@link QueryFilter}.
     * @param params the {@link EventQueryParams}.
     */
    private String toSql( QueryItem item, QueryFilter filter, EventQueryParams params )
    {
        String field = item.hasAggregationType() ? getSelectSql( filter, item, params )
            : getSelectSql( filter, item, params.getEarliestStartDate(),
                params.getLatestEndDate() );

        if ( IN.equals( filter.getOperator() ) )
        {
            InQueryFilter inQueryFilter = new InQueryFilter( field,
                encode( filter.getFilter(), false ), item.isText() );

            return inQueryFilter.getSqlFilter();
        }
        else
        {
            // NV filter has its own specific logic, so skip values
            // comparisons when NV is set as filter
            if ( !NV.equals( filter.getFilter() ) )
            {
                // Specific handling for null and empty values
                switch ( filter.getOperator() )
                {
                case NEQ:
                case NE:
                case NIEQ:
                case NLIKE:
                case NILIKE:
                    return nullAndEmptyMatcher( item, filter, field );
                default:
                    break;
                }
            }

            return field + SPACE + filter.getSqlOperator( true ) + SPACE + getSqlFilter( filter, item ) + SPACE;
        }
    }

    /**
     * Ensures that null/empty values will always match.
     *
     * @param item the {@link QueryItem}.
     * @param filter the {@link QueryFilter}.
     * @param field the field.
     * @return the respective SQL statement matcher.
     */
    private String nullAndEmptyMatcher( QueryItem item, QueryFilter filter, String field )
    {
        if ( item.getValueType() != null && item.getValueType().isText() )
        {
            return "(coalesce(" + field + ", '') = '' or " + field + SPACE + filter.getSqlOperator( true ) + SPACE
                + getSqlFilter( filter, item ) + ") ";
        }
        else
        {
            return "(" + field + " is null or " + field + SPACE + filter.getSqlOperator( true ) + SPACE
                + getSqlFilter( filter, item ) + ") ";
        }
    }

    /**
     * Method responsible for merging query items based on sorting parameters
     *
     * @param params the {@link EventQueryParams} to drive the query item list
     *        generation.
     * @return the distinct {@link List<QueryItem>} relevant for order by DML.
     */
    private List<QueryItem> getDistinctOrderByColumns( EventQueryParams params )
    {
        List<QueryItem> orderByAscColumns = new ArrayList<>();

        List<QueryItem> orderByDescColumns = new ArrayList<>();

        if ( params.getAsc() != null && !params.getAsc().isEmpty() )
        {
            orderByAscColumns.addAll( params.getAsc() );
        }

        if ( params.getDesc() != null && !params.getDesc().isEmpty() )
        {
            orderByDescColumns.addAll( params.getDesc() );
        }

        return ListUtils.distinctUnion( orderByAscColumns, orderByDescColumns );
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
