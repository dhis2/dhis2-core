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
package org.hisp.dhis.dxf2.events.importer.shared.validation;

import static org.hisp.dhis.dxf2.importsummary.ImportStatus.ERROR;
import static org.hisp.dhis.program.ProgramStatus.COMPLETED;
import static org.hisp.dhis.security.Authorities.F_EDIT_EXPIRED;
import static org.hisp.dhis.util.DateUtils.dateIsValid;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.hisp.dhis.util.DateUtils.removeTimeStamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.importer.Checker;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.Enrollment;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class EventBaseCheck implements Checker
{
    @Override
    public ImportSummary check( ImmutableEvent event, WorkContext ctx )
    {
        ImportSummary importSummary = new ImportSummary();
        List<String> errors = validate( event, ctx );

        if ( !errors.isEmpty() )
        {
            importSummary.setStatus( ERROR );
            importSummary.setReference( event.getEvent() );
            errors.forEach( error -> importSummary.addConflict( "Event", error ) );
            importSummary.incrementIgnored();

        }
        return importSummary;
    }

    private List<String> validate( ImmutableEvent event, WorkContext ctx )
    {
        List<String> errors = new ArrayList<>();

        validateDates( event, errors );

        validateProgramInstance( event, ctx, errors );

        return errors;
    }

    private void validateDates( ImmutableEvent event, List<String> errors )
    {
        if ( event.getDueDate() != null && !dateIsValid( event.getDueDate() ) )
        {
            errors.add( "Invalid event due date: " + event.getDueDate() );
        }

        if ( event.getEventDate() != null && !dateIsValid( event.getEventDate() ) )
        {
            errors.add( "Invalid event date: " + event.getEventDate() );
        }

        if ( event.getCreatedAtClient() != null && !dateIsValid( event.getCreatedAtClient() ) )
        {
            errors.add( "Invalid event created at client date: " + event.getCreatedAtClient() );
        }

        if ( event.getLastUpdatedAtClient() != null && !dateIsValid( event.getLastUpdatedAtClient() ) )
        {
            errors.add( "Invalid event last updated at client date: " + event.getLastUpdatedAtClient() );
        }
    }

    private void validateProgramInstance( ImmutableEvent event, WorkContext ctx, List<String> errors )
    {

        Enrollment enrollment = ctx.getProgramInstanceMap().get( event.getUid() );
        ImportOptions importOptions = ctx.getImportOptions();

        if ( enrollment == null )
        {
            errors.add( "No program instance found for event: " + event.getEvent() );

        }
        else if ( COMPLETED.equals( enrollment.getStatus() ) )
        {
            if ( importOptions == null || importOptions.getUser() == null
                || importOptions.getUser().isAuthorized( F_EDIT_EXPIRED.getAuthority() ) )
            {
                return;
            }

            Date referenceDate = parseDate( event.getCreated() );

            if ( referenceDate == null )
            {
                referenceDate = new Date();
            }

            referenceDate = removeTimeStamp( referenceDate );

            if ( referenceDate.after( removeTimeStamp( enrollment.getEndDate() ) ) )
            {
                errors.add( "Not possible to add event to a completed enrollment. Event created date ( " + referenceDate
                    + " ) is after enrollment completed date ( " + removeTimeStamp( enrollment.getEndDate() )
                    + " )." );
            }
        }
    }
}
