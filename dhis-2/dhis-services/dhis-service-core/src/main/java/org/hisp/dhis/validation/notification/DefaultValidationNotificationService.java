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

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.sms.outbound.MessageBatch;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * @author Halvdan Hoem Grelland
 */
public class DefaultValidationNotificationService
    implements ValidationNotificationService
{

    @Autowired
    private NotificationMessageRenderer<ValidationResult> notificationRenderer;

    public DefaultValidationNotificationService()
    {
    }

    @Override
    public void sendNotifications( Set<ValidationResult> results )
    {
        Set<ValidationResult> validationResults = filterNonNotifiable( results );

        Set<NotificationMessage> messages =
            validationResults.stream()
//                .map( Pair.of( renderMessages )
                .collect( HashSet::new, Set::addAll, Set::addAll );

    }

    /**
     * Retains all ValidationResults which are non-null, have a non-null ValidationRule and
     * have at least one ValidationNotificationTemplate.
     */
    private static Set<ValidationResult> filterNonNotifiable( Set<ValidationResult> results )
    {
        return results.stream()
            .filter( Objects::nonNull )
            .filter( vr -> Objects.nonNull( vr.getValidationRule() ) )
            .filter( vr -> !vr.getValidationRule().getNotificationTemplates().isEmpty() )
            .collect( Collectors.toSet() );
    }

    private Map<ValidationNotificationTemplate, NotificationMessage> renderMessages( ValidationResult validationResult )
    {
        Set<ValidationNotificationTemplate> templates = validationResult.getValidationRule().getNotificationTemplates();

        return templates.stream()
                .collect( Collectors.toMap( identity(), t -> notificationRenderer.render( validationResult, t ) ) );
    }

    private MessageBatch createMessageBatch()
    {
        // TODO
    }

    // -------------------------------------------------------------------------
    // Internal classes
    // -------------------------------------------------------------------------

    private static class MessageBatch
    {
        Set<DhisMessage> dhisMessages;
        Set<Message> messages;
    }

    private static class DhisMessage
    {
        NotificationMessage message;
        Set<User> recipients;
    }

    private static class Message
    {
        NotificationMessage message;
        Set<String> recipients;
    }
}
