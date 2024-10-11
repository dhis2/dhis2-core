/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.scheduling.SchedulingController}.
 *
 * @author Jan Bernitt
 */
@Transactional
class JobSchedulingControllerTest extends PostgresControllerIntegrationTestBase {

  @Test
  void testGetRunningProgressTypesOnly() {
    JsonArray types = GET("/scheduling/running/types").content();
    assertEquals(0, types.size());
  }

  @Test
  void testGetRunningProgressTypes() {
    JsonMap<JsonArray> types = GET("/scheduling/running").content().asMap(JsonArray.class);
    assertEquals(0, types.size());
  }

  @Test
  void testGetCompletedProgressTypes() {
    JsonMap<JsonArray> types = GET("/scheduling/completed").content().asMap(JsonArray.class);
    assertEquals(0, types.size());
  }

  @Test
  void testGetRunningProgress() {
    JsonObject progress = GET("/scheduling/running/DATA_INTEGRITY").content();
    assertTrue(progress.isObject());
    assertTrue(progress.isEmpty());
  }

  @Test
  void testGetCompletedProgress() {
    JsonObject progress = GET("/scheduling/completed/DATA_INTEGRITY").content();
    assertTrue(progress.isObject());
    assertTrue(progress.isEmpty());
  }
}
