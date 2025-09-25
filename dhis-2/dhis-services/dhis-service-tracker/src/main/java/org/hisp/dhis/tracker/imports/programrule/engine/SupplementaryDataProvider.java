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

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroupService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupplementaryDataProvider {
  private static final String USER_ROLES = "USER_ROLES";
  private static final String USER_GROUPS = "USER_GROUPS";

  private static final String ORG_UNIT_GROUP_REGEX =
      "d2:inOrgUnitGroup\\( *(([\\d/\\*\\+\\-%\\. ]+)|"
          + "( *'[^']*'))*+( *, *(([\\d/\\*\\+\\-%\\. ]+)|'[^']*'))*+ *\\)";

  private static final String USER_GROUP_REGEX =
      "d2:inUserGroup\\( *(([\\d/\\*\\+\\-%\\. ]+)| *'[^']*') *\\)";

  private static final Pattern ORG_UNIT_GROUP_PATTERN = Pattern.compile(ORG_UNIT_GROUP_REGEX);
  private static final Pattern USER_GROUP_PATTERN = Pattern.compile(USER_GROUP_REGEX);

  @Nonnull private final OrganisationUnitGroupService organisationUnitGroupService;
  @Nonnull private final UserGroupService userGroupService;

  public Map<String, List<String>> getSupplementaryData(
      List<ProgramRule> programRules, UserDetails user) {
    Map<String, List<String>> supplementaryData = Maps.newHashMap();

    Map<String, List<String>> orgUnitGroupData = extractOrgUnitGroups(programRules);
    supplementaryData.putAll(orgUnitGroupData);

    Map<String, List<String>> userGroupData = extractUserGroups(programRules, user);
    supplementaryData.putAll(userGroupData);

    supplementaryData.put(USER_ROLES, new ArrayList<>(user.getUserRoleIds()));

    return supplementaryData;
  }

  private Map<String, List<String>> extractOrgUnitGroups(List<ProgramRule> programRules) {
    List<String> orgUnitGroups = new ArrayList<>();
    for (ProgramRule programRule : programRules) {
      Matcher matcher =
          ORG_UNIT_GROUP_PATTERN.matcher(
              StringUtils.defaultIfBlank(programRule.getCondition(), ""));
      while (matcher.find()) {
        orgUnitGroups.add(StringUtils.replace(matcher.group(1), "'", ""));
      }
    }

    if (orgUnitGroups.isEmpty()) {
      return Collections.emptyMap();
    }

    return orgUnitGroups.stream()
        .collect(
            Collectors.toMap(
                g -> g,
                g ->
                    organisationUnitGroupService.getOrganisationUnitGroup(g).getMembers().stream()
                        .map(OrganisationUnit::getUid)
                        .toList()));
  }

  private Map<String, List<String>> extractUserGroups(
      List<ProgramRule> programRules, UserDetails user) {
    List<String> userGroups = new ArrayList<>();
    for (ProgramRule programRule : programRules) {
      Matcher matcher =
          USER_GROUP_PATTERN.matcher(StringUtils.defaultIfBlank(programRule.getCondition(), ""));
      while (matcher.find()) {
        userGroups.add(matcher.group(1));
      }
    }

    if (userGroups.isEmpty()) {
      return Collections.emptyMap();
    }

    return Map.of(USER_GROUPS, user.getUserGroupIds().stream().toList());
  }
}
