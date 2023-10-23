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

import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.USER_ORGUNIT;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.USER_ORGUNIT_CHILDREN;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.USER_ORGUNIT_GRANDCHILDREN;
import static org.hisp.dhis.util.OrganisationUnitCriteriaUtils.getAnalyticsMetaDataKeys;
import static org.hisp.dhis.util.OrganisationUnitCriteriaUtils.getAnalyticsQueryCriteria;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.common.AggregateAnalyticsQueryCriteria;
import org.hisp.dhis.common.EnrollmentAnalyticsQueryCriteria;
import org.hisp.dhis.common.EventsAnalyticsQueryCriteria;
import org.junit.jupiter.api.Test;

class OrganisationUnitCriteriaUtilsTest {
  private static final String validOuDimensions =
      "ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN;USER_ORGUNIT_GRANDCHILDREN";
  private static final String invalidOuDimensions =
      "USER_ORGUNIT;USER_ORGUNIT_CHILDREN;USER_ORGUNIT_GRANDCHILDREN";

  @Test
  void testGetAnalyticsMetaDataKeys_All() {
    // given
    // when
    List<AnalyticsMetaDataKey> keys = getAnalyticsMetaDataKeys(validOuDimensions);
    // then
    assertEquals(3, keys.size());
    assertEquals(USER_ORGUNIT.getKey(), keys.get(0).getKey());
    assertEquals(USER_ORGUNIT_CHILDREN.getKey(), keys.get(1).getKey());
    assertEquals(USER_ORGUNIT_GRANDCHILDREN.getKey(), keys.get(2).getKey());
  }

  @Test
  void testGetAnalyticsMetaDataKeys_Unsupported() {
    // given
    // when
    List<AnalyticsMetaDataKey> keys = getAnalyticsMetaDataKeys(invalidOuDimensions);

    // then
    assertEquals(0, keys.size());
  }

  @Test
  void testGetAnalyticsQueryCriteria_Enrollment() {
    // given
    EnrollmentAnalyticsQueryCriteria enrollmentAnalyticsQueryCriteria =
        new EnrollmentAnalyticsQueryCriteria();
    enrollmentAnalyticsQueryCriteria.setDimension(Set.of(validOuDimensions));

    // when
    String analyticsQueryCriteria = getAnalyticsQueryCriteria(enrollmentAnalyticsQueryCriteria);

    // then
    assertEquals(validOuDimensions, analyticsQueryCriteria);
  }

  @Test
  void testGetAnalyticsQueryCriteria_Event() {
    // given
    EventsAnalyticsQueryCriteria eventsAnalyticsQueryCriteria = new EventsAnalyticsQueryCriteria();
    eventsAnalyticsQueryCriteria.setDimension(Set.of(validOuDimensions));

    // when
    String analyticsQueryCriteria = getAnalyticsQueryCriteria(eventsAnalyticsQueryCriteria);

    // then
    assertEquals(validOuDimensions, analyticsQueryCriteria);
  }

  @Test
  void testGetAnalyticsQueryCriteria_Aggregate() {
    // given
    AggregateAnalyticsQueryCriteria aggregateAnalyticsQueryCriteria =
        new AggregateAnalyticsQueryCriteria();
    aggregateAnalyticsQueryCriteria.setDimension(Set.of(validOuDimensions));

    // when
    String analyticsQueryCriteria = getAnalyticsQueryCriteria(aggregateAnalyticsQueryCriteria);

    // then
    assertEquals(validOuDimensions, analyticsQueryCriteria);
  }

  @Test
  void testGetAnalyticsQueryCriteria_Enrollment_No_Dimension() {
    // given
    EnrollmentAnalyticsQueryCriteria enrollmentAnalyticsQueryCriteria =
        new EnrollmentAnalyticsQueryCriteria();
    enrollmentAnalyticsQueryCriteria.setDimension(Set.of(invalidOuDimensions));

    // when
    String analyticsQueryCriteria = getAnalyticsQueryCriteria(enrollmentAnalyticsQueryCriteria);

    // then
    assertEquals(StringUtils.EMPTY, analyticsQueryCriteria);
  }

  @Test
  void testGetAnalyticsQueryCriteria_Event_No_Dimension() {
    // given
    EventsAnalyticsQueryCriteria eventsAnalyticsQueryCriteria = new EventsAnalyticsQueryCriteria();
    eventsAnalyticsQueryCriteria.setDimension(Set.of(invalidOuDimensions));

    // when
    String analyticsQueryCriteria = getAnalyticsQueryCriteria(eventsAnalyticsQueryCriteria);

    // then
    assertEquals(StringUtils.EMPTY, analyticsQueryCriteria);
  }

  @Test
  void testGetAnalyticsQueryCriteria_Aggregate_No_Dimension() {
    // given
    AggregateAnalyticsQueryCriteria aggregateAnalyticsQueryCriteria =
        new AggregateAnalyticsQueryCriteria();
    aggregateAnalyticsQueryCriteria.setDimension(Set.of(invalidOuDimensions));

    // when
    String analyticsQueryCriteria = getAnalyticsQueryCriteria(aggregateAnalyticsQueryCriteria);

    // then
    assertEquals(StringUtils.EMPTY, analyticsQueryCriteria);
  }
}
