package org.hisp.dhis.servlet.filter;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.system.util.DateUtils;
import org.joda.time.DateTime;

/**
 * Filter which adds max expiry cache headers to responses. Can be configured in
 * web.xml with:
 * 
 * <filter>
 *   <filter-name>HttpMaxCacheFilter</filter-name>
 *   <filter-class>org.hisp.dhis.servlet.filter.HttpMaxCacheFilter</filter-class>
 *   <init-param>
 *     <param-name>regex</param-name>
 *     <param-value>(\.js|\.css|\.gif|\.woff|\.ttf|\.eot|\.ico|(/dhis-web-commons/|/images/|/icons/).*\.png)$</param-value>
 *   </init-param>
 * </filter>
 *  
 * <filter-mapping>
 *   <filter-name>HttpMaxCacheFilter</filter-name>
 *   <url-pattern>/*</url-pattern>
 * </filter-mapping>
 * 
 * @author Lars Helge Overland
 */
public class HttpMaxCacheFilter
    implements Filter
{
    private static final String PARAM_REGEX = "regex";
    private static final String HEADER_EXPIRES = "Expires";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String HEADER_MAX_CACHE = "public, max-age=31536000";
    
    private Pattern PATTERN = null;
    
    @Override
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
        throws IOException, ServletException
    {
        if ( PATTERN != null && request != null && request instanceof HttpServletRequest &&
            response != null && response instanceof HttpServletResponse )
        {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            String uri = httpRequest.getRequestURI();
            
            if ( uri != null && PATTERN.matcher( uri ).find() )
            {
                DateTime expiry = new DateTime().plusYears( 1 );
                
                httpResponse.setHeader( HEADER_EXPIRES, DateUtils.getHttpDateString( expiry.toDate() ) );
                httpResponse.setHeader( HEADER_CACHE_CONTROL, HEADER_MAX_CACHE );                
            }
        }
        
        chain.doFilter( request, response );
    }

    @Override
    public void init( FilterConfig config )
        throws ServletException
    {
        String regex = config.getInitParameter( PARAM_REGEX );
        
        if ( regex == null )
        {
            throw new IllegalStateException( "Init parameter 'regex' must be defined for HttpMaxCacheFilter" );
        }
        
        PATTERN = Pattern.compile( regex );
    }

    @Override
    public void destroy()
    {
    }
}

