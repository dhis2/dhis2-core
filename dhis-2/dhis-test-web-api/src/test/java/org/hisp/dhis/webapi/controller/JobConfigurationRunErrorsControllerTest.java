/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static java.time.Duration.ofSeconds;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.security.Authorities.F_JOB_LOG_READ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNodeType;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.scheduling.JobRunErrors;
import org.hisp.dhis.scheduling.JobStatus;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonJobConfiguration;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the job run error result API.
 *
 * @author Jan Bernitt
 */
class JobConfigurationRunErrorsControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private DbmsManager dbmsManager;

  private String jobId;

  @BeforeEach
  void setUp() throws InterruptedException {
    dbmsManager.emptyTable("jobconfiguration");
    jobId = createAndRunImportWithErrors();
    switchToNewUser("special-admin", F_JOB_LOG_READ.toString());
  }

  @Test
  void testGetJobRunErrors_List() {
    JsonList<JobRunErrors> list =
        GET("/jobConfigurations/errors").content().asList(JobRunErrors.class);

    assertEquals(1, list.size());
    JobRunErrors job = list.get(0);
    assertEquals(jobId, job.id());
    assertEquals(JobType.METADATA_IMPORT, job._type());
    assertEquals("ErrorCode", job.codes());
    assertEquals(1, job.errors().size());
    JobRunErrors.JobRunError error = job.errors().get(0);
    assertEquals("E4000", error.code());
    assertEquals(List.of("openingDate"), error.args());
    assertEquals("Missing required property `openingDate`", error.message());
  }

  @Test
  void testGetJobRunErrors_ListFilterUser() {
    // note that the superuser created the job with errors that is tested with
    JsonArray list = GET("/jobConfigurations/errors?user={user}", getAdminUid()).content();
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

  @Test
  void testGetJobRunErrors_RequireAuthority() {
    switchToNewUser("guest");

    assertStatus(HttpStatus.FORBIDDEN, GET("/jobConfigurations/errors"));
    assertStatus(HttpStatus.FORBIDDEN, GET("/jobConfigurations/{uid}/errors", jobId));
    assertStatus(HttpStatus.FORBIDDEN, GET("/jobConfigurations/{uid}/progress/errors", jobId));
    assertStatus(HttpStatus.FORBIDDEN, GET("/jobConfigurations/{uid}/progress", jobId));
  }

  @Test
  void testGetJobRunErrors_ListIncludeInput() throws InterruptedException {
    // language=JSON
    String json =
        """
      {
          "trackedEntities": [
              {
                  "trackedEntity":"sHH8mh1Fn0z",
                  "trackedEntityType": "nEenWmSyUEp",
                  "orgUnit": "DiszpKrYNg7"
              }
          ]
      }
      """;
    JsonWebMessage msg =
        POST("/tracker?async=true", json).content(HttpStatus.OK).as(JsonWebMessage.class);
    String jobId = msg.getString("response.id").string();
    waitUntilJobIsComplete(jobId);

    JsonArray errors = GET("/jobConfigurations/errors?includeInput=true").content();
    assertEquals(2, errors.size());
    JsonObject trackerImportError =
        errors.asList(JsonObject.class).stream()
            .filter(obj -> obj.getString("id").string().equals(jobId))
            .findFirst()
            .orElseThrow();

    // language=JSON
    String expected =
        """
      {"trackedEntities":[{"trackedEntity":"sHH8mh1Fn0z","trackedEntityType":{"idScheme":"UID","identifier":"nEenWmSyUEp"},"orgUnit":{"idScheme":"UID","identifier":"DiszpKrYNg7"},"inactive":false,"potentialDuplicate":false,"attributes":[]}],"enrollments":[],"events":[],"relationships":[]}""";
    assertEquals(expected, trackerImportError.getObject("input").node().getDeclaration());
  }

  private String createAndRunImportWithErrors() throws InterruptedException {
    String json = "{'organisationUnits':[{'name':'My Unit', 'shortName':'OU1'}]}";
    JsonWebMessage msg =
        POST("/metadata?async=true", json).content(HttpStatus.OK).as(JsonWebMessage.class);
    String jobId = msg.getString("response.id").string();

    waitUntilJobIsComplete(jobId);
    return jobId;
  }

  private void waitUntilJobIsComplete(String jobId) throws InterruptedException {
    BooleanSupplier jobCompleted =
        () -> isDone(GET("/jobConfigurations/{id}/gist?fields=id,jobStatus", jobId).content());
    assertTrue(await(ofSeconds(10), jobCompleted), "import did not run");
  }

  private static boolean await(Duration timeout, BooleanSupplier test) throws InterruptedException {
    while (!timeout.isNegative() && !test.getAsBoolean()) {
      Thread.sleep(20);
      timeout = timeout.minusMillis(20);
    }
    if (!timeout.isNegative()) return true;
    return test.getAsBoolean();
  }

  private static boolean isDone(JsonMixed config) {
    JsonJobConfiguration c = config.as(JsonJobConfiguration.class);
    return c.getJobStatus() == JobStatus.COMPLETED || c.getJobStatus() == JobStatus.DISABLED;
  }
}
