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

package org.hisp.dhis.dxf2.events.event.postprocess.update;

import static org.hisp.dhis.common.CodeGenerator.generateUid;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;
import static org.hisp.dhis.user.User.getSafeUsername;
import static org.hisp.dhis.user.UserCredentials.USERNAME_MAX_LENGTH;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.springframework.util.StringUtils.isEmpty;

import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.Note;
import org.hisp.dhis.dxf2.events.event.preprocess.PreProcessor;
import org.hisp.dhis.dxf2.events.event.validation.ValidationContext;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;

public class TrackerEntityCommentPostProcessor
    implements
    PreProcessor
{
    @Override
    public void process( final Event event, final ValidationContext ctx )
    {
        for ( final Note note : event.getNotes() )
        {
            final String noteUid = isValidUid( note.getNote() ) ? note.getNote() : generateUid();

            if ( !ctx.getTrackedEntityCommentService().trackedEntityCommentExists( noteUid ) && !isEmpty( note.getValue() ) )
            {
                TrackedEntityComment comment = new TrackedEntityComment();
                comment.setUid( noteUid );
                comment.setCommentText( note.getValue() );
                comment.setCreator( getValidUsername( note.getStoredBy(),
                    ctx.getImportOptions().getUser() != null
                        ? ctx.getImportOptions().getUser().getUsername()
                        : "[Unknown]" ) );

                comment.setCreated( parseDate( note.getStoredDate() ) );

                ctx.getTrackedEntityCommentService().addTrackedEntityComment( comment );

                ctx.getProgramStageInstanceMap().get(event.getEvent()).getComments().add( comment );
            }
        }
    }

    private String getValidUsername( final String userName, final String fallbackUsername )
    {
        String validUsername = userName;

        if ( isEmpty( validUsername ) )
        {
            validUsername = getSafeUsername( fallbackUsername );
        }
        else if ( validUsername.length() > USERNAME_MAX_LENGTH )
        {
            // TODO: I don't think we need this logic here, this will be checked during the
            // validation phase.
//            if ( importSummary != null )
//            {
//                // TODO: luciano this should be moved to the new logic
//                importSummary.getConflicts().add( new ImportConflict( "Username",
//                    validUsername + " is more than " + UserCredentials.USERNAME_MAX_LENGTH + " characters, using current username instead" ) );
//            }

            validUsername = getSafeUsername( fallbackUsername );
        }

        return validUsername;
    }
}
