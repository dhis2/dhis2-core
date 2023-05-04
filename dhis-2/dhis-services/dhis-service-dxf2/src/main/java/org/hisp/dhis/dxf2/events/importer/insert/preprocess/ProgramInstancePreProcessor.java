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
package org.hisp.dhis.dxf2.events.importer.insert.preprocess;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.importer.Processor;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The goal of this Pre-processor is to assign a Program Instance (Enrollment)
 * to the Event getting processed. If the Program Instance can not be assigned,
 * the Event will not pass validation.
 *
 * @author Luciano Fiandesio
 */
@Component
public class ProgramInstancePreProcessor implements Processor
{
    @Override
    public void process( Event event, WorkContext ctx )
    {
        EnrollmentStore enrollmentStore = ctx.getServiceDelegator().getEnrollmentStore();

        Program program = ctx.getProgramsMap().get( event.getProgram() );

        if ( program == null )
        {
            return; // Program is a mandatory value, it will be caught by the
                   // validation
        }

        Enrollment enrollment = ctx.getProgramInstanceMap().get( event.getUid() );
        final Optional<TrackedEntityInstance> trackedEntityInstance = ctx.getTrackedEntityInstance( event.getUid() );

        if ( program.isRegistration() && enrollment == null )
        {
            List<Enrollment> enrollments = new ArrayList<>(
                enrollmentStore.get( trackedEntityInstance.orElse( null ), program, ProgramStatus.ACTIVE ) );

            if ( enrollments.size() == 1 )
            {
                event.setEnrollment( enrollments.get( 0 ).getUid() );
                ctx.getProgramInstanceMap().put( event.getUid(), enrollments.get( 0 ) );
            }
        }
        else if ( program.isWithoutRegistration() && enrollment == null )
        {
            List<Enrollment> enrollments = getProgramInstances( ctx.getServiceDelegator().getJdbcTemplate(),
                program, ProgramStatus.ACTIVE );

            // the "original" event import code creates a Program Instance, if
            // none is found
            // but this is no longer needed, since a Program POST-CREATION hook
            // takes care of that
            if ( enrollments.size() == 1 )
            {
                event.setEnrollment( enrollments.get( 0 ).getUid() );
                ctx.getProgramInstanceMap().put( event.getUid(), enrollments.get( 0 ) );
            }
            // If more than one Program Instance is present, the validation will
            // detect it later
        }
    }

    private List<Enrollment> getProgramInstances( JdbcTemplate jdbcTemplate, Program program,
        ProgramStatus status )
    {
        final String sql = "select pi.programinstanceid, pi.programid, pi.uid "
            + "from programinstance pi "
            + "where pi.programid = ? and pi.status = ?";

        return jdbcTemplate.query( sql, new Object[] { program.getId(), status.name() }, ( ResultSet rs ) -> {
            List<Enrollment> results = new ArrayList<>();

            while ( rs.next() )
            {
                Enrollment pi = new Enrollment();
                pi.setId( rs.getLong( "programinstanceid" ) );
                pi.setUid( rs.getString( "uid" ) );
                pi.setProgram( program );
                results.add( pi );

            }
            return results;
        } );
    }
}
