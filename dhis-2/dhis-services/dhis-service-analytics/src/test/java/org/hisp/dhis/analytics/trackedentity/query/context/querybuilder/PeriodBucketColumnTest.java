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
package org.hisp.dhis.analytics.trackedentity.query.context.querybuilder;

import static org.hisp.dhis.common.IdScheme.UID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PeriodBucketColumnTest {

  @Test
  void resolvesBucketForRelativePeriod() {
    assertEquals("yearly", PeriodBucketColumn.of(eventDate("THIS_YEAR")).orElseThrow());
    assertEquals("monthly", PeriodBucketColumn.of(eventDate("LAST_12_MONTHS")).orElseThrow());
  }

  @Test
  void resolvesBucketForIsoPeriod() {
    assertEquals("yearly", PeriodBucketColumn.of(eventDate("2026")).orElseThrow());
    assertEquals("monthly", PeriodBucketColumn.of(eventDate("202601", "202602")).orElseThrow());
  }

  @Test
  void doesNotResolveBucketForMixedPeriodTypes() {
    assertFalse(PeriodBucketColumn.of(eventDate("THIS_YEAR", "202601")).isPresent());
  }

  @Test
  void doesNotResolveBucketWithoutPeriodItems() {
    assertFalse(PeriodBucketColumn.of(eventDate()).isPresent());
  }

  @Test
  void doesNotResolveBucketForADateRange() {
    assertFalse(PeriodBucketColumn.of(eventDate("2026-01-01_2026-12-31")).isPresent());
  }

  @ParameterizedTest
  @ValueSource(strings = {"GE", "GT", "LE", "LT", "NE"})
  void comparisonsDoNotSelectAPeriodBucket(String operator) {
    assertFalse(PeriodBucketColumn.of(eventDate(operator + ":2021-07-01")).isPresent());
  }

  private DimensionIdentifier<DimensionParam> eventDate(String... items) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            StaticDimension.EVENT_DATE.name(), DimensionParamType.DIMENSIONS, UID, List.of(items));

    Program program = new Program();
    program.setUid("IpHINAT79UW");

    ProgramStage programStage = new ProgramStage();
    programStage.setUid("A03MvHHogjR");

    return DimensionIdentifier.of(
        ElementWithOffset.of(program), ElementWithOffset.of(programStage), dimensionParam);
  }
}
