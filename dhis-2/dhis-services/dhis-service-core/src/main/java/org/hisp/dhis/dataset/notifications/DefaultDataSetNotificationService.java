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
import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.message.MessageType;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by zubair on 04.07.17.
 */

@Transactional
public class DefaultDataSetNotificationService
    implements DataSetNotificationService
{
    private static final Log log = LogFactory.getLog( DefaultDataSetNotificationService.class );

    private final ImmutableMap<DeliveryChannel, BiFunction<Set<OrganisationUnit>,ProgramMessageRecipients, ProgramMessageRecipients>> RECIPIENT_MAPPER =
        new ImmutableMap.Builder<DeliveryChannel, BiFunction<Set<OrganisationUnit>,ProgramMessageRecipients,ProgramMessageRecipients>>()
            .put( DeliveryChannel.SMS, this::resolvePhoneNumbers )
            .put( DeliveryChannel.EMAIL, this::resolveEmails )
            .build();

    private final ImmutableMap<Boolean, Function<DataSetNotificationTemplate, Integer>> DAYS_RESOLVER =
        new ImmutableMap.Builder<Boolean, Function<DataSetNotificationTemplate, Integer>>()
            .put( false, DataSetNotificationTemplate::getRelativeScheduledDays )  // Overdue reminder
            .put( true, template -> template.getRelativeScheduledDays() * -1 )  // Future reminder
            .build();

    private final ImmutableMap<DeliveryChannel, Predicate<OrganisationUnit>> VALIDATOR =
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

    @Autowired
    private CompleteDataSetRegistrationService completeDataSetRegistrationService;

    @Autowired
    private I18nManager i18nManager;

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public void sendScheduledDataSetNotificationsForDay( Date day )
    {
        List<DataSetNotificationTemplate> scheduledTemplates =
            dsntService.getScheduledNotifications( NotificationTrigger.SCHEDULED_DAYS_DUE_DATE );

        Map<CompleteDataSetRegistration, DataSetNotificationTemplate> registrationToTemplateMapper = createGroupedByMapper( scheduledTemplates );

        sendAll( createMessageBatch( registrationToTemplateMapper ) );
    }

    @Override
    public void sendCompleteDataSetNotifications( CompleteDataSetRegistration registration )
    {
        if ( registration == null )
        {
            return;
        }

        List<DataSetNotificationTemplate> templates = dsntService.getCompleteNotifications( registration.getDataSet() );

        MessageBatch batch = createMessageBatch( templates.stream().collect( Collectors.toMap( r -> registration, t -> t ) ) );

        sendAll( batch );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private CompleteDataSetRegistration createRespectiveRegistrationObject( DataSet dataSet, OrganisationUnit ou )
    {
        I18nFormat format = i18nManager.getI18nFormat();

        CompleteDataSetRegistration registration = new CompleteDataSetRegistration();
        registration.setDataSet( dataSet );
        registration.setPeriod( dataSet.getPeriodType().createPeriod() );
        registration.setPeriodName( format.formatPeriod( registration.getPeriod() ) );
        registration.setSource( ou );

        return registration;
    }

    private Map<CompleteDataSetRegistration, DataSetNotificationTemplate> createGroupedByMapper( List<DataSetNotificationTemplate> scheduledTemplates )
    {
        Map<CompleteDataSetRegistration, DataSetNotificationTemplate> mapper = new HashMap<>();

        for ( DataSetNotificationTemplate template : scheduledTemplates )
        {
            Set<DataSet> dataSets = template.getDataSets();

            for ( DataSet dataSet : dataSets )
            {
                mapper = dataSet.getSources().stream()
                    .map( ou -> createRespectiveRegistrationObject( dataSet, ou ) )
                    .filter( r -> isScheduledNow( r, template ) )
                    .collect( Collectors.toMap( r -> r, t -> template ) );
            }
        }

        return mapper;
    }

    private boolean isScheduledNow( CompleteDataSetRegistration registration, DataSetNotificationTemplate template )
    {
        return !isCompleted(registration) && isValidForSending(registration, template);
    }

    private boolean isCompleted( CompleteDataSetRegistration registration )
    {
       CompleteDataSetRegistration completed = completeDataSetRegistrationService.getCompleteDataSetRegistration(
           registration.getDataSet(), registration.getPeriod(), registration.getSource(), registration.getAttributeOptionCombo() );

        return completed != null;
    }

    private boolean isValidForSending( CompleteDataSetRegistration registration, DataSetNotificationTemplate template )
    {
        int daysToCompare;

        Date dueDate = registration.getDataSet().getPeriodType().createPeriod().getEndDate();

        daysToCompare = DAYS_RESOLVER.get( template.getRelativeScheduledDays() < 0 ).apply( template );

        return DateUtils.daysBetween( new Date(), dueDate ) <= daysToCompare;
    }

    private ProgramMessageRecipients resolvePhoneNumbers( Set<OrganisationUnit> ous, ProgramMessageRecipients pmr )
    {
        pmr.setPhoneNumbers( ous.stream().map( OrganisationUnit::getPhoneNumber ).collect( Collectors.toSet() ) );

        return pmr;
    }

    private ProgramMessageRecipients resolveEmails( Set<OrganisationUnit> ous, ProgramMessageRecipients pmr )
    {
        pmr.setEmailAddresses( ous.stream().map( OrganisationUnit::getEmail ).collect( Collectors.toSet() ));

        return pmr;
    }

    private MessageBatch createMessageBatch( Map<CompleteDataSetRegistration,DataSetNotificationTemplate> pair )
    {
        MessageBatch batch = new MessageBatch();

        for ( Map.Entry<CompleteDataSetRegistration,DataSetNotificationTemplate> entry : pair.entrySet() )
        {
            if( entry.getValue().getNotificationRecipient().isExternalRecipient() )
            {
                batch.programMessages.add( createProgramMessage( entry.getValue(), entry.getKey() ) );
            }
            else
            {
                batch.dhisMessages.add( createDhisMessage( entry.getValue(), entry.getKey() ) );
            }
        }

        return batch;
    }

    private ProgramMessage createProgramMessage( DataSetNotificationTemplate template, CompleteDataSetRegistration registration )
    {
        NotificationMessage message = renderer.render( registration, template );

        ProgramMessageRecipients recipients;

        if ( template.getNotificationTrigger().isScheduled() )
        {
            recipients = resolveExternalRecipientsForSchedule( template, registration );
        }
        else
        {
            recipients = resolveExternalRecipients( template, registration );
        }

        ProgramMessage programMessage = new ProgramMessage( message.getSubject(), message.getMessage(), recipients );

        programMessage.setDeliveryChannels( template.getDeliveryChannels() );

        return programMessage;
    }

    private DhisMessage createDhisMessage( DataSetNotificationTemplate template, CompleteDataSetRegistration registration )
    {
        DhisMessage dhisMessage = new DhisMessage();

        dhisMessage.message = renderer.render( registration, template );
        dhisMessage.recipients = resolveInternalRecipients( template, registration );

        return dhisMessage;
    }

    private ProgramMessageRecipients resolveExternalRecipientsForSchedule( DataSetNotificationTemplate template, CompleteDataSetRegistration registration )
    {
        ProgramMessageRecipients recipients = new ProgramMessageRecipients();

        for ( DeliveryChannel channel: template.getDeliveryChannels() )
        {
            Set<OrganisationUnit> ous = registration.getDataSet().getSources().stream().filter( ou -> VALIDATOR.get( channel ).test( ou ) ).collect( Collectors.toSet() );

            recipients = RECIPIENT_MAPPER.get( channel ).apply( ous, recipients );
        }

        return recipients;
    }

    private ProgramMessageRecipients resolveExternalRecipients( DataSetNotificationTemplate template, CompleteDataSetRegistration registration )
    {
        ProgramMessageRecipients recipients = new ProgramMessageRecipients();

        OrganisationUnit ou = registration.getSource();

        for ( DeliveryChannel channel: template.getDeliveryChannels() )
        {
            if ( VALIDATOR.get( channel ).test( ou ) )
            {
                recipients = RECIPIENT_MAPPER.get( channel ).apply( Sets.newHashSet( ou ), recipients );
            }
            else
            {
                log.error( String.format( "Invalid %s recipient", channel ) );

                throw new IllegalArgumentException( String.format( "Invalid %s recipient", channel ) );
            }
        }

        return recipients;
    }

    private Set<User> resolveInternalRecipients( DataSetNotificationTemplate template, CompleteDataSetRegistration registration )
    {
        UserGroup userGroup = template.getRecipientUserGroup();

        return userGroup.getMembers();
    }

    private void sendInternalDhisMessages( List<DhisMessage> messages )
    {
        messages.forEach( m ->
            internalMessageService.sendMessage( m.message.getSubject(), m.message.getMessage(), null, m.recipients, null,
                MessageType.SYSTEM, true )
        );
    }

    private void sendProgramMessages( List<ProgramMessage> messages )
    {
        if ( messages.isEmpty() )
        {
            return;
        }

        log.info( String.format( "Dispatching %d ProgramMessages", messages.size() ) );

        BatchResponseStatus status = externalMessageService.sendMessages( messages );

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
        List<DhisMessage> dhisMessages = new ArrayList<>();
        List<ProgramMessage> programMessages = new ArrayList<>();

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
