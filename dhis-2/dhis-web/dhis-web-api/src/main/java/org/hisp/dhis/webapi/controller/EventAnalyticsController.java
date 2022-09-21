/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.webapi.controller;

import java.util.Date;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.Rectangle;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.event.EventStatus;
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

/**
 * @author Lars Helge Overland
 */
@Controller
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class EventAnalyticsController
{
    private static final String RESOURCE_PATH = "/analytics/events";

    private static final String DEFAULT_OUTPUT_TYPE = "EVENT";

    @Autowired
    private EventDataQueryService eventDataQueryService;

    @Autowired
    private EventAnalyticsService analyticsService;

    @Autowired
    private ContextUtils contextUtils;

    // -------------------------------------------------------------------------
    // Aggregate
    // -------------------------------------------------------------------------

    @RequestMapping( value = RESOURCE_PATH + "/aggregate/{program}", method = RequestMethod.GET, produces = {
        "application/json", "application/javascript" } )
    public @ResponseBody Grid getAggregateJson( // JSON, JSONP
        @PathVariable String program,
        @RequestParam( required = false ) String stage,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String value,
        @RequestParam( required = false ) AggregationType aggregationType,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean skipRounding,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) SortOrder sortOrder,
        @RequestParam( required = false ) Integer limit,
        @RequestParam( required = false, defaultValue = DEFAULT_OUTPUT_TYPE ) EventOutputType outputType,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) boolean collapseDataDimensions,
        @RequestParam( required = false ) boolean aggregateData,
        @RequestParam( required = false ) boolean includeMetadataDetails,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String timeField,
        @RequestParam( required = false ) String orgUnitField,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String columns,
        @RequestParam( required = false ) String rows,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
        throws Exception
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).stage( stage )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).value( value )
            .aggregationType( aggregationType ).skipMeta( skipMeta ).skipData( skipData ).skipRounding( skipRounding )
            .completedOnly( completedOnly ).hierarchyMeta( hierarchyMeta ).showHierarchy( showHierarchy )
            .sortOrder( sortOrder ).limit( limit ).outputType( outputType ).eventStatus( eventStatus )
            .programStatus( programStatus ).collapseDataDimensions( collapseDataDimensions )
            .aggregateData( aggregateData ).includeMetadataDetails( includeMetadataDetails )
            .displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate )
            .timeField( timeField ).orgUnitField( orgUnitField ).userOrgUnit( userOrgUnit ).apiVersion( apiVersion )
            .build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON,
            CacheStrategy.RESPECT_SYSTEM_SETTING );
        return analyticsService.getAggregatedEventData( params, DimensionalObjectUtils.getItemsFromParam( columns ),
            DimensionalObjectUtils.getItemsFromParam( rows ) );
    }

    @RequestMapping( value = RESOURCE_PATH + "/aggregate/{program}.xml", method = RequestMethod.GET )
    public void getAggregateXml(
        @PathVariable String program,
        @RequestParam( required = false ) String stage,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String value,
        @RequestParam( required = false ) AggregationType aggregationType,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean skipRounding,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) SortOrder sortOrder,
        @RequestParam( required = false ) Integer limit,
        @RequestParam( required = false, defaultValue = DEFAULT_OUTPUT_TYPE ) EventOutputType outputType,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) boolean collapseDataDimensions,
        @RequestParam( required = false ) boolean aggregateData,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String timeField,
        @RequestParam( required = false ) String orgUnitField,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String columns,
        @RequestParam( required = false ) String rows,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
        throws Exception
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).stage( stage )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).value( value )
            .aggregationType( aggregationType ).skipMeta( skipMeta ).skipData( skipData ).skipRounding( skipRounding )
            .completedOnly( completedOnly ).hierarchyMeta( hierarchyMeta ).showHierarchy( showHierarchy )
            .sortOrder( sortOrder ).limit( limit ).outputType( outputType ).eventStatus( eventStatus )
            .programStatus( programStatus ).collapseDataDimensions( collapseDataDimensions )
            .aggregateData( aggregateData ).displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate )
            .timeField( timeField ).orgUnitField( orgUnitField ).userOrgUnit( userOrgUnit ).apiVersion( apiVersion )
            .build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_XML, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "events.xml", false );
        Grid grid = analyticsService.getAggregatedEventData( params,
            DimensionalObjectUtils.getItemsFromParam( columns ), DimensionalObjectUtils.getItemsFromParam( rows ) );
        GridUtils.toXml( grid, response.getOutputStream() );
    }

    @RequestMapping( value = RESOURCE_PATH + "/aggregate/{program}.xls", method = RequestMethod.GET )
    public void getAggregateXls(
        @PathVariable String program,
        @RequestParam( required = false ) String stage,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String value,
        @RequestParam( required = false ) AggregationType aggregationType,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean skipRounding,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) SortOrder sortOrder,
        @RequestParam( required = false ) Integer limit,
        @RequestParam( required = false, defaultValue = DEFAULT_OUTPUT_TYPE ) EventOutputType outputType,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) boolean collapseDataDimensions,
        @RequestParam( required = false ) boolean aggregateData,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String timeField,
        @RequestParam( required = false ) String orgUnitField,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String columns,
        @RequestParam( required = false ) String rows,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
        throws Exception
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).stage( stage )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).value( value )
            .aggregationType( aggregationType ).skipMeta( skipMeta ).skipData( skipData ).skipRounding( skipRounding )
            .completedOnly( completedOnly ).hierarchyMeta( hierarchyMeta ).showHierarchy( showHierarchy )
            .sortOrder( sortOrder ).limit( limit ).outputType( outputType ).eventStatus( eventStatus )
            .programStatus( programStatus ).collapseDataDimensions( collapseDataDimensions )
            .aggregateData( aggregateData ).displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate )
            .timeField( timeField ).orgUnitField( orgUnitField ).userOrgUnit( userOrgUnit ).apiVersion( apiVersion )
            .build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_EXCEL, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "events.xls", true );
        Grid grid = analyticsService.getAggregatedEventData( params,
            DimensionalObjectUtils.getItemsFromParam( columns ), DimensionalObjectUtils.getItemsFromParam( rows ) );
        GridUtils.toXls( grid, response.getOutputStream() );
    }

    @RequestMapping( value = RESOURCE_PATH + "/aggregate/{program}.csv", method = RequestMethod.GET )
    public void getAggregateCsv(
        @PathVariable String program,
        @RequestParam( required = false ) String stage,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String value,
        @RequestParam( required = false ) AggregationType aggregationType,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean skipRounding,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) SortOrder sortOrder,
        @RequestParam( required = false ) Integer limit,
        @RequestParam( required = false, defaultValue = DEFAULT_OUTPUT_TYPE ) EventOutputType outputType,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) boolean collapseDataDimensions,
        @RequestParam( required = false ) boolean aggregateData,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String timeField,
        @RequestParam( required = false ) String orgUnitField,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String columns,
        @RequestParam( required = false ) String rows,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
        throws Exception
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).stage( stage )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).value( value )
            .aggregationType( aggregationType ).skipMeta( skipMeta ).skipData( skipData ).skipRounding( skipRounding )
            .completedOnly( completedOnly ).hierarchyMeta( hierarchyMeta ).showHierarchy( showHierarchy )
            .sortOrder( sortOrder ).limit( limit ).outputType( outputType ).eventStatus( eventStatus )
            .programStatus( programStatus ).collapseDataDimensions( collapseDataDimensions )
            .aggregateData( aggregateData ).displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate )
            .timeField( timeField ).orgUnitField( orgUnitField ).userOrgUnit( userOrgUnit ).apiVersion( apiVersion )
            .build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_CSV, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "events.csv", true );
        Grid grid = analyticsService.getAggregatedEventData( params,
            DimensionalObjectUtils.getItemsFromParam( columns ), DimensionalObjectUtils.getItemsFromParam( rows ) );
        GridUtils.toCsv( grid, response.getWriter() );
    }

    @RequestMapping( value = RESOURCE_PATH + "/aggregate/{program}.html", method = RequestMethod.GET )
    public void getAggregateHtml(
        @PathVariable String program,
        @RequestParam( required = false ) String stage,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String value,
        @RequestParam( required = false ) AggregationType aggregationType,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean skipRounding,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) SortOrder sortOrder,
        @RequestParam( required = false ) Integer limit,
        @RequestParam( required = false, defaultValue = DEFAULT_OUTPUT_TYPE ) EventOutputType outputType,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) boolean collapseDataDimensions,
        @RequestParam( required = false ) boolean aggregateData,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String timeField,
        @RequestParam( required = false ) String orgUnitField,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String columns,
        @RequestParam( required = false ) String rows,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
        throws Exception
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).stage( stage )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).value( value )
            .aggregationType( aggregationType ).skipMeta( skipMeta ).skipData( skipData ).skipRounding( skipRounding )
            .completedOnly( completedOnly ).hierarchyMeta( hierarchyMeta ).showHierarchy( showHierarchy )
            .sortOrder( sortOrder ).limit( limit ).outputType( outputType ).eventStatus( eventStatus )
            .programStatus( programStatus ).collapseDataDimensions( collapseDataDimensions )
            .aggregateData( aggregateData ).displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate )
            .timeField( timeField ).orgUnitField( orgUnitField ).userOrgUnit( userOrgUnit ).apiVersion( apiVersion )
            .build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "events.html", false );
        Grid grid = analyticsService.getAggregatedEventData( params,
            DimensionalObjectUtils.getItemsFromParam( columns ), DimensionalObjectUtils.getItemsFromParam( rows ) );
        GridUtils.toHtml( grid, response.getWriter() );
    }

    @RequestMapping( value = RESOURCE_PATH + "/aggregate/{program}.html+css", method = RequestMethod.GET )
    public void getAggregateHtmlCss(
        @PathVariable String program,
        @RequestParam( required = false ) String stage,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String value,
        @RequestParam( required = false ) AggregationType aggregationType,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean skipRounding,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) SortOrder sortOrder,
        @RequestParam( required = false ) Integer limit,
        @RequestParam( required = false, defaultValue = DEFAULT_OUTPUT_TYPE ) EventOutputType outputType,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) boolean collapseDataDimensions,
        @RequestParam( required = false ) boolean aggregateData,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String timeField,
        @RequestParam( required = false ) String orgUnitField,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String columns,
        @RequestParam( required = false ) String rows,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
        throws Exception
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).stage( stage )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).value( value )
            .aggregationType( aggregationType ).skipMeta( skipMeta ).skipData( skipData ).skipRounding( skipRounding )
            .completedOnly( completedOnly ).hierarchyMeta( hierarchyMeta ).showHierarchy( showHierarchy )
            .sortOrder( sortOrder ).limit( limit ).outputType( outputType ).eventStatus( eventStatus )
            .programStatus( programStatus ).collapseDataDimensions( collapseDataDimensions )
            .aggregateData( aggregateData ).displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate )
            .timeField( timeField ).orgUnitField( orgUnitField ).userOrgUnit( userOrgUnit ).apiVersion( apiVersion )
            .build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "events.html", false );
        Grid grid = analyticsService.getAggregatedEventData( params,
            DimensionalObjectUtils.getItemsFromParam( columns ), DimensionalObjectUtils.getItemsFromParam( rows ) );
        GridUtils.toHtmlCss( grid, response.getWriter() );
    }

    // -------------------------------------------------------------------------
    // Count / rectangle
    // -------------------------------------------------------------------------

    @RequestMapping( value = RESOURCE_PATH + "/count/{program}", method = RequestMethod.GET, produces = {
        "application/json", "application/javascript" } )
    public @ResponseBody Rectangle getCountJson( // JSON, JSONP
        @PathVariable String program,
        @RequestParam( required = false ) String stage,
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
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String coordinateField,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).stage( stage )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).ouMode( ouMode )
            .asc( asc ).desc( desc ).skipMeta( skipMeta ).skipData( skipData ).completedOnly( completedOnly )
            .hierarchyMeta( hierarchyMeta ).coordinatesOnly( coordinatesOnly ).eventStatus( eventStatus )
            .programStatus( programStatus ).displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate )
            .userOrgUnit( userOrgUnit ).coordinateField( coordinateField ).page( page ).pageSize( pageSize )
            .apiVersion( apiVersion ).outputType( EventOutputType.EVENT ).build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON,
            CacheStrategy.RESPECT_SYSTEM_SETTING );
        return analyticsService.getRectangle( params );
    }

    // -------------------------------------------------------------------------
    // Clustering
    // -------------------------------------------------------------------------

    @RequestMapping( value = RESOURCE_PATH + "/cluster/{program}", method = RequestMethod.GET, produces = {
        "application/json", "application/javascript" } )
    public @ResponseBody Grid getClusterJson( // JSON, JSONP
        @PathVariable String program,
        @RequestParam( required = false ) String stage,
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
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam Long clusterSize,
        @RequestParam( required = false ) String coordinateField,
        @RequestParam String bbox,
        @RequestParam( required = false ) boolean includeClusterPoints,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).stage( stage )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).ouMode( ouMode )
            .asc( asc ).desc( desc ).skipMeta( skipMeta ).skipData( skipData ).completedOnly( completedOnly )
            .hierarchyMeta( hierarchyMeta ).coordinatesOnly( coordinatesOnly ).eventStatus( eventStatus )
            .programStatus( programStatus ).displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate )
            .userOrgUnit( userOrgUnit ).coordinateField( coordinateField ).page( page ).pageSize( pageSize )
            .apiVersion( apiVersion ).outputType( EventOutputType.EVENT ).build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        params = new EventQueryParams.Builder( params )
            .withClusterSize( clusterSize )
            .withBbox( bbox )
            .withIncludeClusterPoints( includeClusterPoints )
            .build();

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON,
            CacheStrategy.RESPECT_SYSTEM_SETTING );

        return analyticsService.getEventClusters( params );
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    @RequestMapping( value = RESOURCE_PATH + "/query/{program}", method = RequestMethod.GET, produces = {
        "application/json", "application/javascript" } )
    public @ResponseBody Grid getQueryJson( // JSON, JSONP
        @PathVariable String program,
        @RequestParam( required = false ) String stage,
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
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean paging,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String coordinateField,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).stage( stage )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).ouMode( ouMode )
            .asc( asc ).desc( desc ).skipMeta( skipMeta ).skipData( skipData ).completedOnly( completedOnly )
            .hierarchyMeta( hierarchyMeta ).coordinatesOnly( coordinatesOnly )
            .includeMetadataDetails( includeMetadataDetails )
            .dataIdScheme( dataIdScheme ).eventStatus( eventStatus ).programStatus( programStatus )
            .displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate ).userOrgUnit( userOrgUnit )
            .coordinateField( coordinateField ).page( page ).pageSize( pageSize ).paging( paging )
            .apiVersion( apiVersion ).outputType( EventOutputType.EVENT ).build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON,
            CacheStrategy.RESPECT_SYSTEM_SETTING );
        return analyticsService.getEvents( params );
    }

    @RequestMapping( value = RESOURCE_PATH + "/query/{program}.xml", method = RequestMethod.GET )
    public void getQueryXml(
        @PathVariable String program,
        @RequestParam( required = false ) String stage,
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
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean paging,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String coordinateField,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
        throws Exception
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).stage( stage )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).ouMode( ouMode )
            .asc( asc ).desc( desc ).skipMeta( skipMeta ).skipData( skipData ).completedOnly( completedOnly )
            .hierarchyMeta( hierarchyMeta ).coordinatesOnly( coordinatesOnly )
            .includeMetadataDetails( includeMetadataDetails )
            .dataIdScheme( dataIdScheme ).eventStatus( eventStatus )
            .programStatus( programStatus ).displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate )
            .userOrgUnit( userOrgUnit ).coordinateField( coordinateField ).page( page ).pageSize( pageSize )
            .paging( paging ).apiVersion( apiVersion ).outputType( EventOutputType.EVENT ).build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_XML, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "events.xml", false );
        Grid grid = analyticsService.getEvents( params );
        GridUtils.toXml( grid, response.getOutputStream() );
    }

    @RequestMapping( value = RESOURCE_PATH + "/query/{program}.xls", method = RequestMethod.GET )
    public void getQueryXls(
        @PathVariable String program,
        @RequestParam( required = false ) String stage,
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
        @RequestParam( required = false ) IdScheme dataIdScheme,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String coordinateField,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
        throws Exception
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).stage( stage )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).ouMode( ouMode )
            .asc( asc ).desc( desc ).skipMeta( skipMeta ).skipData( skipData ).completedOnly( completedOnly )
            .hierarchyMeta( hierarchyMeta ).coordinatesOnly( coordinatesOnly ).dataIdScheme( dataIdScheme )
            .eventStatus( eventStatus )
            .programStatus( programStatus ).displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate )
            .userOrgUnit( userOrgUnit ).coordinateField( coordinateField ).page( page ).pageSize( pageSize )
            .apiVersion( apiVersion ).outputType( EventOutputType.EVENT ).build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_EXCEL, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "events.xls", true );
        Grid grid = analyticsService.getEvents( params );
        GridUtils.toXls( grid, response.getOutputStream() );
    }

    @RequestMapping( value = RESOURCE_PATH + "/query/{program}.csv", method = RequestMethod.GET )
    public void getQueryCsv(
        @PathVariable String program,
        @RequestParam( required = false ) String stage,
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
        @RequestParam( required = false ) IdScheme dataIdScheme,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String coordinateField,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
        throws Exception
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).stage( stage )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).ouMode( ouMode )
            .asc( asc ).desc( desc ).skipMeta( skipMeta ).skipData( skipData ).completedOnly( completedOnly )
            .hierarchyMeta( hierarchyMeta ).coordinatesOnly( coordinatesOnly ).dataIdScheme( dataIdScheme )
            .eventStatus( eventStatus )
            .programStatus( programStatus ).displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate )
            .userOrgUnit( userOrgUnit ).coordinateField( coordinateField ).page( page ).pageSize( pageSize )
            .apiVersion( apiVersion ).outputType( EventOutputType.EVENT ).build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_CSV, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "events.csv", true );
        Grid grid = analyticsService.getEvents( params );
        GridUtils.toCsv( grid, response.getWriter() );
    }

    @RequestMapping( value = RESOURCE_PATH + "/query/{program}.html", method = RequestMethod.GET )
    public void getQueryHtml(
        @PathVariable String program,
        @RequestParam( required = false ) String stage,
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
        @RequestParam( required = false ) IdScheme dataIdScheme,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String coordinateField,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
        throws Exception
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).stage( stage )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).ouMode( ouMode )
            .asc( asc ).desc( desc ).skipMeta( skipMeta ).skipData( skipData ).completedOnly( completedOnly )
            .hierarchyMeta( hierarchyMeta ).coordinatesOnly( coordinatesOnly ).dataIdScheme( dataIdScheme )
            .eventStatus( eventStatus )
            .programStatus( programStatus ).displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate )
            .userOrgUnit( userOrgUnit ).coordinateField( coordinateField ).page( page ).pageSize( pageSize )
            .apiVersion( apiVersion ).outputType( EventOutputType.EVENT ).build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "events.html", false );
        Grid grid = analyticsService.getEvents( params );
        GridUtils.toHtml( grid, response.getWriter() );
    }

    @RequestMapping( value = RESOURCE_PATH + "/query/{program}.html+css", method = RequestMethod.GET )
    public void getQueryHtmlCss(
        @PathVariable String program,
        @RequestParam( required = false ) String stage,
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
        @RequestParam( required = false ) IdScheme dataIdScheme,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String coordinateField,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
        throws Exception
    {
        EventDataQueryRequest request = EventDataQueryRequest.newBuilder().program( program ).stage( stage )
            .startDate( startDate ).endDate( endDate ).dimension( dimension ).filter( filter ).ouMode( ouMode )
            .asc( asc ).desc( desc ).skipMeta( skipMeta ).skipData( skipData ).completedOnly( completedOnly )
            .hierarchyMeta( hierarchyMeta ).coordinatesOnly( coordinatesOnly ).dataIdScheme( dataIdScheme )
            .eventStatus( eventStatus )
            .programStatus( programStatus ).displayProperty( displayProperty ).relativePeriodDate( relativePeriodDate )
            .userOrgUnit( userOrgUnit ).coordinateField( coordinateField ).page( page ).pageSize( pageSize )
            .apiVersion( apiVersion ).outputType( EventOutputType.EVENT ).build();

        EventQueryParams params = eventDataQueryService.getFromRequest( request );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "events.html", false );
        Grid grid = analyticsService.getEvents( params );
        GridUtils.toHtmlCss( grid, response.getWriter() );
    }
}
