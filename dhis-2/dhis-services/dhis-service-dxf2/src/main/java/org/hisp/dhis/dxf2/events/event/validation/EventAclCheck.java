package org.hisp.dhis.dxf2.events.event.validation;

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

import java.util.List;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.ProgramStageInstance;

/**
 * @author Luciano Fiandesio
 */
public class EventAclCheck
    implements
    ValidationCheck
{

    @Override
    public ImportSummary check( Event event, ValidationContext ctx )
    {
        ImportOptions importOptions = ctx.getImportOptions();
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setOrganisationUnit( ctx.getOrganisationUnitMap().get( event.getEvent() ) );
        programStageInstance.setStatus( event.getStatus() );

        List<String> errors = ctx.getTrackerAccessManager().canCreate( importOptions.getUser(),  programStageInstance, false);

        if ( !errors.isEmpty() )
        {
            ImportSummary importSummary = new ImportSummary( ImportStatus.ERROR, errors.toString() );
            importSummary.incrementIgnored();

            return importSummary;
        }
        return new ImportSummary();
    }

    @Override
    public boolean isFinal()
    {
        return true;
    }
}
