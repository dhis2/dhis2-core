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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.security.authority.SystemAuthoritiesProvider;
import org.hisp.dhis.user.UserService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Abstract automatic access provider class. Delegates methods initialise() and 
 * access() to implementing classes.
 * 
 * @author Torgeir Lorange Ostby
 * @version $Id: AbstractAutomaticAccessProvider.java 6352 2008-11-20 15:49:52Z larshelg $
 */
public abstract class AbstractAutomaticAccessProvider
    implements AutomaticAccessProvider
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    protected UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    protected SystemAuthoritiesProvider systemAuthoritiesProvider;

    public void setSystemAuthoritiesProvider( SystemAuthoritiesProvider systemAuthoritiesProvider )
    {
        this.systemAuthoritiesProvider = systemAuthoritiesProvider;
    }

    // -------------------------------------------------------------------------
    // Overrides
    // -------------------------------------------------------------------------

    protected abstract void initialise();
    
    @Override
    public final void init()
    {
        if ( isEnabled() )
        {
            initialise();
        }
    }

    // -------------------------------------------------------------------------
    // Support methods
    // -------------------------------------------------------------------------

    private boolean isEnabled()
    {
        return ( userService.getAllUsers().size() == 0);
    }

    protected Collection<String> getAuthorities()
    {
        return systemAuthoritiesProvider.getSystemAuthorities();
    }

    protected Collection<GrantedAuthority> getGrantedAuthorities()
    {
        Collection<String> systemAuthorities = getAuthorities();

        Set<GrantedAuthority> grantedAuthorities = new HashSet<>( systemAuthorities.size() );

        for ( String authority : systemAuthorities )
        {
            grantedAuthorities.add( new SimpleGrantedAuthority( authority ) );
        }

        return grantedAuthorities;
    }
}
