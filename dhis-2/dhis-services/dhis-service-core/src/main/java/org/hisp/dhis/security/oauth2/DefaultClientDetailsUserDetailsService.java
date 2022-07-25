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
package org.hisp.dhis.security.oauth2;

import lombok.AllArgsConstructor;

import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.NoSuchClientException;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service( "defaultClientDetailsUserDetailsService" )
@AllArgsConstructor
public class DefaultClientDetailsUserDetailsService implements UserDetailsService
{
    private final DefaultClientDetailsService clientDetailsService;

    private final PasswordEncoder passwordEncoder;

    private final UserService userService;

    public UserDetails loadUserByUsername( String username )
    {
        ClientDetails clientDetails = getClientDetails( username );

        String clientSecret = clientDetails.getClientSecret();
        if ( clientSecret == null || clientSecret.trim().length() == 0 )
        {
            clientSecret = passwordEncoder.encode( "" );
        }

        User user = new User();
        user.setUsername( username );
        user.setPassword( clientSecret );
        user.setDisabled( false );
        user.setAccountNonLocked( true );
        user.setCredentialsNonExpired( true );
        user.setAccountExpiry( null );

        return userService.validateAndCreateUserDetails( user, user.getPassword() );
    }

    private ClientDetails getClientDetails( String username )
    {
        try
        {
            return clientDetailsService.loadClientByClientId( username );
        }
        catch ( NoSuchClientException e )
        {
            throw new UsernameNotFoundException( e.getMessage(), e );
        }
    }
}
