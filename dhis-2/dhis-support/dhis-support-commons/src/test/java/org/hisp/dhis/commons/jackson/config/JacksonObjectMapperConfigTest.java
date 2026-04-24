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
package org.hisp.dhis.commons.jackson.config;

import static java.time.ZoneId.systemDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class JacksonObjectMapperConfigTest {

  private final ObjectMapper jsonMapper = JacksonObjectMapperConfig.staticJsonMapper();

  @Test
  void testIsoDateSupport() {
    assertParsedAsDate(DateUtils.parseDate("2019"), "2019");
    assertParsedAsDate(DateUtils.parseDate("2019-01"), "2019-01");
    assertParsedAsDate(DateUtils.parseDate("2019-01-01"), "2019-01-01");
    assertParsedAsDate(DateUtils.parseDate("2019-01-01T11:55"), "2019-01-01T11:55");
    assertParsedAsDate(DateUtils.parseDate("2019-01-01T11:55:01.444Z"), "2019-01-01T11:55:01.444Z");
    assertParsedAsDate(DateUtils.parseDate("2019-01-01T11:55:01.4444"), "2019-01-01T11:55:01.4444");
    Date expected = DateUtils.parseDate("2019-01-01T11:55:01.4444Z");
    assertParsedAsDate(expected, "2019-01-01T11:55:01.4444Z");
    assertParsedAsDate(
        expected,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
            expected.toInstant().atZone(systemDefault()).toLocalDateTime()));
  }

  @Test
  void testUnixEpochTimestamp() {
    assertParsedAsDate(new Date(1575118800000L), "1575118800000");
    assertParsedAsDate(new Date(1575118800000L), 1575118800000L);
  }

  @Test
  void testNullDate() {
    assertParsedAsDate(null, null);
  }

  @Test
  void testNaNDate() {
    assertNotParsedAsDate("NaN");
  }

  @Test
  void testRubbishDate() {
    assertNotParsedAsDate("NaN NaN NaN");
  }

  @Test
  void testUnclearDate() {
    assertNotParsedAsDate("999999999");
  }

  // DHIS2-8582
  @Test
  void testSerializerUserWithUser() throws JsonProcessingException {
    User user = new User();
    user.setAutoFields();
    user.setCreatedBy(user);
    user.setLastUpdatedBy(user);
    String payload = jsonMapper.writeValueAsString(user);
    User testUser = jsonMapper.readValue(payload, User.class);
    assertNotNull(user.getCreatedBy());
    assertNotNull(user.getLastUpdatedBy());
    assertEquals(user.getUid(), testUser.getUid());
    assertEquals(user.getUid(), user.getCreatedBy().getUid());
    assertEquals(user.getUid(), user.getLastUpdatedBy().getUid());
  }

  // DHIS2-21252
  @Test
  void testLargeMetadataStringExceedsDefaultJacksonLimit() {
    // Jackson 2.15+ defaults StreamReadConstraints.getMaxStringLength() to 20_000_000.
    // A metadata snapshot exceeding this limit throws StreamConstraintsException on an
    // unconfigured mapper — this was the root cause of the DHIS2-21252 bug that prevented
    // large metadata version snapshots from being deserialised during sync.
    ObjectMapper unconfiguredMapper = new ObjectMapper();
    String json = "{\"metadata\":\"" + "x".repeat(20_100_000) + "\"}";

    assertThrows(
        StreamConstraintsException.class, () -> unconfiguredMapper.readValue(json, Map.class));
  }

  // DHIS2-21252
  @Test
  void testConfiguredMapperDeserializesStringLargerThanDefaultJacksonLimit() throws IOException {
    // JacksonObjectMapperConfig raises StreamReadConstraints.maxStringLength to 150 MB,
    // fixing the StreamConstraintsException thrown when metadata snapshots exceed the 20 MB
    // Jackson default.
    String largeContent = "x".repeat(20_100_000);
    String json = "{\"metadata\":\"" + largeContent + "\"}";

    Map<String, String> result =
        jsonMapper.readValue(json, new TypeReference<Map<String, String>>() {});
    assertEquals(largeContent, result.get("metadata"));
  }

  private String createDateString(String str) {
    if (str == null) {
      return "{\"date\": null }";
    }
    return String.format("{\"date\": \"%s\"}", str);
  }

  private String createUnixEpochTest(long ts) {
    return String.format("{\"date\": %d}", ts);
  }

  private static class DateMapTypeReference extends TypeReference<Map<String, Date>> {}

  private void assertNotParsedAsDate(String value) {
    Exception ex = assertThrows(IOException.class, () -> parseAsDate(value));
    assertEquals(
        "Unexpected IOException (of type java.io.IOException): Invalid date format '"
            + value
            + "', only ISO format or UNIX Epoch timestamp is supported.",
        ex.getMessage());
  }

  private void assertParsedAsDate(Date expected, Object value) {
    try {
      assertEquals(expected, parseAsDate(value).get("date"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  private Map<String, Date> parseAsDate(Object value) throws Exception {
    String json;
    if (value instanceof Long) {
      json = createUnixEpochTest((Long) value);
    } else if (value instanceof String || value == null) {
      json = createDateString((String) value);
    } else {
      throw new UnsupportedOperationException("Value type not supported: " + value);
    }
    return jsonMapper.readValue(json, new DateMapTypeReference());
  }
}
