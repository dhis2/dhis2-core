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

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.analytics.orgunit.OrgUnitAnalyticsService;
import org.hisp.dhis.analytics.orgunit.OrgUnitQueryParams;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Lars Helge Overland
 */
@Controller
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class OrgUnitAnalyticsController
{
    private static final String RESOURCE_PATH = "/orgUnitAnalytics";

    @Autowired
    private OrgUnitAnalyticsService analyticsService;

    @Autowired
    private ContextUtils contextUtils;

    @RequestMapping( value = RESOURCE_PATH, method = RequestMethod.GET, produces = { "application/json" } )
    public @ResponseBody Grid getJson(
        @RequestParam String ou,
        @RequestParam String ougs,
        @RequestParam( required = false ) String columns,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
        throws Exception
    {
        OrgUnitQueryParams params = analyticsService.getParams( ou, ougs, columns );
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON,
            CacheStrategy.RESPECT_SYSTEM_SETTING );
        return analyticsService.getOrgUnitData( params );
    }

    @RequestMapping( value = RESOURCE_PATH + ".xls", method = RequestMethod.GET )
    public void getXls(
        @RequestParam String ou,
        @RequestParam String ougs,
        @RequestParam( required = false ) String columns,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
        throws Exception
    {
        OrgUnitQueryParams params = analyticsService.getParams( ou, ougs, columns );
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_EXCEL,
            CacheStrategy.RESPECT_SYSTEM_SETTING );
        Grid grid = analyticsService.getOrgUnitData( params );
        GridUtils.toXls( grid, response.getOutputStream() );
    }

    @RequestMapping( value = RESOURCE_PATH + ".csv", method = RequestMethod.GET )
    public void getCsv(
        @RequestParam String ou,
        @RequestParam String ougs,
        @RequestParam( required = false ) String columns,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
        throws Exception
    {
        OrgUnitQueryParams params = analyticsService.getParams( ou, ougs, columns );
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_CSV, CacheStrategy.RESPECT_SYSTEM_SETTING );
        Grid grid = analyticsService.getOrgUnitData( params );
        GridUtils.toCsv( grid, response.getWriter() );
    }

    @RequestMapping( value = RESOURCE_PATH + ".pdf", method = RequestMethod.GET )
    public void getPdf(
        @RequestParam String ou,
        @RequestParam String ougs,
        @RequestParam( required = false ) String columns,
        DhisApiVersion apiVersion,
        HttpServletResponse response )
        throws Exception
    {
        OrgUnitQueryParams params = analyticsService.getParams( ou, ougs, columns );
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PDF, CacheStrategy.RESPECT_SYSTEM_SETTING );
        Grid grid = analyticsService.getOrgUnitData( params );
        GridUtils.toPdf( grid, response.getOutputStream() );
    }
}
