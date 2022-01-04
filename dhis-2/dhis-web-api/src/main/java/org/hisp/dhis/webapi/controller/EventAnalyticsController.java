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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.common.DimensionalObjectUtils.getItemsFromParam;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import org.hisp.dhis.analytics.Rectangle;
import org.hisp.dhis.analytics.dimension.DimensionFilteringAndPagingService;
import org.hisp.dhis.analytics.event.EventAnalyticsDimensionsService;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DimensionsCriteria;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.EventsAnalyticsQueryCriteria;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Lars Helge Overland
 */
@Controller
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@AllArgsConstructor
public class EventAnalyticsController
{
    private static final String RESOURCE_PATH = "/analytics/events";

    @NonNull
    private final EventDataQueryService eventDataService;

    @NonNull
    private final EventAnalyticsService analyticsService;

    @NonNull
    private final ContextUtils contextUtils;

    @NonNull
    private final DimensionFilteringAndPagingService dimensionFilteringAndPagingService;

    @NonNull
    private final EventAnalyticsDimensionsService eventAnalyticsDimensionsService;

    // -------------------------------------------------------------------------
    // Aggregate
    // -------------------------------------------------------------------------

    @GetMapping( value = RESOURCE_PATH + "/aggregate/{program}", produces = { APPLICATION_JSON_VALUE,
        "application/javascript" } )
    public @ResponseBody Grid getAggregateJson( // JSON, JSONP
        @PathVariable String program,
        EventsAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
        throws Exception
    {
        EventQueryParams params = getEventQueryParams( program, criteria, apiVersion );

        configResponseForJson( response );

        return analyticsService.getAggregatedEventData( params, getItemsFromParam( criteria.getColumns() ),
            getItemsFromParam( criteria.getRows() ) );
    }

    @GetMapping( value = RESOURCE_PATH + "/aggregate/{program}.xml" )
    public void getAggregateXml(
        @PathVariable String program,
        EventsAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
        throws Exception
    {
        GridUtils.toXml( getAggregatedGridWithAttachment( criteria, program, apiVersion,
            ContextUtils.CONTENT_TYPE_XML, "events.xml", response ), response.getOutputStream() );
    }

    @GetMapping( value = RESOURCE_PATH + "/aggregate/{program}.xls" )
    public void getAggregateXls(
        @PathVariable String program,
        EventsAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
        throws Exception
    {
        GridUtils.toXls( getAggregatedGridWithAttachment( criteria, program, apiVersion,
            ContextUtils.CONTENT_TYPE_EXCEL, "events.xls", response ), response.getOutputStream() );
    }

    @GetMapping( value = RESOURCE_PATH + "/aggregate/{program}.csv" )
    public void getAggregateCsv(
        @PathVariable String program,
        EventsAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
        throws Exception
    {
        GridUtils.toCsv( getAggregatedGridWithAttachment( criteria, program, apiVersion,
            ContextUtils.CONTENT_TYPE_CSV, "events.csv", response ), response.getWriter() );
    }

    @GetMapping( value = RESOURCE_PATH + "/aggregate/{program}.html" )
    public void getAggregateHtml(
        @PathVariable String program,
        EventsAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
        throws Exception
    {
        GridUtils.toHtml( getAggregatedGridWithAttachment( criteria, program, apiVersion,
            ContextUtils.CONTENT_TYPE_HTML, "events.html", response ), response.getWriter() );
    }

    @GetMapping( value = RESOURCE_PATH + "/aggregate/{program}.html+css" )
    public void getAggregateHtmlCss(
        @PathVariable String program,
        EventsAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
        throws Exception
    {
        GridUtils.toHtmlCss( getAggregatedGridWithAttachment( criteria, program, apiVersion,
            ContextUtils.CONTENT_TYPE_HTML, "events.html", response ), response.getWriter() );
    }

    @GetMapping( value = RESOURCE_PATH + "/aggregate/dimensions", produces = { APPLICATION_JSON_VALUE,
        "application/javascript" } )
    public @ResponseBody PagingWrapper<ObjectNode> getAggregateDimensions(
        @RequestParam String programStageId,
        @RequestParam( defaultValue = "*" ) List<String> fields,
        DimensionsCriteria dimensionsCriteria,
        HttpServletResponse response )
    {
        configResponseForJson( response );
        return dimensionFilteringAndPagingService
            .pageAndFilter(
                eventAnalyticsDimensionsService.getAggregateDimensionsByProgramStageId( programStageId ),
                dimensionsCriteria,
                fields );
    }

    // -------------------------------------------------------------------------
    // Count / rectangle
    // -------------------------------------------------------------------------

    @GetMapping( value = RESOURCE_PATH + "/count/{program}", produces = { APPLICATION_JSON_VALUE,
        "application/javascript" } )
    public @ResponseBody Rectangle getCountJson( // JSON, JSONP
        @PathVariable String program,
        EventsAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
    {
        EventQueryParams params = getEventQueryParams( program, criteria, apiVersion );

        configResponseForJson( response );

        return analyticsService.getRectangle( params );
    }

    // -------------------------------------------------------------------------
    // Clustering
    // -------------------------------------------------------------------------

    @GetMapping( value = RESOURCE_PATH + "/cluster/{program}", produces = { APPLICATION_JSON_VALUE,
        "application/javascript" } )
    public @ResponseBody Grid getClusterJson( // JSON, JSONP
        @PathVariable String program,
        EventsAnalyticsQueryCriteria criteria,
        @RequestParam Long clusterSize,
        @RequestParam String bbox,
        @RequestParam( required = false ) boolean includeClusterPoints,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
    {
        EventQueryParams params = getEventQueryParams( program, criteria, apiVersion );

        params = new EventQueryParams.Builder( params )
            .withClusterSize( clusterSize )
            .withBbox( bbox )
            .withIncludeClusterPoints( includeClusterPoints )
            .build();

        configResponseForJson( response );

        return analyticsService.getEventClusters( params );
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    @GetMapping( value = RESOURCE_PATH + "/query/{program}", produces = { APPLICATION_JSON_VALUE,
        "application/javascript" } )
    public @ResponseBody Grid getQueryJson( // JSON, JSONP
        @PathVariable String program,
        EventsAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
    {
        EventQueryParams params = getEventQueryParams( program, criteria, apiVersion );

        configResponseForJson( response );

        return analyticsService.getEvents( params );
    }

    @GetMapping( value = RESOURCE_PATH + "/query/{program}.xml" )
    public void getQueryXml(
        @PathVariable String program,
        EventsAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
        throws Exception
    {
        GridUtils.toXml( getListGridWithAttachment( criteria, program, apiVersion, ContextUtils.CONTENT_TYPE_XML,
            "events.xml", false, response ), response.getOutputStream() );
    }

    @GetMapping( value = RESOURCE_PATH + "/query/{program}.xls" )
    public void getQueryXls(
        @PathVariable String program,
        EventsAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
        throws Exception
    {
        GridUtils.toXls( getListGridWithAttachment( criteria, program, apiVersion, ContextUtils.CONTENT_TYPE_EXCEL,
            "events.xls", true, response ), response.getOutputStream() );
    }

    @GetMapping( value = RESOURCE_PATH + "/query/{program}.csv" )
    public void getQueryCsv(
        @PathVariable String program,
        EventsAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
        throws Exception
    {
        GridUtils.toCsv( getListGridWithAttachment( criteria, program, apiVersion, ContextUtils.CONTENT_TYPE_CSV,
            "events.csv", true, response ), response.getWriter() );
    }

    @GetMapping( value = RESOURCE_PATH + "/query/{program}.html" )
    public void getQueryHtml(
        @PathVariable String program,
        EventsAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
        throws Exception
    {
        GridUtils.toHtml( getListGridWithAttachment( criteria, program, apiVersion, ContextUtils.CONTENT_TYPE_HTML,
            "events.html", false, response ), response.getWriter() );
    }

    @GetMapping( value = RESOURCE_PATH + "/query/{program}.html+css" )
    public void getQueryHtmlCss(
        @PathVariable String program,
        EventsAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
        throws Exception
    {
        GridUtils.toHtmlCss( getListGridWithAttachment( criteria, program, apiVersion, ContextUtils.CONTENT_TYPE_HTML,
            "events.html", false, response ), response.getWriter() );
    }

    @GetMapping( value = RESOURCE_PATH + "/query/dimensions", produces = { APPLICATION_JSON_VALUE,
        "application/javascript" } )
    public @ResponseBody PagingWrapper<ObjectNode> getQueryDimensions(
        @RequestParam String programStageId,
        @RequestParam( defaultValue = "*" ) List<String> fields,
        DimensionsCriteria dimensionsCriteria,
        HttpServletResponse response )
    {
        configResponseForJson( response );
        return dimensionFilteringAndPagingService
            .pageAndFilter(
                eventAnalyticsDimensionsService.getQueryDimensionsByProgramStageId( programStageId ),
                dimensionsCriteria,
                fields );
    }

    private Grid getAggregatedGridWithAttachment( EventsAnalyticsQueryCriteria criteria, String program,
        DhisApiVersion apiVersion,
        String contentType, String file,
        HttpServletResponse response )
        throws Exception
    {
        EventQueryParams params = getEventQueryParams( program, criteria, apiVersion );

        contextUtils.configureResponse( response, contentType, CacheStrategy.RESPECT_SYSTEM_SETTING,
            file, false );
        return analyticsService.getAggregatedEventData( params, getItemsFromParam( criteria.getColumns() ),
            getItemsFromParam( criteria.getRows() ) );
    }

    private Grid getListGridWithAttachment( EventsAnalyticsQueryCriteria criteria, String program,
        DhisApiVersion apiVersion,
        String contentType, String file, boolean attachment,
        HttpServletResponse response )
    {
        EventQueryParams params = getEventQueryParams( program, criteria, apiVersion );

        contextUtils.configureResponse( response, contentType, CacheStrategy.RESPECT_SYSTEM_SETTING, file, attachment );
        return analyticsService.getEvents( params );
    }

    private EventQueryParams getEventQueryParams( String program, EventsAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion )
    {
        EventDataQueryRequest request = EventDataQueryRequest.builder()
            .fromCriteria( criteria )
            .program( program )
            .apiVersion( apiVersion ).build();

        return eventDataService.getFromRequest( request );
    }

    private void configResponseForJson( HttpServletResponse response )
    {
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON,
            CacheStrategy.RESPECT_SYSTEM_SETTING );
    }
}
