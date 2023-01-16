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
package org.hisp.dhis.analytics.orgunit.data;

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.system.util.SqlUtils.quote;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.orgunit.OrgUnitAnalyticsManager;
import org.hisp.dhis.analytics.orgunit.OrgUnitQueryParams;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.analytics.orgunit.OrgUnitAnalyticsManager" )
@RequiredArgsConstructor
public class JdbcOrgUnitAnalyticsManager
    implements OrgUnitAnalyticsManager
{
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Integer> getOrgUnitData( OrgUnitQueryParams params )
    {
        Map<String, Integer> dataMap = new HashMap<>();

        List<String> columns = getMetadataColumns( params );

        String sql = getQuerySql( params );

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        while ( rowSet.next() )
        {
            StringBuilder key = new StringBuilder();

            for ( String column : columns )
            {
                key.append( rowSet.getString( column ) ).append( DIMENSION_SEP );
            }

            key.deleteCharAt( key.length() - 1 );

            int value = rowSet.getInt( "count" );

            dataMap.put( key.toString(), value );
        }

        return dataMap;
    }

    /**
     * Returns metadata column names for the given query.
     *
     * @param params the {@link OrgUnitQueryParams}.
     * @return a list of column names.
     */
    private List<String> getMetadataColumns( OrgUnitQueryParams params )
    {
        List<String> columns = Lists.newArrayList( "orgunit" );
        params.getOrgUnitGroupSets().forEach( ougs -> columns.add( ougs.getUid() ) );
        return columns;
    }

    /**
     * Returns a SQL query based on the given query.
     *
     * @param params the {@link OrgUnitQueryParams}.
     * @return a SQL query.
     */
    private String getQuerySql( OrgUnitQueryParams params )
    {
        String levelCol = String.format( "ous.uidlevel%d", params.getOrgUnitLevel() );

        List<String> orgUnits = params.getOrgUnits().stream()
            .map( OrganisationUnit::getUid )
            .collect( Collectors.toList() );

        List<String> quotedGroupSets = params.getOrgUnitGroupSets().stream()
            .map( OrganisationUnitGroupSet::getUid )
            .map( uid -> quote( "ougs", uid ) )
            .collect( Collectors.toList() );

        String sql = "select " + levelCol + " as orgunit, " + getCommaDelimitedString( quotedGroupSets ) + ", " +
            "count(ougs.organisationunitid) as count " +
            "from " + quote( "_orgunitstructure" ) + " ous " +
            "inner join " + quote( "_organisationunitgroupsetstructure" ) + " " +
            "ougs on ous.organisationunitid = ougs.organisationunitid " +
            "where " + levelCol + " in (" + getQuotedCommaDelimitedString( orgUnits ) + ") " +
            "group by " + levelCol + ", " + getCommaDelimitedString( quotedGroupSets ) + ";";

        return sql;
    }
}
