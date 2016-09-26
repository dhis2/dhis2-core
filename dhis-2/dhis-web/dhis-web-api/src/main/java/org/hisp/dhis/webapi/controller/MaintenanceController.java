package org.hisp.dhis.webapi.controller;

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

import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.maintenance.MaintenanceService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = MaintenanceController.RESOURCE_PATH )
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.ALL } )
public class MaintenanceController
{
    public static final String RESOURCE_PATH = "/maintenance";

    @Autowired
    private WebMessageService webMessageService;

    @Autowired
    private MaintenanceService maintenanceService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private HibernateCacheManager cacheManager;

    @Autowired
    private PartitionManager partitionManager;

    @Autowired
    private RenderService renderService;

    @Autowired
    private ResourceTableService resourceTableService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private List<AnalyticsTableService> analyticsTableService;

    @Autowired
    private AppManager appManager;

    @RequestMapping( value = "/analyticsTablesClear", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void clearAnalyticsTables()
    {
        analyticsTableService.forEach( AnalyticsTableService::dropTables );
    }

    @RequestMapping( value = "/expiredInvitationsClear", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void clearExpiredInvitations()
    {
        maintenanceService.removeExpiredInvitations();
    }

    @RequestMapping( value = "/ouPathsUpdate", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void forceUpdatePaths()
    {
        organisationUnitService.forceUpdatePaths();
    }

    @RequestMapping( value = "/periodPruning", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void prunePeriods()
    {
        maintenanceService.prunePeriods();
    }

    @RequestMapping( value = "/zeroDataValueRemoval", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteZeroDataValues()
    {
        maintenanceService.deleteZeroDataValues();
    }

    @RequestMapping( value = "/softDeletedDataValueRemoval", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteSoftDeletedDataValues()
    {
        maintenanceService.deleteSoftDeletedDataValues();
    }
    
    @RequestMapping( value = "/sqlViewsCreate", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void createSqlViews()
    {
        resourceTableService.createAllSqlViews();
    }

    @RequestMapping( value = "/sqlViewsDrop", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void dropSqlViews()
    {
        resourceTableService.dropAllSqlViews();
    }

    @RequestMapping( value = "/categoryOptionComboUpdate", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void updateCategoryOptionCombos()
    {
        categoryService.addAndPruneAllOptionCombos();
    }

    @RequestMapping( value = { "/cacheClear", "/cache" }, method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void clearCache()
    {
        cacheManager.clearCache();
        partitionManager.clearCaches();
    }

    @RequestMapping( value = "/dataPruning/organisationUnits/{uid}", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void pruneDataByOrganisationUnit( @PathVariable String uid, HttpServletResponse response )
        throws Exception
    {
        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( uid );

        if ( organisationUnit == null )
        {
            webMessageService
                .sendJson( WebMessageUtils.conflict( "Organisation unit does not exist: " + uid ), response );
            return;
        }

        boolean result = maintenanceService.pruneData( organisationUnit );

        WebMessage message = result ?
            WebMessageUtils.ok( "Data was pruned successfully" ) :
            WebMessageUtils.conflict( "Data could not be pruned" );

        webMessageService.sendJson( message, response );
    }

    @RequestMapping( value = "/appReload", method = RequestMethod.GET )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public void appReload( HttpServletResponse response )
        throws IOException
    {
        appManager.reloadApps();
        renderService.toJson( response.getOutputStream(), WebMessageUtils.ok( "Apps reloaded" ) );
    }

    @RequestMapping( method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void performMaintenance(
        @RequestParam( required = false ) boolean analyticsTableClear,
        @RequestParam( required = false ) boolean expiredInvitationsClear,
        @RequestParam( required = false ) boolean ouPathsUpdate,
        @RequestParam( required = false ) boolean periodPruning,
        @RequestParam( required = false ) boolean zeroDataValueRemoval,
        @RequestParam( required = false ) boolean softDeletedDataValueRemoval,
        @RequestParam( required = false ) boolean sqlViewsDrop,
        @RequestParam( required = false ) boolean sqlViewsCreate,
        @RequestParam( required = false ) boolean categoryOptionComboUpdate,
        @RequestParam( required = false ) boolean cacheClear,
        @RequestParam( required = false ) boolean appReload )
    {
        if ( analyticsTableClear )
        {
            clearAnalyticsTables();
        }

        if ( expiredInvitationsClear )
        {
            clearExpiredInvitations();
        }

        if ( ouPathsUpdate )
        {
            forceUpdatePaths();
        }

        if ( periodPruning )
        {
            prunePeriods();
        }

        if ( zeroDataValueRemoval )
        {
            deleteZeroDataValues();
        }
        
        if ( softDeletedDataValueRemoval )
        {
            deleteSoftDeletedDataValues();
        }

        if ( sqlViewsDrop )
        {
            dropSqlViews();
        }

        if ( sqlViewsCreate )
        {
            createSqlViews();
        }

        if ( categoryOptionComboUpdate )
        {
            updateCategoryOptionCombos();
        }

        if ( cacheClear )
        {
            clearCache();
        }

        if ( appReload )
        {
            appManager.reloadApps();
        }
    }
}
