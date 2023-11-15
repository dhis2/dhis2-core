/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BooleanSupplier;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNodeType;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.scheduling.JobStatus;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.json.domain.JsonJobConfiguration;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the job run error result API.
 *
 * @author Jan Bernitt
 */
public class JobConfigurationRunErrorsControllerTest extends DhisControllerIntegrationTest {

  private String jobId;

  @BeforeEach
  void setUp() throws InterruptedException {
    jobId = createAndRunImportWithErrors();
  }

  @Test
  void testGetJobRunErrors_List() {
    JsonArray list = GET("/jobConfigurations/errors").content();

    assertEquals(1, list.size());
    JsonObject job = list.getObject(0);
    assertEquals(jobId, job.getString("id").string());
    assertEquals(1, job.getArray("errors").size());
  }

  @Test
  void testGetJobRunErrors_ListFilterUser() {
    JsonArray list =
        GET("/jobConfigurations/errors?user={user}", getCurrentUser().getUid()).content();
    assertEquals(1, list.size());
    assertEquals(0, GET("/jobConfigurations/errors?user=abcde123456").content().size());
  }

  @Test
  void testGetJobRunErrors_ListFilterFrom() {
    assertEquals(1, GET("/jobConfigurations/errors?from=2023-01-01").content().size());
    assertEquals(0, GET("/jobConfigurations/errors?from=2033-01-01").content().size());
  }

  @Test
  void testGetJobRunErrors_ListFilterTo() {
    assertEquals(1, GET("/jobConfigurations/errors?to=2033-01-01").content().size());
    assertEquals(0, GET("/jobConfigurations/errors?to=2023-01-01").content().size());
  }

  @Test
  void testGetJobRunErrors_ListFilterCode() {
    assertEquals(1, GET("/jobConfigurations/errors?code=E4000").content().size());
    assertEquals(0, GET("/jobConfigurations/errors?code=E5000").content().size());
  }

  @Test
  void testGetJobRunErrors_ListFilterType() {
    assertEquals(1, GET("/jobConfigurations/errors?type=METADATA_IMPORT").content().size());
    assertEquals(0, GET("/jobConfigurations/errors?type=DATA_INTEGRITY").content().size());
  }

  @Test
  void testGetJobRunErrors_Object() {
    JsonObject job = GET("/jobConfigurations/{uid}/errors", jobId).content();
    assertEquals(jobId, job.getString("id").string());
    assertEquals("METADATA_IMPORT", job.getString("type").string());
    assertTrue(job.has("created", "executed", "finished", "user", "filesize", "filetype"));
    assertEquals(1, job.getArray("errors").size());
  }

  @Test
  void testGetJobRunErrors_ObjectProgressErrors() {
    JsonArray errors = GET("/jobConfigurations/{uid}/progress/errors", jobId).content();
    assertEquals(JsonNodeType.ARRAY, errors.node().getType());
    assertEquals(1, errors.size());
  }

  private String createAndRunImportWithErrors() throws InterruptedException {
    JsonWebMessage message =
        POST(
                "/metadata?async=true",
                "{'organisationUnits':[{'name':'My Unit', 'shortName':'OU1'}]}")
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    String jobId = message.getString("response.id").string();

    BooleanSupplier jobCompleted =
        () -> isDone(GET("/jobConfigurations/{id}/gist?fields=id,jobStatus", jobId).content());
    assertTrue(await(ofSeconds(10), jobCompleted), "import did not run");
    return jobId;
  }

  private static boolean isDone(JsonMixed config) {
    JsonJobConfiguration c = config.as(JsonJobConfiguration.class);
    return c.getJobStatus() == JobStatus.COMPLETED || c.getJobStatus() == JobStatus.DISABLED;
  }
}
