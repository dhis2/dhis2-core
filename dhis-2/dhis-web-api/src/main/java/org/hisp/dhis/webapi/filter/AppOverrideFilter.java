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
package org.hisp.dhis.webapi.filter;

import static java.util.regex.Pattern.compile;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Austin McGee <austin@dhis2.org>
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AppOverrideFilter
    extends OncePerRequestFilter
{
    public static final String APP_PATH_PATTERN_STRING = "^/" + AppManager.BUNDLED_APP_PREFIX + "("
        + String.join( "|", AppManager.BUNDLED_APPS ) + ")/(.*)";

    public static final Pattern APP_PATH_PATTERN = compile( APP_PATH_PATTERN_STRING );

    private final AppManager appManager;

    private final ObjectMapper jsonMapper;

    @Override
    protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain chain )
        throws IOException,
        ServletException
    {
        String requestPath = request.getServletPath();
        String contextPath = ContextUtils.getContextPath( request );

        Matcher m = APP_PATH_PATTERN.matcher( requestPath );
        if ( m.find() )
        {
            String appName = m.group( 1 );
            String resourcePath = m.group( 2 );

            log.debug( "AppOverrideFilter :: Matched for path " + requestPath );

            App app = appManager.getApp( appName, contextPath );
            if ( app != null && app.getAppState() != AppStatus.DELETION_IN_PROGRESS )
            {
                log.debug( "AppOverrideFilter :: Overridden app " + appName + " found, serving override" );
                serveInstalledAppResource( app, resourcePath, request, response );

                return;
            }
            else
            {
                log.debug( "AppOverrideFilter :: App " + appName + " not found, falling back to bundled app" );
            }
        }

        chain.doFilter( request, response );
    }

    private void serveInstalledAppResource( App app, String resourcePath, HttpServletRequest request,
        HttpServletResponse response )
        throws IOException
    {
        log.debug( String.format( "Serving app resource: '%s'", resourcePath ) );

        // Handling of 'manifest.webapp'
        if ( "manifest.webapp".equals( resourcePath ) )
        {
            // If request was for manifest.webapp, check for * and replace with
            // host
            if ( app.getActivities() != null && app.getActivities().getDhis() != null
                && "*".equals( app.getActivities().getDhis().getHref() ) )
            {
                String contextPath = ContextUtils.getContextPath( request );
                log.debug( String.format( "Manifest context path: '%s'", contextPath ) );
                app.getActivities().getDhis().setHref( contextPath );
            }

            jsonMapper.writeValue( response.getOutputStream(), app );
        }
        else if ( "index.action".equals( resourcePath ) )
        {
            response.sendRedirect( app.getLaunchUrl() );
        }
        // Any other resource
        else
        {
            // Retrieve file
            Resource resource = appManager.getAppResource( app, resourcePath );
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

            String filename = resource.getFilename();
            log.debug( String.format( "App filename: '%s'", filename ) );

            String mimeType = request.getSession().getServletContext().getMimeType( filename );
            if ( mimeType != null )
            {
                response.setContentType( mimeType );
            }

            response.setContentLength( (int) resource.contentLength() );
            response.setHeader( "Last-Modified", DateUtils.getHttpDateString( new Date( resource.lastModified() ) ) );

            StreamUtils.copyThenCloseInputStream( resource.getInputStream(), response.getOutputStream() );
        }
    }
}
