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
package org.hisp.dhis.util;

import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.common.AggregateAnalyticsQueryCriteria;
import org.hisp.dhis.common.EnrollmentAnalyticsQueryCriteria;
import org.hisp.dhis.common.EventsAnalyticsQueryCriteria;

public class OrganisationUnitCriteriaUtils {
  public static List<AnalyticsMetaDataKey> getAnalyticsMetaDataKeys(
      String userOrganisationUnitsCriteria) {
    List<AnalyticsMetaDataKey> keys = new ArrayList<>();

    if (userOrganisationUnitsCriteria == null || userOrganisationUnitsCriteria.isEmpty()) {
      return keys;
    }

    userOrganisationUnitsCriteria = userOrganisationUnitsCriteria.replace("ou:", StringUtils.EMPTY);
    List<String> criteria = Arrays.stream(userOrganisationUnitsCriteria.split(";")).toList();
    return criteria.stream()
        .filter(
            c ->
                c.equalsIgnoreCase(AnalyticsMetaDataKey.USER_ORGUNIT.getKey())
                    || c.equalsIgnoreCase(AnalyticsMetaDataKey.USER_ORGUNIT_CHILDREN.getKey())
                    || c.equalsIgnoreCase(AnalyticsMetaDataKey.USER_ORGUNIT_GRANDCHILDREN.getKey()))
        .map(AnalyticsMetaDataKey::valueOf)
        .toList();
  }

  public static String getAnalyticsQueryCriteria(EnrollmentAnalyticsQueryCriteria criteria) {
    return hasDimensions(criteria.getDimension())
        ? criteria.getDimension().stream()
            .filter(d -> d.contains(ORGUNIT_DIM_ID))
            .collect(Collectors.joining(","))
        : StringUtils.EMPTY;
  }

  public static String getAnalyticsQueryCriteria(EventsAnalyticsQueryCriteria criteria) {
    return hasDimensions(criteria.getDimension())
        ? criteria.getDimension().stream()
            .filter(d -> d.contains(ORGUNIT_DIM_ID))
            .collect(Collectors.joining(","))
        : StringUtils.EMPTY;
  }

  public static String getAnalyticsQueryCriteria(AggregateAnalyticsQueryCriteria criteria) {
    return hasDimensions(criteria.getDimension())
        ? criteria.getDimension().stream()
            .filter(d -> d.contains(ORGUNIT_DIM_ID))
            .collect(Collectors.joining(","))
        : StringUtils.EMPTY;
  }

  private static boolean hasDimensions(Set<String> dimensions) {
    return dimensions != null && !dimensions.isEmpty();
  }
}
