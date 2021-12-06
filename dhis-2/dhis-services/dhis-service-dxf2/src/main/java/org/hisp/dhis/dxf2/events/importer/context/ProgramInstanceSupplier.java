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
package org.hisp.dhis.dxf2.events.importer.context;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component( "workContextProgramInstancesSupplier" )
public class ProgramInstanceSupplier extends AbstractSupplier<Map<String, ProgramInstance>>
{
    public ProgramInstanceSupplier( NamedParameterJdbcTemplate jdbcTemplate )
    {
        super( jdbcTemplate );
    }

    public Map<String, ProgramInstance> get( List<Enrollment> events )
    {
        if ( isEmpty( events ) )
        {
            return new HashMap<>();
        }

        Set<String> programInstanceUids = events.stream()
            .map( Enrollment::getEnrollment )
            .filter( StringUtils::isNotEmpty ).collect( Collectors.toSet() );

        return getProgramInstancesByUid(
            programInstanceUids );
    }

    private Map<String, ProgramInstance> getProgramInstancesByUid(
        Set<String> uids )
    {
        if ( isEmpty( uids ) )
        {
            return new HashMap<>();
        }

        final String sql = "select pi.programinstanceid, pi.status, pi.uid as programinstance_uid, pi.programid, pi.uid, pi.created, t.trackedentityinstanceid as tei_id, t.uid as tei_uid,  p.uid as program_uid "
            + "from programinstance pi join trackedentityinstance t on pi.trackedentityinstanceid = t.trackedentityinstanceid "
            + "join program p on p.programid = pi.programid "
            + "where pi.uid in (:ids)";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "ids", uids );

        return jdbcTemplate.query( sql, parameters, ( ResultSet rs ) -> {
            Map<String, ProgramInstance> results = new HashMap<>();

            while ( rs.next() )
            {
                ProgramInstance pi = mapFromResultset( rs );
                results.put( pi.getUid(), pi );
            }
            return results;
        } );

    }

    private ProgramInstance mapFromResultset( ResultSet rs )
        throws SQLException
    {
        ProgramInstance pi = new ProgramInstance();
        pi.setUid( rs.getString( "programinstance_uid" ) );
        pi.setId( rs.getLong( "programinstanceid" ) );
        pi.setCreated( rs.getDate( "created" ) );
        pi.setStatus( ProgramStatus.valueOf( rs.getString( "status" ) ) );

        String teiUid = rs.getString( "tei_uid" );

        if ( teiUid != null )
        {
            TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
            trackedEntityInstance.setId( rs.getLong( "tei_id" ) );
            trackedEntityInstance.setUid( teiUid );

            pi.setEntityInstance( trackedEntityInstance );
        }

        return pi;
    }
}
