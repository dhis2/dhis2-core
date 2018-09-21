package org.hisp.dhis.logging;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class LogManager implements ApplicationEventPublisherAware, ApplicationListener<LogEvent>
{
    private static final long serialVersionUID = 1L;

    private ApplicationEventPublisher publisher;

    @Override
    public void setApplicationEventPublisher( ApplicationEventPublisher publisher )
    {
        this.publisher = publisher;
    }

    public void log( Log log )
    {
        updateUsername( log );
        publisher.publishEvent( new LogEvent( this, log ) );
    }

    @Override
    public void onApplicationEvent( LogEvent event )
    {
        System.err.println( event.getLog() );
    }

    private void updateUsername( Log log )
    {
        if ( log.getUsername() != null )
        {
            return;
        }

        SecurityContext context = SecurityContextHolder.getContext();

        if ( context.getAuthentication() != null )
        {
            if ( context.getAuthentication().getPrincipal() instanceof String )
            {
                log.setUsername( (String) context.getAuthentication().getPrincipal() );
            }
        }
    }
}
