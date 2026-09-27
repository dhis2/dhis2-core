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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.hibernate.jsonb.type.JsonBinaryType;
import org.hisp.dhis.hibernate.jsonb.type.JsonEventDataValueSetBinaryType;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.util.DateUtils;

/**
 * Reads and writes the {@code eventdatavalues} JSON of {@link TrackerEvent}s and {@link
 * SingleEvent}s.
 *
 * <p>A data value's {@code createdByUserInfo} and {@code lastUpdatedByUserInfo} are only stored
 * where they cannot be derived:
 *
 * <ul>
 *   <li>a missing {@code createdByUserInfo} is the event's {@code createdByUserInfo}
 *   <li>a missing {@code lastUpdatedByUserInfo} is the data value's {@code createdByUserInfo}
 * </ul>
 *
 * Both stay correct when other data values of the event are updated later, since the event's {@code
 * createdByUserInfo} never changes and an updated data value gets its own {@code
 * lastUpdatedByUserInfo}.
 *
 * <p>Data values stored before these rules have their unknown users marked by a snapshot without a
 * uid, so that they are not attributed to the event's creator. In memory a {@code null} snapshot
 * means "derive it" and a snapshot without a uid means "unknown", which is why the import keeps the
 * data values as they are read and only the export turns them into the users they stand for using
 * {@link #fillUserInfo(Set, UserInfoSnapshot)}.
 */
public final class EventDataValuesJson {
  private static final ObjectWriter WRITER = JsonBinaryType.MAPPER.writerFor(EventDataValue.class);

  private static final ObjectReader READER =
      JsonBinaryType.MAPPER.readerFor(new TypeReference<Map<String, EventDataValue>>() {});

  private EventDataValuesJson() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Serializes the data values as a JSON object keyed by data element uid, leaving out the users
   * that can be derived. An empty or null set is serialized as {@code "{}"} to match the column's
   * NOT NULL default.
   *
   * @param values the data values of the event
   * @param eventCreatedBy the {@code createdByUserInfo} of the event
   */
  @Nonnull
  public static String toJson(
      @CheckForNull Set<EventDataValue> values, @CheckForNull UserInfoSnapshot eventCreatedBy) {
    try {
      StringWriter sw = new StringWriter();
      try (JsonGenerator gen = JsonBinaryType.MAPPER.getFactory().createGenerator(sw)) {
        gen.writeStartObject();
        if (values != null) {
          for (EventDataValue value : values) {
            gen.writeFieldName(value.getDataElement());
            WRITER.writeValue(gen, withoutDerivedUsers(value, eventCreatedBy));
          }
        }
        gen.writeEndObject();
      }
      return sw.toString();
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to serialize event data values to JSON", e);
    }
  }

  /**
   * Returns the value itself if none of its users can be derived, otherwise a copy without them, so
   * that data values shared with other objects are never changed.
   */
  private static EventDataValue withoutDerivedUsers(
      EventDataValue value, UserInfoSnapshot eventCreatedBy) {
    UserInfoSnapshot createdBy = value.getCreatedByUserInfo();
    UserInfoSnapshot lastUpdatedBy = value.getLastUpdatedByUserInfo();
    boolean derivedCreatedBy = createdBy != null && sameUser(createdBy, eventCreatedBy);
    boolean derivedLastUpdatedBy =
        lastUpdatedBy != null
            && sameUser(lastUpdatedBy, createdBy == null ? eventCreatedBy : createdBy);
    if (!derivedCreatedBy && !derivedLastUpdatedBy) {
      return value;
    }

    EventDataValue copy = new EventDataValue(value);
    if (derivedCreatedBy) {
      copy.setCreatedByUserInfo(null);
    }
    if (derivedLastUpdatedBy) {
      copy.setLastUpdatedByUserInfo(null);
    }
    return copy;
  }

  /**
   * Parses the {@code eventdatavalues} JSON of an event and fills in the users of its data values.
   *
   * @param json the {@code eventdatavalues} JSON object keyed by data element uid
   * @param eventCreatedBy the {@code createdByUserInfo} of the event
   */
  @Nonnull
  public static Set<EventDataValue> fromJson(
      @Nonnull String json, @CheckForNull UserInfoSnapshot eventCreatedBy) {
    Set<EventDataValue> values;
    try {
      Map<String, EventDataValue> data = READER.readValue(json);
      values = JsonEventDataValueSetBinaryType.convertEventDataValuesMapIntoSet(data);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to parse event data values JSON: " + json, e);
    }
    fillUserInfo(values, eventCreatedBy);
    return values;
  }

  /**
   * Parses one data value of the {@code eventdatavalues} JSON of an event and fills in its users.
   *
   * @param dataElement the identifier of the data element in the requested idScheme
   * @param json the data value's JSON object
   * @param eventCreatedBy the {@code createdByUserInfo} of the event
   */
  @Nonnull
  public static EventDataValue fromJson(
      @Nonnull String dataElement,
      @Nonnull JsonObject json,
      @CheckForNull UserInfoSnapshot eventCreatedBy) {
    EventDataValue value = new EventDataValue();
    value.setDataElement(dataElement);
    value.setValue(json.getString("value").string(""));
    value.setProvidedElsewhere(json.getBoolean("providedElsewhere").booleanValue(false));
    value.setCreated(DateUtils.parseDate(json.getString("created").string("")));
    value.setCreatedByUserInfo(userInfo(json.getObject("createdByUserInfo")));
    value.setLastUpdated(DateUtils.parseDate(json.getString("lastUpdated").string("")));
    value.setLastUpdatedByUserInfo(userInfo(json.getObject("lastUpdatedByUserInfo")));
    fillUserInfo(value, eventCreatedBy);
    return value;
  }

  @CheckForNull
  private static UserInfoSnapshot userInfo(@Nonnull JsonObject json) {
    if (json.isUndefined() || json.isNull()) {
      return null;
    }
    return UserInfoSnapshot.of(
        json.getNumber("id").number(0L).longValue(),
        json.getString("code").string(null),
        json.getString("uid").string(null),
        json.getString("username").string(null),
        json.getString("firstName").string(null),
        json.getString("surname").string(null));
  }

  /**
   * Fills in the users of data values as read from the {@code eventdatavalues} JSON: derived users
   * are set, and users marked as unknown become {@code null}.
   *
   * @param values data values as read from the JSON, changed in place
   * @param eventCreatedBy the {@code createdByUserInfo} of the event
   */
  public static void fillUserInfo(
      @Nonnull Set<EventDataValue> values, @CheckForNull UserInfoSnapshot eventCreatedBy) {
    for (EventDataValue value : values) {
      fillUserInfo(value, eventCreatedBy);
    }
  }

  private static void fillUserInfo(EventDataValue value, UserInfoSnapshot eventCreatedBy) {
    UserInfoSnapshot createdBy =
        value.getCreatedByUserInfo() == null ? eventCreatedBy : value.getCreatedByUserInfo();
    UserInfoSnapshot lastUpdatedBy =
        value.getLastUpdatedByUserInfo() == null ? createdBy : value.getLastUpdatedByUserInfo();
    value.setCreatedByUserInfo(known(createdBy));
    value.setLastUpdatedByUserInfo(known(lastUpdatedBy));
  }

  @CheckForNull
  private static UserInfoSnapshot known(@CheckForNull UserInfoSnapshot user) {
    return user == null || user.getUid() == null ? null : user;
  }

  /**
   * Returns true if both snapshots are of the same user with the same names. A snapshot without a
   * uid is an unknown user, which is never the same as another.
   */
  static boolean sameUser(@CheckForNull UserInfoSnapshot a, @CheckForNull UserInfoSnapshot b) {
    return a != null && a.getUid() != null && a.equals(b);
  }
}
