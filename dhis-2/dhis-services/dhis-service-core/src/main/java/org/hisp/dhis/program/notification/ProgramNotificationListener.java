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
package org.hisp.dhis.program.notification;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.program.notification.event.ProgramEnrollmentCompletionNotificationEvent;
import org.hisp.dhis.program.notification.event.ProgramEnrollmentNotificationEvent;
import org.hisp.dhis.program.notification.event.ProgramRuleEnrollmentEvent;
import org.hisp.dhis.program.notification.event.ProgramRuleStageEvent;
import org.hisp.dhis.program.notification.event.ProgramStageCompletionNotificationEvent;
import org.hisp.dhis.programrule.engine.TrackerEnrollmentWebHookEvent;
import org.hisp.dhis.programrule.engine.TrackerEventWebHookEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Created by zubair@dhis2.org on 18.01.18.
 */
@Async
@RequiredArgsConstructor
@Component( "org.hisp.dhis.program.notification.ProgramNotificationListener" )
public class ProgramNotificationListener
{
    private final ProgramNotificationService programNotificationService;

    private final TrackerNotificationWebHookService trackerNotificationWebHookService;

    @TransactionalEventListener( fallbackExecution = true )
    public void onEnrollment( ProgramEnrollmentNotificationEvent event )
    {
        programNotificationService.sendEnrollmentNotifications( event.getProgramInstance() );
    }

    @TransactionalEventListener( fallbackExecution = true )
    public void onCompletion( ProgramEnrollmentCompletionNotificationEvent event )
    {
        programNotificationService.sendEnrollmentCompletionNotifications( event.getProgramInstance() );
    }

    @TransactionalEventListener( fallbackExecution = true )
    public void onEvent( ProgramStageCompletionNotificationEvent event )
    {
        programNotificationService.sendEventCompletionNotifications( event.getProgramStageInstance() );
    }

    // Published by rule engine
    @TransactionalEventListener( fallbackExecution = true )
    public void onProgramRuleEnrollment( ProgramRuleEnrollmentEvent event )
    {
        programNotificationService.sendProgramRuleTriggeredNotifications( event.getTemplate(),
            event.getProgramInstance() );
    }

    @TransactionalEventListener( fallbackExecution = true )
    public void onProgramRuleEvent( ProgramRuleStageEvent event )
    {
        programNotificationService.sendProgramRuleTriggeredEventNotifications( event.getTemplate(),
            event.getProgramStageInstance() );
    }

    @TransactionalEventListener( fallbackExecution = true )
    public void onTrackerEventWebHook( TrackerEventWebHookEvent event )
    {
        trackerNotificationWebHookService.handleEvent( event.getEvent() );
    }

    @TransactionalEventListener( fallbackExecution = true )
    public void onTrackerEnrollmentWebHook( TrackerEnrollmentWebHookEvent event )
    {
        trackerNotificationWebHookService.handleEnrollment( event.getProgramInstance() );
    }
}
