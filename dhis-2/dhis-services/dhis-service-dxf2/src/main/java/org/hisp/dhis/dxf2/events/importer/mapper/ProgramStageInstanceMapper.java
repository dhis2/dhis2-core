package org.hisp.dhis.dxf2.events.importer.mapper;

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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.common.IdScheme.CODE;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.hisp.dhis.event.EventStatus.fromInt;
import static org.hisp.dhis.util.DateUtils.parseDate;

import java.util.Date;
import java.util.List;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;

/**
 * @author Luciano Fiandesio
 */
public class ProgramStageInstanceMapper extends AbstractMapper<Event, ProgramStageInstance>
{
    private final ProgramStageInstanceNoteMapper noteMapper;

    public ProgramStageInstanceMapper( WorkContext ctx )
    {
        super( ctx );
        noteMapper = new ProgramStageInstanceNoteMapper( ctx );
    }

    @Override
    public ProgramStageInstance map( Event event )
    {
        ImportOptions importOptions = workContext.getImportOptions();
        
        ProgramStageInstance psi = new ProgramStageInstance();
        
        ProgramStageInstance programStageInstance = this.workContext.getProgramStageInstanceMap().get( event.getUid() );
        if ( programStageInstance != null )
        {
            psi.setId( programStageInstance.getId() );
        }

        if ( importOptions.getIdSchemes().getProgramStageInstanceIdScheme().equals( CODE ) )
        {
            // TODO: Is this really correct?
            psi.setCode( event.getUid() );
        }
        else if ( importOptions.getIdSchemes().getProgramStageIdScheme().equals( UID ) )
        {
            psi.setUid( event.getUid() );
        }

        // FKs
        psi.setProgramInstance( this.workContext.getProgramInstanceMap().get( event.getUid() ) );
        psi.setProgramStage( this.workContext.getProgramStage( importOptions.getIdSchemes().getProgramStageIdScheme(),
            event.getProgramStage() ) );
        psi.setOrganisationUnit( this.workContext.getOrganisationUnitMap().get( event.getUid() ) );

        // EXECUTION + DUE DATE
        Date executionDate = null;

        if ( event.getEventDate() != null )
        {
            executionDate = parseDate( event.getEventDate() );
        }

        Date dueDate = new Date();

        if ( event.getDueDate() != null )
        {
            dueDate = parseDate( event.getDueDate() );
        }

        psi.setDueDate( dueDate );
        // Note that execution date can be null
        psi.setExecutionDate( executionDate );

        psi.setStoredBy( event.getStoredBy() );
        psi.setCompletedBy( event.getCompletedBy() );

        if ( psi.isCompleted() )
        {
            Date completedDate = new Date();
            if ( event.getCompletedDate() != null )
            {
                completedDate = parseDate( event.getCompletedDate() );
            }
            psi.setCompletedDate( completedDate );
        }

        // STATUS

        psi.setStatus( fromInt( event.getStatus().getValue() ) );

        // ATTRIBUTE OPTION COMBO

        psi.setAttributeOptionCombo( this.workContext.getCategoryOptionComboMap().get( event.getUid() ) );

        // GEOMETRY

        psi.setGeometry( event.getGeometry() );

        // CREATED AT CLIENT + UPDATED AT CLIENT

        psi.setCreatedAtClient( parseDate( event.getCreatedAtClient() ) );
        psi.setLastUpdatedAtClient( parseDate( event.getLastUpdatedAtClient() ) );

        if ( psi.getProgramStage() != null && psi.getProgramStage().isEnableUserAssignment() )
        {
            psi.setAssignedUser( this.workContext.getAssignedUserMap().get( event.getUid() ) );
        }

        // COMMENTS
        psi.setComments( convertNotes( event, this.workContext ) );

        psi.setEventDataValues( workContext.getEventDataValueMap().get( event.getUid() ));
        return psi;
    }

    private List<TrackedEntityComment> convertNotes( Event event, WorkContext ctx )
    {
        if ( isNotEmpty( event.getNotes() ) )
        {
            return event.getNotes().stream().filter( note -> ctx.getNotesMap().containsKey( note.getNote() ) )
                .map( noteMapper::map ).collect( toList() );
        }

        return emptyList();
    }
}
