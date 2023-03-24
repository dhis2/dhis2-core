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
package org.hisp.dhis.security.apikey;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DefaultDhisConfigurationProvider;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@WebListener
public class DhisHttpSessionListener implements HttpSessionListener
{
    public static final String JLI_SESSION_VARIABLE = "JLI";

    @Override
    public void sessionCreated( HttpSessionEvent sessionEvent )
    {
        log.debug( "-------Session Created--------" );
        DhisConfigurationProvider singleton = DefaultDhisConfigurationProvider.getInstance();
        if ( singleton != null )
        {
            if ( sessionEvent == null || sessionEvent.getSession() == null )
            {
                log.error( "Session is null in DhisHttpSessionListener" );
                return;
            }

            HttpSession session = sessionEvent.getSession();
            session.setAttribute( JLI_SESSION_VARIABLE, Boolean.TRUE );

            try
            {
                String property = singleton.getProperty( ConfigurationKey.SYSTEM_SESSION_TIMEOUT );
                session.setMaxInactiveInterval( Integer.parseInt( property ) );
            }
            catch ( Exception e )
            {
                session.setMaxInactiveInterval(
                    Integer.parseInt( ConfigurationKey.SYSTEM_SESSION_TIMEOUT.getDefaultValue() ) );
                // An exception not caught here could cause the request to fail completely, so we catch all.
                log.error( "Could not parse session timeout from config" );
            }
        }
    }

    @Override
    public void sessionDestroyed( HttpSessionEvent sessionEvent )
    {
        log.debug( "-------Session Destroyed--------" );
    }
}
