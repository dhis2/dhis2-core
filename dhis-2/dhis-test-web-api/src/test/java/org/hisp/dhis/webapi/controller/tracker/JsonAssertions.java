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
package org.hisp.dhis.webapi.controller.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.TrackerType;

public class JsonAssertions {

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
    assertEquals(expected.getUid(), actual.getRelationship(), "relationship UID");
    assertEquals(
        expected.getRelationshipType().getUid(),
        actual.getRelationshipType(),
        "relationshipType UID");
  }

  public static void assertNoRelationships(JsonObject json) {
    assertFalse(json.isEmpty());
    JsonArray rels = json.getArray("instances");
    assertTrue(rels.isEmpty(), "instances should not contain any relationships");
  }

  public static void assertEventWithinRelationshipItem(
      Event expected, JsonRelationshipItem actual) {
    JsonRelationshipItem.JsonEvent jsonEvent = actual.getEvent();
    assertFalse(jsonEvent.isEmpty(), "event should not be empty");
    assertEquals(expected.getUid(), jsonEvent.getEvent(), "event UID");

    assertEquals(expected.getStatus().toString(), jsonEvent.getStatus(), "event status");
    assertEquals(
        expected.getProgramStage().getUid(), jsonEvent.getProgramStage(), "event programStage UID");
    assertEquals(
        expected.getEnrollment().getUid(), jsonEvent.getEnrollment(), "event enrollment UID");
    assertFalse(
        jsonEvent.has("relationships"), "relationships is not returned within relationship items");
  }

  public static void assertTrackedEntityWithinRelationshipItem(
      TrackedEntity expected, JsonRelationshipItem actual) {
    JsonRelationshipItem.JsonTrackedEntity jsonTe = actual.getTrackedEntity();
    assertFalse(jsonTe.isEmpty(), "trackedEntity should not be empty");
    assertEquals(expected.getUid(), jsonTe.getTrackedEntity(), "trackedEntity UID");
    assertEquals(
        expected.getTrackedEntityType().getUid(),
        jsonTe.getTrackedEntityType(),
        "trackedEntityType UID");
    assertEquals(expected.getOrganisationUnit().getUid(), jsonTe.getOrgUnit(), "orgUnit UID");
    assertTrue(jsonTe.getAttributes().isEmpty(), "attributes should be empty");
    assertFalse(
        jsonTe.has("relationships"), "relationships is not returned within relationship items");
  }

  public static void assertHasOnlyUid(String expectedUid, String member, JsonObject json) {
    JsonObject j = json.getObject(member);
    assertFalse(j.isEmpty(), member + " should not be empty");
    assertHasOnlyMembers(j, member);
    assertEquals(expectedUid, j.getString(member).string(), member + " UID");
  }

  public static void assertEnrollmentWithinRelationship(
      Enrollment expected, JsonRelationshipItem actual) {
    JsonRelationshipItem.JsonEnrollment jsonEnrollment = actual.getEnrollment();
    assertFalse(jsonEnrollment.isEmpty(), "enrollment should not be empty");
    assertEquals(expected.getUid(), jsonEnrollment.getEnrollment(), "enrollment UID");
    assertEquals(
        expected.getTrackedEntity().getUid(),
        jsonEnrollment.getTrackedEntity(),
        "trackedEntity UID");
    assertEquals(expected.getProgram().getUid(), jsonEnrollment.getProgram(), "program UID");
    assertEquals(
        expected.getOrganisationUnit().getUid(), jsonEnrollment.getOrgUnit(), "orgUnit UID");
    assertTrue(jsonEnrollment.getArray("events").isEmpty(), "events should be empty");
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

  public static void assertHasNoMember(JsonObject json, String name) {
    assertFalse(json.has(name), String.format("member \"%s\" should NOT be in %s", name, json));
  }

  public static void assertHasMembers(JsonObject json, String... names) {
    for (String name : names) {
      assertHasMember(json, name);
    }
  }

  public static void assertHasMember(JsonObject json, String name) {
    assertTrue(json.has(name), String.format("member \"%s\" should be in %s", name, json));
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
}
