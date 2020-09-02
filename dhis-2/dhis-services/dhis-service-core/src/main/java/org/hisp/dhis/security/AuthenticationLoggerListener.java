package org.hisp.dhis.security;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.util.ClassUtils;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public class AuthenticationLoggerListener
    implements ApplicationListener<AbstractAuthenticationEvent>
{
    public void onApplicationEvent( AbstractAuthenticationEvent event )
    {
        if ( log.isWarnEnabled() )
        {
            final StringBuilder builder = new StringBuilder();
            builder.append( "Authentication event " );
            builder.append( ClassUtils.getShortName( event.getClass() ) );
            builder.append( ": " );
            builder.append( event.getAuthentication().getName() );

            Object details = event.getAuthentication().getDetails();

            if ( details != null &&
                ForwardedIpAwareWebAuthenticationDetails.class.isAssignableFrom( details.getClass() ) )
            {
                ForwardedIpAwareWebAuthenticationDetails authDetails = (ForwardedIpAwareWebAuthenticationDetails) details;
                String ip = authDetails.getIp();

                builder.append( "; ip: " );
                builder.append( ip );

                String sessionId = authDetails.getSessionId();
                if ( sessionId != null )
                {
                    HashCode hash = Hashing.sha256().newHasher().putString( sessionId, Charsets.UTF_8 ).hash();
                    builder.append( " sessionId: " );
                    builder.append( hash.toString() );
                }

            }

            if ( event instanceof AbstractAuthenticationFailureEvent )
            {
                builder.append( "; exception: " );
                builder.append( ((AbstractAuthenticationFailureEvent) event).getException().getMessage() );
            }

            log.warn( builder.toString() );
        }
    }
}
