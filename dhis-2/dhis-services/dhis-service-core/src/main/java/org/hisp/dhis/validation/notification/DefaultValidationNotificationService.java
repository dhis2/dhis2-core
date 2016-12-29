package org.hisp.dhis.validation.notification;

/*
 * Copyright (c) 2004-2016, University of Oslo
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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageBatchService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.validation.ValidationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Halvdan Hoem Grelland
 */
public class DefaultValidationNotificationService
    implements ValidationNotificationService
{
    private static final Log log = LogFactory.getLog( DefaultValidationNotificationService.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private NotificationMessageRenderer<ValidationResult> notificationMessageRenderer;

    public void setNotificationMessageRenderer( NotificationMessageRenderer<ValidationResult> notificationMessageRenderer )
    {
        this.notificationMessageRenderer = notificationMessageRenderer;
    }

    private OutboundMessageBatchService messageBatchService;

    public void setMessageBatchService( OutboundMessageBatchService messageBatchService )
    {
        this.messageBatchService = messageBatchService;
    }

    private MessageService messageService;

    public void setMessageService( MessageService messageService )
    {
        this.messageService = messageService;
    }

    // -------------------------------------------------------------------------
    // ValidationNotificationService implementation
    // -------------------------------------------------------------------------

    @Override
    public void sendNotifications( Set<ValidationResult> validationResults )
    {
        Stream<Message> messages = filterNullAndEmpty( validationResults.stream() )
            .flatMap( this::createMessages );

        // Split messages by type
        Map<MessageType, Set<Message>> messagesByType =
            messages.collect( Collectors.groupingBy( MessageType::getTypeFor, Collectors.toSet() ) );

        // Do dispatching of dhis message and external messages in parallel
        messagesByType.entrySet().parallelStream()
            .forEach( entry -> send( entry.getKey(), entry.getValue() ) );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void send( MessageType type, Set<Message> messages )
    {
        switch ( type )
        {
            case OUTBOUND:
                sendOutboundMessages( messages );
                break;
            case INTERNAL:
                sendDhisMessages( messages );
                break;
        }
    }

    /**
     * Ensure null-safety when processing the stream.
     */
    private static Stream<ValidationResult> filterNullAndEmpty( Stream<ValidationResult> validationResults )
    {
        return validationResults
            .filter( Objects::nonNull )
            .filter( v -> Objects.nonNull( v.getValidationRule() ) )
            .filter( v -> !v.getValidationRule().getNotificationTemplates().isEmpty() );
    }

    private Stream<Message> createMessages( final ValidationResult validationResult )
    {
        return validationResult.getValidationRule().getNotificationTemplates().stream()
            .map( template ->
                new Message(
                    createNotification( validationResult, template ),
                    createRecipients( validationResult, template )
                )
            );
    }

    private NotificationMessage createNotification( ValidationResult validationResult, ValidationNotificationTemplate template )
    {
        return notificationMessageRenderer.render( validationResult, template );
    }

    private Recipients createRecipients( ValidationResult validationResult, ValidationNotificationTemplate template )
    {
        if ( template.getNotificationRecipient().isExternalRecipient() )
        {
            return new Recipients( recipientsFromOrgUnit( validationResult, template ) );
        }
        else
        {
            return new Recipients( recipientsFromUserGroups( validationResult, template ) );
        }
    }

    private static Map<DeliveryChannel, Set<String>> recipientsFromOrgUnit(
        final ValidationResult validationResult, ValidationNotificationTemplate template )
    {
        Set<DeliveryChannel> channels = template.getDeliveryChannels();

        if ( channels.isEmpty() )
        {
            return Maps.newHashMap();
        }

        Map<DeliveryChannel, Set<String>> channelPrincipalMap = new EnumMap<>( DeliveryChannel.class );

        OrganisationUnit organisationUnit = validationResult.getOrgUnit();

        if ( organisationUnit == null )
        {
            return Maps.newHashMap();
        }

        // Resolve e-mail address and/or phone number where applicable
        // Multiple recipients per delivery channel are supported, but
        // and org unit can only have one of each, hence the singleton sets.

        // E-mail address

        String email = organisationUnit.getEmail();

        if ( StringUtils.isNotBlank( email ) && channels.contains( DeliveryChannel.EMAIL ) )
        {
            channelPrincipalMap.put( DeliveryChannel.EMAIL, Collections.singleton( email ) );
        }

        // Phone numbers

        String phone = organisationUnit.getPhoneNumber();

        if ( StringUtils.isNotBlank( phone ) && channels.contains( DeliveryChannel.SMS ) )
        {
            channelPrincipalMap.put( DeliveryChannel.SMS, Collections.singleton( phone ) );
        }

        return channelPrincipalMap;
    }

    private static Set<User> recipientsFromUserGroups( final ValidationResult validationResult, ValidationNotificationTemplate template )
    {
        // Limit recipients to be withing org unit hierarchy only, effectively
        // producing a cross-cut of all users in the configured user groups.

        final boolean limitToHierarchy = template.getNotifyUsersInHierarchyOnly();

        Set<OrganisationUnit> orgUnitsToInclude = Sets.newHashSet();

        if ( limitToHierarchy )
        {
            orgUnitsToInclude.add( validationResult.getOrgUnit() ); // Include self
            orgUnitsToInclude.addAll( validationResult.getOrgUnit().getAncestors() );
        }

        // Get all distinct users in configured user groups
        // Limit (only if configured) to the pre-computed set of ancestors

        return template.getRecipientUserGroups().stream()
            .flatMap( ug -> ug.getMembers().stream() )
            .distinct()
            .filter( user -> !limitToHierarchy /* pass-through */ || orgUnitsToInclude.contains( user.getOrganisationUnit() ) )
            .collect( Collectors.toSet() );
    }

    private void sendDhisMessages( Set<Message> messages )
    {
        messages.forEach( message -> {
                String msgSubj = message.notificationMessage.getSubject();
                String msgText = message.notificationMessage.getMessage();

                Set<User> rcpt = message.recipients.userRecipients.map( r -> r ).orElse( Sets.newHashSet() );

                messageService.sendMessage( msgSubj, msgText, null, rcpt );
            }
        );
    }

    /**
     * TODO Need to split-and-copy messages by delivery channel.
     *      Could consider changing the singular DC on OutboundMessageBatch to a Set instead (?)
     */
    private void sendOutboundMessages( Set<Message> messages )
    {
        List<OutboundMessageBatch> outboundBatches = createOutboundMessageBatches( messages );
        messageBatchService.sendBatches( outboundBatches );
    }

    private static List<OutboundMessageBatch> createOutboundMessageBatches( Set<Message> messages )
    {
        List<OutboundMessage> smsMessages = new ArrayList<>();
        List<OutboundMessage> emailMessages = new ArrayList<>();

        for ( Message message : messages )
        {
            NotificationMessage notification = message.notificationMessage;

            Map<DeliveryChannel, Set<String>> recipients = message.recipients.externalRecipients.map( r -> r ).orElse( Maps.newHashMap() );

            Set<String> smsRcpt = recipients.getOrDefault( DeliveryChannel.SMS, Sets.newHashSet() );
            Set<String> emailRcpt = recipients.getOrDefault( DeliveryChannel.EMAIL, Sets.newHashSet() );

            if ( !smsRcpt.isEmpty() )
            {
                smsMessages.add( new OutboundMessage( notification.getSubject(), notification.getMessage(), smsRcpt ) );
            }

            if ( !emailRcpt.isEmpty() )
            {
                emailMessages.add( new OutboundMessage( notification.getSubject(), notification.getMessage(), emailRcpt ) );
            }
        }

        OutboundMessageBatch smsBatch = new OutboundMessageBatch( smsMessages, DeliveryChannel.SMS )
        OutboundMessageBatch emailBatch = new OutboundMessageBatch( emailMessages, DeliveryChannel.EMAIL );

        return Lists.newArrayList( smsBatch, emailBatch );
    }

    private static OutboundMessage toOutboundMessage( Message message, DeliveryChannel channel )
    {
        String subText = message.notificationMessage.getSubject();
        String msgText = message.notificationMessage.getMessage();

        Set<String> recipients =
            message.recipients.externalRecipients.map( rcpt -> rcpt.get( channel ) ).orElse( Sets.newHashSet() );

        return new OutboundMessage( subText, msgText, recipients );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    @SuppressWarnings( "OptionalUsedAsFieldOrParameterType" )
    private static class Recipients
    {
        final Optional<Set<User>> userRecipients;
        final Optional<Map<DeliveryChannel, Set<String>>> externalRecipients;

        Recipients( Set<User> userRecipients )
        {
            this.userRecipients = Optional.of( userRecipients );
            this.externalRecipients = Optional.empty();
        }

        Recipients( Map<DeliveryChannel, Set<String>> externalRecipients )
        {
            this.userRecipients = Optional.empty();
            this.externalRecipients = Optional.of( externalRecipients );
        }

        boolean isExternal()
        {
            return externalRecipients.isPresent();
        }
    }

    private static class Message
    {
        final NotificationMessage notificationMessage;
        final Recipients recipients;

        public Message( NotificationMessage notificationMessage, Recipients recipients )
        {
            this.notificationMessage = notificationMessage;
            this.recipients = recipients;
        }
    }

    private enum MessageType
    {
        OUTBOUND, INTERNAL;

        static MessageType getTypeFor( Message message )
        {
            return message.recipients.isExternal() ? OUTBOUND : INTERNAL;
        }
    }
}
