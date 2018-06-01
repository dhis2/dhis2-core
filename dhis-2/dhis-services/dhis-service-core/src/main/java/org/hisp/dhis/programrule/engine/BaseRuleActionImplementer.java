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
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateStore;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionScheduleMessage;
import org.hisp.dhis.rules.models.RuleActionSendMessage;
import org.hisp.dhis.rules.models.RuleEffect;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @Author Zubair Asghar.
 */
abstract class BaseRuleActionImplementer implements RuleActionImplementer
{
    private static final Log log = LogFactory.getLog( BaseRuleActionImplementer.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramNotificationTemplateStore programNotificationTemplateStore;

    @Autowired
    private NotificationLoggingService notificationLoggingService;

    protected ExternalNotificationLogEntry createLogEntry( String key, String templateUid )
    {
        ExternalNotificationLogEntry entry = new ExternalNotificationLogEntry();
        entry.setLastSentAt( new Date() );
        entry.setKey( key );
        entry.setNotificationTemplateUid( templateUid );
        entry.setAllowMultiple( false );

        return entry;
    }

    protected ProgramNotificationTemplate getNotificationTemplate( RuleAction action )
    {
        String uid = "";

        if ( action instanceof RuleActionSendMessage )
        {
            RuleActionSendMessage sendMessage = (RuleActionSendMessage) action;
            uid = sendMessage.notification();
        }
        else if ( action instanceof RuleActionScheduleMessage )
        {
            RuleActionScheduleMessage scheduleMessage = (RuleActionScheduleMessage) action;
            uid = scheduleMessage.notification();
        }

        return programNotificationTemplateStore.getByUid( uid );
    }

    protected String generateKey( ProgramNotificationTemplate template, ProgramInstance programInstance )
    {
        return template.getUid() + programInstance.getUid();
    }

    protected boolean validate( RuleEffect ruleEffect, ProgramInstance programInstance )
    {
        if ( ruleEffect == null )
        {
            return false;
        }

        ProgramNotificationTemplate template = getNotificationTemplate( ruleEffect.ruleAction() );

        if ( template == null )
        {
            log.info( String.format( "No template found for Program: %s", programInstance.getProgram().getName() ) );
            return false;
        }

        if ( !notificationLoggingService.isValidForSending( generateKey( template, programInstance ) ) )
        {
            log.info( String.format( "Skipped rule action for template id: %s", template.getUid() ) );
            return false;
        }

        return true;
    }
}
