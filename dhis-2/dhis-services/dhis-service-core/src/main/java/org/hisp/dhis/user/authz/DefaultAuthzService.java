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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Default {@link AuthzService} with dual-gen math, single-flight rebuild, and short-lived
 * in-process cache keyed by username+effectiveGen.
 *
 * @author Morten Svanæs
 */
@Slf4j
@Service
public class DefaultAuthzService implements AuthzService {

  private final AuthzVersionStore versionStore;
  private final UserService userService;

  /** username:effectiveGen -> UserDetails */
  private final Map<String, UserDetails> detailsCache = new ConcurrentHashMap<>();

  /** username -> in-flight rebuild lock */
  private final Map<String, Object> rebuildLocks = new ConcurrentHashMap<>();

  private final Map<String, Long> metrics = new ConcurrentHashMap<>();

  public DefaultAuthzService(AuthzVersionStore versionStore, @Lazy UserService userService) {
    this.versionStore = versionStore;
    this.userService = userService;
  }

  @Override
  public long currentUserGen(@Nonnull String username) {
    return versionStore.getUserGen(username);
  }

  @Override
  public long currentRoleGen(@Nonnull String roleUid) {
    return versionStore.getRoleGen(roleUid);
  }

  @Override
  public long effectiveGen(@Nonnull UserDetails principal) {
    long max = versionStore.getUserGen(principal.getUsername());
    for (String roleUid : principal.getUserRoleIds()) {
      if (roleUid != null) {
        max = Math.max(max, versionStore.getRoleGen(roleUid));
      }
    }
    return max;
  }

  @Override
  public long bumpUserAuthz(@Nonnull String username) {
    long gen = versionStore.bumpUserGen(username);
    invalidateUserCache(username);
    metrics.merge("authz.bump.user", 1L, Long::sum);
    log.debug("Bumped user authz gen for {} -> {}", username, gen);
    return gen;
  }

  @Override
  public long bumpRoleAuthz(@Nonnull String roleUid) {
    long gen = versionStore.bumpRoleGen(roleUid);
    detailsCache.clear();
    metrics.merge("authz.bump.role", 1L, Long::sum);
    log.debug("Bumped role authz gen for {} -> {}", roleUid, gen);
    return gen;
  }

  @Override
  public void bumpUsers(@Nonnull Collection<String> usernames) {
    versionStore.bumpUserGens(usernames);
    for (String username : usernames) {
      if (username != null) {
        invalidateUserCache(username);
      }
    }
    metrics.merge("authz.bump.user", (long) usernames.size(), Long::sum);
  }

  @Override
  @CheckForNull
  public UserDetails loadFreshUserDetails(@Nonnull String username) {
    Object lock = rebuildLocks.computeIfAbsent(username, k -> new Object());
    synchronized (lock) {
      long start = System.nanoTime();
      UserDetails fresh = userService.createUserDetailsByUsername(username);
      long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
      metrics.merge("authz.rebuild.count", 1L, Long::sum);
      metrics.merge("authz.rebuild.latency.ms", elapsedMs, Long::sum);
      if (fresh != null) {
        long trueEffective = effectiveGen(fresh);
        String cacheKey = username + ":" + trueEffective;
        UserDetails cached = detailsCache.get(cacheKey);
        if (cached != null) {
          return cached;
        }
        detailsCache.put(cacheKey, fresh);
        metrics.merge("authz.soft_refresh", 1L, Long::sum);
      }
      return fresh;
    }
  }

  @Override
  @Nonnull
  public UserDetails ensureFresh(@Nonnull UserDetails current) {
    // Without a stored gen on UserDetails, compare store gens against zero baseline is wrong.
    // Callers that already detected staleness should use loadFreshUserDetails.
    // For PAT/JWT: rebuild when any gen for this principal is > 0 relative to a cached gen key.
    long effective = effectiveGen(current);
    String cacheKey = current.getUsername() + ":" + effective;
    UserDetails cached = detailsCache.get(cacheKey);
    if (cached != null) {
      return cached;
    }
    UserDetails fresh = loadFreshUserDetails(current.getUsername());
    return fresh != null ? fresh : current;
  }

  /** Test/observability helper. */
  public long metric(@Nonnull String name) {
    return metrics.getOrDefault(name, 0L);
  }

  private void invalidateUserCache(String username) {
    detailsCache.keySet().removeIf(key -> key.startsWith(username + ":"));
  }
}
