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
package org.hisp.dhis.webapi.security.apikey;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.google.common.net.HttpHeaders;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public final class ApiTokenResolver
{
    private static final Pattern AUTHORIZATION_PATTERN = Pattern.compile( "^ApiToken (?<token>[a-zA-Z0-9-._~+/]+=*)$",
        Pattern.CASE_INSENSITIVE );

    public static final String HEADER_TOKEN_KEY_PREFIX = "apitoken";

    public static final String REQUEST_PARAMETER_NAME = "api_token";

    private boolean allowFormEncodedBodyParameter = false;

    private boolean allowUriQueryParameter = false;

    private String bearerTokenHeaderName = HttpHeaders.AUTHORIZATION;

    public String resolve( HttpServletRequest request )
    {
        String authorizationHeaderToken = resolveFromAuthorizationHeader( request );
        String parameterToken = resolveFromRequestParameters( request );

        if ( authorizationHeaderToken != null )
        {
            validateChecksum( authorizationHeaderToken );

            if ( parameterToken != null )
            {
                throw new ApiTokenAuthenticationException( ApiTokenErrors
                    .invalidRequest( "Found multiple api tokens in the request" ) );
            }
            return authorizationHeaderToken;
        }

        if ( parameterToken != null && isParameterTokenSupportedForRequest( request ) )
        {
            validateChecksum( parameterToken );

            return parameterToken;
        }

        return null;
    }

    private void validateChecksum( String token )
    {

    }

    public void setAllowFormEncodedBodyParameter( boolean allowFormEncodedBodyParameter )
    {
        this.allowFormEncodedBodyParameter = allowFormEncodedBodyParameter;
    }

    public void setAllowUriQueryParameter( boolean allowUriQueryParameter )
    {
        this.allowUriQueryParameter = allowUriQueryParameter;
    }

    public void setBearerTokenHeaderName( String bearerTokenHeaderName )
    {
        this.bearerTokenHeaderName = bearerTokenHeaderName;
    }

    private String resolveFromAuthorizationHeader( HttpServletRequest request )
    {
        String authorization = request.getHeader( this.bearerTokenHeaderName );
        if ( !StringUtils.startsWithIgnoreCase( authorization, HEADER_TOKEN_KEY_PREFIX ) )
        {
            return null;
        }

        Matcher matcher = AUTHORIZATION_PATTERN.matcher( authorization );
        if ( !matcher.matches() )
        {
            throw new ApiTokenAuthenticationException( ApiTokenErrors.invalidToken( "Api token is malformed" ) );
        }

        return matcher.group( "token" );
    }

    private static String resolveFromRequestParameters( HttpServletRequest request )
    {
        String[] values = request.getParameterValues( REQUEST_PARAMETER_NAME );
        if ( values == null || values.length == 0 )
        {
            return null;
        }

        if ( values.length == 1 )
        {
            return values[0];
        }

        throw new ApiTokenAuthenticationException(
            ApiTokenErrors.invalidRequest( "Found multiple Api tokens in the request" ) );
    }

    private boolean isParameterTokenSupportedForRequest( HttpServletRequest request )
    {
        return ((this.allowFormEncodedBodyParameter && "POST".equals( request.getMethod() ))
            || (this.allowUriQueryParameter && "GET".equals( request.getMethod() )));
    }
}
