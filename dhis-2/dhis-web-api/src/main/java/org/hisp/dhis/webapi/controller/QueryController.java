/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import lombok.Value;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Supports shortening of long API request URIs. This is achieved by accepting
 * the long URI in the body of a POST request and creating a deterministic
 * shortened endpoint for subsequent GET requests which are forwarded to the
 * original controller.
 *
 * @author Austin McGee
 */
@RequestMapping( QueryController.RESOURCE_PATH )
@Controller
public class QueryController
{
    public static final String RESOURCE_PATH = "/query";

    private static final String ALIAS_ROOT = "/api/query/alias";

    private static final Pattern MULTIPLE_FORWARD_SLASH_PATTERN = Pattern.compile( "/+" );

    /* Set an upper bound url size to prevent abuse */
    private static final int MAX_TARGET_LENGTH = 16 * 1024;

    private final RenderService renderService;

    private final Cache<String> aliasCache;

    public QueryController( RenderService renderService, CacheProvider cacheProvider )
    {
        this.renderService = renderService;
        this.aliasCache = cacheProvider.createQueryAliasCache();
    }

    @PostMapping( value = "/alias", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE )
    public @ResponseBody QueryAlias postQueryAlias( HttpServletRequest request,
        @RequestBody String bodyString )
        throws BadRequestException
    {
        final String target = parseTargetFromRequestBody( bodyString );

        return createAlias( target, request );
    }

    @PostMapping( value = "/alias/redirect", consumes = APPLICATION_JSON_VALUE )
    public RedirectView redirectQueryAlias( HttpServletRequest request, @RequestBody String bodyString )
        throws BadRequestException
    {
        final String target = parseTargetFromRequestBody( bodyString );
        final QueryAlias alias = createAlias( target, request );

        return new RedirectView( alias.getHref(), false, false );
    }

    @GetMapping( "/alias/{hash}" )
    public String getQueryAlias( @PathVariable( "hash" ) String hash )
        throws NotFoundException
    {
        Optional<String> targetUrl = aliasCache.get( hash );
        if ( targetUrl.isPresent() )
        {
            return "forward:" + targetUrl.get();
        }
        throw new NotFoundException( "No query alias found with this hash id, it may have expired." );
    }

    private String parseTargetFromRequestBody( String bodyString )
        throws BadRequestException
    {
        String target = null;
        try
        {
            Map<String, String> map = renderService.fromJson( bodyString, Map.class );

            if ( map != null )
            {
                target = map.get( "target" );
            }
        }
        catch ( Exception e )
        {
            throw new BadRequestException( "Request body must be a valid JSON object" );
        }

        if ( target == null )
        {
            throw new BadRequestException( "Alias must contain a 'target' property" );
        }

        if ( !target.startsWith( "/api/" ) )
        {
            throw new BadRequestException( "Target must start with /api/" );
        }

        return target;
    }

    private QueryAlias createAlias( String target, HttpServletRequest request )
        throws BadRequestException
    {
        if ( target.length() > MAX_TARGET_LENGTH )
        {
            throw new BadRequestException( "Target url exceeds maximum length" );
        }

        String alias = CodecUtils.sha1Hex( target );
        aliasCache.put( alias, target );

        String contextPath = ContextUtils.getContextPath( request );
        String path = replaceDuplicateSlashes( String.join( "/", ALIAS_ROOT, alias ) );
        String href = constructAliasHref( path, contextPath );

        return new QueryAlias( alias, path, href, target );
    }

    private static String constructAliasHref( String path, String contextPath )
    {
        String scheme = contextPath.substring( 0, contextPath.indexOf( "://" ) + 3 );
        String fullPath = String.join( "/", contextPath.substring( scheme.length() ), path );

        return scheme + replaceDuplicateSlashes( fullPath );
    }

    private static String replaceDuplicateSlashes( String input )
    {
        Matcher matcher = MULTIPLE_FORWARD_SLASH_PATTERN.matcher( input );
        return matcher.replaceAll( "/" );
    }

    @Value
    public static class QueryAlias
    {
        @JsonProperty
        String id;

        @JsonProperty
        String path;

        @JsonProperty
        String href;

        @JsonProperty
        String target;
    }
}
