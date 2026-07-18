/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.user.authz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Epoch-validated, cached UserDetails soft-refresh service.
 *
 * <p>The cached {@code checkedEpoch} is always read before {@code createUserDetailsByUsername}
 * starts. Hence {@code entry.checkedEpoch >= currentEpoch} proves the snapshot includes every bump
 * committed up to {@code currentEpoch}. Never reorder these reads.
 *
 * @author Morten Svanæs
 */
@Slf4j
@Service("org.hisp.dhis.user.authz.AuthzService")
public class DefaultAuthzService implements AuthzService {

  private static final int LOCK_STRIPES = 64;

  /** Snapshot plus the epoch value read BEFORE the snapshot was built. */
  record CachedUserDetails(UserDetails details, long checkedEpoch) {}

  private final AuthzVersionStore versionStore;
  private final UserService userService;
  private final Cache<CachedUserDetails> cache;
  private final Object[] locks = new Object[LOCK_STRIPES];

  public DefaultAuthzService(
      AuthzVersionStore versionStore, @Lazy UserService userService, CacheProvider cacheProvider) {
    this.versionStore = versionStore;
    this.userService = userService;
    this.cache = cacheProvider.createUserDetailsAuthzCache();
    for (int i = 0; i < LOCK_STRIPES; i++) {
      locks[i] = new Object();
    }
  }

  @Override
  public long currentEpoch() {
    return versionStore.getEpoch();
  }

  @Override
  public long effectiveGen(@Nonnull UserDetails principal) {
    String uid = principal.getUid();
    if (uid == null) {
      return 0L;
    }
    return versionStore.getMaxGen(uid, principal.getUserRoleIds());
  }

  @Override
  @CheckForNull
  public UserDetails getFreshUserDetails(@Nonnull String username) {
    long epoch = versionStore.getEpoch(); // MUST be read before any snapshot build
    CachedUserDetails entry = cache.getIfPresent(username).orElse(null);
    if (entry != null && entry.checkedEpoch() >= epoch) {
      return entry.details();
    }
    Object lock = locks[Math.floorMod(username.hashCode(), LOCK_STRIPES)];
    synchronized (lock) {
      entry = cache.getIfPresent(username).orElse(null); // double-check under lock
      if (entry != null && entry.checkedEpoch() >= epoch) {
        return entry.details();
      }
      UserDetails fresh = userService.createUserDetailsByUsername(username);
      if (fresh == null) {
        cache.invalidate(username);
        return null;
      }
      cache.put(username, new CachedUserDetails(fresh, epoch));
      log.debug("Rebuilt UserDetails snapshot for {} at epoch {}", username, epoch);
      return fresh;
    }
  }

  @Override
  public void bumpUserAuthz(@Nonnull String userUid) {
    versionStore.bumpUserGen(userUid);
    // No cache invalidation on bumps: the epoch check self-corrects, and in-tx bumps are not
    // visible to other readers until commit.
    log.debug("Bumped user authz for {}", userUid);
  }

  @Override
  public void bumpRoleAuthz(@Nonnull String roleUid) {
    versionStore.bumpRoleGen(roleUid);
    // No cache invalidation on bumps: the epoch check self-corrects, and in-tx bumps are not
    // visible to other readers until commit.
    log.debug("Bumped role authz for {}", roleUid);
  }

  @Override
  public void bumpUsers(@Nonnull Collection<String> userUids) {
    List<String> filtered = new ArrayList<>();
    for (String uid : userUids) {
      if (uid == null) {
        continue;
      }
      String trimmed = uid.trim();
      if (!trimmed.isEmpty()) {
        filtered.add(trimmed);
      }
    }
    if (filtered.isEmpty()) {
      return;
    }
    versionStore.bumpUserGens(filtered);
    // No cache invalidation on bumps: the epoch check self-corrects, and in-tx bumps are not
    // visible to other readers until commit.
    log.debug("Bumped authz for {} users", filtered.size());
  }
}
