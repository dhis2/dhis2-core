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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.render.RenderFormat.CSV;
import static org.hisp.dhis.render.RenderFormat.XML;
import static org.hisp.dhis.scheduling.JobType.DATAVALUE_IMPORT;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_PDF;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML_ADX;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.Compression;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.dxf2.adx.AdxDataService;
import org.hisp.dhis.dxf2.adx.AdxException;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.datavalueset.tasks.ImportDataValueTask;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = DataValueSetController.RESOURCE_PATH )
@Slf4j
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
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
    private SchedulingManager schedulingManager;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private WebMessageService webMessageService;

    // -------------------------------------------------------------------------
    // Get
    // -------------------------------------------------------------------------

    @GetMapping( params = { "format", "compression", "attachment" } )
    public void getDataValueSet(
        @RequestParam( required = false ) Set<String> dataSet,
        @RequestParam( required = false ) Set<String> dataElementGroup,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) Set<String> orgUnit,
        @RequestParam( required = false ) boolean children,
        @RequestParam( required = false ) Set<String> orgUnitGroup,
        @RequestParam( required = false ) Set<String> attributeOptionCombo,
        @RequestParam( required = false ) boolean includeDeleted,
        @RequestParam( required = false ) Date lastUpdated,
        @RequestParam( required = false ) String lastUpdatedDuration,
        @RequestParam( required = false ) Integer limit,
        @RequestParam( required = false ) String attachment,
        @RequestParam( required = false ) String compression,
        @RequestParam( required = false ) String format,
        IdSchemes idSchemes, HttpServletResponse response )
        throws IOException
    {
        setNoStore( response );

        DataExportParams params = dataValueSetService.getFromUrl( dataSet, dataElementGroup,
            period, startDate, endDate, orgUnit, children, orgUnitGroup, attributeOptionCombo,
            includeDeleted, lastUpdated, lastUpdatedDuration, limit, idSchemes );

        if ( XML.isEqual( format ) )
        {
            response.setContentType( CONTENT_TYPE_XML );
            OutputStream outputStream = compress( params, response, attachment, Compression.fromValue( compression ),
                "xml" );
            dataValueSetService.writeDataValueSetXml( params, outputStream );
        }
        else if ( CSV.isEqual( format ) )
        {
            response.setContentType( CONTENT_TYPE_CSV );
            OutputStream outputStream = compress( params, response, attachment, Compression.fromValue( compression ),
                "csv" );
            PrintWriter printWriter = new PrintWriter( outputStream );
            dataValueSetService.writeDataValueSetCsv( params, printWriter );
        }
        else
        {
            // default to json
            response.setContentType( CONTENT_TYPE_JSON );
            OutputStream outputStream = compress( params, response, attachment, Compression.fromValue( compression ),
                "json" );
            dataValueSetService.writeDataValueSetJson( params, outputStream );
        }
    }

    @RequestMapping( method = RequestMethod.GET, produces = CONTENT_TYPE_XML )
    public void getDataValueSetXml(
        @RequestParam( required = false ) Set<String> dataSet,
        @RequestParam( required = false ) Set<String> dataElementGroup,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) Set<String> orgUnit,
        @RequestParam( required = false ) boolean children,
        @RequestParam( required = false ) Set<String> orgUnitGroup,
        @RequestParam( required = false ) Set<String> attributeOptionCombo,
        @RequestParam( required = false ) boolean includeDeleted,
        @RequestParam( required = false ) Date lastUpdated,
        @RequestParam( required = false ) String lastUpdatedDuration,
        @RequestParam( required = false ) Integer limit,
        @RequestParam( required = false ) String attachment,
        @RequestParam( required = false ) String compression,
        IdSchemes idSchemes, HttpServletResponse response )
        throws IOException
    {
        response.setContentType( CONTENT_TYPE_XML );
        setNoStore( response );

        DataExportParams params = dataValueSetService.getFromUrl( dataSet, dataElementGroup,
            period, startDate, endDate, orgUnit, children, orgUnitGroup, attributeOptionCombo,
            includeDeleted, lastUpdated, lastUpdatedDuration, limit, idSchemes );

        OutputStream outputStream = compress( params, response, attachment, Compression.fromValue( compression ),
            "xml" );

        dataValueSetService.writeDataValueSetXml( params, outputStream );
    }

    @RequestMapping( method = RequestMethod.GET, produces = CONTENT_TYPE_XML_ADX )
    public void getDataValueSetXmlAdx(
        @RequestParam Set<String> dataSet,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) Set<String> orgUnit,
        @RequestParam( required = false ) boolean children,
        @RequestParam( required = false ) Set<String> orgUnitGroup,
        @RequestParam( required = false ) Set<String> attributeOptionCombo,
        @RequestParam( required = false ) boolean includeDeleted,
        @RequestParam( required = false ) Date lastUpdated,
        @RequestParam( required = false ) String lastUpdatedDuration,
        @RequestParam( required = false ) Integer limit,
        @RequestParam( required = false ) String attachment,
        @RequestParam( required = false ) String compression,
        IdSchemes idSchemes, HttpServletResponse response )
        throws IOException,
        AdxException
    {
        response.setContentType( CONTENT_TYPE_XML_ADX );
        setNoStore( response );

        DataExportParams params = adxDataService.getFromUrl( dataSet,
            period, startDate, endDate, orgUnit, children, orgUnitGroup, attributeOptionCombo,
            includeDeleted, lastUpdated, lastUpdatedDuration, limit, idSchemes );

        OutputStream outputStream = compress( params, response, attachment, Compression.fromValue( compression ),
            "xml" );

        adxDataService.writeDataValueSet( params, outputStream );
    }

    @RequestMapping( method = RequestMethod.GET, produces = CONTENT_TYPE_JSON )
    public void getDataValueSetJson(
        @RequestParam( required = false ) Set<String> dataSet,
        @RequestParam( required = false ) Set<String> dataElementGroup,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) Set<String> orgUnit,
        @RequestParam( required = false ) boolean children,
        @RequestParam( required = false ) Set<String> orgUnitGroup,
        @RequestParam( required = false ) Set<String> attributeOptionCombo,
        @RequestParam( required = false ) boolean includeDeleted,
        @RequestParam( required = false ) Date lastUpdated,
        @RequestParam( required = false ) String lastUpdatedDuration,
        @RequestParam( required = false ) Integer limit,
        @RequestParam( required = false ) String attachment,
        @RequestParam( required = false ) String compression,
        IdSchemes idSchemes, HttpServletResponse response )
        throws IOException
    {
        response.setContentType( CONTENT_TYPE_JSON );
        setNoStore( response );

        DataExportParams params = dataValueSetService.getFromUrl( dataSet, dataElementGroup,
            period, startDate, endDate, orgUnit, children, orgUnitGroup, attributeOptionCombo,
            includeDeleted, lastUpdated, lastUpdatedDuration, limit, idSchemes );

        OutputStream outputStream = compress( params, response, attachment, Compression.fromValue( compression ),
            "json" );

        dataValueSetService.writeDataValueSetJson( params, outputStream );
    }

    @RequestMapping( method = RequestMethod.GET, produces = CONTENT_TYPE_CSV )
    public void getDataValueSetCsv(
        @RequestParam( required = false ) Set<String> dataSet,
        @RequestParam( required = false ) Set<String> dataElementGroup,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) Set<String> orgUnit,
        @RequestParam( required = false ) boolean children,
        @RequestParam( required = false ) Set<String> orgUnitGroup,
        @RequestParam( required = false ) Set<String> attributeOptionCombo,
        @RequestParam( required = false ) boolean includeDeleted,
        @RequestParam( required = false ) Date lastUpdated,
        @RequestParam( required = false ) String lastUpdatedDuration,
        @RequestParam( required = false ) Integer limit,
        @RequestParam( required = false ) String attachment,
        @RequestParam( required = false ) String compression,
        IdSchemes idSchemes,
        HttpServletResponse response )
        throws IOException
    {
        response.setContentType( CONTENT_TYPE_CSV );
        setNoStore( response );

        DataExportParams params = dataValueSetService.getFromUrl( dataSet, dataElementGroup,
            period, startDate, endDate, orgUnit, children, orgUnitGroup, attributeOptionCombo,
            includeDeleted, lastUpdated, lastUpdatedDuration, limit, idSchemes );

        OutputStream outputStream = compress( params, response, attachment, Compression.fromValue( compression ),
            "csv" );

        PrintWriter printWriter = new PrintWriter( outputStream );

        dataValueSetService.writeDataValueSetCsv( params, printWriter );
    }

    // -------------------------------------------------------------------------
    // Post
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.POST, consumes = "application/xml" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    public void postDxf2DataValueSet( ImportOptions importOptions,
        HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        if ( importOptions.isAsync() )
        {
            startAsyncImport( importOptions, ImportDataValueTask.FORMAT_XML, request, response );
        }
        else
        {
            ImportSummary summary = dataValueSetService.saveDataValueSet( request.getInputStream(), importOptions );
            summary.setImportOptions( importOptions );

            response.setContentType( CONTENT_TYPE_XML );
            renderService.toXml( response.getOutputStream(), summary );
        }
    }

    @RequestMapping( method = RequestMethod.POST, consumes = CONTENT_TYPE_XML_ADX )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    public void postAdxDataValueSet( ImportOptions importOptions,
        HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        if ( importOptions.isAsync() )
        {
            startAsyncImport( importOptions, ImportDataValueTask.FORMAT_ADX, request, response );
        }
        else
        {
            try
            {
                ImportSummary summary = adxDataService.saveDataValueSet( request.getInputStream(), importOptions,
                    null );

                summary.setImportOptions( importOptions );

                response.setContentType( CONTENT_TYPE_XML );
                renderService.toXml( response.getOutputStream(), summary );
            }
            catch ( Exception ex )
            {
                log.error( "ADX Import error: ", ex );

                throw ex;
            }
        }
    }

    @RequestMapping( method = RequestMethod.POST, consumes = "application/json" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    public void postJsonDataValueSet( ImportOptions importOptions,
        HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        if ( importOptions.isAsync() )
        {
            startAsyncImport( importOptions, ImportDataValueTask.FORMAT_JSON, request, response );
        }
        else
        {
            ImportSummary summary = dataValueSetService.saveDataValueSetJson( request.getInputStream(), importOptions );
            summary.setImportOptions( importOptions );

            response.setContentType( CONTENT_TYPE_JSON );
            renderService.toJson( response.getOutputStream(), summary );
        }
    }

    @RequestMapping( method = RequestMethod.POST, consumes = "application/csv" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    public void postCsvDataValueSet( ImportOptions importOptions,
        HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        if ( importOptions.isAsync() )
        {
            startAsyncImport( importOptions, ImportDataValueTask.FORMAT_CSV, request, response );
        }
        else
        {
            ImportSummary summary = dataValueSetService.saveDataValueSetCsv( request.getInputStream(), importOptions );
            summary.setImportOptions( importOptions );

            response.setContentType( CONTENT_TYPE_JSON );
            renderService.toJson( response.getOutputStream(), summary );
        }
    }

    @RequestMapping( method = RequestMethod.POST, consumes = CONTENT_TYPE_PDF )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    public void postPdfDataValueSet( ImportOptions importOptions,
        HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        if ( importOptions.isAsync() )
        {
            startAsyncImport( importOptions, ImportDataValueTask.FORMAT_PDF, request, response );
        }
        else
        {
            ImportSummary summary = dataValueSetService.saveDataValueSetPdf( request.getInputStream(), importOptions );
            summary.setImportOptions( importOptions );

            response.setContentType( CONTENT_TYPE_JSON );
            renderService.toJson( response.getOutputStream(), summary );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Starts an asynchronous import task.
     *
     * @param importOptions the ImportOptions.
     * @param format the resource representation format.
     * @param request the HttpRequest.
     * @param response the HttpResponse.
     */
    private void startAsyncImport( ImportOptions importOptions, String format, HttpServletRequest request,
        HttpServletResponse response )
        throws IOException
    {
        InputStream inputStream = saveTmp( request.getInputStream() );

        JobConfiguration jobId = new JobConfiguration( "dataValueImport", DATAVALUE_IMPORT,
            currentUserService.getCurrentUser().getUid(), true );
        schedulingManager.executeJob(
            new ImportDataValueTask( dataValueSetService, adxDataService, sessionFactory, inputStream, importOptions,
                jobId, format ) );

        response.setHeader( "Location", ContextUtils.getRootPath( request ) + "/system/tasks/" + DATAVALUE_IMPORT );
        webMessageService.send( jobConfigurationReport( jobId ), response, request );
    }

    /**
     * Writes the input stream to a temporary file, and returns a new input
     * stream connected to the file.
     *
     * @param in the InputStream.
     * @return an InputStream.
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

    /**
     * Returns an output stream with the appropriate compression based on the
     * given {@link Compression} argument.
     *
     * @param response the {@link HttpServletResponse}.
     * @param attachment the file download attachment name
     * @param compression the Compression {@link Compression}
     * @param format the file format, can be json, xml or csv.
     * @return Compressed OutputStream if given compression is given, otherwise
     *         just return uncompressed outputStream
     */
    private OutputStream compress( DataExportParams params, HttpServletResponse response, String attachment,
        Compression compression,
        String format )
        throws IOException,
        HttpMessageNotWritableException
    {
        String fileName = getAttachmentFileName( attachment, params );

        if ( Compression.GZIP == compression )
        {
            response.setHeader( ContextUtils.HEADER_CONTENT_DISPOSITION,
                "attachment; filename=" + fileName + "." + format + ".gzip" );
            response.setHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
            return new GZIPOutputStream( response.getOutputStream() );
        }
        else if ( Compression.ZIP == compression )
        {
            response.setHeader( ContextUtils.HEADER_CONTENT_DISPOSITION,
                "attachment; filename=" + fileName + "." + format + ".zip" );
            response.setHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );

            ZipOutputStream outputStream = new ZipOutputStream( response.getOutputStream() );
            outputStream.putNextEntry( new ZipEntry( fileName ) );

            return outputStream;
        }
        else
        {
            // file download only if attachment is explicitly specified for
            // no-compression option.
            if ( !StringUtils.isEmpty( attachment ) )
            {
                response.addHeader( ContextUtils.HEADER_CONTENT_DISPOSITION,
                    "attachment; filename=" + fileName + "." + format );
                response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
            }
            return response.getOutputStream();
        }
    }

    /**
     * Generate file name with format "dataValues_startDate_endDate" or
     * <p>
     * "{attachment}_startDate_endDate" if value of the attachment parameter is
     * not empty.
     * <p>
     * Date format is "yyyy-mm-dd". This can only apply if the request has
     * startDate and endDate. Otherwise, will return default file name
     * "dataValues".
     *
     * @param attachment The attachment parameter.
     * @param params {@link DataExportParams} contains startDate and endDate
     *        parameter.
     * @return the export file name.
     */
    private String getAttachmentFileName( String attachment, DataExportParams params )
    {
        String fileName = StringUtils.isEmpty( attachment ) ? "dataValues" : attachment;

        if ( params.getStartDate() == null || params.getEndDate() == null )
        {
            return fileName;
        }

        String dates = String.join( "_", DateUtils.getSqlDateString( params.getStartDate() ),
            DateUtils.getSqlDateString( params.getEndDate() ) );

        fileName = fileName.contains( "." ) ? fileName.substring( 0, fileName.indexOf( "." ) ) : fileName;

        return String.join( "_", fileName, dates );
    }
}