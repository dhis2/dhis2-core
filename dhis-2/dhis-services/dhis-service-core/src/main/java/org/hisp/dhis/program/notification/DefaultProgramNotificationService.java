package org.hisp.dhis.program.notification;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.message.MessageType;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
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

    private NotificationMessageRenderer<ProgramInstance> programNotificationRenderer;

    public void setProgramNotificationRenderer( NotificationMessageRenderer<ProgramInstance> programNotificationRenderer )
    {
        this.programNotificationRenderer = programNotificationRenderer;
    }

    private NotificationMessageRenderer<ProgramStageInstance> programStageNotificationRenderer;

    public void setProgramStageNotificationRenderer( NotificationMessageRenderer<ProgramStageInstance> programStageNotificationRenderer )
    {
        this.programStageNotificationRenderer = programStageNotificationRenderer;
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

        List<ProgramNotificationTemplate> scheduledTemplates = getScheduledTemplates();

        int totalMessageCount = 0;

        for ( ProgramNotificationTemplate template : scheduledTemplates )
        {
            MessageBatch batch = createScheduledMessageBatchForDay( template, notificationDate );
            sendAll( batch );

            totalMessageCount += batch.messageCount();
        }

        clock.logTime( String.format( "Created and sent %d messages in %s", totalMessageCount, clock.time() ) );
    }

    @Transactional
    @Override
    public void sendCompletionNotifications( ProgramStageInstance programStageInstance )
    {
        sendProgramStageInstanceNotifications( programStageInstance, NotificationTrigger.COMPLETION );
    }

    @Transactional
    @Override
    public void sendCompletionNotifications( ProgramInstance programInstance )
    {
        sendProgramInstanceNotifications( programInstance, NotificationTrigger.COMPLETION );
    }

    @Transactional
    @Override
    public void sendEnrollmentNotifications( ProgramInstance programInstance )
    {
        sendProgramInstanceNotifications( programInstance, NotificationTrigger.ENROLLMENT );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private MessageBatch createScheduledMessageBatchForDay( ProgramNotificationTemplate template, Date day )
    {
        List<ProgramStageInstance> programStageInstances =
            programStageInstanceStore.getWithScheduledNotifications( template, day );

        List<ProgramInstance> programInstances =
            programInstanceStore.getWithScheduledNotifications( template, day );

        MessageBatch psiBatch = createProgramStageInstanceMessageBatch( template, programStageInstances );
        MessageBatch psBatch = createProgramInstanceMessageBatch( template, programInstances );

        return new MessageBatch( psiBatch, psBatch );
    }

    private List<ProgramNotificationTemplate> getScheduledTemplates()
    {
        return identifiableObjectManager.getAll( ProgramNotificationTemplate.class ).stream()
            .filter( n -> n.getNotificationTrigger().isScheduled() )
            .collect( Collectors.toList() );
    }

    private void sendProgramStageInstanceNotifications( ProgramStageInstance programStageInstance, NotificationTrigger trigger )
    {
        Set<ProgramNotificationTemplate> templates = resolveTemplates( programStageInstance, trigger );

        if ( templates.isEmpty() )
        {
            return;
        }

        for ( ProgramNotificationTemplate template : templates )
        {
            MessageBatch batch = createProgramStageInstanceMessageBatch( template, Lists.newArrayList( programStageInstance ) );
            sendAll( batch );
        }
    }

    private void sendProgramInstanceNotifications( ProgramInstance programInstance, NotificationTrigger trigger )
    {
        Set<ProgramNotificationTemplate> templates = resolveTemplates( programInstance, trigger );

        for ( ProgramNotificationTemplate template : templates )
        {
            MessageBatch batch = createProgramInstanceMessageBatch( template, Lists.newArrayList( programInstance ) );
            sendAll( batch );
        }
    }

    private MessageBatch createProgramStageInstanceMessageBatch( ProgramNotificationTemplate template, List<ProgramStageInstance> programStageInstances )
    {
        MessageBatch batch = new MessageBatch();

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

    private MessageBatch createProgramInstanceMessageBatch( ProgramNotificationTemplate template, List<ProgramInstance> programInstances )
    {
        MessageBatch batch = new MessageBatch();

        if ( template.getNotificationRecipient().isExternalRecipient() )
        {
            batch.programMessages.addAll(
                programInstances.stream()
                    .map( pi -> createProgramMessage( pi, template ) )
                    .collect( Collectors.toSet() )
            );
        }
        else
        {
            batch.dhisMessages.addAll(
                programInstances.stream()
                    .map( ps -> createDhisMessage( ps, template ) )
                    .collect( Collectors.toSet() )
            );
        }

        return batch;
    }

    private ProgramMessage createProgramMessage( ProgramStageInstance psi, ProgramNotificationTemplate template )
    {
        NotificationMessage message = programStageNotificationRenderer.render( psi, template );

        return new ProgramMessage(
            message.getSubject(), message.getMessage(), resolveProgramMessageRecipients( template, psi.getOrganisationUnit(),
            psi.getProgramInstance() ), template.getDeliveryChannels(), psi );
    }

    private ProgramMessage createProgramMessage( ProgramInstance programInstance, ProgramNotificationTemplate template )
    {
        NotificationMessage message = programNotificationRenderer.render( programInstance, template );

        return new ProgramMessage(
            message.getSubject(), message.getMessage(),
            resolveProgramMessageRecipients( template, programInstance.getOrganisationUnit(), programInstance ),
            template.getDeliveryChannels(), programInstance );
    }

    private Set<User> resolveDhisMessageRecipients(
        ProgramNotificationTemplate template, @Nullable ProgramInstance programInstance, @Nullable ProgramStageInstance programStageInstance )
    {
        if ( programInstance == null && programStageInstance == null )
        {
            throw new IllegalArgumentException( "Either of the arguments [programInstance, programStageInstance] must be non-null" );
        }

        Set<User> recipients = Sets.newHashSet();

        ProgramNotificationRecipient recipientType = template.getNotificationRecipient();

        if ( recipientType == ProgramNotificationRecipient.USER_GROUP )
        {
            recipients.addAll( template.getRecipientUserGroup().getMembers() );
        }
        else if ( recipientType == ProgramNotificationRecipient.USERS_AT_ORGANISATION_UNIT )
        {

            OrganisationUnit organisationUnit =
                programInstance != null ? programInstance.getOrganisationUnit() : programStageInstance.getOrganisationUnit();

            recipients.addAll( organisationUnit.getUsers() );
        }

        return recipients;
    }

    private ProgramMessageRecipients resolveProgramMessageRecipients(
            ProgramNotificationTemplate template, OrganisationUnit organisationUnit, ProgramInstance programInstance )
    {
        ProgramMessageRecipients recipients = new ProgramMessageRecipients();

        TrackedEntityInstance trackedEntityInstance = programInstance.getEntityInstance();

        ProgramNotificationRecipient recipientType = template.getNotificationRecipient();

        if ( recipientType == ProgramNotificationRecipient.ORGANISATION_UNIT_CONTACT )
        {
            recipients.setOrganisationUnit( organisationUnit );
        }
        else if ( recipientType == ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE )
        {
            recipients.setTrackedEntityInstance( trackedEntityInstance );
        }
        else if ( recipientType == ProgramNotificationRecipient.PROGRAM_ATTRIBUTE
            && template.getRecipientProgramAttribute() != null )
        {
            List<String> recipientList = programInstance.getEntityInstance().getTrackedEntityAttributeValues().stream()
                .filter( av -> template.getRecipientProgramAttribute().getUid().equals( av.getAttribute().getUid() ) )
                .map( av -> av.getPlainValue() )
                .collect( Collectors.toList() );

            if ( template.getDeliveryChannels().contains( DeliveryChannel.SMS ) )
            {
                recipients.getPhoneNumbers().addAll( recipientList );
            }
            else if ( template.getDeliveryChannels().contains( DeliveryChannel.EMAIL ) )
            {
                recipients.getEmailAddresses().addAll( recipientList );
            }
        }

        return recipients;
    }

    private Set<ProgramNotificationTemplate> resolveTemplates( ProgramInstance programInstance, final NotificationTrigger trigger )
    {
        return programInstance.getProgram().getNotificationTemplates().stream()
            .filter( t -> t.getNotificationTrigger() == trigger )
            .collect( Collectors.toSet() );
    }

    private Set<ProgramNotificationTemplate> resolveTemplates( ProgramStageInstance programStageInstance, final NotificationTrigger trigger )
    {
        return programStageInstance.getProgramStage().getNotificationTemplates().stream()
            .filter( t -> t.getNotificationTrigger() == trigger )
            .collect( Collectors.toSet() );
    }

    private DhisMessage createDhisMessage( ProgramStageInstance psi, ProgramNotificationTemplate template )
    {
        DhisMessage dhisMessage = new DhisMessage();

        dhisMessage.message = programStageNotificationRenderer.render( psi, template );
        dhisMessage.recipients = resolveDhisMessageRecipients( template, null, psi );

        return dhisMessage;
    }

    private DhisMessage createDhisMessage( ProgramInstance pi, ProgramNotificationTemplate template )
    {
        DhisMessage dhisMessage = new DhisMessage();

        dhisMessage.message = programNotificationRenderer.render( pi, template );

        dhisMessage.recipients = resolveDhisMessageRecipients( template, pi, null );

        return dhisMessage;
    }

    private void sendDhisMessages( Set<DhisMessage> messages )
    {
        messages.forEach( m ->
            messageService.sendMessage( m.message.getSubject(), m.message.getMessage(), null, m.recipients, null,
                MessageType.SYSTEM, true )
        );
    }

    private void sendProgramMessages( Set<ProgramMessage> messages )
    {
        if ( messages.isEmpty() )
        {
            return;
        }

        log.debug( String.format( "Dispatching %d ProgramMessages", messages.size() ) );

        BatchResponseStatus status = programMessageService.sendMessages( Lists.newArrayList( messages ) );

        log.debug( String.format( "Resulting status from ProgramMessageService:\n %s", status.toString() ) );
    }

    private void sendAll( MessageBatch messageBatch )
    {
        sendDhisMessages( messageBatch.dhisMessages );
        sendProgramMessages( messageBatch.programMessages );
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
        Set<DhisMessage> dhisMessages = Sets.newHashSet();
        Set<ProgramMessage> programMessages = Sets.newHashSet();

        MessageBatch( MessageBatch ...batches )
        {
            for ( MessageBatch batch : batches )
            {
                dhisMessages.addAll( batch.dhisMessages );
                programMessages.addAll( batch.programMessages );
            }
        }

        int messageCount()
        {
            return dhisMessages.size() + programMessages.size();
        }
    }
}
