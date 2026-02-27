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

import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.UID;

import javax.annotation.Nonnull;

/** Contains functions to manage {@link UserGroup} */
public interface UserGroupStore extends IdentifiableObjectStore<UserGroup> {
  String ID = UserGroupStore.class.getName();

  /**
   * Adds a user to a user group directly via SQL, without loading the members collection. Also
   * updates the group's lastUpdated timestamp if the membership was added. This avoids N+1 query
   * problems when adding users to groups with many members.
   *
   * @param userGroupUid the UID of the user group
   * @param userUid the UID of the user to add
   * @param lastUpdatedByUid the UID of the user performing the operation
   * @return true if the membership was added, false if user was already a member
   */
  boolean addMember(@Nonnull UID userGroupUid, @Nonnull UID userUid, @Nonnull UID lastUpdatedByUid);

  void updateLastUpdated( @Nonnull UID userGroupUid, @Nonnull UID lastUpdatedByUid );
}
