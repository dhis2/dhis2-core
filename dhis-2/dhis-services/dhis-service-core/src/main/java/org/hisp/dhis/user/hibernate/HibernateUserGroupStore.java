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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
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
  public Map<Long, Integer> getMemberCounts(@Nonnull Collection<Long> userGroupIds) {
    Map<Long, Integer> result = new HashMap<>();
    if (userGroupIds.isEmpty()) {
      return result;
    }

    String sql =
        """
        SELECT usergroupid, COUNT(userid) as member_count
        FROM usergroupmembers
        WHERE usergroupid IN (:ids)
        GROUP BY usergroupid
        """;

    List<Object[]> rows =
        entityManager.createNativeQuery(sql).setParameter("ids", userGroupIds).getResultList();

    for (Object[] row : rows) {
      Long groupId = ((Number) row[0]).longValue();
      Integer count = ((Number) row[1]).intValue();
      result.put(groupId, count);
    }

    // Groups with no members won't appear in results, add them with count 0
    for (Long id : userGroupIds) {
      result.putIfAbsent(id, 0);
    }

    return result;
  }

  @Override
  public boolean addMemberViaSQL(long userGroupId, long userId) {
    // Check if already a member
    String checkSql = "SELECT 1 FROM usergroupmembers WHERE usergroupid = ? AND userid = ?";
    List<?> existing = jdbcTemplate.queryForList(checkSql, userGroupId, userId);
    if (!existing.isEmpty()) {
      return false; // Already a member
    }

    // Add the membership
    String insertSql = "INSERT INTO usergroupmembers (usergroupid, userid) VALUES (?, ?)";
    jdbcTemplate.update(insertSql, userGroupId, userId);
    return true;
  }

  @Override
  public boolean removeMemberViaSQL(long userGroupId, long userId) {
    String deleteSql = "DELETE FROM usergroupmembers WHERE usergroupid = ? AND userid = ?";
    int rowsAffected = jdbcTemplate.update(deleteSql, userGroupId, userId);
    return rowsAffected > 0;
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
