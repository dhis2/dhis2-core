package org.hisp.dhis.analytics.data;

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

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.RawAnalyticsManager;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.google.common.collect.Sets;

/**
 * This class is responsible for retrieving raw data from the
 * analytics tables.
 *
 * @author Lars Helge Overland
 */
public class JdbcRawAnalyticsManager
    implements RawAnalyticsManager
{
    private static final Log log = LogFactory.getLog( JdbcRawAnalyticsManager.class );
    
    private static final Set<String> DIMS_IGNORE_CRITERIA = Sets.newHashSet( DimensionalObject.PERIOD_DIM_ID );
    private static final String DIM_NAME_OU = "ou.path";
    
    @Resource( name = "readOnlyJdbcTemplate" )
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StatementBuilder statementBuilder;

    // -------------------------------------------------------------------------
    // RawAnalyticsManager implementation
    // -------------------------------------------------------------------------

    @Override
    public Grid getRawDataValues( DataQueryParams params, Grid grid )
    {        
        List<DimensionalObject> dimensions = params.getDimensions();
        
        String sql = getStatement( params );
        
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

    private String getStatement( DataQueryParams params )
    {
        List<String> dimensionColumns = params.getDimensions()
            .stream().map( d -> statementBuilder.columnQuote( d.getDimensionName() ) )
            .collect( Collectors.toList() );
        
        SqlHelper sqlHelper = new SqlHelper();
        
        String sql = 
            "select " + StringUtils.join( dimensionColumns, ", " ) + ", " + DIM_NAME_OU + ", value " +
            "from " + params.getPartitions().getSinglePartition() + " ax " +
            "inner join organisationunit ou on ax.ou = ou.uid ";
        
        for ( DimensionalObject dim : params.getDimensions() )
        {
            if ( !dim.getItems().isEmpty() && !dim.isFixed() && !DIMS_IGNORE_CRITERIA.contains( dim.getDimension() ) )
            {
                String col = statementBuilder.columnQuote( dim.getDimensionName() );

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
                    sql += sqlHelper.whereAnd() + " " + col + " in (" + getQuotedCommaDelimitedString( getUids( dim.getItems() ) ) + ") ";
                }                
            }
        }
        
        return sql;
    }
}
