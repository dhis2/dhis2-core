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

import org.apache.commons.io.IOUtils;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.dxf2.adx.AdxDataService;
import org.hisp.dhis.dxf2.adx.AdxException;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataExportParams;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.datavalueset.tasks.ImportDataValueTask;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.TaskCategory;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.ALL } )
public class DataValueSetController
{
    public static final String RESOURCE_PATH = "/dataValueSets";

    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private AdxDataService adxDataService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private SessionFactory sessionFactory;

    // -------------------------------------------------------------------------
    // Get
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.GET, produces = CONTENT_TYPE_XML )
    public void getDataValueSetXml(
        @RequestParam( required = false ) Set<String> dataSet,
        @RequestParam( required = false ) Set<String> dataElementGroup,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> orgUnit,
        @RequestParam( required = false ) boolean children,
        @RequestParam( required = false ) Date lastUpdated,
        @RequestParam( required = false ) String lastUpdatedDuration,
        @RequestParam( required = false ) Integer limit,
        IdSchemes idSchemes, HttpServletResponse response ) throws IOException
    {
        response.setContentType( CONTENT_TYPE_XML );

        DataExportParams params = dataValueSetService.getFromUrl( dataSet, dataElementGroup,
            period, startDate, endDate, orgUnit, children, lastUpdated, lastUpdatedDuration, limit, idSchemes );

        dataValueSetService.writeDataValueSetXml( params, response.getOutputStream() );
    }

    @RequestMapping( method = RequestMethod.GET, produces = CONTENT_TYPE_XML_ADX )
    public void getDataValueSetXmlAdx(
        @RequestParam Set<String> dataSet,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> orgUnit,
        @RequestParam( required = false ) boolean children,
        @RequestParam( required = false ) Date lastUpdated,
        @RequestParam( required = false ) Integer limit,
        IdSchemes idSchemes, HttpServletResponse response ) throws IOException, AdxException
    {
        response.setContentType( CONTENT_TYPE_XML_ADX );

        DataExportParams params = adxDataService.getFromUrl( dataSet, period,
            startDate, endDate, orgUnit, children, lastUpdated, limit, idSchemes );

        adxDataService.writeDataValueSet( params, response.getOutputStream() );
    }

    @RequestMapping( method = RequestMethod.GET, produces = CONTENT_TYPE_JSON )
    public void getDataValueSetJson(
        @RequestParam( required = false ) Set<String> dataSet,
        @RequestParam( required = false ) Set<String> dataElementGroup,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> orgUnit,
        @RequestParam( required = false ) boolean children,
        @RequestParam( required = false ) Date lastUpdated,
        @RequestParam( required = false ) String lastUpdatedDuration,
        @RequestParam( required = false ) Integer limit,
        IdSchemes idSchemes, HttpServletResponse response ) throws IOException
    {
        response.setContentType( CONTENT_TYPE_JSON );

        DataExportParams params = dataValueSetService.getFromUrl( dataSet, dataElementGroup,
            period, startDate, endDate, orgUnit, children, lastUpdated, lastUpdatedDuration, limit, idSchemes );

        dataValueSetService.writeDataValueSetJson( params, response.getOutputStream() );
    }

    @RequestMapping( method = RequestMethod.GET, produces = CONTENT_TYPE_CSV )
    public void getDataValueSetCsv(
        @RequestParam( required = false ) Set<String> dataSet,
        @RequestParam( required = false ) Set<String> dataElementGroup,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam Set<String> orgUnit,
        @RequestParam( required = false ) boolean children,
        @RequestParam( required = false ) Date lastUpdated,
        @RequestParam( required = false ) String lastUpdatedDuration,
        @RequestParam( required = false ) Integer limit,
        IdSchemes idSchemes,
        HttpServletResponse response ) throws IOException
    {
        response.setContentType( CONTENT_TYPE_CSV );

        DataExportParams params = dataValueSetService.getFromUrl( dataSet, dataElementGroup,
            period, startDate, endDate, orgUnit, children, lastUpdated, lastUpdatedDuration, limit, idSchemes );

        dataValueSetService.writeDataValueSetCsv( params, response.getWriter() );
    }

    // -------------------------------------------------------------------------
    // Post
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.POST, consumes = "application/xml" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    public void postDxf2DataValueSet( ImportOptions importOptions,
        HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        if ( importOptions.isAsync() )
        {
            startAsyncImport( importOptions, ImportDataValueTask.FORMAT_XML, request, response );
        }
        else
        {
            ImportSummary summary = dataValueSetService.saveDataValueSet( request.getInputStream(), importOptions );

            response.setContentType( CONTENT_TYPE_XML );
            renderService.toXml( response.getOutputStream(), summary );
        }
    }

    @RequestMapping( method = RequestMethod.POST, consumes = CONTENT_TYPE_XML_ADX )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    public void postAdxDataValueSet( ImportOptions importOptions,
        HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        if ( importOptions.isAsync() )
        {
            startAsyncImport( importOptions, ImportDataValueTask.FORMAT_ADX, request, response );
        }
        else
        {
            ImportSummaries summaries = adxDataService.saveDataValueSet( request.getInputStream(), importOptions, null );

            response.setContentType( CONTENT_TYPE_XML );
            renderService.toXml( response.getOutputStream(), summaries );
        }
    }

    @RequestMapping( method = RequestMethod.POST, consumes = "application/json" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    public void postJsonDataValueSet( ImportOptions importOptions,
        HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        if ( importOptions.isAsync() )
        {
            startAsyncImport( importOptions, ImportDataValueTask.FORMAT_JSON, request, response );
        }
        else
        {
            ImportSummary summary = dataValueSetService.saveDataValueSetJson( request.getInputStream(), importOptions );

            response.setContentType( CONTENT_TYPE_JSON );
            renderService.toJson( response.getOutputStream(), summary );
        }
    }

    @RequestMapping( method = RequestMethod.POST, consumes = "application/csv" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    public void postCsvDataValueSet( ImportOptions importOptions,
        HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        if ( importOptions.isAsync() )
        {
            startAsyncImport( importOptions, ImportDataValueTask.FORMAT_CSV, request, response );
        }
        else
        {
            ImportSummary summary = dataValueSetService.saveDataValueSetCsv( request.getInputStream(), importOptions );

            response.setContentType( CONTENT_TYPE_XML );
            renderService.toXml( response.getOutputStream(), summary );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Starts an asynchronous import task.
     *
     * @param importOptions the ImportOptions.
     * @param format        the resource representation format.
     * @param request       the HttpRequest.
     * @param response      the HttpResponse.
     * @throws IOException
     */
    private void startAsyncImport( ImportOptions importOptions, String format, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        InputStream inputStream = saveTmp( request.getInputStream() );

        TaskId taskId = new TaskId( TaskCategory.DATAVALUE_IMPORT, currentUserService.getCurrentUser() );
        scheduler.executeTask( new ImportDataValueTask( dataValueSetService, adxDataService, sessionFactory, inputStream, importOptions, taskId, format ) );

        response.setHeader( "Location", ContextUtils.getRootPath( request ) + "/system/tasks/" + TaskCategory.DATAVALUE_IMPORT );
        response.setStatus( HttpServletResponse.SC_ACCEPTED );
    }

    /**
     * Writes the input stream to a temporary file, and returns a new input
     * stream connected to the file.
     *
     * @param in the InputStream.
     * @return an InputStream.
     * @throws IOException
     */
    private InputStream saveTmp( InputStream in )
        throws IOException
    {
        File tmpFile = File.createTempFile( "dvs", null );

        tmpFile.deleteOnExit();

        try ( FileOutputStream out = new FileOutputStream( tmpFile ) )
        {
            IOUtils.copy( in, out );
        }

        return new BufferedInputStream( new FileInputStream( tmpFile ) );
    }
}
