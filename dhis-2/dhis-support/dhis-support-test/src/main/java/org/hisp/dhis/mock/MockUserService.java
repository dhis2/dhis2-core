/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccountExpiryInfo;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;

/**
 * @author Adrian Quintana
 */
public class MockUserService implements UserService {

  private List<User> users;

  public MockUserService(List<User> users) {
    this.users = users;
  }

  @Override
  public long addUser(User user) {
    this.users.add(user);
    return user.getId();
  }

  @Override
  public void updateUser(User user) {}

  @Override
  public User getUser(long id) {
    return null;
  }

  @Override
  public User getUser(String uid) {
    return null;
  }

  @Override
  public User getUserByUuid(UUID uuid) {
    return null;
  }

  @Override
  public List<User> getUsers(Collection<String> uid) {
    return this.users;
  }

  @Override
  public List<User> getAllUsers() {
    return null;
  }

  @Override
  public List<User> getAllUsersBetweenByName(String name, int first, int max) {
    return null;
  }

  @Override
  public void deleteUser(User user) {}

  @Override
  public boolean isLastSuperUser(User user) {
    return false;
  }

  @Override
  public boolean isLastSuperRole(UserRole userRole) {
    return false;
  }

  @Override
  public List<User> getUsers(UserQueryParams params) {
    return null;
  }

  @Override
  public List<User> getUsers(UserQueryParams params, @Nullable List<String> orders) {
    return null;
  }

  @Override
  public int getUserCount(UserQueryParams params) {
    return 0;
  }

  @Override
  public int getUserCount() {
    return 0;
  }

  @Override
  public List<User> getUsersByPhoneNumber(String phoneNumber) {
    return null;
  }

  @Override
  public boolean canAddOrUpdateUser(Collection<String> userGroups) {
    return false;
  }

  @Override
  public boolean canAddOrUpdateUser(Collection<String> userGroups, User currentUser) {
    return false;
  }

  @Override
  public List<User> getUsersByUsernames(Collection<String> usernames) {
    List<User> usersByUsername = new ArrayList<>();

    for (User user : this.users) {
      if (usernames.contains(user.getUsername())) {
        usersByUsername.add(user);
      }
    }

    return usersByUsername;
  }

  public User getUserByIdToken(String idToken) {
    for (User user : users) {
      if (user.getIdToken().equals(idToken)) {
        return user;
      }
    }
    return null;
  }

  @Override
  public User getUserWithEagerFetchAuthorities(String username) {
    for (User user : users) {
      if (user.getUsername().equals(username)) {
        user.getAllAuthorities();
        return user;
      }
    }
    return null;
  }

  @Override
  public User getUserByOpenId(String openId) {
    return null;
  }

  @Override
  public User getUserByLdapId(String ldapId) {
    return null;
  }

  @Override
  public void encodeAndSetPassword(User user, String rawPassword) {}

  @Override
  public void setLastLogin(String username) {}

  @Override
  public int getActiveUsersCount(int days) {
    return 0;
  }

  @Override
  public int getActiveUsersCount(Date since) {
    return 0;
  }

  @Override
  public boolean userNonExpired(User user) {
    return false;
  }

  @Override
  public boolean isAccountExpired(User user) {
    return false;
  }

  @Override
  public long addUserRole(UserRole userRole) {
    return 0;
  }

  @Override
  public void updateUserRole(UserRole userRole) {}

  @Override
  public UserRole getUserRole(long id) {
    return null;
  }

  @Override
  public UserRole getUserRole(String uid) {
    return null;
  }

  @Override
  public UserRole getUserRoleByName(String name) {
    return null;
  }

  @Override
  public void deleteUserRole(UserRole userRole) {}

  @Override
  public List<UserRole> getAllUserRoles() {
    return null;
  }

  @Override
  public List<UserRole> getUserRolesByUid(Collection<String> uids) {
    return null;
  }

  @Override
  public List<UserRole> getUserRolesBetween(int first, int max) {
    return null;
  }

  @Override
  public List<UserRole> getUserRolesBetweenByName(String name, int first, int max) {
    return null;
  }

  @Override
  public int countDataSetUserRoles(DataSet dataSet) {
    return 0;
  }

  @Override
  public void canIssueFilter(Collection<UserRole> userRoles) {}

  @Override
  public List<ErrorReport> validateUser(User user, User currentUser) {
    return null;
  }

  @Override
  public List<User> getExpiringUsers() {
    return null;
  }

  @Override
  public List<UserAccountExpiryInfo> getExpiringUserAccounts(int inDays) {
    return null;
  }

  @Override
  public void set2FA(User user, Boolean twoFA) {}

  @Override
  public void expireActiveSessions(User user) {}

  @Override
  public User getUserByUsername(String username) {
    return null;
  }

  @Override
  public User getUserByIdentifier(String id) {
    return null;
  }

  @Override
  public int disableUsersInactiveSince(Date inactiveSince) {
    throw new UnsupportedOperationException("Not supported by this mock!");
  }

  @Override
  public Set<String> findNotifiableUsersWithLastLoginBetween(Date from, Date to) {
    return null;
  }

  @Override
  public String getDisplayName(String userUid) {
    return null;
  }

  @Override
  public List<User> getUsersWithAuthority(String authority) {
    return Collections.emptyList();
  }
}
