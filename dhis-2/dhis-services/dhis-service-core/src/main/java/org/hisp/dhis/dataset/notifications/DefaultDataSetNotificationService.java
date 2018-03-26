package org.hisp.dhis.dataset.notifications;

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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageConversationParams;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.message.MessageType;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.notification.SendStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
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
import java.util.stream.Stream;

/**
 * Created by zubair on 04.07.17.
 */

@Transactional
public class DefaultDataSetNotificationService
    implements DataSetNotificationService
{
    private static final Log log = LogFactory.getLog( DefaultDataSetNotificationService.class );

    private static final String SUMMARY_TEXT = "Organisation units : %d" + TextUtils.LN + "Period : %s" + TextUtils.LN + "DataSet : %s";
    private static final String SUMMARY_SUBJECT = " DataSet Summary";
    private static final String PENDING = "Pending";
    private static final String OVERDUE = "Overdue";
    private static final String TEXT_SEPARATOR =  TextUtils.LN + TextUtils.LN;

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
            .put( DeliveryChannel.SMS, ou ->  ou.getPhoneNumber() != null && !ou.getPhoneNumber().isEmpty() ) // Valid Ou phoneNumber
            .put( DeliveryChannel.EMAIL, ou ->  ou.getEmail() != null && !ou.getEmail().isEmpty() ) // Valid Ou Email
            .build();

    private final BiFunction<SendStrategy, Set<DataSetNotificationTemplate>, Set<DataSetNotificationTemplate>> SEGREGATOR = ( s, t )  -> t.stream()
        .filter( f -> s.equals( f.getSendStrategy() ) )
        .collect( Collectors.toSet() );

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
    private PeriodService periodService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public void sendScheduledDataSetNotificationsForDay( Date day )
    {
        List<MessageBatch> batches = new ArrayList<>();

        List<DataSetNotificationTemplate> scheduledTemplates =
            dsntService.getScheduledNotifications( NotificationTrigger.SCHEDULED_DAYS_DUE_DATE );

        if ( scheduledTemplates == null || scheduledTemplates.isEmpty() )
        {
            return;
        }

        Map<SendStrategy, Set<DataSetNotificationTemplate>> sendStrategySetMap = createMapBasedOnStrategy( scheduledTemplates );

        batches.add( createBatchForSingleNotifications( sendStrategySetMap.getOrDefault( SendStrategy.SINGLE_NOTIFICATION, Sets.newHashSet() ) ) );
        batches.add( createBatchForSummaryNotifications( sendStrategySetMap.getOrDefault( SendStrategy.COLLECTIVE_SUMMARY, Sets.newHashSet() ) ) );

        batches.parallelStream().forEach( this::sendAll );
    }

    @Override
    public void sendCompleteDataSetNotifications( CompleteDataSetRegistration registration )
    {
        if ( registration == null )
        {
            return;
        }

        List<DataSetNotificationTemplate> templates = dsntService.getCompleteNotifications( registration.getDataSet() );

        if ( templates == null || templates.isEmpty() )
        {
            log.info( "No template found" );
            return;
        }

        MessageBatch batch = createMessageBatch( createBatchForCompletionNotifications( registration, Sets.newHashSet( templates ) ) );

        sendAll( batch );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Map<SendStrategy, Set<DataSetNotificationTemplate>> createMapBasedOnStrategy( List<DataSetNotificationTemplate> templates )
    {
        Map<SendStrategy, Set<DataSetNotificationTemplate>> sendStrategySetMap = new HashMap<>();

        Stream.of( SendStrategy.values() ).forEach( ss -> sendStrategySetMap.put( ss, SEGREGATOR.apply( ss, Sets.newHashSet( templates ) ) ) );

        return sendStrategySetMap;
    }

    private MessageBatch createBatchForSummaryNotifications( Set<DataSetNotificationTemplate> templates )
    {
        MessageBatch batch = new MessageBatch();

        String messageText = "";

        boolean summaryCreated = false;

        Long pendingOus;

        for ( DataSetNotificationTemplate template : templates )
        {
            DhisMessage dhisMessage = new DhisMessage();

            for ( DataSet dataSet : template.getDataSets() )
            {
                if ( isValidForSending( createRespectiveRegistrationObject( dataSet, new OrganisationUnit() ), template ) )
                {
                    summaryCreated = true;

                    pendingOus = dataSet.getSources().stream().filter( ou -> !isCompleted( createRespectiveRegistrationObject( dataSet, ou ) ) ).count();

                    messageText += String.format( SUMMARY_TEXT, pendingOus, getPeriodString( dataSet.getPeriodType().createPeriod() ), dataSet.getName() ) + TEXT_SEPARATOR;
                }
            }

            if ( summaryCreated )
            {
                dhisMessage.message = new NotificationMessage( createSubjectString( template ), messageText );

                dhisMessage.recipients = resolveInternalRecipients( template );

                batch.dhisMessages.add( dhisMessage );

                messageText = "";
            }
        }

        log.info( String.format( "%d summary dataset notifications created.", batch.dhisMessages.size() ) );

        return batch;
    }

    private List<Map<CompleteDataSetRegistration, DataSetNotificationTemplate>> createBatchForCompletionNotifications( CompleteDataSetRegistration registration,
       Set<DataSetNotificationTemplate> templates )
    {
        List<Map<CompleteDataSetRegistration, DataSetNotificationTemplate>> dataSetMapList = new ArrayList<>();

        for ( DataSetNotificationTemplate template : templates )
        {
            Map<CompleteDataSetRegistration, DataSetNotificationTemplate> mapper = new HashMap<>();

            mapper.put( registration, template );

            dataSetMapList.add( mapper );
        }

        return dataSetMapList;
    }

    private String createSubjectString( DataSetNotificationTemplate template )
    {
        return template.getRelativeScheduledDays() < 0 ? PENDING + SUMMARY_SUBJECT : OVERDUE + SUMMARY_SUBJECT;
    }

    private CompleteDataSetRegistration createRespectiveRegistrationObject( DataSet dataSet, OrganisationUnit ou )
    {
        Period period = dataSet.getPeriodType().createPeriod();

        CompleteDataSetRegistration registration = new CompleteDataSetRegistration();
        registration.setDataSet( dataSet );
        registration.setPeriod( periodService.getPeriod( period.getStartDate(), period.getEndDate(), period.getPeriodType() ) );
        registration.setPeriodName( getPeriodString( registration.getPeriod() ) );
        registration.setAttributeOptionCombo( categoryService.getDefaultDataElementCategoryOptionCombo() );
        registration.setSource( ou );

        return registration;
    }

    private String getPeriodString( Period period )
    {
        I18nFormat format = i18nManager.getI18nFormat();

        return format.formatPeriod( period );
    }

    private List<Map<CompleteDataSetRegistration, DataSetNotificationTemplate>> createGroupedByMapper( Set<DataSetNotificationTemplate> templates )
    {
        List<Map<CompleteDataSetRegistration, DataSetNotificationTemplate>> dataSetMapList = new ArrayList<>();

        for ( DataSetNotificationTemplate template : templates )
        {
            Map<CompleteDataSetRegistration, DataSetNotificationTemplate> mapper = new HashMap<>();

            Set<DataSet> dataSets = template.getDataSets();

            for ( DataSet dataSet : dataSets )
            {
                mapper.putAll( dataSet.getSources().stream()
                    .map( ou -> createRespectiveRegistrationObject( dataSet, ou ) )
                    .filter( r -> isScheduledNow( r, template ) )
                    .collect( Collectors.toMap( r -> r, t -> template ) ) );
            }

            dataSetMapList.add( mapper );
        }

        return dataSetMapList;
    }

    private boolean isScheduledNow( CompleteDataSetRegistration registration, DataSetNotificationTemplate template )
    {
        return !isCompleted( registration ) && isValidForSending( registration, template );
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

        Date dueDate = registration.getPeriod().getEndDate();

        daysToCompare = DAYS_RESOLVER.get( template.getRelativeScheduledDays() < 0 ).apply( template );

        return DateUtils.daysBetween( new Date(), dueDate ) == daysToCompare;
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

    private MessageBatch createBatchForSingleNotifications( Set<DataSetNotificationTemplate> templates )
    {
        List<Map<CompleteDataSetRegistration, DataSetNotificationTemplate>> dataSetMapList = createGroupedByMapper( templates );

        return createMessageBatch( dataSetMapList );
    }

    private MessageBatch createMessageBatch( List<Map<CompleteDataSetRegistration, DataSetNotificationTemplate>> pairs )
    {
        MessageBatch batch = new MessageBatch();

        for( Map<CompleteDataSetRegistration, DataSetNotificationTemplate> pair : pairs )
        {
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
        }

        log.info( String.format( "Number of SINGLE notifications created: %d", batch.totalMessageCount() ) );

        return batch;
    }

    private ProgramMessage createProgramMessage( DataSetNotificationTemplate template, CompleteDataSetRegistration registration )
    {
        NotificationMessage message = renderer.render( registration, template );

        ProgramMessageRecipients recipients;

        if ( template.getDataSetNotificationTrigger().isScheduled() )
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
                log.error( String.format( "DataSet notification not sent due to invalid %s recipient", channel ) );

                throw new IllegalArgumentException( String.format( "Invalid %s recipient", channel ) );
            }
        }

        return recipients;
    }

    private Set<User> resolveInternalRecipients( DataSetNotificationTemplate template  )
    {
        UserGroup userGroup = template.getRecipientUserGroup();

        if ( userGroup == null )
        {
            return Sets.newHashSet();
        }

        return userGroup.getMembers();
    }

    private Set<User> resolveInternalRecipients( DataSetNotificationTemplate template, CompleteDataSetRegistration registration )
    {
        UserGroup userGroup = template.getRecipientUserGroup();

        Set<User> users = Sets.newHashSet();

        if ( userGroup == null || registration == null )
        {
            return users;
        }

        users = userGroup.getMembers().stream()
            .filter( user -> organisationUnitService.isInUserHierarchy( registration.getSource().getUid(), user.getOrganisationUnits() ) )
            .collect( Collectors.toSet() );

        return users;
    }

    private void sendInternalDhisMessages( List<DhisMessage> messages )
    {
        messages.forEach( m ->
            internalMessageService.sendMessage(
                new MessageConversationParams.Builder( m.recipients, null, m.message.getSubject(), m.message.getMessage(), MessageType.SYSTEM )
                .build()
            )
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

        int totalMessageCount()
        {
            return dhisMessages.size() + programMessages.size();
        }
    }
}
