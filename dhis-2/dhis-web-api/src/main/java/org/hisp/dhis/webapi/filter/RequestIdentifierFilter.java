/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.filter;

import static org.hisp.dhis.external.conf.ConfigurationKey.LOGGING_REQUEST_ID_ENABLED;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * This filter places an hashed version of the Session ID in the Log4j Mapped
 * Diagnostic Context (MDC) of log4j. The session id is then logged in all log
 * statements and can be used to correlate different requests.
 *
 * @author Luciano Fiandesio
 */
@Slf4j
@Component
public class RequestIdentifierFilter
    extends OncePerRequestFilter
{
    private static final String SESSION_ID_KEY = "sessionId";

    /**
     * The hash algorithm to use.
     */
    private static final String HASH_ALGO = "SHA-256";

    private static final String IDENTIFIER_PREFIX = "ID";

    private final boolean enabled;

    public RequestIdentifierFilter( DhisConfigurationProvider dhisConfig )
    {
        this.enabled = dhisConfig.isEnabled( LOGGING_REQUEST_ID_ENABLED );
    }

    @Override
    protected void doFilterInternal( HttpServletRequest req, HttpServletResponse res, FilterChain chain )
        throws ServletException,
        IOException
    {
        if ( enabled )
        {
            try
            {
                MDC.put( SESSION_ID_KEY, IDENTIFIER_PREFIX + hashToBase64( req.getSession().getId() ) );

            }
            catch ( NoSuchAlgorithmException e )
            {
                log.error( String.format( "Invalid Hash algorithm provided (%s)", HASH_ALGO ), e );
            }
        }

        chain.doFilter( req, res );
    }

    private String hashToBase64( String sessionId )
        throws NoSuchAlgorithmException
    {
        byte[] data = sessionId.getBytes();
        MessageDigest digester = MessageDigest.getInstance( HASH_ALGO );
        digester.update( data );
        return Base64.getEncoder().encodeToString( digester.digest() );
    }
}
