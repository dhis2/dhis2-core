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

import static org.hisp.dhis.security.apikey.ApiKeyTokenGenerator.isValidTokenChecksum;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;

import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.security.apikey.ApiKeyTokenGenerator;

import com.google.common.net.HttpHeaders;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Setter
public final class ApiTokenResolver
{

    private static final Pattern AUTHORIZATION_PATTERN = Pattern.compile( "^ApiToken (?<token>[a-z0-9-._~+/]+=*)$",
        Pattern.CASE_INSENSITIVE );

    public static final String HEADER_TOKEN_KEY_PREFIX = "apitoken";

    public static final String REQUEST_PARAMETER_NAME = "api_token";

    public static final String CHECKSUM_VALIDATION_FAILED = "Checksum validation failed";

    private boolean allowFormEncodedBodyParameter = false;

    private boolean allowUriQueryParameter = false;

    private String bearerTokenHeaderName = HttpHeaders.AUTHORIZATION;

    private boolean isParameterTokenSupportedForRequest( HttpServletRequest request )
    {
        return (this.allowFormEncodedBodyParameter && "POST".equals( request.getMethod() ))
            || (this.allowUriQueryParameter && "GET".equals( request.getMethod() ));
    }

    @CheckForNull
    public String resolve( HttpServletRequest request )
    {
        char[] headerToken = extractTokenFromHeader( request );
        char[] parameterToken = extractTokenFromParameters( request );

        if ( validateHeaderToken( headerToken, parameterToken ) )
            return hashToken( headerToken );

        if ( validateParameterToken( request, parameterToken ) )
            return hashToken( parameterToken );

        return null;
    }

    private char[] extractTokenFromHeader( HttpServletRequest request )
    {
        String authorization = request.getHeader( this.bearerTokenHeaderName );
        if ( !StringUtils.startsWithIgnoreCase( authorization, HEADER_TOKEN_KEY_PREFIX ) )
        {
            return new char[0];
        }

        Matcher matcher = AUTHORIZATION_PATTERN.matcher( authorization );
        if ( !matcher.matches() )
        {
            throw new ApiTokenAuthenticationException( ApiTokenErrors.invalidRequest( "Api token is malformed" ) );
        }

        return matcher.group( "token" ).toCharArray();
    }

    private static boolean validateHeaderToken( char[] authorizationHeaderToken, char[] parameterToken )
    {
        if ( authorizationHeaderToken.length > 0 )
        {
            checksum( authorizationHeaderToken );

            if ( parameterToken.length > 0 )
            {
                throw new ApiTokenAuthenticationException( ApiTokenErrors
                    .invalidRequest( "Found multiple tokens in the request" ) );
            }
            return true;
        }

        return false;
    }

    private static char[] extractTokenFromParameters( HttpServletRequest request )
    {
        String[] values = request.getParameterValues( REQUEST_PARAMETER_NAME );
        if ( values == null || values.length == 0 )
        {
            return new char[0];
        }

        if ( values.length == 1 )
        {
            return values[0].toCharArray();
        }

        throw new ApiTokenAuthenticationException(
            ApiTokenErrors.invalidRequest( "Found multiple Api tokens in the request" ) );
    }

    private boolean validateParameterToken( HttpServletRequest request, char[] parameterToken )
    {
        if ( parameterToken.length > 0 && isParameterTokenSupportedForRequest( request ) )
        {
            checksum( parameterToken );
            return true;
        }
        return false;
    }

    private static void checksum( char[] token )
    {
        try
        {
            if ( !isValidTokenChecksum( token ) )
            {
                throw new ApiTokenAuthenticationException( ApiTokenErrors
                    .invalidRequest( CHECKSUM_VALIDATION_FAILED ) );
            }
        }
        catch ( ApiTokenAuthenticationException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new ApiTokenAuthenticationException( ApiTokenErrors
                .invalidRequest( CHECKSUM_VALIDATION_FAILED ) );
        }
    }

    private static String hashToken( char[] tokenInHeader )
    {
        try
        {
            return ApiKeyTokenGenerator.hashToken( tokenInHeader );
        }
        catch ( Exception e )
        {
            throw new ApiTokenAuthenticationException( ApiTokenErrors
                .invalidRequest( "Could not hash token" ) );
        }
    }
}
