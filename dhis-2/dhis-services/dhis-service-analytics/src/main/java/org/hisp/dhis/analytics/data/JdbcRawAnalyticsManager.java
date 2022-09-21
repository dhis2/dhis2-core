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
import static org.hisp.dhis.analytics.DataQueryParams.PERIOD_END_DATE_ID;
import static org.hisp.dhis.analytics.DataQueryParams.PERIOD_END_DATE_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.PERIOD_START_DATE_ID;
import static org.hisp.dhis.analytics.DataQueryParams.PERIOD_START_DATE_NAME;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.RawAnalyticsManager;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Class responsible for retrieving raw data from the analytics tables.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Component( "org.hisp.dhis.analytics.RawAnalyticsManager" )
public class JdbcRawAnalyticsManager
    implements RawAnalyticsManager
{
    private static final String DIM_NAME_OU = "ou.path";

    private final JdbcTemplate jdbcTemplate;

    public JdbcRawAnalyticsManager( @Qualifier( "readOnlyJdbcTemplate" )
    final JdbcTemplate jdbcTemplate )
    {
        checkNotNull( jdbcTemplate );

        this.jdbcTemplate = jdbcTemplate;
    }

    // -------------------------------------------------------------------------
    // RawAnalyticsManager implementation
    // -------------------------------------------------------------------------

    @Override
    public Grid getRawDataValues( DataQueryParams params, Grid grid )
    {
        Assert.isTrue( params.hasStartEndDate(), "Start and end dates must be specified" );

        List<DimensionalObject> dimensions = new ArrayList<>();
        dimensions.addAll( params.getDimensions() );
        dimensions.addAll( params.getOrgUnitLevelsAsDimensions() );

        if ( params.isIncludePeriodStartEndDates() )
        {
            dimensions.add( new BaseDimensionalObject( PERIOD_START_DATE_ID, DimensionType.STATIC,
                PERIOD_START_DATE_NAME, new ArrayList<>() ) );
            dimensions.add( new BaseDimensionalObject( PERIOD_END_DATE_ID, DimensionType.STATIC, PERIOD_END_DATE_NAME,
                new ArrayList<>() ) );
        }

        String sql = getSelectStatement( params, dimensions );

        log.debug( "Get raw data SQL: " + sql );

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        while ( rowSet.next() )
        {
            grid.addRow();

            for ( DimensionalObject dim : dimensions )
            {
                grid.addValue( rowSet.getString( dim.getDimensionName() ) );
            }

            grid.addValue( rowSet.getDouble( "value" ) );
        }

        return grid;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns a SQL select statement.
     *
     * @param params the {@link DataQueryParams}.
     * @param dimensions the list of dimensions.
     * @return a SQL select statement.
     */
    private String getSelectStatement( DataQueryParams params, List<DimensionalObject> dimensions )
    {
        String idScheme = ObjectUtils.firstNonNull( params.getOutputIdScheme(), IdScheme.UID ).getIdentifiableString()
            .toLowerCase();

        List<String> dimensionColumns = dimensions.stream()
            .map( d -> asColumnSelect( d, idScheme ) )
            .collect( Collectors.toList() );

        SqlHelper sqlHelper = new SqlHelper();

        String sql = "select " + StringUtils.join( dimensionColumns, ", " ) + ", " + DIM_NAME_OU + ", value " +
            "from " + params.getTableName() + " as " + ANALYTICS_TBL_ALIAS + " " +
            "inner join organisationunit ou on ax.ou = ou.uid " +
            "inner join _orgunitstructure ous on ax.ou = ous.organisationunituid " +
            "inner join _periodstructure ps on ax.pe = ps.iso ";

        for ( DimensionalObject dim : dimensions )
        {
            if ( !dim.getItems().isEmpty() && !dim.isFixed() )
            {
                String col = quote( dim.getDimensionName() );

                if ( DimensionalObject.ORGUNIT_DIM_ID.equals( dim.getDimension() ) )
                {
                    sql += sqlHelper.whereAnd() + " (";

                    for ( DimensionalItemObject item : dim.getItems() )
                    {
                        OrganisationUnit unit = (OrganisationUnit) item;

                        sql += DIM_NAME_OU + " like '" + unit.getPath() + "%' or ";
                    }

                    sql = TextUtils.removeLastOr( sql ) + ") ";
                }
                else
                {
                    sql += sqlHelper.whereAnd() + " " + col + " in ("
                        + getQuotedCommaDelimitedString( getUids( dim.getItems() ) ) + ") ";
                }
            }
        }

        sql += sqlHelper.whereAnd() + " " +
            "ps.startdate >= '" + DateUtils.getMediumDateString( params.getStartDate() ) + "' and " +
            "ps.enddate <= '" + DateUtils.getMediumDateString( params.getEndDate() ) + "' ";

        return sql;
    }

    /**
     * Converts the given dimension to a column select statement according to
     * the given identifier scheme.
     *
     * @param dimension the dimensional object.
     * @param idScheme the identifier scheme.
     * @return a column select statement.
     */
    private String asColumnSelect( DimensionalObject dimension, String idScheme )
    {
        if ( DimensionType.ORGANISATION_UNIT == dimension.getDimensionType() )
        {
            return ("ou." + idScheme + " as " + quote( dimension.getDimensionName() ));
        }
        else if ( DimensionType.ORGANISATION_UNIT_LEVEL == dimension.getDimensionType() )
        {
            int level = AnalyticsUtils.getLevelFromOrgUnitDimensionName( dimension.getDimensionName() );

            return ("ous." + idScheme + "level" + level + " as " + quote( dimension.getDimensionName() ));
        }
        else
        {
            return quote( dimension.getDimensionName() );
        }
    }
}
