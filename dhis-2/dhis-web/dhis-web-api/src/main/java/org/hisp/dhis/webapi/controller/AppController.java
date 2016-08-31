package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import org.hisp.dhis.dxf2.render.DefaultRenderService;
import org.hisp.dhis.dxf2.render.RenderService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.setting.Setting;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
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

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( AppController.RESOURCE_PATH )
public class AppController
{
    public static final String RESOURCE_PATH = "/apps";

    @Autowired
    private AppManager appManager;

    @Autowired
    private RenderService renderService;

    @Autowired
    private LocationManager locationManager;

    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @RequestMapping( method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_JSON )
    public void getApps( @RequestParam( required = false ) String key, HttpServletResponse response )
        throws IOException
    {
        List<App> apps = new ArrayList<>();

        if ( key != null )
        {
            App app = appManager.getApp( key );

            if ( app == null )
            {
                response.sendError( HttpServletResponse.SC_NOT_FOUND );
                return;
            }

            apps.add( app );
        }
        else
        {
            apps = appManager.getApps();
        }

        renderService.toJson( response.getOutputStream(), apps );
    }

    @RequestMapping( method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    @PreAuthorize( "hasRole('ALL') or hasRole('M_dhis-web-maintenance-appmanager')" )
    public void installApp( @RequestParam( "file" ) MultipartFile file, HttpServletRequest request,
        HttpServletResponse response )
        throws IOException, WebMessageException
    {
        File tempFile = File.createTempFile( "IMPORT_", "_ZIP" );
        file.transferTo( tempFile );

        String contextPath = ContextUtils.getContextPath( request );

        switch ( appManager.installApp( tempFile, file.getOriginalFilename(), contextPath ) )
        {
        case OK:
            break;
        case NAMESPACE_TAKEN:
            throw new WebMessageException(
                WebMessageUtils.conflict( "The namespace defined in manifest.webapp is already protected." ) );
        case INVALID_ZIP_FORMAT:
            throw new WebMessageException(
                WebMessageUtils.unprocessableEntity( "Zip-file could not be read." ) );
        case INVALID_MANIFEST_JSON:
            throw new WebMessageException(
                WebMessageUtils.conflict( "Invalid JSON in app manifest file." ) );
        case INSTALLATION_FAILED:
            throw new WebMessageException(
                WebMessageUtils.conflict( "App could not be installed on file system, check permissions." ) );
        }
    }

    @RequestMapping( method = RequestMethod.PUT )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    @PreAuthorize( "hasRole('ALL') or hasRole('M_dhis-web-maintenance-appmanager')" )
    public void reloadApps()
    {
        appManager.reloadApps();
    }

    @RequestMapping( value = "/{app}/**", method = RequestMethod.GET )
    public void renderApp( @PathVariable( "app" ) String app, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        Iterable<Resource> locations = Lists.newArrayList(
            resourceLoader.getResource( "file:" + appManager.getAppFolderPath() + "/" + app + "/" ),
            resourceLoader.getResource( "classpath*:/apps/" + app + "/" )
        );

        Resource manifest = findResource( locations, "manifest.webapp" );

        if ( manifest == null )
        {
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }

        ObjectMapper jsonMapper = ((DefaultRenderService) renderService).getJsonMapper();
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

        Resource resource = findResource( locations, pageName );

        if ( resource == null )
        {
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }

        if ( new ServletWebRequest( request ).checkNotModified( resource.lastModified() ) )
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
    @PreAuthorize( "hasRole('ALL') or hasRole('M_dhis-web-maintenance-appmanager')" )
    public void deleteApp( @PathVariable( "app" ) String app,
        @RequestParam( value = "deleteappdata", required = false, defaultValue = "false" ) boolean deleteAppData,
        HttpServletRequest request, HttpServletResponse response )
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
    @PreAuthorize( "hasRole('ALL') or hasRole('M_dhis-web-maintenance-appmanager')" )
    public void setConfig( HttpServletRequest request, HttpServletResponse response )
        throws IOException, WebMessageException
    {
        Map<String, String> config = renderService.fromJson( request.getInputStream(), Map.class );

        if ( config == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "No config specified" ) );
        }

        String appBaseUrl = StringUtils.trimToNull( config.get( Setting.APP_BASE_URL.getName() ) );
        String appFolderPath = StringUtils.trimToNull( config.get( Setting.APP_FOLDER_PATH.getName() ) );
        String appStoreUrl = StringUtils.trimToNull( config.get( Setting.APP_STORE_URL.getName() ) );

        if ( appBaseUrl != null )
        {
            appManager.setAppBaseUrl( appBaseUrl );
        }

        if ( appFolderPath != null )
        {
            appManager.setAppFolderPath( appFolderPath );
        }

        if ( appStoreUrl != null )
        {
            appManager.setAppStoreUrl( appStoreUrl );
        }
    }

    @RequestMapping( value = "/config", method = RequestMethod.DELETE )
    @PreAuthorize( "hasRole('ALL') or hasRole('M_dhis-web-maintenance-appmanager')" )
    public void resetConfig( HttpServletRequest request )
    {
        String contextPath = ContextUtils.getContextPath( request );

        String appFolderPath = locationManager.getExternalDirectoryPath() + AppManager.APPS_DIR;
        String appBaseUrl = contextPath + AppManager.APPS_API_PATH;

        appManager.setAppFolderPath( appFolderPath );
        appManager.setAppBaseUrl( appBaseUrl );
    }

    //--------------------------------------------------------------------------
    // Helpers
    //--------------------------------------------------------------------------

    private Resource findResource( Iterable<Resource> locations, String resourceName )
        throws IOException
    {
        for ( Resource location : locations )
        {
            Resource resource = location.createRelative( resourceName );

            if ( resource.exists() && resource.isReadable() )
            {
                return resource;
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

        return path;
    }
}
