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
package org.hisp.dhis.tracker.export;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hisp.dhis.helpers.matchers.MatchesJson.matchesJSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.apache.http.HttpHeaders;
import org.hamcrest.Matcher;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.dto.TrackerApiResponse;
import org.hisp.dhis.test.e2e.helpers.JsonObjectBuilder;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackerExportTest extends TrackerApiTest {
  private static final String DEFAULT_JSON_CONTENT_TYPE_WITH_HTML_REQUEST =
      "%s do not default to application/json format when the Accept header is html";
  private static final String TE = "Kj6vYde4LHh";

  private static final String TE_POTENTIAL_DUPLICATE = "Nav6inZRw1u";

  private static String trackedEntityA;

  private static String trackedEntityB;

  private static String enrollment;

  private static String event;

  private static String trackedEntityToTrackedEntityRelationship;

  private static String enrollmentToTrackedEntityRelationship;

  private static String eventToTrackedEntityRelationship;

  private static JsonObject trackedEntityWithEnrollmentAndEventsTemplate;

  @BeforeAll
  public void beforeAll() throws Exception {
    loginActions.loginAsSuperUser();

    TrackerApiResponse response =
        trackerImportExportActions.postAndGetJobReport(
            new File(
                "src/test/resources/tracker/importer/trackedEntities/trackedEntitiesWithEnrollmentsAndEvents.json"));

    trackedEntityA = response.validateSuccessfulImport().extractImportedTrackedEntities().get(0);
    trackedEntityB = response.validateSuccessfulImport().extractImportedTrackedEntities().get(1);

    enrollment = response.extractImportedEnrollments().get(0);

    event = response.extractImportedEvents().get(0);

    trackedEntityToTrackedEntityRelationship =
        importRelationshipBetweenTrackedEntities(trackedEntityA, trackedEntityB)
            .extractImportedRelationships()
            .get(0);
    enrollmentToTrackedEntityRelationship =
        importRelationshipEnrollmentToTrackedEntity(enrollment, trackedEntityB)
            .extractImportedRelationships()
            .get(0);

    eventToTrackedEntityRelationship =
        importRelationshipEventToTrackedEntity(event, trackedEntityB)
            .extractImportedRelationships()
            .get(0);

    trackedEntityWithEnrollmentAndEventsTemplate =
        new FileReaderUtils()
            .read(
                new File(
                    "src/test/resources/tracker/importer/trackedEntities/trackedEntityWithEnrollmentAndEventsNested.json"))
            .get(JsonObject.class);
  }

  @Test
  public void shouldGetTrackedEntityWhenAttributeFilterValueContainsComma() {
    trackerImportExportActions
        .getTrackedEntities(
            new QueryParamsBuilder()
                .add("trackedEntityType", "Q9GufDoplCL")
                .add("orgUnits", "O6uvpzGd5pu")
                .add("filter", "kZeSYCgaHTk:eq:Test/,Test"))
        .validate()
        .statusCode(200)
        .body("trackedEntities[0].attributes.value", hasItem("Test,Test"));
  }

  @Test
  public void shouldGetTrackedEntityWhenAttributeFilterValueContainsColon() {
    trackerImportExportActions
        .getTrackedEntities(
            new QueryParamsBuilder()
                .add("trackedEntityType", "Q9GufDoplCL")
                .add("orgUnits", "O6uvpzGd5pu")
                .add("filter", "dIVt4l5vIOa:eq:Test/:Test"))
        .validate()
        .statusCode(200)
        .body("trackedEntities[0].attributes.value", hasItem("Test:Test"));
  }

  private Stream<Arguments> shouldReturnRequestedFields() {
    return Stream.of(
        Arguments.of(
            "/trackedEntities/" + trackedEntityA,
            "enrollments[createdAt],relationships[from[trackedEntity[trackedEntity]],to[trackedEntity[trackedEntity]]]",
            "enrollments.createdAt,relationships.from.trackedEntity.trackedEntity,relationships.to.trackedEntity.trackedEntity"),
        Arguments.of("/trackedEntities/" + trackedEntityA, "trackedEntity,enrollments", null),
        Arguments.of(
            "/enrollments/" + enrollment,
            "program,status,enrolledAt,relationships,attributes",
            null),
        Arguments.of(
            "/trackedEntities/" + trackedEntityA,
            "*",
            "trackedEntity,trackedEntityType,createdAt,updatedAt,orgUnit,inactive,deleted,potentialDuplicate,updatedBy,attributes",
            null),
        Arguments.of("/events/" + event, "enrollment,createdAt", null),
        Arguments.of(
            "/relationships/" + trackedEntityToTrackedEntityRelationship,
            "from,to[trackedEntity[trackedEntity]]",
            "from,to.trackedEntity.trackedEntity"),
        Arguments.of(
            "/relationships/" + enrollmentToTrackedEntityRelationship,
            "from,from[enrollment[enrollment]]",
            "from,from.enrollment.enrollment"));
  }

  @MethodSource
  @ParameterizedTest
  public void shouldReturnRequestedFields(String endpoint, String fields, String fieldsToValidate) {
    ApiResponse response = trackerImportExportActions.get(endpoint + "?fields=" + fields);

    response.validate().statusCode(200);

    List<String> fieldList =
        fieldsToValidate == null ? splitFields(fields) : splitFields(fieldsToValidate);

    fieldList.forEach(
        p ->
            response
                .validate()
                .body(
                    p, allOf(not(nullValue()), not(contains(nullValue())), not(emptyIterable()))));
  }

  @Test
  public void shouldGetSingleTrackedEntityWithNoEventsWhenEventsAreSoftDeleted() {
    TrackerApiResponse response =
        trackerImportExportActions
            .postAndGetJobReport(
                trackedEntityWithEnrollmentAndEventsTemplate,
                new QueryParamsBuilder().add("async=false"))
            .validateSuccessfulImport();

    assertEquals(1, response.extractImportedEvents().size());
    deleteEvent(response.extractImportedEvents().get(0));

    trackerImportExportActions
        .getTrackedEntity(
            response.extractImportedTrackedEntities().get(0),
            new QueryParamsBuilder().add("fields", "enrollments"))
        .validate()
        .statusCode(200)
        .body("enrollments.events.flatten()", empty());
  }

  @Test
  public void shouldGetSingleEnrollmentWithNoEventsWhenEventsAreSofDeleted() {
    TrackerApiResponse response =
        trackerImportExportActions
            .postAndGetJobReport(
                trackedEntityWithEnrollmentAndEventsTemplate,
                new QueryParamsBuilder().add("async=false"))
            .validateSuccessfulImport();

    assertEquals(1, response.extractImportedEvents().size());
    deleteEvent(response.extractImportedEvents().get(0)).validateSuccessfulImport();

    trackerImportExportActions
        .getEnrollment(
            response.extractImportedEnrollments().get(0),
            new QueryParamsBuilder().add("fields", "events"))
        .validate()
        .statusCode(200)
        .body("events.flatten()", empty());
  }

  @Test
  public void shouldGetTrackedEntitiesWithSoftDeletedEventsWhenIncludeDeletedInRequest() {
    TrackerApiResponse response =
        trackerImportExportActions
            .postAndGetJobReport(
                trackedEntityWithEnrollmentAndEventsTemplate,
                new QueryParamsBuilder().add("async=false"))
            .validateSuccessfulImport();

    assertEquals(1, response.extractImportedEvents().size());
    deleteEvent(response.extractImportedEvents().get(0)).validateSuccessfulImport();

    trackerImportExportActions
        .getTrackedEntities(
            new QueryParamsBuilder()
                .add("fields", "enrollments")
                .add("program", "f1AyMswryyQ")
                .add("orgUnits", "O6uvpzGd5pu")
                .add("trackedEntities", response.extractImportedTrackedEntities().get(0)))
        .validate()
        .statusCode(200)
        .body(
            "trackedEntities.enrollments.flatten().findAll { it.trackedEntity == '"
                + response.extractImportedTrackedEntities().get(0)
                + "' }.events.flatten()",
            empty());

    trackerImportExportActions
        .getTrackedEntities(
            new QueryParamsBuilder()
                .add("fields", "enrollments")
                .add("program", "f1AyMswryyQ")
                .add("orgUnits", "O6uvpzGd5pu")
                .add("trackedEntities", response.extractImportedTrackedEntities().get(0))
                .add("includeDeleted", "true"))
        .validate()
        .statusCode(200)
        .body("trackedEntities[0].enrollments.events", hasSize(1));
  }

  @Test
  public void shouldGetEnrollmentsWithEventsWhenIncludeDeletedInRequest() {
    TrackerApiResponse response =
        trackerImportExportActions
            .postAndGetJobReport(
                trackedEntityWithEnrollmentAndEventsTemplate,
                new QueryParamsBuilder().add("async=false"))
            .validateSuccessfulImport();

    assertEquals(1, response.extractImportedEvents().size());
    deleteEvent(response.extractImportedEvents().get(0)).validateSuccessfulImport();

    trackerImportExportActions
        .getEnrollments(
            new QueryParamsBuilder()
                .add("fields", "events")
                .add("program", "f1AyMswryyQ")
                .add("orgUnits", "O6uvpzGd5pu")
                .add("enrollments", response.extractImportedEnrollments().get(0)))
        .validate()
        .statusCode(200)
        .body("enrollments[0].events.flatten()", empty());

    trackerImportExportActions
        .getEnrollments(
            new QueryParamsBuilder()
                .add("fields", "events")
                .add("program", "f1AyMswryyQ")
                .add("orgUnits", "O6uvpzGd5pu")
                .add("enrollments", response.extractImportedEnrollments().get(0))
                .add("includeDeleted", "true"))
        .validate()
        .statusCode(200)
        .body("enrollments[0].events", hasSize(1));
  }

  private TrackerApiResponse deleteEvent(String eventToDelete) {
    return trackerImportExportActions.postAndGetJobReport(
        new JsonObjectBuilder()
            .addArray("events", new JsonObjectBuilder().addProperty("event", eventToDelete).build())
            .build(),
        new QueryParamsBuilder().add("importStrategy=DELETE").add("async=false"));
  }

  @Test
  public void singleTrackedEntitiesAndCollectionTrackedEntityShouldReturnSameResult()
      throws Exception {

    TrackerApiResponse trackedEntity =
        trackerImportExportActions.getTrackedEntity(
            "Kj6vYde4LHh", new QueryParamsBuilder().add("fields", "*"));

    TrackerApiResponse trackedEntities =
        trackerImportExportActions.getTrackedEntities(
            new QueryParamsBuilder()
                .add("fields", "*")
                .add("trackedEntities", "Kj6vYde4LHh")
                .add("orgUnits", "O6uvpzGd5pu"));

    JSONAssert.assertEquals(
        trackedEntity.getBody().toString(),
        trackedEntities.extractJsonObject("trackedEntities[0]").toString(),
        false);
  }

  private List<String> splitFields(String fields) {
    List<String> split = new ArrayList<>();

    // separate fields using comma delimiter, skipping commas within []
    Arrays.stream(fields.split("(?![^)(]*\\([^)(]*?\\)\\)),(?![^\\[]*\\])"))
        .forEach(
            field -> {
              if (field.contains("[")) {
                for (String s :
                    field.substring(field.indexOf("[") + 1, field.indexOf("]")).split(",")) {
                  if (s.equalsIgnoreCase("*")) {
                    split.add(field.substring(0, field.indexOf("[")));
                    return;
                  }

                  split.add(field.substring(0, field.indexOf("[")) + "." + s);
                }

                return;
              }

              split.add(field);
            });

    return split;
  }

  @Test
  public void shouldReturnSingleTrackedEntityGivenFilter() {
    trackerImportExportActions
        .get("trackedEntities?orgUnits=O6uvpzGd5pu&program=f1AyMswryyQ&filter=kZeSYCgaHTk:in:Bravo")
        .validate()
        .statusCode(200)
        .body("trackedEntities.findAll { it.trackedEntity == 'Kj6vYde4LHh' }.size()", is(1))
        .body(
            "trackedEntities.attributes.flatten().findAll { it.attribute == 'kZeSYCgaHTk' }.value",
            everyItem(is("Bravo")));
  }

  Stream<Arguments> shouldReturnTrackedEntitiesMatchingAttributeCriteria() {
    return Stream.of(
        Arguments.of("like", "av", containsString("av")),
        Arguments.of("sw", "Te", startsWith("Te")),
        Arguments.of("ew", "AVO", endsWith("avo")),
        Arguments.of("ew", "Bravo", endsWith("Bravo")),
        Arguments.of("in", "Bravo", equalTo("Bravo")));
  }

  @MethodSource()
  @ParameterizedTest
  public void shouldReturnTrackedEntitiesMatchingAttributeCriteria(
      String operator, String searchCriteria, Matcher<?> everyItemMatcher) {
    QueryParamsBuilder queryParamsBuilder =
        new QueryParamsBuilder()
            .add("orgUnits", "O6uvpzGd5pu")
            .add("program", Constants.TRACKER_PROGRAM_ID)
            .add("filter", String.format("kZeSYCgaHTk:%s:%s", operator, searchCriteria));

    trackerImportExportActions
        .getTrackedEntities(queryParamsBuilder)
        .validate()
        .statusCode(200)
        .body("trackedEntities", hasSize(greaterThanOrEqualTo(1)))
        .body(
            "trackedEntities.attributes.flatten().findAll { it.attribute == 'kZeSYCgaHTk' }.value",
            everyItem(everyItemMatcher));
  }

  @Test
  public void shouldReturnSingleTrackedEntityGivenFilterWhileSkippingPaging() {
    trackerImportExportActions
        .get(
            "trackedEntities?paging=false&orgUnits=O6uvpzGd5pu&program=f1AyMswryyQ&filter=kZeSYCgaHTk:in:Bravo")
        .validate()
        .statusCode(200)
        .body("trackedEntities.findAll { it.trackedEntity == 'Kj6vYde4LHh' }.size()", is(1))
        .body(
            "trackedEntities.attributes.flatten().findAll { it.attribute == 'kZeSYCgaHTk' }.value",
            everyItem(is("Bravo")));
  }

  @Test
  public void shouldReturnRelationshipsByTrackedEntity() {
    trackerImportExportActions
        .getRelationship("?trackedEntity=" + trackedEntityA)
        .validate()
        .statusCode(200)
        .body("relationships", hasSize(greaterThanOrEqualTo(1)))
        .rootPath("relationships[0]")
        .body("relationship", equalTo(trackedEntityToTrackedEntityRelationship))
        .body("from.trackedEntity.trackedEntity", equalTo(trackedEntityA))
        .body("to.trackedEntity.trackedEntity", equalTo(trackedEntityB));
  }

  @Test
  public void shouldReturnRelationshipsWhenEventHasRelationshipsAndFieldsIncludeRelationships() {
    trackerImportExportActions
        .get("events?events=" + event + "&fields=relationships")
        .validate()
        .statusCode(200)
        .body("events", hasSize(greaterThanOrEqualTo(1)))
        .rootPath("events[0].relationships[0]")
        .body("relationship", equalTo(eventToTrackedEntityRelationship))
        .body("from.event.event", equalTo(event))
        .body("to.trackedEntity.trackedEntity", equalTo(trackedEntityB));
  }

  @Test
  public void shouldNotReturnRelationshipsWhenEventHasRelationshipsAndFieldsExcludeRelationships() {
    trackerImportExportActions
        .get("events?events=" + event)
        .validate()
        .statusCode(200)
        .body("events[0].relationships", emptyOrNullString());
  }

  @Test
  public void shouldReturnEventWithEnrollmentOccurredOnADateWhenBeforeAndAfterIsTheSameDate() {
    trackerImportExportActions
        .get(
            "events?enrollmentOccurredAfter=2019-08-19&enrollmentOccurredBefore=2019-08-19&events=ZwwuwNp6gVd")
        .validate()
        .statusCode(200)
        .rootPath("events[0]")
        .body("event", equalTo("ZwwuwNp6gVd"));
  }

  @Test
  public void shouldReturnDescOrderedEventByTrackedEntityAttribute() {
    ApiResponse response =
        trackerImportExportActions.get(
            "events?order=dIVt4l5vIOa:desc&events=olfXZzSGacW,ZwwuwNp6gVd");
    response.validate().statusCode(200).body("events", hasSize(equalTo(2)));
    List<String> events = response.extractList("events.event.flatten()");
    assertEquals(
        List.of("olfXZzSGacW", "ZwwuwNp6gVd"), events, "Events are not in the correct order");
  }

  @Test
  public void shouldReturnAscOrderedEventByTrackedEntityAttribute() {
    ApiResponse response =
        trackerImportExportActions.get(
            "events?order=dIVt4l5vIOa:asc&events=olfXZzSGacW,ZwwuwNp6gVd");
    response.validate().statusCode(200).body("events", hasSize(equalTo(2)));
    List<String> events = response.extractList("events.event.flatten()");
    assertEquals(
        List.of("ZwwuwNp6gVd", "olfXZzSGacW"), events, "Events are not in the correct order");
  }

  @Test
  void getTrackedEntitiesByPotentialDuplicateParamNull() {
    ApiResponse response =
        trackerImportExportActions.getTrackedEntities(
            paramsForTrackedEntitiesIncludingPotentialDuplicate());

    response.validate().statusCode(200).body("trackedEntities", iterableWithSize(2));

    assertThat(
        response.getBody().getAsJsonObject(),
        matchesJSON(
            new JsonObjectBuilder()
                .addArray(
                    "trackedEntities",
                    new JsonObjectBuilder().addProperty("trackedEntity", TE).build(),
                    new JsonObjectBuilder()
                        .addProperty("trackedEntity", TE_POTENTIAL_DUPLICATE)
                        .build())
                .build()));
  }

  @Test
  void getTrackedEntitiesByPotentialDuplicateParamFalse() {
    ApiResponse response =
        trackerImportExportActions.getTrackedEntities(
            paramsForTrackedEntitiesIncludingPotentialDuplicate().add("potentialDuplicate=false"));

    response
        .validate()
        .statusCode(200)
        .body("trackedEntities", iterableWithSize(1))
        .body("trackedEntities[0].trackedEntity", equalTo(TE))
        .body("trackedEntities[0].potentialDuplicate", equalTo(false));
  }

  @Test
  void getTrackedEntitiesByPotentialDuplicateParamTrue() {
    ApiResponse response =
        trackerImportExportActions.getTrackedEntities(
            paramsForTrackedEntitiesIncludingPotentialDuplicate().add("potentialDuplicate=true"));

    response
        .validate()
        .statusCode(200)
        .body("trackedEntities", iterableWithSize(1))
        .body("trackedEntities[0].trackedEntity", equalTo(TE_POTENTIAL_DUPLICATE))
        .body("trackedEntities[0].potentialDuplicate", equalTo(true));
  }

  @Test
  void whenGetEventsShouldDefaultToJsonContentTypeWithHtmlAcceptHeader() {
    List<String> events =
        trackerImportExportActions
            .getWithHeaders(
                "events?events=" + event,
                null,
                new Headers(new Header(HttpHeaders.ACCEPT, "text/html")))
            .validate()
            .statusCode(200)
            .contentType("application/json;charset=utf-8")
            .extract()
            .jsonPath()
            .getList("events.event.flatten()");

    assertEquals(
        List.of(event),
        events,
        String.format(DEFAULT_JSON_CONTENT_TYPE_WITH_HTML_REQUEST, "Events"));
  }

  @Test
  void whenGetEventsCsvShouldGetCsvContentTypeWithHtmlAcceptHeader() {
    ApiResponse response =
        trackerImportExportActions.getWithHeaders(
            "events.csv?events=" + event,
            null,
            new Headers(new Header(HttpHeaders.ACCEPT, "text/html")));

    assertTrue(response.getContentType().contains("application/csv"));
  }

  @Test
  void whenGetTrackedEntitiesShouldDefaultToJsonContentTypeWithHtmlAcceptHeader() {
    List<String> trackedEntities =
        trackerImportExportActions
            .getWithHeaders(
                "trackedEntities?trackedEntities=" + trackedEntityA,
                null,
                new Headers(new Header(HttpHeaders.ACCEPT, "text/html")))
            .validate()
            .statusCode(200)
            .contentType("application/json;charset=utf-8")
            .extract()
            .jsonPath()
            .getList("trackedEntities.trackedEntity.flatten()");

    assertEquals(
        List.of(trackedEntityA),
        trackedEntities,
        String.format(DEFAULT_JSON_CONTENT_TYPE_WITH_HTML_REQUEST, "Tracked Entities"));
  }

  @Test
  void whenGetTrackedEntitiesCsvShouldGetCsvContentTypeWithHtmlAcceptHeader() {
    ApiResponse response =
        trackerImportExportActions.getWithHeaders(
            "trackedEntities.csv?trackedEntities=" + trackedEntityA,
            null,
            new Headers(new Header(HttpHeaders.ACCEPT, "text/html")));

    assertTrue(response.getContentType().contains("application/csv"));
  }

  @Test
  void whenGetEnrollmentsShouldDefaultToJsonContentTypeWithHtmlAcceptHeader() {
    List<String> enrollments =
        trackerImportExportActions
            .getWithHeaders(
                "enrollments?enrollments=" + enrollment,
                null,
                new Headers(new Header(HttpHeaders.ACCEPT, "text/html")))
            .validate()
            .statusCode(200)
            .contentType("application/json;charset=utf-8")
            .extract()
            .jsonPath()
            .getList("enrollments.enrollment.flatten()");

    assertEquals(
        List.of(enrollment),
        enrollments,
        String.format(DEFAULT_JSON_CONTENT_TYPE_WITH_HTML_REQUEST, "Enrollments"));
  }

  @Test
  void whenGetRelationshipsShouldDefaultToJsonContentTypeWithHtmlAcceptHeader() {
    List<String> relationships =
        trackerImportExportActions
            .getWithHeaders(
                "relationships?trackedEntity=" + trackedEntityA,
                null,
                new Headers(new Header(HttpHeaders.ACCEPT, "text/html")))
            .validate()
            .statusCode(200)
            .contentType("application/json;charset=utf-8")
            .extract()
            .jsonPath()
            .getList("relationships.relationship.flatten()");

    assertEquals(
        List.of(trackedEntityToTrackedEntityRelationship),
        relationships,
        String.format(DEFAULT_JSON_CONTENT_TYPE_WITH_HTML_REQUEST, "Relationships"));
  }

  private static QueryParamsBuilder paramsForTrackedEntitiesIncludingPotentialDuplicate() {
    return new QueryParamsBuilder()
        .addAll(
            "trackedEntities=" + TE + "," + TE_POTENTIAL_DUPLICATE,
            "trackedEntityType=" + "Q9GufDoplCL",
            "orgUnits=" + "O6uvpzGd5pu");
  }
}
