/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.user;

import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.system.deletion.IdObjectDeletionHandler;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 */
@Component
@RequiredArgsConstructor
public class UserRoleDeletionHandler extends IdObjectDeletionHandler<UserRole> {
  private final UserRoleStore userRoleStore;

  @Override
  protected void registerHandler() {
    whenDeleting(User.class, this::deleteUser);
  }

  // Note: unlike UserGroupDeletionHandler, we cannot call updateLastUpdatedForUserRolesViaSQL
  // here. That method evicts UserRole entities from the Hibernate session, and because
  // User.userRoles is the owning side with cascade="all", Hibernate will encounter the detached
  // UserRole during auto-flush and throw PersistentObjectException. User.groups is inverse="true"
  // (no cascade from User), so evicting UserGroup entities is safe. This asymmetry is due to
  // inconsistent Hibernate mapping ownership between User↔UserRole and User↔UserGroup.
  private void deleteUser(User user) {
    userRoleStore.removeAllMembershipsViaSQL(user.getUID());
    user.setUserRoles(new HashSet<>());
  }
}
