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

import org.hisp.dhis.i18n.I18nService;
import org.hisp.dhis.i18n.ui.locale.UserSettingLocaleManager;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class AppCacheFilter implements Filter
{
    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SystemService systemService;

    @Autowired
    private UserSettingLocaleManager localeManager;

    @Autowired
    private I18nService i18nService;

    @Override
    public void doFilter( ServletRequest req, ServletResponse res, FilterChain chain ) throws IOException, ServletException
    {
        if ( req != null && req instanceof HttpServletRequest && res != null && res instanceof HttpServletResponse )
        {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;

            PrintWriter writer = response.getWriter();
            CharResponseWrapper responseWrapper = new CharResponseWrapper( response );

            chain.doFilter( request, responseWrapper );
            responseWrapper.setContentType( "text/cache-manifest" );

            SystemInfo systemInfo = systemService.getSystemInfo();

            writer.print( responseWrapper.toString() );
            writer.println( "# DHIS2 " + systemInfo.getVersion() + " r" + systemInfo.getRevision() );
            writer.println( "# User: " + currentUserService.getCurrentUsername() );
            writer.println( "# User UI Language: " + localeManager.getCurrentLocale() );
            writer.println( "# User DB Language: " + i18nService.getCurrentLocale() );
            writer.println( "# Calendar: " + systemInfo.getCalendar() );
        }
    }

    @Override
    public void init( FilterConfig filterConfig ) throws ServletException
    {
    }

    @Override
    public void destroy()
    {
    }
}

class CharResponseWrapper extends HttpServletResponseWrapper
{
    private CharArrayWriter output;

    public String toString()
    {
        return output.toString();
    }

    public CharResponseWrapper( HttpServletResponse response )
    {
        super( response );
        output = new CharArrayWriter();
    }

    @Override
    public PrintWriter getWriter()
    {
        return new PrintWriter( output );
    }
}
