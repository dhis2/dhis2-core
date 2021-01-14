package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2021, University of Oslo
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

import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsService;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.*;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Set;

/**
 * @author Markus Bekken
 */
@Controller
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class EnrollmentAnalyticsController
{
    private static final String RESOURCE_PATH = "/analytics/enrollments";

    @Autowired
    private EventDataQueryService eventDataQueryService;

    @Autowired
    private EnrollmentAnalyticsService analyticsService;

    @Autowired
    private ContextUtils contextUtils;

    @RequestMapping( value = RESOURCE_PATH + "/query/{program}", method = RequestMethod.GET, produces = { "application/json", "application/javascript" } )
    public @ResponseBody Grid getQueryJson( // JSON, JSONP
        @PathVariable String program,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) Set<String> asc,
        @RequestParam( required = false ) Set<String> desc,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean coordinatesOnly,
        @RequestParam( required = false ) boolean includeMetadataDetails,
        @RequestParam( required = false ) IdScheme dataIdScheme,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String coordinateField,
        @RequestParam( required = false ) SortOrder sortOrder,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).sortOrder( sortOrder )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).ouMode( ouMode )
            .asc( asc ).desc( desc ).skipMeta( skipMeta ).skipData( skipData ).completedOnly( completedOnly )
            .hierarchyMeta( hierarchyMeta ).coordinatesOnly( coordinatesOnly ).includeMetadataDetails( includeMetadataDetails )
            .dataIdScheme( dataIdScheme ).programStatus( programStatus ).outputType( EventOutputType.ENROLLMENT )
            .displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate ).userOrgUnit( userOrgUnit )
            .coordinateField( coordinateField ).page( page ).pageSize( pageSize ).apiVersion( apiVersion ).build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.RESPECT_SYSTEM_SETTING );
        return analyticsService.getEnrollments( params );
    }

    @RequestMapping( value = RESOURCE_PATH + "/query/{program}.xml", method = RequestMethod.GET )
    public void getQueryXml(
        @PathVariable String program,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) Set<String> asc,
        @RequestParam( required = false ) Set<String> desc,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean coordinatesOnly,
        @RequestParam( required = false ) boolean includeMetadataDetails,
        @RequestParam( required = false ) IdScheme dataIdScheme,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String coordinateField,
        @RequestParam( required = false ) SortOrder sortOrder,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).sortOrder( sortOrder )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).ouMode( ouMode )
            .asc( asc ).desc( desc ).skipMeta( skipMeta ).skipData( skipData ).completedOnly( completedOnly )
            .hierarchyMeta( hierarchyMeta ).coordinatesOnly( coordinatesOnly ).includeMetadataDetails( includeMetadataDetails )
            .dataIdScheme( dataIdScheme ).programStatus( programStatus ).outputType( EventOutputType.ENROLLMENT )
            .displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate ).userOrgUnit( userOrgUnit )
            .coordinateField( coordinateField ).page( page ).pageSize( pageSize ).apiVersion( apiVersion ).build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_XML, CacheStrategy.RESPECT_SYSTEM_SETTING, "enrollments.xml", false );
        Grid grid = analyticsService.getEnrollments( params );
        GridUtils.toXml( grid, response.getOutputStream() );
    }

    @RequestMapping( value = RESOURCE_PATH + "/query/{program}.xls", method = RequestMethod.GET )
    public void getQueryXls(
        @PathVariable String program,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) Set<String> asc,
        @RequestParam( required = false ) Set<String> desc,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean coordinatesOnly,
        @RequestParam( required = false ) boolean includeMetadataDetails,
        @RequestParam( required = false ) IdScheme dataIdScheme,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String coordinateField,
        @RequestParam( required = false ) SortOrder sortOrder,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).sortOrder( sortOrder )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).ouMode( ouMode )
            .asc( asc ).desc( desc ).skipMeta( skipMeta ).skipData( skipData ).completedOnly( completedOnly )
            .hierarchyMeta( hierarchyMeta ).coordinatesOnly( coordinatesOnly ).includeMetadataDetails( includeMetadataDetails )
            .dataIdScheme( dataIdScheme ).programStatus( programStatus ).outputType( EventOutputType.ENROLLMENT )
            .displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate ).userOrgUnit( userOrgUnit )
            .coordinateField( coordinateField ).page( page ).pageSize( pageSize ).apiVersion( apiVersion ).build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_EXCEL, CacheStrategy.RESPECT_SYSTEM_SETTING, "enrollments.xls", true );
        Grid grid = analyticsService.getEnrollments( params );
        GridUtils.toXls( grid, response.getOutputStream() );
    }

    @RequestMapping( value = RESOURCE_PATH + "/query/{program}.csv", method = RequestMethod.GET )
    public void getQueryCsv(
        @PathVariable String program,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) Set<String> asc,
        @RequestParam( required = false ) Set<String> desc,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean coordinatesOnly,
        @RequestParam( required = false ) boolean includeMetadataDetails,
        @RequestParam( required = false ) IdScheme dataIdScheme,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String coordinateField,
        @RequestParam( required = false ) SortOrder sortOrder,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).sortOrder( sortOrder )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).ouMode( ouMode )
            .asc( asc ).desc( desc ).skipMeta( skipMeta ).skipData( skipData ).completedOnly( completedOnly )
            .hierarchyMeta( hierarchyMeta ).coordinatesOnly( coordinatesOnly ).includeMetadataDetails( includeMetadataDetails )
            .dataIdScheme( dataIdScheme ).programStatus( programStatus ).outputType( EventOutputType.ENROLLMENT )
            .displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate ).userOrgUnit( userOrgUnit )
            .coordinateField( coordinateField ).page( page ).pageSize( pageSize ).apiVersion( apiVersion ).build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_CSV, CacheStrategy.RESPECT_SYSTEM_SETTING, "enrollments.csv", true );
        Grid grid = analyticsService.getEnrollments( params );
        GridUtils.toCsv( grid, response.getWriter() );
    }

    @RequestMapping( value = RESOURCE_PATH + "/query/{program}.html", method = RequestMethod.GET )
    public void getQueryHtml(
        @PathVariable String program,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) Set<String> asc,
        @RequestParam( required = false ) Set<String> desc,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean coordinatesOnly,
        @RequestParam( required = false ) boolean includeMetadataDetails,
        @RequestParam( required = false ) IdScheme dataIdScheme,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String coordinateField,
        @RequestParam( required = false ) SortOrder sortOrder,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).sortOrder( sortOrder )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).ouMode( ouMode )
            .asc( asc ).desc( desc ).skipMeta( skipMeta ).skipData( skipData ).completedOnly( completedOnly )
            .hierarchyMeta( hierarchyMeta ).coordinatesOnly( coordinatesOnly ).includeMetadataDetails( includeMetadataDetails )
            .dataIdScheme( dataIdScheme ).programStatus( programStatus ).outputType( EventOutputType.ENROLLMENT )
            .displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate ).userOrgUnit( userOrgUnit )
            .coordinateField( coordinateField ).page( page ).pageSize( pageSize ).apiVersion( apiVersion ).build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING, "enrollments.html", false );
        Grid grid = analyticsService.getEnrollments( params );
        GridUtils.toHtml( grid, response.getWriter() );
    }

    @RequestMapping( value = RESOURCE_PATH + "/query/{program}.html+css", method = RequestMethod.GET )
    public void getQueryHtmlCss(
        @PathVariable String program,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) Set<String> asc,
        @RequestParam( required = false ) Set<String> desc,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean coordinatesOnly,
        @RequestParam( required = false ) boolean includeMetadataDetails,
        @RequestParam( required = false ) IdScheme dataIdScheme,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String coordinateField,
        @RequestParam( required = false ) SortOrder sortOrder,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).sortOrder( sortOrder )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).ouMode( ouMode )
            .asc( asc ).desc( desc ).skipMeta( skipMeta ).skipData( skipData ).completedOnly( completedOnly )
            .hierarchyMeta( hierarchyMeta ).coordinatesOnly( coordinatesOnly ).includeMetadataDetails( includeMetadataDetails )
            .dataIdScheme( dataIdScheme ).programStatus( programStatus ).outputType( EventOutputType.ENROLLMENT )
            .displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate ).userOrgUnit( userOrgUnit )
            .coordinateField( coordinateField ).page( page ).pageSize( pageSize ).apiVersion( apiVersion ).build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING, "enrollments.html", false );
        Grid grid = analyticsService.getEnrollments( params );
        GridUtils.toHtmlCss( grid, response.getWriter() );
    }
}
