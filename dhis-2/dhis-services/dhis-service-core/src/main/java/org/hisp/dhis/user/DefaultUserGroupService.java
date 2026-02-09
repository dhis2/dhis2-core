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

import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.hibernate.Session;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Service("org.hisp.dhis.user.UserGroupService")
public class DefaultUserGroupService implements UserGroupService {
  private final UserGroupStore userGroupStore;
  private final AclService aclService;
  private final HibernateCacheManager cacheManager;
  private final Cache<String> userGroupNameCache;
  private final EntityManager entityManager;

  public DefaultUserGroupService(
      UserGroupStore userGroupStore,
      AclService aclService,
      HibernateCacheManager cacheManager,
      CacheProvider cacheProvider,
      EntityManager entityManager) {
    checkNotNull(userGroupStore);
    checkNotNull(aclService);
    checkNotNull(cacheManager);
    checkNotNull(entityManager);

    this.userGroupStore = userGroupStore;
    this.aclService = aclService;
    this.cacheManager = cacheManager;
    this.entityManager = entityManager;

    userGroupNameCache = cacheProvider.createUserGroupNameCache();
  }

  // -------------------------------------------------------------------------
  // UserGroup
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addUserGroup(UserGroup userGroup) {
    userGroupStore.save(userGroup);
    aclService.invalidateCurrentUserGroupInfoCache();
    return userGroup.getId();
  }

  @Override
  @Transactional
  public void deleteUserGroup(UserGroup userGroup) {
    userGroupStore.delete(userGroup);
    aclService.invalidateCurrentUserGroupInfoCache();
  }

  @Override
  @Transactional
  public void updateUserGroup(UserGroup userGroup) {
    userGroupStore.update(userGroup);

    // Clear query cache due to sharing and user group membership

    cacheManager.clearQueryCache();
    aclService.invalidateCurrentUserGroupInfoCache();
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserGroup> getAllUserGroups() {
    return userGroupStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public UserGroup getUserGroup(long userGroupId) {
    return userGroupStore.get(userGroupId);
  }

  @Override
  @Transactional(readOnly = true)
  public UserGroup getUserGroup(String uid) {
    return userGroupStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canAddOrRemoveMember(String uid) {
    return canAddOrRemoveMember(uid, CurrentUserUtil.getCurrentUserDetails());
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canAddOrRemoveMember(String uid, @Nonnull UserDetails userDetails) {
    return canAddOrRemoveMember(getUserGroup(uid), userDetails);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canAddOrRemoveMember(UserGroup userGroup, @Nonnull UserDetails userDetails) {
    if (userGroup == null) {
      return false;
    }

    boolean canUpdate = aclService.canUpdate(userDetails, userGroup);
    boolean canAddMember =
        userDetails.isAuthorized(Authorities.F_USER_GROUPS_READ_ONLY_ADD_MEMBERS.name());

    return canUpdate || canAddMember;
  }

  @Override
  @Transactional
  public void addUserToGroups(User user, Collection<String> uids, UserDetails currentUser) {
    Session session = entityManager.unwrap(Session.class);
    User lastUpdatedByUser = null;

    for (String uid : uids) {
      UserGroup userGroup = getUserGroup(uid);
      if (canAddOrRemoveMember(userGroup, currentUser)) {
        // Use SQL to add membership (avoids loading members collection)
        if (userGroupStore.addMemberViaSQL(userGroup.getId(), user.getId())) {
          // Update in-memory state
          user.getGroups().add(userGroup);
          // Update lastUpdatedBy
          if (lastUpdatedByUser == null) {
            lastUpdatedByUser = session.getReference(User.class, currentUser.getId());
          }
          userGroup.setLastUpdatedBy(lastUpdatedByUser);
          userGroupStore.updateNoAcl(userGroup);
        }
      }
    }
    aclService.invalidateCurrentUserGroupInfoCache();
  }

  @Override
  @Transactional
  public void removeUserFromGroups(User user, Collection<String> uids) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    Session session = entityManager.unwrap(Session.class);
    User lastUpdatedByUser = null;

    for (String uid : uids) {
      UserGroup userGroup = getUserGroup(uid);
      if (canAddOrRemoveMember(userGroup, currentUser)) {
        // Use SQL to remove membership (avoids loading members collection)
        if (userGroupStore.removeMemberViaSQL(userGroup.getId(), user.getId())) {
          // Update in-memory state
          user.getGroups().remove(userGroup);
          // Update lastUpdatedBy
          if (lastUpdatedByUser == null) {
            lastUpdatedByUser = session.getReference(User.class, currentUser.getId());
          }
          userGroup.setLastUpdatedBy(lastUpdatedByUser);
          userGroupStore.updateNoAcl(userGroup);
        }
      }
    }
    aclService.invalidateCurrentUserGroupInfoCache();
  }

  @Override
  @Transactional
  public void updateUserGroups(
      User user, @Nonnull Collection<String> uids, UserDetails currentUser) {
    Collection<UserGroup> updates = getUserGroupsByUid(uids);
    Set<UserGroup> currentGroups = new HashSet<>(user.getGroups());

    // Determine which groups will have membership changes
    // Groups to add to: in updates but user is not currently a member
    Set<UserGroup> groupsToAddTo = new HashSet<>();
    for (UserGroup userGroup : updates) {
      if (!currentGroups.contains(userGroup) && canAddOrRemoveMember(userGroup, currentUser)) {
        groupsToAddTo.add(userGroup);
      }
    }

    // Groups to remove from: user is currently a member but group not in updates
    Set<UserGroup> groupsToRemoveFrom = new HashSet<>();
    for (UserGroup userGroup : currentGroups) {
      if (!updates.contains(userGroup) && canAddOrRemoveMember(userGroup, currentUser)) {
        groupsToRemoveFrom.add(userGroup);
      }
    }

    // Track which groups actually changed membership
    Set<UserGroup> changedGroups = new HashSet<>();

    // Perform the removals via SQL (avoids loading members collection)
    for (UserGroup userGroup : groupsToRemoveFrom) {
      if (userGroupStore.removeMemberViaSQL(userGroup.getId(), user.getId())) {
        changedGroups.add(userGroup);
        // Update in-memory state to keep Hibernate session consistent
        user.getGroups().remove(userGroup);
      }
    }

    // Perform the additions via SQL (avoids loading members collection)
    for (UserGroup userGroup : groupsToAddTo) {
      if (userGroupStore.addMemberViaSQL(userGroup.getId(), user.getId())) {
        changedGroups.add(userGroup);
        // Update in-memory state to keep Hibernate session consistent
        user.getGroups().add(userGroup);
      }
    }

    // Update lastUpdatedBy for all groups that actually changed
    if (!changedGroups.isEmpty()) {
      Session session = entityManager.unwrap(Session.class);
      User lastUpdatedByUser = session.getReference(User.class, currentUser.getId());
      for (UserGroup userGroup : changedGroups) {
        userGroup.setLastUpdatedBy(lastUpdatedByUser);
        userGroupStore.updateNoAcl(userGroup);
      }
    }

    aclService.invalidateCurrentUserGroupInfoCache();
  }

  private Collection<UserGroup> getUserGroupsByUid(@Nonnull Collection<String> uids) {
    return userGroupStore.getByUid(uids);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserGroup> getUserGroupByName(String name) {
    return userGroupStore.getAllEqName(name);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserGroup> getUserGroupsBetween(int first, int max) {
    return userGroupStore.getAllOrderedName(first, max);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserGroup> getUserGroupsBetweenByName(String name, int first, int max) {
    return userGroupStore.getAllLikeName(name, first, max, false);
  }

  @Override
  @Transactional(readOnly = true)
  public String getDisplayName(String uid) {
    return userGroupNameCache.get(uid, n -> userGroupStore.getByUidNoAcl(uid).getDisplayName());
  }
}
