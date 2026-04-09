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
package org.hisp.dhis.hibernate.jsonb.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JsonEventDataValueSetBinaryTypeTest {

  private JsonEventDataValueSetBinaryType type;

  @BeforeEach
  void setUp() {
    type = new JsonEventDataValueSetBinaryType();
  }

  @Test
  void deepCopyReturnsNullForNull() {
    assertNull(type.deepCopy(null));
  }

  @Test
  void deepCopyReturnsEmptySetForEmptySet() {
    Set<EventDataValue> original = new HashSet<>();

    @SuppressWarnings("unchecked")
    Set<EventDataValue> copy = (Set<EventDataValue>) type.deepCopy(original);

    assertNotSame(original, copy);
    assertTrue(copy.isEmpty());
  }

  @Test
  void deepCopyCreatesIndependentCopy() {
    Date created = new Date(1000L);
    Date lastUpdated = new Date(2000L);
    UserInfoSnapshot userInfo = UserInfoSnapshot.of(1L, "code", "uid", "admin", "Admin", "User");

    EventDataValue edv = new EventDataValue();
    edv.setDataElement("de1");
    edv.setValue("value1");
    edv.setCreated(created);
    edv.setLastUpdated(lastUpdated);
    edv.setCreatedByUserInfo(userInfo);
    edv.setLastUpdatedByUserInfo(userInfo);
    edv.setStoredBy("admin");
    edv.setProvidedElsewhere(true);

    Set<EventDataValue> original = new HashSet<>();
    original.add(edv);

    @SuppressWarnings("unchecked")
    Set<EventDataValue> copy = (Set<EventDataValue>) type.deepCopy(original);

    assertNotSame(original, copy);
    assertEquals(1, copy.size());

    EventDataValue copiedEdv = copy.iterator().next();
    assertNotSame(edv, copiedEdv);
    assertEquals("de1", copiedEdv.getDataElement());
    assertEquals("value1", copiedEdv.getValue());
    assertEquals("admin", copiedEdv.getStoredBy());
    assertTrue(copiedEdv.getProvidedElsewhere());
    assertEquals(userInfo, copiedEdv.getCreatedByUserInfo());
    assertEquals(userInfo, copiedEdv.getLastUpdatedByUserInfo());

    // Dates must be independent copies
    assertNotSame(edv.getCreated(), copiedEdv.getCreated());
    assertEquals(edv.getCreated(), copiedEdv.getCreated());
    assertNotSame(edv.getLastUpdated(), copiedEdv.getLastUpdated());
    assertEquals(edv.getLastUpdated(), copiedEdv.getLastUpdated());
  }

  @Test
  void deepCopyIsIndependentOfOriginal() {
    EventDataValue edv1 = new EventDataValue("de1", "value1");
    edv1.setCreated(new Date(1000L));
    edv1.setLastUpdated(new Date(2000L));

    EventDataValue edv2 = new EventDataValue("de2", "value2");
    edv2.setCreated(new Date(3000L));
    edv2.setLastUpdated(new Date(4000L));

    Set<EventDataValue> original = new HashSet<>();
    original.add(edv1);
    original.add(edv2);

    @SuppressWarnings("unchecked")
    Set<EventDataValue> copy = (Set<EventDataValue>) type.deepCopy(original);

    // Mutating original value does not affect the copy
    edv1.setValue("modified");
    edv1.getCreated().setTime(9999L);

    EventDataValue copiedEdv1 =
        copy.stream().filter(e -> "de1".equals(e.getDataElement())).findFirst().orElse(null);
    assertNotNull(copiedEdv1);
    assertEquals("value1", copiedEdv1.getValue());
    assertEquals(1000L, copiedEdv1.getCreated().getTime());
  }

  @Test
  void deepCopyHandlesNullDates() {
    EventDataValue edv = new EventDataValue();
    edv.setDataElement("de1");
    edv.setValue("value1");
    edv.setCreated(null);
    edv.setLastUpdated(null);

    Set<EventDataValue> original = new HashSet<>();
    original.add(edv);

    @SuppressWarnings("unchecked")
    Set<EventDataValue> copy = (Set<EventDataValue>) type.deepCopy(original);

    EventDataValue copiedEdv = copy.iterator().next();
    assertNull(copiedEdv.getCreated());
    assertNull(copiedEdv.getLastUpdated());
  }

  @Test
  void deepCopyProducesSameJsonAsOriginal() {
    UserInfoSnapshot userInfo = UserInfoSnapshot.of(1L, "code", "uid", "admin", "Admin", "User");

    EventDataValue edv1 = new EventDataValue();
    edv1.setDataElement("de1");
    edv1.setValue("value1");
    edv1.setCreated(new Date(1000L));
    edv1.setLastUpdated(new Date(2000L));
    edv1.setCreatedByUserInfo(userInfo);
    edv1.setLastUpdatedByUserInfo(userInfo);
    edv1.setStoredBy("admin");
    edv1.setProvidedElsewhere(false);

    EventDataValue edv2 = new EventDataValue();
    edv2.setDataElement("de2");
    edv2.setValue("value2");
    edv2.setCreated(new Date(3000L));
    edv2.setLastUpdated(new Date(4000L));
    edv2.setStoredBy("system");

    Set<EventDataValue> original = new HashSet<>();
    original.add(edv1);
    original.add(edv2);

    @SuppressWarnings("unchecked")
    Set<EventDataValue> copy = (Set<EventDataValue>) type.deepCopy(original);

    String originalJson = type.convertObjectToJson(original);
    String copyJson = type.convertObjectToJson(copy);
    assertEquals(originalJson, copyJson);
  }

  @Test
  void convertObjectToJsonProducesMapKeyedByDataElement() throws Exception {
    EventDataValue edv = new EventDataValue();
    edv.setDataElement("deABCDEF123");
    edv.setValue("hello");
    edv.setCreated(new Date(1000L));
    edv.setLastUpdated(new Date(2000L));
    edv.setStoredBy("admin");
    edv.setProvidedElsewhere(false);

    String json = type.convertObjectToJson(Set.of(edv));

    ObjectMapper om = new ObjectMapper();
    JsonNode root = om.readTree(json);
    assertTrue(root.isObject());
    assertTrue(root.has("deABCDEF123"));

    JsonNode entry = root.get("deABCDEF123");
    assertEquals("hello", entry.get("value").asText());
    assertEquals("admin", entry.get("storedBy").asText());
    assertFalse(entry.get("providedElsewhere").asBoolean());
    // dataElement must NOT appear inside the value (it's the key)
    assertNull(entry.get("dataElement"));
  }

  @Test
  void convertObjectToJsonWithMultipleDataValues() throws Exception {
    EventDataValue edv1 = new EventDataValue();
    edv1.setDataElement("de1");
    edv1.setValue("val1");
    edv1.setCreated(new Date(1000L));
    edv1.setLastUpdated(new Date(2000L));

    EventDataValue edv2 = new EventDataValue();
    edv2.setDataElement("de2");
    edv2.setValue("val2");
    edv2.setCreated(new Date(3000L));
    edv2.setLastUpdated(new Date(4000L));

    Set<EventDataValue> set = new HashSet<>();
    set.add(edv1);
    set.add(edv2);

    String json = type.convertObjectToJson(set);

    ObjectMapper om = new ObjectMapper();
    JsonNode root = om.readTree(json);
    assertEquals(2, root.size());
    assertEquals("val1", root.get("de1").get("value").asText());
    assertEquals("val2", root.get("de2").get("value").asText());
  }

  @Test
  void convertObjectToJsonWithEmptySet() {
    String json = type.convertObjectToJson(Collections.emptySet());
    assertEquals("{}", json);
  }

  @Test
  void convertObjectToJsonWithNullSet() {
    String json = type.convertObjectToJson(null);
    assertEquals("{}", json);
  }

  @Test
  void convertObjectToJsonWithUserInfoSnapshot() throws Exception {
    UserInfoSnapshot userInfo = UserInfoSnapshot.of(1L, "code", "uid", "admin", "Admin", "User");

    EventDataValue edv = new EventDataValue();
    edv.setDataElement("de1");
    edv.setValue("value1");
    edv.setCreated(new Date(1000L));
    edv.setLastUpdated(new Date(2000L));
    edv.setCreatedByUserInfo(userInfo);
    edv.setLastUpdatedByUserInfo(userInfo);

    String json = type.convertObjectToJson(Set.of(edv));

    ObjectMapper om = new ObjectMapper();
    JsonNode entry = om.readTree(json).get("de1");
    JsonNode createdBy = entry.get("createdByUserInfo");
    assertNotNull(createdBy);
    assertEquals("admin", createdBy.get("username").asText());
    assertEquals("Admin", createdBy.get("firstName").asText());
    assertEquals("User", createdBy.get("surname").asText());
    assertEquals("uid", createdBy.get("uid").asText());
  }

  @Test
  void roundTripJsonPreservesAllFields() {
    UserInfoSnapshot userInfo = UserInfoSnapshot.of(1L, "code", "uid", "admin", "Admin", "User");

    EventDataValue edv = new EventDataValue();
    edv.setDataElement("de1");
    edv.setValue("value1");
    edv.setCreated(new Date(1000L));
    edv.setLastUpdated(new Date(2000L));
    edv.setCreatedByUserInfo(userInfo);
    edv.setLastUpdatedByUserInfo(userInfo);
    edv.setStoredBy("admin");
    edv.setProvidedElsewhere(true);

    Set<EventDataValue> expected = Set.of(edv);
    String json = type.convertObjectToJson(expected);

    @SuppressWarnings("unchecked")
    Set<EventDataValue> actual = (Set<EventDataValue>) type.convertJsonToObject(json);

    assertEquals(expected, actual);
  }

  @Test
  void roundTripJsonWithNullOptionalFields() {
    EventDataValue edv = new EventDataValue();
    edv.setDataElement("de1");
    edv.setValue("value1");
    edv.setCreated(null);
    edv.setLastUpdated(null);
    edv.setCreatedByUserInfo(null);
    edv.setLastUpdatedByUserInfo(null);
    edv.setStoredBy(null);
    edv.setProvidedElsewhere(null);

    String json = type.convertObjectToJson(Set.of(edv));

    @SuppressWarnings("unchecked")
    Set<EventDataValue> result = (Set<EventDataValue>) type.convertJsonToObject(json);

    assertEquals(1, result.size());
    EventDataValue restored = result.iterator().next();
    assertEquals("de1", restored.getDataElement());
    assertEquals("value1", restored.getValue());
    // EventDataValue initializes created/lastUpdated to new Date() in its field declaration,
    // so null dates are restored as current time after deserialization
    assertNotNull(restored.getCreated());
    assertNotNull(restored.getLastUpdated());
    assertNull(restored.getCreatedByUserInfo());
    assertNull(restored.getLastUpdatedByUserInfo());
    assertNull(restored.getStoredBy());
    // providedElsewhere defaults to false in EventDataValue field declaration
    assertFalse(restored.getProvidedElsewhere());
  }

  @Test
  void equalsReturnsTrueForIdenticalSets() {
    EventDataValue edv = new EventDataValue();
    edv.setDataElement("de1");
    edv.setValue("value1");
    edv.setCreated(new Date(1000L));
    edv.setLastUpdated(new Date(2000L));
    edv.setStoredBy("admin");

    Set<EventDataValue> set = Set.of(edv);

    @SuppressWarnings("unchecked")
    Set<EventDataValue> snapshot = (Set<EventDataValue>) type.deepCopy(set);

    assertTrue(type.equals(snapshot, set));
  }

  @Test
  void equalsReturnsFalseWhenValueChanged() {
    EventDataValue edv = new EventDataValue();
    edv.setDataElement("de1");
    edv.setValue("value1");
    edv.setCreated(new Date(1000L));
    edv.setLastUpdated(new Date(2000L));

    Set<EventDataValue> set = new HashSet<>();
    set.add(edv);

    @SuppressWarnings("unchecked")
    Set<EventDataValue> snapshot = (Set<EventDataValue>) type.deepCopy(set);

    // Simulate what TrackerEventPersister.updateDataValue does: modify value in-place
    edv.setValue("modified");

    assertFalse(type.equals(snapshot, set));
  }

  @Test
  void equalsReturnsFalseWhenStoredByChanged() {
    EventDataValue edv = new EventDataValue();
    edv.setDataElement("de1");
    edv.setValue("value1");
    edv.setStoredBy("admin");

    Set<EventDataValue> set = new HashSet<>();
    set.add(edv);

    @SuppressWarnings("unchecked")
    Set<EventDataValue> snapshot = (Set<EventDataValue>) type.deepCopy(set);

    edv.setStoredBy("other_user");

    assertFalse(type.equals(snapshot, set));
  }

  @Test
  void equalsReturnsFalseWhenDataValueAdded() {
    EventDataValue edv1 = new EventDataValue();
    edv1.setDataElement("de1");
    edv1.setValue("value1");

    Set<EventDataValue> set = new HashSet<>();
    set.add(edv1);

    @SuppressWarnings("unchecked")
    Set<EventDataValue> snapshot = (Set<EventDataValue>) type.deepCopy(set);

    EventDataValue edv2 = new EventDataValue();
    edv2.setDataElement("de2");
    edv2.setValue("value2");
    set.add(edv2);

    assertFalse(type.equals(snapshot, set));
  }

  @Test
  void equalsReturnsFalseWhenDataValueRemoved() {
    EventDataValue edv1 = new EventDataValue();
    edv1.setDataElement("de1");
    edv1.setValue("value1");

    EventDataValue edv2 = new EventDataValue();
    edv2.setDataElement("de2");
    edv2.setValue("value2");

    Set<EventDataValue> set = new HashSet<>();
    set.add(edv1);
    set.add(edv2);

    @SuppressWarnings("unchecked")
    Set<EventDataValue> snapshot = (Set<EventDataValue>) type.deepCopy(set);

    set.remove(edv2);

    assertFalse(type.equals(snapshot, set));
  }
}
