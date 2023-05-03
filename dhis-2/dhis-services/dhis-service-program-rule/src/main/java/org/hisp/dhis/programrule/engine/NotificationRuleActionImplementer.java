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
package org.hisp.dhis.programrule.engine;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.notification.logging.ExternalNotificationLogEntry;
import org.hisp.dhis.notification.logging.NotificationLoggingService;
import org.hisp.dhis.notification.logging.NotificationValidationResult;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionScheduleMessage;
import org.hisp.dhis.rules.models.RuleActionSendMessage;
import org.hisp.dhis.rules.models.RuleEffect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair Asghar
 */
@Slf4j
@RequiredArgsConstructor
@Component( "org.hisp.dhis.programrule.engine.NotificationRuleActionImplementer" )
abstract class NotificationRuleActionImplementer implements RuleActionImplementer
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    protected final ProgramNotificationTemplateService programNotificationTemplateService;

    protected final NotificationLoggingService notificationLoggingService;

    protected final ProgramInstanceService programInstanceService;

    protected final EventService eventService;

    protected ExternalNotificationLogEntry createLogEntry( String key, String templateUid )
    {
        ExternalNotificationLogEntry entry = new ExternalNotificationLogEntry();
        entry.setLastSentAt( new Date() );
        entry.setKey( key );
        entry.setNotificationTemplateUid( templateUid );
        entry.setAllowMultiple( false );

        return entry;
    }

    @Transactional( readOnly = true )
    public ProgramNotificationTemplate getNotificationTemplate( RuleAction action )
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

        return programNotificationTemplateService.getByUid( uid );
    }

    protected String generateKey( ProgramNotificationTemplate template, Enrollment enrollment )
    {
        return template.getUid() + enrollment.getUid();
    }

    @Transactional( readOnly = true )
    public NotificationValidationResult validate( RuleEffect ruleEffect, Enrollment enrollment )
    {
        checkNotNull( ruleEffect, "Rule Effect cannot be null" );
        checkNotNull( enrollment, "ProgramInstance cannot be null" );

        ProgramNotificationTemplate template = getNotificationTemplate( ruleEffect.ruleAction() );

        if ( template == null )
        {
            log.warn( String.format( "No template found for Program: %s", enrollment.getProgram().getName() ) );

            return NotificationValidationResult.builder().valid( false ).build();
        }

        ExternalNotificationLogEntry logEntry = notificationLoggingService
            .getByKey( generateKey( template, enrollment ) );

        // template has already been delivered and repeated delivery not allowed
        if ( logEntry != null && !logEntry.isAllowMultiple() )
        {
            return NotificationValidationResult.builder().valid( false )
                .template( template ).logEntry( logEntry ).build();
        }

        return NotificationValidationResult.builder().valid( true ).template( template ).logEntry( logEntry ).build();
    }

    protected void checkNulls( RuleEffect ruleEffect, Event event )
    {
        checkNotNull( ruleEffect, "Rule Effect cannot be null" );
        checkNotNull( event, "Event cannot be null" );
    }
}
