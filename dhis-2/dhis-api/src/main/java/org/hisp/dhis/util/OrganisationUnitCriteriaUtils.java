/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.util;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.USER_ORGUNIT;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.USER_ORGUNIT_CHILDREN;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.USER_ORGUNIT_GRANDCHILDREN;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;

/** Utilities for organisation unit criteria of incoming analytics request. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrganisationUnitCriteriaUtils {
  /**
   * Converts the given user organisation unit criteria into a list of {@link AnalyticsMetaDataKey}
   * (MetaData).
   *
   * @param userOrganisationUnitsCriteria {@link String} ("ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN").
   * @return a list of {@link AnalyticsMetaDataKeys} or an empty list.
   */
  public static List<AnalyticsMetaDataKey> getAnalyticsMetaDataKeys(
      String userOrganisationUnitsCriteria) {
    List<AnalyticsMetaDataKey> keys = new ArrayList<>();

    // Order matters, don't change unless you know what you're doing.
    AnalyticsMetaDataKey[] analyticsMetaDataKeys = {
      USER_ORGUNIT_CHILDREN, USER_ORGUNIT_GRANDCHILDREN, USER_ORGUNIT
    };
    for (AnalyticsMetaDataKey key : analyticsMetaDataKeys) {
      if (StringUtils.trimToEmpty(userOrganisationUnitsCriteria).contains(key.getKey())) {
        keys.add(key);
        // We need this hack, and the analyticsMetaDataKeys order, because otherwise it would match
        // USER_ORGUNIT_CHILDREN even if we only get USER_ORGUNIT, since the latter contains the
        // former.
        userOrganisationUnitsCriteria =
            userOrganisationUnitsCriteria.replace(key.getKey(), StringUtils.EMPTY);
      }
    }

    return keys;
  }

  /**
   * Transform request criteria into a comma separated string
   * (USER_ORG_UNIT,USER_ORGUNIT_CHILDREN,USER_ORGUNIT_GRANDCHILDREN)
   *
   * @param dimensions, set of the requested dimensions
   * @return string of the criteria, or empty.
   */
  public static String getAnalyticsQueryCriteria(Set<String> dimensions) {
    return isNotEmpty(dimensions)
        ? dimensions.stream()
            .filter(OrganisationUnitCriteriaUtils::containsAnyUserOrgUnitKeyword)
            .collect(Collectors.joining(","))
        : StringUtils.EMPTY;
  }

  private static boolean containsAnyUserOrgUnitKeyword(String dimension) {
    return Stream.of(
            USER_ORGUNIT.getKey(),
            AnalyticsMetaDataKey.USER_ORGUNIT_CHILDREN.getKey(),
            AnalyticsMetaDataKey.USER_ORGUNIT_GRANDCHILDREN.getKey())
        .anyMatch(dimension::contains);
  }
}
