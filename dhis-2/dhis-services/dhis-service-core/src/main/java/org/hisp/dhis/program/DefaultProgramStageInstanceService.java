package org.hisp.dhis.program;

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

import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.sms.SmsServiceException;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceReminder;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceReminderService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Abyot Asalefew
 */
@Transactional
public class DefaultProgramStageInstanceService
    implements ProgramStageInstanceService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramStageInstanceStore programStageInstanceStore;

    public void setProgramStageInstanceStore( ProgramStageInstanceStore programStageInstanceStore )
    {
        this.programStageInstanceStore = programStageInstanceStore;
    }

    private ProgramInstanceService programInstanceService;

    public void setProgramInstanceService( ProgramInstanceService programInstanceService )
    {
        this.programInstanceService = programInstanceService;
    }

    private MessageSender smsSender;

    public void setSmsSender( MessageSender smsSender )
    {
        this.smsSender = smsSender;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private TrackedEntityInstanceReminderService reminderService;

    public void setReminderService( TrackedEntityInstanceReminderService reminderService )
    {
        this.reminderService = reminderService;
    }

    private MessageService messageService;

    public void setMessageService( MessageService messageService )
    {
        this.messageService = messageService;
    }

    @Autowired
    private TrackedEntityDataValueAuditService dataValueAuditService;

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public int addProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        programStageInstance.setAutoFields();
        return programStageInstanceStore.save( programStageInstance );
    }

    @Override
    public void deleteProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        dataValueAuditService.deleteTrackedEntityDataValueAudits( programStageInstance );
        programStageInstanceStore.delete( programStageInstance );
    }

    @Override
    public ProgramStageInstance getProgramStageInstance( int id )
    {
        return programStageInstanceStore.get( id );
    }

    @Override
    public ProgramStageInstance getProgramStageInstance( String uid )
    {
        return programStageInstanceStore.getByUid( uid );
    }

    @Override
    public ProgramStageInstance getProgramStageInstance( ProgramInstance programInstance, ProgramStage programStage )
    {
        return programStageInstanceStore.get( programInstance, programStage );
    }

    @Override
    public void updateProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        programStageInstance.setAutoFields();
        programStageInstanceStore.update( programStageInstance );
    }

    @Override
    public boolean programStageInstanceExists( String uid )
    {
        return programStageInstanceStore.exists( uid );
    }

    @Override
    public List<ProgramStageInstance> getProgramStageInstances( Collection<ProgramInstance> programInstances,
        EventStatus status )
    {
        return programStageInstanceStore.get( programInstances, status );
    }

    @Override
    public List<ProgramStageInstance> getProgramStageInstances( TrackedEntityInstance entityInstance,
        EventStatus status )
    {
        return programStageInstanceStore.get( entityInstance, status );
    }

    @Override
    public long getProgramStageInstanceCount( int days )
    {
        Calendar cal = PeriodType.createCalendarInstance();
        cal.add( Calendar.DAY_OF_YEAR, (days * -1) );

        return programStageInstanceStore.getProgramStageInstanceCountLastUpdatedAfter( cal.getTime() );
    }

    @Override
    public Collection<SchedulingProgramObject> getSendMessageEvents()
    {
        return programStageInstanceStore.getSendMessageEvents();
    }

    @Override
    public void completeProgramStageInstance( ProgramStageInstance programStageInstance, boolean sendNotifications,
        I18nFormat format )
    {
        Calendar today = Calendar.getInstance();
        PeriodType.clearTimeOfDay( today );
        Date date = today.getTime();

        programStageInstance.setStatus( EventStatus.COMPLETED );
        programStageInstance.setCompletedDate( date );
        programStageInstance.setCompletedBy( currentUserService.getCurrentUsername() );

        if ( sendNotifications )
        {
            // ---------------------------------------------------------------------
            // Send SMS message when to completed the event
            // ---------------------------------------------------------------------

            List<OutboundSms> outboundSms = programStageInstance.getOutboundSms();

            if ( outboundSms == null )
            {
                outboundSms = new ArrayList<>();
            }

            outboundSms.addAll( sendMessages( programStageInstance,
                TrackedEntityInstanceReminder.SEND_WHEN_TO_C0MPLETED_EVENT, format ) );

            // ---------------------------------------------------------------------
            // Send DHIS message when to completed the event
            // ---------------------------------------------------------------------

            List<MessageConversation> messageConversations = programStageInstance.getMessageConversations();

            if ( messageConversations == null )
            {
                messageConversations = new ArrayList<>();
            }

            messageConversations.addAll( sendMessageConversations( programStageInstance,
                TrackedEntityInstanceReminder.SEND_WHEN_TO_C0MPLETED_EVENT, format ) );
        }

        // ---------------------------------------------------------------------
        // Update the event
        // ---------------------------------------------------------------------

        updateProgramStageInstance( programStageInstance );

        // ---------------------------------------------------------------------
        // Check Completed status for all of ProgramStageInstance of
        // ProgramInstance
        // ---------------------------------------------------------------------

        if ( programStageInstance.getProgramInstance().getProgram().isRegistration() )
        {
            boolean canComplete = programInstanceService
                .canAutoCompleteProgramInstanceStatus( programStageInstance.getProgramInstance() );

            if ( canComplete )
            {
                programInstanceService.completeProgramInstanceStatus( programStageInstance.getProgramInstance() );
            }
        }
    }

    @Override
    public ProgramStageInstance createProgramStageInstance( TrackedEntityInstance instance, Program program,
        Date executionDate, OrganisationUnit organisationUnit )
    {
        ProgramStage programStage = null;

        if ( program.getProgramStages() != null )
        {
            programStage = program.getProgramStages().iterator().next();
        }

        ProgramInstance programInstance = null;

        if ( program.isWithoutRegistration() )
        {
            Collection<ProgramInstance> programInstances = programInstanceService.getProgramInstances( program );

            if ( programInstances == null || programInstances.size() == 0 )
            {
                // Add a new program instance if it doesn't exist
                programInstance = new ProgramInstance();
                programInstance.setEnrollmentDate( executionDate );
                programInstance.setIncidentDate( executionDate );
                programInstance.setProgram( program );
                programInstance.setStatus( ProgramStatus.ACTIVE );
                programInstanceService.addProgramInstance( programInstance );
            }
            else
            {
                programInstance = programInstanceService.getProgramInstances( program ).iterator().next();
            }
        }

        // Add a new program stage instance
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setProgramStage( programStage );
        programStageInstance.setDueDate( executionDate );
        programStageInstance.setExecutionDate( executionDate );
        programStageInstance.setOrganisationUnit( organisationUnit );

        addProgramStageInstance( programStageInstance );

        return programStageInstance;
    }

    @Override
    public ProgramStageInstance createProgramStageInstance( ProgramInstance programInstance, ProgramStage programStage,
        Date enrollmentDate, Date incidentDate, OrganisationUnit organisationUnit )
    {
        ProgramStageInstance programStageInstance = null;
        Date currentDate = new Date();
        Date dateCreatedEvent = null;

        if ( programStage.getGeneratedByEnrollmentDate() )
        {
            dateCreatedEvent = enrollmentDate;
        }
        else
        {
            dateCreatedEvent = incidentDate;
        }

        Date dueDate = DateUtils.getDateAfterAddition( dateCreatedEvent, programStage.getMinDaysFromStart() );

        if ( !programInstance.getProgram().getIgnoreOverdueEvents() || dueDate.before( currentDate ) )
        {
            programStageInstance = new ProgramStageInstance();
            programStageInstance.setProgramInstance( programInstance );
            programStageInstance.setProgramStage( programStage );
            programStageInstance.setOrganisationUnit( organisationUnit );
            programStageInstance.setDueDate( dueDate );
            programStageInstance.setStatus( EventStatus.SCHEDULE );

            if ( programStage.getOpenAfterEnrollment() || programInstance.getProgram().isWithoutRegistration()
                || programStage.getPeriodType() != null )
            {
                programStageInstance.setExecutionDate( dueDate );
                programStageInstance.setStatus( EventStatus.ACTIVE );
            }

            addProgramStageInstance( programStageInstance );
        }

        return programStageInstance;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private OutboundSms sendEventMessage( TrackedEntityInstanceReminder reminder,
        ProgramStageInstance programStageInstance, TrackedEntityInstance entityInstance, I18nFormat format )
    {
        Set<String> phoneNumbers = reminderService.getPhoneNumbers( reminder, entityInstance );
        OutboundSms outboundSms = null;

        if ( phoneNumbers.size() > 0 )
        {
            String msg = reminderService.getMessageFromTemplate( reminder, programStageInstance, format );

            try
            {
                outboundSms = new OutboundSms();
                outboundSms.setMessage( msg );
                outboundSms.setRecipients( phoneNumbers );
                outboundSms.setSender( currentUserService.getCurrentUsername() );
                smsSender.sendMessage( null, outboundSms.getMessage(), outboundSms.getRecipients() );
            }
            catch ( SmsServiceException e )
            {
                e.printStackTrace();
            }
        }

        return outboundSms;
    }

    private Collection<OutboundSms> sendMessages( ProgramStageInstance programStageInstance, int status,
        I18nFormat format )
    {
        TrackedEntityInstance entityInstance = programStageInstance.getProgramInstance().getEntityInstance();
        Collection<OutboundSms> outboundSmsList = new HashSet<>();

        Collection<TrackedEntityInstanceReminder> reminders = programStageInstance.getProgramStage().getReminders();
        for ( TrackedEntityInstanceReminder rm : reminders )
        {
            if ( rm != null && rm.getWhenToSend() != null && rm.getWhenToSend() == status
                && (rm.getMessageType() == TrackedEntityInstanceReminder.MESSAGE_TYPE_DIRECT_SMS
                    || rm.getMessageType() == TrackedEntityInstanceReminder.MESSAGE_TYPE_BOTH) )
            {
                OutboundSms outboundSms = sendEventMessage( rm, programStageInstance, entityInstance, format );
                if ( outboundSms != null )
                {
                    outboundSmsList.add( outboundSms );
                }
            }
        }

        return outboundSmsList;
    }

    private Collection<MessageConversation> sendMessageConversations( ProgramStageInstance programStageInstance,
        int status, I18nFormat format )
    {
        Collection<MessageConversation> messageConversations = new HashSet<>();

        Collection<TrackedEntityInstanceReminder> reminders = programStageInstance.getProgramStage().getReminders();
        for ( TrackedEntityInstanceReminder rm : reminders )
        {
            if ( rm != null && rm.getWhenToSend() != null && rm.getWhenToSend() == status
                && (rm.getMessageType() == TrackedEntityInstanceReminder.MESSAGE_TYPE_DHIS_MESSAGE
                    || rm.getMessageType() == TrackedEntityInstanceReminder.MESSAGE_TYPE_BOTH) )
            {
                int id = messageService.sendMessage( programStageInstance.getProgramStage().getDisplayName(),
                    reminderService.getMessageFromTemplate( rm, programStageInstance, format ), null,
                    reminderService.getUsers( rm, programStageInstance.getProgramInstance().getEntityInstance() ), null,
                    false, true );
                messageConversations.add( messageService.getMessageConversation( id ) );
            }
        }

        return messageConversations;
    }
}
