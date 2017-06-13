package org.hisp.dhis.security;

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

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * This access provider will put an Authentication object with all GrantedAuthorities
 * in the SecurityContext in any case. This means that any user will be authenticated
 * and the login effectively bypassed.
 *
 * @author Torgeir Lorange Ostby
 * @version $Id: GhostAutomaticAccessProvider.java 3160 2007-03-24 20:15:06Z torgeilo $
 */
public class GhostAutomaticAccessProvider
    extends AbstractAutomaticAccessProvider
{
    private Authentication authentication;

    // -------------------------------------------------------------------------
    // AdminAccessManager implementation
    // -------------------------------------------------------------------------

    @Override
    public void initialise()
    {
        String username = "ghost_admin";
        String password = "";

        UserDetails user = new User( username, password, true, true, true, true,
            getGrantedAuthorities() );

        authentication = new UsernamePasswordAuthenticationToken( user, user.getPassword(), user.getAuthorities() );
    }

    @Override
    public void access()
    {
        if ( authentication != null && SecurityContextHolder.getContext().getAuthentication() == null )
        {
            SecurityContextHolder.getContext().setAuthentication( authentication );
        }
    }
}
