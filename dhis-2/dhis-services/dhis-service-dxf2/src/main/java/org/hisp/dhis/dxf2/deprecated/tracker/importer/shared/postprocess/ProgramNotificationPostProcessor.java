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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.shared.postprocess;

import static org.hisp.dhis.event.EventStatus.SCHEDULE;

import java.util.Optional;

import org.hisp.dhis.dxf2.deprecated.tracker.importer.Processor;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.context.WorkContext;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.mapper.ProgramStageInstanceMapper;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.notification.event.ProgramStageCompletionNotificationEvent;
import org.hisp.dhis.programrule.engine.StageCompletionEvaluationEvent;
import org.hisp.dhis.programrule.engine.StageScheduledEvaluationEvent;
import org.hisp.dhis.programrule.engine.TrackerEventWebHookEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * @author maikel arabori
 */
@Component
public class ProgramNotificationPostProcessor implements Processor
{
    @Override
    public void process( final org.hisp.dhis.dxf2.deprecated.tracker.event.Event event, final WorkContext ctx )
    {
        if ( !ctx.getImportOptions().isSkipNotifications() )
        {
            // When this processor is invoked from insert event, then
            // event
            // might be null and need to be built from Event.
            final Event programStageInstance = getProgramStageInstance( ctx, event );

            final ApplicationEventPublisher applicationEventPublisher = ctx.getServiceDelegator()
                .getApplicationEventPublisher();

            if ( programStageInstance.isCompleted() )
            {
                applicationEventPublisher
                    .publishEvent( new ProgramStageCompletionNotificationEvent( this, programStageInstance.getId() ) );
                applicationEventPublisher
                    .publishEvent( new StageCompletionEvaluationEvent( this, programStageInstance.getUid() ) );
            }

            if ( SCHEDULE.equals( programStageInstance.getStatus() ) )
            {
                applicationEventPublisher
                    .publishEvent( new StageScheduledEvaluationEvent( this, programStageInstance.getUid() ) );
            }

            ctx.getServiceDelegator().getApplicationEventPublisher().publishEvent(
                new TrackerEventWebHookEvent( this, event.getEvent() ) );
        }
    }

    private Event getProgramStageInstance( WorkContext ctx, org.hisp.dhis.dxf2.deprecated.tracker.event.Event event )
    {
        return Optional.ofNullable( ctx.getProgramStageInstanceMap().get( event.getUid() ) )
            .orElseGet( () -> new ProgramStageInstanceMapper( ctx ).map( event ) );
    }
}
