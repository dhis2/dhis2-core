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

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.analytics.dimension.DimensionFilteringAndPagingService;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsDimensionsService;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsService;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DimensionsCriteria;
import org.hisp.dhis.common.EnrollmentAnalyticsQueryCriteria;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Markus Bekken
 */
@Controller
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequestMapping( "/analytics/enrollments" )
public class EnrollmentAnalyticsController
{
    @Autowired
    private EventDataQueryService eventDataQueryService;

    @Autowired
    private EnrollmentAnalyticsService analyticsService;

    @Autowired
    private ContextUtils contextUtils;

    @Autowired
    private DimensionFilteringAndPagingService dimensionFilteringAndPagingService;

    @Autowired
    private EnrollmentAnalyticsDimensionsService enrollmentAnalyticsDimensionsService;

    @GetMapping( value = "/query/{program}", produces = { APPLICATION_JSON_VALUE, "application/javascript" } )
    public @ResponseBody Grid getQueryJson( // JSON, JSONP
        @PathVariable String program,
        EnrollmentAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
    {
        EventQueryParams params = getEventQueryParams( program, criteria, apiVersion );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON,
            CacheStrategy.RESPECT_SYSTEM_SETTING );
        return analyticsService.getEnrollments( params );
    }

    @GetMapping( "/query/{program}.xml" )
    public void getQueryXml(
        @PathVariable String program,
        EnrollmentAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
        throws Exception
    {
        EventQueryParams params = getEventQueryParams( program, criteria, apiVersion );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_XML, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "enrollments.xml", false );
        Grid grid = analyticsService.getEnrollments( params );
        GridUtils.toXml( grid, response.getOutputStream() );
    }

    @GetMapping( "/query/{program}.xls" )
    public void getQueryXls(
        @PathVariable String program,
        EnrollmentAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
        throws Exception
    {
        EventQueryParams params = getEventQueryParams( program, criteria, apiVersion );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_EXCEL, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "enrollments.xls", true );
        Grid grid = analyticsService.getEnrollments( params );
        GridUtils.toXls( grid, response.getOutputStream() );
    }

    @GetMapping( "/query/{program}.csv" )
    public void getQueryCsv(
        @PathVariable String program,
        EnrollmentAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
        throws Exception
    {
        EventQueryParams params = getEventQueryParams( program, criteria, apiVersion );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_CSV, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "enrollments.csv", true );
        Grid grid = analyticsService.getEnrollments( params );
        GridUtils.toCsv( grid, response.getWriter() );
    }

    @GetMapping( "/query/{program}.html" )
    public void getQueryHtml(
        @PathVariable String program,
        EnrollmentAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
        throws Exception
    {
        EventQueryParams params = getEventQueryParams( program, criteria, apiVersion );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "enrollments.html", false );
        Grid grid = analyticsService.getEnrollments( params );
        GridUtils.toHtml( grid, response.getWriter() );
    }

    @GetMapping( "/query/{program}.html+css" )
    public void getQueryHtmlCss(
        @PathVariable String program,
        EnrollmentAnalyticsQueryCriteria criteria,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response )
        throws Exception
    {
        EventQueryParams params = getEventQueryParams( program, criteria, apiVersion );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "enrollments.html", false );
        Grid grid = analyticsService.getEnrollments( params );
        GridUtils.toHtmlCss( grid, response.getWriter() );
    }

    @GetMapping( "/query/dimensions" )
    public @ResponseBody PagingWrapper<ObjectNode> getQueryDimensions(
        @RequestParam String programId,
        @RequestParam( defaultValue = "*" ) List<String> fields,
        DimensionsCriteria dimensionsCriteria,
        HttpServletResponse response )
    {
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON,
            CacheStrategy.RESPECT_SYSTEM_SETTING );
        return dimensionFilteringAndPagingService
            .pageAndFilter(
                enrollmentAnalyticsDimensionsService.getQueryDimensionsByProgramStageId( programId ),
                dimensionsCriteria,
                fields );
    }

    @GetMapping( "/aggregate/dimensions" )
    public @ResponseBody PagingWrapper<ObjectNode> getAggregateDimensions(
        @RequestParam String programId,
        @RequestParam( defaultValue = "*" ) List<String> fields,
        DimensionsCriteria dimensionsCriteria,
        HttpServletResponse response )
    {
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON,
            CacheStrategy.RESPECT_SYSTEM_SETTING );
        return dimensionFilteringAndPagingService
            .pageAndFilter(
                enrollmentAnalyticsDimensionsService.getAggregateDimensionsByProgramStageId( programId ),
                dimensionsCriteria,
                fields );
    }

    private EventQueryParams getEventQueryParams( @PathVariable String program,
        EnrollmentAnalyticsQueryCriteria criteria, DhisApiVersion apiVersion )
    {
        EventDataQueryRequest request = EventDataQueryRequest.builder()
            .fromCriteria( criteria )
            .program( program )
            .apiVersion( apiVersion )
            .build();

        return eventDataQueryService.getFromRequest( request );
    }

}
