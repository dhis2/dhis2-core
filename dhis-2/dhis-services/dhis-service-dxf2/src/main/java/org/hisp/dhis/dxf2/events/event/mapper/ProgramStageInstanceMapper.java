package org.hisp.dhis.dxf2.events.event.mapper;

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

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.validation.WorkContext;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.util.DateUtils;

/**
 * @author Luciano Fiandesio
 */
public class ProgramStageInstanceMapper
    extends
    AbstractMapper<Event, ProgramStageInstance>
{
    private final ProgramStageInstanceNoteMapper noteMapper;
    private final ProgramStageInstanceDataValueMapper dataValueMapper;

    public ProgramStageInstanceMapper( WorkContext ctx )
    {
        super( ctx );
        noteMapper = new ProgramStageInstanceNoteMapper( ctx );
        dataValueMapper = new ProgramStageInstanceDataValueMapper( ctx );
    }

    public ProgramStageInstance map( Event event )
    {
        ImportOptions importOptions = workContext.getImportOptions();
        ProgramStageInstance psi = new ProgramStageInstance();

        if ( importOptions.getIdSchemes().getProgramStageInstanceIdScheme().equals( IdScheme.CODE ) )
        {
            // TODO: Is this really correct?
            psi.setCode( event.getUid() );
        }
        else if ( importOptions.getIdSchemes().getProgramStageIdScheme().equals( IdScheme.UID ) )
        {
            psi.setUid( event.getUid() );
        } // TODO what about other schemes, like id?

        // FKs
        psi.setProgramInstance( this.workContext.getProgramInstanceMap().get( event.getUid() ) );
        psi.setProgramStage( this.workContext.getProgramStage( event.getProgramStage() ) );
        psi.setOrganisationUnit( this.workContext.getOrganisationUnitMap().get( event.getUid() ) );

        // EXECUTION + DUE DATE
        Date executionDate = null;

        if ( event.getEventDate() != null )
        {
            executionDate = DateUtils.parseDate( event.getEventDate() );
        }

        Date dueDate = new Date();

        if ( event.getDueDate() != null )
        {
            dueDate = DateUtils.parseDate( event.getDueDate() );
        }

        psi.setDueDate( dueDate );
        psi.setExecutionDate( executionDate );

        psi.setStoredBy( event.getStoredBy() );
        psi.setCompletedBy( event.getCompletedBy() );

        if ( psi.isCompleted() )
        {
            Date completedDate = new Date();
            if ( event.getCompletedDate() != null )
            {
                completedDate = DateUtils.parseDate( event.getCompletedDate() );
            }
            psi.setCompletedDate( completedDate );
        }

        // STATUS

        psi.setStatus( EventStatus.fromInt( event.getStatus().getValue() ) );

        // ATTRIBUTE OPTION COMBO

        psi.setAttributeOptionCombo( this.workContext.getCategoryOptionComboMap().get( event.getUid() ) );

        // GEOMETRY

        psi.setGeometry( event.getGeometry() );

        // CREATED AT CLIENT + UPDATED AT CLIENT

        psi.setCreatedAtClient( DateUtils.parseDate( event.getCreatedAtClient() ) );
        psi.setLastUpdatedAtClient( DateUtils.parseDate( event.getLastUpdatedAtClient() ) );

        if ( psi.getProgramStage().isEnableUserAssignment() )
        {
            psi.setAssignedUser( this.workContext.getAssignedUserMap().get( event.getUid() ) );
        }

        // COMMENTS
        psi.setComments( convertNotes( event, this.workContext ) );

        // DATA VALUES

        psi.setEventDataValues(
            event.getDataValues().stream().map( dataValueMapper::map ).collect( Collectors.toSet() ) );
        
        return psi;

    }

    private List<TrackedEntityComment> convertNotes( Event event, WorkContext ctx )
    {
        return event.getNotes().stream().filter( note -> ctx.getNotesMap().containsKey( note.getNote() ) )
            .map( noteMapper::map ).collect( Collectors.toList() );
    }
}
