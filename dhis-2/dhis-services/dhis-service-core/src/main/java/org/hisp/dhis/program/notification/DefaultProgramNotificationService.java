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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.util.DebugUtils;
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
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.user.User;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Halvdan Hoem Grelland
 */
public class DefaultProgramNotificationService
    implements ProgramNotificationService
{
    private static final Log log = LogFactory.getLog( DefaultProgramNotificationService.class );

    private static final Predicate<ProgramNotificationInstance> IS_SCHEDULED_BY_PROGRAM_RULE = pnt ->
        Objects.nonNull( pnt ) && NotificationTrigger.PROGRAM_RULE.equals( pnt.getProgramNotificationTemplate().getNotificationTrigger() ) &&
        pnt.getScheduledAt() != null && DateUtils.isToday( pnt.getScheduledAt() );

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

    private ProgramNotificationTemplateStore notificationTemplateStore;

    public void setProgramNotificationStore( ProgramNotificationTemplateStore notificationTemplateStore )
    {
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

        List<ProgramNotificationInstance> templates = identifiableObjectManager.getAll( ProgramNotificationInstance.class ).stream()
            .filter( IS_SCHEDULED_BY_PROGRAM_RULE ).collect( Collectors.toList() );

        if ( templates.isEmpty() )
        {
            return;
        }

        int totalMessageCount = 0;

        List<MessageBatch> batches = templates.stream().filter( ProgramNotificationInstance::hasProgramInstance )
            .map( t -> createProgramInstanceMessageBatch( t.getProgramNotificationTemplate(), Arrays.asList( t.getProgramInstance() ) ) )
            .collect( Collectors.toList() );

        batches.addAll( templates.stream().filter( ProgramNotificationInstance::hasProgramStageInstance )
            .map( t -> createProgramStageInstanceMessageBatch( t.getProgramNotificationTemplate(), Arrays.asList( t.getProgramStageInstance() ) ) )
            .collect( Collectors.toList() ) );

        batches.stream().forEach( this::sendAll );

        totalMessageCount = batches.stream().mapToInt( MessageBatch::messageCount ).sum();

        clock.logTime( String.format( "Created and sent %d messages in %s", totalMessageCount, clock.time() ) );
    }

    @Transactional
    @Override
    public void sendCompletionNotifications( ProgramStageInstance programStageInstance )
    {
        if ( programStageInstance == null )
        {
            return;
        }

        sendProgramStageInstanceNotifications( programStageInstanceStore.get( programStageInstance.getId() ) , NotificationTrigger.COMPLETION );
    }

    @Transactional
    @Override
    public void sendCompletionNotifications( ProgramInstance programInstance )
    {
        if ( programInstance == null )
        {
            return;
        }

        sendProgramInstanceNotifications( programInstanceStore.get( programInstance.getId() ), NotificationTrigger.COMPLETION );
    }

    @Transactional
    @Override
    public void sendEnrollmentNotifications( ProgramInstance programInstance )
    {
        if ( programInstance == null )
        {
            return;
        }

        sendProgramInstanceNotifications( programInstanceStore.get( programInstance.getId() ), NotificationTrigger.ENROLLMENT );
    }

    @Transactional
    @Override
    public void sendProgramRuleTriggeredNotifications( ProgramNotificationTemplate pnt, ProgramInstance programInstance )
    {
        if ( programInstance == null || pnt == null )
        {
            return;
        }

        MessageBatch messageBatch = createProgramInstanceMessageBatch( notificationTemplateStore.get( pnt.getId() ),
            Collections.singletonList( programInstanceStore.get( programInstance.getId() ) ) );

        sendAll( messageBatch );
    }

    @Transactional
    @Override
    public void sendProgramRuleTriggeredNotifications( ProgramNotificationTemplate pnt, ProgramStageInstance programStageInstance )
    {
        if ( programStageInstance == null || pnt == null )
        {
            return;
        }

        MessageBatch messageBatch = createProgramStageInstanceMessageBatch( notificationTemplateStore.get( pnt.getId() ),
            Collections.singletonList( programStageInstanceStore.get( programStageInstance.getId() ) ) );

        sendAll( messageBatch );
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
            MessageBatch batch = createProgramStageInstanceMessageBatch( template, Lists.newArrayList( programStageInstance ) );
            sendAll( batch );
        }
    }

    private void sendProgramInstanceNotifications( ProgramInstance programInstance, NotificationTrigger trigger )
    {
        if ( programInstance == null  )
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
                message.getSubject(), message.getMessage(), resolveProgramStageNotificationRecipients( template, psi.getOrganisationUnit(),
                psi ), Sets.newHashSet( template.getDeliveryChannels() ), psi );
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
            ProgramNotificationTemplate template, @Nullable ProgramInstance programInstance, @Nullable ProgramStageInstance programStageInstance )
    {
        if ( programInstance == null && programStageInstance == null )
        {
            throw new IllegalArgumentException( "Either of the arguments [programInstance, programStageInstance] must be non-null" );
        }

        Set<User> recipients = Sets.newHashSet();

        OrganisationUnit eventOrgUnit = programInstance != null ? programInstance.getOrganisationUnit() : programStageInstance.getOrganisationUnit();

        Set<OrganisationUnit> orgUnitInHierarchy = Sets.newHashSet();

        ProgramNotificationRecipient recipientType = template.getNotificationRecipient();

        if ( recipientType == ProgramNotificationRecipient.USER_GROUP )
        {
             recipients = template.getRecipientUserGroup().getMembers();

            final boolean limitToHierarchy = BooleanUtils.toBoolean( template.getNotifyUsersInHierarchyOnly() );

            final boolean parentOrgUnitOnly = BooleanUtils.toBoolean( template.getNotifyParentOrganisationUnitOnly() );

            if ( limitToHierarchy )
            {
                orgUnitInHierarchy.add( eventOrgUnit );
                orgUnitInHierarchy.addAll( eventOrgUnit.getAncestors() );

                recipients = recipients.stream().filter( r -> orgUnitInHierarchy.contains( r.getOrganisationUnit() ) ).collect( Collectors.toSet() );

                return recipients;
            }
            else if ( parentOrgUnitOnly )
            {
                Set<User> parents = Sets.newHashSet();

                recipients.stream().forEach( r -> parents.addAll( r.getOrganisationUnit().getParent().getUsers() ) );

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
            List<String> recipientList = psi.getDataValues().stream()
                .filter( dv -> template.getRecipientDataElement().getUid().equals( dv.getDataElement().getUid() ) )
                .map( TrackedEntityDataValue::getValue )
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
                                                        TrackedEntityInstance tei, ProgramInstance pi)
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
            messageService.sendMessage(
                new MessageConversationParams.Builder( m.recipients, null, m.message.getSubject(), m.message.getMessage(), MessageType.SYSTEM )
                    .withForceNotification( true )
                    .build()
            )
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
        try
        {
            sendDhisMessages( messageBatch.dhisMessages );
            sendProgramMessages( messageBatch.programMessages );
        }
        catch ( Exception e )
        {
            log.error( DebugUtils.getStackTrace( e ) );
        }
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
