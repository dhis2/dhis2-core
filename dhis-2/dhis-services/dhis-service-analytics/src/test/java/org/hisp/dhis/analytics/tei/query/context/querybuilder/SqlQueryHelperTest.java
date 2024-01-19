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
package org.hisp.dhis.analytics.tei.query.context.querybuilder;

import static org.junit.jupiter.api.Assertions.*;

import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlParameterManager;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SqlQueryHelper}. */
class SqlQueryHelperTest {

  @Test
  void testEnrollmentSelectPositiveOffset() {
    // Given
    int positiveOffset = 1;

    Program program = new Program();
    program.setUid("uid1");

    ElementWithOffset<Program> programElement = ElementWithOffset.of(program, positiveOffset);

    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid("uid2");

    SqlParameterManager sqlParameterManager = new SqlParameterManager();

    // When
    String statement =
        SqlQueryHelper.enrollmentSelect(programElement, trackedEntityType, sqlParameterManager);

    // Then
    assertEquals(
        "select innermost_enr.* from (select *, row_number() over (partition by trackedentityinstanceuid order by enrollmentdate desc) as rn  from analytics_tei_enrollments_uid2 where programuid = :1) innermost_enr where innermost_enr.rn = 2",
        statement);
  }

  @Test
  void testEnrollmentSelectNegativeOffset() {
    // Given
    int negativeOffset = -1;

    Program program = new Program();
    program.setUid("uid1");

    ElementWithOffset<Program> programElement = ElementWithOffset.of(program, negativeOffset);

    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid("uid2");

    SqlParameterManager sqlParameterManager = new SqlParameterManager();

    // When
    String statement =
        SqlQueryHelper.enrollmentSelect(programElement, trackedEntityType, sqlParameterManager);

    // Then
    assertEquals(
        "select innermost_enr.* from (select *, row_number() over (partition by trackedentityinstanceuid order by enrollmentdate asc) as rn  from analytics_tei_enrollments_uid2 where programuid = :1) innermost_enr where innermost_enr.rn = 1",
        statement);
  }

  @Test
  void testEnrollmentSelectZeroOffset() {
    // Given
    int zeroOffset = 0;

    Program program = new Program();
    program.setUid("uid1");

    ElementWithOffset<Program> programElement = ElementWithOffset.of(program, zeroOffset);

    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid("uid2");

    SqlParameterManager sqlParameterManager = new SqlParameterManager();

    // When
    String statement =
        SqlQueryHelper.enrollmentSelect(programElement, trackedEntityType, sqlParameterManager);

    // Then
    assertEquals(
        "select innermost_enr.* from (select *, row_number() over (partition by trackedentityinstanceuid order by enrollmentdate desc) as rn  from analytics_tei_enrollments_uid2 where programuid = :1) innermost_enr where innermost_enr.rn = 1",
        statement);
  }

  @Test
  void testEventSelectZeroOffset() {
    // Given
    int zeroOffset = 0;

    Program program = new Program();
    program.setUid("uid1");

    ProgramStage programStage = new ProgramStage();
    programStage.setProgram(program);

    ElementWithOffset<Program> programElement = ElementWithOffset.of(program, zeroOffset);
    ElementWithOffset<ProgramStage> programStageElement =
        ElementWithOffset.of(programStage, zeroOffset);

    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid("uid2");

    SqlParameterManager sqlParameterManager = new SqlParameterManager();

    // When
    String statement =
        SqlQueryHelper.eventSelect(
            programElement, programStageElement, trackedEntityType, sqlParameterManager);

    // Then
    assertEquals(
        "select innermost_evt.* from (select *, row_number() over (partition by programinstanceuid order by occurreddate desc ) as rn from analytics_tei_events_uid2 where programuid = :1 and programstageuid = :2) innermost_evt where innermost_evt.rn = 1",
        statement);
  }

  @Test
  void testEventSelectPositiveOffset() {
    // Given
    int positiveOffset = 2;

    Program program = new Program();
    program.setUid("uid1");

    ProgramStage programStage = new ProgramStage();
    programStage.setProgram(program);

    ElementWithOffset<Program> programElement = ElementWithOffset.of(program, positiveOffset);
    ElementWithOffset<ProgramStage> programStageElement =
        ElementWithOffset.of(programStage, positiveOffset);

    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid("uid2");

    SqlParameterManager sqlParameterManager = new SqlParameterManager();

    // When
    String statement =
        SqlQueryHelper.eventSelect(
            programElement, programStageElement, trackedEntityType, sqlParameterManager);

    // Then
    assertEquals(
        "select innermost_evt.* from (select *, row_number() over (partition by programinstanceuid order by occurreddate desc ) as rn from analytics_tei_events_uid2 where programuid = :1 and programstageuid = :2) innermost_evt where innermost_evt.rn = 3",
        statement);
  }

  @Test
  void testEventSelectNegativeOffset() {
    // Given
    int positiveOffset = -3;

    Program program = new Program();
    program.setUid("uid1");

    ProgramStage programStage = new ProgramStage();
    programStage.setProgram(program);

    ElementWithOffset<Program> programElement = ElementWithOffset.of(program, positiveOffset);
    ElementWithOffset<ProgramStage> programStageElement =
        ElementWithOffset.of(programStage, positiveOffset);

    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid("uid2");

    SqlParameterManager sqlParameterManager = new SqlParameterManager();

    // When
    String statement =
        SqlQueryHelper.eventSelect(
            programElement, programStageElement, trackedEntityType, sqlParameterManager);

    // Then
    assertEquals(
        "select innermost_evt.* from (select *, row_number() over (partition by programinstanceuid order by occurreddate asc ) as rn from analytics_tei_events_uid2 where programuid = :1 and programstageuid = :2) innermost_evt where innermost_evt.rn = 3",
        statement);
  }
}
