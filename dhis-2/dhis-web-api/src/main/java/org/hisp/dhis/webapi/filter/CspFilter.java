/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_ENABLED;
import static org.hisp.dhis.security.utils.CspUtils.DEFAULT_HEADER_VALUE;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.utils.CspUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class CspFilter
    extends OncePerRequestFilter
{
    public static final String CONTENT_SECURITY_POLICY_HEADER_NAME = "Content-Security-Policy";

    public static final String FRAME_ANCESTORS_STATIC_CONTENT_CSP = "frame-ancestors 'self' ";

    private final boolean enabled;

    private final List<Pattern> externalContentInsideApiPatterns;

    ConfigurationService configurationService;

    public CspFilter( DhisConfigurationProvider dhisConfig, ConfigurationService configurationService )
    {
        this.enabled = dhisConfig.isEnabled( CSP_ENABLED );
        this.externalContentInsideApiPatterns = CspUtils.EXTERNAL_STATIC_CONTENT_URL_PATTERNS;
        this.configurationService = configurationService;
    }

    @Override
    protected void doFilterInternal( HttpServletRequest req, HttpServletResponse res, FilterChain chain )
        throws ServletException,
        IOException
    {
        if ( !enabled )
        {
            chain.doFilter( req, res );
            return;
        }

        if ( isStaticContentInsideApi( req.getRequestURI() ) )
        {
            setFrameAncestorsCspRule( res );
            chain.doFilter( req, res );
            return;
        }

        if ( isExternalContentInsideApi( req.getRequestURL().toString() ) )
        {
            // Add default CSP header for external content
            res.addHeader( CONTENT_SECURITY_POLICY_HEADER_NAME, DEFAULT_HEADER_VALUE );
            chain.doFilter( req, res );
            return;
        }

        if ( isRegularApi( req.getRequestURI() ) )
        {
            chain.doFilter( req, res );
            return;
        }

        // Rest must be static content
        setFrameAncestorsCspRule( res );
        chain.doFilter( req, res );
    }

    private void setFrameAncestorsCspRule( HttpServletResponse res )
    {
        Set<String> corsWhitelist = configurationService.getConfiguration().getCorsWhitelist();
        if ( !corsWhitelist.isEmpty() )
        {
            String corsAllowedOrigins = String.join( " ", corsWhitelist );
            res.addHeader( CONTENT_SECURITY_POLICY_HEADER_NAME,
                FRAME_ANCESTORS_STATIC_CONTENT_CSP + corsAllowedOrigins );
        }
    }

    private boolean isRegularApi( String requestURI )
    {
        return CspUtils.EVERYTHING_START_WITH_API.matcher( requestURI ).find();
    }

    private boolean isStaticContentInsideApi( String requestURI )
    {
        for ( Pattern pattern : CspUtils.STATIC_RESOURCES_IN_API_URL_PATTERNS )
        {
            if ( pattern.matcher( requestURI ).matches() )
            {
                return true;
            }
        }
        return false;
    }

    // Returns false if URI matches one of the regexp patterns in the list
    private boolean isExternalContentInsideApi( String requestURI )
    {
        for ( Pattern pattern : externalContentInsideApiPatterns )
        {
            if ( pattern.matcher( requestURI ).matches() )
            {
                return true;
            }
        }
        return false;
    }
}
