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
package org.hisp.dhis.tracker.imports.programrule.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.rules.api.RuleSupplementaryData;
import org.hisp.dhis.user.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SupplementaryDataProvider {

  private static final String ORG_UNIT_GROUP_MEMBERS_SQL =
      """
      SELECT oug.uid, oug.name, ou.uid AS ou_uid
      FROM orgunitgroup oug
      JOIN orgunitgroupmembers ougm ON ougm.orgunitgroupid = oug.orgunitgroupid
      JOIN organisationunit ou ON ou.organisationunitid = ougm.organisationunitid
      WHERE oug.uid = ANY(:identifiers) OR oug.name = ANY(:identifiers)
      """;

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public SupplementaryDataProvider(
      @Qualifier("readOnlyJdbcTemplate") org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
  }

  SupplementaryDataProvider(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public RuleSupplementaryData getSupplementaryData(Set<String> orgUnitGroups, UserDetails user) {
    List<String> userGroups = user.getUserGroupIds().stream().toList();
    List<String> userRoles = user.getUserRoleIds().stream().toList();

    if (orgUnitGroups.isEmpty()) {
      return new RuleSupplementaryData(userGroups, userRoles, Collections.emptyMap());
    }

    String[] identifiers = orgUnitGroups.toArray(String[]::new);
    MapSqlParameterSource params = new MapSqlParameterSource("identifiers", identifiers);

    // Group member UIDs by whichever identifier (UID or name) was used in the rule expression.
    // A UID takes precedence if it appears in the requested set; otherwise fall back to name.
    Map<String, List<String>> resolved = new HashMap<>();
    jdbcTemplate.query(
        ORG_UNIT_GROUP_MEMBERS_SQL,
        params,
        rs -> {
          String groupUid = rs.getString("uid");
          String groupName = rs.getString("name");

          if (orgUnitGroups.contains(groupUid)) {
            resolved.computeIfAbsent(groupUid, k -> new ArrayList<>()).add(rs.getString("ou_uid"));
          }
          if (orgUnitGroups.contains(groupName)) {
            resolved.computeIfAbsent(groupName, k -> new ArrayList<>()).add(rs.getString("ou_uid"));
          }
        });

    Map<String, List<String>> orgUnitGroupData =
        orgUnitGroups.stream()
            .collect(Collectors.toMap(id -> id, id -> resolved.getOrDefault(id, List.of())));

    return new RuleSupplementaryData(userGroups, userRoles, orgUnitGroupData);
  }
}
