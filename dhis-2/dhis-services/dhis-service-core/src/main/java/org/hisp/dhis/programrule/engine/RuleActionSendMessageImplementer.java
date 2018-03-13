package org.hisp.dhis.programrule.engine;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.notification.logging.ExternalNotificationLogEntry;
import org.hisp.dhis.notification.logging.NotificationLoggingService;
import org.hisp.dhis.notification.logging.NotificationTriggerEvent;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.notification.*;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionSendMessage;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * Created by zubair@dhis2.org on 04.01.18.
 */
public class RuleActionSendMessageImplementer implements RuleActionImplementer
{
    private static final Log log = LogFactory.getLog( RuleActionSendMessageImplementer.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramNotificationTemplateStore programNotificationTemplateStore;

    @Autowired
    private ProgramNotificationPublisher publisher;

    @Autowired
    private NotificationLoggingService notificationLoggingService;

    @Override
    public boolean accept( RuleAction ruleAction )
    {
        return ruleAction instanceof RuleActionSendMessage;
    }

    @Override
    public void implement( RuleAction ruleAction, ProgramInstance programInstance )
    {
        ProgramNotificationTemplate template = getNotificationTemplate( ruleAction );

        if ( template == null )
        {
            log.info( String.format( "No template found for Program: %s", programInstance.getProgram().getName() ) );
            return;
        }

        String key = generateKey( template, programInstance );

        if ( !notificationLoggingService.isValidForSending( key ) )
        {
            log.info( String.format( "Skipped notification for template id: %s", template.getUid() ) );
            return;
        }

        ExternalNotificationLogEntry entry = createLogEntry( key, template.getUid() );
        entry.setNotificationTriggeredBy( NotificationTriggerEvent.PROGRAM );

        notificationLoggingService.save( entry );

        publisher.publishEnrollment( template, programInstance, ProgramNotificationEventType.PROGRAM_RULE_ENROLLMENT );
    }

    @Override
    public void implement( RuleAction ruleAction, ProgramStageInstance programStageInstance )
    {
        ProgramNotificationTemplate template = getNotificationTemplate( ruleAction );

        if ( template == null )
        {
            log.info( String.format( "No template found for ProgramStage: %s", programStageInstance.getProgramStage().getName() ) );
            return;
        }

        String key = generateKey( template, programStageInstance.getProgramInstance() );

        if ( !notificationLoggingService.isValidForSending( key ) )
        {
            log.info( String.format( "Skipped notification for template id: %s", template.getUid() ) );
            return;
        }

        ExternalNotificationLogEntry entry = createLogEntry( key, template.getUid() );
        entry.setNotificationTriggeredBy( NotificationTriggerEvent.PROGRAM_STAGE );

        notificationLoggingService.save( entry );

        publisher.publishEvent( template, programStageInstance, ProgramNotificationEventType.PROGRAM_RULE_EVENT );
    }

    private ProgramNotificationTemplate getNotificationTemplate( RuleAction action )
    {
        if ( action == null )
        {
            return null;
        }

        RuleActionSendMessage sendMessage = (RuleActionSendMessage) action;

        return programNotificationTemplateStore.getByUid( sendMessage.notification() );
    }

    private String generateKey( ProgramNotificationTemplate template, ProgramInstance programInstance )
    {
        return template.getUid() + programInstance.getUid();
    }

    private ExternalNotificationLogEntry createLogEntry( String key, String templateUid )
    {
        ExternalNotificationLogEntry entry = new ExternalNotificationLogEntry();
        entry.setLastSentAt( new Date() );
        entry.setKey( key );
        entry.setNotificationTemplateUid( templateUid );
        entry.setAllowMultiple( false );

        return entry;
    }
}