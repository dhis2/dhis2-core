package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.collect.Lists;
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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

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

    @RequestMapping( value = { "/uid", "/id" }, method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode getUid( @RequestParam( required = false, defaultValue = "1" ) Integer limit, HttpServletResponse response )
        throws IOException, InvalidTypeException
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

    @RequestMapping( value = { "/uid", "/id" }, method = RequestMethod.GET, produces = { "application/csv" } )
    public void getUidCsv( @RequestParam( required = false, defaultValue = "1" ) Integer limit, HttpServletResponse response )
        throws IOException, InvalidTypeException
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

    @RequestMapping( value = "/uuid", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode getUuid( @RequestParam( required = false, defaultValue = "1" ) Integer limit, HttpServletResponse response )
        throws IOException, InvalidTypeException
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

    @RequestMapping( value = "/tasks", method = RequestMethod.GET, produces = { "*/*", "application/json" } )
    public void getTasksJson( HttpServletResponse response )
        throws IOException
    {
        setNoStore( response );

        renderService.toJson( response.getOutputStream(), notifier.getNotifications() );
    }

    @RequestMapping( value = "/tasks/{jobType}", method = RequestMethod.GET, produces = { "*/*", "application/json" } )
    @ApiVersion( include = { DhisApiVersion.DEFAULT, DhisApiVersion.ALL }, exclude = { DhisApiVersion.V29 } )
    public void getTaskJson( @PathVariable( "jobType" ) String jobType, @RequestParam( required = false ) String lastId, HttpServletResponse response )
        throws IOException
    {
        List<Notification> notifications = new ArrayList<>();

        if ( jobType != null )
        {
            notifications = notifier.getLastNotificationsByJobType( JobType.valueOf( jobType.toUpperCase() ), lastId );
        }

        setNoStore( response );

        renderService.toJson( response.getOutputStream(), notifications );
    }

    @RequestMapping( value = "/tasks/{jobType}", method = RequestMethod.GET, produces = { "*/*", "application/json" } )
    @ApiVersion( include = { DhisApiVersion.V29 }, exclude = { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
    public void getTasksExtendedJson( @PathVariable( "jobType" ) String jobType, HttpServletResponse response ) throws IOException
    {
        Map<String, LinkedList<Notification>> notifications = new HashMap<>();

        if ( jobType != null )
        {
            notifications = notifier.getNotificationsByJobType( JobType.valueOf( jobType.toUpperCase() ) );
        }

        setNoStore( response );

        renderService.toJson( response.getOutputStream(), notifications );
    }

    @RequestMapping( value = "/tasks/{jobType}/{jobId}", method = RequestMethod.GET, produces = { "*/*", "application/json" } )
    public void getTaskJsonByUid( @PathVariable( "jobType" ) String jobType, @PathVariable( "jobId" ) String jobId,
        HttpServletResponse response )
        throws IOException
    {
        List<Notification> notifications = new ArrayList<>();

        if ( jobType != null )
        {
            notifications = notifier.getNotificationsByJobId( JobType.valueOf( jobType.toUpperCase() ), jobId );
        }

        setNoStore( response );

        renderService.toJson( response.getOutputStream(), notifications );
    }

    @RequestMapping( value = "/taskSummaries/{jobType}", method = RequestMethod.GET, produces = { "*/*", "application/json" } )
    @ApiVersion( include = { DhisApiVersion.V29 }, exclude = { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
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
    }

    @RequestMapping( value = "/taskSummaries/{jobType}", method = RequestMethod.GET, produces = { "*/*", "application/json" } )
    @ApiVersion( include = { DhisApiVersion.DEFAULT, DhisApiVersion.ALL }, exclude = { DhisApiVersion.V29 } )
    public void getTaskSummaryJson( @PathVariable( "jobType" ) String jobType, HttpServletResponse response )
        throws IOException
    {
        if ( jobType != null )
        {
            Object summary = notifier.getJobSummary( JobType.valueOf( jobType.toUpperCase() ) );

            handleSummary( response, summary );
            return;
        }

        setNoStore( response );
    }

    @RequestMapping( value = "/taskSummaries/{jobType}/{jobId}", method = RequestMethod.GET, produces = { "*/*",
        "application/json" } )
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
    }

    private void handleSummary( HttpServletResponse response, Object summary )
        throws IOException
    {
        if ( summary != null && ImportSummary.class.isInstance( summary ) ) //TODO improve this
        {
            ImportSummary importSummary = (ImportSummary) summary;
            renderService.toJson( response.getOutputStream(), importSummary );
        }
        else
        {
            renderService.toJson( response.getOutputStream(), summary );
        }
    }

    @RequestMapping( value = "/info", method = RequestMethod.GET, produces = { "application/json",
        "application/javascript" } )
    public @ResponseBody
    SystemInfo getSystemInfo( Model model, HttpServletRequest request, HttpServletResponse response )
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

    @RequestMapping( value = "/objectCounts", method = RequestMethod.GET )
    public @ResponseBody RootNode getObjectCounts()
    {
        Map<Objects, Integer> objectCounts = statisticsProvider.getObjectCounts();
        RootNode rootNode = NodeUtils.createRootNode( "objectCounts" );

        for ( Objects objects : objectCounts.keySet() )
        {
            rootNode.addChild( new SimpleNode( objects.getValue(), objectCounts.get( objects ) ) );
        }

        return rootNode;
    }

    @RequestMapping( value = "/ping", method = RequestMethod.GET, produces = "text/plain" )
    @ApiVersion( exclude = { DhisApiVersion.V26, DhisApiVersion.V27, DhisApiVersion.V28, DhisApiVersion.V29 } )
    public @ResponseBody String pingLegacy()
    {
        return "pong";
    }

    @RequestMapping( value = "/ping", method = RequestMethod.GET )
    @ApiVersion( exclude = { DhisApiVersion.DEFAULT } )
    public @ResponseBody String ping()
    {
        return "pong";
    }

    @RequestMapping( value = "/flags", method = RequestMethod.GET, produces = { "application/json" } )
    public @ResponseBody List<StyleObject> getFlags()
    {
        return getFlagObjects();
    }

    @RequestMapping( value = "/styles", method = RequestMethod.GET, produces = { "application/json" } )
    public @ResponseBody List<StyleObject> getStyles()
    {
        return styleManager.getStyles();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private List<StyleObject> getFlagObjects()
    {
        List<String> flags = systemSettingManager.getFlags();

        I18n i18n = i18nManager.getI18n();

        List<StyleObject> list = Lists.newArrayList();

        for ( String flag : flags )
        {
            String file = flag + ".png";

            list.add( new StyleObject( i18n.getString( flag ), flag, file ) );
        }

        return list;
    }
}
