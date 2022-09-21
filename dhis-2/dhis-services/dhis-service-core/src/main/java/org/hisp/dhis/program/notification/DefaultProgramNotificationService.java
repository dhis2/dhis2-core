/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;
<<<<<<< HEAD
=======

>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
import org.apache.commons.lang.BooleanUtils;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.message.MessageConversationParams;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.message.MessageType;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Halvdan Hoem Grelland
 */
@Slf4j
@Service( "org.hisp.dhis.program.notification.ProgramNotificationService" )
public class DefaultProgramNotificationService
    implements ProgramNotificationService
{
<<<<<<< HEAD
    private static final Predicate<ProgramNotificationInstance> IS_SCHEDULED_BY_PROGRAM_RULE = pnt ->
        Objects.nonNull( pnt ) && NotificationTrigger.PROGRAM_RULE.equals( pnt.getProgramNotificationTemplate().getNotificationTrigger() ) &&
            pnt.getScheduledAt() != null && DateUtils.isToday( pnt.getScheduledAt() );
=======
    private static final Predicate<ProgramNotificationInstance> IS_SCHEDULED_BY_PROGRAM_RULE = pnt -> Objects
        .nonNull( pnt )
        && NotificationTrigger.PROGRAM_RULE.equals( pnt.getProgramNotificationTemplate().getNotificationTrigger() ) &&
        pnt.getScheduledAt() != null && DateUtils.isToday( pnt.getScheduledAt() );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final ProgramMessageService programMessageService;

    private final MessageService messageService;

    private final ProgramInstanceStore programInstanceStore;

    private final ProgramStageInstanceStore programStageInstanceStore;

    private final IdentifiableObjectManager identifiableObjectManager;

    private final NotificationMessageRenderer<ProgramInstance> programNotificationRenderer;

    private final NotificationMessageRenderer<ProgramStageInstance> programStageNotificationRenderer;

    private final ProgramNotificationTemplateStore notificationTemplateStore;

    public DefaultProgramNotificationService( ProgramMessageService programMessageService,
        MessageService messageService, ProgramInstanceStore programInstanceStore,
        ProgramStageInstanceStore programStageInstanceStore, IdentifiableObjectManager identifiableObjectManager,
        NotificationMessageRenderer<ProgramInstance> programNotificationRenderer,
        NotificationMessageRenderer<ProgramStageInstance> programStageNotificationRenderer,
        ProgramNotificationTemplateStore notificationTemplateStore )
    {

        checkNotNull( programMessageService );
        checkNotNull( messageService );
        checkNotNull( programInstanceStore );
        checkNotNull( programStageInstanceStore );
        checkNotNull( identifiableObjectManager );
        checkNotNull( programNotificationRenderer );
        checkNotNull( programStageNotificationRenderer );
        checkNotNull( notificationTemplateStore );

        this.programMessageService = programMessageService;
        this.messageService = messageService;
        this.programInstanceStore = programInstanceStore;
        this.programStageInstanceStore = programStageInstanceStore;
        this.identifiableObjectManager = identifiableObjectManager;
        this.programNotificationRenderer = programNotificationRenderer;
        this.programStageNotificationRenderer = programStageNotificationRenderer;
        this.notificationTemplateStore = notificationTemplateStore;
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
    public void sendScheduledNotifications()
    {
        Clock clock = new Clock( log ).startClock()
            .logTime( "Processing ProgramStageNotification messages scheduled by program rules" );

        List<ProgramNotificationInstance> templates = identifiableObjectManager
            .getAll( ProgramNotificationInstance.class ).stream()
            .filter( IS_SCHEDULED_BY_PROGRAM_RULE ).collect( Collectors.toList() );

        if ( templates.isEmpty() )
        {
            return;
        }

        int totalMessageCount;

        List<MessageBatch> batches = templates.stream().filter( ProgramNotificationInstance::hasProgramInstance )
            .map( t -> createProgramInstanceMessageBatch( t.getProgramNotificationTemplate(),
                Collections.singletonList( t.getProgramInstance() ) ) )
            .collect( Collectors.toList() );

        batches.addAll( templates.stream().filter( ProgramNotificationInstance::hasProgramStageInstance )
            .map( t -> createProgramStageInstanceMessageBatch( t.getProgramNotificationTemplate(),
                Collections.singletonList( t.getProgramStageInstance() ) ) )
            .collect( Collectors.toList() ) );

        batches.forEach( this::sendAll );

        totalMessageCount = batches.stream().mapToInt( MessageBatch::messageCount ).sum();

        clock.logTime( String.format( "Created and sent %d messages in %s", totalMessageCount, clock.time() ) );
    }

    @Transactional
    @Override
    public void sendEventCompletionNotifications( long programStageInstance )
    {
        sendProgramStageInstanceNotifications( programStageInstanceStore.get( programStageInstance ),
            NotificationTrigger.COMPLETION );
    }

    @Transactional
    @Override
    public void sendEnrollmentCompletionNotifications( long programInstance )
    {
        sendProgramInstanceNotifications( programInstanceStore.get( programInstance ), NotificationTrigger.COMPLETION );
    }

    @Transactional
    @Override
    public void sendEnrollmentNotifications( long programInstance )
    {
        sendProgramInstanceNotifications( programInstanceStore.get( programInstance ), NotificationTrigger.ENROLLMENT );
    }

    @Transactional
    @Override
    public void sendProgramRuleTriggeredNotifications( long pnt, long programInstance )
    {
        MessageBatch messageBatch = createProgramInstanceMessageBatch( notificationTemplateStore.get( pnt ),
            Collections.singletonList( programInstanceStore.get( programInstance ) ) );
        sendAll( messageBatch );
    }

    @Transactional
    @Override
    public void sendProgramRuleTriggeredEventNotifications( long pnt, long programStageInstance )
    {
        MessageBatch messageBatch = createProgramStageInstanceMessageBatch( notificationTemplateStore.get( pnt ),
            Collections.singletonList( programStageInstanceStore.get( programStageInstance ) ) );
        sendAll( messageBatch );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private MessageBatch createScheduledMessageBatchForDay( ProgramNotificationTemplate template, Date day )
    {
        List<ProgramStageInstance> programStageInstances = programStageInstanceStore
            .getWithScheduledNotifications( template, day );

        List<ProgramInstance> programInstances = programInstanceStore.getWithScheduledNotifications( template, day );

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

    private void sendProgramStageInstanceNotifications( ProgramStageInstance programStageInstance,
        NotificationTrigger trigger )
    {
        if ( programStageInstance == null )
        {
            return;
        }

        Set<ProgramNotificationTemplate> templates = resolveTemplates( programStageInstance, trigger );

        if ( templates.isEmpty() )
        {
            return;
        }

        for ( ProgramNotificationTemplate template : templates )
        {
            MessageBatch batch = createProgramStageInstanceMessageBatch( template,
                Lists.newArrayList( programStageInstance ) );
            sendAll( batch );
        }
    }

    private void sendProgramInstanceNotifications( ProgramInstance programInstance, NotificationTrigger trigger )
    {
        if ( programInstance == null )
        {
            return;
        }

        Set<ProgramNotificationTemplate> templates = resolveTemplates( programInstance, trigger );

        for ( ProgramNotificationTemplate template : templates )
        {
            MessageBatch batch = createProgramInstanceMessageBatch( template, Lists.newArrayList( programInstance ) );
            sendAll( batch );
        }
    }

    private MessageBatch createProgramStageInstanceMessageBatch( ProgramNotificationTemplate template,
        List<ProgramStageInstance> programStageInstances )
    {
        MessageBatch batch = new MessageBatch();

        if ( template.getNotificationRecipient().isExternalRecipient() )
        {
            batch.programMessages.addAll(
                programStageInstances.stream()
                    .map( psi -> createProgramMessage( psi, template ) )
                    .collect( Collectors.toSet() ) );
        }
        else
        {
            batch.dhisMessages.addAll(
                programStageInstances.stream()
                    .map( psi -> createDhisMessage( psi, template ) )
                    .collect( Collectors.toSet() ) );
        }

        return batch;
    }

    private MessageBatch createProgramInstanceMessageBatch( ProgramNotificationTemplate template,
        List<ProgramInstance> programInstances )
    {
        MessageBatch batch = new MessageBatch();

        if ( template.getNotificationRecipient().isExternalRecipient() )
        {
            batch.programMessages.addAll(
                programInstances.stream()
                    .map( pi -> createProgramMessage( pi, template ) )
                    .collect( Collectors.toSet() ) );
        }
        else
        {
            batch.dhisMessages.addAll(
                programInstances.stream()
                    .map( ps -> createDhisMessage( ps, template ) )
                    .collect( Collectors.toSet() ) );
        }

        return batch;
    }

    private ProgramMessage createProgramMessage( ProgramStageInstance psi, ProgramNotificationTemplate template )
    {
        NotificationMessage message = programStageNotificationRenderer.render( psi, template );

        return new ProgramMessage(
            message.getSubject(), message.getMessage(),
            resolveProgramStageNotificationRecipients( template, psi.getOrganisationUnit(),
                psi ),
            Sets.newHashSet( template.getDeliveryChannels() ), psi );
    }

    private ProgramMessage createProgramMessage( ProgramInstance programInstance, ProgramNotificationTemplate template )
    {
        NotificationMessage message = programNotificationRenderer.render( programInstance, template );

        return new ProgramMessage(
            message.getSubject(), message.getMessage(),
            resolveProgramNotificationRecipients( template, programInstance.getOrganisationUnit(), programInstance ),
            Sets.newHashSet( template.getDeliveryChannels() ), programInstance );
    }

    private Set<User> resolveDhisMessageRecipients(
        ProgramNotificationTemplate template, @Nullable ProgramInstance programInstance,
        @Nullable ProgramStageInstance programStageInstance )
    {
        if ( programInstance == null && programStageInstance == null )
        {
            throw new IllegalArgumentException(
                "Either of the arguments [programInstance, programStageInstance] must be non-null" );
        }

        Set<User> recipients = Sets.newHashSet();

        OrganisationUnit eventOrgUnit = programInstance != null ? programInstance.getOrganisationUnit()
            : programStageInstance.getOrganisationUnit();

        Set<OrganisationUnit> orgUnitInHierarchy = Sets.newHashSet();

        ProgramNotificationRecipient recipientType = template.getNotificationRecipient();

        if ( recipientType == ProgramNotificationRecipient.USER_GROUP )
        {
            recipients = Optional.ofNullable( template )
                .map( ProgramNotificationTemplate::getRecipientUserGroup )
                .map( UserGroup::getMembers )
                .orElse( recipients );

            final boolean limitToHierarchy = BooleanUtils.toBoolean( template.getNotifyUsersInHierarchyOnly() );

            final boolean parentOrgUnitOnly = BooleanUtils.toBoolean( template.getNotifyParentOrganisationUnitOnly() );

            if ( limitToHierarchy )
            {
                orgUnitInHierarchy.add( eventOrgUnit );
                orgUnitInHierarchy.addAll( eventOrgUnit.getAncestors() );

                recipients = recipients.stream().filter( r -> orgUnitInHierarchy.contains( r.getOrganisationUnit() ) )
                    .collect( Collectors.toSet() );

                return recipients;
            }
            else if ( parentOrgUnitOnly )
            {
                Set<User> parents = Sets.newHashSet();

                recipients.forEach( r -> parents.addAll( r.getOrganisationUnit().getParent().getUsers() ) );

                return parents;
            }

            recipients.addAll( template.getRecipientUserGroup().getMembers() );
        }
        else if ( recipientType == ProgramNotificationRecipient.USERS_AT_ORGANISATION_UNIT )
        {
            recipients.addAll( eventOrgUnit.getUsers() );
        }

        return recipients;
    }

    private ProgramMessageRecipients resolveProgramNotificationRecipients(
        ProgramNotificationTemplate template, OrganisationUnit organisationUnit, ProgramInstance programInstance )
    {
        return resolveRecipients( template, organisationUnit, programInstance.getEntityInstance(), programInstance );
    }

    private ProgramMessageRecipients resolveProgramStageNotificationRecipients(
        ProgramNotificationTemplate template, OrganisationUnit organisationUnit, ProgramStageInstance psi )
    {
        ProgramMessageRecipients recipients = new ProgramMessageRecipients();

        if ( template.getNotificationRecipient() == ProgramNotificationRecipient.DATA_ELEMENT
            && template.getRecipientDataElement() != null )
        {
            List<String> recipientList = psi.getEventDataValues().stream()
                .filter( dv -> template.getRecipientDataElement().getUid().equals( dv.getDataElement() ) )
                .map( EventDataValue::getValue )
                .collect( Collectors.toList() );

            if ( template.getDeliveryChannels().contains( DeliveryChannel.SMS ) )
            {
                recipients.getPhoneNumbers().addAll( recipientList );
            }
            else if ( template.getDeliveryChannels().contains( DeliveryChannel.EMAIL ) )
            {
                recipients.getEmailAddresses().addAll( recipientList );
            }

            return recipients;
        }
        else
        {
            TrackedEntityInstance trackedEntityInstance = psi.getProgramInstance().getEntityInstance();

            return resolveRecipients( template, organisationUnit, trackedEntityInstance, psi.getProgramInstance() );
        }
    }

    private ProgramMessageRecipients resolveRecipients( ProgramNotificationTemplate template, OrganisationUnit ou,
        TrackedEntityInstance tei, ProgramInstance pi )
    {
        ProgramMessageRecipients recipients = new ProgramMessageRecipients();

        ProgramNotificationRecipient recipientType = template.getNotificationRecipient();

        if ( recipientType == ProgramNotificationRecipient.ORGANISATION_UNIT_CONTACT )
        {
            recipients.setOrganisationUnit( ou );
        }
        else if ( recipientType == ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE )
        {
            recipients.setTrackedEntityInstance( tei );
        }
        else if ( recipientType == ProgramNotificationRecipient.PROGRAM_ATTRIBUTE
            && template.getRecipientProgramAttribute() != null )
        {
            List<String> recipientList = pi.getEntityInstance().getTrackedEntityAttributeValues().stream()
                .filter( av -> template.getRecipientProgramAttribute().getUid().equals( av.getAttribute().getUid() ) )
                .map( TrackedEntityAttributeValue::getPlainValue )
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

    private Set<ProgramNotificationTemplate> resolveTemplates( ProgramInstance programInstance,
        final NotificationTrigger trigger )
    {
        return programInstance.getProgram().getNotificationTemplates().stream()
            .filter( t -> t.getNotificationTrigger() == trigger )
            .collect( Collectors.toSet() );
    }

    private Set<ProgramNotificationTemplate> resolveTemplates( ProgramStageInstance programStageInstance,
        final NotificationTrigger trigger )
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
        messages.forEach( m -> messageService.sendMessage(
            new MessageConversationParams.Builder( m.recipients, null, m.message.getSubject(), m.message.getMessage(),
                MessageType.SYSTEM )
                    .withForceNotification( true )
                    .build() ) );
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

        MessageBatch( MessageBatch... batches )
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
