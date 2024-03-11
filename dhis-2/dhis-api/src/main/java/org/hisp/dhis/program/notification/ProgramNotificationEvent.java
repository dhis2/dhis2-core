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
import org.springframework.context.ApplicationEvent;

/**
 * Created by zubair@dhis2.org on 18.01.18.
 */
public class ProgramNotificationEvent extends ApplicationEvent
{
    private ProgramInstance programInstance;

    private ProgramStageInstance programStageInstance;

    private ProgramNotificationTemplate template;

    private ProgramNotificationEventType eventType;

    public ProgramNotificationEvent( Object source, ProgramInstance programInstance, ProgramNotificationEventType eventType )
    {
        super(source);
        this.programInstance = programInstance;
        this.eventType = eventType;
    }

    public ProgramNotificationEvent( Object source, ProgramStageInstance programStageInstance, ProgramNotificationEventType eventType )
    {
        super(source);
        this.programStageInstance = programStageInstance;
        this.eventType = eventType;
    }

    public ProgramNotificationEvent( Object source, ProgramNotificationTemplate template, ProgramInstance programInstance, ProgramNotificationEventType eventType )
    {
        super(source);
        this.template = template;
        this.programInstance = programInstance;
        this.eventType = eventType;
    }

    public ProgramNotificationEvent( Object source, ProgramNotificationTemplate template, ProgramStageInstance programStageInstance, ProgramNotificationEventType eventType )
    {
        super(source);
        this.template = template;
        this.programStageInstance = programStageInstance;
        this.eventType = eventType;
    }

    public ProgramInstance getProgramInstance()
    {
        return programInstance;
    }

    public ProgramStageInstance getProgramStageInstance()
    {
        return programStageInstance;
    }

    public ProgramNotificationEventType getEventType()
    {
        return eventType;
    }

    public ProgramNotificationTemplate getTemplate()
    {
        return template;
    }
}
