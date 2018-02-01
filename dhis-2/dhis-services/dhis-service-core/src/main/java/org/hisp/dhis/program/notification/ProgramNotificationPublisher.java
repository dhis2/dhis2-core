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

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Created by zubair@dhis2.org on 18.01.18.
 */
public class ProgramNotificationPublisher
{
    @Autowired
    private ApplicationEventPublisher publisher;

    public void publishEnrollment( ProgramInstance programInstance, ProgramNotificationEventType eventType )
    {
        ProgramNotificationEvent event = new ProgramNotificationEvent( this, programInstance, eventType );

        publisher.publishEvent( event );
    }

    public void publishEnrollment( ProgramNotificationTemplate template, ProgramInstance programInstance, ProgramNotificationEventType type )
    {
        ProgramNotificationEvent event = new ProgramNotificationEvent( this, template, programInstance, type );

        publisher.publishEvent( event );
    }

    public void publishEvent( ProgramStageInstance programStageInstance, ProgramNotificationEventType eventType )
    {
        ProgramNotificationEvent event = new ProgramNotificationEvent( this, programStageInstance, eventType );

        publisher.publishEvent( event );
    }

    public void publishEvent( ProgramNotificationTemplate template, ProgramStageInstance programStageInstance, ProgramNotificationEventType type )
    {
        ProgramNotificationEvent event = new ProgramNotificationEvent( this, template, programStageInstance, type );

        publisher.publishEvent( event );
    }
}
