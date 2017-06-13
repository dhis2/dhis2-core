package org.hisp.dhis.webportal.module;

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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Torgeir Lorange Ostby
 * @version $Id: CurrentModuleDetectorFilter.java 6216 2008-11-06 18:06:42Z eivindwa $
 */
public class CurrentModuleDetectorFilter
    implements Filter
{
    private static final Log LOG = LogFactory.getLog( CurrentModuleDetectorFilter.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ModuleManager moduleManager;

    public void setModuleManager( ModuleManager moduleManager )
    {
        this.moduleManager = moduleManager;
    }

    // -------------------------------------------------------------------------
    // Filter
    // -------------------------------------------------------------------------

    @Override
    public void init( FilterConfig filterConfig )
        throws ServletException
    {
    }

    @Override
    public void destroy()
    {
    }

    @Override
    public void doFilter( ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain )
        throws IOException, ServletException
    {
        // ---------------------------------------------------------------------
        // Convert to HttpServletRequest and -Response
        // ---------------------------------------------------------------------

        if ( !(servletRequest instanceof HttpServletRequest) )
        {
            throw new ServletException( "Can only handle HttpServletRequests. Got: "
                + servletRequest.getClass().getName() );
        }

        HttpServletRequest request = (HttpServletRequest) servletRequest;

        // ---------------------------------------------------------------------
        // Detect requested module
        // ---------------------------------------------------------------------

        String actionURL = request.getServletPath();

        int endOfNamespace = actionURL.lastIndexOf( '/' );

        String namespace = actionURL.substring( 0, endOfNamespace );

        Module module = moduleManager.getModuleByNamespace( namespace );

        if ( module == null )
        {
            LOG.error( "Requesting a module which doesn't exist: '" + namespace + "' (" + actionURL + ")" );
        }
        else
        {
            moduleManager.setCurrentModule( module );
        }

        // ---------------------------------------------------------------------
        // Continue with next filter
        // ---------------------------------------------------------------------

        filterChain.doFilter( servletRequest, servletResponse );
    }
}
