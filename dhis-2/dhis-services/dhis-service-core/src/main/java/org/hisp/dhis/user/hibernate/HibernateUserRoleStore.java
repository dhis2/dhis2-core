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
import org.hibernate.query.Query;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserRoleStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Repository("org.hisp.dhis.user.UserRoleStore")
public class HibernateUserRoleStore extends HibernateIdentifiableObjectStore<UserRole>
    implements UserRoleStore {
  public HibernateUserRoleStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, UserRole.class, aclService, true);
  }

  @Override
  public int countDataSetUserRoles(DataSet dataSet) {
    Query<Long> query =
        getTypedQuery(
            "select count(distinct c) from UserRole c where :dataSet in elements(c.dataSets)");
    query.setParameter("dataSet", dataSet);

    return query.getSingleResult().intValue();
  }

  @Override
  public boolean addMember(@Nonnull UID userRoleUid, @Nonnull UID userUid) {
    getSession().flush();
    String sql =
        """
        INSERT INTO userrolemembers (userroleid, userid)
        SELECT ur.userroleid, u.userinfoid
        FROM userrole ur, userinfo u
        WHERE ur.uid = :roleUid AND u.uid = :userUid
        AND NOT EXISTS (
          SELECT 1 FROM userrolemembers urm
          WHERE urm.userroleid = ur.userroleid AND urm.userid = u.userinfoid
        )
        """;
    int rows =
        getSession()
            .createNativeQuery(sql)
            .setParameter("roleUid", userRoleUid.getValue())
            .setParameter("userUid", userUid.getValue())
            .executeUpdate();
    if (rows > 0) {
      evictUserRolesCollectionCache(userUid);
    }
    return rows > 0;
  }

  @Override
  public boolean removeMember(@Nonnull UID userRoleUid, @Nonnull UID userUid) {
    String sql =
        """
        DELETE FROM userrolemembers
        WHERE userroleid = (SELECT userroleid FROM userrole WHERE uid = ?)
        AND userid = (SELECT userinfoid FROM userinfo WHERE uid = ?)
        """;
    boolean removed = jdbcTemplate.update(sql, userRoleUid.getValue(), userUid.getValue()) > 0;
    if (removed) {
      evictUserRolesCollectionCache(userUid);
    }
    return removed;
  }

  @Override
  public void removeAllMemberships(@Nonnull UID userUid) {
    String sql =
        """
        DELETE FROM userrolemembers
        WHERE userid = (SELECT userinfoid FROM userinfo WHERE uid = ?)
        """;
    jdbcTemplate.update(sql, userUid.getValue());
    evictUserRolesCollectionCache(userUid);
  }

  @Override
  public void removeAllMembershipsForRole(@Nonnull UID userRoleUid) {
    String sql =
        """
        DELETE FROM userrolemembers
        WHERE userroleid = (SELECT userroleid FROM userrole WHERE uid = ?)
        """;
    jdbcTemplate.update(sql, userRoleUid.getValue());
  }

  @Override
  public void updateLastUpdated(@Nonnull UID userRoleUid, @Nonnull UID lastUpdatedByUid) {
    String sql =
        """
        UPDATE userrole SET lastupdated = now(),
        lastupdatedby = (SELECT userinfoid FROM userinfo WHERE uid = ?)
        WHERE uid = ?
        """;
    jdbcTemplate.update(sql, lastUpdatedByUid.getValue(), userRoleUid.getValue());
    Long id =
        jdbcTemplate.queryForObject(
            "SELECT userroleid FROM userrole WHERE uid = ?", Long.class, userRoleUid.getValue());
    getSession().evict(getSession().getReference(UserRole.class, id));
    getSession().getSessionFactory().getCache().evictEntityData(UserRole.class, id);
  }

  @Override
  public void updateLastUpdatedForUserRoles(@Nonnull UID userUid, @Nonnull UID lastUpdatedByUid) {
    List<String> roleUids =
        jdbcTemplate.queryForList(
            """
            SELECT ur.uid FROM userrole ur
            JOIN userrolemembers urm ON ur.userroleid = urm.userroleid
            WHERE urm.userid = (SELECT userinfoid FROM userinfo WHERE uid = ?)
            """,
            String.class,
            userUid.getValue());

    for (String roleUid : roleUids) {
      updateLastUpdated(UID.of(roleUid), lastUpdatedByUid);
    }
  }

  private void evictUserRolesCollectionCache(@Nonnull UID userUid) {
    Long userId =
        jdbcTemplate.queryForObject(
            "SELECT userinfoid FROM userinfo WHERE uid = ?", Long.class, userUid.getValue());
    if (userId != null) {
      getSession()
          .getSessionFactory()
          .getCache()
          .evictCollectionData(User.class.getName() + ".userRoles", userId);
    }
  }
}
