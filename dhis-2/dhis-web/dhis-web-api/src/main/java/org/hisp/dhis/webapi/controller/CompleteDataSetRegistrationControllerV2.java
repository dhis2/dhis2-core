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
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.dataset.DefaultCompleteDataSetRegistrationExchangeExchangeService;
import org.hisp.dhis.dxf2.dataset.ExportParams;
import org.hisp.dhis.dxf2.dataset.tasks.ImportCompleteDataSetRegistrationsTask;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.TaskCategory;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.nio.file.Path;
import java.util.Date;
import java.util.Set;

import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML;

/**
 * @author Halvdan Hoem Grelland
 */
@Controller
@RequestMapping( value = CompleteDataSetRegistrationControllerV2.RESOURCE_PATH )
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.ALL } )
public class CompleteDataSetRegistrationControllerV2
{
    public static final String RESOURCE_PATH = "completeDataSetRegistrationsV2";

    @Autowired
    private DefaultCompleteDataSetRegistrationExchangeExchangeService registrationService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private Scheduler scheduler;

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.GET, produces = CONTENT_TYPE_XML )
    public void getCompleteRegistrationsXml(
        @RequestParam Set<String> dataSet,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false, name = "children" ) boolean includeChildren,
        @RequestParam( required = false ) Set<String> orgUnit,
        @RequestParam( required = false ) Set<String> orgUnitGroup,
        @RequestParam( required = false ) Date created,
        @RequestParam( required = false ) String createdDuration,
        @RequestParam( required = false ) Integer limit,
        IdSchemes idSchemes,
        HttpServletRequest request,
        HttpServletResponse response
    )
        throws IOException
    {
        response.setContentType( CONTENT_TYPE_XML );

        ExportParams params = registrationService.paramsFromUrl(
            dataSet, orgUnit, orgUnitGroup, period, startDate, endDate, includeChildren, created, createdDuration, limit, idSchemes );

        registrationService.writeCompleteDataSetRegistrationsXml( params, response.getOutputStream() );
    }

    @RequestMapping( method = RequestMethod.GET, produces = CONTENT_TYPE_JSON )
    public void getCompleteRegistrationsJson(
        @RequestParam Set<String> dataSet,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false, name = "children" ) boolean includeChildren,
        @RequestParam( required = false ) Set<String> orgUnit,
        @RequestParam( required = false ) Set<String> orgUnitGroup,
        @RequestParam( required = false ) Date created,
        @RequestParam( required = false ) String createdDuration,
        @RequestParam( required = false ) Integer limit,
        IdSchemes idSchemes,
        HttpServletRequest request,
        HttpServletResponse response
    )
        throws IOException
    {
        response.setContentType( CONTENT_TYPE_JSON );

        ExportParams params = registrationService.paramsFromUrl(
            dataSet, orgUnit, orgUnitGroup, period, startDate, endDate, includeChildren, created, createdDuration, limit, idSchemes );

        registrationService.writeCompleteDataSetRegistrationsJson( params, response.getOutputStream() );
    }

    // -------------------------------------------------------------------------
    // POST
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.POST, consumes = CONTENT_TYPE_XML )
    public void postCompleteRegistrationsXml(
        ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response
    )
        throws IOException
    {
        if ( importOptions.isAsync() )
        {
            asyncImport( importOptions, ImportCompleteDataSetRegistrationsTask.FORMAT_XML, request, response );
        }
        else
        {
            response.setContentType( CONTENT_TYPE_XML );
            ImportSummary summary = registrationService.saveCompleteDataSetRegistrationsXml( request.getInputStream(), importOptions );
            renderService.toXml( response.getOutputStream(), summary );
        }
    }

    @RequestMapping( method = RequestMethod.POST, consumes = CONTENT_TYPE_JSON )
    public void postCompleteRegistrationsJson(
        ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response
    )
        throws IOException
    {
        if ( importOptions.isAsync() )
        {
            asyncImport( importOptions, ImportCompleteDataSetRegistrationsTask.FORMAT_JSON, request, response );
        }
        else
        {
            response.setContentType( CONTENT_TYPE_JSON );
            ImportSummary summary = registrationService.saveCompleteDataSetRegistrationsJson( request.getInputStream(), importOptions );
            renderService.toJson( response.getOutputStream(), summary );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void asyncImport( ImportOptions importOptions, String format, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        Pair<InputStream, Path> tmpFile = saveTmpFile( request.getInputStream() );

        TaskId taskId = new TaskId( TaskCategory.COMPLETE_DATA_SET_REGISTRATION_IMPORT, currentUserService.getCurrentUser() );

        scheduler.executeTask(
            new ImportCompleteDataSetRegistrationsTask(
                registrationService, tmpFile.getLeft(), tmpFile.getRight(), importOptions, format, taskId )
        );

        response.setHeader(
            "Location", ContextUtils.getRootPath( request) + "/system/tasks/" + TaskCategory.COMPLETE_DATA_SET_REGISTRATION_IMPORT );
        response.setStatus( HttpServletResponse.SC_ACCEPTED );
    }

    private Pair<InputStream, Path> saveTmpFile( InputStream in )
        throws IOException
    {
        String filename = RandomStringUtils.randomAlphanumeric( 6 );

        File tmpFile = File.createTempFile( filename, null );
        tmpFile.deleteOnExit();

        try ( FileOutputStream out = new FileOutputStream( tmpFile ) )
        {
            IOUtils.copy( in, out );
        }

        return Pair.of( new BufferedInputStream( new FileInputStream( tmpFile ) ), tmpFile.toPath() );
    }
}
