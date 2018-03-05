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

import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.removeLastOr;
import static org.hisp.dhis.system.util.DateUtils.getMediumDateString;

import java.util.List;

import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.commons.util.ExpressionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;

import com.google.common.collect.Sets;

/**
 * @author Markus Bekken
 */
public class JdbcEnrollmentAnalyticsManager 
    extends AbstractJdbcEventAnalyticsManager
        implements EnrollmentAnalyticsManager
{
    /**
     * Returns a from SQL clause for the given analytics table partition.
     * 
     * @param params the {@link EventQueryParams}.
     */
    protected String getFromClause( EventQueryParams params )
    {
        return " from " + params.getTableName() + " as enrollmenttable ";
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
                // Event data joined in the program indicator service, as part of translating the expression or filter for the program indicator
                
                if ( !boundary.isEventDateBoundary() )
                {
                    sql += sqlHelper.whereAnd() + " " + boundary.getSqlCondition( params.getEarliestStartDate(), params.getLatestEndDate() ) + " ";
                }
            }
            
            // Filter for evaluating enrollments which have any events in the boundary period
            
            if ( params.getProgramIndicator().hasEventBoundary() )
            {
                sql += sqlHelper.whereAnd() + "( select count * from analytics_event_" + params.getProgramIndicator().getProgram().getUid() + 
                    " where pi = enrollmenttable.pi " + 
                    ( params.getProgramIndicator().getEndEventBoundary() != null ? ( sqlHelper.whereAnd() + " " + 
                    params.getProgramIndicator().getEndEventBoundary().getSqlCondition( params.getEarliestStartDate(), params.getLatestEndDate() ) + " " ) : "") + 
                    ( params.getProgramIndicator().getStartEventBoundary() != null ? ( sqlHelper.whereAnd() + " "  + 
                    params.getProgramIndicator().getStartEventBoundary().getSqlCondition( params.getEarliestStartDate(), params.getLatestEndDate() ) + " " ) : "") + ") > 0";
            }
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
                sql += sqlHelper.whereAnd() + " " + statementBuilder.columnQuote( params.getPeriodType().toLowerCase() ) + " in (" + getQuotedCommaDelimitedString( getUids( params.getDimensionOrFilterItems( PERIOD_DIM_ID ) ) ) + ") ";
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
            String filter = programIndicatorService.getAnalyticsSQl( params.getProgramIndicator().getFilter(), 
                params.getProgramIndicator(), false, params.getEarliestStartDate(), params.getLatestEndDate() );
            
            String sqlFilter = ExpressionUtils.asSql( filter );
            
            sql += "and (" + sqlFilter + ") ";
        }
        
        if ( params.hasProgramIndicatorDimension() )
        {
            String anyValueFilter = programIndicatorService.getAnyValueExistsClauseAnalyticsSql( params.getProgramIndicator().getExpression(), params.getProgramIndicator().getAnalyticsType() );
            
            if ( anyValueFilter != null )
            {
                sql += "and (" + anyValueFilter + ") ";
            }
        }
        
        // ---------------------------------------------------------------------
        // Various filters
        // ---------------------------------------------------------------------

        if ( params.hasProgramStatus() )
        {
            sql += "and pistatus = '" + params.getProgramStatus().name() + "' ";
        }

        if ( params.hasEventStatus() )
        {
            sql += "and psistatus = '" + params.getEventStatus().name() + "' ";
        }

        if ( params.isCoordinatesOnly() )
        {
            sql += "and (longitude is not null and latitude is not null) ";
        }
        
        if ( params.isGeometryOnly() )
        {
            sql += "and " + statementBuilder.columnQuote( params.getCoordinateField() ) + " is not null ";
        }
        
        if ( params.isCompletedOnly() )
        {
            sql += "and completeddate is not null ";
        }
        
        if ( params.hasBbox() )
        {
            sql += "and " + statementBuilder.columnQuote( params.getCoordinateField() ) + " && ST_MakeEnvelope(" + params.getBbox() + ",4326) ";
        }
        
        return sql;
    }
}
