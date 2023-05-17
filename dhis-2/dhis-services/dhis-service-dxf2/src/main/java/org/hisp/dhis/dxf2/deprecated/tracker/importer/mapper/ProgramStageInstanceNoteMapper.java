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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.mapper;

import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventUtils.getValidUsername;
import static org.hisp.dhis.util.DateUtils.parseDate;

import java.util.Date;

import org.hisp.dhis.dxf2.deprecated.tracker.event.Note;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.context.WorkContext;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;

/**
 * @author Luciano Fiandesio
 */
public class ProgramStageInstanceNoteMapper
    extends
    AbstractMapper<Note, TrackedEntityComment>
{
    public ProgramStageInstanceNoteMapper( WorkContext ctx )
    {
        super( ctx );
    }

    @Override
    public TrackedEntityComment map( Note note )
    {
        final TrackedEntityComment comment = new TrackedEntityComment();
        comment.setUid( note.getNote() );
        comment.setCommentText( note.getValue() );
        comment.setCreator( getValidUsername( note.getStoredBy(), workContext.getImportOptions() ) );
        comment.setCreated( note.getStoredDate() == null ? new Date() : parseDate( note.getStoredDate() ) );
        comment.setLastUpdated( new Date() );
        comment.setLastUpdatedBy( workContext.getServiceDelegator().getEventImporterUserService().getCurrentUser() );
        return comment;
    }
}
