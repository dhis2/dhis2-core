/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller.event;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Set;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageStatus;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonProgramMessage;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.message.ProgramMessageController;
import org.hisp.dhis.webapi.controller.tracker.TestSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link ProgramMessageController} using REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProgramMessageControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String TEXT = "Hi";
  private static final String SUBJECT = "subjectText";

  @Autowired private TestSetup testSetup;
  private Enrollment enrollment;

  private TrackerEvent trackerEvent;

  private SingleEvent singleEvent;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();
    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);
    testSetup.importTrackerData();
    enrollment = manager.get(Enrollment.class, "nxP7UnKhomJ");
    trackerEvent = manager.get(TrackerEvent.class, "pTzf9KYMk72");
    singleEvent = manager.get(SingleEvent.class, "QRYjLTiJTrA");
  }

  @Test
  void shouldGetProgramMessageWhenPassingEnrollmentParam() {
    ProgramMessage programMessage =
        createProgramMessage(
            TEXT,
            SUBJECT,
            new ProgramMessageRecipients(),
            ProgramMessageStatus.OUTBOUND,
            Set.of(DeliveryChannel.SMS));
    programMessage.setEnrollment(enrollment);
    manager.save(programMessage);
    JsonList<JsonProgramMessage> programMessages =
        GET("/messages?enrollment={id}", enrollment.getUid())
            .content(HttpStatus.OK)
            .asList(JsonProgramMessage.class);
    assertAll(
        () -> assertEquals(1, programMessages.size()),
        () -> assertEquals(enrollment.getUid(), programMessages.get(0).getEnrollment().getId()));
  }

  @Test
  void shouldGetProgramMessageWhenPassingEventParamForATrackerEvent() {
    ProgramMessage programMessage =
        createProgramMessage(
            TEXT,
            SUBJECT,
            new ProgramMessageRecipients(),
            ProgramMessageStatus.OUTBOUND,
            Set.of(DeliveryChannel.SMS));
    programMessage.setTrackerEvent(trackerEvent);
    manager.save(programMessage);
    JsonList<JsonProgramMessage> programMessages =
        GET("/messages?event={id}", trackerEvent.getUid())
            .content(HttpStatus.OK)
            .asList(JsonProgramMessage.class);
    assertAll(
        () -> assertEquals(1, programMessages.size()),
        () -> assertEquals(trackerEvent.getUid(), programMessages.get(0).getEvent().getId()));
  }

  @Test
  void shouldGetProgramMessageWhenPassingEventParamForASingleEvent() {
    ProgramMessage programMessage =
        createProgramMessage(
            TEXT,
            SUBJECT,
            new ProgramMessageRecipients(),
            ProgramMessageStatus.OUTBOUND,
            Set.of(DeliveryChannel.SMS));
    programMessage.setSingleEvent(singleEvent);
    manager.save(programMessage);
    manager.flush();
    manager.clear();
    JsonList<JsonProgramMessage> programMessages =
        GET("/messages?event={id}", singleEvent.getUid())
            .content(HttpStatus.OK)
            .asList(JsonProgramMessage.class);
    assertAll(
        () -> assertEquals(1, programMessages.size()),
        () -> assertEquals(singleEvent.getUid(), programMessages.get(0).getEvent().getId()));
  }

  @Test
  void shouldFailToGetProgramMessageWhenNoEventOrEnrollmentParamIsSpecified() {
    assertEquals(
        "Enrollment or Event must be specified.",
        GET("/messages").error(HttpStatus.CONFLICT).getMessage());
  }

  @Test
  void shouldScheduleProgramMessageWhenPassingEnrollmentParam() {
    assertTrue(
        GET("/messages/scheduled/sent?enrollment={id}", enrollment.getUid())
            .content(HttpStatus.OK)
            .isArray());
  }

  @Test
  void shouldScheduleProgramMessageWhenPassingEventParam() {
    assertTrue(
        GET("/messages/scheduled/sent?event={id}", trackerEvent.getUid())
            .content(HttpStatus.OK)
            .isArray());
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
