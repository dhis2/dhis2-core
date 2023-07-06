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
package org.hisp.dhis.tracker.events;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import joptsimple.internal.Strings;
import org.hamcrest.Matchers;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.RelationshipTypeActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.actions.tracker.importer.TrackerActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.hisp.dhis.tracker.importer.databuilder.RelationshipDataBuilder;
import org.hisp.dhis.tracker.importer.databuilder.TeiDataBuilder;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventExportTests extends TrackerApiTest {
  private final String withoutRegistrationProgram = Constants.EVENT_PROGRAM_ID;

  private final String withoutRegistrationProgramStage = Constants.EVENT_PROGRAM_STAGE_ID;

  private final String withRegistrationProgram = Constants.TRACKER_PROGRAM_ID;

  private final String withRegistrationProgramStage = "nlXNK4b7LVr";

  private final String userName =
      ("TA_EVENTS_ACL_USER" + DataGenerator.randomString()).toLowerCase();

  private final String password = Constants.USER_PASSWORD;

  private final String rootOu = "ImspTQPwCqd";

  private final String captureOu = "DiszpKrYNg8"; // level 4

  private final String searchOu = "O6uvpzGd5pu"; // level 2

  private final String dataReadOu = "YuQRtpLP10I"; // level 3

  private String closedProgramId;

  private String closedProgramProgramStageId;

  HashMap<String, String> events = new HashMap<>();

  HashMap<String, String> trackerEvents = new HashMap<>();

  HashMap<String, String> closedProgramEvents = new HashMap<>();

  String relationshipId;

  private UserActions userActions;

  @BeforeAll
  public void beforeAll() {
    userActions = new UserActions();
    eventActions = new EventActions();

    loginActions.loginAsSuperUser();

    setupUser();
    closedProgramId =
        programActions.createProgramWithAccessLevel(
            "CLOSED", rootOu, captureOu, searchOu, dataReadOu);
    closedProgramProgramStageId =
        programActions.createProgramStage(
            closedProgramId, "Event export tests" + DataGenerator.randomString());
    setupTrackerEvents();
    setupEvents();

    String relationshipTypeId =
        new RelationshipTypeActions()
            .createRelationshipType(
                "PROGRAM_STAGE_INSTANCE",
                withoutRegistrationProgramStage,
                "PROGRAM_STAGE_INSTANCE",
                withRegistrationProgram,
                true);

    relationshipId =
        createRelationship(events.get(captureOu), trackerEvents.get(captureOu), relationshipTypeId);
  }

  @BeforeEach
  public void beforeEach() {
    loginActions.loginAsAdmin();
  }

  Stream<Arguments> shouldUseCorrectScopeWhenOuIsProvided() {
    return Stream.of(
        Arguments.of("OU: root", "SELECTED", rootOu, false, null),
        Arguments.of("OU: capture", "SELECTED", captureOu, true, Arrays.asList(captureOu)),
        Arguments.of("OU: search", "SELECTED", searchOu, true, Arrays.asList(searchOu)),
        Arguments.of("OU: data read", "SELECTED", dataReadOu, true, Arrays.asList(dataReadOu)),
        Arguments.of(
            "OU: data read ( DESCENDANTS ) ",
            "DESCENDANTS",
            captureOu,
            true,
            Arrays.asList(captureOu)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  public void shouldUseCorrectScopeWhenOuIsProvided(
      String description,
      String mode,
      String orgUnitId,
      Boolean shouldReturn,
      List<String> orgUnit) {
    loginActions.loginAsUser(userName, password);

    QueryParamsBuilder builder =
        new QueryParamsBuilder().add("ouMode", mode).add("orgUnit", orgUnitId);

    ApiResponse response = eventActions.get(builder.build());

    if (shouldReturn) {
      response
          .validate()
          .statusCode(200)
          .body("events", hasSize(greaterThanOrEqualTo(1)))
          .body("events.orgUnit", everyItem(in(orgUnit)));

      return;
    }

    response.validateStatus(409);
  }

  Stream<Arguments> shouldUseCorrectScopeWhenNoOu() {
    return Stream.of(
        Arguments.of(
            "should use capture scope when no ou, no program", null, Arrays.asList(captureOu)),
        Arguments.of(
            "should use capture scope when no ou, closed program",
            closedProgramId,
            Arrays.asList(captureOu)),
        Arguments.of(
            "should use search scope when no ou, open program",
            withRegistrationProgram,
            Arrays.asList(captureOu, searchOu, dataReadOu)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  public void shouldUseCorrectScopeWhenNoOu(
      String description, String program, List<String> expectedOrgUnits) {
    loginActions.loginAsUser(userName, password);

    QueryParamsBuilder builder = new QueryParamsBuilder();
    if (program != null) {
      builder.add("program", program);
    }

    eventActions
        .get(builder.build())
        .validate()
        .statusCode(200)
        .body("events.orgUnit", everyItem(in(expectedOrgUnits)));
  }

  private Stream<Arguments> shouldReturnSpecificEvents() {
    List<String> allEvents = new ArrayList<>();
    allEvents.addAll(events.values());
    allEvents.addAll(trackerEvents.values());
    return Stream.of(
        Arguments.of(
            "Event program events",
            new ArrayList<>(events.values()),
            Arrays.asList(captureOu, searchOu, dataReadOu)),
        Arguments.of(
            "All programs events", allEvents, Arrays.asList(captureOu, searchOu, dataReadOu)),
        Arguments.of(
            "Tracker program events",
            new ArrayList<>(trackerEvents.values()),
            Arrays.asList(captureOu, searchOu, dataReadOu)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  public void shouldReturnSpecificEvents(
      String description, List<String> events, List<String> orgUnits) {
    loginActions.loginAsUser(userName, password);

    eventActions
        .get(String.format("?event=%s", Strings.join(events, ";")))
        .validate()
        .statusCode(200)
        .body("events", hasSize(greaterThanOrEqualTo(1)))
        .body("events.orgUnit", everyItem(in(orgUnits)));
  }

  private Stream<Arguments> shouldUseCorrectScopeWhenOuModeIsProvided() {
    return Stream.of(
        new Arguments[] {
          Arguments.of(
              "PROGRAM: tracker, OU_MODE: ACCESSIBLE, EXPECTED: search scope",
              "ACCESSIBLE",
              withRegistrationProgram,
              Arrays.asList(captureOu, searchOu, dataReadOu)),
          Arguments.of(
              "PROGRAM: event, OU_MODE: ACCESSIBLE, EXPECTED: search scope,",
              "ACCESSIBLE",
              withoutRegistrationProgram,
              Arrays.asList(searchOu, dataReadOu, captureOu)),
          Arguments.of(
              "PROGRAM: none, OU_MODE: ACCESSIBLE, EXPECTED: search scope",
              "ACCESSIBLE",
              null,
              Arrays.asList(dataReadOu, captureOu, searchOu)),
          Arguments.of(
              "PROGRAM: closed tracker, OU_MODE: ACCESSIBLE, EXPECTED: capture scope",
              "ACCESSIBLE",
              closedProgramId,
              Arrays.asList(captureOu)),
          Arguments.of("PROGRAM: none, OU_MODE: CAPTURE", "CAPTURE", null, Arrays.asList(captureOu))
        });
  }

  @ParameterizedTest
  @MethodSource
  public void shouldUseCorrectScopeWhenOuModeIsProvided(
      String description, String ouMode, String programId, List<String> expectedOrgUnits) {
    loginActions.loginAsUser(userName, password);

    QueryParamsBuilder builder = new QueryParamsBuilder().add("ouMode", ouMode);
    if (!Strings.isNullOrEmpty(programId)) {
      builder.add("program", programId);
    }

    eventActions
        .get(builder.build())
        .validate()
        .statusCode(200)
        .body("events", hasSize(greaterThanOrEqualTo(1)))
        .body("events.orgUnit", everyItem(in(expectedOrgUnits)));
  }

  private Stream<Arguments> shouldReturnSingleEvent() {
    return Stream.of(
        new Arguments[] {
          Arguments.of(
              "PROGRAM1: event, OU: search, shouldReturn: true", events.get(searchOu), true),
          Arguments.of(
              "PROGRAM2: tracker, OU: search, shouldReturn: true",
              trackerEvents.get(searchOu),
              true),
          Arguments.of(
              "PROGRAM3: event, OU: dataRead, shouldReturn: true", events.get(dataReadOu), true),
          Arguments.of("PROGRAM4: event, OU: root, shouldReturn: false", events.get(rootOu), false),
          Arguments.of(
              "PROGRAM5: tracker, OU: root, shouldReturn: false", trackerEvents.get(rootOu), false),
          Arguments.of(
              "PROGRAM: tracker, OU: dataRead, shouldReturn: true ",
              trackerEvents.get(dataReadOu),
              true),
          Arguments.of(
              "PROGRAM: closed tracker, OU: search, shouldReturn: false",
              closedProgramEvents.get(searchOu),
              false),
          Arguments.of(
              "PROGRAM6: closed tracker, OU: dataRead, shouldReturn: false",
              closedProgramEvents.get(dataReadOu),
              false),
          Arguments.of(
              "PROGRAM7: closed tracker, OU: capture, shouldReturn: true",
              closedProgramEvents.get(captureOu),
              true)
        });
  }

  @ParameterizedTest(name = "[{0}]")
  @MethodSource
  public void shouldReturnSingleEvent(String description, String eventId, Boolean shouldGet) {
    loginActions.loginAsUser(userName, password);

    ApiResponse response = eventActions.get(eventId);

    if (shouldGet) {
      response.validate().statusCode(200);

      return;
    }

    response.validate().statusCode(409);
  }

  @ValueSource(
      strings = {
        "?program=programId&event=eventId&fields=*",
        "?program=programId&event=eventId&fields=relationships",
        "?event=eventId&fields=*",
        "?event=eventId&fields=relationships",
        "?program=programId&fields=*",
        "?program=programId&fields=relationships"
      })
  @ParameterizedTest
  public void shouldFetchRelationships(String queryParams) {

    ApiResponse response =
        eventActions
            .get(
                queryParams
                    .replace("eventId", events.get(captureOu))
                    .replace("programId", withoutRegistrationProgram))
            .validateStatus(200);
    String body = "relationships";

    if (response.extractList("events") != null) {
      body = "events.relationships";
    }

    response
        .validate()
        .body(body, hasSize(Matchers.greaterThanOrEqualTo(1)))
        .body(body + ".relationship", hasItems(hasItems(containsString(relationshipId))));
  }

  @ValueSource(
      strings = {
        "?event=eventId",
        "?event=eventId&fields=*,!relationships",
        "?program=programId&fields=*,!relationships"
      })
  @ParameterizedTest
  public void shouldSkipRelationshipsForEventId(String queryParams) {
    ApiResponse response =
        eventActions.get(
            queryParams
                .replace("eventId", events.get(captureOu))
                .replace("programId", withoutRegistrationProgram));
    String body = "relationships";

    if (response.extractList("events") != null) {
      body = "events[0].relationships";
    }

    response.validate().body(body, anyOf(nullValue(), hasSize(0)));
  }

  @Test
  public void shouldNotHaveAccessToAllOuMode() {
    loginActions.loginAsUser(userName, password);

    eventActions
        .get(
            String.format(
                "?program=%s&ouMode=%s&orgUnit", withoutRegistrationProgram, "ALL", "ImspTQPwCqd"))
        .validate()
        .statusCode(409)
        .body(
            "message",
            equalTo("Current user is not authorized to query across all organisation units"));
  }

  private String createEvent(String ou) {
    JsonObject payload =
        eventActions.createEventBody(
            ou, withoutRegistrationProgram, withoutRegistrationProgramStage);

    return eventActions
        .post(payload, new QueryParamsBuilder().add("skipCache=true"))
        .validateStatus(200)
        .extractUid();
  }

  private String setupUser() {
    String userId = userActions.addUserFull("firstNameA", "lastNameB", userName, password, "NONE");

    userActions.grantUserAccessToOrgUnits(userId, captureOu, searchOu, dataReadOu);
    userActions.addUserToUserGroup(userId, Constants.USER_GROUP_ID);

    return userId;
  }

  private void setupEvents() {
    Arrays.asList(captureOu, dataReadOu, searchOu, rootOu)
        .forEach(
            ou -> {
              String event = createEvent(ou);
              events.put(ou, event);
            });
  }

  private void setupTrackerEvents() {
    Arrays.asList(captureOu, dataReadOu, searchOu, rootOu)
        .forEach(
            ou -> {
              JsonObject object =
                  new TeiDataBuilder()
                      .buildWithEnrollmentAndEvent(
                          Constants.TRACKED_ENTITY_TYPE,
                          ou,
                          withRegistrationProgram,
                          withRegistrationProgramStage);
              String eventId =
                  new TrackerActions()
                      .postAndGetJobReport(object)
                      .validateSuccessfulImport()
                      .extractImportedEvents()
                      .get(0);

              trackerEvents.put(ou, eventId);

              // closed program events

              object =
                  new TeiDataBuilder()
                      .buildWithEnrollmentAndEvent(
                          Constants.TRACKED_ENTITY_TYPE,
                          ou,
                          closedProgramId,
                          closedProgramProgramStageId);
              eventId =
                  new TrackerActions()
                      .postAndGetJobReport(object, new QueryParamsBuilder().add("async", "false"))
                      .validateSuccessfulImport()
                      .extractImportedEvents()
                      .get(0);

              closedProgramEvents.put(ou, eventId);
            });
  }

  private String createRelationship(String eventId, String event2Id, String relationshipTypeId) {
    JsonObject relationships =
        new RelationshipDataBuilder()
            .setToEntity("event", eventId)
            .setFromEntity("event", event2Id)
            .setRelationshipType(relationshipTypeId)
            .array();

    return new TrackerActions()
        .postAndGetJobReport(relationships)
        .validateSuccessfulImport()
        .extractImportedRelationships()
        .get(0);
  }
}
