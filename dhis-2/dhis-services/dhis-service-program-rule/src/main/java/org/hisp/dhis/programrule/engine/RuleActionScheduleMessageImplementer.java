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

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.notification.logging.ExternalNotificationLogEntry;
import org.hisp.dhis.notification.logging.NotificationLoggingService;
import org.hisp.dhis.notification.logging.NotificationTriggerEvent;
import org.hisp.dhis.notification.logging.NotificationValidationResult;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceService;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.program.notification.template.snapshot.NotificationTemplateService;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionScheduleMessage;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair Asghar
 */
@Slf4j
@Component( "org.hisp.dhis.programrule.engine.RuleActionScheduleMessageImplementer" )
public class RuleActionScheduleMessageImplementer extends NotificationRuleActionImplementer
{
    public static final String LOG_MESSAGE = "Notification with id:%s has been scheduled";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final ProgramNotificationInstanceService programNotificationInstanceService;

    private final NotificationTemplateService notificationTemplateService;

    public RuleActionScheduleMessageImplementer( ProgramNotificationTemplateService programNotificationTemplateService,
        NotificationLoggingService notificationLoggingService,
        EnrollmentService enrollmentService,
        EventService eventService,
        ProgramNotificationInstanceService programNotificationInstanceService,
        NotificationTemplateService notificationTemplateService )
    {
        super( programNotificationTemplateService, notificationLoggingService, enrollmentService,
            eventService );
        this.programNotificationInstanceService = programNotificationInstanceService;
        this.notificationTemplateService = notificationTemplateService;
    }

    @Override
    public boolean accept( RuleAction ruleAction )
    {
        return ruleAction instanceof RuleActionScheduleMessage;
    }

    @Override
    @Transactional
    public void implement( RuleEffect ruleEffect, Enrollment enrollment )
    {
        NotificationValidationResult result = validate( ruleEffect, enrollment );

        if ( !result.isValid() )
        {
            return;
        }

        ProgramNotificationTemplate template = result.getTemplate();

        String key = generateKey( template, enrollment );

        String date = StringUtils.unwrap( ruleEffect.data(), '\'' );

        if ( !isDateValid( date ) )
        {
            return;
        }

        ProgramNotificationInstance notificationInstance = notificationTemplateService
            .createNotificationInstance( template, date );
        notificationInstance.setEvent( null );
        notificationInstance.setEnrollment( enrollment );

        programNotificationInstanceService.save( notificationInstance );

        log.info( String.format( LOG_MESSAGE, template.getUid() ) );

        if ( result.getLogEntry() != null )
        {
            return;
        }

        ExternalNotificationLogEntry entry = createLogEntry( key, template.getUid() );
        entry.setNotificationTriggeredBy( NotificationTriggerEvent.PROGRAM );
        entry.setAllowMultiple( template.isSendRepeatable() );

        notificationLoggingService.save( entry );
    }

    @Override
    @Transactional
    public void implement( RuleEffect ruleEffect, Event event )
    {
        checkNotNull( event, "Event cannot be null" );

        NotificationValidationResult result = validate( ruleEffect, event.getEnrollment() );

        // For program without registration
        if ( event.getProgramStage().getProgram().isWithoutRegistration() )
        {
            handleSingleEvent( ruleEffect, event );
            return;
        }

        if ( !result.isValid() )
        {
            return;
        }

        ProgramNotificationTemplate template = result.getTemplate();
        String key = generateKey( template, event.getEnrollment() );

        String date = StringUtils.unwrap( ruleEffect.data(), '\'' );

        if ( !isDateValid( date ) )
        {
            return;
        }

        ProgramNotificationInstance notificationInstance = notificationTemplateService
            .createNotificationInstance( template, date );
        notificationInstance.setEvent( event );
        notificationInstance.setEnrollment( null );

        programNotificationInstanceService.save( notificationInstance );

        log.info( String.format( LOG_MESSAGE, template.getUid() ) );

        if ( result.getLogEntry() != null )
        {
            return;
        }

        ExternalNotificationLogEntry entry = createLogEntry( key, template.getUid() );
        entry.setNotificationTriggeredBy( NotificationTriggerEvent.PROGRAM_STAGE );
        entry.setAllowMultiple( template.isSendRepeatable() );

        notificationLoggingService.save( entry );

    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    private void handleSingleEvent( RuleEffect ruleEffect, Event event )
    {
        ProgramNotificationTemplate template = getNotificationTemplate( ruleEffect.ruleAction() );

        if ( template == null )
        {
            return;
        }

        String date = StringUtils.unwrap( ruleEffect.data(), '\'' );

        if ( !isDateValid( date ) )
        {
            return;
        }

        ProgramNotificationInstance notificationInstance = notificationTemplateService
            .createNotificationInstance( template, date );
        notificationInstance.setEvent( event );
        notificationInstance.setEnrollment( null );

        programNotificationInstanceService.save( notificationInstance );

        log.info( String.format( LOG_MESSAGE, template.getUid() ) );
    }

    private boolean isDateValid( String date )
    {
        if ( StringUtils.isNotBlank( date ) )
        {
            if ( DateUtils.dateIsValid( date ) )
            {
                return true;
            }
        }

        log.error( "Invalid date: " + date );

        return false;
    }
}
