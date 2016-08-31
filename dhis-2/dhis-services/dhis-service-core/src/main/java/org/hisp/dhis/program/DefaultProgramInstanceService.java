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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.sms.SmsServiceException;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceReminder;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceReminderService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.*;

/**
 * @author Abyot Asalefew
 */
@Transactional
public class DefaultProgramInstanceService
    implements ProgramInstanceService
{
    private static final Log log = LogFactory.getLog( DefaultProgramInstanceService.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramInstanceStore programInstanceStore;
    
    @Autowired
    private ProgramStageInstanceStore programStageInstanceStore;

    @Autowired
    private ProgramService programService;

    @Autowired
    @Resource( name = "smsMessageSender" )
    private MessageSender smsSender;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private TrackedEntityInstanceReminderService reminderService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private TrackedEntityService trackedEntityService;

    @Autowired
    private I18nManager i18nManager;

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public int addProgramInstance( ProgramInstance programInstance )
    {
        return programInstanceStore.save( programInstance );
    }

    @Override
    public void deleteProgramInstance( ProgramInstance programInstance )
    {
        programInstanceStore.delete( programInstance );
    }

    @Override
    public ProgramInstance getProgramInstance( int id )
    {
        return programInstanceStore.get( id );
    }

    @Override
    public ProgramInstance getProgramInstance( String id )
    {
        return programInstanceStore.getByUid( id );
    }

    @Override
    public boolean programInstanceExists( String uid )
    {
        return programInstanceStore.exists( uid );
    }

    @Override
    public void updateProgramInstance( ProgramInstance programInstance )
    {
        programInstanceStore.update( programInstance );
    }

    @Override
    public ProgramInstanceQueryParams getFromUrl( Set<String> ou, OrganisationUnitSelectionMode ouMode, Date lastUpdated, String program, ProgramStatus programStatus,
        Date programStartDate, Date programEndDate, String trackedEntity, String trackedEntityInstance, Boolean followUp, Integer page, Integer pageSize, boolean totalPages, boolean skipPaging )
    {
        ProgramInstanceQueryParams params = new ProgramInstanceQueryParams();

        if ( ou != null )
        {
            for ( String orgUnit : ou )
            {
                OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( orgUnit );

                if ( organisationUnit == null )
                {
                    throw new IllegalQueryException( "Organisation unit does not exist: " + orgUnit );
                }

                params.getOrganisationUnits().add( organisationUnit );
            }
        }

        Program pr = program != null ? programService.getProgram( program ) : null;

        if ( program != null && pr == null )
        {
            throw new IllegalQueryException( "Program does not exist: " + program );
        }

        TrackedEntity te = trackedEntity != null ? trackedEntityService.getTrackedEntity( trackedEntity ) : null;

        if ( trackedEntity != null && te == null )
        {
            throw new IllegalQueryException( "Tracked entity does not exist: " + program );
        }

        TrackedEntityInstance tei = trackedEntityInstance != null ? trackedEntityInstanceService.getTrackedEntityInstance( trackedEntityInstance ) : null;

        if ( trackedEntityInstance != null && tei == null )
        {
            throw new IllegalQueryException( "Tracked entity instance does not exist: " + program );
        }

        params.setProgram( pr );
        params.setProgramStatus( programStatus );
        params.setFollowUp( followUp );
        params.setLastUpdated( lastUpdated );
        params.setProgramStartDate( programStartDate );
        params.setProgramEndDate( programEndDate );
        params.setTrackedEntity( te );
        params.setTrackedEntityInstance( tei );
        params.setOrganisationUnitMode( ouMode );
        params.setPage( page );
        params.setPageSize( pageSize );
        params.setTotalPages( totalPages );
        params.setSkipPaging( skipPaging );

        return params;
    }

    // TODO consider security
    @Override
    public List<ProgramInstance> getProgramInstances( ProgramInstanceQueryParams params )
    {
        decideAccess( params );
        validate( params );

        User user = currentUserService.getCurrentUser();

        if ( user != null && params.isOrganisationUnitMode( OrganisationUnitSelectionMode.ACCESSIBLE ) )
        {
            params.setOrganisationUnits( user.getDataViewOrganisationUnitsWithFallback() );
            params.setOrganisationUnitMode( OrganisationUnitSelectionMode.DESCENDANTS );
        }
        else if ( params.isOrganisationUnitMode( CHILDREN ) )
        {
            Set<OrganisationUnit> organisationUnits = new HashSet<>();
            organisationUnits.addAll( params.getOrganisationUnits() );

            for ( OrganisationUnit organisationUnit : params.getOrganisationUnits() )
            {
                organisationUnits.addAll( organisationUnit.getChildren() );
            }

            params.setOrganisationUnits( organisationUnits );
        }

        if ( !params.isPaging() && !params.isSkipPaging() )
        {
            params.setDefaultPaging();
        }

        return programInstanceStore.getProgramInstances( params );
    }

    @Override
    public int countProgramInstances( ProgramInstanceQueryParams params )
    {
        decideAccess( params );
        validate( params );

        User user = currentUserService.getCurrentUser();

        if ( user != null && params.isOrganisationUnitMode( OrganisationUnitSelectionMode.ACCESSIBLE ) )
        {
            params.setOrganisationUnits( user.getDataViewOrganisationUnitsWithFallback() );
            params.setOrganisationUnitMode( OrganisationUnitSelectionMode.DESCENDANTS );
        }
        else if ( params.isOrganisationUnitMode( CHILDREN ) )
        {
            Set<OrganisationUnit> organisationUnits = new HashSet<>();
            organisationUnits.addAll( params.getOrganisationUnits() );

            for ( OrganisationUnit organisationUnit : params.getOrganisationUnits() )
            {
                organisationUnits.addAll( organisationUnit.getChildren() );
            }

            params.setOrganisationUnits( organisationUnits );
        }

        params.setSkipPaging( true );

        return programInstanceStore.countProgramInstances( params );
    }

    @Override
    public void decideAccess( ProgramInstanceQueryParams params )
    {

    }

    @Override
    public void validate( ProgramInstanceQueryParams params ) throws IllegalQueryException
    {
        String violation = null;

        if ( params == null )
        {
            throw new IllegalQueryException( "Params cannot be null" );
        }

        User user = currentUserService.getCurrentUser();

        if ( !params.hasOrganisationUnits() && !( params.isOrganisationUnitMode( ALL ) || params.isOrganisationUnitMode( ACCESSIBLE ) ) )
        {
            violation = "At least one organisation unit must be specified";
        }

        if ( params.isOrganisationUnitMode( ACCESSIBLE ) && (user == null || !user.hasDataViewOrganisationUnitWithFallback()) )
        {
            violation = "Current user must be associated with at least one organisation unit when selection mode is ACCESSIBLE";
        }

        if ( params.hasProgram() && params.hasTrackedEntity() )
        {
            violation = "Program and tracked entity cannot be specified simultaneously";
        }

        if ( params.hasProgramStatus() && !params.hasProgram() )
        {
            violation = "Program must be defined when program status is defined";
        }

        if ( params.hasFollowUp() && !params.hasProgram() )
        {
            violation = "Program must be defined when follow up status is defined";
        }

        if ( params.hasProgramStartDate() && !params.hasProgram() )
        {
            violation = "Program must be defined when program start date is specified";
        }

        if ( params.hasProgramEndDate() && !params.hasProgram() )
        {
            violation = "Program must be defined when program end date is specified";
        }

        if ( violation != null )
        {
            log.warn( "Validation failed: " + violation );

            throw new IllegalQueryException( violation );
        }
    }

    @Override
    public List<ProgramInstance> getProgramInstances( Program program )
    {
        return programInstanceStore.get( program );
    }

    @Override
    public List<ProgramInstance> getProgramInstances( Collection<Program> programs )
    {
        return programInstanceStore.get( programs );
    }

    @Override
    public List<ProgramInstance> getProgramInstances( Collection<Program> programs,
        OrganisationUnit organisationUnit )
    {
        return programInstanceStore.get( programs, organisationUnit );
    }

    @Override
    public List<ProgramInstance> getProgramInstances( Collection<Program> programs,
        OrganisationUnit organisationUnit, ProgramStatus status )
    {
        return programInstanceStore.get( programs, organisationUnit, status );
    }

    @Override
    public List<ProgramInstance> getProgramInstances( Collection<Program> programs, ProgramStatus status )
    {
        return programInstanceStore.get( programs, status );
    }

    @Override
    public List<ProgramInstance> getProgramInstances( Program program, ProgramStatus status )
    {
        return programInstanceStore.get( program, status );
    }

    @Override
    public List<ProgramInstance> getProgramInstances( TrackedEntityInstance entityInstance, ProgramStatus status )
    {
        return programInstanceStore.get( entityInstance, status );
    }

    @Override
    public List<ProgramInstance> getProgramInstances( TrackedEntityInstance entityInstance, Program program )
    {
        return programInstanceStore.get( entityInstance, program );
    }

    @Override
    public List<ProgramInstance> getProgramInstances( TrackedEntityInstance entityInstance, Program program, ProgramStatus status )
    {
        return programInstanceStore.get( entityInstance, program, status );
    }

    @Override
    public List<ProgramInstance> getProgramInstances( Program program, OrganisationUnit organisationUnit, Integer min, Integer max )
    {
        return programInstanceStore.get( program, organisationUnit, min, max );
    }

    @Override
    public List<ProgramInstance> getProgramInstances( Program program, Collection<Integer> orgunitIds,
        Date startDate, Date endDate, Integer min, Integer max )
    {
        return programInstanceStore.get( program, orgunitIds, startDate, endDate, min, max );
    }

    @Override
    public int countProgramInstances( Program program, Collection<Integer> orgunitIds, Date startDate, Date endDate )
    {
        return programInstanceStore.count( program, orgunitIds, startDate, endDate );
    }

    @Override
    public int countProgramInstancesByStatus( ProgramStatus status, Program program, Collection<Integer> orgunitIds,
        Date startDate, Date endDate )
    {
        return programInstanceStore.countByStatus( status, program, orgunitIds, startDate, endDate );
    }

    @Override
    public List<ProgramInstance> getProgramInstancesByStatus( ProgramStatus status, Program program,
        Collection<Integer> orgunitIds, Date startDate, Date endDate )
    {
        return programInstanceStore.getByStatus( status, program, orgunitIds, startDate, endDate );
    }

    @Override
    public Collection<SchedulingProgramObject> getScheduledMessages()
    {
        Collection<SchedulingProgramObject> result = programInstanceStore
            .getSendMesssageEvents( TrackedEntityInstanceReminder.ENROLLEMENT_DATE_TO_COMPARE );

        result.addAll( programInstanceStore
            .getSendMesssageEvents( TrackedEntityInstanceReminder.INCIDENT_DATE_TO_COMPARE ) );

        return result;
    }

    @Override
    public ProgramInstance enrollTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance, Program program,
        Date enrollmentDate, Date incidentDate, OrganisationUnit organisationUnit )
    {
        return enrollTrackedEntityInstance( trackedEntityInstance, program, enrollmentDate,
            incidentDate, organisationUnit, CodeGenerator.generateCode() );
    }

    @Override
    public ProgramInstance enrollTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance,
        Program program, Date enrollmentDate, Date incidentDate, OrganisationUnit organisationUnit, String uid )
    {
        // ---------------------------------------------------------------------
        // Add program instance
        // ---------------------------------------------------------------------
        
        if ( program.getTrackedEntity() != null && !program.getTrackedEntity().equals( trackedEntityInstance.getTrackedEntity() ) )
        {
            throw new IllegalQueryException( "Tracked entitiy instance must have same tracked entity as program: " + program.getUid() );
        }

        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( CodeGenerator.isValidCode( uid ) ? uid : CodeGenerator.generateCode() );
        programInstance.setOrganisationUnit( organisationUnit );
        programInstance.enrollTrackedEntityInstance( trackedEntityInstance, program );

        if ( enrollmentDate != null )
        {
            programInstance.setEnrollmentDate( enrollmentDate );
        }
        else
        {
            programInstance.setEnrollmentDate( new Date() );
        }

        if ( incidentDate != null )
        {
            programInstance.setIncidentDate( incidentDate );
        }
        else
        {
            programInstance.setIncidentDate( new Date() );
        }

        programInstance.setStatus( ProgramStatus.ACTIVE );
        addProgramInstance( programInstance );

        // -----------------------------------------------------------------
        // Send messages after enrolling in program
        // -----------------------------------------------------------------

        List<OutboundSms> outboundSms = programInstance.getOutboundSms();

        if ( outboundSms == null ) // TODO remove
        {
            outboundSms = new ArrayList<>();
        }

        outboundSms.addAll( sendMessages( programInstance, TrackedEntityInstanceReminder.SEND_WHEN_TO_ENROLLMENT ) );

        // -----------------------------------------------------------------
        // Send message when to completed the program
        // -----------------------------------------------------------------

        List<MessageConversation> messages = programInstance.getMessageConversations();

        if ( messages == null ) // TODO remove
        {
            messages = new ArrayList<>();
        }

        messages.addAll( sendMessageConversations( programInstance,
            TrackedEntityInstanceReminder.SEND_WHEN_TO_ENROLLMENT ) );

        updateProgramInstance( programInstance );
        trackedEntityInstanceService.updateTrackedEntityInstance( trackedEntityInstance );

        return programInstance;
    }

    @Override
    public boolean canAutoCompleteProgramInstanceStatus( ProgramInstance programInstance )
    {
        Set<ProgramStageInstance> programStageInstances = new HashSet<>( programInstance.getProgramStageInstances() );
        Set<ProgramStage> programStages = new HashSet<>();

        for ( ProgramStageInstance programStageInstance : programStageInstances )
        {
            if ( ( !programStageInstance.isCompleted() && programStageInstance.getStatus() != EventStatus.SKIPPED )
                || programStageInstance.getProgramStage().getRepeatable() )
            {
                return false;
            }

            programStages.add( programStageInstance.getProgramStage() );
        }

        return programStages.size() == programInstance.getProgram().getProgramStages().size();
    }

    @Override
    public void completeProgramInstanceStatus( ProgramInstance programInstance )
    {
        // ---------------------------------------------------------------------
        // Send sms-message when to completed the program
        // ---------------------------------------------------------------------

        List<OutboundSms> outboundSms = programInstance.getOutboundSms();

        if ( outboundSms == null )
        {
            outboundSms = new ArrayList<>();
        }

        outboundSms
            .addAll( sendMessages( programInstance, TrackedEntityInstanceReminder.SEND_WHEN_TO_COMPLETED_PROGRAM ) );

        // -----------------------------------------------------------------
        // Send DHIS message when to completed the program
        // -----------------------------------------------------------------

        List<MessageConversation> messageConversations = programInstance.getMessageConversations();

        if ( messageConversations == null )
        {
            messageConversations = new ArrayList<>();
        }

        messageConversations.addAll( sendMessageConversations( programInstance,
            TrackedEntityInstanceReminder.SEND_WHEN_TO_COMPLETED_PROGRAM ) );

        // -----------------------------------------------------------------
        // Update program-instance
        // -----------------------------------------------------------------

        programInstance.setStatus( ProgramStatus.COMPLETED );
        programInstance.setEndDate( new Date() );
        programInstance.setCompletedBy( currentUserService.getCurrentUsername() );

        updateProgramInstance( programInstance );
    }

    @Override
    public void cancelProgramInstanceStatus( ProgramInstance programInstance )
    {
        // ---------------------------------------------------------------------
        // Set status of the program-instance
        // ---------------------------------------------------------------------

        Calendar today = Calendar.getInstance();
        PeriodType.clearTimeOfDay( today );
        Date currentDate = today.getTime();

        programInstance.setEndDate( currentDate );
        programInstance.setStatus( ProgramStatus.CANCELLED );
        updateProgramInstance( programInstance );

        // ---------------------------------------------------------------------
        // Set statuses of the program-stage-instances
        // ---------------------------------------------------------------------

        for ( ProgramStageInstance programStageInstance : programInstance.getProgramStageInstances() )
        {
            if ( programStageInstance.getExecutionDate() == null )
            {
                // -------------------------------------------------------------
                // Set status as skipped for overdue events, or delete
                // -------------------------------------------------------------
                
                if ( programStageInstance.getDueDate().before( currentDate ) )
                {
                    programStageInstance.setStatus( EventStatus.SKIPPED );
                    programStageInstanceStore.update( programStageInstance );
                }
                else
                {
                    programStageInstanceStore.delete( programStageInstance );
                }
            }
        }
    }
    
    @Override
    public void incompleteProgramInstanceStatus( ProgramInstance programInstance )
    {        
        Program program = programInstance.getProgram();
        
        TrackedEntityInstance tei = programInstance.getEntityInstance();
        
        if( getProgramInstances( tei, program, ProgramStatus.ACTIVE).size() > 0 )
        {
            log.warn( "Program has another active enrollment going on. Not possible to incomplete" );

            throw new IllegalQueryException( "Program has another active enrollment going on. Not possible to incomplete" );
        }
        
        // -----------------------------------------------------------------
        // Update program-instance
        // -----------------------------------------------------------------

        programInstance.setStatus( ProgramStatus.ACTIVE );
        
        updateProgramInstance( programInstance );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private OutboundSms sendProgramMessage( TrackedEntityInstanceReminder reminder, ProgramInstance programInstance,
        TrackedEntityInstance entityInstance )
    {
        I18nFormat format = i18nManager.getI18nFormat();

        Set<String> phoneNumbers = reminderService.getPhoneNumbers( reminder, entityInstance );
        OutboundSms outboundSms = null;

        if ( phoneNumbers.size() > 0 )
        {
            String msg = reminderService.getMessageFromTemplate( reminder, programInstance, format );

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

    private Collection<OutboundSms> sendMessages( ProgramInstance programInstance, int status )
    {
        TrackedEntityInstance entityInstance = programInstance.getEntityInstance();
        Collection<OutboundSms> outboundSmsList = new HashSet<>();

        Collection<TrackedEntityInstanceReminder> reminders = programInstance.getProgram().getInstanceReminders();

        for ( TrackedEntityInstanceReminder rm : reminders )
        {
            if ( rm != null
                && rm.getWhenToSend() != null
                && rm.getWhenToSend() == status
                && (rm.getMessageType() == TrackedEntityInstanceReminder.MESSAGE_TYPE_DIRECT_SMS || rm.getMessageType() == TrackedEntityInstanceReminder.MESSAGE_TYPE_BOTH) )
            {
                OutboundSms outboundSms = sendProgramMessage( rm, programInstance, entityInstance );

                if ( outboundSms != null )
                {
                    outboundSmsList.add( outboundSms );
                }
            }
        }

        return outboundSmsList;
    }

    private Collection<MessageConversation> sendMessageConversations( ProgramInstance programInstance, int status )
    {
        I18nFormat format = i18nManager.getI18nFormat();

        Collection<MessageConversation> messageConversations = new HashSet<>();

        Collection<TrackedEntityInstanceReminder> reminders = programInstance.getProgram().getInstanceReminders();
        for ( TrackedEntityInstanceReminder rm : reminders )
        {
            if ( rm != null
                && rm.getWhenToSend() != null
                && rm.getWhenToSend() == status
                && (rm.getMessageType() == TrackedEntityInstanceReminder.MESSAGE_TYPE_DHIS_MESSAGE || rm
                .getMessageType() == TrackedEntityInstanceReminder.MESSAGE_TYPE_BOTH) )
            {
                int id = messageService.sendMessage( programInstance.getProgram().getDisplayName(),
                    reminderService.getMessageFromTemplate( rm, programInstance, format ), null,
                    reminderService.getUsers( rm, programInstance.getEntityInstance() ), null, false, true );
                messageConversations.add( messageService.getMessageConversation( id ) );
            }
        }

        return messageConversations;
    }
}
