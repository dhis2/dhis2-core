package org.hisp.dhis.program;

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

import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.notification.ProgramNotificationEventType;
import org.hisp.dhis.program.notification.ProgramNotificationPublisher;
import org.hisp.dhis.programrule.engine.ProgramRuleEngineService;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Calendar;
import java.util.Date;

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

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    @Autowired
    private TrackedEntityDataValueAuditService dataValueAuditService;

    @Autowired
    private ProgramRuleEngineService programRuleEngineService;

    @Autowired
    private ProgramNotificationPublisher programNotificationPublisher;

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public int addProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        programStageInstance.setAutoFields();
        programStageInstanceStore.save( programStageInstance );

        return programStageInstance.getId();
    }

    @Override
    public void deleteProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        deleteProgramStageInstance( programStageInstance, false );
    }

    public void deleteProgramStageInstance( ProgramStageInstance programStageInstance, boolean forceDelete )
    {
        dataValueAuditService.deleteTrackedEntityDataValueAudits( programStageInstance );

        if ( forceDelete )
        {
            programStageInstanceStore.delete( programStageInstance );
        }
        else
        {
            // Soft delete
            programStageInstance.setDeleted( !forceDelete );
            programStageInstanceStore.save( programStageInstance );
        }

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
    public long getProgramStageInstanceCount( int days )
    {
        Calendar cal = PeriodType.createCalendarInstance();
        cal.add( Calendar.DAY_OF_YEAR, (days * -1) );

        return programStageInstanceStore.getProgramStageInstanceCountLastUpdatedAfter( cal.getTime() );
    }

    @Override
    public void completeProgramStageInstance( ProgramStageInstance programStageInstance, boolean skipNotifications,
        I18nFormat format )
    {
        Calendar today = Calendar.getInstance();
        PeriodType.clearTimeOfDay( today );
        Date date = today.getTime();

        programStageInstance.setStatus( EventStatus.COMPLETED );
        programStageInstance.setCompletedDate( date );

        if ( StringUtils.isEmpty( programStageInstance.getCompletedBy() ) )
        {
            programStageInstance.setCompletedBy( currentUserService.getCurrentUsername() );
        }

        if ( !skipNotifications )
        {
            programNotificationPublisher.publishEvent( programStageInstance, ProgramNotificationEventType.PROGRAM_STAGE_COMPLETION );

            programRuleEngineService.evaluate( programStageInstance );
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
}
