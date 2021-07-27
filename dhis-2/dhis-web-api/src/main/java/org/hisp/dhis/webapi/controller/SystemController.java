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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import java.io.IOException;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.Objects;
import org.hisp.dhis.dxf2.common.ImportSummary;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.exception.InvalidTypeException;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.StyleManager;
import org.hisp.dhis.setting.StyleObject;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.statistics.StatisticsProvider;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.system.notification.Notification;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = SystemController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class SystemController
{
    public static final String RESOURCE_PATH = "/system";

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SystemService systemService;

    @Autowired
    private StyleManager styleManager;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private Notifier notifier;

    @Autowired
    private RenderService renderService;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private StatisticsProvider statisticsProvider;

    private static final CsvFactory CSV_FACTORY = new CsvMapper().getFactory();

    // -------------------------------------------------------------------------
    // UID Generator
    // -------------------------------------------------------------------------

    @GetMapping( value = { "/uid", "/id" }, produces = {
        MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode getUid( @RequestParam( required = false, defaultValue = "1" ) Integer limit,
        HttpServletResponse response )
        throws IOException,
        InvalidTypeException
    {
        limit = Math.min( limit, 10000 );

        RootNode rootNode = new RootNode( "codes" );
        CollectionNode collectionNode = rootNode.addChild( new CollectionNode( "codes" ) );
        collectionNode.setWrapping( false );

        for ( int i = 0; i < limit; i++ )
        {
            collectionNode.addChild( new SimpleNode( "code", CodeGenerator.generateUid() ) );
        }

        setNoStore( response );

        return rootNode;
    }

    @GetMapping( value = { "/uid", "/id" }, produces = { "application/csv" } )
    public void getUidCsv( @RequestParam( required = false, defaultValue = "1" ) Integer limit,
        HttpServletResponse response )
        throws IOException,
        InvalidTypeException
    {
        limit = Math.min( limit, 10000 );

        CsvGenerator csvGenerator = CSV_FACTORY.createGenerator( response.getOutputStream() );

        CsvSchema.Builder schemaBuilder = CsvSchema.builder()
            .addColumn( "uid" )
            .setUseHeader( true );

        csvGenerator.setSchema( schemaBuilder.build() );

        for ( int i = 0; i < limit; i++ )
        {
            csvGenerator.writeStartObject();
            csvGenerator.writeStringField( "uid", CodeGenerator.generateUid() );
            csvGenerator.writeEndObject();
        }

        csvGenerator.flush();
    }

    @GetMapping( value = "/uuid", produces = { MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode getUuid( @RequestParam( required = false, defaultValue = "1" ) Integer limit,
        HttpServletResponse response )
        throws IOException,
        InvalidTypeException
    {
        limit = Math.min( limit, 10000 );

        RootNode rootNode = new RootNode( "codes" );
        CollectionNode collectionNode = rootNode.addChild( new CollectionNode( "codes" ) );
        collectionNode.setWrapping( false );

        for ( int i = 0; i < limit; i++ )
        {
            collectionNode.addChild( new SimpleNode( "code", UUID.randomUUID().toString() ) );
        }

        setNoStore( response );

        return rootNode;
    }

    // -------------------------------------------------------------------------
    // Tasks
    // -------------------------------------------------------------------------

    @GetMapping( value = "/tasks", produces = { "*/*", "application/json" } )
    public void getTasksJson( HttpServletResponse response )
        throws IOException
    {
        setNoStore( response );
        response.setContentType( CONTENT_TYPE_JSON );

        renderService.toJson( response.getOutputStream(), notifier.getNotifications() );
    }

    @GetMapping( value = "/tasks/{jobType}", produces = { "*/*", "application/json" } )
    public void getTasksExtendedJson( @PathVariable( "jobType" ) String jobType, HttpServletResponse response )
        throws IOException
    {
        Map<String, Deque<Notification>> notifications = jobType == null
            ? emptyMap()
            : notifier.getNotificationsByJobType( JobType.valueOf( jobType.toUpperCase() ) );

        setNoStore( response );
        response.setContentType( CONTENT_TYPE_JSON );

        renderService.toJson( response.getOutputStream(), notifications );
    }

    @GetMapping( value = "/tasks/{jobType}/{jobId}", produces = { "*/*", "application/json" } )
    public void getTaskJsonByUid( @PathVariable( "jobType" ) String jobType, @PathVariable( "jobId" ) String jobId,
        HttpServletResponse response )
        throws IOException
    {
        Collection<Notification> notifications = jobType == null
            ? emptyList()
            : notifier.getNotificationsByJobId( JobType.valueOf( jobType.toUpperCase() ), jobId );

        setNoStore( response );
        response.setContentType( CONTENT_TYPE_JSON );

        renderService.toJson( response.getOutputStream(), notifications );
    }

    // -------------------------------------------------------------------------
    // Tasks summary
    // -------------------------------------------------------------------------

    @GetMapping( value = "/taskSummaries/{jobType}", produces = { "*/*", "application/json" } )
    public void getTaskSummaryExtendedJson( @PathVariable( "jobType" ) String jobType, HttpServletResponse response )
        throws IOException
    {
        if ( jobType != null )
        {
            Object summary = notifier.getJobSummariesForJobType( JobType.valueOf( jobType.toUpperCase() ) );

            handleSummary( response, summary );
            return;
        }

        setNoStore( response );
        response.setContentType( CONTENT_TYPE_JSON );
    }

    @GetMapping( value = "/taskSummaries/{jobType}/{jobId}", produces = { "*/*", "application/json" } )
    public void getTaskSummaryJson( @PathVariable( "jobType" ) String jobType, @PathVariable( "jobId" ) String jobId,
        HttpServletResponse response )
        throws IOException
    {
        if ( jobType != null )
        {
            Object summary = notifier.getJobSummaryByJobId( JobType.valueOf( jobType.toUpperCase() ), jobId );

            handleSummary( response, summary );
        }

        setNoStore( response );
        response.setContentType( CONTENT_TYPE_JSON );
    }

    private void handleSummary( HttpServletResponse response, Object summary )
        throws IOException
    {
        if ( summary != null && ImportSummary.class.isInstance( summary ) ) // TODO
                                                                            // improve
                                                                            // this
        {
            ImportSummary importSummary = (ImportSummary) summary;
            renderService.toJson( response.getOutputStream(), importSummary );
        }
        else
        {
            renderService.toJson( response.getOutputStream(), summary );
        }
    }

    // -------------------------------------------------------------------------
    // Various
    // -------------------------------------------------------------------------

    @GetMapping( value = "/info", produces = { "application/json", "application/javascript" } )
    public @ResponseBody SystemInfo getSystemInfo( Model model, HttpServletRequest request,
        HttpServletResponse response )
    {
        SystemInfo info = systemService.getSystemInfo();

        info.setContextPath( ContextUtils.getContextPath( request ) );
        info.setUserAgent( request.getHeader( ContextUtils.HEADER_USER_AGENT ) );

        if ( !currentUserService.currentUserIsSuper() )
        {
            info.clearSensitiveInfo();
        }

        setNoStore( response );

        return info;
    }

    @GetMapping( "/objectCounts" )
    public @ResponseBody RootNode getObjectCounts()
    {
        Map<Objects, Long> objectCounts = statisticsProvider.getObjectCounts();
        RootNode rootNode = NodeUtils.createRootNode( "objectCounts" );

        for ( Objects objects : objectCounts.keySet() )
        {
            rootNode.addChild( new SimpleNode( objects.getValue(), objectCounts.get( objects ) ) );
        }

        return rootNode;
    }

    @GetMapping( "/ping" )
    @ResponseStatus( HttpStatus.OK )
    public @ResponseBody String ping( HttpServletResponse response )
    {
        setNoStore( response );

        return "pong";
    }

    @GetMapping( value = "/flags", produces = { "application/json" } )
    public @ResponseBody List<StyleObject> getFlags()
    {
        return getFlagObjects();
    }

    @GetMapping( value = "/styles", produces = { "application/json" } )
    public @ResponseBody List<StyleObject> getStyles()
    {
        return styleManager.getStyles();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private List<StyleObject> getFlagObjects()
    {
        I18n i18n = i18nManager.getI18n();
        return systemSettingManager.getFlags().stream()
            .map( flag -> new StyleObject( i18n.getString( flag ), flag, (flag + ".png") ) )
            .collect( Collectors.toList() );
    }
}
