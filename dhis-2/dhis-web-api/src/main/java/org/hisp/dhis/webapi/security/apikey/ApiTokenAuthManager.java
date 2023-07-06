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
package org.hisp.dhis.webapi.security.apikey;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenAuthenticationToken;
import org.hisp.dhis.security.apikey.ApiTokenService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Processes API token authentication requests, looks for the 'Authorization'
 * header containing an API token. If the token is found, the request will be
 * authenticated only if the token is not expired and the request constraint
 * rules are matching.
 */
@Slf4j
public class ApiTokenAuthManager implements AuthenticationManager
{
    private final ApiTokenService apiTokenService;

    private final UserService userService;

    private final Cache<ApiTokenAuthenticationToken> apiTokenCache;

    private final SecurityService securityService;

    public ApiTokenAuthManager( UserService userService, SecurityService securityService,
        ApiTokenService apiTokenService, CacheProvider cacheProvider )
    {
        this.securityService = securityService;
        this.userService = userService;
        this.apiTokenService = apiTokenService;
        this.apiTokenCache = cacheProvider.createApiKeyCache();
    }

    @Override
    public Authentication authenticate( Authentication authentication )
        throws AuthenticationException
    {
        final String tokenKey = ((ApiTokenAuthenticationToken) authentication).getTokenKey();

        final Optional<ApiTokenAuthenticationToken> cachedToken = apiTokenCache.getIfPresent( tokenKey );

        if ( cachedToken.isPresent() )
        {
            validateTokenExpiry( cachedToken.get().getToken().getExpire() );
            return cachedToken.get();
        }
        else
        {
            ApiToken apiToken = apiTokenService.getWithKey( tokenKey );
            if ( apiToken == null )
            {
                throw new ApiTokenAuthenticationException(
                    ApiTokenErrors.invalidToken( "The API token does not exists." ) );
            }

            validateTokenExpiry( apiToken.getExpire() );

            User user = validateUser( apiToken );

            ApiTokenAuthenticationToken authenticationToken = new ApiTokenAuthenticationToken( apiToken,
                user );

            apiTokenCache.put( tokenKey, authenticationToken );

            return authenticationToken;
        }
    }

    private User validateUser( ApiToken apiToken )
    {
        User createdBy = apiToken.getCreatedBy();
        if ( createdBy == null )
        {
            throw new ApiTokenAuthenticationException(
                ApiTokenErrors.invalidToken( "The API token does not have any owner." ) );
        }

        User user = userService
            .getUserWithEagerFetchAuthorities( createdBy.getUsername() );
        if ( user == null )
        {
            throw new ApiTokenAuthenticationException(
                ApiTokenErrors.invalidToken( "The API token owner does not exists." ) );
        }

        boolean is2FAEnabled = !user.isTwoFA();
        boolean enabled = !user.isDisabled();
        boolean credentialsNonExpired = userService.userNonExpired( user );
        boolean accountNonLocked = !securityService.isLocked( user.getUsername() );
        boolean accountNonExpired = !userService.isAccountExpired( user );

        if ( ObjectUtils.anyIsFalse( enabled, is2FAEnabled, credentialsNonExpired, accountNonLocked,
            accountNonExpired ) )
        {
            throw new ApiTokenAuthenticationException(
                ApiTokenErrors.invalidToken( "The API token is disabled, locked or 2FA is enabled." ) );
        }

        return user;
    }

    private void validateTokenExpiry( Long expiry )
    {
        if ( expiry <= System.currentTimeMillis() )
        {
            throw new ApiTokenExpiredException( "Failed to authenticate API token, token has expired." );
        }
    }
}
