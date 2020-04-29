package org.hisp.dhis.dxf2.events.event.context;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
@Component( "workContextOrgUnitsSupplier" )
public class OrganisationUnitSupplier extends AbstractSupplier<Map<String, OrganisationUnit>>
{
    public OrganisationUnitSupplier( @Qualifier( "readOnlyJdbcTemplate" ) JdbcTemplate jdbcTemplate )
    {
        super( jdbcTemplate );
    }

    @Override
    public Map<String, OrganisationUnit> get( List<Event> events )
    {
        // @formatter:off
        // Collect all the org unit uids to pass as SQL query argument
        final Set<String> orgUnitUids = events.stream()
                .filter( e -> e.getOrgUnit() != null ).map( Event::getOrgUnit )
                .collect( Collectors.toSet() );

        // Create a map: org unit uid -> List [event uid]
        Multimap<String, String> orgUnitToEvent = HashMultimap.create();
        for ( Event event : events )
        {
            orgUnitToEvent.put( event.getOrgUnit(), event.getUid() );
        }
        // @formatter:on

        final String sql = "select ou.organisationunitid, ou.uid, ou.code, ou.path, ou.hierarchylevel from organisationunit ou where ou.uid in (:ids)";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "ids", orgUnitUids );

        return jdbcTemplate.query( sql, parameters, ( ResultSet rs ) -> {
            Map<String, OrganisationUnit> results = new HashMap<>();

            while ( rs.next() )
            {
                OrganisationUnit ou = new OrganisationUnit();
                ou.setId( rs.getLong( "organisationunitid" ) );
                ou.setUid( rs.getString( "uid" ) );
                ou.setCode( rs.getString( "code" ) );
                ou.setPath( rs.getString( "path" ) );
                ou.setHierarchyLevel( rs.getInt( "hierarchylevel" ) );
                for ( String event : orgUnitToEvent.get( ou.getUid() ) )
                {
                    results.put( event, ou );
                }

            }
            return results;
        } );
    }
}
