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
package org.hisp.dhis.security.oidc;

import java.io.Serializable;
import java.util.Map;

import org.hisp.dhis.user.CurrentUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class DhisOidcUser
    extends DefaultOAuth2User
    implements OidcUser, CurrentUserDetails
{
    private final OidcIdToken oidcIdToken;

    private final CurrentUserDetails user;

    public DhisOidcUser( CurrentUserDetails user, Map<String, Object> attributes, String nameAttributeKey,
        OidcIdToken idToken )
    {
        super( user.getAuthorities(), attributes, nameAttributeKey );
        this.oidcIdToken = idToken;
        this.user = user;
    }

    @Override
    public Map<String, Object> getClaims()
    {
        return this.getAttributes();
    }

    @Override
    public OidcUserInfo getUserInfo()
    {
        return null;
    }

    @Override
    public OidcIdToken getIdToken()
    {
        return oidcIdToken;
    }

    public UserDetails getUser()
    {
        return user;
    }

    @Override
    public String getUsername()
    {
        return user.getUsername();
    }

    @Override
    public String getPassword()
    {
        return user.getPassword();
    }

    @Override
    public boolean isAccountNonExpired()
    {
        return user.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked()
    {
        return user.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired()
    {
        return user.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled()
    {
        return user.isEnabled();
    }

    @Override
    public String getUid()
    {
        return user.getUid();

    }

    @Override
    public Map<String, Serializable> getUserSettings()
    {
        return user.getUserSettings();
    }
}
