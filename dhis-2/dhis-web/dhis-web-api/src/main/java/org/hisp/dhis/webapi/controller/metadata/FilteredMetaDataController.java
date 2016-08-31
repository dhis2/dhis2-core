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

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.json.JSONObject;
import org.hisp.dhis.common.view.ExportView;
import org.hisp.dhis.common.filter.MetaDataFilter;
import org.hisp.dhis.dxf2.common.FilterOptions;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.common.JacksonUtils;
import org.hisp.dhis.dxf2.metadata.ExportService;
import org.hisp.dhis.dxf2.metadata.ImportService;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.dxf2.metadata.tasks.ImportMetaDataTask;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.scheduling.TaskCategory;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Ovidiu Rosu <rosu.ovi@gmail.com>
 */
@Controller
public class FilteredMetaDataController
{
    public static final String RESOURCE_PATH = "/filteredMetaData";

    @Autowired
    private ExportService exportService;

    @Qualifier( "contextUtils" )
    @Autowired
    private ContextUtils contextUtils;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private ImportService importService;

    @Autowired
    private CurrentUserService currentUserService;

    private boolean dryRun;

    private ImportStrategy strategy;

    //--------------------------------------------------------------------------
    // Getters & Setters
    //--------------------------------------------------------------------------

    public void setDryRun( boolean dryRun )
    {
        this.dryRun = dryRun;
    }

    public void setStrategy( String strategy )
    {
        this.strategy = ImportStrategy.valueOf( strategy );
    }

    //--------------------------------------------------------------------------
    // Detailed MetaData Export - POST Requests
    //--------------------------------------------------------------------------

    @RequestMapping( value = FilteredMetaDataController.RESOURCE_PATH, headers = "Accept=application/json" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_EXPORT')" )
    public String detailedExport( @RequestParam Map<String, String> parameters, Model model )
    {
        WebOptions options = new WebOptions( parameters );
        Metadata metadata = exportService.getMetaData( options );

        model.addAttribute( "model", metadata );
        model.addAttribute( "viewClass", "export" );

        return "export";
    }

    @RequestMapping( method = RequestMethod.POST, value = FilteredMetaDataController.RESOURCE_PATH + ".xml", produces = "*/*" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_EXPORT')" )
    public void exportXml( @RequestParam String exportJsonValue, HttpServletResponse response ) throws IOException
    {
        FilterOptions filterOptions = new FilterOptions( JSONObject.fromObject( exportJsonValue ) );
        Metadata metadata = exportService.getFilteredMetaData( filterOptions );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_XML, CacheStrategy.NO_CACHE, "metaData.xml", true );
        JacksonUtils.toXmlWithView( response.getOutputStream(), metadata, ExportView.class );
    }

    @RequestMapping( method = RequestMethod.POST, value = FilteredMetaDataController.RESOURCE_PATH + ".json", produces = "*/*" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_EXPORT')" )
    public void exportJson( @RequestParam String exportJsonValue, HttpServletResponse response ) throws IOException
    {
        FilterOptions filterOptions = new FilterOptions( JSONObject.fromObject( exportJsonValue ) );
        Metadata metadata = exportService.getFilteredMetaData( filterOptions );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.NO_CACHE, "metaData.json", true );
        JacksonUtils.toJsonWithView( response.getOutputStream(), metadata, ExportView.class );
    }

    @RequestMapping( method = RequestMethod.POST, value = { FilteredMetaDataController.RESOURCE_PATH + ".xml.zip" }, produces = "*/*" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_EXPORT')" )
    public void exportZippedXML( @RequestParam String exportJsonValue, HttpServletResponse response ) throws IOException
    {
        FilterOptions filterOptions = new FilterOptions( JSONObject.fromObject( exportJsonValue ) );
        Metadata metadata = exportService.getFilteredMetaData( filterOptions );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_ZIP, CacheStrategy.NO_CACHE, "metaData.xml.zip", true );
        response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );

        ZipOutputStream zip = new ZipOutputStream( response.getOutputStream() );
        zip.putNextEntry( new ZipEntry( "metaData.xml" ) );

        JacksonUtils.toXmlWithView( zip, metadata, ExportView.class );
    }

    @RequestMapping( method = RequestMethod.POST, value = { FilteredMetaDataController.RESOURCE_PATH + ".json.zip" }, produces = "*/*" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_EXPORT')" )
    public void exportZippedJSON( @RequestParam String exportJsonValue, HttpServletResponse response ) throws IOException
    {
        FilterOptions filterOptions = new FilterOptions( JSONObject.fromObject( exportJsonValue ) );
        Metadata metadata = exportService.getFilteredMetaData( filterOptions );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_ZIP, CacheStrategy.NO_CACHE, "metaData.json.zip", true );
        response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );

        ZipOutputStream zip = new ZipOutputStream( response.getOutputStream() );
        zip.putNextEntry( new ZipEntry( "metaData.json" ) );

        JacksonUtils.toJsonWithView( zip, metadata, ExportView.class );
    }

    @RequestMapping( method = RequestMethod.POST, value = { FilteredMetaDataController.RESOURCE_PATH + ".xml.gz" }, produces = "*/*" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_EXPORT')" )
    public void exportGZippedXML( @RequestParam String exportJsonValue, HttpServletResponse response ) throws IOException
    {
        FilterOptions filterOptions = new FilterOptions( JSONObject.fromObject( exportJsonValue ) );
        Metadata metadata = exportService.getFilteredMetaData( filterOptions );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_GZIP, CacheStrategy.NO_CACHE, "metaData.xml.gz", true );
        response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );

        GZIPOutputStream gzip = new GZIPOutputStream( response.getOutputStream() );
        JacksonUtils.toXmlWithView( gzip, metadata, ExportView.class );
    }

    @RequestMapping( method = RequestMethod.POST, value = { FilteredMetaDataController.RESOURCE_PATH + ".json.gz" }, produces = "*/*" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_EXPORT')" )
    public void exportGZippedJSON( @RequestParam String exportJsonValue, HttpServletResponse response ) throws IOException
    {
        FilterOptions filterOptions = new FilterOptions( JSONObject.fromObject( exportJsonValue ) );
        Metadata metadata = exportService.getFilteredMetaData( filterOptions );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_GZIP, CacheStrategy.NO_CACHE, "metaData.json.gz", true );
        response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );

        GZIPOutputStream gzip = new GZIPOutputStream( response.getOutputStream() );
        JacksonUtils.toJsonWithView( gzip, metadata, ExportView.class );
    }

    //--------------------------------------------------------------------------
    // Detailed MetaData Export - Filter functionality
    //--------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.GET, value = FilteredMetaDataController.RESOURCE_PATH + "/getMetaDataFilters" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_EXPORT')" )
    public @ResponseBody String getFilters( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        List<MetaDataFilter> metaDataFilters = exportService.getFilters();
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.NO_CACHE );
        return JacksonUtils.toJsonAsString( metaDataFilters );
    }

    @RequestMapping( method = RequestMethod.POST, value = FilteredMetaDataController.RESOURCE_PATH + "/saveFilter" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_EXPORT')" )
    public void saveFilter( @RequestBody JSONObject json, HttpServletResponse response ) throws IOException
    {
        exportService.saveFilter( json );
    }

    @RequestMapping( method = RequestMethod.POST, value = FilteredMetaDataController.RESOURCE_PATH + "/updateFilter" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_EXPORT')" )
    public void updateFilter( @RequestBody JSONObject json, HttpServletResponse response ) throws IOException
    {
        exportService.updateFilter( json );
    }

    @RequestMapping( method = RequestMethod.POST, value = FilteredMetaDataController.RESOURCE_PATH + "/deleteFilter" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_EXPORT')" )
    public void deleteFilter( @RequestBody JSONObject json, HttpServletResponse response ) throws IOException
    {
        exportService.deleteFilter( json );
    }

    //--------------------------------------------------------------------------
    // Detailed MetaData Import - POST Requests
    //--------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.POST, value = FilteredMetaDataController.RESOURCE_PATH + "/importDetailedMetaData" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_IMPORT')" )
    public void importDetailedMetaData( @RequestBody JSONObject json, HttpServletResponse response ) throws IOException
    {
        strategy = ImportStrategy.valueOf( json.getString( "strategy" ) );
        dryRun = json.getBoolean( "dryRun" );

        TaskId taskId = new TaskId( TaskCategory.METADATA_IMPORT, currentUserService.getCurrentUser() );
        User user = currentUserService.getCurrentUser();

        Metadata metadata = new ObjectMapper().readValue( json.getString( "metaData" ), Metadata.class );

        ImportOptions importOptions = new ImportOptions();
        importOptions.setStrategy( strategy );
        importOptions.setDryRun( dryRun );

        scheduler.executeTask( new ImportMetaDataTask( user.getUid(), importService, importOptions, taskId, metadata ) );
    }
}
