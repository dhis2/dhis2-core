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
package org.hisp.dhis.user;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.spring.AbstractSpringSecurityCurrentUserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for retrieving information about the currently authenticated user.
 *
 * <p>Note that most methods are transactional, except for retrieving current UserInfo.
 *
 * @author Torgeir Lorange Ostby
 */
@Service("org.hisp.dhis.user.CurrentUserService")
@Slf4j
public class DefaultCurrentUserService extends AbstractSpringSecurityCurrentUserService {
  /**
   * Cache contains Set of UserGroup UID for each user. Key is username. This will be used for ACL
   * check in {@link org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore}
   */
  private final Cache<CurrentUserGroupInfo> currentUserGroupInfoCache;

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final UserStore userStore;

  public DefaultCurrentUserService(CacheProvider cacheProvider, @Lazy UserStore userStore) {
    checkNotNull(cacheProvider);
    checkNotNull(userStore);

    this.userStore = userStore;
    this.currentUserGroupInfoCache = cacheProvider.createCurrentUserGroupInfoCache();
  }

  // -------------------------------------------------------------------------
  // CurrentUserService implementation
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public User getCurrentUser() {
    String username = getCurrentUsername();

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication.getPrincipal() == null) {
      return null;
    }

    if (username == null) {
      throw new IllegalStateException("No current user");
    }

    User user = userStore.getUserByUsername(username);
    if (user == null) {
      log.debug("User is NULL, this should only happen at startup!");
      return null;
    }

    user.getAllAuthorities();
    return user;
  }

  @Override
  public Long getUserId(String username) {
    User user = userStore.getUserByUsername(username);

    return user != null ? user.getId() : null;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean currentUserIsSuper() {
    User user = getCurrentUser();

    return user != null && user.isSuper();
  }

  @Override
  @Transactional(readOnly = true)
  public Set<OrganisationUnit> getCurrentUserOrganisationUnits() {
    User user = getCurrentUser();

    return user != null ? new HashSet<>(user.getOrganisationUnits()) : new HashSet<>();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean currentUserIsAuthorized(String auth) {
    User user = getCurrentUser();

    return user != null && user.isAuthorized(auth);
  }

  @Override
  @Transactional(readOnly = true)
  public CurrentUserGroupInfo getCurrentUserGroupsInfo() {
    User currentUser = getCurrentUser();

    if (currentUser == null) {
      return null;
    }

    return currentUserGroupInfoCache.get(currentUser.getUsername(), this::getCurrentUserGroupsInfo);
  }

  @Override
  @Transactional(readOnly = true)
  public CurrentUserGroupInfo getCurrentUserGroupsInfo(User userInfo) {
    if (userInfo == null) {
      return null;
    }

    return currentUserGroupInfoCache.get(userInfo.getUsername(), this::getCurrentUserGroupsInfo);
  }

  @Override
  public void invalidateUserGroupCache(String username) {
    try {
      currentUserGroupInfoCache.invalidate(username);
    } catch (NullPointerException exception) {
      // Ignore if key doesn't exist
    }
  }

  private CurrentUserGroupInfo getCurrentUserGroupsInfo(String username) {
    if (username == null) {
      return null;
    }

    User currentUser = getCurrentUser();
    if (currentUser == null) {
      log.warn("User is null, this should only happen at startup!");
      return null;
    }
    return userStore.getCurrentUserGroupInfo(currentUser.getId());
  }
}
