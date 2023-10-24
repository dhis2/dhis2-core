/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.util;

import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNITS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.USER_ORGUNIT;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.USER_ORGUNIT_CHILDREN;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.USER_ORGUNIT_GRANDCHILDREN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.user.User;

/** Utilities for organisation unit criteria of outcoming analytics response. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnalyticsOrganisationUnitUtils {
  /**
   * Retrieve collection of all uids of organisation units belongs to current user and present in
   * response grid.
   *
   * @param currentUser the {@link org.hisp.dhis.user.CurrentUser}.
   * @return intersection of requested user organisation units and all units in response grid.
   */
  public static Collection<Map<String, Object>> getUserOrganisationUnitsItems(
      User currentUser, List<AnalyticsMetaDataKey> userOrganisationUnitsCriteria) {
    List<Map<String, Object>> userOrganisations = new ArrayList<>();

    if (userOrganisationUnitsCriteria == null || userOrganisationUnitsCriteria.isEmpty()) {
      return userOrganisations;
    }

    if (userOrganisationUnitsCriteria.contains(USER_ORGUNIT)) {
      Map<String, Object> userOrganisationUnits =
          AnalyticsOrganisationUnitUtils.getUserOrganisationUnitUidList(USER_ORGUNIT, currentUser);
      userOrganisations.add(userOrganisationUnits);
    }

    if (userOrganisationUnitsCriteria.contains(USER_ORGUNIT_CHILDREN)) {
      Map<String, Object> userChildrenOrganisationUnits =
          AnalyticsOrganisationUnitUtils.getUserOrganisationUnitUidList(
              USER_ORGUNIT_CHILDREN, currentUser);
      userOrganisations.add(userChildrenOrganisationUnits);
    }

    if (userOrganisationUnitsCriteria.contains(USER_ORGUNIT_GRANDCHILDREN)) {
      Map<String, Object> userGrandChildrenOrganisationUnits =
          AnalyticsOrganisationUnitUtils.getUserOrganisationUnitUidList(
              USER_ORGUNIT_GRANDCHILDREN, currentUser);
      userOrganisations.add(userGrandChildrenOrganisationUnits);
    }

    return userOrganisations;
  }

  private static Map<String, Object> getUserOrganisationUnitUidList(
      AnalyticsMetaDataKey analyticsMetaDataKey, User currentUser) {

    List<String> userOrgUnitList;

    switch (analyticsMetaDataKey) {
      case USER_ORGUNIT -> userOrgUnitList =
          currentUser.getOrganisationUnits().stream().map(BaseIdentifiableObject::getUid).toList();
      case USER_ORGUNIT_CHILDREN -> userOrgUnitList =
          currentUser.getOrganisationUnits().stream()
              .flatMap(ou -> ou.getChildren().stream())
              .map(BaseIdentifiableObject::getUid)
              .toList();
      case USER_ORGUNIT_GRANDCHILDREN -> userOrgUnitList =
          currentUser.getOrganisationUnits().stream()
              .flatMap(ou -> ou.getGrandChildren().stream())
              .map(BaseIdentifiableObject::getUid)
              .toList();
      default -> userOrgUnitList = List.of();
    }

    return Map.of(analyticsMetaDataKey.getKey(), Map.of(ORG_UNITS.getKey(), userOrgUnitList));
  }
}
