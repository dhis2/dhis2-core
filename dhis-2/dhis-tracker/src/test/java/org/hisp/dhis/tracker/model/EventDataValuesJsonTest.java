/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.tracker.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.hibernate.jsonb.type.JsonEventDataValueSetBinaryType;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.junit.jupiter.api.Test;

class EventDataValuesJsonTest {
  private static final UserInfoSnapshot ALICE =
      UserInfoSnapshot.of(1, "alice", "aliceUid001", "alice", "Alice", "Aydin");

  private static final UserInfoSnapshot BOB =
      UserInfoSnapshot.of(2, null, "bobUid00001", "bob", "Bob", "Bello");

  @Test
  void shouldNotStoreUsersThatAreTheEventsCreator() {
    String json = EventDataValuesJson.toJson(Set.of(value("de1", ALICE, ALICE)), ALICE);

    JsonObject stored = JsonMixed.of(json).getObject("de1");
    assertAll(
        () -> assertFalse(stored.has("createdByUserInfo"), json),
        () -> assertFalse(stored.has("lastUpdatedByUserInfo"), json),
        () -> assertEquals("1", stored.getString("value").string()));
  }

  @Test
  void shouldStoreCreatorThatIsNotTheEventsCreator() {
    String json = EventDataValuesJson.toJson(Set.of(value("de1", BOB, BOB)), ALICE);

    JsonObject stored = JsonMixed.of(json).getObject("de1");
    assertAll(
        () ->
            assertEquals(
                "bobUid00001", stored.getObject("createdByUserInfo").getString("uid").string()),
        () -> assertFalse(stored.has("lastUpdatedByUserInfo"), json));
  }

  @Test
  void shouldStoreUpdaterThatIsNotTheCreator() {
    String json = EventDataValuesJson.toJson(Set.of(value("de1", ALICE, BOB)), ALICE);

    JsonObject stored = JsonMixed.of(json).getObject("de1");
    assertAll(
        () -> assertFalse(stored.has("createdByUserInfo"), json),
        () ->
            assertEquals(
                "bobUid00001",
                stored.getObject("lastUpdatedByUserInfo").getString("uid").string()));
  }

  @Test
  void shouldStoreUpdaterWhenCreatorIsDerivedFromTheEvent() {
    // as read by the import preheat from a stored value: the creator is derived from the event
    EventDataValue value = value("de1", null, BOB);

    String json = EventDataValuesJson.toJson(Set.of(value), ALICE);

    assertUsers(EventDataValuesJson.fromJson(json, ALICE), "de1", ALICE, BOB);
  }

  @Test
  void shouldStoreUserThatWasRenamedSinceTheEventWasCreated() {
    UserInfoSnapshot renamed =
        UserInfoSnapshot.of(1, "alice", "aliceUid001", "alice", "Alice", "Okafor");

    String json = EventDataValuesJson.toJson(Set.of(value("de1", renamed, renamed)), ALICE);

    JsonObject stored = JsonMixed.of(json).getObject("de1");
    assertAll(
        () ->
            assertEquals(
                "Okafor", stored.getObject("createdByUserInfo").getString("surname").string()),
        () -> assertFalse(stored.has("lastUpdatedByUserInfo"), json));
    assertUsers(EventDataValuesJson.fromJson(json, ALICE), "de1", renamed, renamed);
  }

  @Test
  void shouldStoreCreatorIfEventsCreatorWasStoredWithoutIdAndCode() {
    UserInfoSnapshot withoutIdAndCode =
        UserInfoSnapshot.of(0, null, "aliceUid001", "alice", "Alice", "Aydin");

    String json = EventDataValuesJson.toJson(Set.of(value("de1", ALICE, ALICE)), withoutIdAndCode);

    assertTrue(JsonMixed.of(json).getObject("de1").has("createdByUserInfo"), json);
    assertUsers(EventDataValuesJson.fromJson(json, withoutIdAndCode), "de1", ALICE, ALICE);
  }

  @Test
  void shouldNotChangeTheValuesItSerializes() {
    EventDataValue value = value("de1", ALICE, ALICE);

    EventDataValuesJson.toJson(Set.of(value), ALICE);

    assertSame(ALICE, value.getCreatedByUserInfo());
    assertSame(ALICE, value.getLastUpdatedByUserInfo());
  }

  @Test
  void shouldReadBackTheUsersItLeftOut() {
    Set<EventDataValue> values =
        Set.of(value("de1", ALICE, ALICE), value("de2", ALICE, BOB), value("de3", BOB, BOB));

    Set<EventDataValue> read =
        EventDataValuesJson.fromJson(EventDataValuesJson.toJson(values, ALICE), ALICE);

    assertUsers(read, "de1", ALICE, ALICE);
    assertUsers(read, "de2", ALICE, BOB);
    assertUsers(read, "de3", BOB, BOB);
  }

  @Test
  void shouldReadUsersOfValuesStoredWithAllUsers() {
    String json =
        """
        {"de1": {"value": "1", "created": "2026-09-27T08:00:00.000",
                 "lastUpdated": "2026-09-27T09:00:00.000", "providedElsewhere": false,
                 "createdByUserInfo": {"id": 2, "uid": "bobUid00001", "username": "bob",
                                       "firstName": "Bob", "surname": "Bello"},
                 "lastUpdatedByUserInfo": {"id": 1, "code": "alice", "uid": "aliceUid001",
                                           "username": "alice",
                                           "firstName": "Alice", "surname": "Aydin"}}}
        """;

    assertUsers(EventDataValuesJson.fromJson(json, ALICE), "de1", BOB, ALICE);
    assertUsers(EventDataValuesJson.fromJson("de1", dataValue(json), ALICE), BOB, ALICE);
  }

  @Test
  void shouldReadNoUsersIfEventHasNoCreator() {
    String json = "{\"de1\": {\"value\": \"1\"}}";

    assertUsers(EventDataValuesJson.fromJson(json, null), "de1", null, null);
    assertUsers(EventDataValuesJson.fromJson("de1", dataValue(json), null), null, null);
  }

  @Test
  void shouldStoreAndReadUsersIfEventHasNoCreator() {
    String json = EventDataValuesJson.toJson(Set.of(value("de1", BOB, BOB)), null);

    assertTrue(JsonMixed.of(json).getObject("de1").has("createdByUserInfo"), json);
    assertUsers(EventDataValuesJson.fromJson(json, null), "de1", BOB, BOB);
  }

  @Test
  void shouldReadUsersMarkedAsUnknownAsNull() {
    String json =
        """
        {"de1": {"value": "1", "createdByUserInfo": {}, "lastUpdatedByUserInfo": {}},
         "de2": {"value": "2", "createdByUserInfo": {"id": 0},
                 "lastUpdatedByUserInfo": {"id": 2, "uid": "bobUid00001", "username": "bob",
                                           "firstName": "Bob", "surname": "Bello"}}}
        """;

    Set<EventDataValue> read = EventDataValuesJson.fromJson(json, ALICE);

    assertUsers(read, "de1", null, null);
    assertUsers(read, "de2", null, BOB);
    assertUsers(EventDataValuesJson.fromJson("de1", dataValue(json), ALICE), null, null);
  }

  @Test
  void shouldKeepUsersMarkedAsUnknownWhenStoredAgain() {
    String json = "{\"de1\": {\"value\": \"1\", \"createdByUserInfo\": {}}}";
    // the import preheat reads values through Hibernate, which does not fill in users
    @SuppressWarnings("unchecked")
    Set<EventDataValue> preheated =
        (Set<EventDataValue>) new JsonEventDataValueSetBinaryType().convertJsonToObject(json);

    String stored = EventDataValuesJson.toJson(preheated, ALICE);

    assertUsers(EventDataValuesJson.fromJson(stored, ALICE), "de1", null, null);
  }

  private static EventDataValue value(
      String dataElement, UserInfoSnapshot createdBy, UserInfoSnapshot lastUpdatedBy) {
    EventDataValue value = new EventDataValue(dataElement, "1");
    value.setCreated(new Date());
    value.setLastUpdated(new Date());
    value.setCreatedByUserInfo(createdBy);
    value.setLastUpdatedByUserInfo(lastUpdatedBy);
    return value;
  }

  private static JsonObject dataValue(String json) {
    return JsonMixed.of(json).getObject("de1");
  }

  private static void assertUsers(
      Set<EventDataValue> values,
      String dataElement,
      UserInfoSnapshot createdBy,
      UserInfoSnapshot lastUpdatedBy) {
    Map<String, EventDataValue> byDataElement =
        values.stream()
            .collect(Collectors.toMap(EventDataValue::getDataElement, Function.identity()));
    assertUsers(byDataElement.get(dataElement), createdBy, lastUpdatedBy);
  }

  private static void assertUsers(
      EventDataValue value, UserInfoSnapshot createdBy, UserInfoSnapshot lastUpdatedBy) {
    assertAll(
        () -> assertSameUser(createdBy, value.getCreatedByUserInfo(), "createdByUserInfo"),
        () ->
            assertSameUser(
                lastUpdatedBy, value.getLastUpdatedByUserInfo(), "lastUpdatedByUserInfo"));
  }

  private static void assertSameUser(
      UserInfoSnapshot expected, UserInfoSnapshot actual, String property) {
    if (expected == null) {
      assertNull(actual, property);
      return;
    }
    assertTrue(
        EventDataValuesJson.sameUser(expected, actual),
        () -> property + ": expected " + describe(expected) + " but was " + describe(actual));
  }

  private static String describe(UserInfoSnapshot user) {
    return user == null
        ? "null"
        : String.join(
            " ",
            String.valueOf(user.getId()),
            user.getCode(),
            user.getUid(),
            user.getUsername(),
            user.getFirstName(),
            user.getSurname());
  }
}
