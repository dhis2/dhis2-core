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

import static org.hisp.dhis.common.DhisApiVersion.V38;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.importSummary;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.scheduling.JobType.DATAVALUE_IMPORT;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_PDF;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML_ADX;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;
import static org.hisp.dhis.webapi.utils.ContextUtils.stripFormatCompressionExtension;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.common.Compression;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.adx.AdxDataService;
import org.hisp.dhis.dxf2.adx.AdxException;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetQueryParams;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.datavalueset.tasks.ImportDataValueTask;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = "/dataValueSets" )
@Slf4j
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class DataValueSetController
{
    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private AdxDataService adxDataService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private AsyncTaskExecutor taskExecutor;

    @Autowired
    private SessionFactory sessionFactory;

    // -------------------------------------------------------------------------
    // Get
    // -------------------------------------------------------------------------

    @GetMapping( params = { "format" } )
    public void getDataValueSet(
        DataValueSetQueryParams params,
        @RequestParam( required = false ) String attachment,
        @RequestParam( required = false ) String compression,
        @RequestParam( required = false ) String format,
        HttpServletResponse response )
    {
        switch ( format )
        {
        case "xml":
            getDataValueSetXml( params, attachment, compression, response );
            break;
        case "adx+xml":
            getDataValueSetXmlAdx( params, attachment, compression, response );
            break;
        case "csv":
            getDataValueSetCsv( params, attachment, compression, response );
            break;
        default:
            getDataValueSetJson( params, attachment, compression, response );
        }
    }

    @GetMapping( produces = CONTENT_TYPE_XML )
    public void getDataValueSetXml( DataValueSetQueryParams params,
        @RequestParam( required = false ) String attachment,
        @RequestParam( required = false ) String compression,
        HttpServletResponse response )
    {
        getDataValueSet( attachment, compression, "xml", response, CONTENT_TYPE_XML,
            out -> dataValueSetService.writeDataValueSetXml( dataValueSetService.getFromUrl( params ), out ) );
    }

    @GetMapping( produces = CONTENT_TYPE_XML_ADX )
    public void getDataValueSetXmlAdx( DataValueSetQueryParams params,
        @RequestParam( required = false ) String attachment,
        @RequestParam( required = false ) String compression,
        HttpServletResponse response )
    {
        getDataValueSet( attachment, compression, "xml", response, CONTENT_TYPE_XML_ADX,
            out -> {
                try
                {
                    adxDataService.writeDataValueSet( adxDataService.getFromUrl( params ), out );
                }
                catch ( AdxException ex )
                {
                    // this will end up in same exception handler
                    throw new IllegalStateException( ex.getMessage(), ex );
                }
            } );
    }

    @GetMapping( produces = CONTENT_TYPE_JSON )
    public void getDataValueSetJson( DataValueSetQueryParams params,
        @RequestParam( required = false ) String attachment,
        @RequestParam( required = false ) String compression,
        HttpServletResponse response )
    {
        getDataValueSet( attachment, compression, "json", response, CONTENT_TYPE_JSON,
            out -> dataValueSetService.writeDataValueSetJson( dataValueSetService.getFromUrl( params ), out ) );
    }

    @GetMapping( produces = CONTENT_TYPE_CSV )
    public void getDataValueSetCsv( DataValueSetQueryParams params,
        @RequestParam( required = false ) String attachment,
        @RequestParam( required = false ) String compression,
        HttpServletResponse response )
    {
        getDataValueSet( attachment, compression, "csv", response, CONTENT_TYPE_CSV,
            out -> dataValueSetService.writeDataValueSetCsv( dataValueSetService.getFromUrl( params ),
                new PrintWriter( out ) ) );
    }

    private void getDataValueSet( String attachment,
        String compression, String format, HttpServletResponse response, String contentType,
        Consumer<OutputStream> writeOutput )
    {
        response.setContentType( contentType );
        setNoStore( response );

        try (
            OutputStream out = compress( response, attachment, Compression.fromValue( compression ), format ) )
        {
            writeOutput.accept( out );
        }
        catch ( IOException ex )
        {
            throw new UncheckedIOException( ex );
        }
    }

    // -------------------------------------------------------------------------
    // Post
    // -------------------------------------------------------------------------

    @PostMapping( consumes = APPLICATION_XML_VALUE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    @ResponseBody
    public WebMessage postDxf2DataValueSet( ImportOptions importOptions, HttpServletRequest request )
        throws IOException
    {
        if ( importOptions.isAsync() )
        {
            return startAsyncImport( importOptions, ImportDataValueTask.FORMAT_XML, request );
        }
        ImportSummary summary = dataValueSetService.saveDataValueSet( request.getInputStream(), importOptions );
        summary.setImportOptions( importOptions );

        return importSummary( summary ).withPlainResponseBefore( V38 );
    }

    @PostMapping( consumes = CONTENT_TYPE_XML_ADX )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    @ResponseBody
    public WebMessage postAdxDataValueSet( ImportOptions importOptions, HttpServletRequest request )
        throws IOException
    {
        if ( importOptions.isAsync() )
        {
            return startAsyncImport( importOptions, ImportDataValueTask.FORMAT_ADX, request );
        }
        ImportSummary summary = adxDataService.saveDataValueSet( request.getInputStream(), importOptions,
            null );
        summary.setImportOptions( importOptions );

        return importSummary( summary ).withPlainResponseBefore( V38 );
    }

    @PostMapping( consumes = APPLICATION_JSON_VALUE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    @ResponseBody
    public WebMessage postJsonDataValueSet( ImportOptions importOptions, HttpServletRequest request )
        throws IOException
    {
        if ( importOptions.isAsync() )
        {
            return startAsyncImport( importOptions, ImportDataValueTask.FORMAT_JSON, request );
        }
        ImportSummary summary = dataValueSetService.saveDataValueSetJson( request.getInputStream(), importOptions );
        summary.setImportOptions( importOptions );

        return importSummary( summary ).withPlainResponseBefore( V38 );
    }

    @PostMapping( consumes = "application/csv" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    @ResponseBody
    public WebMessage postCsvDataValueSet( ImportOptions importOptions, HttpServletRequest request )
        throws IOException
    {
        if ( importOptions.isAsync() )
        {
            return startAsyncImport( importOptions, ImportDataValueTask.FORMAT_CSV, request );
        }
        ImportSummary summary = dataValueSetService.saveDataValueSetCsv( request.getInputStream(), importOptions );
        summary.setImportOptions( importOptions );

        return importSummary( summary ).withPlainResponseBefore( V38 );
    }

    @PostMapping( consumes = CONTENT_TYPE_PDF )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    @ResponseBody
    public WebMessage postPdfDataValueSet( ImportOptions importOptions, HttpServletRequest request )
        throws IOException
    {
        if ( importOptions.isAsync() )
        {
            return startAsyncImport( importOptions, ImportDataValueTask.FORMAT_PDF, request );
        }
        ImportSummary summary = dataValueSetService.saveDataValueSetPdf( request.getInputStream(), importOptions );
        summary.setImportOptions( importOptions );

        return importSummary( summary ).withPlainResponseBefore( V38 );
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
     */
    private WebMessage startAsyncImport( ImportOptions importOptions, String format, HttpServletRequest request )
        throws IOException
    {
        InputStream inputStream = saveTmp( request.getInputStream() );

        JobConfiguration jobId = new JobConfiguration( "dataValueImport", DATAVALUE_IMPORT,
            currentUserService.getCurrentUser().getUid(), true );
        taskExecutor.executeTask(
            new ImportDataValueTask( dataValueSetService, adxDataService, sessionFactory, inputStream, importOptions,
                jobId, format ) );

        return jobConfigurationReport( jobId )
            .setLocation( "/system/tasks/" + DATAVALUE_IMPORT );
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
    private OutputStream compress( HttpServletResponse response, String attachment, Compression compression,
        String format )
        throws IOException,
        HttpMessageNotWritableException
    {
        String fileName = StringUtils.isEmpty( attachment ) ? "datavalue" : attachment;

        if ( Compression.GZIP == compression )
        {
            // make sure to remove format + gz/gzip extension if present
            fileName = stripFormatCompressionExtension( fileName, format, "gzip" );
            fileName = stripFormatCompressionExtension( fileName, format, "gz" );

            response.setHeader( ContextUtils.HEADER_CONTENT_DISPOSITION,
                "attachment; filename=" + fileName + "." + format + ".gz" );
            response.setHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
            return new GZIPOutputStream( response.getOutputStream() );
        }
        else if ( Compression.ZIP == compression )
        {
            // make sure to remove format + zip extension if present
            fileName = stripFormatCompressionExtension( fileName, format, "zip" );

            response.setHeader( ContextUtils.HEADER_CONTENT_DISPOSITION,
                "attachment; filename=" + fileName + "." + format + ".zip" );
            response.setHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );

            ZipOutputStream outputStream = new ZipOutputStream( response.getOutputStream() );
            outputStream.putNextEntry( new ZipEntry( fileName + "." + format ) );

            return outputStream;
        }
        else
        {
            // file download only if attachment is explicitly specified for
            // no-compression option.
            if ( !StringUtils.isEmpty( attachment ) )
            {
                response.addHeader( ContextUtils.HEADER_CONTENT_DISPOSITION, "attachment; filename=" + attachment );
                response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
            }

            return response.getOutputStream();
        }
    }
}
