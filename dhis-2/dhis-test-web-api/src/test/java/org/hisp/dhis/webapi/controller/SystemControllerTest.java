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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link SystemController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
class SystemControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testGetTasksJson() {
    JsonObject tasks = GET("/system/tasks").content(HttpStatus.OK);
    assertTrue(tasks.isObject());
    tasks.values().forEach(m -> assertTrue(m.isObject(), m + " is not an object"));
  }

  @Test
  void testGetTasksExtendedJson() {
    JsonObject tasks = GET("/system/tasks/{jobType}", "META_DATA_SYNC").content(HttpStatus.OK);
    assertTrue(tasks.isObject());
    assertEquals(0, tasks.size());
  }

  @Test
  void testGetTaskJsonByUid() {
    JsonArray task =
        GET("/system/tasks/{jobType}/{jobId}", "META_DATA_SYNC", "a1234567890")
            .content(HttpStatus.OK);
    assertTrue(task.isArray());
    assertEquals(0, task.size());
  }

  @Test
  void testGetTaskSummaryExtendedJson() {
    JsonObject summary = GET("/system/taskSummaries/META_DATA_SYNC").content(HttpStatus.OK);
    assertTrue(summary.isObject());
    assertEquals(0, summary.size());
  }

  @Test
  void testGetTaskSummaryJson() {
    JsonObject summary =
        GET("/system/taskSummaries/META_DATA_SYNC/a1234567890").content(HttpStatus.OK);
    assertTrue(summary.isObject());
    assertEquals(0, summary.size());
  }

  @Test
  void testGetSystemInfo() {
    JsonObject info = GET("/system/info").content();
    // testing one sensitive and one non-sensitive property
    assertNotNull(info.getString("javaVersion").string());
    assertNotNull(info.getString("serverDate").string());
  }

  @Test
  void testGetSystemInfo_NonSuperUser() {
    switchToNewUser("guest");
    JsonObject info = GET("/system/info").content();
    // testing one sensitive and one non-sensitive property
    assertNull(info.getString("javaVersion").string());
    assertNotNull(info.getString("serverDate").string());
  }

  @Test
  void testGetFlags_NoDuplicates() {
    JsonArray flags = GET("/system/flags").content(HttpStatus.OK);
    assertTrue(flags.isArray());
    assertTrue(flags.size() > 0);

    Set<String> flagKeys = new HashSet<>();
    for (int i = 0; i < flags.size(); i++) {
      JsonObject flag = flags.getObject(i);
      String key = flag.getString("key").string();
      assertTrue(flagKeys.add(key), "Duplicate flag found: " + key);
    }
  }
}
