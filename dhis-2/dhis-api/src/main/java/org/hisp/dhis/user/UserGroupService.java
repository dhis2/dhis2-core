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

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;

public interface UserGroupService {
  String ID = UserGroupService.class.getName();

  long addUserGroup(UserGroup userGroup);

  void updateUserGroup(UserGroup userGroup);

  void deleteUserGroup(UserGroup userGroup);

  UserGroup getUserGroup(long userGroupId);

  UserGroup getUserGroup(String uid);

  /**
   * Indicates whether the current user can add or remove members for the user group with the given
   * UID. To do so the current user must have write access to the group or have read access as well
   * as the F_USER_GROUPS_READ_ONLY_ADD_MEMBERS authority.
   *
   * @param uid the user group UID.
   * @return true if the current user can add or remove members of the user group.
   */
  boolean canAddOrRemoveMember(String uid);

  boolean canAddOrRemoveMember(String uid, UserDetails currentUser);

  void addUserToGroups(User user, @Nonnull Collection<String> uids, UserDetails currentUser);

  void removeUserFromGroups(User user, @Nonnull Collection<String> uids);

  void updateUserGroups(User user, @Nonnull Collection<String> uids, UserDetails currentUser);

  List<UserGroup> getAllUserGroups();

  List<UserGroup> getUserGroupByName(String name);

  List<UserGroup> getUserGroupsBetween(int first, int max);

  List<UserGroup> getUserGroupsBetweenByName(String name, int first, int max);

  /** Get UserGroup's display name by given userGroup uid Return null if UserGroup does not exist */
  String getDisplayName(String uid);
}
