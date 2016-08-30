package org.hisp.dhis.security;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.dispatcher.Dispatcher;
import org.hisp.dhis.security.authority.RequiredAuthoritiesProvider;

import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.opensymphony.xwork2.config.Configuration;
import com.opensymphony.xwork2.config.entities.ActionConfig;
import com.opensymphony.xwork2.config.entities.PackageConfig;

/**
 * @author Torgeir Lorange Ostby
 * @version $Id: SpringSecurityActionAccessResolver.java 3160 2007-03-24 20:15:06Z torgeilo $
 */
public class SpringSecurityActionAccessResolver
    implements ActionAccessResolver
{
    private static final Log log = LogFactory.getLog( SpringSecurityActionAccessResolver.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private RequiredAuthoritiesProvider requiredAuthoritiesProvider;

    public void setRequiredAuthoritiesProvider( RequiredAuthoritiesProvider requiredAuthoritiesProvider )
    {
        this.requiredAuthoritiesProvider = requiredAuthoritiesProvider;
    }

    private AccessDecisionManager accessDecisionManager;

    public void setAccessDecisionManager( AccessDecisionManager accessDecisionManager )
    {
        this.accessDecisionManager = accessDecisionManager;
    }

    // -------------------------------------------------------------------------
    // ActionAccessResolver implementation
    // -------------------------------------------------------------------------

    @Override
    public boolean hasAccess( String module, String name )
    {
        // ---------------------------------------------------------------------
        // Get ObjectDefinitionSource
        // ---------------------------------------------------------------------

        Configuration config = Dispatcher.getInstance().getConfigurationManager().getConfiguration();

        PackageConfig packageConfig = config.getPackageConfig( module );

        if ( packageConfig == null )
        {
            throw new IllegalArgumentException( "Module doesn't exist: '" + module + "'" );
        }

        ActionConfig actionConfig = packageConfig.getActionConfigs().get( name );

        if ( actionConfig == null )
        {
            throw new IllegalArgumentException( "Module " + module + " doesn't have an action named: '" + name + "'" );
        }

        SecurityMetadataSource securityMetadataSource = requiredAuthoritiesProvider
            .createSecurityMetadataSource( actionConfig );

        // ---------------------------------------------------------------------
        // Test access
        // ---------------------------------------------------------------------

        SecurityContext securityContext = SecurityContextHolder.getContext();

        Authentication authentication = securityContext.getAuthentication();

        try
        {
            if ( securityMetadataSource.getAttributes( actionConfig ) != null )
            {
                if ( authentication == null || !authentication.isAuthenticated() )
                {
                    return false;
                }

                accessDecisionManager.decide( authentication, actionConfig, securityMetadataSource
                    .getAttributes( actionConfig ) );
            }

            log.debug( "Access to [" + module + ", " + name + "]: TRUE" );

            return true;
        }
        catch ( AccessDeniedException e )
        {
            log.debug( "Access to [" + module + ", " + name + "]: FALSE (access denied)" );

            return false;
        }
        catch ( InsufficientAuthenticationException e )
        {
            log.debug( "Access to [" + module + ", " + name + "]: FALSE (insufficient authentication)" );

            return false;
        }
    }
}
