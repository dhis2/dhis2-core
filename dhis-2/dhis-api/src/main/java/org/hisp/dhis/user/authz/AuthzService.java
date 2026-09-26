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
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.user.UserDetails;

/**
 * Soft-refresh facade over authz generation stamps and cached UserDetails snapshots.
 *
 * @author Morten Svanæs
 */
public interface AuthzService {

  /**
   * Epoch-validated, cached, immutable snapshot for username, stamped with the authz epoch read
   * before the snapshot was built and the effective generation it reflects (see {@link
   * UserDetails#getAuthzCheckedEpoch()}). Returns a snapshot that reflects every authz change
   * committed up to the epoch value read at call entry. Null if user unknown.
   */
  @CheckForNull
  UserDetails getFreshUserDetails(@Nonnull String username);

  /**
   * Three-way freshness check against the principal's own authz stamp. Returns:
   *
   * <ul>
   *   <li>the same instance: stamp is current (or the user is unknown) — nothing to do;
   *   <li>a re-stamped copy: the epoch moved but this principal's generations did not — persist the
   *       copy so the next check takes the epoch fast path;
   *   <li>a rebuilt snapshot: this principal's generations moved — persist and use it.
   * </ul>
   *
   * <p>Identity comparison with the argument tells the caller whether anything must be persisted.
   */
  @Nonnull
  UserDetails refreshIfStale(@Nonnull UserDetails principal);

  void bumpUserAuthz(@Nonnull String userUid);

  void bumpRoleAuthz(@Nonnull String roleUid);

  void bumpUsers(@Nonnull Collection<String> userUids);
}
