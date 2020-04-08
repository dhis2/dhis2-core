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

import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.preprocess.PreProcessor;
import org.hisp.dhis.dxf2.events.event.validation.ValidationContext;
import org.hisp.dhis.program.ProgramStageInstance;

public class TrackedEntityInstancePostProcessor
    implements
    PreProcessor
{
    @Override
    public void process( final Event event, final ValidationContext ctx )
    {
        if ( !ctx.getImportOptions().isSkipLastUpdated() )
        {
            final ProgramStageInstance programStageInstance = ctx.getProgramStageInstanceMap().get( event.getEvent() );

            if ( programStageInstance.getProgramInstance() != null )
            {
                // TODO: Should we handle it always as bulk update? Does it make sense to not execute bulk updatade?
//                if ( !bulkUpdate )
//                {
//                    if ( programStageInstance.getProgramInstance().getEntityInstance() != null )
//                    {
//                        ctx.getIdentifiableObjectManager().update(
//                            programStageInstance.getProgramInstance().getEntityInstance(),
//                            ctx.getImportOptions().getUser() );
//                    }
//                }
//                else
//                {
//                    if ( programStageInstance.getProgramInstance().getEntityInstance() != null )
//                    {
//                        trackedEntityInstancesToUpdate
//                            .add( programStageInstance.getProgramInstance().getEntityInstance() );
//                    }
//                }

                // TODO: I'm assuming we always do a bulk update. Also assuming that the TrackedEntityInstanceMap has not null elements,
                // and they are all associated to the events being update.
                ctx.getTrackedEntityInstanceMap().values().forEach(
                    tei -> ctx.getIdentifiableObjectManager().update( tei, ctx.getImportOptions().getUser() ) );
            }
        }
    }
}
