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
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.hisp.dhis.analytics.DataQueryParams.NUMERATOR_DENOMINATOR_PROPERTIES_COUNT;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.analytics.QueryKey.NV;
import static org.hisp.dhis.analytics.SortOrder.ASC;
import static org.hisp.dhis.analytics.SortOrder.DESC;
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.OU_GEOMETRY_COL_SUFFIX;
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.OU_NAME_COL_SUFFIX;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.DATE_PERIOD_STRUCT_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ORG_UNIT_STRUCT_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.encode;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quoteAlias;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_INDICATOR;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.hisp.dhis.common.RequestTypeAware.EndpointItem.ENROLLMENT;
import static org.hisp.dhis.system.util.MathUtils.getRounded;

import java.text.ParseException;
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
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.InQueryFilter;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryRuntimeException;
import org.hisp.dhis.common.Reference;
import org.hisp.dhis.common.RepeatableStageParams;
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
    private static final String LIMIT = "limit";

    protected static final String COL_COUNT = "count";

    protected static final String COL_EXTENT = "extent";

    protected static final int COORD_DEC = 6;

    protected static final int LAST_VALUE_YEARS_OFFSET = -10;

    private static final String _AND_ = " and ";

    private static final String _OR_ = " or ";

    private static final Collector<CharSequence, ?, String> OR_JOINER = joining( _OR_, "(", ")" );

    private static final Collector<CharSequence, ?, String> AND_JOINER = joining( _AND_ );

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
                /*
                 * Query returns UIDs but we want sorting on name or shortName
                 * (depending on DisplayProperty) for OUGS/COGS
                 */
                sql += Optional.ofNullable( extract( params.getDimensions(), item.getItem() ) )
                    .filter( this::isSupported )
                    .filter( this::hasItems )
                    .map( dim -> toCase( dim, quoteAlias( item.getItem().getUid() ), params.getDisplayProperty() ) )
                    .orElse( quoteAlias( item.getItem().getUid() ) );
            }

            sql += order == ASC ? " asc," : " desc,";
        }

        return sql;
    }

    /**
     * builds a CASE statement to use in sorting, mapping each OUGS/COGS uid
     * into its name/shortName
     */
    private String toCase( DimensionalObject dimension, String quotedAlias, DisplayProperty displayProperty )
    {
        return dimension.getItems().stream()
            .map( dio -> toWhenEntry( dio, quotedAlias, displayProperty ) )
            .collect( Collectors.joining( " ", "(CASE ", " ELSE '' END)" ) );
    }

    /**
     * given an DimensionalItemObject, builds a WHEN statement
     */
    private String toWhenEntry( DimensionalItemObject dio, String quotedAlias, DisplayProperty dp )
    {
        return "WHEN " +
            quotedAlias + "=" + encode( dio.getUid(), true ) +
            " THEN " + (dp == DisplayProperty.NAME
                ? encode( dio.getName(), true )
                : encode( dio.getShortName(), true ));
    }

    private boolean hasItems( DimensionalObject dimensionalObject )
    {
        return !dimensionalObject.getItems().isEmpty();
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
    private List<String> getGroupByColumnNames( EventQueryParams params, boolean isAggregated )
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
     * second. Program indicator expressions are converted to SQL expressions.
     * In the case of non-default boundaries
     * {@link EventQueryParams#hasNonDefaultBoundaries}, the period is
     * hard-coded into the select statement with "(isoPeriod) as (periodType)".
     *
     * @param isGroupByClause used to avoid grouping by period when using
     *        non-default boundaries where the column content would be
     *        hard-coded. Used by the group-by calls.
     */
    private List<String> getSelectColumns( EventQueryParams params, boolean isGroupByClause, boolean isAggregated )
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
                throw new IllegalStateException( "Program indicator non-default boundary query must have " +
                    "exactly one period, or no periods and a period filter" );
            }
        }

        for ( QueryItem queryItem : params.getItems() )
        {
            columns.add( getColumnAndAlias( queryItem, params, isGroupByClause, isAggregated ).asSql() );
        }

        return columns;
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

            if ( queryItem.getValueType() == ValueType.NUMBER )
            {
                return ColumnAndAlias.ofColumnAndAlias(
                    coalesceAsDoubleNan( programIndicatorSubquery ),
                    asClause );
            }
            else
            {
                return ColumnAndAlias.ofColumnAndAlias( programIndicatorSubquery, asClause );
            }

        }
        else if ( ValueType.COORDINATE == queryItem.getValueType() )
        {
            return getCoordinateColumn( queryItem );
        }
        else if ( ValueType.ORGANISATION_UNIT == queryItem.getValueType() )
        {
            if ( queryItem.getItem().getUid().equals( params.getCoordinateField() ) )
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
                coalesceAsDoubleNan( columnAndAlias.getColumn() ),
                defaultIfNull( columnAndAlias.getAlias(), queryItem.getItemName() ) );
        }
        else
        {
            return getColumnAndAlias( queryItem, isGroupByClause, "" );
        }
    }

    protected String coalesceAsDoubleNan( String column )
    {
        return "coalesce(" + column + ", double precision 'NaN')";
    }

    private ColumnAndAlias getColumnAndAlias( QueryItem queryItem, boolean isGroupByClause, String aliasIfMissing )
    {
        String column = getColumn( queryItem );
        if ( !isGroupByClause )
        {
            return ColumnAndAlias.ofColumnAndAlias(
                column,
                getAlias( queryItem )
                    .orElse( aliasIfMissing ) );
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
        String countClause = getAggregateClause( params );

        String sql = TextUtils.removeLastComma( "select " + countClause + " as value," +
            StringUtils.join( getSelectColumns( params, true ), "," ) + " " );

        // ---------------------------------------------------------------------
        // Criteria
        // ---------------------------------------------------------------------

        sql += getFromClause( params );

        sql += getWhereClause( params );

        // ---------------------------------------------------------------------
        // Group by
        // ---------------------------------------------------------------------

        List<String> selectColumnNames = getGroupByColumnNames( params, true );

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

    private void getAggregatedEventData( Grid grid, EventQueryParams params, String sql )
    {
        log.debug( "Event analytics aggregate SQL: " + sql );

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

                    ColumnAndAlias columnAndAlias = getColumnAndAlias( queryItem, params, false, true );
                    String alias = columnAndAlias.getAlias();
                    if ( StringUtils.isEmpty( alias ) )
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
     */
    protected String getAggregateClause( EventQueryParams params )
    {
        // TODO include output type if aggregation type is count

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
     * @param item the {@link QueryItem}
     * @return the column select statement for the given item
     */
    protected ColumnAndAlias getCoordinateColumn( final QueryItem item )
    {
        final String colName = item.getItemName();

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
     * @param item the {@link QueryItem}
     * @param suffix the suffix to append to the item id
     * @return the column select statement for the given item
     */
    protected ColumnAndAlias getCoordinateColumn( final QueryItem item, final String suffix )
    {
        final String colName = item.getItemId() + suffix;

        String stCentroidFunction = "";

        if ( ValueType.ORGANISATION_UNIT == item.getValueType() )
        {
            stCentroidFunction = "ST_Centroid";
        }

        return ColumnAndAlias.ofColumnAndAlias(
            "'[' || round(ST_X(" + stCentroidFunction + "(" + quote( colName ) + "))::numeric, 6) || ',' || round(ST_Y("
                + stCentroidFunction + "(" + quote( colName ) + "))::numeric, 6) || ']'",
            colName );
    }

    /**
     * Creates a column selector for the given item name. The suffix will be
     * appended as part of the item name.
     *
     * @param item the {@link QueryItem}.
     * @param suffix the suffix.
     * @return the the column select statement for the given item
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
            return programIndicatorService.getAnalyticsSql( programIndicator.getExpression(), NUMERIC, programIndicator,
                startDate, endDate );
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
        String encodedFilter = statementBuilder.encode( filter, false );

        return item.getSqlFilter( queryFilter, encodedFilter );
    }

    /**
     * Returns the analytics table alias for the organisation unit dimension.
     *
     * @param params the {@link EventQueryParams}.
     */
    protected String getOrgUnitAlias( EventQueryParams params )
    {
        return params.hasOrgUnitField() ? ORG_UNIT_STRUCT_ALIAS : ANALYTICS_TBL_ALIAS;
    }

    /**
     * Returns the analytics table alias.
     *
     * @param params the {@link EventQueryParams}.
     * @param dimensionType the {@link DimensionType}.
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
     * @param runnable a {@link Runnable} containing the code block to execute
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
            else
            {
                grid.addValue( params.isSkipRounding() ? value : MathUtils.getRoundedObject( value ) );
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
     * Return SQL string based on both query items and filters
     *
     * @param params a {@link EventQueryParams}.
     * @param helper a {@link SqlHelper}.
     */
    protected String getStatementForDimensionsAndFilters( EventQueryParams params, SqlHelper helper )
    {
        if ( params.isEnhancedCondition() )
        {
            return getItemsSqlForEnhancedConditions( params, helper );
        }

        // Creates a map grouping queryItems referring to repeatable stages and
        // those referring to non-repeatable stages
        // Only for enrollments, for events all query items are treated as
        // non-repeatable
        Map<Boolean, List<QueryItem>> itemsByRepeatableFlag = Stream.concat(
            params.getItems().stream(), params.getItemFilters().stream() )
            .filter( QueryItem::hasFilter )
            .collect( groupingBy(
                queryItem -> queryItem.hasRepeatableStageParams() && params.getEndpointItem() == ENROLLMENT ) );

        // groups repeatable conditions based on PSI.DEID
        Map<String, List<String>> repeatableConditionsByIdentifier = asSqlCollection( itemsByRepeatableFlag.get( true ),
            params )
                .collect( groupingBy(
                    IdentifiableSql::getIdentifier,
                    mapping( IdentifiableSql::getSql, toList() ) ) );

        // joins each group with OR
        Collection<String> orConditions = repeatableConditionsByIdentifier.values()
            .stream()
            .map( sameGroup -> joinSql( sameGroup, OR_JOINER ) )
            .collect( toList() );

        // non repeatable conditions
        Collection<String> andConditions = asSqlCollection( itemsByRepeatableFlag.get( false ), params )
            .map( IdentifiableSql::getSql )
            .collect( toList() );

        if ( orConditions.isEmpty() && andConditions.isEmpty() )
        {
            return "";
        }

        return helper.whereAnd() + " " + joinSql( Stream.concat(
            orConditions.stream(),
            andConditions.stream() ), AND_JOINER );

    }

    /**
     * joins a stream of conditions using given join function, returns empty
     * string if collection is empty
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
        return hlp.whereAnd() + " " + String.join( _AND_, sqlConditionByGroup.values() );
    }

    /**
     * joins a collection of conditions using given join function, returns empty
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
        return emptyIfNull( queryItems )
            .stream()
            .map( queryItem -> toIdentifiableSql( queryItem, params ) );
    }

    /**
     * Converts given queryItem into IdentifiableSql joining its filters using
     * AND.
     */
    private IdentifiableSql toIdentifiableSql( QueryItem queryItem, EventQueryParams params )
    {
        return IdentifiableSql.builder()
            .identifier( getIdentifier( queryItem ) )
            .sql( toSql( queryItem, params ) )
            .build();
    }

    /**
     * Converts given queryItem into sql joining its filters using AND.
     */
    private String toSql( QueryItem queryItem, EventQueryParams params )
    {
        return queryItem.getFilters().stream()
            .map( filter -> toSql( queryItem, filter, params ) )
            .collect( joining( _AND_ ) );
    }

    /**
     * returns PSID.ITEM_ID of given queryItem.
     */
    private String getIdentifier( QueryItem queryItem )
    {
        String programStageId = Optional.of( queryItem )
            .map( QueryItem::getProgramStage )
            .map( BaseIdentifiableObject::getUid )
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
        String field = getSelectSql( filter, item, params.getEarliestStartDate(),
            params.getLatestEndDate() );

        if ( IN.equals( filter.getOperator() ) )
        {
            InQueryFilter inQueryFilter = new InQueryFilter( field,
                statementBuilder.encode( filter.getFilter(), false ), item.isText() );
            return inQueryFilter.getSqlFilter();
        }
        else
        {
            // NV filter has its own specific logic, so we should skip values
            // comparisons when NV is set as filter.
            if ( !NV.equals( filter.getFilter() ) )
            {
                switch ( filter.getOperator() )
                {
                case NEQ:
                case NE:
                case NIEQ:
                    return "(" + field + " is null or " + field + SPACE + filter.getSqlOperator() + SPACE
                        + getSqlFilter( filter, item ) + ") ";
                }
            }

            return field + SPACE + filter.getSqlOperator() + SPACE + getSqlFilter( filter, item ) + SPACE;
        }
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
