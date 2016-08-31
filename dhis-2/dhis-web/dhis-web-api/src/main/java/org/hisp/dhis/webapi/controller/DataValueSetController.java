package org.hisp.dhis.webapi.controller;

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

import org.hisp.dhis.dxf2.common.IdSchemes;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataExportParams;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.render.RenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Set;

import static org.hisp.dhis.webapi.utils.ContextUtils.*;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = DataValueSetController.RESOURCE_PATH )
public class DataValueSetController
{
    public static final String RESOURCE_PATH = "/dataValueSets";

    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private RenderService renderService;

    // -------------------------------------------------------------------------
    // Get
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.GET, produces = CONTENT_TYPE_XML )
    public void getDataValueSetXml(
        @RequestParam Set<String> dataSet,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> orgUnit,
        @RequestParam( required = false ) boolean children,
        @RequestParam( required = false ) Date lastUpdated,
        @RequestParam( required = false ) Integer limit,
        IdSchemes idSchemes, HttpServletResponse response ) throws IOException
    {
        response.setContentType( CONTENT_TYPE_XML );

        DataExportParams params = dataValueSetService.getFromUrl( dataSet, period,
            startDate, endDate, orgUnit, children, lastUpdated, limit, idSchemes );

        dataValueSetService.writeDataValueSetXml( params, response.getOutputStream() );
    }

    @RequestMapping( method = RequestMethod.GET, produces = CONTENT_TYPE_JSON )
    public void getDataValueSetJson(
        @RequestParam Set<String> dataSet,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> orgUnit,
        @RequestParam( required = false ) boolean children,
        @RequestParam( required = false ) Date lastUpdated,
        @RequestParam( required = false ) Integer limit,
        IdSchemes idSchemes, HttpServletResponse response ) throws IOException
    {
        response.setContentType( CONTENT_TYPE_JSON );

        DataExportParams params = dataValueSetService.getFromUrl( dataSet, period,
            startDate, endDate, orgUnit, children, lastUpdated, limit, idSchemes );

        dataValueSetService.writeDataValueSetJson( params, response.getOutputStream() );
    }

    @RequestMapping( method = RequestMethod.GET, produces = CONTENT_TYPE_CSV )
    public void getDataValueSetCsv(
        @RequestParam Set<String> dataSet,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> orgUnit,
        @RequestParam( required = false ) boolean children,
        @RequestParam( required = false ) Date lastUpdated,
        @RequestParam( required = false ) Integer limit,
        IdSchemes idSchemes,
        HttpServletResponse response ) throws IOException
    {
        response.setContentType( CONTENT_TYPE_CSV );

        DataExportParams params = dataValueSetService.getFromUrl( dataSet, period,
            startDate, endDate, orgUnit, children, lastUpdated, limit, idSchemes );

        dataValueSetService.writeDataValueSetCsv( params, response.getWriter() );
    }

    // -------------------------------------------------------------------------
    // Post
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.POST, consumes = "application/xml" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    public void postDxf2DataValueSet( ImportOptions importOptions,
        HttpServletResponse response, InputStream in, Model model ) throws IOException
    {
        ImportSummary summary = dataValueSetService.saveDataValueSet( in, importOptions );

        response.setContentType( CONTENT_TYPE_XML );
        renderService.toXml( response.getOutputStream(), summary );
    }

    @RequestMapping( method = RequestMethod.POST, consumes = "application/json" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    public void postJsonDataValueSet( ImportOptions importOptions,
        HttpServletResponse response, InputStream in, Model model ) throws IOException
    {
        ImportSummary summary = dataValueSetService.saveDataValueSetJson( in, importOptions );

        response.setContentType( CONTENT_TYPE_JSON );
        renderService.toJson( response.getOutputStream(), summary );
    }

    @RequestMapping( method = RequestMethod.POST, consumes = "application/csv" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    public void postCsvDataValueSet( ImportOptions importOptions,
        HttpServletResponse response, InputStream in, Model model ) throws IOException
    {
        ImportSummary summary = dataValueSetService.saveDataValueSetCsv( in, importOptions );

        response.setContentType( CONTENT_TYPE_XML );
        renderService.toXml( response.getOutputStream(), summary );
    }
}
