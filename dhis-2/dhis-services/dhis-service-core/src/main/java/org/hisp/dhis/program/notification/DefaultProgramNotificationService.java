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
package org.hisp.dhis.program.notification;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.program.notification.NotificationTrigger.PROGRAM_RULE;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM_OUTLIER;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventStore;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.program.notification.template.snapshot.NotificationTemplateMapper;
import org.hisp.dhis.scheduling.JobProgress;
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
@RequiredArgsConstructor
@Service( "org.hisp.dhis.program.notification.ProgramNotificationService" )
public class DefaultProgramNotificationService
    implements ProgramNotificationService
{
    private static final Predicate<NotificationInstanceWithTemplate> IS_SCHEDULED_BY_PROGRAM_RULE = (
        iwt ) -> Objects.nonNull( iwt.getProgramNotificationInstance() ) &&
            PROGRAM_RULE.equals( iwt.getProgramNotificationTemplate().getNotificationTrigger() ) &&
            iwt.getProgramNotificationInstance().getScheduledAt() != null &&
            DateUtils.isToday( iwt.getProgramNotificationInstance().getScheduledAt() );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Nonnull
    private final ProgramMessageService programMessageService;

    @Nonnull
    private final MessageService messageService;

    @Nonnull
    private final ProgramInstanceStore programInstanceStore;

    @Nonnull
    private final EventStore eventStore;

    @Nonnull
    private final IdentifiableObjectManager identifiableObjectManager;

    @Nonnull
    private final NotificationMessageRenderer<ProgramInstance> programNotificationRenderer;

    @Nonnull
    private final NotificationMessageRenderer<Event> programStageNotificationRenderer;

    @Nonnull
    private final ProgramNotificationTemplateService notificationTemplateService;

    @Nonnull
    private final NotificationTemplateMapper notificationTemplateMapper;

    // -------------------------------------------------------------------------
    // ProgramStageNotificationService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void sendScheduledNotificationsForDay( Date notificationDate, JobProgress progress )
    {
        progress.startingStage( "Fetching and filtering scheduled templates " );
        List<ProgramNotificationTemplate> scheduledTemplates = progress.runStage( List.of(),
            this::getScheduledTemplates );

        progress.startingStage( "Processing ProgramStageNotification messages", scheduledTemplates.size(),
            SKIP_ITEM_OUTLIER );
        AtomicInteger totalMessageCount = new AtomicInteger();
        progress.runStage( scheduledTemplates.stream(),
            template -> "Processing template " + template.getName(),
            template -> {
                MessageBatch batch = createScheduledMessageBatchForDay( template, notificationDate );
                sendAll( batch );
                totalMessageCount.addAndGet( batch.messageCount() );
            },
            ( success, failed ) -> format( "Created and sent %d messages", totalMessageCount.get() ) );
    }

    @Override
    @Transactional
    public void sendScheduledNotifications( JobProgress progress )
    {
        progress.startingStage( "Fetching and filtering ProgramStageNotification messages scheduled by program rules" );
        List<NotificationInstanceWithTemplate> instancesWithTemplates = progress.runStage( List.of(),
            () -> identifiableObjectManager.getAll( ProgramNotificationInstance.class ).stream()
                .map( this::withTemplate )
                .filter( this::hasTemplate )
                .filter( IS_SCHEDULED_BY_PROGRAM_RULE )
                .collect( toList() ) );

        progress.startingStage( "Processing ProgramStageNotification messages scheduled by program rules",
            instancesWithTemplates.size(), SKIP_ITEM_OUTLIER );
        if ( instancesWithTemplates.isEmpty() )
        {
            progress.completedStage( "No instances with templates found." );
            return;
        }

        List<MessageBatch> batches = progress.runStage( List.of(), () -> {
            Stream<MessageBatch> programInstanceBatches = instancesWithTemplates.stream()
                .filter( this::hasProgramInstance )
                .map( iwt -> createProgramInstanceMessageBatch(
                    iwt.getProgramNotificationTemplate(),
                    List.of( iwt.getProgramNotificationInstance().getProgramInstance() ) ) );

            Stream<MessageBatch> programStageInstanceBatches = instancesWithTemplates.stream()
                .filter( this::hasEvent )
                .map( iwt -> createEventMessageBatch(
                    iwt.getProgramNotificationTemplate(),
                    List.of( iwt.getProgramNotificationInstance().getEvent() ) ) );

            return Stream.concat( programInstanceBatches, programStageInstanceBatches ).collect( toList() );
        } );

        progress.startingStage( "Sending message batches", batches.size(), SKIP_ITEM_OUTLIER );
        progress.runStage( batches.stream(),
            batch -> format( "Sending batch with %d DHIS messages and %d program messages",
                batch.dhisMessages.size(), batch.programMessages.size() ),
            this::sendAll,
            ( success, failed ) -> format( "Created and sent %d messages",
                batches.stream().mapToInt( MessageBatch::messageCount ).sum() ) );
    }

    private boolean hasEvent( NotificationInstanceWithTemplate notificationInstanceWithTemplate )
    {
        return Optional.of( notificationInstanceWithTemplate )
            .map( NotificationInstanceWithTemplate::getProgramNotificationInstance )
            .filter( ProgramNotificationInstance::hasEvent )
            .isPresent();
    }

    private boolean hasProgramInstance( NotificationInstanceWithTemplate instanceWithTemplate )
    {
        return Optional.of( instanceWithTemplate )
            .map( NotificationInstanceWithTemplate::getProgramNotificationInstance )
            .filter( ProgramNotificationInstance::hasProgramInstance )
            .isPresent();
    }

    private boolean hasTemplate( NotificationInstanceWithTemplate instanceWithTemplate )
    {
        if ( Objects.isNull( instanceWithTemplate.getProgramNotificationTemplate() ) )
        {
            log.warn( "Cannot process scheduled notification with id: "
                + instanceWithTemplate.getProgramNotificationInstance().getId()
                + " since it has no associated templates" );
            return false;
        }
        return true;
    }

    private NotificationInstanceWithTemplate withTemplate(
        ProgramNotificationInstance programNotificationInstance )
    {
        return NotificationInstanceWithTemplate.builder()
            .programNotificationInstance( programNotificationInstance )
            .programNotificationTemplate( getApplicableTemplate( programNotificationInstance ) )
            .build();
    }

    private ProgramNotificationTemplate getApplicableTemplate( ProgramNotificationInstance programNotificationInstance )
    {
        return Optional.of( programNotificationInstance )
            .map( ProgramNotificationInstance::getProgramNotificationTemplateSnapshot )
            .map( notificationTemplateMapper::toProgramNotificationTemplate )
            .orElseGet( () -> this.getDatabaseTemplate( programNotificationInstance ) );
    }

    private ProgramNotificationTemplate getDatabaseTemplate( ProgramNotificationInstance programNotificationInstance )
    {
        log.warn( "Couldn't use template from jsonb column, using the one from database if possible" );
        if ( Objects.nonNull( programNotificationInstance.getProgramNotificationTemplateId() ) )
        {
            ProgramNotificationTemplate programNotificationTemplate = notificationTemplateService
                .get( programNotificationInstance.getProgramNotificationTemplateId() );
            if ( Objects.isNull( programNotificationTemplate ) )
            {
                log.warn(
                    "Unable to load program notification template from database, because it might have been deleted." );
            }
            return programNotificationTemplate;
        }
        return null;
    }

    @Override
    @Transactional
    public void sendEventCompletionNotifications( long eventId )
    {
        sendEventNotifications( eventStore.get( eventId ),
            NotificationTrigger.COMPLETION );
    }

    @Override
    @Transactional
    public void sendEnrollmentCompletionNotifications( long programInstance )
    {
        sendProgramInstanceNotifications( programInstanceStore.get( programInstance ), NotificationTrigger.COMPLETION );
    }

    @Override
    @Transactional
    public void sendEnrollmentNotifications( long programInstance )
    {
        sendProgramInstanceNotifications( programInstanceStore.get( programInstance ), NotificationTrigger.ENROLLMENT );
    }

    @Override
    @Transactional
    public void sendProgramRuleTriggeredNotifications( long pnt, long programInstance )
    {
        MessageBatch messageBatch = createProgramInstanceMessageBatch( notificationTemplateService.get( pnt ),
            Collections.singletonList( programInstanceStore.get( programInstance ) ) );
        sendAll( messageBatch );
    }

    @Override
    @Transactional
    public void sendProgramRuleTriggeredNotifications( long pnt, ProgramInstance programInstance )
    {
        MessageBatch messageBatch = createProgramInstanceMessageBatch( notificationTemplateService.get( pnt ),
            Collections.singletonList( programInstance ) );
        sendAll( messageBatch );
    }

    @Override
    @Transactional
    public void sendProgramRuleTriggeredEventNotifications( long pnt, long eventId )
    {
        MessageBatch messageBatch = createEventMessageBatch( notificationTemplateService.get( pnt ),
            Collections.singletonList( eventStore.get( eventId ) ) );
        sendAll( messageBatch );
    }

    @Override
    @Transactional
    public void sendProgramRuleTriggeredEventNotifications( long pnt, Event event )
    {
        MessageBatch messageBatch = createEventMessageBatch( notificationTemplateService.get( pnt ),
            Collections.singletonList( eventStore.getByUid( event.getUid() ) ) );
        sendAll( messageBatch );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private MessageBatch createScheduledMessageBatchForDay( ProgramNotificationTemplate template, Date day )
    {
        List<Event> events = eventStore
            .getWithScheduledNotifications( template, day );

        List<ProgramInstance> programInstances = programInstanceStore.getWithScheduledNotifications( template, day );

        MessageBatch psiBatch = createEventMessageBatch( template, events );
        MessageBatch psBatch = createProgramInstanceMessageBatch( template, programInstances );

        return new MessageBatch( psiBatch, psBatch );
    }

    private List<ProgramNotificationTemplate> getScheduledTemplates()
    {
        return identifiableObjectManager.getAll( ProgramNotificationTemplate.class ).stream()
            .filter( n -> n.getNotificationTrigger().isScheduled() )
            .collect( toList() );
    }

    private void sendEventNotifications( Event event, NotificationTrigger trigger )
    {
        if ( event == null )
        {
            return;
        }

        Set<ProgramNotificationTemplate> templates = resolveTemplates( event, trigger );

        if ( templates.isEmpty() )
        {
            return;
        }

        for ( ProgramNotificationTemplate template : templates )
        {
            MessageBatch batch = createEventMessageBatch( template,
                Lists.newArrayList( event ) );
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

    private MessageBatch createEventMessageBatch( ProgramNotificationTemplate template, List<Event> events )
    {
        MessageBatch batch = new MessageBatch();

        if ( template.getNotificationRecipient().isExternalRecipient() )
        {
            batch.programMessages.addAll(
                events.stream()
                    .map( psi -> createProgramMessage( psi, template ) )
                    .collect( Collectors.toSet() ) );
        }
        else
        {
            batch.dhisMessages.addAll(
                events.stream()
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

    private ProgramMessage createProgramMessage( Event event, ProgramNotificationTemplate template )
    {
        NotificationMessage message = programStageNotificationRenderer.render( event, template );

        return ProgramMessage.builder().subject( message.getSubject() )
            .text( message.getMessage() )
            .recipients( resolveProgramStageNotificationRecipients( template, event.getOrganisationUnit(), event ) )
            .deliveryChannels( Sets.newHashSet( template.getDeliveryChannels() ) )
            .event( event )
            .notificationTemplate( Optional.ofNullable( template.getUid() ).orElse( StringUtils.EMPTY ) )
            .build();
    }

    private ProgramMessage createProgramMessage( ProgramInstance programInstance, ProgramNotificationTemplate template )
    {
        NotificationMessage message = programNotificationRenderer.render( programInstance, template );

        return ProgramMessage.builder().subject( message.getSubject() )
            .text( message.getMessage() )
            .recipients( resolveProgramNotificationRecipients( template, programInstance.getOrganisationUnit(),
                programInstance ) )
            .deliveryChannels( Sets.newHashSet( template.getDeliveryChannels() ) )
            .programInstance( programInstance )
            .notificationTemplate( Optional.ofNullable( template.getUid() ).orElse( StringUtils.EMPTY ) )
            .build();
    }

    private Set<User> resolveDhisMessageRecipients(
        ProgramNotificationTemplate template, @Nullable ProgramInstance programInstance,
        @Nullable Event event )
    {
        if ( programInstance == null && event == null )
        {
            throw new IllegalArgumentException(
                "Either of the arguments [programInstance, event] must be non-null" );
        }

        Set<User> recipients = Sets.newHashSet();

        OrganisationUnit eventOrgUnit = programInstance != null ? programInstance.getOrganisationUnit()
            : event.getOrganisationUnit();

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
        ProgramNotificationTemplate template, OrganisationUnit organisationUnit, Event event )
    {
        ProgramMessageRecipients recipients = new ProgramMessageRecipients();

        if ( template.getNotificationRecipient() == ProgramNotificationRecipient.DATA_ELEMENT
            && template.getRecipientDataElement() != null )
        {
            List<String> recipientList = event.getEventDataValues().stream()
                .filter( dv -> template.getRecipientDataElement().getUid().equals( dv.getDataElement() ) )
                .map( EventDataValue::getValue )
                .collect( toList() );

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
            TrackedEntityInstance trackedEntityInstance = event.getProgramInstance().getEntityInstance();

            return resolveRecipients( template, organisationUnit, trackedEntityInstance, event.getProgramInstance() );
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
                .collect( toList() );

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

    private Set<ProgramNotificationTemplate> resolveTemplates( Event event,
        final NotificationTrigger trigger )
    {
        return event.getProgramStage().getNotificationTemplates().stream()
            .filter( t -> t.getNotificationTrigger() == trigger )
            .collect( Collectors.toSet() );
    }

    private DhisMessage createDhisMessage( Event event, ProgramNotificationTemplate template )
    {
        DhisMessage dhisMessage = new DhisMessage();

        dhisMessage.message = programStageNotificationRenderer.render( event, template );
        dhisMessage.recipients = resolveDhisMessageRecipients( template, null, event );

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
                MessageType.SYSTEM, null )
                    .withForceNotification( true )
                    .build() ) );
    }

    private void sendProgramMessages( Set<ProgramMessage> messages )
    {
        if ( messages.isEmpty() )
        {
            return;
        }

        log.debug( format( "Dispatching %d ProgramMessages", messages.size() ) );

        BatchResponseStatus status = programMessageService.sendMessages( Lists.newArrayList( messages ) );

        log.debug( format( "Resulting status from ProgramMessageService:%n %s", status.toString() ) );
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

    @Data
    @Builder
    static class NotificationInstanceWithTemplate
    {
        private final ProgramNotificationInstance programNotificationInstance;

        private final ProgramNotificationTemplate programNotificationTemplate;
    }
}
