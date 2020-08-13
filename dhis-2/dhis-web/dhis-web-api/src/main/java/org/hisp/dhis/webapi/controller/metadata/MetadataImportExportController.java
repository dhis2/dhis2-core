package org.hisp.dhis.webapi.controller.metadata;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UserContext;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.csv.CsvImportClass;
import org.hisp.dhis.dxf2.csv.CsvImportOptions;
import org.hisp.dhis.dxf2.csv.CsvImportService;
import org.hisp.dhis.dxf2.gml.GmlImportService;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.dxf2.metadata.MetadataExportParams;
import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.scheduling.JobType.GML_IMPORT;
import static org.hisp.dhis.scheduling.JobType.METADATA_IMPORT;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( "/metadata" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class MetadataImportExportController
{
    @Autowired
    private MetadataImportService metadataImportService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private WebMessageService webMessageService;

    @Autowired
    private CsvImportService csvImportService;

    @Autowired
    private GmlImportService gmlImportService;

    @Autowired
    private SchedulingManager schedulingManager;

    @Autowired
    private MetadataExportService metadataExportService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UserSettingService userSettingService;

    @Autowired
    private ObjectFactory<MetadataAsyncImporter> metadataAsyncImporterFactory;

    @Autowired
    private ObjectFactory<GmlAsyncImporter> gmlAsyncImporterFactory;

    @PostMapping( value = "", consumes = MediaType.APPLICATION_JSON_VALUE )
    public void postJsonMetadata( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        MetadataImportParams params = metadataImportService.getParamsFromMap( contextService.getParameterValuesMap() );

        final Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects =
            renderService.fromMetadata( StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() ), RenderFormat.JSON );
        params.setObjects( objects );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );

        if ( params.hasJobId() )
        {
            startAsyncMetadata( params, request, response );
        }
        else
        {
            ImportReport importReport = metadataImportService.importMetadata( params );
            renderService.toJson( response.getOutputStream(), importReport );
        }
    }

    @PostMapping( value = "", consumes = "application/csv" )
    public void postCsvMetadata( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        MetadataImportParams params = metadataImportService.getParamsFromMap( contextService.getParameterValuesMap() );

        String classKey = request.getParameter( "classKey" );

        if ( StringUtils.isEmpty( classKey ) || !CsvImportClass.classExists( classKey ) )
        {
            webMessageService.send( WebMessageUtils.conflict( "Cannot find Csv import class:  " + classKey ), response, request );
            return;
        }

        params.setCsvImportClass( CsvImportClass.valueOf( classKey ) );

        Metadata metadata = csvImportService.fromCsv( request.getInputStream(), new CsvImportOptions()
            .setImportClass( params.getCsvImportClass() )
            .setFirstRowIsHeader( params.isFirstRowIsHeader() ) );

        params.addMetadata( schemaService.getMetadataSchemas(), metadata );

        if ( params.hasJobId() )
        {
            startAsyncMetadata( params, request, response );
        }
        else
        {
            ImportReport importReport = metadataImportService.importMetadata( params );
            renderService.toJson( response.getOutputStream(), importReport );
        }
    }

    @PostMapping( value = "/gml", consumes = MediaType.APPLICATION_XML_VALUE )
    public void postGmlMetadata( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        MetadataImportParams params = metadataImportService.getParamsFromMap( contextService.getParameterValuesMap() );

        if ( params.hasJobId() )
        {
            startAsyncGml( params, request, response );
        }
        else
        {
            ImportReport importReport = gmlImportService.importGml( request.getInputStream(), params );
            renderService.toJson( response.getOutputStream(), importReport );
        }
    }

    @PostMapping( value = "", consumes = MediaType.APPLICATION_XML_VALUE )
    public void postXmlMetadata( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        MetadataImportParams params = metadataImportService.getParamsFromMap( contextService.getParameterValuesMap() );
        Metadata metadata = renderService.fromXml( StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() ), Metadata.class );
        params.addMetadata( schemaService.getMetadataSchemas(), metadata );
        response.setContentType( MediaType.APPLICATION_XML_VALUE );

        if ( params.hasJobId() )
        {
            startAsyncMetadata( params, request, response );
        }
        else
        {
            ImportReport importReport = metadataImportService.importMetadata( params );
            renderService.toXml( response.getOutputStream(), importReport );
        }
    }

    @GetMapping( "/csvImportClasses" )
    public @ResponseBody List<CsvImportClass> getCsvImportClasses()
    {
        return Arrays.asList( CsvImportClass.values() );
    }

    @GetMapping
    public ResponseEntity<RootNode> getMetadata(
        @RequestParam( required = false, defaultValue = "false" ) boolean translate,
        @RequestParam( required = false ) String locale,
        @RequestParam( required = false, defaultValue = "false" ) boolean download )
    {
        if ( translate )
        {
            TranslateParams translateParams = new TranslateParams( true, locale );
            setUserContext( currentUserService.getCurrentUser(), translateParams );
        }

        MetadataExportParams params = metadataExportService.getParamsFromMap( contextService.getParameterValuesMap() );
        metadataExportService.validate( params );

        RootNode rootNode = metadataExportService.getMetadataAsNode( params );

        return MetadataExportControllerUtils.createResponseEntity( rootNode, download );
    }

    //----------------------------------------------------------------------------------------------------------------------------------------
    // Helpers
    //----------------------------------------------------------------------------------------------------------------------------------------

    private void startAsyncMetadata( MetadataImportParams params, HttpServletRequest request, HttpServletResponse response )
    {
        MetadataAsyncImporter metadataImporter = metadataAsyncImporterFactory.getObject();
        metadataImporter.setParams( params );
        schedulingManager.executeJob( metadataImporter );

        response.setHeader( "Location", ContextUtils.getRootPath( request ) + "/system/tasks/" + METADATA_IMPORT );
        webMessageService.send( jobConfigurationReport( params.getId() ), response, request );
    }

    private void startAsyncGml( MetadataImportParams params, HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        GmlAsyncImporter gmlImporter = gmlAsyncImporterFactory.getObject();
        gmlImporter.setInputStream( request.getInputStream() );
        gmlImporter.setParams( params );
        schedulingManager.executeJob( gmlImporter );

        response.setHeader( "Location", ContextUtils.getRootPath( request ) + "/system/tasks/" + GML_IMPORT );
        webMessageService.send( jobConfigurationReport( params.getId() ), response, request );
    }

    private void setUserContext( User user, TranslateParams translateParams )
    {
        Locale dbLocale = getLocaleWithDefault( translateParams );
        UserContext.setUser( user );
        UserContext.setUserSetting( UserSettingKey.DB_LOCALE, dbLocale );
    }

    private Locale getLocaleWithDefault( TranslateParams translateParams )
    {
        return translateParams.isTranslate() ?
            translateParams.getLocaleWithDefault( (Locale) userSettingService.getUserSetting( UserSettingKey.DB_LOCALE ) ) : null;
    }
}