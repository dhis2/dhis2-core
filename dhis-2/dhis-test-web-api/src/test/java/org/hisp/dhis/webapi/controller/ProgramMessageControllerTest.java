/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.webapi.controller.message.ProgramMessageController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link ProgramMessageController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
class ProgramMessageControllerTest extends H2ControllerIntegrationTestBase {

  private Enrollment enrollmentA;

  private Event eventA;

  @BeforeEach
  void setUp() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    manager.save(ouA);
    Program prA = createProgram('A', Sets.newHashSet(), ouA);
    manager.save(prA);
    ProgramStage psA = createProgramStage('A', prA);
    manager.save(psA);
    TrackedEntity trackedEntityA = createTrackedEntity('A', ouA);
    manager.save(trackedEntityA);
    enrollmentA = createEnrollment(prA, trackedEntityA, ouA);
    manager.save(enrollmentA);
    eventA = createEvent(psA, enrollmentA, ouA);
    manager.save(eventA);
  }

  @Test
  void shouldGetProgramMessageWhenPassingDeprecatedProgramInstanceParam() {
    assertTrue(
        GET("/messages?programInstance={id}", enrollmentA.getUid())
            .content(HttpStatus.OK)
            .isArray());
  }

  @Test
  void shouldGetProgramMessageWhenPassingEnrollmentParam() {
    assertTrue(
        GET("/messages?enrollment={id}", enrollmentA.getUid()).content(HttpStatus.OK).isArray());
  }

  @Test
  void shouldFailToGetProgramMessageWhenPassingEnrollmentAndProgramInstanceParams() {
    assertEquals(
        "Only one parameter of 'programInstance' and 'enrollment' must be specified. Prefer 'enrollment' as 'programInstance' will be removed.",
        GET(
                "/messages?enrollment={id}&programInstance={id}",
                enrollmentA.getUid(),
                enrollmentA.getUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldGetProgramMessageWhenPassingDeprecatedProgramStageInstanceParam() {
    assertTrue(
        GET("/messages?programStageInstance={id}", eventA.getUid())
            .content(HttpStatus.OK)
            .isArray());
  }

  @Test
  void shouldGetProgramMessageWhenPassingEventParam() {
    assertTrue(GET("/messages?event={id}", eventA.getUid()).content(HttpStatus.OK).isArray());
  }

  @Test
  void shouldFailToGetProgramMessageWhenPassingEventAndProgramStageInstanceParams() {
    assertEquals(
        "Only one parameter of 'programStageInstance' and 'event' must be specified. Prefer 'event' as 'programStageInstance' will be removed.",
        GET("/messages?event={id}&programStageInstance={id}", eventA.getUid(), eventA.getUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldFailToGetProgramMessageWhenNoEventOrEnrollmentParamIsSpecified() {
    assertEquals(
        "Enrollment or Event must be specified.",
        GET("/messages").error(HttpStatus.CONFLICT).getMessage());
  }

  @Test
  void shouldScheduleProgramMessageWhenPassingDeprecatedProgramInstanceParam() {
    assertTrue(
        GET("/messages/scheduled/sent?programInstance={id}", enrollmentA.getUid())
            .content(HttpStatus.OK)
            .isArray());
  }

  @Test
  void shouldScheduleProgramMessageWhenPassingEnrollmentParam() {
    assertTrue(
        GET("/messages/scheduled/sent?enrollment={id}", enrollmentA.getUid())
            .content(HttpStatus.OK)
            .isArray());
  }

  @Test
  void shouldFailToScheduleProgramMessageWhenPassingEnrollmentAndProgramInstanceParams() {
    assertEquals(
        "Only one parameter of 'programInstance' and 'enrollment' must be specified. Prefer 'enrollment' as 'programInstance' will be removed.",
        GET(
                "/messages/scheduled/sent?enrollment={id}&programInstance={id}",
                enrollmentA.getUid(),
                enrollmentA.getUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldScheduleProgramMessageWhenPassingDeprecatedProgramStageInstanceParam() {
    assertTrue(
        GET("/messages/scheduled/sent?programStageInstance={id}", eventA.getUid())
            .content(HttpStatus.OK)
            .isArray());
  }

  @Test
  void shouldScheduleProgramMessageWhenPassingEventParam() {
    assertTrue(
        GET("/messages/scheduled/sent?event={id}", eventA.getUid())
            .content(HttpStatus.OK)
            .isArray());
  }

  @Test
  void shouldFailToScheduleProgramMessageWhenPassingEventAndProgramStageInstanceParams() {
    assertEquals(
        "Only one parameter of 'programStageInstance' and 'event' must be specified. Prefer 'event' as 'programStageInstance' will be removed.",
        GET(
                "/messages/scheduled/sent?event={id}&programStageInstance={id}",
                eventA.getUid(),
                eventA.getUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void testSaveMessages() {
    JsonObject status = POST("/messages", "{'programMessages': []}").content(HttpStatus.OK);
    assertTrue(status.isObject());
    assertEquals(1, status.size());
    JsonArray summaries = status.getArray("summaries");
    assertTrue(summaries.isArray());
    assertTrue(summaries.isEmpty());
  }
}
