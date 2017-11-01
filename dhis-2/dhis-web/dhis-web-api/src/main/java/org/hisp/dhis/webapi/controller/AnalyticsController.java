package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.analytics.*;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Set;

import static org.hisp.dhis.common.DimensionalObjectUtils.getItemsFromParam;

/**
 * @author Lars Helge Overland
 */
@Controller
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class AnalyticsController
{
    private static final String RESOURCE_PATH = "/analytics";
    private static final String DATA_VALUE_SET_PATH = "/dataValueSet";
    private static final String RAW_DATA_PATH = "/rawData";

    @Autowired
    private DataQueryService dataQueryService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ContextUtils contextUtils;

    // -------------------------------------------------------------------------
    // Resources
    // -------------------------------------------------------------------------

    @RequestMapping( value = RESOURCE_PATH, method = RequestMethod.GET, produces = { "application/json", "application/javascript" } )
    public @ResponseBody Grid getJson( // JSON, JSONP
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) AggregationType aggregationType,
        @RequestParam( required = false ) String measureCriteria,
        @RequestParam( required = false ) String preAggregationMeasureCriteria,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean skipRounding,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean ignoreLimit,
        @RequestParam( required = false ) boolean hideEmptyRows,
        @RequestParam( required = false ) boolean hideEmptyColumns,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) boolean includeNumDen,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) IdScheme outputIdScheme,
        @RequestParam( required = false ) IdScheme inputIdScheme,
        @RequestParam( required = false ) String approvalLevel,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String columns,
        @RequestParam( required = false ) String rows,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {

        DataQueryParams params = dataQueryService.getFromUrl( dimension, filter, aggregationType, measureCriteria, preAggregationMeasureCriteria, startDate, endDate, skipMeta, skipData, skipRounding, completedOnly, hierarchyMeta,
            ignoreLimit, hideEmptyRows, hideEmptyColumns, showHierarchy, includeNumDen, displayProperty, outputIdScheme, inputIdScheme, false, approvalLevel, relativePeriodDate, userOrgUnit, false, apiVersion );

        contextUtils.configureAnalyticsResponse( response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.RESPECT_SYSTEM_SETTING, null, false, params.getLatestEndDate() );
        return analyticsService.getAggregatedDataValues( params, getItemsFromParam( columns ), getItemsFromParam( rows ) );
    }

    @RequestMapping( value = RESOURCE_PATH + ".xml", method = RequestMethod.GET )
    public void getXml(
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) AggregationType aggregationType,
        @RequestParam( required = false ) String measureCriteria,
        @RequestParam( required = false ) String preAggregationMeasureCriteria,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean skipRounding,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean ignoreLimit,
        @RequestParam( required = false ) boolean hideEmptyRows,
        @RequestParam( required = false ) boolean hideEmptyColumns,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) boolean includeNumDen,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) IdScheme outputIdScheme,
        @RequestParam( required = false ) IdScheme inputIdScheme,
        @RequestParam( required = false ) String approvalLevel,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String columns,
        @RequestParam( required = false ) String rows,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        DataQueryParams params = dataQueryService.getFromUrl( dimension, filter, aggregationType, measureCriteria, preAggregationMeasureCriteria, startDate, endDate, skipMeta, skipData, skipRounding, completedOnly, hierarchyMeta,
            ignoreLimit, hideEmptyRows, hideEmptyColumns, showHierarchy, includeNumDen, displayProperty, outputIdScheme, inputIdScheme, false, approvalLevel, relativePeriodDate, userOrgUnit, false, apiVersion );

        contextUtils.configureAnalyticsResponse( response, ContextUtils.CONTENT_TYPE_XML, CacheStrategy.RESPECT_SYSTEM_SETTING, null, false, params.getLatestEndDate() );
        Grid grid = analyticsService.getAggregatedDataValues( params, getItemsFromParam( columns ), getItemsFromParam( rows ) );
        GridUtils.toXml( grid, response.getOutputStream() );
    }

    @RequestMapping( value = RESOURCE_PATH + ".html", method = RequestMethod.GET )
    public void getHtml(
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) AggregationType aggregationType,
        @RequestParam( required = false ) String measureCriteria,
        @RequestParam( required = false ) String preAggregationMeasureCriteria,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean skipRounding,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean ignoreLimit,
        @RequestParam( required = false ) boolean hideEmptyRows,
        @RequestParam( required = false ) boolean hideEmptyColumns,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) boolean includeNumDen,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) IdScheme outputIdScheme,
        @RequestParam( required = false ) IdScheme inputIdScheme,
        @RequestParam( required = false ) String approvalLevel,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String columns,
        @RequestParam( required = false ) String rows,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        DataQueryParams params = dataQueryService.getFromUrl( dimension, filter, aggregationType, measureCriteria, preAggregationMeasureCriteria, startDate, endDate, skipMeta, skipData, skipRounding, completedOnly, hierarchyMeta,
            ignoreLimit, hideEmptyRows, hideEmptyColumns, showHierarchy, includeNumDen, displayProperty, outputIdScheme, inputIdScheme, false, approvalLevel, relativePeriodDate, userOrgUnit, false, apiVersion );

        contextUtils.configureAnalyticsResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING, null, false, params.getLatestEndDate() );
        Grid grid = analyticsService.getAggregatedDataValues( params, getItemsFromParam( columns ), getItemsFromParam( rows ) );
        GridUtils.toHtml( grid, response.getWriter() );
    }

    @RequestMapping( value = RESOURCE_PATH + ".html+css", method = RequestMethod.GET )
    public void getHtmlCss(
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) AggregationType aggregationType,
        @RequestParam( required = false ) String measureCriteria,
        @RequestParam( required = false ) String preAggregationMeasureCriteria,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean skipRounding,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean ignoreLimit,
        @RequestParam( required = false ) boolean hideEmptyRows,
        @RequestParam( required = false ) boolean hideEmptyColumns,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) boolean includeNumDen,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) IdScheme outputIdScheme,
        @RequestParam( required = false ) IdScheme inputIdScheme,
        @RequestParam( required = false ) String approvalLevel,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String columns,
        @RequestParam( required = false ) String rows,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        DataQueryParams params = dataQueryService.getFromUrl( dimension, filter, aggregationType, measureCriteria, preAggregationMeasureCriteria, startDate, endDate, skipMeta, skipData, skipRounding, completedOnly, hierarchyMeta,
            ignoreLimit, hideEmptyRows, hideEmptyColumns, showHierarchy, includeNumDen, displayProperty, outputIdScheme, inputIdScheme, false, approvalLevel, relativePeriodDate, userOrgUnit, false, apiVersion );

        contextUtils.configureAnalyticsResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING, null, false, params.getLatestEndDate() );
        Grid grid = analyticsService.getAggregatedDataValues( params, getItemsFromParam( columns ), getItemsFromParam( rows ) );
        GridUtils.toHtmlCss( grid, response.getWriter() );
    }

    @RequestMapping( value = RESOURCE_PATH + ".csv", method = RequestMethod.GET )
    public void getCsv(
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) AggregationType aggregationType,
        @RequestParam( required = false ) String measureCriteria,
        @RequestParam( required = false ) String preAggregationMeasureCriteria,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean skipRounding,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean ignoreLimit,
        @RequestParam( required = false ) boolean hideEmptyRows,
        @RequestParam( required = false ) boolean hideEmptyColumns,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) boolean includeNumDen,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) IdScheme outputIdScheme,
        @RequestParam( required = false ) IdScheme inputIdScheme,
        @RequestParam( required = false ) String approvalLevel,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String columns,
        @RequestParam( required = false ) String rows,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        DataQueryParams params = dataQueryService.getFromUrl( dimension, filter, aggregationType, measureCriteria, preAggregationMeasureCriteria, startDate, endDate, skipMeta, skipData, skipRounding, completedOnly, hierarchyMeta,
            ignoreLimit, hideEmptyRows, hideEmptyColumns, showHierarchy, includeNumDen, displayProperty, outputIdScheme, inputIdScheme, false, approvalLevel, relativePeriodDate, userOrgUnit, false, apiVersion );

        contextUtils.configureAnalyticsResponse( response, ContextUtils.CONTENT_TYPE_CSV, CacheStrategy.RESPECT_SYSTEM_SETTING, "data.csv", true, params.getLatestEndDate() );
        Grid grid = analyticsService.getAggregatedDataValues( params, getItemsFromParam( columns ), getItemsFromParam( rows ) );
        GridUtils.toCsv( grid, response.getWriter() );
    }

    @RequestMapping( value = RESOURCE_PATH + ".xls", method = RequestMethod.GET )
    public void getXls(
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) AggregationType aggregationType,
        @RequestParam( required = false ) String measureCriteria,
        @RequestParam( required = false ) String preAggregationMeasureCriteria,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean skipRounding,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean ignoreLimit,
        @RequestParam( required = false ) boolean hideEmptyRows,
        @RequestParam( required = false ) boolean hideEmptyColumns,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) boolean includeNumDen,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) IdScheme outputIdScheme,
        @RequestParam( required = false ) IdScheme inputIdScheme,
        @RequestParam( required = false ) String approvalLevel,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String columns,
        @RequestParam( required = false ) String rows,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        DataQueryParams params = dataQueryService.getFromUrl( dimension, filter, aggregationType, measureCriteria, preAggregationMeasureCriteria, startDate, endDate, skipMeta, skipData, skipRounding, completedOnly, hierarchyMeta,
            ignoreLimit, hideEmptyRows, hideEmptyColumns, showHierarchy, includeNumDen, displayProperty, outputIdScheme, inputIdScheme, false, approvalLevel, relativePeriodDate, userOrgUnit, false, apiVersion );

        contextUtils.configureAnalyticsResponse( response, ContextUtils.CONTENT_TYPE_EXCEL, CacheStrategy.RESPECT_SYSTEM_SETTING, "data.xls", true, params.getLatestEndDate() );
        Grid grid = analyticsService.getAggregatedDataValues( params, getItemsFromParam( columns ), getItemsFromParam( rows ) );
        GridUtils.toXls( grid, response.getOutputStream() );
    }

    @RequestMapping( value = RESOURCE_PATH + ".jrxml", method = RequestMethod.GET )
    public void getJrxml(
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) AggregationType aggregationType,
        @RequestParam( required = false ) String measureCriteria,
        @RequestParam( required = false ) String preAggregationMeasureCriteria,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean skipRounding,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean ignoreLimit,
        @RequestParam( required = false ) boolean hideEmptyRows,
        @RequestParam( required = false ) boolean hideEmptyColumns,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) boolean includeNumDen,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) IdScheme outputIdScheme,
        @RequestParam( required = false ) IdScheme inputIdScheme,
        @RequestParam( required = false ) OutputFormat outputFormat,
        @RequestParam( required = false ) Integer approvalLevel,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String columns,
        @RequestParam( required = false ) String rows,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        DataQueryParams params = dataQueryService.getFromUrl( dimension, filter, null, null, null, startDate, endDate, true, false, false, false, false, 
            false, false, false, false, false, null, null, null, false, null, null, null, false, apiVersion );

        contextUtils.configureAnalyticsResponse( response, ContextUtils.CONTENT_TYPE_XML, CacheStrategy.RESPECT_SYSTEM_SETTING, "data.jrxml", false, params.getLatestEndDate() );
        Grid grid = analyticsService.getAggregatedDataValues( params );

        GridUtils.toJrxml( grid, null, response.getWriter() );
    }

    @RequestMapping( value = RESOURCE_PATH + "/debug/sql", method = RequestMethod.GET, produces = { "text/html", "text/plain" } )
    public @ResponseBody String getDebugSql(
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) AggregationType aggregationType,
        @RequestParam( required = false ) String measureCriteria,
        @RequestParam( required = false ) String preAggregationMeasureCriteria,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean skipRounding,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean ignoreLimit,
        @RequestParam( required = false ) boolean hideEmptyRows,
        @RequestParam( required = false ) boolean hideEmptyColumns,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) boolean includeNumDen,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) IdScheme outputIdScheme,
        @RequestParam( required = false ) IdScheme inputIdScheme,
        @RequestParam( required = false ) String approvalLevel,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String columns,
        @RequestParam( required = false ) String rows,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        DataQueryParams params = dataQueryService.getFromUrl( dimension, filter, aggregationType, measureCriteria, preAggregationMeasureCriteria, startDate, endDate, skipMeta, skipData, skipRounding, completedOnly, hierarchyMeta,
            ignoreLimit, hideEmptyRows, hideEmptyColumns, showHierarchy, includeNumDen, displayProperty, outputIdScheme, inputIdScheme, false, approvalLevel, relativePeriodDate, userOrgUnit, false, apiVersion );

        contextUtils.configureAnalyticsResponse( response, ContextUtils.CONTENT_TYPE_TEXT, CacheStrategy.NO_CACHE, "debug.sql", false, params.getLatestEndDate() );
        return AnalyticsUtils.getDebugDataSql( params );
    }

    // -------------------------------------------------------------------------
    // Raw data
    // -------------------------------------------------------------------------

    @RequestMapping( value = RESOURCE_PATH + RAW_DATA_PATH + ".json", method = RequestMethod.GET )
    public @ResponseBody Grid getRawDataJson(
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) IdScheme outputIdScheme,
        @RequestParam( required = false ) IdScheme inputIdScheme,
        @RequestParam( required = false ) String userOrgUnit,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        DataQueryParams params = dataQueryService.getFromUrl( dimension, null, null, null, null, startDate, endDate, skipMeta, skipData, false, false, hierarchyMeta,
            false, false, false, showHierarchy, false, displayProperty, outputIdScheme, inputIdScheme, false, null, null, userOrgUnit, true, apiVersion );

        contextUtils.configureAnalyticsResponse( response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.RESPECT_SYSTEM_SETTING, null, false, params.getLatestEndDate() );
        return analyticsService.getRawDataValues( params );
    }

    @RequestMapping( value = RESOURCE_PATH + RAW_DATA_PATH + ".csv", method = RequestMethod.GET )
    public void getRawDataCsv(
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) IdScheme outputIdScheme,
        @RequestParam( required = false ) IdScheme inputIdScheme,
        @RequestParam( required = false ) String userOrgUnit,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        DataQueryParams params = dataQueryService.getFromUrl( dimension, null, null, null, null, startDate, endDate, skipMeta, skipData, false, false, hierarchyMeta,
            false, false, false, showHierarchy, false, displayProperty, outputIdScheme, inputIdScheme, false, null, null, userOrgUnit, true, apiVersion );

        contextUtils.configureAnalyticsResponse( response, ContextUtils.CONTENT_TYPE_CSV, CacheStrategy.RESPECT_SYSTEM_SETTING, null, false, params.getLatestEndDate() );
        Grid grid = analyticsService.getRawDataValues( params );
        GridUtils.toCsv( grid, response.getWriter() );
    }

    // -------------------------------------------------------------------------
    // Data value set
    // -------------------------------------------------------------------------

    @RequestMapping( value = RESOURCE_PATH + DATA_VALUE_SET_PATH + ".xml", method = RequestMethod.GET )
    public @ResponseBody DataValueSet getDataValueSetXml(
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) AggregationType aggregationType,
        @RequestParam( required = false ) String measureCriteria,
        @RequestParam( required = false ) String preAggregationMeasureCriteria,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean skipRounding,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean ignoreLimit,
        @RequestParam( required = false ) boolean hideEmptyRows,
        @RequestParam( required = false ) boolean hideEmptyColumns,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) boolean includeNumDen,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) IdScheme outputIdScheme,
        @RequestParam( required = false ) IdScheme inputIdScheme,
        @RequestParam( required = false ) boolean duplicatesOnly,
        @RequestParam( required = false ) String approvalLevel,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String columns,
        @RequestParam( required = false ) String rows,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        DataQueryParams params = dataQueryService.getFromUrl( dimension, filter, aggregationType, measureCriteria, preAggregationMeasureCriteria, startDate, endDate, skipMeta, skipData, skipRounding, completedOnly, hierarchyMeta,
            ignoreLimit, hideEmptyRows, hideEmptyColumns, showHierarchy, includeNumDen, displayProperty, outputIdScheme, inputIdScheme, duplicatesOnly, approvalLevel, relativePeriodDate, userOrgUnit, false, apiVersion );

        contextUtils.configureAnalyticsResponse( response, ContextUtils.CONTENT_TYPE_XML, CacheStrategy.RESPECT_SYSTEM_SETTING, null, false, params.getLatestEndDate() );
        return analyticsService.getAggregatedDataValueSet( params );
    }

    @RequestMapping( value = RESOURCE_PATH + DATA_VALUE_SET_PATH + ".json", method = RequestMethod.GET )
    public @ResponseBody DataValueSet getDataValueSetJson(
        @RequestParam Set<String> dimension,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) AggregationType aggregationType,
        @RequestParam( required = false ) String measureCriteria,
        @RequestParam( required = false ) String preAggregationMeasureCriteria,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) boolean skipData,
        @RequestParam( required = false ) boolean skipRounding,
        @RequestParam( required = false ) boolean completedOnly,
        @RequestParam( required = false ) boolean hierarchyMeta,
        @RequestParam( required = false ) boolean ignoreLimit,
        @RequestParam( required = false ) boolean hideEmptyRows,
        @RequestParam( required = false ) boolean hideEmptyColumns,
        @RequestParam( required = false ) boolean showHierarchy,
        @RequestParam( required = false ) boolean includeNumDen,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) IdScheme outputIdScheme,
        @RequestParam( required = false ) IdScheme inputIdScheme,
        @RequestParam( required = false ) boolean duplicatesOnly,
        @RequestParam( required = false ) String approvalLevel,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( required = false ) String columns,
        @RequestParam( required = false ) String rows,
        DhisApiVersion apiVersion,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        DataQueryParams params = dataQueryService.getFromUrl( dimension, filter, aggregationType, measureCriteria, preAggregationMeasureCriteria, startDate, endDate, skipMeta, skipData, skipRounding, completedOnly, hierarchyMeta,
            ignoreLimit, hideEmptyRows, hideEmptyColumns, showHierarchy, includeNumDen, displayProperty, outputIdScheme, inputIdScheme, duplicatesOnly, approvalLevel, relativePeriodDate, userOrgUnit, false, apiVersion );

        contextUtils.configureAnalyticsResponse( response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.RESPECT_SYSTEM_SETTING, null, false, params.getLatestEndDate() );
        return analyticsService.getAggregatedDataValueSet( params );
    }
}
