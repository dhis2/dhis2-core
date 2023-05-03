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

import java.util.List;

import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.importer.Checker;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.User;

/**
 * @author Luciano Fiandesio
 */
public abstract class BaseEventAclCheck implements Checker
{
    public ImportSummary check( ImmutableEvent event, WorkContext ctx )
    {
        ImportOptions importOptions = ctx.getImportOptions();

        Event programStageInstance = prepareForAclValidation( ctx, event );

        List<String> errors = checkAcl( ctx.getServiceDelegator().getTrackerAccessManager(), importOptions.getUser(),
            programStageInstance );

        final ImportSummary importSummary = new ImportSummary();

        if ( !errors.isEmpty() )
        {
            errors.forEach( error -> importSummary.addConflict( event.getUid(), error ) );
            importSummary.incrementIgnored();
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.setReference( event.getEvent() );
        }
        return importSummary;
    }

    private Event prepareForAclValidation( WorkContext ctx, ImmutableEvent event )
    {
        final IdScheme programStageIdScheme = ctx.getImportOptions().getIdSchemes().getProgramStageIdScheme();

        Event programStageInstance = new Event();
        programStageInstance.setProgramStage( ctx.getProgramStage( programStageIdScheme, event.getProgramStage() ) );
        programStageInstance.setOrganisationUnit( ctx.getOrganisationUnitMap().get( event.getUid() ) );
        programStageInstance.setStatus( event.getStatus() );
        ProgramInstance programInstance = ctx.getProgramInstanceMap().get( event.getUid() );
        programStageInstance.setEnrollment( programInstance );

        return programStageInstance;
    }

    public abstract List<String> checkAcl( TrackerAccessManager trackerAccessManager, User user,
        Event event );
}
