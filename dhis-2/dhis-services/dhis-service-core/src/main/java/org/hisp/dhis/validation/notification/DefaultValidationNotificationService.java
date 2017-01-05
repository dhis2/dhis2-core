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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.validation.Importance;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationRule;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.hisp.dhis.commons.util.TextUtils.LN;

/**
 * @author Halvdan Hoem Grelland
 */
public class DefaultValidationNotificationService
    implements ValidationNotificationService
{
    private static final Log log = LogFactory.getLog( DefaultValidationNotificationService.class );

    private static final Predicate<ValidationResult> IS_APPLICABLE_RESULT =
        vr ->
            Objects.nonNull( vr ) &&
                Objects.nonNull( vr.getValidationRule() ) &&
                !vr.getValidationRule().getNotificationTemplates().isEmpty();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private NotificationMessageRenderer<ValidationResult> notificationMessageRenderer;

    public void setNotificationMessageRenderer( NotificationMessageRenderer<ValidationResult> notificationMessageRenderer )
    {
        this.notificationMessageRenderer = notificationMessageRenderer;
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
        // Filter out un-applicable validation results
        Set<ValidationResult> applicableResults = validationResults.stream()
            .filter( IS_APPLICABLE_RESULT )
            .collect( Collectors.toSet() );

        Clock clock = new Clock( log ).startClock().
            logTime( String.format( "Creating notification messages for %d validation rule violations", applicableResults.size() ) );

        Set<Message> allMessages = createMessages( applicableResults );

        clock.logTime( String.format( "Rendered %d individual messages", allMessages.size() ) );

        Map<Set<User>, SortedSet<Message>> usersToMessagesMap = createUserToMessagesMap( allMessages );

        clock.logTime( String.format( "Messages going out to %d users", userToMessagesMap.keySet().size() ) );

        Map<Set<User>, NotificationMessage> summarizedMessages = summarizeMessagesPerUser( userToMessagesMap );
        summarizedMessages.forEach( this::send );

        clock.logTime( "Done sending validation notifications" );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private static Map<User, NotificationMessage> summarizeMessagesPerUser( Map<User, SortedSet<Message>> userToMessagesMap )
    {
        return userToMessagesMap.entrySet().stream()
            .collect( Collectors.toMap( Map.Entry::getKey, entry -> createSummarizedMessage( entry.getValue() ) ) );
    }

    /**
     * Creates a summarized message from the given messages.
     * The messages are concatenated in their given order.
     */
    private static NotificationMessage createSummarizedMessage( SortedSet<Message> messages )
    {
        Map<Importance, Long> counts = messages.stream()
            .map( m -> m.validationResult.getValidationRule().getImportance() )
            .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );

        String subject = String.format(
            "Validation violations as of %s: High %d, medium %d, low %d",
            DateUtils.getLongDateString( new Date() ),
            counts.getOrDefault( Importance.HIGH, 0L ),
            counts.getOrDefault( Importance.MEDIUM, 0L ),
            counts.getOrDefault( Importance.LOW, 0L )
        );

        String message = messages.stream()
            .map( m -> m.notificationMessage )
            .map( n -> String.format( "%s%s%s", n.getSubject(), LN, n.getMessage() ) )
            .reduce( "", (initStr, newStr) -> String.format( "%s%s%s", initStr, LN, newStr ) );

        return new NotificationMessage( subject, message );
    }

    private Map<SortedSet<ValidationResult>, Set<User>> splitByRecipients( SortedSet<ValidationResult> validationResults )
    {

        Map<User, SortedSet<ValidationResult>> userResults = getUserResults( validationResults );
        // TODO Perform for-loop gymnastics to transform map

        return null;
    }

    private Map<User, SortedSet<ValidationResult>> getUserResults( SortedSet<ValidationResult> results )
    {
        Map<User, SortedSet<ValidationResult>> userResults = new HashMap<>();

        for ( ValidationResult result : results )
        {
            ValidationRule rule = result.getValidationRule();

            Set<ValidationNotificationTemplate> templates = rule.getNotificationTemplates();

            for ( ValidationNotificationTemplate template : templates )
            {
                Set<User> usersForTemplate = resolveRecipients( result, template );

                for ( User user : usersForTemplate )
                {
                    userResults.computeIfAbsent( user, k -> new TreeSet<>() ).add( result );
                }
            }
        }

        return userResults;
    }

    private Set<Message> createMessages( Set<ValidationResult> validationResults )
    {
        return validationResults.stream()
            .flatMap(
                result -> result.getValidationRule().getNotificationTemplates().stream()
                    .map( template ->
                        new Message(
                            result,
                            notificationMessageRenderer.render( result, template ),
                            resolveRecipients( result, template )
                        )
                    )
            )
            .collect( Collectors.toSet() );
    }

    private Map<Set<User>, SortedSet<Message>> createUserToMessagesMap( Set<Message> messages )
    {
        Map<User, SortedSet<Message>> usersToMessages = new HashMap<>();

        for ( Message message : messages )
        {
            Set<User> users = message.recipients;

            for ( User user : users )
            {
                usersToMessages.computeIfAbsent( users, k -> new TreeSet<>() ).add( message );
            }
        }



        return usersToMessages;
    }

    private static Set<User> resolveRecipients( final ValidationResult validationResult, ValidationNotificationTemplate template )
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
            .filter( user -> !limitToHierarchy || orgUnitsToInclude.contains( user.getOrganisationUnit() ) )
            .collect( Collectors.toSet() );
    }

    private void send( User user, NotificationMessage notificationMessage )
    {
        messageService.sendMessage(
            notificationMessage.getSubject(),
            notificationMessage.getMessage(),
            null,
            Sets.newHashSet( user ), // TODO Should be multiple?
            null,
            false,
            false // TODO Consider this?
        );
    }

    // -------------------------------------------------------------------------
    // Internal classes
    // -------------------------------------------------------------------------

    private static class Message
    {
        final ValidationResult validationResult;
        final NotificationMessage notificationMessage;
        final Set<User> recipients;

        public Message( ValidationResult validationResult, NotificationMessage notificationMessage, Set<User> recipients )
        {
            this.validationResult = validationResult;
            this.notificationMessage = notificationMessage;
            this.recipients = recipients;
        }
    }
}
