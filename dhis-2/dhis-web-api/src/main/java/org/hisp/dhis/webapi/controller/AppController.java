/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.appmanager.*;
import org.hisp.dhis.appmanager.webmodules.WebModule;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.user.CurrentUserDetails;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Tags( "ui" )
@Controller
@RequestMapping( AppController.RESOURCE_PATH )
@Slf4j
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class AppController
{
    public static final String RESOURCE_PATH = "/apps";

    public static final Pattern REGEX_REMOVE_PROTOCOL = Pattern.compile( ".+:/+" );

    @Autowired
    private AppManager appManager;

    @Autowired
    private AppMenuManager appMenuManager;

    @Autowired
    private RenderService renderService;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private ContextService contextService;

    @Autowired
    private ObjectMapper jsonMapper;

    @GetMapping( value = "/menu", produces = ContextUtils.CONTENT_TYPE_JSON )
    public @ResponseBody Map<String, List<WebModule>> getWebModules( HttpServletRequest request )
    {
        checkForEmbeddedJettyRuntime( request );

        String contextPath = ContextUtils.getContextPath( request );
        return Map.of( "modules", getAccessibleAppMenu( contextPath ) );
    }

    /**
     * Checks if we are running in embedded Jetty mode. If so, we need to set
     * the SecurityContext manually from the session object
     * SPRING_SECURITY_CONTEXT. This is done for compatibility with the old
     * Struts action, which is not 100% ported yet. To be removed when
     * application is ported away from Struts
     */
    private static void checkForEmbeddedJettyRuntime( HttpServletRequest request )
    {
        Object springSecurityContext = request.getSession().getAttribute( "SPRING_SECURITY_CONTEXT" );
        if ( springSecurityContext != null )
        {
            SecurityContextImpl context = (SecurityContextImpl) springSecurityContext;
            Authentication authentication = context.getAuthentication();

            CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();

            if ( authentication != null && currentUserDetails == null )
            {
                SecurityContext newContext = SecurityContextHolder.createEmptyContext();
                newContext.setAuthentication( authentication );
                SecurityContextHolder.setContext( context );
            }
        }
    }

    private List<WebModule> getAccessibleAppMenu( String contextPath )
    {
        List<WebModule> modules = appMenuManager.getAccessibleWebModules();

        List<App> apps = appManager
            .getApps( contextPath )
            .stream()
            .filter( app -> app.getAppType() == AppType.APP && app.hasAppEntrypoint() && !app.isBundled() )
            .collect( Collectors.toList() );

        modules.addAll( apps.stream().map( WebModule::getModule ).collect( Collectors.toList() ) );

        return modules;
    }

    @GetMapping( produces = ContextUtils.CONTENT_TYPE_JSON )
    public ResponseEntity<List<App>> getApps( @RequestParam( required = false ) String key )
    {
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );
        String contextPath = contextService.getContextPath();

        List<App> apps = new ArrayList<>();

        if ( key != null )
        {
            App app = appManager.getApp( key, contextPath );

            if ( app == null )
            {
                return ResponseEntity.notFound().build();
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
        return ResponseEntity.ok( apps );
    }

    @PostMapping
    @PreAuthorize( "hasRole('ALL') or hasRole('M_dhis-web-app-management')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void installApp( @RequestParam( "file" ) MultipartFile file )
        throws IOException,
        WebMessageException
    {
        File tempFile = File.createTempFile( "IMPORT_", "_ZIP" );
        file.transferTo( tempFile );

        AppStatus status = appManager.installApp( tempFile, file.getOriginalFilename() );

        if ( !status.ok() )
        {
            String message = i18nManager.getI18n().getString( status.getMessage() );

            throw new WebMessageException( conflict( message ) );
        }
    }

    @PutMapping
    @PreAuthorize( "hasRole('ALL') or hasRole('M_dhis-web-app-management')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void reloadApps()
    {
        appManager.reloadApps();
    }

    @GetMapping( "/{app}/**" )
    public void renderApp( @PathVariable( "app" ) String app,
        HttpServletRequest request, HttpServletResponse response )
        throws IOException,
        WebMessageException
    {
        String contextPath = ContextUtils.getContextPath( request );
        App application = appManager.getApp( app, contextPath );

        // Get page requested
        String pageName = getUrl( request.getPathInfo(), app );

        if ( application == null )
        {
            throw new WebMessageException( notFound( "App '" + app + "' not found." ) );
        }

        if ( application.isBundled() )
        {
            String redirectPath = application.getBaseUrl() + "/" + pageName;

            log.info( String.format( "Redirecting to bundled app: %s", redirectPath ) );

            response.sendRedirect( redirectPath );
            return;
        }

        if ( !appManager.isAccessible( application ) )
        {
            throw new ReadAccessDeniedException( "You don't have access to application " + app + "." );
        }

        if ( application.getAppState() == AppStatus.DELETION_IN_PROGRESS )
        {
            throw new WebMessageException(
                conflict( "App '" + app + "' deletion is still in progress." ) );
        }

        log.debug( String.format( "App page name: '%s'", pageName ) );

        // Handling of 'manifest.webapp'
        if ( "manifest.webapp".equals( pageName ) )
        {
            // If request was for manifest.webapp, check for * and replace with
            // host
            if ( application.getActivities() != null && application.getActivities().getDhis() != null
                && "*".equals( application.getActivities().getDhis().getHref() ) )
            {
                log.debug( String.format( "Manifest context path: '%s'", contextPath ) );

                application.getActivities().getDhis().setHref( contextPath );
            }

            jsonMapper.writeValue( response.getOutputStream(), application );
        }
        // Any other page
        else
        {
            // Retrieve file
            Resource resource = appManager.getAppResource( application, pageName );

            if ( resource == null )
            {
                response.sendError( HttpServletResponse.SC_NOT_FOUND );
                return;
            }

            String filename = resource.getFilename();
            log.debug( String.format( "App filename: '%s'", filename ) );

            if ( new ServletWebRequest( request, response ).checkNotModified( resource.lastModified() ) )
            {
                response.setStatus( HttpServletResponse.SC_NOT_MODIFIED );
                return;
            }

            String mimeType = request.getSession().getServletContext().getMimeType( filename );

            if ( mimeType != null )
            {
                response.setContentType( mimeType );
            }

            response.setContentLengthLong( resource.contentLength() );
            response.setHeader( "Last-Modified", DateUtils.getHttpDateString( new Date( resource.lastModified() ) ) );

            StreamUtils.copyThenCloseInputStream( resource.getInputStream(), response.getOutputStream() );
        }
    }

    @DeleteMapping( "/{app}" )
    @PreAuthorize( "hasRole('ALL') or hasRole('M_dhis-web-app-management')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteApp( @PathVariable( "app" ) String app, @RequestParam( required = false ) boolean deleteAppData )
        throws WebMessageException
    {
        App appToDelete = appManager.getApp( app );
        if ( appToDelete == null )
        {
            throw new WebMessageException( notFound( "App does not exist: " + app ) );
        }

        if ( appToDelete.getAppState() == AppStatus.DELETION_IN_PROGRESS )
        {
            throw new WebMessageException( conflict( "App is already being deleted: " + app ) );
        }

        appManager.markAppToDelete( appToDelete );
        appManager.deleteApp( appToDelete, deleteAppData );
    }

    @SuppressWarnings( "unchecked" )
    @PostMapping( value = "/config", consumes = ContextUtils.CONTENT_TYPE_JSON )
    @PreAuthorize( "hasRole('ALL') or hasRole('M_dhis-web-app-management')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void setConfig( HttpServletRequest request )
        throws IOException,
        WebMessageException
    {
        Map<String, String> config = renderService.fromJson( request.getInputStream(), Map.class );

        if ( config == null )
        {
            throw new WebMessageException( conflict( "No config specified" ) );
        }
    }

    // --------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------

    private String getUrl( String path, String app )
    {
        String prefix = RESOURCE_PATH + "/" + app + "/";

        if ( path.startsWith( prefix ) )
        {
            path = path.substring( prefix.length() );
        }

        // if path is prefixed by any protocol, clear it out (this is to ensure
        // that only files inside app directory can be resolved)
        path = REGEX_REMOVE_PROTOCOL.matcher( path ).replaceAll( "" );

        return path;
    }
}
