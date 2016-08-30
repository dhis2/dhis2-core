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

import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;

import java.util.HashSet;

/**
 * This access provider will put a user with all granted authorities in the database.
 *
 * @author Torgeir Lorange Ostby
 */
public class DatabaseAutomaticAccessProvider
    extends AbstractAutomaticAccessProvider
{
    // -------------------------------------------------------------------------
    // AdminAccessManager implementation
    // -------------------------------------------------------------------------

    @Override
    public void initialise()
    {
        // ---------------------------------------------------------------------
        // Assumes no UserAuthorityGroup called "Superuser" in database
        // ---------------------------------------------------------------------

        String username = "admin";
        String password = "district";

        User user = new User();
        user.setUid( "M5zQapPyTZI" );
        user.setCode( "admin" );
        user.setFirstName( username );
        user.setSurname( username );

        userService.addUser( user );

        UserAuthorityGroup userAuthorityGroup = new UserAuthorityGroup();
        userAuthorityGroup.setUid( "yrB6vc5Ip3r" );
        userAuthorityGroup.setCode( "Superuser" );
        userAuthorityGroup.setName( "Superuser" );
        userAuthorityGroup.setDescription( "Superuser" );

        userAuthorityGroup.setAuthorities( new HashSet<>( getAuthorities() ) );

        userService.addUserAuthorityGroup( userAuthorityGroup );

        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUid( "KvMx6c1eoYo" );
        userCredentials.setCode( username );
        userCredentials.setUsername( username );
        userCredentials.setUserInfo( user );
        userCredentials.getUserAuthorityGroups().add( userAuthorityGroup );

        userService.encodeAndSetPassword( userCredentials, password );
        userService.addUserCredentials( userCredentials );
    }

    @Override
    public void access()
    {
    }
}
