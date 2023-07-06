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
package org.hisp.dhis.tracker.importer.relationships;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hisp.dhis.helpers.matchers.MatchesJson.matchesJSON;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.hisp.dhis.actions.IdGenerator;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.actions.metadata.RelationshipTypeActions;
import org.hisp.dhis.actions.tracker.RelationshipActions;
import org.hisp.dhis.actions.tracker.TEIActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.TestCleanUp;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.hisp.dhis.tracker.importer.databuilder.RelationshipDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class RelationshipsTests extends TrackerNtiApiTest {
  private static final String relationshipType = "TV9oB9LT3sh";

  private static List<String> teis;

  private static List<String> enrollments;

  private static List<String> events;

  private List<String> createdRelationships = new ArrayList<>();

  private RelationshipTypeActions relationshipTypeActions;

  private TEIActions teiActions;

  private RelationshipActions relationshipActions;

  private MetadataActions metadataActions;

  private static Stream<Arguments> provideRelationshipData() {
    return Stream.of(
        /*
         * Arguments.of( "WmNgnmedbQj", "trackedEntity", teis.get( 0 ),
         * "enrollment", enrollments.get( 1 )), // tei to enrollment todo:
         * uncomment when DHIS2-12625 is fixed
         */
        Arguments.of("HrS7b5Lis6E", "event", events.get(0), "trackedEntity", teis.get(0)), // event
        // to
        // tei
        Arguments.of("HrS7b5Lis6w", "trackedEntity", teis.get(0), "event", events.get(0)), // tei
        // to
        // event
        Arguments.of("HrS7b5Lis6P", "event", events.get(0), "event", events.get(1)), // event
        // to
        // event
        Arguments.of(
            relationshipType,
            "trackedEntity",
            teis.get(0),
            "trackedEntity",
            teis.get(1))); // tei to tei
  }

  private static Stream<Arguments> provideDuplicateRelationshipData() {
    return Stream.of(
        Arguments.of(
            teis.get(0),
            teis.get(1),
            teis.get(1),
            teis.get(0),
            true,
            1,
            "bi: reversed direction should import 1"),
        Arguments.of(
            teis.get(0),
            teis.get(1),
            teis.get(0),
            teis.get(1),
            false,
            1,
            "uni: same direction should import 1"),
        Arguments.of(
            teis.get(0),
            teis.get(1),
            teis.get(0),
            teis.get(1),
            true,
            1,
            "bi: same direction should import 1"),
        Arguments.of(
            teis.get(0),
            teis.get(1),
            teis.get(1),
            teis.get(0),
            false,
            2,
            "uni: reversed direction should import 2"));
  }

  @BeforeAll
  public void beforeAll() throws Exception {
    teiActions = new TEIActions();
    metadataActions = new MetadataActions();
    relationshipTypeActions = new RelationshipTypeActions();
    relationshipActions = new RelationshipActions();

    loginActions.loginAsSuperUser();

    metadataActions.importAndValidateMetadata(
        new File("src/test/resources/tracker/relationshipTypes.json"));

    TrackerApiResponse importResponse =
        importTeisWithEnrollmentAndEvent().validateSuccessfulImport();
    teis = importResponse.extractImportedTeis();
    enrollments = importResponse.extractImportedEnrollments();
    events = importEvents();
  }

  @Test
  public void shouldNotUpdateRelationship() {
    // arrange
    String relationshipId = new IdGenerator().generateUniqueId();

    JsonObject originalRelationship =
        new RelationshipDataBuilder()
            .setRelationshipId(relationshipId)
            .setRelationshipType(relationshipType)
            .setToEntity("trackedEntity", teis.get(1))
            .setFromEntity("trackedEntity", teis.get(0))
            .array();

    trackerActions.postAndGetJobReport(originalRelationship).validateSuccessfulImport();

    JsonObject updatedRelationship =
        trackerActions.getRelationship(relationshipId).validateStatus(200).getBody();

    updatedRelationship =
        JsonObjectBuilder.jsonObject(updatedRelationship)
            .addObjectByJsonPath(
                "relationships[0]", "from", relationshipItem("trackedEntity", teis.get(0)).build())
            .addObjectByJsonPath(
                "relationships[0]", "to", relationshipItem("trackedEntity", teis.get(1)).build())
            .wrapIntoArray("relationships");

    // act
    trackerActions
        .postAndGetJobReport(
            updatedRelationship, new QueryParamsBuilder().addAll("importStrategy=UPDATE"))
        .validateWarningReport()
        .body("warningCode", hasItems("E4015"))
        .body("message", hasItem(containsString("already exists")));

    trackerActions
        .getRelationship(relationshipId)
        .validate()
        .body("", matchesJSON(originalRelationship.getAsJsonArray("relationships").get(0)));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "src/test/resources/tracker/importer/teis/teisAndRelationship.json",
        "src/test/resources/tracker/importer/teis/teisWithRelationship.json"
      })
  public void shouldImportObjectsWithRelationship(String file) throws Exception {
    JsonObject jsonObject = new FileReaderUtils().read(new File(file)).get(JsonObject.class);

    TrackerApiResponse response =
        trackerActions.postAndGetJobReport(jsonObject).validateSuccessfulImport();

    response.validate().body("stats.total", equalTo(3));

    createdRelationships = response.extractImportedRelationships();

    ApiResponse relationshipResponse =
        trackerActions.get("/relationships/" + createdRelationships.get(0));

    relationshipResponse
        .validate()
        .statusCode(200)
        .body("from.trackedEntity.trackedEntity", notNullValue())
        .body("to.trackedEntity.trackedEntity", notNullValue());

    response
        .extractImportedTeis()
        .forEach(
            tei -> {
              teiActions
                  .get(tei, new QueryParamsBuilder().add("fields=relationships"))
                  .validate()
                  .statusCode(200)
                  .body("relationships.relationship", contains(createdRelationships.get(0)));
            });
  }

  @Test
  public void shouldNotDuplicateNonBidirectionalRelationship() throws Exception {
    // Given 2 existing tracked entities and a unidirectional relationship
    // between them

    String trackedEntity_1 = importTei();
    String trackedEntity_2 = importTei();

    JsonObject jsonObject =
        new RelationshipDataBuilder()
            .buildUniDirectionalRelationship(trackedEntity_1, trackedEntity_2)
            .array();

    trackerActions
        .postAndGetJobReport(jsonObject)
        .validateSuccessfulImport()
        .validateRelationships()
        .body("stats.created", equalTo(1));

    // when posting the same payload, then relationship is ignored when in
    // the same way
    trackerActions
        .postAndGetJobReport(jsonObject)
        .validateErrorReport()
        .body("errorCode", contains("E4018"));

    // and is imported again when the relation is in inverse order
    jsonObject =
        new RelationshipDataBuilder()
            .buildUniDirectionalRelationship(trackedEntity_2, trackedEntity_1)
            .array();
    trackerActions
        .postAndGetJobReport(jsonObject)
        .validateSuccessfulImport()
        .validateRelationships()
        .body("stats.ignored", equalTo(0))
        .body("stats.created", equalTo(1));

    // and there are 2 relationships for any of the tracked entities
    ApiResponse relationshipResponse = trackerActions.get("/relationships?tei=" + trackedEntity_1);

    relationshipResponse.validate().statusCode(200).body("instances.size()", is(2));
  }

  @Test
  public void shouldNotDuplicateBidirectionalRelationship() throws Exception {
    // Given 2 existing tracked entities and a bidirectional relationship
    // between them

    String trackedEntity_1 = importTei();
    String trackedEntity_2 = importTei();

    JsonObject jsonObject =
        new RelationshipDataBuilder()
            .buildBidirectionalRelationship(trackedEntity_1, trackedEntity_2)
            .array();
    JsonObject invertedRelationship =
        new RelationshipDataBuilder()
            .buildBidirectionalRelationship(trackedEntity_2, trackedEntity_1)
            .array();

    TrackerApiResponse trackerApiResponse =
        trackerActions.postAndGetJobReport(jsonObject).validateSuccessfulImport();

    trackerApiResponse.validateRelationships().body("stats.created", equalTo(1));

    String createdRelationshipUid = trackerApiResponse.extractImportedRelationships().get(0);

    // when posting the same payload, then relationship is ignored both ways
    Stream.of(jsonObject, invertedRelationship)
        .map(trackerActions::postAndGetJobReport)
        .map(TrackerApiResponse::validateErrorReport)
        .forEach(validatableResponse -> validatableResponse.body("errorCode", contains("E4018")));

    // and relationship is not duplicated
    ApiResponse relationshipResponse = trackerActions.get("/relationships?tei=" + trackedEntity_1);

    relationshipResponse
        .validate()
        .statusCode(200)
        .body("instances[0].relationship", is(createdRelationshipUid))
        .body("instances.size()", is(1));
  }

  @Test
  public void shouldDeleteRelationshipWithDeleteStrategy() {
    // arrage
    TrackerApiResponse response =
        trackerActions
            .postAndGetJobReport(
                new File("src/test/resources/tracker/importer/teis/teisAndRelationship.json"))
            .validateSuccessfulImport();

    List<String> teis = response.extractImportedTeis();
    String relationship = response.extractImportedRelationships().get(0);

    JsonObject obj =
        new JsonObjectBuilder()
            .addObject("from", relationshipItem("trackedEntity", teis.get(0)))
            .addObject("to", relationshipItem("trackedEntity", teis.get(1)))
            .addProperty("relationshipType", relationshipType)
            .addProperty("relationship", relationship)
            .wrapIntoArray("relationships");

    // act
    response =
        trackerActions.postAndGetJobReport(
            obj, new QueryParamsBuilder().add("importStrategy=DELETE"));

    // assert
    response.validate().body("status", equalTo("OK")).body("stats.deleted", equalTo(1));

    trackerActions.get("/relationships/" + relationship).validate().statusCode(404);

    trackerActions
        .getTrackedEntity(teis.get(0) + "?fields=relationships")
        .validate()
        .body("relationships", Matchers.empty());
  }

  @Test
  public void shouldValidateRelationshipType() {
    JsonObject object =
        JsonObjectBuilder.jsonObject()
            .addProperty("relationshipType", relationshipType)
            .addObject("from", relationshipItem("event", events.get(0)))
            .addObject("to", relationshipItem("trackedEntity", teis.get(0)))
            .wrapIntoArray("relationships");

    trackerActions
        .postAndGetJobReport(object)
        .validateErrorReport()
        .body("errorCode", contains("E4010"));
  }

  @Test
  public void shouldValidateBothSidesOfRelationship() {
    JsonObject object =
        JsonObjectBuilder.jsonObject()
            .addProperty("relationshipType", "xLmPUYJX8Ks")
            .addObject("from", relationshipItem("trackedEntity", "invalid-tei"))
            .addObject("to", relationshipItem("trackedEntity", "more-invalid"))
            .wrapIntoArray("relationships");
    System.out.println(object.toString());

    trackerActions
        .postAndGetJobReport(object)
        .validateErrorReport()
        .body("", hasSize(2))
        .body("errorCode", everyItem(equalTo("E4012")));
  }

  @MethodSource("provideRelationshipData")
  @ParameterizedTest(name = "{index} {1} to {3}")
  public void shouldImportRelationshipsToExistingEntities(
      String relType,
      String fromInstance,
      String fromInstanceId,
      String toInstance,
      String toInstanceId) {
    // arrange
    JsonObject relationship =
        JsonObjectBuilder.jsonObject()
            .addProperty("relationshipType", relType)
            .addObject("from", relationshipItem(fromInstance, fromInstanceId))
            .addObject("to", relationshipItem(toInstance, toInstanceId))
            .wrapIntoArray("relationships");

    createdRelationships =
        trackerActions
            .postAndGetJobReport(relationship)
            .validateSuccessfulImport()
            .extractImportedRelationships();

    ApiResponse response = trackerActions.getRelationship(createdRelationships.get(0));

    validateRelationship(
        response,
        relType,
        fromInstance,
        fromInstanceId,
        toInstance,
        toInstanceId,
        createdRelationships.get(0));

    ApiResponse entityResponse = getEntityInRelationship(toInstance, toInstanceId);
    validateRelationship(
        entityResponse,
        relType,
        fromInstance,
        fromInstanceId,
        toInstance,
        toInstanceId,
        createdRelationships.get(0));

    entityResponse = getEntityInRelationship(fromInstance, fromInstanceId);
    validateRelationship(
        entityResponse,
        relType,
        fromInstance,
        fromInstanceId,
        toInstance,
        toInstanceId,
        createdRelationships.get(0));
  }

  @MethodSource("provideDuplicateRelationshipData")
  @ParameterizedTest(name = "{index} {6}")
  public void shouldNotImportDuplicateRelationships(
      String fromTei1,
      String toTei1,
      String fromTei2,
      String toTei2,
      boolean bidirectional,
      int expectedCount,
      String representation) {
    // arrange
    String relationshipTypeId =
        relationshipTypeActions
            .get(
                "",
                new QueryParamsBuilder()
                    .addAll(
                        "filter=fromConstraint.relationshipEntity:eq:TRACKED_ENTITY_INSTANCE",
                        "filter=toConstraint.relationshipEntity:eq:TRACKED_ENTITY_INSTANCE",
                        "filter=bidirectional:eq:" + bidirectional,
                        "filter=name:like:TA"))
            .extractString("relationshipTypes.id[0]");

    JsonObject relationship1 =
        JsonObjectBuilder.jsonObject()
            .addProperty("relationshipType", relationshipTypeId)
            .addObject("from", relationshipItem("trackedEntity", fromTei1))
            .addObject("to", relationshipItem("trackedEntity", toTei1))
            .build();

    JsonObject relationship2 =
        JsonObjectBuilder.jsonObject()
            .addProperty("relationshipType", relationshipTypeId)
            .addObject("from", relationshipItem("trackedEntity", fromTei2))
            .addObject("to", relationshipItem("trackedEntity", toTei2))
            .build();

    JsonObject payload =
        JsonObjectBuilder.jsonObject()
            .addArray("relationships", relationship1, relationship2)
            .build();

    // act
    TrackerApiResponse response = trackerActions.postAndGetJobReport(payload);

    // assert
    response.validateSuccessfulImport().validate().body("stats.created", equalTo(expectedCount));

    createdRelationships = response.extractImportedRelationships();
  }

  @Test
  public void shouldReturnErrorWhenUpdatingSoftDeletedEvent() throws Exception {
    String trackedEntity_1 = importTei();
    String trackedEntity_2 = importTei();

    JsonObject relationships =
        new RelationshipDataBuilder()
            .buildBidirectionalRelationship(trackedEntity_1, trackedEntity_2)
            .array();

    // Create Relationship
    TrackerApiResponse response =
        trackerActions.postAndGetJobReport(relationships).validateSuccessfulImport();

    String relationshipId = response.extractImportedRelationships().get(0);

    JsonObject relationshipsToDelete =
        new RelationshipDataBuilder().setRelationshipId(relationshipId).array();

    // Delete Relationship
    TrackerApiResponse deleteResponse =
        trackerActions.postAndGetJobReport(
            relationshipsToDelete, new QueryParamsBuilder().add("importStrategy=DELETE"));

    deleteResponse.validateSuccessfulImport();

    JsonObject relationshipsToImportAgain =
        new RelationshipDataBuilder()
            .setRelationshipId(relationshipId)
            .buildBidirectionalRelationship(trackedEntity_1, trackedEntity_2)
            .array();

    // Update Relationship
    TrackerApiResponse responseImportAgain =
        trackerActions.postAndGetJobReport(relationshipsToImportAgain);

    responseImportAgain.validateErrorReport().body("errorCode", Matchers.hasItem("E4017"));
  }

  @Test
  public void shouldSuccessfullyCreateHardDeletedRelationship() throws Exception {
    String trackedEntity_1 = importTei();
    String trackedEntity_2 = importTei();

    JsonObject relationships =
        new RelationshipDataBuilder()
            .buildBidirectionalRelationship(trackedEntity_1, trackedEntity_2)
            .array();

    // Create Relationship
    TrackerApiResponse response =
        trackerActions.postAndGetJobReport(relationships).validateSuccessfulImport();

    String relationshipId = response.extractImportedRelationships().get(0);

    // Hard Delete Relationship
    ApiResponse deleteResponse = relationshipActions.delete(relationshipId);

    Assertions.assertEquals(200, deleteResponse.statusCode());

    JsonObject relationshipsToImportAgain =
        new RelationshipDataBuilder()
            .setRelationshipId(relationshipId)
            .buildBidirectionalRelationship(trackedEntity_1, trackedEntity_2)
            .array();

    // Create again Relationship
    TrackerApiResponse responseImportAgain =
        trackerActions.postAndGetJobReport(relationshipsToImportAgain);

    responseImportAgain.validateRelationships().body("stats.created", equalTo(1));
  }

  private ApiResponse getEntityInRelationship(String toOrFromInstance, String id) {
    String queryParams = "?fields=relationships";
    switch (toOrFromInstance) {
      case "trackedEntity":
        {
          return trackerActions.getTrackedEntity(id + queryParams);
        }

      case "event":
        {
          return trackerActions.getEvent(id + queryParams);
        }

      case "enrollment":
        {
          return trackerActions.getEnrollment(id + queryParams);
        }
      default:
        {
          return null;
        }
    }
  }

  private void validateRelationship(
      ApiResponse response,
      String relationshipTypeId,
      String fromInstance,
      String fromInstanceId,
      String toInstance,
      String toInstanceId,
      String relationshipId) {
    String bodyPrefix = "";
    if (response.getBody().getAsJsonArray("relationships") != null) {
      bodyPrefix = String.format("relationships.find { it.relationship == '%s' }", relationshipId);
    }

    response
        .validate()
        .statusCode(200)
        .body(bodyPrefix, notNullValue())
        .rootPath(bodyPrefix)
        .body("", notNullValue())
        .body("relationshipType", equalTo(relationshipTypeId))
        .body("relationship", equalTo(relationshipId))
        .body(String.format("from.%s.%s", fromInstance, fromInstance), equalTo(fromInstanceId))
        .body(String.format("to.%s.%s", toInstance, toInstance), equalTo(toInstanceId));
  }

  @AfterEach
  public void cleanup() {
    createdRelationships.forEach(
        rel -> {
          new TestCleanUp().deleteEntity("relationships", rel);
        });
  }

  private JsonObjectBuilder relationshipItem(String type, String identifier) {
    return JsonObjectBuilder.jsonObject()
        .addObject(type, JsonObjectBuilder.jsonObject().addProperty(type, identifier));
  }
}
