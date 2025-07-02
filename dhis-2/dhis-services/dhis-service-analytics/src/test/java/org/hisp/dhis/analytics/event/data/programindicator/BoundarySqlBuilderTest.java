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
package org.hisp.dhis.analytics.event.data.programindicator;

import static org.hisp.dhis.program.AnalyticsPeriodBoundary.DB_ENROLLMENT_DATE;
import static org.hisp.dhis.program.AnalyticsPeriodBoundary.DB_INCIDENT_DATE;
import static org.hisp.dhis.program.AnalyticsPeriodBoundary.DB_SCHEDULED_DATE;
import static org.hisp.dhis.program.AnalyticsPeriodBoundary.EVENT_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.AnalyticsPeriodBoundaryType;
import org.hisp.dhis.program.ProgramIndicator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoundarySqlBuilderTest {

  private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  @Mock private ProgramIndicator programIndicator;

  private final SimpleDateFormat dateFormat = new SimpleDateFormat(Period.DEFAULT_DATE_FORMAT);

  private Date reportingStartDate;
  private Date reportingEndDate;

  @BeforeEach
  void setUp() throws ParseException {
    reportingStartDate = dateFormat.parse("2023-01-01");
    reportingEndDate = dateFormat.parse("2023-12-31");
  }

  @Test
  void shouldReturnEmptyStringWhenBoundariesIsNull() {
    String result =
        BoundarySqlBuilder.buildSql(
            null, "eventdate", programIndicator, reportingStartDate, reportingEndDate, sqlBuilder);

    assertEquals("", result);
  }

  @Test
  void shouldReturnEmptyStringWhenBoundariesIsEmpty() {
    Set<AnalyticsPeriodBoundary> emptyBoundaries = new HashSet<>();

    String result =
        BoundarySqlBuilder.buildSql(
            emptyBoundaries,
            "eventdate",
            programIndicator,
            reportingStartDate,
            reportingEndDate,
            sqlBuilder);

    assertEquals("", result);
  }

  @Test
  void shouldSkipNullBoundaries() {
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    boundaries.add(null);
    boundaries.add(
        createEventDateBoundary(AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD));

    String result =
        BoundarySqlBuilder.buildSql(
            boundaries,
            "eventdate",
            programIndicator,
            reportingStartDate,
            reportingEndDate,
            sqlBuilder);

    assertTrue(result.contains("\"eventdate\" >= '2023-01-01'"));
  }

  @Test
  void shouldHandleEventDateBoundary() {
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    boundaries.add(
        createEventDateBoundary(AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD));

    String result =
        BoundarySqlBuilder.buildSql(
            boundaries,
            "occurreddate",
            programIndicator,
            reportingStartDate,
            reportingEndDate,
            sqlBuilder);

    assertEquals(" and \"occurreddate\" >= '2023-01-01'", result);
  }

  @Test
  void shouldHandleEnrollmentDateBoundary() {
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    boundaries.add(
        createEnrollmentDateBoundary(AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD));

    String result =
        BoundarySqlBuilder.buildSql(
            boundaries,
            "eventdate",
            programIndicator,
            reportingStartDate,
            reportingEndDate,
            sqlBuilder);

    assertEquals(" and \"" + DB_ENROLLMENT_DATE + "\" >= '2023-01-01'", result);
  }

  @Test
  void shouldHandleIncidentDateBoundary() {
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    boundaries.add(
        createIncidentDateBoundary(AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD));

    String result =
        BoundarySqlBuilder.buildSql(
            boundaries,
            "eventdate",
            programIndicator,
            reportingStartDate,
            reportingEndDate,
            sqlBuilder);

    assertEquals(" and \"" + DB_INCIDENT_DATE + "\" >= '2023-01-01'", result);
  }

  @Test
  void shouldHandleScheduledDateBoundary() {
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    boundaries.add(
        createScheduledDateBoundary(AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD));

    String result =
        BoundarySqlBuilder.buildSql(
            boundaries,
            "eventdate",
            programIndicator,
            reportingStartDate,
            reportingEndDate,
            sqlBuilder);

    assertEquals(" and \"" + DB_SCHEDULED_DATE + "\" >= '2023-01-01'", result);
  }

  @Test
  void shouldSkipUnsupportedBoundaryType() {
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    AnalyticsPeriodBoundary unsupportedBoundary = new AnalyticsPeriodBoundary();
    unsupportedBoundary.setUid("unsupported-boundary");
    unsupportedBoundary.setAnalyticsPeriodBoundaryType(
        AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD);
    boundaries.add(unsupportedBoundary);

    String result =
        BoundarySqlBuilder.buildSql(
            boundaries,
            "eventdate",
            programIndicator,
            reportingStartDate,
            reportingEndDate,
            sqlBuilder);

    assertEquals("", result);
  }

  @Test
  void shouldSkipBoundaryWithNullBoundaryDate() {
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    AnalyticsPeriodBoundary boundaryWithNullDate = createEventDateBoundary(null);
    boundaries.add(boundaryWithNullDate);

    String result =
        BoundarySqlBuilder.buildSql(
            boundaries,
            "eventdate",
            programIndicator,
            reportingStartDate,
            reportingEndDate,
            sqlBuilder);

    assertEquals("", result);
  }

  @Test
  void shouldHandleEndBoundary() {
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    boundaries.add(
        createEventDateBoundary(AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD));

    String result =
        BoundarySqlBuilder.buildSql(
            boundaries,
            "eventdate",
            programIndicator,
            reportingStartDate,
            reportingEndDate,
            sqlBuilder);

    assertEquals(" and \"eventdate\" < '2024-01-01'", result);
  }

  @Test
  void shouldHandleMultipleBoundaries() {
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    boundaries.add(
        createEventDateBoundary(AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD));
    boundaries.add(
        createEventDateBoundary(AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD));

    String result =
        BoundarySqlBuilder.buildSql(
            boundaries,
            "eventdate",
            programIndicator,
            reportingStartDate,
            reportingEndDate,
            sqlBuilder);

    assertTrue(result.contains(" and \"eventdate\" >= '2023-01-01'"));
    assertTrue(result.contains(" and \"eventdate\" < '2024-01-01'"));
  }

  private AnalyticsPeriodBoundary createEventDateBoundary(
      AnalyticsPeriodBoundaryType boundaryType) {
    AnalyticsPeriodBoundary boundary = new AnalyticsPeriodBoundary();
    boundary.setUid("event-boundary");
    boundary.setAnalyticsPeriodBoundaryType(boundaryType);
    boundary.setBoundaryTarget(EVENT_DATE);
    return boundary;
  }

  private AnalyticsPeriodBoundary createEnrollmentDateBoundary(
      AnalyticsPeriodBoundaryType boundaryType) {
    AnalyticsPeriodBoundary boundary = new AnalyticsPeriodBoundary();
    boundary.setUid("enrollment-boundary");
    boundary.setAnalyticsPeriodBoundaryType(boundaryType);
    boundary.setBoundaryTarget(AnalyticsPeriodBoundary.ENROLLMENT_DATE);
    return boundary;
  }

  private AnalyticsPeriodBoundary createIncidentDateBoundary(
      AnalyticsPeriodBoundaryType boundaryType) {
    AnalyticsPeriodBoundary boundary = new AnalyticsPeriodBoundary();
    boundary.setUid("incident-boundary");
    boundary.setAnalyticsPeriodBoundaryType(boundaryType);
    boundary.setBoundaryTarget(AnalyticsPeriodBoundary.INCIDENT_DATE);
    return boundary;
  }

  private AnalyticsPeriodBoundary createScheduledDateBoundary(
      AnalyticsPeriodBoundaryType boundaryType) {
    AnalyticsPeriodBoundary boundary = new AnalyticsPeriodBoundary();
    boundary.setUid("scheduled-boundary");
    boundary.setAnalyticsPeriodBoundaryType(boundaryType);
    boundary.setBoundaryTarget(AnalyticsPeriodBoundary.SCHEDULED_DATE);
    return boundary;
  }
}
