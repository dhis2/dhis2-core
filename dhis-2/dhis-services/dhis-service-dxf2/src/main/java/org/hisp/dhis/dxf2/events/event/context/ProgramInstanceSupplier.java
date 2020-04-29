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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component( "workContextProgramInstancesSupplier" )
public class ProgramInstanceSupplier extends AbstractSupplier<Map<String, ProgramInstance>>
{
    private final ProgramSupplier programSupplier;

    public ProgramInstanceSupplier( @Qualifier( "readOnlyJdbcTemplate" ) JdbcTemplate jdbcTemplate,
        ProgramSupplier programSupplier )
    {
        super( jdbcTemplate );
        this.programSupplier = programSupplier;
    }

    @Override
    public Map<String, ProgramInstance> get( List<Event> events )
    {
        // @formatter:off
        // Collect all the program instance UIDs to pass as SQL query argument
        Set<String> programInstanceUids = events.stream()
                .filter( e -> e.getEnrollment() != null )
                .map( Event::getEnrollment ).collect( Collectors.toSet() );

        // Create a bi-directional map tei uid -> org unit id
        Map<String, String> programInstanceToEvent = events.stream()
                .filter( e -> e.getEnrollment() != null )
                .collect( Collectors.toMap( Event::getEnrollment, Event::getUid  ) );
        // @formatter:on

        if ( !programInstanceUids.isEmpty() )
        {
            final String sql = "select pi.programinstanceid, pi.programid, pi.uid, t.uid as tei_uid, ou.uid as tei_ou_uid "
                + "from programinstance pi join trackedentityinstance t on pi.trackedentityinstanceid = t.trackedentityinstanceid "
                + "join organisationunit ou on t.organisationunitid = ou.organisationunitid "
                + "where pi.uid in (:ids)";
            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue( "ids", programInstanceUids );

            return jdbcTemplate.query( sql, parameters, ( ResultSet rs ) -> {
                Map<String, ProgramInstance> results = new HashMap<>();

                while ( rs.next() )
                {
                    ProgramInstance pi = new ProgramInstance();
                    pi.setId( rs.getLong( "programinstanceid" ) );
                    pi.setUid( rs.getString( "uid" ) );
                    pi.setProgram(
                        getProgramById( rs.getLong( "programid" ), programSupplier.get( events ).values() ) );
                    TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
                    trackedEntityInstance.setUid( rs.getString( "tei_uid" ) );
                    OrganisationUnit organisationUnit = new OrganisationUnit();
                    organisationUnit.setUid( rs.getString( "tei_ou_uid" ) );
                    trackedEntityInstance.setOrganisationUnit( organisationUnit );
                    pi.setEntityInstance( trackedEntityInstance );

                    results.put( programInstanceToEvent.get( pi.getUid() ), pi );

                }
                return results;
            } );
        }
        return new HashMap<>();
    }

    private Program getProgramById( long id, Collection<Program> programs )
    {
        return programs.stream().filter( p -> p.getId() == id ).findFirst().orElse( null );
    }
}
