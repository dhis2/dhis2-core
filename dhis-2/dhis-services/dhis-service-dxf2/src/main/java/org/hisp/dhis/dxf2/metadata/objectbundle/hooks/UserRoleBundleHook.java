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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.authz.AuthzService;
import org.springframework.stereotype.Component;

/**
 * Soft-refreshes principals affected by user-role authority or restriction changes by bumping the
 * role authz generation (and global epoch) instead of invalidating sessions.
 *
 * <p>Role-side membership is intentionally not handled here: {@code UserRole.members} is mapped
 * with {@code inverse="true"} and the exposed {@code users} property has no setter, so the metadata
 * importer cannot mutate membership from the role side. Membership deltas flow through the owning
 * side ({@code User.userRoles}) and are handled by {@link UserObjectBundleHook}.
 *
 * @author Morten Svanæs
 */
@Component
@AllArgsConstructor
@Slf4j
public class UserRoleBundleHook extends AbstractObjectBundleHook<UserRole> {

  public static final String BUMP_ROLE_AUTHZ_KEY = "shouldBumpRoleAuthz";

  private final AuthzService authzService;

  @Override
  public void preUpdate(UserRole update, UserRole existing, ObjectBundle bundle) {
    if (update == null) return;
    bundle.putExtras(update, BUMP_ROLE_AUTHZ_KEY, roleAuthzChanged(update, existing));
  }

  private Boolean roleAuthzChanged(UserRole update, UserRole existing) {
    return !Objects.equals(update.getAuthorities(), existing.getAuthorities())
        || !Objects.equals(update.getRestrictions(), existing.getRestrictions());
  }

  @Override
  public void postUpdate(UserRole updatedUserRole, ObjectBundle bundle) {
    final Boolean shouldBump = (Boolean) bundle.getExtras(updatedUserRole, BUMP_ROLE_AUTHZ_KEY);

    if (Boolean.TRUE.equals(shouldBump) && updatedUserRole.getUid() != null) {
      authzService.bumpRoleAuthz(updatedUserRole.getUid());
    }

    bundle.removeExtras(updatedUserRole, BUMP_ROLE_AUTHZ_KEY);
  }

  @Override
  public void preDelete(UserRole persistedObject, ObjectBundle bundle) {
    // Bumping the soon-orphaned role key moves the epoch and the gens still referenced by live
    // principals' snapshots, forcing their next-request refresh which then drops the deleted role.
    // O(1); the orphan authz_version row is harmless.
    if (persistedObject.getUid() != null) {
      authzService.bumpRoleAuthz(persistedObject.getUid());
    }
  }
}
