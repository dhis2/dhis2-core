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
package org.hisp.dhis.webapi.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
public class DefaultContextService implements ContextService
{
    private static Pattern API_VERSION = Pattern.compile( "(/api/(\\d+)?/)" );

    @Override
    public String getServletPath()
    {
        return getContextPath() + getRequest().getServletPath();
    }

    @Override
    public String getContextPath()
    {
        HttpServletRequest request = getRequest();
        StringBuilder builder = new StringBuilder();
        String xForwardedProto = request.getHeader( "X-Forwarded-Proto" );
        String xForwardedPort = request.getHeader( "X-Forwarded-Port" );

        if ( xForwardedProto != null
            && (xForwardedProto.equalsIgnoreCase( "http" ) || xForwardedProto.equalsIgnoreCase( "https" )) )
        {
            builder.append( xForwardedProto );
        }
        else
        {
            builder.append( request.getScheme() );
        }

        builder.append( "://" ).append( request.getServerName() );

        int port;

        try
        {
            port = Integer.parseInt( xForwardedPort );
        }
        catch ( NumberFormatException e )
        {
            port = request.getServerPort();
        }

        if ( port != 80 && port != 443 )
        {
            builder.append( ":" ).append( port );
        }

        builder.append( request.getContextPath() );

        return builder.toString();
    }

    @Override
    public String getApiPath()
    {
        HttpServletRequest request = getRequest();
        Matcher matcher = API_VERSION.matcher( request.getRequestURI() );
        String version = "";

        if ( matcher.find() )
        {
            version = "/" + matcher.group( 2 );
        }

        return getServletPath() + version;
    }

    @Override
    public HttpServletRequest getRequest()
    {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    @Override
    public List<String> getParameterValues( String name )
    {
        return Optional.ofNullable( name )
            .map( this::getRequestParameterValues )
            .orElse( Collections.emptyList() );
    }

    private List<String> getRequestParameterValues( String paramName )
    {
        String[] parameterValues = getRequest().getParameterValues( paramName );

        if ( parameterValues != null )
        {
            return Arrays.stream( parameterValues )
                .distinct()
                .collect( Collectors.toList() );
        }

        return Collections.emptyList();
    }

    @Override
    public Map<String, List<String>> getParameterValuesMap()
    {
        return getRequest().getParameterMap().entrySet().stream()
            .collect( Collectors.toMap(
                Map.Entry::getKey,
                stringEntry -> Lists.newArrayList( stringEntry.getValue() ) ) );
    }

    @Override
    public List<String> getFieldsFromRequestOrAll()
    {
        return getFieldsFromRequestOrElse( ":all" );
    }

    @Override
    public List<String> getFieldsFromRequestOrElse( String fields )
    {
        return Optional.ofNullable( getParameterValues( "fields" ) )
            .filter( CollectionUtils::isNotEmpty )
            .orElse( Collections.singletonList( fields ) );
    }
}
