package org.hisp.dhis.analytics.event.data;

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

import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.*;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.removeLastOr;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.*;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.ExpressionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.system.util.MathUtils;
import org.locationtech.jts.util.Assert;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Markus Bekken
 */
@Slf4j
@Component( "org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager" )
public class JdbcEnrollmentAnalyticsManager
    extends AbstractJdbcEventAnalyticsManager
        implements EnrollmentAnalyticsManager
{
    private List<String> COLUMNS = Lists.newArrayList( "pi", "tei", "enrollmentdate", "incidentdate",
        "ST_AsGeoJSON(pigeometry)", "longitude", "latitude", "ouname", "oucode" );

    public JdbcEnrollmentAnalyticsManager( JdbcTemplate jdbcTemplate, StatementBuilder statementBuilder,
        ProgramIndicatorService programIndicatorService, ProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder )
    {
        super( jdbcTemplate, statementBuilder, programIndicatorService, programIndicatorSubqueryBuilder );
    }

    @Override
    public void getEnrollments( EventQueryParams params, Grid grid, int maxLimit )
    {
        withExceptionHandling( () -> getEnrollments( params, grid, getEventsOrEnrollmentsSql( params, maxLimit ) ) );
    }

    /**
     * Adds enrollments to the given grid based on the given parameters and SQL statement.
     *
     * @param params the {@link EventQueryParams}.
     * @param grid the {@link Grid}.
     * @param sql the SQL statement used to retrieve events.
     */
    private void getEnrollments( EventQueryParams params, Grid grid, String sql )
    {
        log.debug( String.format( "Analytics enrollment query SQL: %s", sql ) );

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
    public long getEnrollmentCount( EventQueryParams params )
    {
        String sql = "select count(pi) ";

        sql += getFromClause( params );

        sql += getWhereClause( params );

        long count = 0;

        try
        {
            log.debug( "Analytics enrollment count SQL: " + sql );

            count = jdbcTemplate.queryForObject( sql, Long.class );
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

        return count;
    }

    /**
     * Returns a from SQL clause for the given analytics table partition.
     *
     * @param params the {@link EventQueryParams}.
     */
    @Override
    protected String getFromClause( EventQueryParams params )
    {
        return " from " + params.getTableName() + " as " + ANALYTICS_TBL_ALIAS + " ";
    }

    /**
     * Returns a from and where SQL clause. If this is a program indicator with non-default boundaries, the relationship
     * with the reporting period is specified with where conditions on the enrollment or incident dates. If the default
     * boundaries is used, or the params does not include program indicators, the periods are joined in from the analytics
     * tables the normal way. A where clause can never have a mix of indicators with non-default boundaries and regular
     * analytics table periods.
     *
     * @param params the {@link EventQueryParams}.
     */
    @Override
    protected String getWhereClause( EventQueryParams params )
    {
        String sql = "";
        SqlHelper sqlHelper = new SqlHelper();

        // ---------------------------------------------------------------------
        // Periods
        // ---------------------------------------------------------------------

        if ( params.hasNonDefaultBoundaries() )
        {
            sql += statementBuilder.getBoundaryCondition( params.getProgramIndicator(), params.getEarliestStartDate(), params.getLatestEndDate(), sqlHelper );
        }
        else
        {
            if ( params.hasStartEndDate() )
            {
                sql += sqlHelper.whereAnd() + " enrollmentdate >= '" + getMediumDateString( params.getStartDate() ) + "' ";
                sql += "and enrollmentdate <= '" + getMediumDateString( params.getEndDate() ) + "' ";
            }
            else // Periods
            {
                sql += sqlHelper.whereAnd() + " " + quote( ANALYTICS_TBL_ALIAS, params.getPeriodType().toLowerCase() ) + " in (" + getQuotedCommaDelimitedString( getUids( params.getDimensionOrFilterItems( PERIOD_DIM_ID ) ) ) + ") ";
            }
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
            sql += sqlHelper.whereAnd() + " ou in (" + getQuotedCommaDelimitedString( getUids( params.getOrganisationUnitChildren() ) ) + ") ";
        }
        else // Descendants
        {
            sql += sqlHelper.whereAnd() + " (";

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

        List<DimensionalObject> dynamicDimensions = params.getDimensionsAndFilters(
            Sets.newHashSet( DimensionType.ORGANISATION_UNIT_GROUP_SET, DimensionType.CATEGORY ) );

        for ( DimensionalObject dim : dynamicDimensions )
        {
            String col = quoteAlias( dim.getDimensionName() );

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
                    sql += "and " + getSelectSql( item, params.getEarliestStartDate(), params.getLatestEndDate() ) + " " + filter.getSqlOperator() + " " + getSqlFilter( filter, item ) + " ";
                }
            }
        }

        for ( QueryItem item : params.getItemFilters() )
        {
            if ( item.hasFilter() )
            {
                for ( QueryFilter filter : item.getFilters() )
                {
                    sql += "and " + getSelectSql( item, params.getEarliestStartDate(), params.getLatestEndDate() ) + " " + filter.getSqlOperator() + " " + getSqlFilter( filter, item ) + " ";
                }
            }
        }

        // ---------------------------------------------------------------------
        // Filter expression
        // ---------------------------------------------------------------------

        if ( params.hasProgramIndicatorDimension() && params.getProgramIndicator().hasFilter() )
        {
            String filter = programIndicatorService.getAnalyticsSql( params.getProgramIndicator().getFilter(),
                params.getProgramIndicator(), params.getEarliestStartDate(), params.getLatestEndDate() );

            String sqlFilter = ExpressionUtils.asSql( filter );

            sql += "and (" + sqlFilter + ") ";
        }

        // ---------------------------------------------------------------------
        // Various filters
        // ---------------------------------------------------------------------

        if ( params.hasProgramStatus() )
        {
            sql += "and enrollmentstatus = '" + params.getProgramStatus().name() + "' ";
        }

        if ( params.isCoordinatesOnly() )
        {
            sql += "and (longitude is not null and latitude is not null) ";
        }

        if ( params.isGeometryOnly() )
        {
            sql += "and " + quoteAlias( params.getCoordinateField() ) + " is not null ";
        }

        if ( params.isCompletedOnly() )
        {
            sql += "and completeddate is not null ";
        }

        if ( params.hasBbox() )
        {
            sql += "and " + quoteAlias( params.getCoordinateField() ) + " && ST_MakeEnvelope(" + params.getBbox() + ",4326) ";
        }

        return sql;
    }

    @Override
    protected String getSelectClause( EventQueryParams params )
    {
        List<String> selectCols = ListUtils.distinctUnion( COLUMNS, getSelectColumns( params ) );

        return  "select " + StringUtils.join( selectCols, "," ) + " ";
    }

    /**
     * Returns an encoded column name wrapped in lower directive if not numeric
     * or boolean.
     *
     * @param item the {@link QueryItem}.
     */
    @Override
    protected String getColumn( QueryItem item )
    {
        String colName = item.getItemName();

        if ( item.hasProgramStage() )
        {
            colName = quote( colName );
            Assert.isTrue( item.hasProgram(), "Can not query item with program stage but no program:" + item.getItemName() );
            String eventTableName = "analytics_event_" + item.getProgram().getUid();
            return "(select " +  colName  + " from " + eventTableName +
            " where " + eventTableName + ".pi = " + ANALYTICS_TBL_ALIAS + ".pi " +
            "and " + colName + " is not null " + "and ps = '" + item.getProgramStage().getUid() + "' " +
            "order by executiondate " + "desc limit 1 )";
        }
        else
        {
            return quoteAlias( colName );
        }
    }

    @Override
    protected AnalyticsType getAnalyticsType()
    {
        return AnalyticsType.ENROLLMENT;
    }
}
