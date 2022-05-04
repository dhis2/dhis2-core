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
package org.hisp.dhis.security;

import static org.hisp.dhis.user.CurrentUserUtil.initializeUser;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.SessionFactory;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Torgeir Lorange Ostby
 */
@Slf4j
@Service( "userDetailsService" )
@AllArgsConstructor
public class DefaultUserDetailsService
    implements UserDetailsService
{
    private final UserService userService;

    private final SecurityService securityService;

    private final SessionFactory sessionFactory;

    @Override
    @Transactional( readOnly = true )
    public UserDetails loadUserByUsername( String username )
        throws UsernameNotFoundException,
        DataAccessException
    {
        User user = userService.getUserByUsername( username );

        boolean enabled = !user.isDisabled();
        boolean credentialsNonExpired = userService.userNonExpired( user );
        boolean accountNonLocked = !securityService.isLocked( user.getUsername() );
        boolean accountNonExpired = !userService.isAccountExpired( user );

        if ( ObjectUtils.anyIsFalse( enabled, credentialsNonExpired, accountNonLocked, accountNonExpired ) )
        {
            log.debug( String.format(
                "Login attempt for disabled/locked user: '%s', enabled: %b, account non-expired: %b, user non-expired: %b, account non-locked: %b",
                username, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked ) );
        }

        initializeUser( user );

        sessionFactory.getCurrentSession().evict( user );

        user.setAccountNonLocked( accountNonLocked );
        user.setCredentialsNonExpired( credentialsNonExpired );

        return user;
    }
}
