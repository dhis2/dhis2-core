package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.render.DefaultRenderService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( AppController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class AppController
{
    public static final String RESOURCE_PATH = "/apps";

    public final Pattern REGEX_REMOVE_PROTOCOL = Pattern.compile( ".+:/+" );

    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Autowired
    private AppManager appManager;

    @Autowired
    private RenderService renderService;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private ContextService contextService;

    // -------------------------------------------------------------------------
    // Resources
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_JSON )
    public void getApps( @RequestParam( required = false ) String key,
        HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );
        String contextPath = ContextUtils.getContextPath( request );

        List<App> apps = new ArrayList<>();

        if ( key != null )
        {
            App app = appManager.getApp( key, contextPath );

            if ( app == null )
            {
                response.sendError( HttpServletResponse.SC_NOT_FOUND );
                return;
            }

            apps.add( app );
        }
        else if ( !filters.isEmpty() )
        {
            apps = appManager.filterApps( filters, contextPath );
        }
        else
        {
            apps = appManager.getApps( contextPath );
        }

        response.setContentType( MediaType.APPLICATION_JSON_UTF8_VALUE );
        renderService.toJson( response.getOutputStream(), apps );
    }

    @RequestMapping( method = RequestMethod.POST )
    @PreAuthorize( "hasRole('ALL') or hasRole('M_dhis-web-app-management')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void installApp( @RequestParam( "file" ) MultipartFile file )
        throws IOException, WebMessageException
    {
        File tempFile = File.createTempFile( "IMPORT_", "_ZIP" );
        file.transferTo( tempFile );

        AppStatus status = appManager.installApp( tempFile, file.getOriginalFilename() );

        if ( !status.ok() )
        {
            String message = i18nManager.getI18n().getString( status.getMessage() );

            throw new WebMessageException( WebMessageUtils.conflict( message ) );
        }
    }

    @RequestMapping( method = RequestMethod.PUT )
    @PreAuthorize( "hasRole('ALL') or hasRole('M_dhis-web-app-management')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void reloadApps()
    {
        appManager.reloadApps();
    }

    @RequestMapping( value = "/{app}/**", method = RequestMethod.GET )
    public void renderApp( @PathVariable( "app" ) String app,
        HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        String folderPath = appManager.getAppFolderPath() + "/" + app + "/";

        Iterable<Resource> locations = Lists.newArrayList(
            resourceLoader.getResource( "file:" + folderPath )
        );

        Resource manifest = findResource( locations, folderPath, "manifest.webapp" );

        if ( manifest == null )
        {
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }

        ObjectMapper jsonMapper = DefaultRenderService.getJsonMapper();
        App application = jsonMapper.readValue( manifest.getInputStream(), App.class );

        if ( application.getName() == null || !appManager.isAccessible( application ) )
        {
            throw new ReadAccessDeniedException( "You don't have access to application " + app + "." );
        }

        String pageName = getUrl( request.getPathInfo(), app );

        // if request was for manifest.webapp, check for * and replace with host
        if ( "manifest.webapp".equals( pageName ) )
        {
            if ( "*".equals( application.getActivities().getDhis().getHref() ) )
            {
                String contextPath = ContextUtils.getContextPath( request );

                application.getActivities().getDhis().setHref( contextPath );
                jsonMapper.writeValue( response.getOutputStream(), application );
                return;
            }
        }

        Resource resource = findResource( locations, folderPath, pageName );

        if ( resource == null )
        {
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }

        if ( new ServletWebRequest( request, response ).checkNotModified( resource.lastModified() ) )
        {
            response.setStatus( HttpServletResponse.SC_NOT_MODIFIED );
            return;
        }

        String mimeType = request.getSession().getServletContext().getMimeType( resource.getFilename() );

        if ( mimeType != null )
        {
            response.setContentType( mimeType );
        }

        response.setContentLength( (int) resource.contentLength() );
        response.setHeader( "Last-Modified", DateUtils.getHttpDateString( new Date( resource.lastModified() ) ) );
        StreamUtils.copy( resource.getInputStream(), response.getOutputStream() );
    }

    @RequestMapping( value = "/{app}", method = RequestMethod.DELETE )
    @PreAuthorize( "hasRole('ALL') or hasRole('M_dhis-web-app-management')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteApp( @PathVariable( "app" ) String app, @RequestParam( required = false ) boolean deleteAppData )
        throws WebMessageException
    {
        if ( !appManager.exists( app ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "App does not exist: " + app ) );
        }

        if ( !appManager.deleteApp( app, deleteAppData ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "There was an error deleting app: " + app ) );
        }
    }

    @SuppressWarnings( "unchecked" )
    @RequestMapping( value = "/config", method = RequestMethod.POST, consumes = ContextUtils.CONTENT_TYPE_JSON )
    @PreAuthorize( "hasRole('ALL') or hasRole('M_dhis-web-app-management')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void setConfig( HttpServletRequest request )
        throws IOException, WebMessageException
    {
        Map<String, String> config = renderService.fromJson( request.getInputStream(), Map.class );

        if ( config == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "No config specified" ) );
        }

        String appStoreUrl = StringUtils.trimToNull( config.get( SettingKey.APP_STORE_URL.getName() ) );

        if ( appStoreUrl != null )
        {
            appManager.setAppStoreUrl( appStoreUrl );
        }
    }

    //--------------------------------------------------------------------------
    // Helpers
    //--------------------------------------------------------------------------

    private Resource findResource( Iterable<Resource> locations, String folder, String resourceName )
        throws IOException
    {
        for ( Resource location : locations )
        {
            Resource resource = location.createRelative( resourceName );

            if ( resource.exists() && resource.isReadable() )
            {
                File file = resource.getFile();

                // make sure that file resolves into path app folder
                if ( file != null && file.toPath().startsWith( folder ) )
                {
                    return resource;
                }
            }
        }

        return null;
    }

    private String getUrl( String path, String app )
    {
        String prefix = RESOURCE_PATH + "/" + app + "/";

        if ( path.startsWith( prefix ) )
        {
            path = path.substring( prefix.length() );
        }

        // if path is prefixed by any protocol, clear it out (this is to ensure that only files inside app directory can be resolved)
        path = REGEX_REMOVE_PROTOCOL.matcher( path ).replaceAll( "" );

        return path;
    }
}
