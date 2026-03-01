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
import java.util.Set;
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
  public void removeAllMemberships(@Nonnull UID userUid) {
    jdbcTemplate.update(
        "DELETE FROM userrolemembers WHERE userid = (SELECT userinfoid FROM userinfo WHERE uid = ?)",
        userUid.getValue());
  }

  @Override
  public int countDataSetUserRoles(DataSet dataSet) {
    Query<Long> query =
        getTypedQuery(
            "select count(distinct c) from UserRole c where :dataSet in elements(c.dataSets)");
    query.setParameter("dataSet", dataSet);

    return query.getSingleResult().intValue();
  }

  /**
   * Bypasses the generic JPA Criteria implementation to avoid triggering Hibernate auto-flush,
   * which would cascade through User.userRoles and load the entire UserRole.members collection
   * (potentially hundreds of thousands of rows).
   *
   * <p>createdBy → column 'userid' in userrole (see UserRole.hbm.xml)<br>
   * lastUpdatedBy → column 'lastupdatedby' (from identifiableProperties.hbm)
   */
  @Override
  public boolean existsByUser(@Nonnull User user, final Set<String> checkProperties) {
    boolean checkCreatedBy = checkProperties.contains("createdBy");
    boolean checkLastUpdatedBy = checkProperties.contains("lastUpdatedBy");
    if (!checkCreatedBy && !checkLastUpdatedBy) {
      return false;
    }
    long userId = user.getId();
    if (checkCreatedBy && checkLastUpdatedBy) {
      return Boolean.TRUE.equals(
          jdbcTemplate.queryForObject(
              "SELECT EXISTS(SELECT 1 FROM userrole WHERE userid = ? OR lastupdatedby = ?)",
              Boolean.class,
              userId,
              userId));
    } else if (checkCreatedBy) {
      return Boolean.TRUE.equals(
          jdbcTemplate.queryForObject(
              "SELECT EXISTS(SELECT 1 FROM userrole WHERE userid = ?)", Boolean.class, userId));
    } else {
      return Boolean.TRUE.equals(
          jdbcTemplate.queryForObject(
              "SELECT EXISTS(SELECT 1 FROM userrole WHERE lastupdatedby = ?)",
              Boolean.class,
              userId));
    }
  }
}
