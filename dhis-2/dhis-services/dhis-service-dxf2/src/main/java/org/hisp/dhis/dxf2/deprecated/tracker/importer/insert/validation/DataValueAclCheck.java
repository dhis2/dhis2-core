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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.insert.validation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.Checker;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.context.WorkContext;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class DataValueAclCheck implements Checker
{
    @Override
    public ImportSummary check( ImmutableEvent event, WorkContext ctx )
    {
        final TrackerAccessManager trackerAccessManager = ctx.getServiceDelegator().getTrackerAccessManager();
        final Event programStageInstance = ctx.getProgramStageInstanceMap().get( event.getUid() );

        Map<String, Set<EventDataValue>> eventDataValueMap = ctx.getEventDataValueMap();

        final User user = ctx.getImportOptions().getUser();
        final ImportSummary importSummary = new ImportSummary();

        // Note that here we are passing a Event, which during a
        // INSERT
        // operation
        // is going to be null, so the ACL method will not be able to check that
        final Set<EventDataValue> dataValues = eventDataValueMap.get( event.getUid() );

        for ( EventDataValue dataValue : dataValues )
        {
            DataElement dataElement = ctx.getDataElementMap().get( dataValue.getDataElement() );
            List<String> errors = trackerAccessManager.canWrite( user, programStageInstance, dataElement, true );

            if ( !errors.isEmpty() )
            {
                errors.forEach( error -> importSummary.addConflict( dataElement.getUid(), error ) );
            }
        }

        return importSummary;
    }
}
