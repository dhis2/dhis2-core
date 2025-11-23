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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link WriteDateStdSerializer} and {@link WriteInstantStdSerializer}. */
class DateSerializersTest {

  private ObjectMapper objectMapper;
  private TimeZone defaultTimeZone;

  @BeforeEach
  void setUp() {
    // Save and set fixed timezone to make tests independent of system timezone
    defaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo")); // UTC+2
    objectMapper = JacksonObjectMapperConfig.staticJsonMapper();
  }

  @AfterEach
  void tearDown() {
    // Restore original timezone
    TimeZone.setDefault(defaultTimeZone);
  }

  @Test
  void testWriteDateStdSerializer() throws JsonProcessingException {
    Date date = new Date(1696338896789L);

    String json = objectMapper.writeValueAsString(date);

    assertEquals("\"2023-10-03T15:14:56.789\"", json);
  }

  @Test
  void testWriteDateStdSerializer_withNull() throws JsonProcessingException {
    Date date = null;

    String json = objectMapper.writeValueAsString(date);

    assertEquals("null", json);
  }

  @Test
  void testWriteInstantStdSerializer() throws JsonProcessingException {
    Instant instant = Instant.ofEpochMilli(1696338896789L);

    String json = objectMapper.writeValueAsString(instant);

    assertEquals("\"2023-10-03T15:14:56.789\"", json);
  }

  @Test
  void testWriteInstantStdSerializer_withNull() throws JsonProcessingException {
    Instant instant = null;

    String json = objectMapper.writeValueAsString(instant);

    assertEquals("null", json);
  }

  @Test
  void testDateAndInstantConsistency() throws JsonProcessingException {
    long epochMilli = 1696338896789L;
    Instant instant = Instant.ofEpochMilli(epochMilli);
    Date date = Date.from(instant);

    String instantJson = objectMapper.writeValueAsString(instant);
    String dateJson = objectMapper.writeValueAsString(date);

    assertEquals(dateJson, instantJson);
  }
}
