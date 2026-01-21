/*
 * Copyright (c) 2004-2025, University of Oslo
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

// ABOUTME: Tests for EventAttributeQueryBuilder which handles stage-level dimensions
// ABOUTME: like EVENT_DATE, SCHEDULED_DATE, OU, and EVENT_STATUS.

import static org.hisp.dhis.common.IdScheme.UID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventAttributeQueryBuilderTest {

  private EventAttributeQueryBuilder builder;

  @BeforeEach
  void setUp() {
    builder = new EventAttributeQueryBuilder();
  }

  @Test
  void testHeaderFilterAcceptsEventDateAtEventLevel() {
    DimensionIdentifier<DimensionParam> eventDateDimension =
        createEventLevelDimension(StaticDimension.EVENT_DATE);

    boolean accepted =
        builder.getHeaderFilters().stream().allMatch(p -> p.test(eventDateDimension));

    assertTrue(accepted);
  }

  @Test
  void testFilterAcceptsEventDateAtEventLevel() {
    DimensionIdentifier<DimensionParam> eventDateDimension =
        createEventLevelDimension(StaticDimension.EVENT_DATE);

    boolean accepted =
        builder.getDimensionFilters().stream().allMatch(p -> p.test(eventDateDimension));

    assertTrue(accepted);
  }

  @Test
  void testFilterAcceptsScheduledDateAtEventLevel() {
    DimensionIdentifier<DimensionParam> scheduledDateDimension =
        createEventLevelDimension(StaticDimension.SCHEDULED_DATE);

    boolean accepted =
        builder.getDimensionFilters().stream().allMatch(p -> p.test(scheduledDateDimension));

    assertTrue(accepted);
  }

  @Test
  void testFilterAcceptsOuAtEventLevel() {
    DimensionIdentifier<DimensionParam> ouDimension = createEventLevelDimension(StaticDimension.OU);

    boolean accepted = builder.getDimensionFilters().stream().allMatch(p -> p.test(ouDimension));

    assertTrue(accepted);
  }

  @Test
  void testFilterAcceptsEventStatusAtEventLevel() {
    DimensionIdentifier<DimensionParam> eventStatusDimension =
        createEventLevelDimension(StaticDimension.EVENT_STATUS);

    boolean accepted =
        builder.getDimensionFilters().stream().allMatch(p -> p.test(eventStatusDimension));

    assertTrue(accepted);
  }

  @Test
  void testFilterRejectsEnrollmentStatusAtEventLevel() {
    DimensionIdentifier<DimensionParam> enrollmentStatusDimension =
        createEventLevelDimension(StaticDimension.ENROLLMENT_STATUS);

    boolean accepted =
        builder.getDimensionFilters().stream().allMatch(p -> p.test(enrollmentStatusDimension));

    assertFalse(accepted);
  }

  @Test
  void testFilterRejectsEventDateAtEnrollmentLevel() {
    DimensionIdentifier<DimensionParam> eventDateDimension =
        createEnrollmentLevelDimension(StaticDimension.EVENT_DATE);

    boolean accepted =
        builder.getDimensionFilters().stream().allMatch(p -> p.test(eventDateDimension));

    assertFalse(accepted);
  }

  @Test
  void testFilterRejectsEventDateAtTeLevel() {
    DimensionIdentifier<DimensionParam> eventDateDimension =
        createTeLevelDimension(StaticDimension.EVENT_DATE);

    boolean accepted =
        builder.getDimensionFilters().stream().allMatch(p -> p.test(eventDateDimension));

    assertFalse(accepted);
  }

  private DimensionIdentifier<DimensionParam> createEventLevelDimension(
      StaticDimension staticDimension) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            staticDimension.name(), DimensionParamType.DIMENSIONS, UID, List.of());

    Program program = createProgram();
    ProgramStage programStage = createProgramStage();

    return DimensionIdentifier.of(
        ElementWithOffset.of(program, 0), ElementWithOffset.of(programStage, 0), dimensionParam);
  }

  private DimensionIdentifier<DimensionParam> createEnrollmentLevelDimension(
      StaticDimension staticDimension) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            staticDimension.name(), DimensionParamType.DIMENSIONS, UID, List.of());

    Program program = createProgram();

    return DimensionIdentifier.of(
        ElementWithOffset.of(program, 0),
        ElementWithOffset.emptyElementWithOffset(),
        dimensionParam);
  }

  private DimensionIdentifier<DimensionParam> createTeLevelDimension(
      StaticDimension staticDimension) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            staticDimension.name(), DimensionParamType.DIMENSIONS, UID, List.of());

    return DimensionIdentifier.of(
        ElementWithOffset.emptyElementWithOffset(),
        ElementWithOffset.emptyElementWithOffset(),
        dimensionParam);
  }

  private Program createProgram() {
    Program program = new Program();
    program.setUid("programUid");
    TrackedEntityType tet = new TrackedEntityType();
    tet.setUid("tetUid");
    program.setTrackedEntityType(tet);
    return program;
  }

  private ProgramStage createProgramStage() {
    ProgramStage programStage = new ProgramStage();
    programStage.setUid("programStageUid");
    return programStage;
  }
}
