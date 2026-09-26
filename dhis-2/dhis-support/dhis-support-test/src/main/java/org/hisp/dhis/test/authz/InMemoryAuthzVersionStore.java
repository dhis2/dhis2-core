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
package org.hisp.dhis.test.authz;

import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import org.hisp.dhis.user.authz.AuthzVersionStore;

/**
 * In-memory {@link AuthzVersionStore} test double for H2-backed test contexts, where the
 * PostgreSQL-only production JDBC store cannot run (feature coverage lives in the Postgres
 * integration tests). Mirrors the JDBC contract: missing keys are 0, and each bump (or batch)
 * advances the epoch once. The epoch MUST move on bumps: {@code DefaultAuthzService} invalidates
 * its per-username cache only on epoch movement, so a frozen store would leak stale principals.
 *
 * @author Morten Svanæs
 */
public class InMemoryAuthzVersionStore implements AuthzVersionStore {

  private final ConcurrentHashMap<String, AtomicLong> userGens = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AtomicLong> roleGens = new ConcurrentHashMap<>();
  private final AtomicLong epoch = new AtomicLong(0);

  @Override
  public long getEpoch() {
    return epoch.get();
  }

  @Override
  public long getMaxGen(@Nonnull String userUid, @Nonnull Collection<String> roleUids) {
    long max = genOf(userGens, userUid);
    for (String roleUid : roleUids) {
      max = Math.max(max, genOf(roleGens, roleUid));
    }
    return max;
  }

  @Override
  public void bumpUserGen(@Nonnull String userUid) {
    bump(userGens, userUid);
    epoch.incrementAndGet();
  }

  @Override
  public void bumpRoleGen(@Nonnull String roleUid) {
    bump(roleGens, roleUid);
    epoch.incrementAndGet();
  }

  @Override
  public void bumpUserGens(@Nonnull Collection<String> userUids) {
    TreeSet<String> distinct = new TreeSet<>();
    for (String uid : userUids) {
      if (uid == null) {
        continue;
      }
      String trimmed = uid.trim();
      if (!trimmed.isEmpty()) {
        distinct.add(trimmed);
      }
    }
    if (distinct.isEmpty()) {
      return;
    }
    for (String uid : distinct) {
      bump(userGens, uid);
    }
    epoch.incrementAndGet();
  }

  private static long genOf(ConcurrentHashMap<String, AtomicLong> map, String key) {
    AtomicLong value = map.get(key);
    return value == null ? 0L : value.get();
  }

  private static void bump(ConcurrentHashMap<String, AtomicLong> map, String key) {
    map.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
  }
}
