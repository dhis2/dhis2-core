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
package org.hisp.dhis.analytics.common.params.dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.analytics.trackedentity.query.context.TrackedEntityStaticField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StaticDimensionTest {

  /**
   * The full name is the label a client renders, so it must never fall through to the raw enum
   * constant. The dimensions listed here have no backing {@code TrackedEntityStaticField}, which is
   * what used to expose names like {@code OU} and {@code CREATEDBYDISPLAYNAME} as column labels.
   */
  @ParameterizedTest
  @CsvSource({
    "OU,Organisation unit",
    "OUNAME,Organisation unit name",
    "OUCODE,Organisation unit code",
    "OUNAMEHIERARCHY,Organisation unit hierarchy",
    "CREATED,Created",
    "CREATEDBYDISPLAYNAME,Created by",
    "LASTUPDATEDBYDISPLAYNAME,Last updated by",
    "LASTUPDATED,Last updated",
    "TRACKEDENTITY,Tracked entity",
    "ENROLLMENTDATE,Enrollment Date"
  })
  void testFullNameIsALabelAndNotTheEnumConstant(StaticDimension dimension, String expected) {
    assertEquals(expected, dimension.getFullName());
  }

  /**
   * The same column must read the same on the tracked entity query and aggregate endpoints. The
   * query endpoint labels it from {@link TrackedEntityStaticField}; a requested dimension is
   * labelled from this enum.
   */
  @ParameterizedTest
  @CsvSource({
    "OUNAME,ORG_UNIT_NAME",
    "OUCODE,ORG_UNIT_CODE",
    "OUNAMEHIERARCHY,ORG_UNIT_NAME_HIERARCHY",
    "CREATED,CREATED",
    "CREATEDBYDISPLAYNAME,CREATED_BY_DISPLAY_NAME",
    "LASTUPDATED,LAST_UPDATED",
    "LASTUPDATEDBYDISPLAYNAME,LAST_UPDATED_BY_DISPLAY_NAME",
    "TRACKEDENTITY,TRACKED_ENTITY",
    "LONGITUDE,LONGITUDE",
    "LATITUDE,LATITUDE",
    "GEOMETRY,GEOMETRY"
  })
  void testFullNameMatchesTheTrackedEntityStaticFieldLabel(
      StaticDimension dimension, TrackedEntityStaticField staticField) {
    assertEquals(staticField.getFullName(), dimension.getFullName());
  }

  @Test
  void testEveryFullNameDiffersFromTheEnumConstant() {
    for (StaticDimension dimension : StaticDimension.values()) {
      assertNotEquals(
          dimension.name(),
          dimension.getFullName(),
          "getFullName() must return a label, not the enum constant, for " + dimension.name());
    }
  }

  @Test
  void testMatchesByExactEnumName() {
    Optional<StaticDimension> result = StaticDimension.of("ENROLLMENTDATE");

    assertTrue(result.isPresent());
    assertEquals(StaticDimension.ENROLLMENTDATE, result.get());
  }

  @Test
  void testMatchesByExactEnumNameCaseInsensitive() {
    Optional<StaticDimension> result = StaticDimension.of("enrollmentdate");

    assertTrue(result.isPresent());
    assertEquals(StaticDimension.ENROLLMENTDATE, result.get());
  }

  @Test
  void testMatchesEnrollmentDateWithUnderscore() {
    Optional<StaticDimension> result = StaticDimension.of("ENROLLMENT_DATE");

    assertTrue(result.isPresent());
    assertEquals(StaticDimension.ENROLLMENTDATE, result.get());
  }

  @Test
  void testMatchesEnrollmentDateWithUnderscoreLowercase() {
    Optional<StaticDimension> result = StaticDimension.of("enrollment_date");

    assertTrue(result.isPresent());
    assertEquals(StaticDimension.ENROLLMENTDATE, result.get());
  }

  @Test
  void testMatchesIncidentDateWithUnderscore() {
    Optional<StaticDimension> result = StaticDimension.of("INCIDENT_DATE");

    assertTrue(result.isPresent());
    assertEquals(StaticDimension.INCIDENTDATE, result.get());
  }

  @Test
  void testMatchesOccurredDateWithUnderscore() {
    Optional<StaticDimension> result = StaticDimension.of("OCCURRED_DATE");

    assertTrue(result.isPresent());
    assertEquals(StaticDimension.OCCURREDDATE, result.get());
  }

  @Test
  void testMatchesProgramStatusByEnumName() {
    Optional<StaticDimension> result = StaticDimension.of("PROGRAM_STATUS");

    assertTrue(result.isPresent());
    assertEquals(StaticDimension.PROGRAM_STATUS, result.get());
  }

  @Test
  void testMatchesProgramStatusByColumnName() {
    Optional<StaticDimension> result = StaticDimension.of("programstatus");

    assertTrue(result.isPresent());
    assertEquals(StaticDimension.PROGRAM_STATUS, result.get());
  }

  @Test
  void testMatchesEnrollmentStatusByEnumName() {
    Optional<StaticDimension> result = StaticDimension.of("ENROLLMENT_STATUS");

    assertTrue(result.isPresent());
    assertEquals(StaticDimension.ENROLLMENT_STATUS, result.get());
  }

  @Test
  void testMatchesEventStatusWithUnderscore() {
    Optional<StaticDimension> result = StaticDimension.of("EVENT_STATUS");

    assertTrue(result.isPresent());
    assertEquals(StaticDimension.EVENT_STATUS, result.get());
  }

  @Test
  void testMatchesLastUpdatedWithUnderscore() {
    Optional<StaticDimension> result = StaticDimension.of("LAST_UPDATED");

    assertTrue(result.isPresent());
    assertEquals(StaticDimension.LASTUPDATED, result.get());
  }

  @Test
  void testReturnsEmptyForUnknownDimension() {
    Optional<StaticDimension> result = StaticDimension.of("UNKNOWN_DIMENSION");

    assertTrue(result.isEmpty());
  }

  @Test
  void testReturnsEmptyForNull() {
    Optional<StaticDimension> result = StaticDimension.of(null);

    assertTrue(result.isEmpty());
  }
}
