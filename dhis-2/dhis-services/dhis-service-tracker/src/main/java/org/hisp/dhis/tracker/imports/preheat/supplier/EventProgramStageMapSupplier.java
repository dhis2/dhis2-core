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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * This supplier adds to the pre-heat object a List of all Program Stages UIDs
 * that have at least ONE event that is not logically deleted ('deleted = true')
 * and the status is not 'SKIPPED' among the Program Stages and the enrollments
 * present in the payload.
 *
 * @author Luciano Fiandesio
 */
@Component
public class EventProgramStageMapSupplier
    extends JdbcAbstractPreheatSupplier
{
    private static final String PS_UID = "programStageUid";

    private static final String PI_UID = "enrollmentUid";

    private static final String SQL = "select distinct ps.uid as " + PS_UID + ", pi.uid as " + PI_UID + " " +
        " from programinstance as pi " +
        " join programstage as ps on pi.programid = ps.programid " +
        " join event as psi on pi.programinstanceid = psi.programinstanceid " +
        " where psi.deleted = false " +
        " and psi.status != 'SKIPPED' " +
        " and ps.programstageid = psi.programstageid " +
        " and ps.uid in (:programStageUids) " +
        " and pi.uid in (:enrollmentUids) ";

    protected EventProgramStageMapSupplier( JdbcTemplate jdbcTemplate )
    {
        super( jdbcTemplate );
    }

    @Override
    public void preheatAdd( TrackerImportParams params, TrackerPreheat preheat )
    {
        if ( params.getEvents().isEmpty() )
        {
            return;
        }

        List<String> notRepeatableProgramStageUids = params.getEvents().stream()
            .map( Event::getProgramStage )
            .filter( Objects::nonNull )
            .map( preheat::getProgramStage )
            .filter( Objects::nonNull )
            .filter( ps -> ps.getProgram().isRegistration() )
            .filter( ps -> !ps.getRepeatable() )
            .map( ProgramStage::getUid )
            .distinct()
            .collect( Collectors.toList() );

        List<String> enrollmentUids = params.getEvents().stream()
            .map( Event::getEnrollment )
            .filter( Objects::nonNull )
            .distinct()
            .collect( Collectors.toList() );

        if ( !notRepeatableProgramStageUids.isEmpty() && !enrollmentUids.isEmpty() )
        {
            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue( "programStageUids", notRepeatableProgramStageUids );
            parameters.addValue( "enrollmentUids", enrollmentUids );
            jdbcTemplate.query( SQL, parameters, (RowCallbackHandler) rs -> preheat
                .addProgramStageWithEvents( rs.getString( PS_UID ), rs.getString( PI_UID ) ) );
        }
    }
}
