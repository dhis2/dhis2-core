/*
 * Copyright (c) 2004-2004-2020, University of Oslo
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
package org.hisp.dhis.commons.jsonfiltering.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.commons.jsonfiltering.context.provider.AbstractJsonFilteringContextProvider;
import org.hisp.dhis.commons.jsonfiltering.name.AnyDeepName;
import org.hisp.dhis.commons.jsonfiltering.parser.JsonFilteringParser;

import com.google.common.base.MoreObjects;

/**
 * Custom context provider that gets the filter expression from the request.
 */
public class RequestJsonFilteringContextProvider extends AbstractJsonFilteringContextProvider
{

    private final String defaultFilter;

    private final String filterParam;

    public RequestJsonFilteringContextProvider( String filterParam, String defaultFilter )
    {
        this( new JsonFilteringParser(), filterParam, defaultFilter );

    }

    public RequestJsonFilteringContextProvider( JsonFilteringParser parser, String filterParam, String defaultFilter )
    {
        super( parser );
        this.filterParam = filterParam;
        this.defaultFilter = defaultFilter;
    }

    @Override
    protected String getFilter( Class beanClass )
    {
        HttpServletRequest request = getRequest();

        FilterCache cache = FilterCache.getOrCreate( request );
        String filter = cache.get( beanClass );

        if ( filter == null )
        {
            filter = MoreObjects.firstNonNull( getFilter( request ), defaultFilter );
            filter = customizeFilter( filter, request, beanClass );
            cache.put( beanClass, filter );
        }

        return filter;
    }

    @Override
    public boolean isFilteringEnabled()
    {
        HttpServletRequest request = getRequest();

        if ( request == null )
        {
            return false;
        }

        HttpServletResponse response = getResponse();

        if ( response == null )
        {
            return false;
        }

        return isFilteringEnabled( request, response );
    }

    protected boolean isFilteringEnabled( HttpServletRequest request, HttpServletResponse response )
    {
        int status = getResponseStatusCode( request, response );

        if ( !isSuccessStatusCode( status ) )
        {
            return false;
        }

        String filter = getFilter( request );

        if ( AnyDeepName.ID.equals( filter ) )
        {
            return false;
        }

        if ( filter != null )
        {
            return true;
        }

        if ( AnyDeepName.ID.equals( defaultFilter ) )
        {
            return false;
        }

        return defaultFilter != null;
    }

    protected int getResponseStatusCode( HttpServletRequest request, HttpServletResponse response )
    {
        return response.getStatus();
    }

    protected boolean isSuccessStatusCode( int status )
    {
        return status >= HttpServletResponse.SC_OK && status < HttpServletResponse.SC_MULTIPLE_CHOICES;
    }

    protected String getFilter( HttpServletRequest request )
    {
        return request.getParameter( filterParam );
    }

    protected HttpServletRequest getRequest()
    {
        return JsonFilteringRequestHolder.getRequest();
    }

    protected HttpServletResponse getResponse()
    {
        return JsonFilteringResponseHolder.getResponse();
    }

    protected String customizeFilter( String filter, HttpServletRequest request, Class beanClass )
    {
        return customizeFilter( filter, beanClass );
    }

    protected String customizeFilter( String filter, Class beanClass )
    {
        return filter;
    }

    private static class FilterCache
    {
        public static final String REQUEST_KEY = FilterCache.class.getName();

        @SuppressWarnings( "RedundantStringConstructorCall" )
        private static final String NULL = new String();

        private final Map<Class, String> map = new HashMap<>();

        public static FilterCache getOrCreate( HttpServletRequest request )
        {
            FilterCache cache = (FilterCache) request.getAttribute( REQUEST_KEY );

            if ( cache == null )
            {
                cache = new FilterCache();
                request.setAttribute( REQUEST_KEY, cache );
            }

            return cache;
        }

        @SuppressWarnings( "StringEquality" )
        public String get( Class key )
        {
            String value = map.get( key );

            if ( value == NULL )
            {
                value = null;
            }

            return value;
        }

        public void put( Class key, String value )
        {
            if ( value == null )
            {
                value = NULL;
            }

            map.put( key, value );
        }

        public void remove( Class key )
        {
            map.remove( key );
        }

        public void clear()
        {
            map.clear();
        }

    }
}
