package org.hisp.dhis.security.filter;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.hisp.dhis.security.AutomaticAccessProvider;

/**
 * This filter provides access to the system in situations where no users exists
 * in the database. Access providers can be registered with the accessProviders map.
 * The access provider to use must be defined as an init parameter in web.xml in
 * the various web modules.
 * 
 * @author Torgeir Lorange Ostby
 * @version $Id: AutomaticAccessFilter.java 3160 2007-03-24 20:15:06Z torgeilo $
 */
public class AutomaticAccessFilter
    implements Filter
{
    private static String automaticAccessType;

    private static boolean initialised;

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    private Map<String, AutomaticAccessProvider> accessProviders = new HashMap<>();

    public void setAccessProviders( Map<String, AutomaticAccessProvider> accessProviders )
    {
        this.accessProviders = accessProviders;
    }

    // -------------------------------------------------------------------------
    // Filter implementation
    // -------------------------------------------------------------------------

    @Override
    public void init( FilterConfig filterConfig )
        throws ServletException
    {
    }

    @Override
    public void doFilter( ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain )
        throws IOException, ServletException
    {
        if ( !initialised )
        {
            initialised = true;

            HttpServletRequest request = (HttpServletRequest) servletRequest;

            String automaticAccessType = request.getSession( true ).getServletContext().getInitParameter(
                "automaticAccessType" );

            if ( automaticAccessType != null )
            {
                if ( !accessProviders.containsKey( automaticAccessType ) )
                {
                    throw new IllegalArgumentException( "Unrecognised automatic access type: " + automaticAccessType );
                }
                else
                {
                    AutomaticAccessFilter.automaticAccessType = automaticAccessType;

                    accessProviders.get( automaticAccessType ).init();
                }
            }
        }

        if ( automaticAccessType != null )
        {
            accessProviders.get( automaticAccessType ).access();
        }

        filterChain.doFilter( servletRequest, servletResponse );
    }

    @Override
    public void destroy()
    {
    }
}
