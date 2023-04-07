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
package org.hisp.dhis.dxf2.events.importer.context;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component( "workContextProgramOrgUnitsSupplier" )
public class ProgramOrgUnitSupplier extends AbstractSupplier<Map<Long, List<Long>>>
{
    public ProgramOrgUnitSupplier( NamedParameterJdbcTemplate jdbcTemplate )
    {
        super( jdbcTemplate );
    }

    public Map<Long, List<Long>> get( ImportOptions importOptions, List<Event> events,
        Map<String, OrganisationUnit> orgUniMap )
    {
        if ( events == null )
        {
            return new HashMap<>();
        }

        //
        // Collect all the org unit IDs to pass as SQL query
        // argument
        //
        final Set<Long> orgUnitIds = orgUniMap.values().stream().map( IdentifiableObject::getId )
            .collect( Collectors.toSet() );

        if ( isEmpty( orgUnitIds ) )
        {
            return new HashMap<>();
        }

        final String sql = "select programid, organisationunitid from program_organisationunits where organisationunitid in ( :ids )";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "ids", orgUnitIds );

        return jdbcTemplate.query( sql, parameters, rs -> {

            Map<Long, List<Long>> map = new HashMap<>();
            while ( rs.next() )
            {
                final Long pid = rs.getLong( "programid" );
                final Long ouid = rs.getLong( "organisationunitid" );

                if ( map.containsKey( pid ) )
                {
                    map.get( pid ).add( ouid );
                }
                else
                {
                    List<Long> ouids = new ArrayList<>();
                    ouids.add( ouid );
                    map.put( pid, ouids );
                }
            }

            return map;
        } );

    }

    @Override
    public Map<Long, List<Long>> get( ImportOptions importOptions, List<Event> events )
    {
        throw new NotImplementedException( "Use other get method" );
    }
}
