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
import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository("org.hisp.dhis.user.UserGroupStore")
public class HibernateUserGroupStore extends HibernateIdentifiableObjectStore<UserGroup>
    implements UserGroupStore {
  public HibernateUserGroupStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, UserGroup.class, aclService, true);
  }

  @Override
  public void save(@Nonnull UserGroup object, boolean clearSharing) {
    super.save(object, clearSharing);

    // TODO: MAS: send event to invalidate sessions for users in this group
    //        object
    //            .getMembers()
    //            .forEach(member -> currentUserService.invalidateUserGroupCache(member.getUid()));
  }

  @Override
  public boolean addMemberViaSQL(@Nonnull UID userGroupUid, @Nonnull UID userUid) {
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
    return jdbcTemplate.update(sql, userGroupUid.getValue(), userUid.getValue()) > 0;
  }

  @Override
  public boolean removeMemberViaSQL(@Nonnull UID userGroupUid, @Nonnull UID userUid) {
    String sql =
        """
        DELETE FROM usergroupmembers
        WHERE usergroupid = (SELECT usergroupid FROM usergroup WHERE uid = ?)
        AND userid = (SELECT userinfoid FROM userinfo WHERE uid = ?)
        """;
    return jdbcTemplate.update(sql, userGroupUid.getValue(), userUid.getValue()) > 0;
  }

  @Override
  public void updateLastUpdatedViaSQL(@Nonnull UID userGroupUid, @Nonnull UID lastUpdatedByUid) {
    String sql =
        """
        UPDATE usergroup SET lastupdated = now(),
        lastupdatedby = (SELECT userinfoid FROM userinfo WHERE uid = ?)
        WHERE uid = ?
        """;
    jdbcTemplate.update(sql, lastUpdatedByUid.getValue(), userGroupUid.getValue());
    // Evict from both L1 (session) and L2 caches since we bypassed Hibernate
    Long id =
        jdbcTemplate.queryForObject(
            "SELECT usergroupid FROM usergroup WHERE uid = ?", Long.class, userGroupUid.getValue());
    getSession().evict(getSession().getReference(UserGroup.class, id));
    getSession().getSessionFactory().getCache().evictEntityData(UserGroup.class, id);
  }

  @Override
  public void removeAllMembershipsViaSQL(@Nonnull UID userUid) {
    String sql =
        """
        DELETE FROM usergroupmembers
        WHERE userid = (SELECT userinfoid FROM userinfo WHERE uid = ?)
        """;
    jdbcTemplate.update(sql, userUid.getValue());
  }

  @Override
  public void updateLastUpdatedForUserGroupsViaSQL(
      @Nonnull UID userUid, @Nonnull UID lastUpdatedByUid) {
    List<String> groupUids =
        jdbcTemplate.queryForList(
            """
            SELECT ug.uid FROM usergroup ug
            JOIN usergroupmembers ugm ON ug.usergroupid = ugm.usergroupid
            WHERE ugm.userid = (SELECT userinfoid FROM userinfo WHERE uid = ?)
            """,
            String.class,
            userUid.getValue());

    for (String groupUid : groupUids) {
      updateLastUpdatedViaSQL(UID.of(groupUid), lastUpdatedByUid);
    }
  }

  //  @Override
  // TODO: MAS: send event to invalidate sessions for users in this group
  //  public void update(@Nonnull UserGroup object, User user) {
  //    super.update(object, user);
  //    //    object
  //    //        .getMembers()
  //    //        .forEach(member -> currentUserService.invalidateUserGroupCache(member.getUid()));
  //  }
}
