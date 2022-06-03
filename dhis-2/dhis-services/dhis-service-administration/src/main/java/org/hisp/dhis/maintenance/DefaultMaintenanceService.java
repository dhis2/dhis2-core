/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.maintenance;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.event.ApplicationCacheClearedEvent;
import org.hisp.dhis.commons.util.PageRange;
import org.hisp.dhis.dataapproval.DataApprovalAuditService;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.datavalue.DataValueAuditService;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
import org.hisp.dhis.user.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service( "org.hisp.dhis.maintenance.MaintenanceService" )
public class DefaultMaintenanceService
    implements MaintenanceService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private MaintenanceStore maintenanceStore;

    private PeriodService periodService;

    private UserService userService;

    private CurrentUserService currentUserService;

    private DataValueService dataValueService;

    private DataValueAuditService dataValueAuditService;

    private CompleteDataSetRegistrationService completeRegistrationService;

    private DataApprovalService dataApprovalService;

    private DataApprovalAuditService dataApprovalAuditService;

    private ApplicationEventPublisher eventPublisher;

    private TrackedEntityDataValueAuditService trackedEntityDataValueAuditService;

    public DefaultMaintenanceService( MaintenanceStore maintenanceStore, PeriodService periodService,
        UserService userService, CurrentUserService currentUserService, DataValueService dataValueService,
        DataValueAuditService dataValueAuditService, CompleteDataSetRegistrationService completeRegistrationService,
        DataApprovalService dataApprovalService, DataApprovalAuditService dataApprovalAuditService,
        ApplicationEventPublisher eventPublisher,
        TrackedEntityDataValueAuditService trackedEntityDataValueAuditService )
    {

        checkNotNull( maintenanceStore );
        checkNotNull( periodService );
        checkNotNull( userService );
        checkNotNull( currentUserService );
        checkNotNull( dataValueService );
        checkNotNull( dataValueAuditService );
        checkNotNull( completeRegistrationService );
        checkNotNull( dataApprovalService );
        checkNotNull( dataApprovalAuditService );
        checkNotNull( trackedEntityDataValueAuditService );

        this.maintenanceStore = maintenanceStore;
        this.periodService = periodService;
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.dataValueService = dataValueService;
        this.dataValueAuditService = dataValueAuditService;
        this.completeRegistrationService = completeRegistrationService;
        this.dataApprovalService = dataApprovalService;
        this.dataApprovalAuditService = dataApprovalAuditService;
        this.eventPublisher = eventPublisher;
        this.trackedEntityDataValueAuditService = trackedEntityDataValueAuditService;
    }

    // -------------------------------------------------------------------------
    // MaintenanceService implementation
    // -------------------------------------------------------------------------

    @Override
    public int deleteZeroDataValues()
    {
        int result = maintenanceStore.deleteZeroDataValues();

        log.info( "Deleted zero data values: " + result );

        return result;
    }

    @Override
    public int deleteSoftDeletedDataValues()
    {
        int result = maintenanceStore.deleteSoftDeletedDataValues();

        log.info( "Permanently deleted soft deleted data values: " + result );

        return result;
    }

    @Override
    public int deleteSoftDeletedProgramStageInstances()
    {
        int result = maintenanceStore.deleteSoftDeletedProgramStageInstances();

        log.info( "Permanently deleted soft deleted events: " + result );

        return result;
    }

    @Override
    public int deleteSoftDeletedProgramInstances()
    {
        int result = maintenanceStore.deleteSoftDeletedProgramInstances();

        log.info( "Permanently deleted soft deleted enrollments: " + result );

        return result;
    }

    @Override
    public int deleteSoftDeletedTrackedEntityInstances()
    {
        int result = maintenanceStore.deleteSoftDeletedTrackedEntityInstances();

        log.info( "Permanently deleted soft deleted tracked entity instances: " + result );

        return result;
    }

    @Override
    public void prunePeriods()
    {
        for ( Period period : periodService.getAllPeriods() )
        {
            long periodId = period.getId();

            try
            {
                periodService.deletePeriod( period );

                log.info( "Deleted period with id: " + periodId );
            }
            catch ( DeleteNotAllowedException ex )
            {
                log.debug( "Period has associated objects and could not be deleted: " + periodId );
            }
        }
    }

    @Override
    @Transactional
    public boolean pruneData( OrganisationUnit organisationUnit )
    {
        User user = currentUserService.getCurrentUser();

        if ( user == null || !user.isSuper() )
        {
            return false;
        }

        dataApprovalService.deleteDataApprovals( organisationUnit );
        dataApprovalAuditService.deleteDataApprovalAudits( organisationUnit );
        completeRegistrationService.deleteCompleteDataSetRegistrations( organisationUnit );
        dataValueAuditService.deleteDataValueAudits( organisationUnit );
        dataValueService.deleteDataValues( organisationUnit );

        log.info( "Pruned data for organisation unit: " + organisationUnit );

        return true;
    }

    @Override
    @Transactional
    public boolean pruneData( DataElement dataElement )
    {
        User user = currentUserService.getCurrentUser();

        if ( user == null || !user.isSuper() )
        {
            return false;
        }

        trackedEntityDataValueAuditService.deleteTrackedEntityDataValueAudit( dataElement );
        dataValueAuditService.deleteDataValueAudits( dataElement );
        dataValueService.deleteDataValues( dataElement );

        log.info( "Pruned data for data element: " + dataElement );

        return true;
    }

    @Override
    @Transactional
    public int removeExpiredInvitations()
    {
        UserQueryParams params = new UserQueryParams();
        params.setInvitationStatus( UserInvitationStatus.EXPIRED );

        int userCount = userService.getUserCount( params );
        int removeCount = 0;

        PageRange range = new PageRange( userCount ).setPageSize( 200 );
        List<int[]> pages = range.getPages();
        Collections.reverse( pages ); // Iterate from end since users are
                                      // deleted

        log.debug( "Pages: " + pages );

        for ( int[] page : pages )
        {
            params.setFirst( page[0] );
            params.setMax( range.getPageSize() );
            List<User> users = userService.getUsers( params );

            for ( User user : users )
            {
                try
                {
                    userService.deleteUser( user );
                    removeCount++;
                }
                catch ( DeleteNotAllowedException ex )
                {
                    log.warn( "Could not delete user " + user.getUsername() );
                }
            }
        }

        log.info( "Removed expired invitations: " + removeCount );

        return removeCount;
    }

    @Override
    public void clearApplicationCaches()
    {
        eventPublisher.publishEvent( new ApplicationCacheClearedEvent() );
    }
}
