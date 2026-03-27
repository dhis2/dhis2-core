/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.imports.notification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.tracker.imports.programrule.engine.Notification;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Batch-fetches notification dependencies before async dispatch. Runs in the coordinator async
 * task. Produces a {@link NotificationContext} containing plain data (no Hibernate entities) that
 * crosses thread boundaries safely.
 *
 * <p>All data is fetched via SQL (no Hibernate). Template metadata (recipient type, user group ID)
 * is resolved by joining on the template UID directly, avoiding per-template Hibernate lookups.
 */
@Component
@RequiredArgsConstructor
public class NotificationContextFactory {
  private final NamedParameterJdbcTemplate namedJdbcTemplate;

  public NotificationContext create(List<EntityNotifications> notifications) {
    if (notifications.isEmpty()) {
      return NotificationContext.EMPTY;
    }

    Set<String> templateUids = new HashSet<>();
    for (EntityNotifications en : notifications) {
      for (Notification n : en.notifications()) {
        templateUids.add(n.template().getValue());
      }
    }

    Map<Long, Set<NotificationContext.GroupMemberInfo>> groupMembers =
        fetchGroupMembers(templateUids);
    return new NotificationContext(groupMembers);
  }

  /**
   * Fetches enabled user group members with their org unit UIDs for all USER_GROUP templates in one
   * query. Joins through programnotificationtemplate to resolve template UID -> user group ID
   * without Hibernate.
   */
  private Map<Long, Set<NotificationContext.GroupMemberInfo>> fetchGroupMembers(
      Set<String> templateUids) {
    if (templateUids.isEmpty()) {
      return Map.of();
    }

    String sql =
        """
        select pnt.usergroupid, u.userinfoid, ou.uid as ou_uid
        from programnotificationtemplate pnt
        join usergroupmembers ugm on ugm.usergroupid = pnt.usergroupid
        join userinfo u on u.userinfoid = ugm.userid
        left join usermembership um on um.userinfoid = u.userinfoid
        left join organisationunit ou on ou.organisationunitid = um.organisationunitid
        where pnt.uid in (:templateUids)
          and pnt.notificationrecipienttype = 'USER_GROUP'
          and u.disabled = false
        """;

    MapSqlParameterSource params = new MapSqlParameterSource("templateUids", templateUids);
    Map<Long, Set<NotificationContext.GroupMemberInfo>> result = new HashMap<>(templateUids.size());
    namedJdbcTemplate.query(
        sql,
        params,
        rs -> {
          long groupId = rs.getLong("usergroupid");
          NotificationContext.GroupMemberInfo info =
              new NotificationContext.GroupMemberInfo(
                  rs.getLong("userinfoid"), rs.getString("ou_uid"));
          result.computeIfAbsent(groupId, k -> new HashSet<>()).add(info);
        });

    return result;
  }
}
