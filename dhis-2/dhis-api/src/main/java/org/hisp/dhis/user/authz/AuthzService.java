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
 * Shared authz freshness service for form/OIDC sessions, JWT, and PAT.
 *
 * <p>Keeps {@link UserDetails} immutable: callers rebuild snapshots when generation stamps change.
 *
 * @author Morten Svanæs
 */
public interface AuthzService {

  long currentUserGen(@Nonnull String username);

  long currentRoleGen(@Nonnull String roleUid);

  /**
   * Effective generation for a principal:
   * {@code max(userGen, max(roleGen for each role id on the principal))}.
   */
  long effectiveGen(@Nonnull UserDetails principal);

  long bumpUserAuthz(@Nonnull String username);

  long bumpRoleAuthz(@Nonnull String roleUid);

  void bumpUsers(@Nonnull Collection<String> usernames);

  @CheckForNull
  UserDetails loadFreshUserDetails(@Nonnull String username);

  /**
   * Returns {@code current} if still fresh, otherwise a newly built immutable {@link UserDetails}.
   */
  @Nonnull
  UserDetails ensureFresh(@Nonnull UserDetails current);
}
