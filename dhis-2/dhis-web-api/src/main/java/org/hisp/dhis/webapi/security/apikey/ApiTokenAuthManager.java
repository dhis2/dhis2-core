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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenAuthenticationToken;
import org.hisp.dhis.security.apikey.ApiTokenDeletedEvent;
import org.hisp.dhis.security.apikey.ApiTokenService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserStore;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Processes API token authentication requests, looks for the 'Authorization' header containing an
 * API token. If the token is found, the request will be authenticated only if the token is not
 * expired and the request constraint rules are matching.
 */
@Service
public class ApiTokenAuthManager implements AuthenticationManager {
  private final ApiTokenService apiTokenService;
  private final OrganisationUnitService organisationUnitService;
  private final UserService userService;
  private final UserStore userStore;

  private final Cache<ApiTokenAuthenticationToken> apiTokenCache;

  public ApiTokenAuthManager(
      UserStore userStore,
      ApiTokenService apiTokenService,
      CacheProvider cacheProvider,
      @Lazy UserService userService,
      OrganisationUnitService organisationUnitService) {
    this.userService = userService;
    this.userStore = userStore;
    this.apiTokenService = apiTokenService;
    this.organisationUnitService = organisationUnitService;
    this.apiTokenCache = cacheProvider.createApiKeyCache();
  }

  @EventListener
  public void handleApiTokenDeleted(ApiTokenDeletedEvent event) {
    apiTokenCache.invalidate(event.getTokenHash());
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    final String tokenKey = ((ApiTokenAuthenticationToken) authentication).getTokenKey();

    final Optional<ApiTokenAuthenticationToken> cachedToken = apiTokenCache.getIfPresent(tokenKey);

    if (cachedToken.isPresent()) {
      validateTokenExpiry(cachedToken.get().getToken().getExpire());
      return cachedToken.get();
    } else {
      ApiToken apiToken = apiTokenService.getByKey(tokenKey);
      if (apiToken == null) {
        throw new ApiTokenAuthenticationException(
            ApiTokenErrors.invalidToken("The API token does not exists"));
      }

      validateTokenExpiry(apiToken.getExpire());

      UserDetails currentUserDetails = validateAndCreateUserDetails(apiToken.getCreatedBy());

      ApiTokenAuthenticationToken authenticationToken =
          new ApiTokenAuthenticationToken(apiToken, currentUserDetails);

      apiTokenCache.put(tokenKey, authenticationToken);

      return authenticationToken;
    }
  }

  private UserDetails validateAndCreateUserDetails(User createdBy) {
    if (createdBy == null) {
      throw new ApiTokenAuthenticationException(
          ApiTokenErrors.invalidToken("The API token does not have any owner."));
    }

    User user = userStore.getUserByUsername(createdBy.getUsername());
    if (user == null) {
      throw new ApiTokenAuthenticationException(
          ApiTokenErrors.invalidToken("The API token owner does not exists."));
    }

    boolean isTwoFactorDisabled = !user.isTwoFactorEnabled();
    boolean enabled = !user.isDisabled();

    boolean credentialsNonExpired = userService.userNonExpired(user);
    boolean accountNonLocked = !userService.isLocked(user.getUsername());
    boolean accountNonExpired = user.isAccountNonExpired();

    if (ObjectUtils.anyIsFalse(
        enabled, isTwoFactorDisabled, credentialsNonExpired, accountNonLocked, accountNonExpired)) {

      throw new ApiTokenAuthenticationException(
          ApiTokenErrors.invalidToken("The API token is disabled, locked or 2FA is enabled."));
    }

    List<String> organisationUnitsUidsByUser =
        organisationUnitService.getOrganisationUnitsUidsByUser(user.getUsername());
    List<String> searchOrganisationUnitsUidsByUser =
        organisationUnitService.getSearchOrganisationUnitsUidsByUser(user.getUsername());
    List<String> dataViewOrganisationUnitsUidsByUser =
        organisationUnitService.getDataViewOrganisationUnitsUidsByUser(user.getUsername());

    return UserDetails.createUserDetails(
        user,
        accountNonLocked,
        credentialsNonExpired,
        new HashSet<>(organisationUnitsUidsByUser),
        new HashSet<>(searchOrganisationUnitsUidsByUser),
        new HashSet<>(dataViewOrganisationUnitsUidsByUser));
  }

  private static void validateTokenExpiry(Long expiry) {
    if (expiry <= System.currentTimeMillis()) {
      throw new ApiTokenExpiredException("Failed to authenticate API token, token has expired.");
    }
  }
}
