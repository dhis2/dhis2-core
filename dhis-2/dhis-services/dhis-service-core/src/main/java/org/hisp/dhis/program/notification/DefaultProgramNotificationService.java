package org.hisp.dhis.program.notification;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Halvdan Hoem Grelland
 */
public class DefaultProgramNotificationService
    implements ProgramNotificationService
{
    private static final Log log = LogFactory.getLog( DefaultProgramNotificationService.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramMessageService programMessageService;

    public void setProgramMessageService( ProgramMessageService programMessageService )
    {
        this.programMessageService = programMessageService;
    }

    private MessageService messageService;

    public void setMessageService( MessageService messageService )
    {
        this.messageService = messageService;
    }

    private ProgramInstanceStore programInstanceStore;

    public void setProgramInstanceStore( ProgramInstanceStore programInstanceStore )
    {
        this.programInstanceStore = programInstanceStore;
    }

    private ProgramStageInstanceStore programStageInstanceStore;

    public void setProgramStageInstanceStore( ProgramStageInstanceStore programStageInstanceStore )
    {
        this.programStageInstanceStore = programStageInstanceStore;
    }

    private IdentifiableObjectManager identifiableObjectManager;

    public void setIdentifiableObjectManager( IdentifiableObjectManager identifiableObjectManager )
    {
        this.identifiableObjectManager = identifiableObjectManager;
    }

    // -------------------------------------------------------------------------
    // ProgramStageNotificationService implementation
    // -------------------------------------------------------------------------

    @Transactional
    @Override
    public void sendScheduledNotificationsForDay( Date notificationDate )
    {
        Clock clock = new Clock( log ).startClock()
            .logTime( "Processing ProgramStageNotification messages" );

        List<ProgramNotificationTemplate> scheduledNotifications =
            identifiableObjectManager.getAll( ProgramNotificationTemplate.class ).stream()
                .filter( n -> n.getNotificationTrigger().isScheduled() )
                .collect( Collectors.toList() );

        for ( ProgramNotificationTemplate notification : scheduledNotifications )
        {
            List<ProgramStageInstance> programStageInstances =
                programStageInstanceStore.getWithScheduledNotifications( notification, notificationDate );

            List<ProgramInstance> programInstances =
                programInstanceStore.getWithScheduledNotifications( notification, notificationDate );

            MessageBatch psiBatch = createEventMessageBatch( notification, programStageInstances );
            MessageBatch psBatch = createEnrollmentMessageBatch( notification, programInstances );

            sendDhisMessages( psiBatch.dhisMessages );
            sendDhisMessages( psBatch.dhisMessages );

            sendProgramMessages( psiBatch.programMessages );
            sendProgramMessages( psBatch.programMessages );
        }

        clock.logTime( String.format( "Processed and sent ProgramStageNotification messages in %s", clock.time() ) );
    }

    @Transactional
    @Override
    public void sendImmediateNotifications( ProgramInstance programInstance )
    {
        Set<ProgramNotificationTemplate> templates = programInstance.getProgram().getNotificationTemplates().stream()
            .filter( n -> n.getNotificationTrigger().isImmediate() )
            .collect( Collectors.toSet() );

        if ( templates.isEmpty() )
        {
            return;
        }

        for ( ProgramNotificationTemplate template : templates )
        {
            MessageBatch batch = createSingleEnrollmentMessageBatch( template, programInstance );
            sendDhisMessages( batch.dhisMessages );
            sendProgramMessages( batch.programMessages );
        }
    }

    @Transactional
    @Override
    public void sendImmediateNotifications( ProgramStageInstance programStageInstance )
    {
        Set<ProgramNotificationTemplate> templates = programStageInstance.getProgramStage().getNotificationTemplates().stream()
            .filter( n -> n.getNotificationTrigger().isImmediate() )
            .collect( Collectors.toSet() );

        if ( templates.isEmpty() )
        {
            return;
        }

        for ( ProgramNotificationTemplate template : templates )
        {
            MessageBatch batch = createSingleEventMessageBatch( template, programStageInstance );
            sendDhisMessages( batch.dhisMessages );
            sendProgramMessages( batch.programMessages );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private MessageBatch createEventMessageBatch( ProgramNotificationTemplate template, List<ProgramStageInstance> programStageInstances )
    {
        MessageBatch batch = new MessageBatch( template );

        if ( template.getNotificationRecipient().isExternalRecipient() )
        {
            batch.programMessages.addAll(
                programStageInstances.stream()
                    .map( psi -> createProgramMessage( psi, template ) )
                    .collect( Collectors.toSet() )
            );
        }
        else
        {
            batch.dhisMessages.addAll(
                programStageInstances.stream()
                    .map( psi -> createDhisMessage( psi, template ) )
                    .collect( Collectors.toSet() )
            );
        }

        return batch;
    }

    private MessageBatch createEnrollmentMessageBatch( ProgramNotificationTemplate template, List<ProgramInstance> programInstances )
    {
        MessageBatch batch = new MessageBatch( template );

        if ( template.getNotificationRecipient().isExternalRecipient() )
        {
            batch.programMessages.addAll(
                programInstances.stream()
                    .map( ps -> createProgramMessage( ps, template ) )
                    .collect( Collectors.toSet() )
            );
        }

        return batch;
    }

    private MessageBatch createSingleEnrollmentMessageBatch( ProgramNotificationTemplate template, ProgramInstance programInstance )
    {
        MessageBatch batch = new MessageBatch( template );

        if ( template.getNotificationRecipient().isExternalRecipient() )
        {
            batch.programMessages.add( createProgramMessage( programInstance, template ) );
        }
        else
        {
            batch.dhisMessages.add( createDhisMessage( programInstance, template ) );
        }

        return batch;
    }

    private MessageBatch createSingleEventMessageBatch( ProgramNotificationTemplate template, ProgramStageInstance programStageInstance )
    {
        MessageBatch batch = new MessageBatch( template );

        if ( template.getNotificationRecipient().isExternalRecipient() )
        {
            batch.programMessages.add( createProgramMessage( programStageInstance, template ) );
        }
        else
        {
            batch.dhisMessages.add( createDhisMessage( programStageInstance, template ) );
        }

        return batch;
    }

    private ProgramMessage createProgramMessage( ProgramStageInstance psi, ProgramNotificationTemplate template )
    {
        NotificationMessage message = NotificationMessageRenderer.render( psi, template );

        return new ProgramMessage(
            message.getSubject(), message.getMessage(), resolveProgramMessageRecipients( template, psi.getOrganisationUnit(),
            psi.getProgramInstance().getEntityInstance() ), template.getDeliveryChannels(), psi );
    }

    private ProgramMessage createProgramMessage( ProgramInstance programInstance, ProgramNotificationTemplate template )
    {
        NotificationMessage message = NotificationMessageRenderer.render( programInstance, template );

        return new ProgramMessage( message.getSubject(), message.getMessage(),
            resolveProgramMessageRecipients( template, programInstance.getOrganisationUnit(), programInstance.getEntityInstance() ) );
    }

    private ProgramMessageRecipients resolveProgramMessageRecipients( ProgramNotificationTemplate template,
        OrganisationUnit organisationUnit, TrackedEntityInstance trackedEntityInstance )
    {
        ProgramMessageRecipients recipients = new ProgramMessageRecipients();

        NotificationRecipient recipientType = template.getNotificationRecipient();

        if ( recipientType == NotificationRecipient.ORGANISATION_UNIT_CONTACT )
        {
            recipients.setOrganisationUnit( organisationUnit );
        }
        else if ( recipientType == NotificationRecipient.TRACKED_ENTITY_INSTANCE )
        {
            recipients.setTrackedEntityInstance( trackedEntityInstance );
        }

        return recipients;
    }

    private DhisMessage createDhisMessage( ProgramStageInstance psi, ProgramNotificationTemplate template )
    {
        DhisMessage dhisMessage = new DhisMessage();

        dhisMessage.message = NotificationMessageRenderer.render( psi, template );

        return dhisMessage;
    }

    private DhisMessage createDhisMessage( ProgramInstance pi, ProgramNotificationTemplate template )
    {
        DhisMessage dhisMessage = new DhisMessage();

        dhisMessage.message = NotificationMessageRenderer.render( pi, template );

        return dhisMessage;
    }

    private void sendDhisMessages( Set<DhisMessage> messages )
    {
        messages.forEach( m ->
            messageService.sendMessage( m.message.getSubject(), m.message.getMessage(), null, m.recipients, null, false, true )
        );
    }

    private void sendProgramMessages( Set<ProgramMessage> messages )
    {
        programMessageService.sendMessages( Lists.newArrayList( messages ) );
    }

    // -------------------------------------------------------------------------
    // Internal classes
    // -------------------------------------------------------------------------

    private static class DhisMessage
    {
        NotificationMessage message;
        Set<User> recipients;
    }

    private static class MessageBatch
    {
        ProgramNotificationTemplate template;
        Set<DhisMessage> dhisMessages = Sets.newHashSet();
        Set<ProgramMessage> programMessages = Sets.newHashSet();

        MessageBatch( ProgramNotificationTemplate template)
        {
            this.template = template;
        }
    }
}
