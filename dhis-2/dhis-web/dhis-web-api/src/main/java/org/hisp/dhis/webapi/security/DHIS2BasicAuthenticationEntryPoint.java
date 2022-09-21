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
package org.hisp.dhis.webapi.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.render.RenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import com.google.common.base.MoreObjects;
import com.google.common.net.HttpHeaders;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class DHIS2BasicAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint
{
    @Autowired
    private RenderService renderService;

    /**
     * @param loginFormUrl URL where the login page can be found. Should either
     *        be relative to the web-app context path (include a leading
     *        {@code /}) or an absolute URL.
     */
    public DHIS2BasicAuthenticationEntryPoint( String loginFormUrl )
    {
        super( loginFormUrl );
    }

    @Override
    public void commence( HttpServletRequest request, HttpServletResponse response,
        AuthenticationException authException )
        throws IOException,
        ServletException
    {
        String acceptHeader = MoreObjects.firstNonNull( request.getHeader( HttpHeaders.ACCEPT ), "" );
        String requestWithHeader = MoreObjects.firstNonNull( request.getHeader( HttpHeaders.X_REQUESTED_WITH ), "" );
        String authorizationHeader = MoreObjects.firstNonNull( request.getHeader( HttpHeaders.AUTHORIZATION ), "" );

        if ( "XMLHttpRequest".equals( requestWithHeader ) || authorizationHeader.contains( "Basic" ) )
        {
            String message = "Unauthorized";

            if ( ExceptionUtils.indexOfThrowable( authException, LockedException.class ) != -1 )
            {
                message = "Account locked";
            }

            response.setStatus( HttpServletResponse.SC_UNAUTHORIZED );

            if ( acceptHeader.contains( MediaType.APPLICATION_XML_VALUE ) )
            {
                response.setContentType( MediaType.APPLICATION_XML_VALUE );
                renderService.toXml( response.getOutputStream(), WebMessageUtils.unathorized( message ) );
            }
            else
            {
                response.setContentType( MediaType.APPLICATION_JSON_VALUE );
                renderService.toJson( response.getOutputStream(), WebMessageUtils.unathorized( message ) );
            }

            return;
        }

        super.commence( request, response, authException );
    }
}
