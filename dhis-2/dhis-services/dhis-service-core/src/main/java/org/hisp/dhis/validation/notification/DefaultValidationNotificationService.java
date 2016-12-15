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

import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private OrganisationUnitService orgUnitService;

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
        Set<ValidationResult> validationResults = retainApplicableValidationResults( results );

        Map<ValidationResult, Map<ValidationNotificationTemplate, NotificationMessage>> resultsWithNotifications =
            validationResults.stream()
                .map( validationResult -> ImmutablePair.of( validationResult, renderNotificationsForValidationResult( validationResult ) ) )
                .collect( Collectors.toMap( Pair::getLeft, Pair::getRight ) );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Set<Message> createMessages( Map<ValidationResult, Map<ValidationNotificationTemplate, NotificationMessage>> resultsWithNotifications )
    {
        return resultsWithNotifications.entrySet().stream()
            .map( entry -> toMessageStream( entry.getKey(), entry.getValue() ) )
            .flatMap( identity() )
            .collect( Collectors.toSet() );
    }

    private Stream<Message> toMessageStream(
        final ValidationResult validationResult, Map<ValidationNotificationTemplate, NotificationMessage> templateToNotificationMap )
    {
        return templateToNotificationMap.entrySet().stream()
            .map( entry -> new Message( entry.getValue(), resolveRecipients( validationResult, entry.getKey() ) ) );
    }

    // TODO Implement actual resolving...
    private Recipients resolveRecipients( ValidationResult validationResult, ValidationNotificationTemplate template )
    {
        ValidationNotificationRecipient recipient = template.getNotificationRecipient();

        if ( recipient.isExternalRecipient() )
        {
            final boolean isLimitToHierarchy = template.getNotifyUsersInHierarchyOnly();

            Set<User> users = template.getRecipientUserGroups().stream()
                .flatMap( ug -> ug.getMembers().stream() )
                .distinct()
                .filter( user -> isLimitToHierarchy ? orgUnitService.isInUserHierarchy(  ) )

            return new Recipients( Maps.newHashMap() ); // TODO
        }
        else
        {

            return new Recipients( Sets.newHashSet() ); // TODO
        }
    }

    /**
     * Retains all ValidationResults which are non-null, have a non-null ValidationRule and
     * have at least one ValidationNotificationTemplate.
     */
    private static Set<ValidationResult> retainApplicableValidationResults( Set<ValidationResult> results )
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

        Recipients( Set<User> userRecipients )
        {
            this.userRecipients = userRecipients;
            this.externalRecipients = null;
        }

        Recipients( Map<DeliveryChannel, String> externalRecipients )
        {
            this.userRecipients = null;
            this.externalRecipients = externalRecipients;
        }
    }

    private static class Message
    {
        final NotificationMessage message;
        final Recipients recipients;

        public Message( NotificationMessage message, Recipients recipients )
        {
            this.message = message;
            this.recipients = recipients;
        }
    }
}
