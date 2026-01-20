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
package org.hisp.dhis.webapi.controller.tracker;

import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.test.utils.Assertions;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.util.DateUtils;

public class JsonAssertions {

  public static void assertUser(JsonUser expected, JsonUser actual, String property) {
    assertAll(
        property,
        () -> assertEquals(expected.getUid(), actual.getUid(), "uid"),
        () -> assertEquals(expected.getUsername(), actual.getUsername(), "username"),
        () -> assertEquals(expected.getFirstName(), actual.getFirstName(), "firstName"),
        () -> assertEquals(expected.getSurname(), actual.getSurname(), "surname"),
        () -> assertEquals(expected.getDisplayName(), actual.getDisplayName(), "displayName"));
  }

  public static void assertProgramOwners(
      List<Enrollment> enrollments, JsonList<JsonProgramOwner> programOwners) {
    for (Enrollment enrollment : enrollments) {
      JsonProgramOwner programOwner =
          programOwners.stream()
              .filter(po -> po.getProgram().equals(enrollment.getProgram().getIdentifier()))
              .findFirst()
              .get();
      assertAll(
          () -> assertEquals(enrollment.getOrgUnit().getIdentifier(), programOwner.getOrgUnit()),
          () ->
              assertEquals(
                  enrollment.getTrackedEntity().getValue(), programOwner.getTrackedEntity()),
          () -> assertEquals(enrollment.getProgram().getIdentifier(), programOwner.getProgram()));
    }
  }

  public static JsonRelationship assertFirstRelationship(
      Relationship expected, JsonList<JsonRelationship> actual) {
    return assertNthRelationship(expected, 0, actual);
  }

  public static JsonRelationship assertNthRelationship(
      Relationship expected, int n, JsonList<JsonRelationship> actual) {
    assertFalse(actual.isEmpty(), "relationships should not be empty");
    assertTrue(
        actual.size() >= n,
        String.format("element %d does not exist in %d relationships elements", n, actual.size()));
    JsonRelationship jsonRelationship = actual.get(n);
    assertRelationship(expected, jsonRelationship);
    return jsonRelationship;
  }

  public static void assertRelationship(Relationship expected, JsonRelationship actual) {
    assertFalse(actual.isEmpty(), "relationship should not be empty");
    assertEquals(expected.getUid().getValue(), actual.getRelationship(), "relationship UID");
    assertEquals(
        DateUtils.toIso8601NoTz(DateUtils.fromInstant(expected.getCreatedAtClient())),
        actual.getCreatedAtClient(),
        "createdAtClient date");
    assertEquals(
        expected.getRelationshipType().getIdentifier(),
        actual.getRelationshipType(),
        "relationshipType UID");
  }

  public static void assertNoRelationships(JsonObject json) {
    assertFalse(json.isEmpty());
    JsonArray rels = json.getArray("relationships");
    assertTrue(rels.isEmpty(), "relationships should not contain any relationships");
  }

  public static void assertEventWithinRelationshipItem(
      TrackerEvent expected, JsonRelationshipItem actual) {
    JsonRelationshipItem.JsonEvent jsonEvent = actual.getEvent();
    assertFalse(jsonEvent.isEmpty(), "event should not be empty");
    assertEquals(expected.getUid().getValue(), jsonEvent.getEvent(), "event UID");

    assertEquals(expected.getStatus().toString(), jsonEvent.getStatus(), "event status");
    assertEquals(
        expected.getProgramStage().getIdentifier(),
        jsonEvent.getProgramStage(),
        "event programStage UID");
    assertEquals(
        expected.getEnrollment().getValue(), jsonEvent.getEnrollment(), "event enrollment UID");
    assertFalse(
        jsonEvent.has("relationships"), "relationships is not returned within relationship items");
  }

  public static void assertTrackedEntityWithinRelationshipItem(
      TrackedEntity expected, JsonRelationshipItem actual) {
    JsonRelationshipItem.JsonTrackedEntity jsonTe = actual.getTrackedEntity();
    assertFalse(jsonTe.isEmpty(), "trackedEntity should not be empty");
    assertEquals(expected.getUid().getValue(), jsonTe.getTrackedEntity(), "trackedEntity UID");
    assertEquals(
        expected.getTrackedEntityType().getIdentifier(),
        jsonTe.getTrackedEntityType(),
        "trackedEntityType UID");
    assertEquals(expected.getOrgUnit().getIdentifier(), jsonTe.getOrgUnit(), "orgUnit UID");
    assertFalse(jsonTe.getAttributes().isEmpty(), "attributes should be empty");
    assertFalse(
        jsonTe.has("relationships"), "relationships is not returned within relationship items");
  }

  public static void assertHasOnlyUid(UID expected, String member, JsonObject json) {
    JsonObject j = json.getObject(member);
    assertFalse(j.isEmpty(), member + " should not be empty");
    assertHasOnlyMembers(j, member);
    assertEquals(expected.getValue(), j.getString(member).string(), member + " UID");
  }

  public static void assertEnrollmentWithinRelationshipItem(
      Enrollment expected, JsonRelationshipItem actual) {
    JsonRelationshipItem.JsonEnrollment jsonEnrollment = actual.getEnrollment();
    assertFalse(jsonEnrollment.isEmpty(), "enrollment should not be empty");
    assertEquals(expected.getUid().getValue(), jsonEnrollment.getEnrollment(), "enrollment UID");
    assertEquals(
        expected.getTrackedEntity().getValue(),
        jsonEnrollment.getTrackedEntity(),
        "trackedEntity UID");
    assertEquals(expected.getProgram().getIdentifier(), jsonEnrollment.getProgram(), "program UID");
    assertEquals(expected.isFollowUp(), jsonEnrollment.getFollowUp(), "followUp");
    assertEquals(expected.getOrgUnit().getIdentifier(), jsonEnrollment.getOrgUnit(), "orgUnit UID");
    assertFalse(jsonEnrollment.getArray("events").isEmpty(), "events should be empty");
    assertFalse(
        jsonEnrollment.has("relationships"),
        "relationships is not returned within relationship items");
  }

  public static void assertHasOnlyMembers(JsonObject json, String... names) {
    Set<String> actual = new HashSet<>(json.names());
    Set<String> expected = Set.of(names);
    assertEquals(
        expected.size(), actual.size(), () -> "unexpected total number of members in " + json);
    assertTrue(
        actual.containsAll(expected),
        () -> "members mismatch between actual: " + actual + ", expected: " + expected);
  }

  public static void assertHasNoMember(JsonObject json, String... names) {
    assertAll(
        String.format("Unexpected member(s) in %s", json),
        Arrays.stream(names)
            .map(
                name ->
                    () ->
                        assertFalse(
                            json.has(name), String.format("member \"%s\" unexpected ", name))));
  }

  public static void assertHasMember(JsonObject json, String name) {
    assertTrue(json.has(name), String.format("member \"%s\" should be in %s", name, json));
  }

  /**
   * Asserts that the actual list contains an element matching the predicate. If the element is
   * found, it is returned.
   *
   * @param actual the list to search
   * @param predicate the predicate to match the element
   * @param messageSubject the subject of the message in case the element is not found
   * @return the element that matches the predicate
   * @param <T> the type of the elements in the list
   */
  public static <T extends JsonValue> T assertContains(
      JsonList<T> actual, Predicate<? super T> predicate, String messageSubject) {
    Optional<T> element = actual.stream().filter(predicate).findFirst();
    assertTrue(
        element.isPresent(), () -> String.format("%s not found in %s", messageSubject, actual));
    return element.get();
  }

  public static <E extends JsonValue, T> void assertContainsAll(
      Collection<T> expected, JsonList<E> actual, Function<E, T> toValue) {
    assertFalse(
        actual.isEmpty(), () -> String.format("expected %s instead actual is empty", expected));
    assertTrue(
        actual.containsAll(toValue, expected),
        () -> String.format("expected %s instead got %s", expected, actual));
  }

  public static void assertReportEntities(
      List<String> expectedEntityUids, TrackerType trackerType, JsonImportReport importReport) {
    JsonTypeReport jsonTypeReport =
        switch (trackerType) {
          case TRACKED_ENTITY -> importReport.getBundleReport().getTrackedEntities();
          case ENROLLMENT -> importReport.getBundleReport().getEnrollments();
          case EVENT -> importReport.getBundleReport().getEvents();
          case RELATIONSHIP -> importReport.getBundleReport().getRelationships();
        };

    List<String> reportEntityUids =
        jsonTypeReport.getEntityReport().stream().map(JsonEntity::getUid).toList();
    assertEquals(expectedEntityUids, reportEntityUids);
  }

  public static void assertPagerLink(String actual, int page, int pageSize, String start) {
    assertNotNull(actual, "expected a link to a prev/nextPage");
    assertAll(
        "asserting link to a prev/nextPage",
        () -> assertStartsWith(start, actual),
        () -> Assertions.assertContains("page=" + page, actual),
        () -> Assertions.assertContains("pageSize=" + pageSize, actual));
  }
}
