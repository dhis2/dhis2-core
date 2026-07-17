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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;

/**
 * Process-local generation store for tests and non-Redis single-node deployments.
 *
 * @author Morten Svanæs
 */
public class InMemoryAuthzVersionStore implements AuthzVersionStore {

  private final ConcurrentHashMap<String, AtomicLong> userGens = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AtomicLong> roleGens = new ConcurrentHashMap<>();

  @Override
  public long getUserGen(@Nonnull String username) {
    AtomicLong gen = userGens.get(username);
    return gen == null ? 0L : gen.get();
  }

  @Override
  public long getRoleGen(@Nonnull String roleUid) {
    AtomicLong gen = roleGens.get(roleUid);
    return gen == null ? 0L : gen.get();
  }

  @Override
  public long bumpUserGen(@Nonnull String username) {
    return userGens.computeIfAbsent(username, k -> new AtomicLong(0)).incrementAndGet();
  }

  @Override
  public long bumpRoleGen(@Nonnull String roleUid) {
    return roleGens.computeIfAbsent(roleUid, k -> new AtomicLong(0)).incrementAndGet();
  }

  @Override
  public void bumpUserGens(@Nonnull Collection<String> usernames) {
    for (String username : usernames) {
      if (username != null && !username.isBlank()) {
        bumpUserGen(username);
      }
    }
  }
}
