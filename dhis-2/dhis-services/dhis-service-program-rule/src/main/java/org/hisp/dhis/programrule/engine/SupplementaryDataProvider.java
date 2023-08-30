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

import com.google.common.collect.Maps;
import java.util.ArrayList;
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
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserRole;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupplementaryDataProvider {
  private static final String USER = "USER";

  private static final String REGEX =
      "d2:inOrgUnitGroup\\( *(([\\d/\\*\\+\\-%\\. ]+)|"
          + "( *'[^']*'))*+( *, *(([\\d/\\*\\+\\-%\\. ]+)|'[^']*'))*+ *\\)";

  private static final Pattern PATTERN = Pattern.compile(REGEX);

  @Nonnull private final OrganisationUnitGroupService organisationUnitGroupService;

  @Nonnull private final CurrentUserService currentUserService;

  public Map<String, List<String>> getSupplementaryData(List<ProgramRule> programRules) {
    List<String> orgUnitGroups = new ArrayList<>();

    for (ProgramRule programRule : programRules) {
      Matcher matcher = PATTERN.matcher(StringUtils.defaultIfBlank(programRule.getCondition(), ""));

      while (matcher.find()) {
        orgUnitGroups.add(StringUtils.replace(matcher.group(1), "'", ""));
      }
    }

    Map<String, List<String>> supplementaryData = Maps.newHashMap();

    if (!orgUnitGroups.isEmpty()) {
      supplementaryData =
          orgUnitGroups.stream()
              .collect(
                  Collectors.toMap(
                      g -> g,
                      g ->
                          organisationUnitGroupService
                              .getOrganisationUnitGroup(g)
                              .getMembers()
                              .stream()
                              .map(OrganisationUnit::getUid)
                              .collect(Collectors.toList())));
    }

    if (currentUserService.getCurrentUser() != null) {
      supplementaryData.put(
          USER,
          currentUserService.getCurrentUser().getUserRoles().stream()
              .map(UserRole::getUid)
              .collect(Collectors.toList()));
    }

    return supplementaryData;
  }
}
