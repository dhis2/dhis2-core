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
package org.hisp.dhis.webapi.security.config;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.system.velocity.VelocityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.util.HtmlUtils;

@OpenApi.Tags( { "user", "login" } )
@Controller
@SessionAttributes( "authorizationRequest" )
public class OAuth2ConfirmAccessController
{
    @Autowired
    @Qualifier( "org.hisp.dhis.system.velocity.VelocityManager" )
    VelocityManager velocityManager;

    @GetMapping( "/oauth/confirm_access" )
    public ModelAndView getAccessConfirmationB( Map<String, Object> model, HttpServletRequest request )
        throws Exception
    {
        Map<String, Object> vars = new HashMap<>();

        AuthorizationRequest authorizationRequest = (AuthorizationRequest) model.get( "authorizationRequest" );
        if ( authorizationRequest != null )
        {
            String clientId = authorizationRequest.getClientId();
            vars.put( "client_id", clientId );
        }

        String approvalContent = velocityManager.render( vars, "confirm_access" );

        if ( request.getAttribute( "_csrf" ) != null )
        {
            model.put( "_csrf", request.getAttribute( "_csrf" ) );
        }

        View approvalView = new View()
        {
            @Override
            public String getContentType()
            {
                return "text/html";
            }

            @Override
            public void render( Map<String, ?> model, HttpServletRequest request, HttpServletResponse response )
                throws Exception
            {
                response.setContentType( getContentType() );
                response.getWriter().append( approvalContent );
            }
        };

        return new ModelAndView( approvalView, model );
    }

    @GetMapping( "/oauth/error" )
    public ModelAndView handleError( HttpServletRequest request )
    {
        String errorSummary;

        // The error summary may contain malicious user input,
        // it needs to be escaped to prevent XSS
        Object error = request.getAttribute( "error" );
        if ( error instanceof OAuth2Exception )
        {
            OAuth2Exception oauthError = (OAuth2Exception) error;
            errorSummary = HtmlUtils.htmlEscape( oauthError.getSummary() );
        }
        else
        {
            errorSummary = "Unknown error";
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put( "error_summary", errorSummary );

        String errorContent = velocityManager.render( vars, "error" );

        View errorView = new View()
        {
            @Override
            public String getContentType()
            {
                return "text/html";
            }

            @Override
            public void render( Map<String, ?> model, HttpServletRequest request, HttpServletResponse response )
                throws Exception
            {
                response.setContentType( getContentType() );
                response.getWriter().append( errorContent );
            }
        };

        return new ModelAndView( errorView, new HashMap<>() );
    }
}
