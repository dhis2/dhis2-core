package org.hisp.dhis.dataadmin.action.maintenance;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.maintenance.MaintenanceService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Lars Helge Overland
 */
public class PerformMaintenanceAction
    implements Action
{
    private static final Log log = LogFactory.getLog( PerformMaintenanceAction.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Resource( name = "org.hisp.dhis.analytics.AnalyticsTableService" )
    private AnalyticsTableService analyticsTableService;

    @Resource( name = "org.hisp.dhis.analytics.CompletenessTableService" )
    private AnalyticsTableService completenessTableService;

    @Resource( name = "org.hisp.dhis.analytics.CompletenessTargetTableService" )
    private AnalyticsTableService completenessTargetTableService;

    @Resource( name = "org.hisp.dhis.analytics.OrgUnitTargetTableService" )
    private AnalyticsTableService orgUnitTargetTableService;

    @Resource( name = "org.hisp.dhis.analytics.EventAnalyticsTableService" )
    private AnalyticsTableService eventAnalyticsTableService;

    private MaintenanceService maintenanceService;

    public void setMaintenanceService( MaintenanceService maintenanceService )
    {
        this.maintenanceService = maintenanceService;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private ResourceTableService resourceTableService;

    public void setResourceTableService( ResourceTableService resourceTableService )
    {
        this.resourceTableService = resourceTableService;
    }

    @Autowired
    private OrganisationUnitService organisationUnitService;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private boolean clearAnalytics;

    public void setClearAnalytics( boolean clearAnalytics )
    {
        this.clearAnalytics = clearAnalytics;
    }

    private boolean zeroValues;

    public void setZeroValues( boolean zeroValues )
    {
        this.zeroValues = zeroValues;
    }

    private boolean prunePeriods;

    public void setPrunePeriods( boolean prunePeriods )
    {
        this.prunePeriods = prunePeriods;
    }

    private boolean removeExpiredInvitations;

    public void setRemoveExpiredInvitations( boolean removeExpiredInvitations )
    {
        this.removeExpiredInvitations = removeExpiredInvitations;
    }

    private boolean dropSqlViews;

    public void setDropSqlViews( boolean dropSqlViews )
    {
        this.dropSqlViews = dropSqlViews;
    }

    private boolean createSqlViews;

    public void setCreateSqlViews( boolean createSqlViews )
    {
        this.createSqlViews = createSqlViews;
    }

    private boolean updateCategoryOptionCombos;

    public void setUpdateCategoryOptionCombos( boolean updateCategoryOptionCombos )
    {
        this.updateCategoryOptionCombos = updateCategoryOptionCombos;
    }

    private boolean updateOrganisationUnitPaths;

    public void setUpdateOrganisationUnitPaths( boolean updateOrganisationUnitPaths )
    {
        this.updateOrganisationUnitPaths = updateOrganisationUnitPaths;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        String username = currentUserService.getCurrentUsername();

        if ( clearAnalytics )
        {
            resourceTableService.dropAllSqlViews();
            analyticsTableService.dropTables();
            completenessTableService.dropTables();
            completenessTargetTableService.dropTables();
            orgUnitTargetTableService.dropTables();
            eventAnalyticsTableService.dropTables();

            log.info( "'" + username + "': Cleared analytics tables" );
        }

        if ( zeroValues )
        {
            maintenanceService.deleteZeroDataValues();

            log.info( "Cleared zero values" );
        }

        if ( prunePeriods )
        {
            maintenanceService.prunePeriods();

            log.info( "'" + username + "': Pruned periods" );
        }

        if ( removeExpiredInvitations )
        {
            maintenanceService.removeExpiredInvitations();

            log.info( "'" + username + "': Removed expired invitations" );
        }

        if ( dropSqlViews )
        {
            resourceTableService.dropAllSqlViews();

            log.info( "'" + username + "': Dropped SQL views" );
        }

        if ( createSqlViews )
        {
            resourceTableService.createAllSqlViews();

            log.info( "'" + username + "': Created SQL views" );
        }

        if ( updateCategoryOptionCombos )
        {
            categoryService.updateAllOptionCombos();

            log.info( "'" + username + "': Updated category option combos" );
        }

        if ( updateOrganisationUnitPaths )
        {
            organisationUnitService.forceUpdatePaths();

            log.info( "'" + username + "': Updated organisation unit paths" );
        }

        return SUCCESS;
    }
}
