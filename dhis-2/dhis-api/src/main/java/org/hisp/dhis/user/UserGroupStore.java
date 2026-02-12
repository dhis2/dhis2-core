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

import javax.annotation.Nonnull;
import org.hisp.dhis.common.IdentifiableObjectStore;

/** Contains functions to manage {@link UserGroup} */
public interface UserGroupStore extends IdentifiableObjectStore<UserGroup> {
  String ID = UserGroupStore.class.getName();

  /**
   * Adds a user to a user group directly via SQL, without loading the members collection. This
   * avoids N+1 query problems when adding users to groups with many members.
   *
   * @param userGroupUid the UID of the user group
   * @param userUid the UID of the user to add
   * @return true if the membership was added, false if user was already a member
   */
  boolean addMemberViaSQL(@Nonnull String userGroupUid, @Nonnull String userUid);

  /**
   * Removes a user from a user group directly via SQL, without loading the members collection. This
   * avoids N+1 query problems when removing users from groups with many members.
   *
   * @param userGroupUid the UID of the user group
   * @param userUid the UID of the user to remove
   * @return true if the membership was removed, false if user was not a member
   */
  boolean removeMemberViaSQL(@Nonnull String userGroupUid, @Nonnull String userUid);

  /**
   * Updates the lastUpdated timestamp and lastUpdatedBy user for a user group directly via SQL.
   * This avoids loading the entity through Hibernate which can trigger lazy initialization of the
   * members collection.
   *
   * @param userGroupUid the UID of the user group
   * @param lastUpdatedByUid the UID of the user who made the change
   */
  void updateLastUpdatedViaSQL(@Nonnull String userGroupUid, @Nonnull String lastUpdatedByUid);
}
