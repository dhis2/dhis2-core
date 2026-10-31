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
package org.hisp.dhis.user.hibernate;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupMembershipChangeCollector;
import org.hisp.dhis.user.UserGroupStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository("org.hisp.dhis.user.UserGroupStore")
public class HibernateUserGroupStore extends HibernateIdentifiableObjectStore<UserGroup>
    implements UserGroupStore {

  private final UserGroupMembershipChangeCollector membershipChangeCollector;

  public HibernateUserGroupStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService,
      UserGroupMembershipChangeCollector membershipChangeCollector) {
    super(entityManager, jdbcTemplate, publisher, UserGroup.class, aclService, true);
    this.membershipChangeCollector = membershipChangeCollector;
  }

  @Override
  @Nonnull
  public Set<Long> getManagedGroupIds(@Nonnull Collection<Long> userGroupIds) {
    if (userGroupIds.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(
        getSession()
            .createQuery(
                "select managed.id from UserGroup g join g.managedGroups managed where g.id in (:userGroupIds)",
                Long.class)
            .setParameterList("userGroupIds", userGroupIds)
            .list());
  }

  @Override
  public boolean addMember(
      @Nonnull UID userGroupUid, @Nonnull UID userUid, @Nonnull UID lastUpdatedByUid) {
    String sql =
        """
        INSERT INTO usergroupmembers (usergroupid, userid)
        SELECT ug.usergroupid, u.userinfoid
        FROM usergroup ug, userinfo u
        WHERE ug.uid = ? AND u.uid = ?
        AND NOT EXISTS (
          SELECT 1 FROM usergroupmembers ugm
          WHERE ugm.usergroupid = ug.usergroupid AND ugm.userid = u.userinfoid
        )
        """;
    boolean changed = jdbcTemplate.update(sql, userGroupUid.getValue(), userUid.getValue()) > 0;
    if (changed) {
      membershipChanged(userUid);
      updateLastUpdated(userGroupUid, lastUpdatedByUid);
    }
    return changed;
  }

  /**
   * Evicts the cached user groups of the user and reports the membership change, since writing the
   * membership via SQL bypasses Hibernate.
   */
  private void membershipChanged(@Nonnull UID userUid) {
    Long userId =
        jdbcTemplate.queryForObject(
            "SELECT userinfoid FROM userinfo WHERE uid = ?", Long.class, userUid.getValue());
    if (userId != null) {
      getSession()
          .getSessionFactory()
          .getCache()
          .evictCollectionData(User.class.getName() + ".groups", userId);
      membershipChangeCollector.membershipsChanged(Set.of(userId));
    }
  }

  @Override
  public void updateLastUpdated(@Nonnull UID userGroupUid, @Nonnull UID lastUpdatedByUid) {
    String sql =
        """
        UPDATE usergroup SET lastupdated = now(),
        lastupdatedby = (SELECT userinfoid FROM userinfo WHERE uid = ?)
        WHERE uid = ?
        """;
    jdbcTemplate.update(sql, lastUpdatedByUid.getValue(), userGroupUid.getValue());
    // Evict from L1 (session) and L2 caches since we bypassed Hibernate's event system.
    // UserGroup and its members collection are both L2-cached via UserGroup.hbm.xml.
    // NOTE: No Redis pub/sub message is published here, so other cluster nodes will retain stale
    // UserGroup entity and members collection data until their L2 TTL expires. A full fix would
    // require publishing via CacheInvalidationMessagePublisher, since PostCacheEventPublisher only
    // fires on Hibernate-managed operations.
    Long id =
        jdbcTemplate.queryForObject(
            "SELECT usergroupid FROM usergroup WHERE uid = ?", Long.class, userGroupUid.getValue());
    getSession().evict(getSession().getReference(UserGroup.class, id));
    getSession().getSessionFactory().getCache().evictEntityData(UserGroup.class, id);
    getSession()
        .getSessionFactory()
        .getCache()
        .evictCollectionData("org.hisp.dhis.user.UserGroup.members", id);
  }

  @Override
  public boolean removeMember(
      @Nonnull UID userGroupUid, @Nonnull UID userUid, @Nonnull UID lastUpdatedByUid) {
    String sql =
        """
        DELETE FROM usergroupmembers
        WHERE usergroupid = (SELECT usergroupid FROM usergroup WHERE uid = ?)
        AND userid = (SELECT userinfoid FROM userinfo WHERE uid = ?)
        """;
    boolean changed = jdbcTemplate.update(sql, userGroupUid.getValue(), userUid.getValue()) > 0;
    if (changed) {
      membershipChanged(userUid);
      updateLastUpdated(userGroupUid, lastUpdatedByUid);
    }
    return changed;
  }

  @Override
  public void removeAllMemberships(@Nonnull UID userUid) {
    jdbcTemplate.update(
        "DELETE FROM usergroupmembers WHERE userid = (SELECT userinfoid FROM userinfo WHERE uid = ?)",
        userUid.getValue());
  }
}
