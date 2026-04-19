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
package org.hisp.dhis.programrule.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.rules.api.RuleSupplementaryData;
import org.hisp.dhis.user.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SupplementaryDataProvider {

  /**
   * For each given org unit, returns the groups it belongs to. Keying by both {@code uid} and
   * {@code code} lets rule expressions use either identifier in {@code d2:inOrgUnitGroup('...')}.
   */
  private static final String ORG_UNIT_GROUP_MEMBERS_SQL =
      """
      SELECT oug.uid, oug.code, ou.uid AS ou_uid
      FROM orgunitgroup oug
      JOIN orgunitgroupmembers ougm ON ougm.orgunitgroupid = oug.orgunitgroupid
      JOIN organisationunit ou ON ou.organisationunitid = ougm.organisationunitid
      WHERE ou.uid = ANY(:orgUnitUids)
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

  /**
   * Builds supplementary data for the rule engine.
   *
   * <p>When {@code needsOrgUnitGroups} is {@code true}, queries all org unit groups that contain
   * any of the given {@code orgUnitUids} and returns a map keyed by both group UID and group code
   * so that rule expressions can reference groups by either identifier.
   */
  public RuleSupplementaryData getSupplementaryData(
      boolean needsOrgUnitGroups, Set<String> orgUnitUids, UserDetails user) {
    List<String> userGroups = user.getUserGroupIds().stream().toList();
    List<String> userRoles = user.getUserRoleIds().stream().toList();

    if (!needsOrgUnitGroups || orgUnitUids.isEmpty()) {
      return new RuleSupplementaryData(userGroups, userRoles, Collections.emptyMap());
    }

    MapSqlParameterSource params =
        new MapSqlParameterSource("orgUnitUids", orgUnitUids.toArray(String[]::new));

    Map<String, List<String>> orgUnitGroupData = new HashMap<>();
    jdbcTemplate.query(
        ORG_UNIT_GROUP_MEMBERS_SQL,
        params,
        rs -> {
          String groupUid = rs.getString("uid");
          String groupCode = rs.getString("code");
          String ouUid = rs.getString("ou_uid");
          orgUnitGroupData.computeIfAbsent(groupUid, k -> new ArrayList<>()).add(ouUid);
          if (groupCode != null && !groupCode.isBlank()) {
            orgUnitGroupData.computeIfAbsent(groupCode, k -> new ArrayList<>()).add(ouUid);
          }
        });

    return new RuleSupplementaryData(userGroups, userRoles, orgUnitGroupData);
  }
}
