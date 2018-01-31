package org.hisp.dhis.program.notification;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

/**
 * Created by zubair@dhis2.org on 18.01.18.
 */
public class ProgramNotificationListener
{
    @Autowired
    private ProgramNotificationService programNotificationService;

    @EventListener( condition = "#event.eventType.name() == 'PROGRAM_ENROLLMENT'" )
    @Async
    public void onEnrollment( ProgramNotificationEvent event )
    {
        programNotificationService.sendEnrollmentNotifications( event.getProgramInstance() );
    }

    @EventListener( condition = "#event.eventType.name() == 'PROGRAM_COMPLETION'" )
    @Async
    public void onCompletion( ProgramNotificationEvent event )
    {
        programNotificationService.sendCompletionNotifications( event.getProgramInstance() );
    }

    @EventListener( condition = "#event.eventType.name() == 'PROGRAM_RULE_ENROLLMENT'" )
    @Async
    public void onProgramRuleEnrollment( ProgramNotificationEvent event )
    {
        programNotificationService.sendProgramRuleTriggeredNotifications( event.getTemplate(), event.getProgramInstance() );
    }

    @EventListener( condition = "#event.eventType.name() == 'PROGRAM_STAGE_COMPLETION'" )
    @Async
    public void onEvent( ProgramNotificationEvent event )
    {
        programNotificationService.sendCompletionNotifications( event.getProgramStageInstance() );
    }

    @EventListener( condition = "#event.eventType.name() == 'PROGRAM_RULE_EVENT'" )
    @Async
    public void onProgramRuleEvent( ProgramNotificationEvent event )
    {
        programNotificationService.sendProgramRuleTriggeredNotifications( event.getTemplate(), event.getProgramStageInstance() );
    }
}
