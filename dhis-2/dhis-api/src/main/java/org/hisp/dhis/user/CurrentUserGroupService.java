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

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("org.hisp.dhis.user.CurrentUserGroupService")
public class CurrentUserGroupService {

  private final Cache<CurrentUserGroupInfo> currentUserGroupInfoCache;

  private final UserService userService;
  public CurrentUserGroupService(UserService userService, CacheProvider cacheProvider) {
    checkNotNull(cacheProvider);
    checkNotNull(cacheProvider);

    this.userService = userService;
    this.currentUserGroupInfoCache = cacheProvider.createCurrentUserGroupInfoCache();
  }

  @Transactional(readOnly = true)
  public CurrentUserGroupInfo getCurrentUserGroupsInfo() {
    CurrentUserDetails user = CurrentUserUtil.getCurrentUserDetails();
    return user == null ? null : getCurrentUserGroupsInfo(user.getUid());
  }

  @Transactional(readOnly = true)
  public CurrentUserGroupInfo getCurrentUserGroupsInfo(String userUID) {
    return currentUserGroupInfoCache.get(userUID, userService::getCurrentUserGroupInfo);
  }

  @Transactional(readOnly = true)
  public void invalidateUserGroupCache(String userUID) {
    try {
      currentUserGroupInfoCache.invalidate(userUID);
    } catch (NullPointerException exception) {
      // Ignore if key doesn't exist
    }
  }
}
