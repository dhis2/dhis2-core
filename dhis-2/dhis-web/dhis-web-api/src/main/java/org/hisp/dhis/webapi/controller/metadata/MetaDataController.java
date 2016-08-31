package org.hisp.dhis.webapi.controller.metadata;

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

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.common.JacksonUtils;
import org.hisp.dhis.dxf2.metadata.ExportService;
import org.hisp.dhis.dxf2.metadata.ImportService;
import org.hisp.dhis.dxf2.metadata.ImportSummary;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.dxf2.metadata.tasks.ImportMetaDataTask;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.scheduling.TaskCategory;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( method = RequestMethod.GET )
public class MetaDataController
{
    public static final String RESOURCE_PATH = "/meta{xyz:[Dd]}ata";

    @Autowired
    private ExportService exportService;

    @Autowired
    private ImportService importService;

    @Autowired
    private ContextUtils contextUtils;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private CurrentUserService currentUserService;

    //--------------------------------------------------------------------------
    // Export
    //--------------------------------------------------------------------------

    @RequestMapping( value = MetaDataController.RESOURCE_PATH )
    public String export( @RequestParam Map<String, String> parameters, Model model )
    {
        WebOptions options = new WebOptions( parameters );
        Metadata metadata = exportService.getMetaData( options );

        model.addAttribute( "model", metadata );
        model.addAttribute( "viewClass", options.getViewClass( "export" ) );

        return "export";
    }

    @RequestMapping( value = MetaDataController.RESOURCE_PATH + ".xml", produces = "*/*" )
    public void exportXml( @RequestParam Map<String, String> parameters, HttpServletResponse response ) throws IOException
    {
        WebOptions options = new WebOptions( parameters );
        Metadata metadata = exportService.getMetaData( options );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_XML, CacheStrategy.NO_CACHE, "metadata.xml", true );

        Class<?> viewClass = JacksonUtils.getViewClass( options.getViewClass( "export" ) );
        JacksonUtils.toXmlWithView( response.getOutputStream(), metadata, viewClass );
    }

    @RequestMapping( value = MetaDataController.RESOURCE_PATH + ".json", produces = "*/*" )
    public void exportJson( @RequestParam Map<String, String> parameters, HttpServletResponse response ) throws IOException
    {
        WebOptions options = new WebOptions( parameters );
        Metadata metadata = exportService.getMetaData( options );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.NO_CACHE, "metadata.json", true );

        Class<?> viewClass = JacksonUtils.getViewClass( options.getViewClass( "export" ) );
        JacksonUtils.toJsonWithView( response.getOutputStream(), metadata, viewClass );
    }

    @RequestMapping( value = { MetaDataController.RESOURCE_PATH + ".zip" }, produces = "*/*" )
    public void exportZipped( @RequestParam Map<String, String> parameters, HttpServletResponse response, HttpServletRequest request ) throws IOException
    {
        String accept = request.getHeader( "Accept" );

        if ( isJson( accept ) )
        {
            exportZippedJSON( parameters, response );
        }
        else
        {
            exportZippedXML( parameters, response );
        }
    }

    @RequestMapping( value = { MetaDataController.RESOURCE_PATH + ".xml.zip" }, produces = "*/*" )
    public void exportZippedXML( @RequestParam Map<String, String> parameters, HttpServletResponse response ) throws IOException
    {
        WebOptions options = new WebOptions( parameters );
        Metadata metadata = exportService.getMetaData( options );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_ZIP, CacheStrategy.NO_CACHE, "metadata.xml.zip", true );
        response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );

        ZipOutputStream zip = new ZipOutputStream( response.getOutputStream() );
        zip.putNextEntry( new ZipEntry( "metadata.xml" ) );

        Class<?> viewClass = JacksonUtils.getViewClass( options.getViewClass( "export" ) );
        JacksonUtils.toXmlWithView( zip, metadata, viewClass );
    }

    @RequestMapping( value = { MetaDataController.RESOURCE_PATH + ".json.zip" }, produces = "*/*" )
    public void exportZippedJSON( @RequestParam Map<String, String> parameters, HttpServletResponse response ) throws IOException
    {
        WebOptions options = new WebOptions( parameters );
        Metadata metadata = exportService.getMetaData( options );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_ZIP, CacheStrategy.NO_CACHE, "metadata.json.zip", true );
        response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );

        ZipOutputStream zip = new ZipOutputStream( response.getOutputStream() );
        zip.putNextEntry( new ZipEntry( "metadata.json" ) );

        Class<?> viewClass = JacksonUtils.getViewClass( options.getViewClass( "export" ) );
        JacksonUtils.toJsonWithView( zip, metadata, viewClass );
    }

    @RequestMapping( value = { MetaDataController.RESOURCE_PATH + ".gz" }, produces = "*/*" )
    public void exportGZipped( @RequestParam Map<String, String> parameters, HttpServletResponse response, HttpServletRequest request ) throws IOException
    {
        String accept = request.getHeader( "Accept" );

        if ( isJson( accept ) )
        {
            exportGZippedJSON( parameters, response );
        }
        else
        {
            exportGZippedXML( parameters, response );
        }
    }

    @RequestMapping( value = { MetaDataController.RESOURCE_PATH + ".xml.gz" }, produces = "*/*" )
    public void exportGZippedXML( @RequestParam Map<String, String> parameters, HttpServletResponse response ) throws IOException
    {
        WebOptions options = new WebOptions( parameters );
        Metadata metadata = exportService.getMetaData( options );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_GZIP, CacheStrategy.NO_CACHE, "metadata.xml.gz", true );
        response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );

        GZIPOutputStream gzip = new GZIPOutputStream( response.getOutputStream() );

        Class<?> viewClass = JacksonUtils.getViewClass( options.getViewClass( "export" ) );
        JacksonUtils.toXmlWithView( gzip, metadata, viewClass );
    }

    @RequestMapping( value = { MetaDataController.RESOURCE_PATH + ".json.gz" }, produces = "*/*" )
    public void exportGZippedJSON( @RequestParam Map<String, String> parameters, HttpServletResponse response ) throws IOException
    {
        WebOptions options = new WebOptions( parameters );
        Metadata metadata = exportService.getMetaData( options );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_GZIP, CacheStrategy.NO_CACHE, "metadata.json.gz", true );
        response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );

        GZIPOutputStream gzip = new GZIPOutputStream( response.getOutputStream() );

        Class<?> viewClass = JacksonUtils.getViewClass( options.getViewClass( "export" ) );
        JacksonUtils.toJsonWithView( gzip, metadata, viewClass );
    }

    //--------------------------------------------------------------------------
    // Import
    //--------------------------------------------------------------------------

    @RequestMapping( value = MetaDataController.RESOURCE_PATH, method = RequestMethod.POST, consumes = { "application/xml", "text/*" } )
    public void importXml( ImportOptions importOptions, HttpServletResponse response, HttpServletRequest request ) throws IOException
    {
        Metadata metadata = JacksonUtils.fromXml( request.getInputStream(), Metadata.class );

        if ( importOptions.isAsync() )
        {
            startAsyncImport( importOptions, response, request, metadata );
        }
        else
        {
            startSyncImportXml( importOptions, response, metadata );
        }
    }

    @RequestMapping( value = MetaDataController.RESOURCE_PATH, method = RequestMethod.DELETE, consumes = { "application/xml", "text/*" } )
    public void deleteXml( ImportOptions importOptions, HttpServletResponse response, HttpServletRequest request ) throws IOException
    {
        importOptions.setImportStrategy( ImportStrategy.DELETE );
        importXml( importOptions, response, request );
    }

    @RequestMapping( value = MetaDataController.RESOURCE_PATH, method = RequestMethod.POST, consumes = "application/json" )
    public void importJson( ImportOptions importOptions, HttpServletResponse response, HttpServletRequest request ) throws IOException
    {
        Metadata metadata = JacksonUtils.fromJson( request.getInputStream(), Metadata.class );

        if ( importOptions.isAsync() )
        {
            startAsyncImport( importOptions, response, request, metadata );
        }
        else
        {
            startSyncImportJson( importOptions, response, metadata );
        }
    }

    @RequestMapping( value = MetaDataController.RESOURCE_PATH, method = RequestMethod.DELETE, consumes = "application/json" )
    public void deleteJson( ImportOptions importOptions, HttpServletResponse response, HttpServletRequest request ) throws IOException
    {
        importOptions.setImportStrategy( ImportStrategy.DELETE );
        importJson( importOptions, response, request );
    }

    @RequestMapping( value = { MetaDataController.RESOURCE_PATH + ".zip", MetaDataController.RESOURCE_PATH + ".xml.zip" }, method = RequestMethod.POST, consumes = { "application/xml", "text/*" } )
    public void importZippedXml( ImportOptions importOptions, HttpServletResponse response, HttpServletRequest request ) throws IOException
    {
        ZipInputStream zip = new ZipInputStream( request.getInputStream() );
        zip.getNextEntry();

        Metadata metadata = JacksonUtils.fromXml( zip, Metadata.class );

        if ( importOptions.isAsync() )
        {
            startAsyncImport( importOptions, response, request, metadata );
        }
        else
        {
            startSyncImportXml( importOptions, response, metadata );
        }
    }

    @RequestMapping( value = { MetaDataController.RESOURCE_PATH + ".zip", MetaDataController.RESOURCE_PATH + ".xml.zip" }, method = RequestMethod.DELETE, consumes = { "application/xml", "text/*" } )
    public void deleteZippedXml( ImportOptions importOptions, HttpServletResponse response, HttpServletRequest request ) throws IOException
    {
        importOptions.setImportStrategy( ImportStrategy.DELETE );
        importZippedXml( importOptions, response, request );
    }

    @RequestMapping( value = { MetaDataController.RESOURCE_PATH + ".zip", MetaDataController.RESOURCE_PATH + ".json.zip" }, method = RequestMethod.POST, consumes = "application/json" )
    public void importZippedJson( ImportOptions importOptions, HttpServletResponse response, HttpServletRequest request ) throws IOException
    {
        ZipInputStream zip = new ZipInputStream( request.getInputStream() );
        zip.getNextEntry();

        Metadata metadata = JacksonUtils.fromJson( zip, Metadata.class );

        if ( importOptions.isAsync() )
        {
            startAsyncImport( importOptions, response, request, metadata );
        }
        else
        {
            startSyncImportJson( importOptions, response, metadata );
        }
    }

    @RequestMapping( value = { MetaDataController.RESOURCE_PATH + ".zip", MetaDataController.RESOURCE_PATH + ".json.zip" }, method = RequestMethod.DELETE, consumes = "application/json" )
    public void deleteZippedJson( ImportOptions importOptions, HttpServletResponse response, HttpServletRequest request ) throws IOException
    {
        importOptions.setImportStrategy( ImportStrategy.DELETE );
        importZippedJson( importOptions, response, request );
    }

    @RequestMapping( value = { MetaDataController.RESOURCE_PATH + ".gz", MetaDataController.RESOURCE_PATH + ".xml.gz" }, method = RequestMethod.POST, consumes = { "application/xml", "text/*" } )
    public void importGZippedXml( ImportOptions importOptions, HttpServletResponse response, HttpServletRequest request ) throws IOException
    {
        GZIPInputStream gzip = new GZIPInputStream( request.getInputStream() );
        Metadata metadata = JacksonUtils.fromXml( gzip, Metadata.class );

        if ( importOptions.isAsync() )
        {
            startAsyncImport( importOptions, response, request, metadata );
        }
        else
        {
            startSyncImportXml( importOptions, response, metadata );
        }
    }

    @RequestMapping( value = { MetaDataController.RESOURCE_PATH + ".gz", MetaDataController.RESOURCE_PATH + ".xml.gz" }, method = RequestMethod.DELETE, consumes = { "application/xml", "text/*" } )
    public void deleteGZippedXml( ImportOptions importOptions, HttpServletResponse response, HttpServletRequest request ) throws IOException
    {
        importOptions.setImportStrategy( ImportStrategy.DELETE );
        importGZippedXml( importOptions, response, request );
    }

    @RequestMapping( value = { MetaDataController.RESOURCE_PATH + ".gz", MetaDataController.RESOURCE_PATH + ".json.gz" }, method = RequestMethod.POST, consumes = "application/json" )
    public void importGZippedJson( ImportOptions importOptions, HttpServletResponse response, HttpServletRequest request ) throws IOException
    {
        GZIPInputStream gzip = new GZIPInputStream( request.getInputStream() );
        Metadata metadata = JacksonUtils.fromJson( gzip, Metadata.class );

        if ( importOptions.isAsync() )
        {
            startAsyncImport( importOptions, response, request, metadata );
        }
        else
        {
            startSyncImportJson( importOptions, response, metadata );
        }
    }

    @RequestMapping( value = { MetaDataController.RESOURCE_PATH + ".gz", MetaDataController.RESOURCE_PATH + ".json.gz" }, method = RequestMethod.DELETE, consumes = "application/json" )
    public void deleteGZippedJson( ImportOptions importOptions, HttpServletResponse response, HttpServletRequest request ) throws IOException
    {
        importOptions.setImportStrategy( ImportStrategy.DELETE );
        importGZippedJson( importOptions, response, request );
    }

    //--------------------------------------------------------------------------
    // Helpers
    //--------------------------------------------------------------------------

    private void startSyncImportXml( ImportOptions importOptions, HttpServletResponse response, Metadata metadata ) throws IOException
    {
        String userUid = currentUserService.getCurrentUser().getUid();

        ImportSummary importSummary = importService.importMetaData( userUid, metadata, importOptions, null );
        response.setStatus( HttpServletResponse.SC_OK );
        response.setContentType( ContextUtils.CONTENT_TYPE_XML );
        JacksonUtils.toXml( response.getOutputStream(), importSummary );
    }

    private void startSyncImportJson( ImportOptions importOptions, HttpServletResponse response, Metadata metadata ) throws IOException
    {
        String userUid = currentUserService.getCurrentUser().getUid();

        ImportSummary importSummary = importService.importMetaData( userUid, metadata, importOptions, null );
        response.setStatus( HttpServletResponse.SC_OK );
        response.setContentType( ContextUtils.CONTENT_TYPE_JSON );
        JacksonUtils.toJson( response.getOutputStream(), importSummary );
    }

    private void startAsyncImport( ImportOptions importOptions, HttpServletResponse response, HttpServletRequest request, Metadata metadata )
    {
        String userUid = currentUserService.getCurrentUser().getUid();

        TaskId taskId = new TaskId( TaskCategory.METADATA_IMPORT, currentUserService.getCurrentUser() );

        scheduler.executeTask( new ImportMetaDataTask( userUid, importService, importOptions, taskId, metadata ) );

        response.setHeader( "Location", ContextUtils.getRootPath( request ) + "/system/tasks/" + TaskCategory.METADATA_IMPORT );
        response.setStatus( HttpServletResponse.SC_ACCEPTED );
    }

    //--------------------------------------------------------------------------
    // Supportive Methods
    //--------------------------------------------------------------------------

    public boolean isJson( String accept )
    {
        return accept != null && MediaType.parseMediaType( accept ).isCompatibleWith( MediaType.APPLICATION_JSON );
    }

    public boolean isXml( String accept )
    {
        return accept != null && MediaType.parseMediaType( accept ).isCompatibleWith( MediaType.APPLICATION_XML );
    }
}
