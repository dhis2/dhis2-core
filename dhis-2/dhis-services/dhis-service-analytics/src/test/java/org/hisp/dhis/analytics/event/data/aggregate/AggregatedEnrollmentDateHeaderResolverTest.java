/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.analytics.event.data.aggregate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.test.TestBase.createPeriodDimensions;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionConstants;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.period.PeriodDimension;
import org.junit.jupiter.api.Test;

class AggregatedEnrollmentDateHeaderResolverTest {
  private final AggregatedEnrollmentDateHeaderResolver subject =
      new AggregatedEnrollmentDateHeaderResolver();

  @Test
  void shouldNormalizeQuotedHeaderKey() {
    assertThat(subject.normalizeHeaderKey("\"incidentdate\""), is("incidentdate"));
    assertThat(subject.normalizeHeaderKey("`eventdate`"), is("eventdate"));
  }

  @Test
  void shouldResolveIncidentDateToOccurredDateColumn() {
    var projection = subject.resolveBaseProjection("incidentdate", false);

    assertTrue(projection.isPresent());
    assertThat(projection.get().sourceColumn(), is("occurreddate"));
    assertThat(projection.get().tableAlias(), is("ax"));
    assertThat(projection.get().alias(), is("incidentdate"));
  }

  @Test
  void shouldResolveCompletedToCompletedDateColumn() {
    var projection = subject.resolveBaseProjection("completed", false);

    assertTrue(projection.isPresent());
    assertThat(projection.get().sourceColumn(), is("completeddate"));
    assertThat(projection.get().tableAlias(), is("ax"));
    assertThat(projection.get().alias(), is("completed"));
  }

  @Test
  void shouldResolveEventDateToJoinColumnWhenEventJoinIsUsed() {
    var projection = subject.resolveBaseProjection("eventdate", true);

    assertTrue(projection.isPresent());
    assertThat(
        projection.get().sourceColumn(),
        is(AggregatedEnrollmentDateHeaderResolver.EVENT_DATE_JOIN_COLUMN));
    assertThat(
        projection.get().tableAlias(),
        is(AggregatedEnrollmentDateHeaderResolver.EVENT_DATE_JOIN_ALIAS));
    assertThat(projection.get().alias(), is("eventdate"));
  }

  @Test
  void shouldNotResolveEventDateToEnrollmentColumnWithoutJoin() {
    assertFalse(subject.resolveBaseProjection("eventdate", false).isPresent());
  }

  @Test
  void shouldNotResolveLastUpdatedAsProjectionOverride() {
    assertFalse(subject.resolveBaseProjection("lastupdated", false).isPresent());
  }

  @Test
  void shouldIdentifyDerivedStaticPeriodHeader() {
    EventQueryParams params = createAggregateParamsWithStaticDatePeriod("INCIDENT_DATE");

    assertTrue(subject.isDerivedStaticPeriodHeader(params, "incidentdate"));
    assertFalse(subject.isDerivedStaticPeriodHeader(params, "lastupdated"));
  }

  @Test
  void shouldRecognizeEventDateAsDerivedStaticPeriodHeader() {
    EventQueryParams params = createAggregateParamsWithStaticDatePeriod("EVENT_DATE");

    assertTrue(subject.isDerivedStaticPeriodHeader(params, "eventdate"));
  }

  private EventQueryParams createAggregateParamsWithStaticDatePeriod(String dateField) {
    List<PeriodDimension> periods = createPeriodDimensions("2022Sep");
    periods.forEach(period -> period.setDateField(dateField));
    DimensionalObject periodDimension =
        new BaseDimensionalObject(
            DimensionConstants.PERIOD_DIM_ID, DimensionType.PERIOD, "yearly", "Period", periods);

    return new EventQueryParams.Builder().addDimension(periodDimension).build();
  }
}
