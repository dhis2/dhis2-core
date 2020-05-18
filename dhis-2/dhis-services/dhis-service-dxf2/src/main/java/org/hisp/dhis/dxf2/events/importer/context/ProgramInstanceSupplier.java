package org.hisp.dhis.dxf2.events.importer.context;

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

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
@Component( "workContextProgramInstancesSupplier" )
public class ProgramInstanceSupplier extends AbstractSupplier<Map<String, ProgramInstance>>
{
    private final ProgramSupplier programSupplier;

    public ProgramInstanceSupplier( NamedParameterJdbcTemplate jdbcTemplate, ProgramSupplier programSupplier )
    {
        super( jdbcTemplate );
        this.programSupplier = programSupplier;
    }

    @Override
    public Map<String, ProgramInstance> get( ImportOptions importOptions, List<Event> events )
    {
        if ( events == null )
        {
            return new HashMap<>();
        }

        // @formatter:off
        // Collect all the program instance UIDs to pass as SQL query argument
        // TODO: Maikel, ask Luciano about it. Should we use also "program" ?
        Set<String> programInstanceUids = events.stream()
                .filter( e -> e.getEnrollment() != null )
                .map( Event::getEnrollment ).collect( Collectors.toSet() );

        // Create a bi-directional map enrollment uid -> event uid
        Multimap<String, String> programInstanceToEvent = HashMultimap.create();
        for ( Event event : events )
        {
            programInstanceToEvent.put( event.getEnrollment(), event.getUid() );
        }
        // @formatter:on

        Map<String, ProgramInstance> programInstances = new HashMap<>();

        if ( !programInstanceUids.isEmpty() )
        {
            // Collect all the Program Stage Instances specified in the Events (enrollment property)
            programInstances = getProgramInstancesByUid( importOptions, events, programInstanceToEvent,
                programInstanceUids );
        }
        // Collect all the Program Stage Instances by event uid
        final Map<String, ProgramInstance> programInstancesByEvent = getProgramInstanceByEvent( importOptions, events );
        
        // if the Event does not have the "enrollment" property set OR enrollment is pointing to an invalid UID
        // use the Program Instance connected to the Event. This only makes sense if the Program Stage Instance
        // already exist in the database
        if ( programInstancesByEvent.size() > 0 )
        {
            for ( Event event : events )
            {
                if ( !programInstances.containsKey( event.getUid() ) )
                {
                    if ( programInstancesByEvent.containsKey( event.getUid() ) )
                    {
                        programInstances.put( event.getUid(), programInstancesByEvent.get( event.getUid() ) );
                    }
                }
            }
        }

        return programInstances;
    }

    private Program getProgramById( long id, Collection<Program> programs )
    {
        return programs.stream().filter( p -> p.getId() == id ).findFirst().orElse( null );
    }

    private Map<String, ProgramInstance> getProgramInstanceByEvent( ImportOptions importOptions, List<Event> events )
    {
        final Set<String> eventUids = events.stream().map( Event::getUid ).collect( Collectors.toSet() );
        if ( isEmpty( eventUids ) )
        {
            return new HashMap<>();
        }
        
        final String sql = "select psi.uid as psi_uid, pi.programinstanceid, pi.programid, pi.uid , t.uid as tei_uid, "
            + "ou.uid as tei_ou_uid, ou.path as tei_ou_path from programinstance pi "
            + "left outer join trackedentityinstance t on pi.trackedentityinstanceid = t.trackedentityinstanceid "
            + "left join organisationunit ou on t.organisationunitid = ou.organisationunitid "
            + "join programstageinstance psi on pi.programinstanceid = psi.programinstanceid "
            + "where psi.uid in (:ids)";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "ids", eventUids );

        return jdbcTemplate.query( sql, parameters, ( ResultSet rs ) -> {
            Map<String, ProgramInstance> results = new HashMap<>();

            while ( rs.next() )
            {
                results.put( rs.getString( "psi_uid" ), mapFromResultset( rs, importOptions, events ) );
            }
            return results;
        } );
    }

    private Map<String, ProgramInstance> getProgramInstancesByUid( ImportOptions importOptions, List<Event> events,
        Multimap<String, String> programInstanceToEvent, Set<String> uids )
    {

        final String sql = "select pi.programinstanceid, pi.programid, pi.uid, t.uid as tei_uid, "
            + "ou.uid as tei_ou_uid, ou.path as tei_ou_path "
            + "from programinstance pi join trackedentityinstance t on pi.trackedentityinstanceid = t.trackedentityinstanceid "
            + "join organisationunit ou on t.organisationunitid = ou.organisationunitid where pi.uid in (:ids)";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "ids", uids );

        return jdbcTemplate.query( sql, parameters, ( ResultSet rs ) -> {
            Map<String, ProgramInstance> results = new HashMap<>();

            while ( rs.next() )
            {
                ProgramInstance pi = mapFromResultset( rs, importOptions, events );

                for ( String event : programInstanceToEvent.get( pi.getUid() ) )
                {
                    results.put( event, pi );
                }
            }
            return results;
        } );

    }

    private ProgramInstance mapFromResultset( ResultSet rs, ImportOptions importOptions, List<Event> events )
        throws SQLException
    {
        ProgramInstance pi = new ProgramInstance();
        pi.setId( rs.getLong( "programinstanceid" ) );
        pi.setUid( rs.getString( "uid" ) );
        pi.setProgram(
            getProgramById( rs.getLong( "programid" ), programSupplier.get( importOptions, events ).values() ) );
        String teiUid = rs.getString( "tei_uid" );

        if ( teiUid != null )
        {
            OrganisationUnit organisationUnit = new OrganisationUnit();
            organisationUnit.setUid( rs.getString( "tei_ou_uid" ) );
            organisationUnit.setParent( SupplierUtils.getParentHierarchy( organisationUnit, rs.getString( "tei_ou_path" ) ) );

            TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
            trackedEntityInstance.setUid( rs.getString( "tei_uid" ) );
            trackedEntityInstance.setOrganisationUnit( organisationUnit );
            pi.setEntityInstance( trackedEntityInstance );
        }


        return pi;
    }
}
