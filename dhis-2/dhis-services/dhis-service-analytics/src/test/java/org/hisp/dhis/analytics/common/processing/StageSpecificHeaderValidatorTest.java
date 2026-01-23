/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for validation of stage-specific headers against dimensions. When a stage-specific header
 * (e.g., stageUid.eventdate) is requested, there must be a matching stage-specific dimension (e.g.,
 * stageUid.EVENT_DATE:xxx) with the same stage UID and same static dimension type.
 */
class StageSpecificHeaderValidatorTest {

  private Program program;
  private ProgramStage programStage;

  @BeforeEach
  void setUp() {
    program = new Program("Test Program");
    program.setUid("lxAQ7Zs9VYR");

    programStage = new ProgramStage("Test Stage", program);
    programStage.setUid("ZkbAXlQUYJG");
    program.setProgramStages(Set.of(programStage));
  }

  @Test
  void shouldFailWhenHeaderHasEventDateButDimensionHasScheduledDate() {
    // Given - header requests eventdate for a specific stage
    DimensionIdentifier<DimensionParam> header =
        createEventLevelDimensionIdentifier(StaticDimension.EVENT_DATE, DimensionParamType.HEADERS);

    // And - dimension specifies SCHEDULED_DATE for the same stage (mismatch!)
    DimensionIdentifier<DimensionParam> dimension =
        createEventLevelDimensionIdentifier(
            StaticDimension.SCHEDULED_DATE, DimensionParamType.DIMENSIONS);

    Set<DimensionIdentifier<DimensionParam>> parsedHeaders = Set.of(header);
    List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers = List.of(dimension);

    // When/Then - validation should fail
    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class,
            () -> StageSpecificHeaderValidator.validate(parsedHeaders, dimensionIdentifiers));

    assertTrue(
        exception.getMessage().contains("ZkbAXlQUYJG"),
        "Error message should mention the program stage UID");
    assertTrue(
        exception.getMessage().contains("eventdate")
            || exception.getMessage().contains("EVENT_DATE"),
        "Error message should mention the header dimension");
  }

  @Test
  void shouldFailWhenHeaderHasScheduledDateButDimensionHasEventDate() {
    // Given - header requests scheduleddate for a specific stage
    DimensionIdentifier<DimensionParam> header =
        createEventLevelDimensionIdentifier(
            StaticDimension.SCHEDULED_DATE, DimensionParamType.HEADERS);

    // And - dimension specifies EVENT_DATE for the same stage (mismatch!)
    DimensionIdentifier<DimensionParam> dimension =
        createEventLevelDimensionIdentifier(
            StaticDimension.EVENT_DATE, DimensionParamType.DIMENSIONS);

    Set<DimensionIdentifier<DimensionParam>> parsedHeaders = Set.of(header);
    List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers = List.of(dimension);

    // When/Then - validation should fail
    assertThrows(
        IllegalQueryException.class,
        () -> StageSpecificHeaderValidator.validate(parsedHeaders, dimensionIdentifiers));
  }

  @Test
  void shouldPassWhenHeaderAndDimensionMatch() {
    // Given - header requests eventdate for a specific stage
    DimensionIdentifier<DimensionParam> header =
        createEventLevelDimensionIdentifier(StaticDimension.EVENT_DATE, DimensionParamType.HEADERS);

    // And - dimension specifies EVENT_DATE for the same stage (match!)
    DimensionIdentifier<DimensionParam> dimension =
        createEventLevelDimensionIdentifier(
            StaticDimension.EVENT_DATE, DimensionParamType.DIMENSIONS);

    Set<DimensionIdentifier<DimensionParam>> parsedHeaders = Set.of(header);
    List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers = List.of(dimension);

    // When/Then - validation should pass
    assertDoesNotThrow(
        () -> StageSpecificHeaderValidator.validate(parsedHeaders, dimensionIdentifiers));
  }

  @Test
  void shouldPassWhenHeaderHasNoMatchingDimensionButIsNotStageSpecific() {
    // Given - a non-stage-specific header (e.g., just "ouname" without stage prefix)
    DimensionParam ouNameParam =
        DimensionParam.ofObject(
            StaticDimension.OUNAME.name(), DimensionParamType.HEADERS, IdScheme.UID, List.of());
    DimensionIdentifier<DimensionParam> header =
        DimensionIdentifier.of(
            ElementWithOffset.emptyElementWithOffset(),
            ElementWithOffset.emptyElementWithOffset(),
            ouNameParam);

    Set<DimensionIdentifier<DimensionParam>> parsedHeaders = Set.of(header);
    List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers = List.of();

    // When/Then - validation should pass (non-stage-specific headers don't require matching
    // dimensions)
    assertDoesNotThrow(
        () -> StageSpecificHeaderValidator.validate(parsedHeaders, dimensionIdentifiers));
  }

  @Test
  void shouldFailWhenStageSpecificHeaderHasNoDimension() {
    // Given - header requests eventdate for a specific stage
    DimensionIdentifier<DimensionParam> header =
        createEventLevelDimensionIdentifier(StaticDimension.EVENT_DATE, DimensionParamType.HEADERS);

    // And - no dimensions provided at all
    Set<DimensionIdentifier<DimensionParam>> parsedHeaders = Set.of(header);
    List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers = List.of();

    // When/Then - validation should fail (stage-specific header requires matching dimension)
    assertThrows(
        IllegalQueryException.class,
        () -> StageSpecificHeaderValidator.validate(parsedHeaders, dimensionIdentifiers));
  }

  @Test
  void shouldPassWhenMultipleHeadersAndDimensionsMatch() {
    // Given - headers for both EVENT_DATE and OU for the same stage
    DimensionIdentifier<DimensionParam> eventDateHeader =
        createEventLevelDimensionIdentifier(StaticDimension.EVENT_DATE, DimensionParamType.HEADERS);
    DimensionIdentifier<DimensionParam> ouHeader =
        createEventLevelDimensionIdentifier(StaticDimension.OU, DimensionParamType.HEADERS);

    // And - matching dimensions for both
    DimensionIdentifier<DimensionParam> eventDateDimension =
        createEventLevelDimensionIdentifier(
            StaticDimension.EVENT_DATE, DimensionParamType.DIMENSIONS);
    DimensionIdentifier<DimensionParam> ouDimension =
        createEventLevelDimensionIdentifier(StaticDimension.OU, DimensionParamType.DIMENSIONS);

    Set<DimensionIdentifier<DimensionParam>> parsedHeaders = Set.of(eventDateHeader, ouHeader);
    List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers =
        List.of(eventDateDimension, ouDimension);

    // When/Then - validation should pass
    assertDoesNotThrow(
        () -> StageSpecificHeaderValidator.validate(parsedHeaders, dimensionIdentifiers));
  }

  @Test
  void shouldPassWhenOuHeaderMatchesDimensionalObjectOuDimension() {
    // Given - header requests OU for a specific stage (static dimension)
    DimensionIdentifier<DimensionParam> ouHeader =
        createEventLevelDimensionIdentifier(StaticDimension.OU, DimensionParamType.HEADERS);

    // And - dimension is a DimensionalObject-based OU (which occurs when OU items like
    // USER_ORGUNIT or specific UIDs are resolved through dataQueryService)
    DimensionalObject ouDimensionalObject = mock(DimensionalObject.class);
    when(ouDimensionalObject.getDimensionType()).thenReturn(DimensionType.ORGANISATION_UNIT);

    DimensionParam ouDimParam =
        DimensionParam.ofObject(
            ouDimensionalObject, DimensionParamType.DIMENSIONS, IdScheme.UID, List.of());
    DimensionIdentifier<DimensionParam> ouDimension =
        DimensionIdentifier.of(
            ElementWithOffset.of(program, null),
            ElementWithOffset.of(programStage, null),
            ouDimParam);

    Set<DimensionIdentifier<DimensionParam>> parsedHeaders = Set.of(ouHeader);
    List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers = List.of(ouDimension);

    // When/Then - validation should pass (OU header matches DimensionalObject-based OU dimension)
    assertDoesNotThrow(
        () -> StageSpecificHeaderValidator.validate(parsedHeaders, dimensionIdentifiers));
  }

  private DimensionIdentifier<DimensionParam> createEventLevelDimensionIdentifier(
      StaticDimension staticDimension, DimensionParamType paramType) {
    DimensionParam param =
        DimensionParam.ofObject(staticDimension.name(), paramType, IdScheme.UID, List.of());
    return DimensionIdentifier.of(
        ElementWithOffset.of(program, null), ElementWithOffset.of(programStage, null), param);
  }
}
