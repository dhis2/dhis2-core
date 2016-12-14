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

import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.user.User;
import org.hisp.dhis.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * @author Halvdan Hoem Grelland
 */
public class DefaultValidationNotificationService
    implements ValidationNotificationService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private NotificationMessageRenderer<ValidationResult> notificationRenderer;

    @Autowired
    private MessageService messageService;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DefaultValidationNotificationService()
    {
    }

    // -------------------------------------------------------------------------
    // ValidationNotificationService implementation
    // -------------------------------------------------------------------------

    @Override
    public void sendNotifications( Set<ValidationResult> results )
    {
        Set<ValidationResult> validationResults = retainNotifiable( results );

        Map<ValidationResult, Map<ValidationNotificationTemplate, NotificationMessage>> resultsWithNotifications =
            validationResults.stream()
                .map( validationResult -> ImmutablePair.of( validationResult, renderNotificationsForValidationResult( validationResult ) ) )
                .collect( Collectors.toMap( Pair::getLeft, Pair::getRight ) );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Set<MessageBatch> createBatches( Map<ValidationResult, Map<ValidationNotificationTemplate, NotificationMessage>> resultsWithNotifications )
    {
        for ( Map.Entry<ValidationResult, Map<ValidationNotificationTemplate, NotificationMessage>> entry : resultsWithNotifications.entrySet() )
        {
            // Unwrap... This is complicated
            ValidationResult result = entry.getKey();
            Map<ValidationNotificationTemplate, NotificationMessage> templateToMessageMap = entry.getValue();
            Set<ValidationNotificationTemplate> templates = templateToMessageMap.keySet();

            //templates.stream().forEach(  );
        }

        return null;
    }

    private MessageBatch createBatch( final ValidationResult validationResult, Map<ValidationNotificationTemplate, NotificationMessage> templateToNotificationMap )
    {
        MessageBatch batch = new MessageBatch();

        templateToNotificationMap.entrySet().forEach( entry -> {
            ValidationNotificationTemplate template = entry.getKey();
            NotificationMessage message = entry.getValue();

            if ( template.getNotificationRecipient().isExternalRecipient() )
            {
                Pair<Set<String>, Set<String>> recipients = resolveExternalRecipients( validationResult, template );
                batch.messages.add(new Message( message, recipients.getLeft(), recipients.getRight() ) );
            }
            else
            {
                Set<User> recipients = resolveUserRecipients( validationResult, template );
                batch.dhisMessages.add( new DhisMessage( message, recipients ) );
            }
        } );

        return batch;
    }

    private Recipients resolveRecipents( ValidationResult validationResult, ValidationNotificationTemplate template )
    {
        return new Recipients( resolveUserRecipients( template ), resolveExternalRecipients( validationResult, template ) );
    }

    private Set<User> resolveUserRecipients( ValidationResult validationResult, ValidationNotificationTemplate template )
    {
        template.getNotificationRecipient().isExternalRecipient()
    }

    private Pair<Set<String>, Set<String>> resolveExternalRecipients( ValidationResult validationResult, ValidationNotificationTemplate template )
    {

    }

    /**
     * Retains all ValidationResults which are non-null, have a non-null ValidationRule and
     * have at least one ValidationNotificationTemplate.
     */
    private static Set<ValidationResult> retainNotifiable( Set<ValidationResult> results )
    {
        return results.stream()
            .filter( Objects::nonNull )
            .filter( vr -> Objects.nonNull( vr.getValidationRule() ) )
            .filter( vr -> !vr.getValidationRule().getNotificationTemplates().isEmpty() )
            .collect( Collectors.toSet() );
    }

    private Map<ValidationNotificationTemplate, NotificationMessage> renderNotificationsForValidationResult( ValidationResult validationResult )
    {
        return validationResult.getValidationRule().getNotificationTemplates().stream()
                .collect(
                    Collectors.toMap(
                        identity(),
                        template -> render( validationResult, template )
                    )
                );
    }

    private NotificationMessage render( ValidationResult validationResult, ValidationNotificationTemplate template )
    {
        return notificationRenderer.render( validationResult, template );
    }

    // -------------------------------------------------------------------------
    // Internal classes
    // -------------------------------------------------------------------------

    private static class Recipients
    {
        final Set<User> userRecipients;
        final Map<DeliveryChannel, String> externalRecipients;

        Recipients( Set<User> userRecipients, Map<DeliveryChannel, String> externalRecipients )
        {
            this.userRecipients = userRecipients;
            this.externalRecipients = externalRecipients;
        }
    }

    private static class MessageBatch
    {
        Set<DhisMessage> dhisMessages = Sets.newHashSet();
        Set<Message> messages = Sets.newHashSet();

        MessageBatch() {}

        MessageBatch( MessageBatch ...batches )
        {
            for ( MessageBatch batch : batches )
            {
                dhisMessages.addAll( batch.dhisMessages );
                messages.addAll( batch.messages);
            }
        }

        int messageCount()
        {
            return dhisMessages.size() + messages.size();
        }
    }

    private static class DhisMessage
    {
        final NotificationMessage notification;
        final Set<User> recipients;

        public DhisMessage( NotificationMessage notification, Set<User> recipients )
        {
            this.notification = notification;
            this.recipients = recipients;
        }
    }

    private static class Message
    {
        final NotificationMessage notification;
        final Set<String> phoneNumbers;
        final Set<String> emailAddresses;

        public Message( NotificationMessage notification, Set<String> phoneNumbers, Set<String> emailAddresses )
        {
            this.notification = notification;
            this.phoneNumbers = phoneNumbers;
            this.emailAddresses = emailAddresses;
        }
    }
}
