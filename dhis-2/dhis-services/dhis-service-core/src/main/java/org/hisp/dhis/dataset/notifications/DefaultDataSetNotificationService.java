package org.hisp.dhis.dataset.notifications;

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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.taskdefs.condition.Or;
import org.hibernate.cfg.DefaultNamingStrategy;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.message.Message;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.message.MessageType;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by zubair on 04.07.17.
 */
public class DefaultDataSetNotificationService
    implements DataSetNotificationService
{
    private static final Log log = LogFactory.getLog( DefaultDataSetNotificationService.class );

    private ImmutableMap<DeliveryChannel, BiFunction<OrganisationUnit,ProgramMessageRecipients, ProgramMessageRecipients>> RECIPIENT_MAPPER =
        new ImmutableMap.Builder<DeliveryChannel, BiFunction<OrganisationUnit,ProgramMessageRecipients,ProgramMessageRecipients>>()
            .put( DeliveryChannel.SMS, ( ou, pmr ) -> resolvePhoneNumber( ou, pmr ) )
            .put( DeliveryChannel.EMAIL, ( ou, pmr ) -> resolveEmail( ou, pmr ) )
            .build();

    private ImmutableMap<DeliveryChannel, Predicate<OrganisationUnit>> VALIDATOR =
        new ImmutableMap.Builder<DeliveryChannel, Predicate<OrganisationUnit>>()
            .put( DeliveryChannel.SMS, ou ->  ou.getPhoneNumber() != null && !ou.getPhoneNumber().isEmpty() )
            .put( DeliveryChannel.EMAIL, ou ->  ou.getEmail() != null && !ou.getEmail().isEmpty() )
            .build();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private DataSetNotificationTemplateService dsntService;

    @Autowired
    private MessageService internalMessageService;

    @Autowired
    private ProgramMessageService externalMessageService;

    @Autowired
    private NotificationMessageRenderer<CompleteDataSetRegistration> renderer;

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public void sendScheduledDataSetNotificationsForDay( Date day )
    {

    }

    @Override
    public void sendCompleteDataSetNotifications( CompleteDataSetRegistration registration )
    {
        List<DataSetNotificationTemplate> templates = dsntService.getCompleteNotifications( registration.getDataSet() );

        MessageBatch batch = createMessageBatch( templates, registration );

        sendAll( batch );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private ProgramMessageRecipients resolvePhoneNumber( OrganisationUnit ou, ProgramMessageRecipients pmr )
    {
        pmr.setPhoneNumbers( Sets.newHashSet( ou.getPhoneNumber() ) );

        return pmr;
    }

    private ProgramMessageRecipients resolveEmail( OrganisationUnit ou, ProgramMessageRecipients pmr )
    {
        pmr.setEmailAddresses( Sets.newHashSet( ou.getEmail() ) );

        return pmr;
    }

    private MessageBatch createMessageBatch( List<DataSetNotificationTemplate> templates, CompleteDataSetRegistration registration )
    {
        MessageBatch batch = new MessageBatch();

        for ( DataSetNotificationTemplate template : templates )
        {
            if ( template.getNotificationRecipient().isExternalRecipient() )
            {
                batch.programMessages.add( createProgramMessage( template, registration ) );
            }
            else
            {
                batch.dhisMessages.add( createDhisMessage( template, registration ) );
            }
        }

        return batch;
    }

    private ProgramMessage createProgramMessage( DataSetNotificationTemplate template, CompleteDataSetRegistration registration )
    {
        NotificationMessage message = renderer.render( registration, template );

        return new ProgramMessage( message.getSubject(), message.getMessage(), resolveExternalRecipients( template, registration ) );
    }

    private DhisMessage createDhisMessage( DataSetNotificationTemplate template, CompleteDataSetRegistration registration )
    {
        DhisMessage dhisMessage = new DhisMessage();

        dhisMessage.message = renderer.render( registration, template );
        dhisMessage.recipients = resolveInternalRecipients( template, registration );

        return dhisMessage;
    }

    private ProgramMessageRecipients resolveExternalRecipients( DataSetNotificationTemplate template, CompleteDataSetRegistration registration )
    {
        ProgramMessageRecipients recipients = new ProgramMessageRecipients();

        OrganisationUnit ou = registration.getSource();

        for ( DeliveryChannel channel: template.getDeliveryChannels() )
        {
            if ( VALIDATOR.get( channel ).test( ou ) )
            {
                recipients = RECIPIENT_MAPPER.get( channel ).apply( ou, recipients );
            }
            else
            {
                log.error( String.format( "Invalid %s recipient", channel ) );
            }
        }

        return recipients;
    }

    private Set<User> resolveInternalRecipients( DataSetNotificationTemplate template, CompleteDataSetRegistration registration )
    {
        return null;
    }

    private void sendInternalDhisMessages( Set<DhisMessage> messages )
    {
        messages.forEach( m ->
                internalMessageService.sendMessage( m.message.getSubject(), m.message.getMessage(), null, m.recipients, null,
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

        BatchResponseStatus status = externalMessageService.sendMessages( Lists.newArrayList( messages ) );

        log.debug( String.format( "Resulting status from ProgramMessageService:\n %s", status.toString() ) );
    }

    private void sendAll( MessageBatch messageBatch )
    {
        sendInternalDhisMessages( messageBatch.dhisMessages );
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
