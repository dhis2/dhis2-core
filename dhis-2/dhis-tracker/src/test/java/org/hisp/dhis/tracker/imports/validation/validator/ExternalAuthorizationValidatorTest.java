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
package org.hisp.dhis.tracker.imports.validation.validator;

import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1400;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1401;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExternalAuthorizationValidatorTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final List<FakeJudge> judges = new ArrayList<>();

  private IdentifiableObjectManager manager;
  private Attribute attribute;
  private TrackerPreheat preheat;
  private TrackerBundle bundle;
  private Reporter reporter;
  private ExternalAuthorizationValidator validator;
  private User user;

  @BeforeEach
  void setUp() {
    manager = mock(IdentifiableObjectManager.class);
    attribute = new Attribute();
    attribute.setUid("AttrExtAuth1");
    attribute.setCode(ExternalAuthorizationValidator.ATTRIBUTE_CODE);
    org.mockito.Mockito.lenient()
        .when(manager.getAllNoAcl(Attribute.class))
        .thenReturn(List.of(otherAttribute(), attribute));

    user = new User();
    user.setUid("UserUid0001");
    user.setUsername("jdoe");
    UserGroup group = new UserGroup();
    group.setUid("GroupClear1");
    user.getGroups().add(group);

    preheat = new TrackerPreheat();
    bundle = TrackerBundle.builder().user(UserDetails.fromUser(user)).build();
    bundle.setPreheat(preheat);
    reporter = new Reporter(TrackerIdSchemeParams.builder().build());
    validator = new ExternalAuthorizationValidator(manager, JSON, Duration.ofSeconds(2));
  }

  @AfterEach
  void tearDown() {
    judges.forEach(j -> j.server.stop(0));
  }

  @Test
  void noAttributeConfiguredMakesNoCall() throws IOException {
    when(manager.getAllNoAcl(Attribute.class)).thenReturn(List.of(otherAttribute()));
    FakeJudge judge = judge(200, "");
    ProgramStage stage = stage("StageAAAAA1", program("ProgramAAA1", judge.url()));
    addEvent(event(stage), TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, bundle);

    assertIsEmpty(judge.requests);
    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void allowedWhenJudgeReturns200() throws IOException {
    FakeJudge judge = judge(200, "");
    ProgramStage stage = stage("StageAAAAA1", program("ProgramAAA1", judge.url()));
    addEvent(event(stage), TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, bundle);

    assertEquals(1, judge.requests.size());
    JsonNode body = judge.requests.get(0);
    assertEquals("CREATE", body.at("/events/0/operation").asText());
    assertEquals("UserUid0001", body.at("/user/uid").asText());
    assertEquals("StageAAAAA1", body.at("/events/0/event/programStage").asText());
    assertTrue(body.at("/events/0/existing").isMissingNode());
    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void sendsDataValuesAndUserGroupsForTheJudgeToDecideOn() throws IOException {
    FakeJudge judge = judge(200, "");
    ProgramStage stage = stage("StageAAAAA1", program("ProgramAAA1", judge.url()));
    TrackerEvent ev = (TrackerEvent) event(stage);
    ev.getDataValues()
        .add(
            DataValue.builder()
                .dataElement(MetadataIdentifier.ofUid("DataElemAA1"))
                .value("HIGH")
                .build());
    addEvent(ev, TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, bundle);

    JsonNode body = judge.requests.get(0);
    assertEquals("HIGH", body.at("/events/0/event/dataValues/DataElemAA1").asText());
    assertEquals("GroupClear1", body.at("/user/userGroups/0").asText());
  }

  @Test
  void rejectedEventsGetE1400() throws IOException {
    FakeJudge judge = judge(409, "");
    ProgramStage stage = stage("StageAAAAA1", program("ProgramAAA1", judge.url()));
    Event ev1 = event(stage);
    Event ev2 = event(stage);
    addEvent(ev1, TrackerImportStrategy.CREATE);
    addEvent(ev2, TrackerImportStrategy.CREATE);
    judge.body =
        """
        {"rejected":[{"uid":"%s","reason":"Clearance too low"},{"uid":"unknownUid01","reason":"x"}]}
        """
            .formatted(ev1.getUID().getValue());

    validator.validate(reporter, bundle, bundle);

    assertEquals(1, reporter.getErrors().size());
    Validation error = reporter.getErrors().get(0);
    assertEquals(E1400.name(), error.getCode());
    assertEquals(ev1.getUID(), error.getUid());
    assertTrue(error.getMessage().contains("Clearance too low"));
  }

  @Test
  void judgeFailureRejectsAllWithE1401() throws IOException {
    FakeJudge judge = judge(500, "boom");
    ProgramStage stage = stage("StageAAAAA1", program("ProgramAAA1", judge.url()));
    addEvent(event(stage), TrackerImportStrategy.CREATE);
    addEvent(event(stage), TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, bundle);

    assertEquals(2, reporter.getErrors().size());
    reporter.getErrors().forEach(e -> assertEquals(E1401.name(), e.getCode()));
  }

  @Test
  void unparsable409RejectsAllWithE1401() throws IOException {
    FakeJudge judge = judge(409, "not json");
    ProgramStage stage = stage("StageAAAAA1", program("ProgramAAA1", judge.url()));
    addEvent(event(stage), TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, bundle);

    assertEquals(1, reporter.getErrors().size());
    assertEquals(E1401.name(), reporter.getErrors().get(0).getCode());
  }

  @Test
  void unreachableJudgeRejectsWithE1401() throws IOException {
    FakeJudge judge = judge(200, "");
    judge.server.stop(0);
    ProgramStage stage = stage("StageAAAAA1", program("ProgramAAA1", judge.url()));
    addEvent(event(stage), TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, bundle);

    assertEquals(1, reporter.getErrors().size());
    assertEquals(E1401.name(), reporter.getErrors().get(0).getCode());
  }

  @Test
  void deleteSendsExistingFromStoredEvent() throws IOException {
    FakeJudge judge = judge(200, "");
    ProgramStage stage = stage("StageAAAAA1", program("ProgramAAA1", judge.url()));
    UID uid = UID.generate();
    org.hisp.dhis.tracker.model.TrackerEvent stored =
        new org.hisp.dhis.tracker.model.TrackerEvent();
    stored.setUid(uid.getValue());
    stored.setProgramStage(stage);
    preheat.putTrackerEvent(stored);
    addEvent(TrackerEvent.builder().event(uid).build(), TrackerImportStrategy.DELETE);

    validator.validate(reporter, bundle, bundle);

    assertEquals(1, judge.requests.size());
    JsonNode body = judge.requests.get(0);
    assertEquals("DELETE", body.at("/events/0/operation").asText());
    assertTrue(body.at("/events/0/event").isMissingNode());
    assertEquals("StageAAAAA1", body.at("/events/0/existing/programStage").asText());
    assertEquals("ProgramAAA1", body.at("/events/0/existing/program").asText());
  }

  @Test
  void updateSendsBothNewAndExisting() throws IOException {
    FakeJudge judge = judge(200, "");
    ProgramStage stage = stage("StageAAAAA1", program("ProgramAAA1", judge.url()));
    Event ev = event(stage);
    org.hisp.dhis.tracker.model.TrackerEvent stored =
        new org.hisp.dhis.tracker.model.TrackerEvent();
    stored.setUid(ev.getUID().getValue());
    stored.setProgramStage(stage);
    preheat.putTrackerEvent(stored);
    addEvent(ev, TrackerImportStrategy.UPDATE);

    validator.validate(reporter, bundle, bundle);

    JsonNode body = judge.requests.get(0);
    assertEquals("UPDATE", body.at("/events/0/operation").asText());
    assertFalse(body.at("/events/0/event").isMissingNode());
    assertEquals(ev.getUID().getValue(), body.at("/events/0/existing/event").asText());
  }

  @Test
  void oneCallPerUrl() throws IOException {
    FakeJudge judgeA = judge(200, "");
    FakeJudge judgeB = judge(200, "");
    ProgramStage stageA = stage("StageAAAAA1", program("ProgramAAA1", judgeA.url()));
    ProgramStage stageB = stage("StageBBBBB1", program("ProgramBBB1", judgeB.url()));
    Event evA = event(stageA);
    Event evB = event(stageB);
    addEvent(evA, TrackerImportStrategy.CREATE);
    addEvent(evB, TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, bundle);

    assertEquals(1, judgeA.requests.size());
    assertEquals(1, judgeB.requests.size());
    assertEquals(1, judgeA.requests.get(0).get("events").size());
    assertEquals(
        evB.getUID().getValue(), judgeB.requests.get(0).at("/events/0/event/event").asText());
  }

  @Test
  void attributeLookupIgnoresSharingOfTheImportingUser() throws IOException {
    // the importing user cannot read the attribute: a sharing-aware lookup returns null
    org.mockito.Mockito.lenient()
        .when(manager.getByCode(Attribute.class, ExternalAuthorizationValidator.ATTRIBUTE_CODE))
        .thenReturn(null);
    FakeJudge judge = judge(200, "");
    ProgramStage stage = stage("StageAAAAA1", program("ProgramAAA1", judge.url()));
    addEvent(event(stage), TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, bundle);

    assertEquals(1, judge.requests.size());
  }

  @Test
  void deleteNamingAnotherProgramIsJudgedByTheStoredEventsProgram() throws IOException {
    FakeJudge judge = judge(200, "");
    ProgramStage guarded = stage("StageAAAAA1", program("ProgramAAA1", judge.url()));
    ProgramStage unguarded = stage("StageBBBBB1", program("ProgramBBB1", ""));
    UID uid = UID.generate();
    org.hisp.dhis.tracker.model.TrackerEvent stored =
        new org.hisp.dhis.tracker.model.TrackerEvent();
    stored.setUid(uid.getValue());
    stored.setProgramStage(guarded);
    preheat.putTrackerEvent(stored);
    addEvent(
        TrackerEvent.builder()
            .event(uid)
            .program(MetadataIdentifier.ofUid(unguarded.getProgram().getUid()))
            .programStage(MetadataIdentifier.ofUid(unguarded.getUid()))
            .build(),
        TrackerImportStrategy.DELETE);

    validator.validate(reporter, bundle, bundle);

    assertEquals(1, judge.requests.size());
    assertEquals("ProgramAAA1", judge.requests.get(0).at("/events/0/existing/program").asText());
  }

  @Test
  void conflictThatRejectsNoEventOfTheBatchFailsClosed() throws IOException {
    FakeJudge judge = judge(409, "");
    ProgramStage stage = stage("StageAAAAA1", program("ProgramAAA1", judge.url()));
    Event ev = event(stage);
    addEvent(ev, TrackerImportStrategy.CREATE);
    // judge used "event" instead of "uid" as key: nothing in the batch matches
    judge.body =
        """
        {"rejected":[{"event":"%s","reason":"no"}]}
        """
            .formatted(ev.getUID().getValue());

    validator.validate(reporter, bundle, bundle);

    assertEquals(1, reporter.getErrors().size());
    assertEquals(E1401.name(), reporter.getErrors().get(0).getCode());
  }

  @Test
  void conflictWithEmptyRejectedListFailsClosed() throws IOException {
    FakeJudge judge = judge(409, "{\"rejected\":[]}");
    ProgramStage stage = stage("StageAAAAA1", program("ProgramAAA1", judge.url()));
    addEvent(event(stage), TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, bundle);

    assertEquals(1, reporter.getErrors().size());
    assertEquals(E1401.name(), reporter.getErrors().get(0).getCode());
  }

  private static Attribute otherAttribute() {
    Attribute other = new Attribute();
    other.setUid("AttrOther01");
    other.setCode("SOMETHING_ELSE");
    return other;
  }

  private Program program(String uid, String url) {
    Program program = new Program();
    program.setUid(uid);
    program.setAttributeValues(AttributeValues.of(Map.of(attribute.getUid(), url)));
    preheat.put(program);
    return program;
  }

  private ProgramStage stage(String uid, Program program) {
    ProgramStage stage = new ProgramStage();
    stage.setUid(uid);
    stage.setProgram(program);
    preheat.put(stage);
    return stage;
  }

  private Event event(ProgramStage stage) {
    return TrackerEvent.builder()
        .event(UID.generate())
        .program(MetadataIdentifier.ofUid(stage.getProgram().getUid()))
        .programStage(MetadataIdentifier.ofUid(stage.getUid()))
        .orgUnit(MetadataIdentifier.ofUid("OrgUnitAAA1"))
        .build();
  }

  private void addEvent(Event event, TrackerImportStrategy strategy) {
    List<Event> events = new ArrayList<>(bundle.getEvents());
    events.add(event);
    bundle.setEvents(events);
    bundle.setStrategy(event, strategy);
  }

  private FakeJudge judge(int status, String body) throws IOException {
    FakeJudge judge = new FakeJudge(status, body);
    judges.add(judge);
    return judge;
  }

  private static class FakeJudge {
    final HttpServer server;
    final List<JsonNode> requests = new ArrayList<>();
    volatile int status;
    volatile String body;

    FakeJudge(int status, String body) throws IOException {
      this.status = status;
      this.body = body;
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            requests.add(JSON.readTree(exchange.getRequestBody()));
            byte[] out = this.body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(this.status, out.length == 0 ? -1 : out.length);
            if (out.length > 0) {
              exchange.getResponseBody().write(out);
            }
            exchange.close();
          });
      server.start();
    }

    String url() {
      return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }
  }
}
