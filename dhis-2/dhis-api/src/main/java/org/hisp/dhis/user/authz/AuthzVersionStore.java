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
import javax.annotation.Nonnull;

/**
 * Generation stamps that drive UserDetails soft-refresh.
 *
 * <p>Every bump advances the global epoch. Bumps participate in the caller's ambient transaction
 * (JdbcTemplate joins it), so a reader can never observe a gen/epoch value without also seeing the
 * committed data that caused it.
 *
 * @author Morten Svanæs
 */
public interface AuthzVersionStore {
  /** Global epoch; advanced by every bump. Missing row = 0. */
  long getEpoch();

  /** max(user gen for userUid, role gens for roleUids); missing keys count as 0. */
  long getMaxGen(@Nonnull String userUid, @Nonnull Collection<String> roleUids);

  void bumpUserGen(@Nonnull String userUid);

  void bumpRoleGen(@Nonnull String roleUid);

  /** Bumps each distinct uid once and the epoch once. */
  void bumpUserGens(@Nonnull Collection<String> userUids);
}
