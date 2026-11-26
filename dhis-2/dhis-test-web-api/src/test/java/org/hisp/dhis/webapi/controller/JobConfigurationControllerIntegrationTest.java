/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static java.lang.String.format;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonJobConfiguration;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.scheduling.JobConfigurationController} that
 * cannot be tested with H2.
 *
 * @author Jan Bernitt
 */
class JobConfigurationControllerIntegrationTest extends PostgresControllerIntegrationTestBase {

  @Autowired private DbmsManager dbmsManager;

  @AfterEach
  void tearDown() {
    dbmsManager.emptyTable("jobconfiguration");
  }

  @Test
  void testRevert() {
    String json =
        """
        {
        "name": "test",
        "jobType": "DATA_INTEGRITY",
        "cronExpression": "0 0 12 ? * MON-FRI"
        }
        """;
    String jobId = assertStatus(HttpStatus.CREATED, POST("/jobConfigurations", json));
    switchToNewUser("no-auth");
    assertStatus(HttpStatus.FORBIDDEN, POST("/jobConfigurations/" + jobId + "/revert"));
    switchToAdminUser();
    assertStatus(HttpStatus.CONFLICT, POST("/jobConfigurations/" + jobId + "/revert"));
  }

  @Test
  @DisplayName("AGGREGATE_DATA_EXCHANGE job should have an executedBy value when job set up")
  void aggregateDataExchangeJobHasExecutedByValueTest() {

    // given a agg data exchange job is set up
    @Language("json5")
    String job =
        """
        {
             "name": "adex-job-1",
             "jobType": "AGGREGATE_DATA_EXCHANGE",
             "cronExpression": "2 1 * ? * *",
             "jobParameters": {
                 "dataExchangeIds": [
                     "RandomUid86"
                 ]
             }
         }
        """;
    String jobId = assertStatus(HttpStatus.CREATED, POST("/jobConfigurations", job));

    // when retrieving the job config
    HttpResponse get = GET("/jobConfigurations/" + jobId);
    assertEquals(HttpStatus.OK, get.status());
    String executedBy = get.content().getString("executedBy").string();

    // then the executedBy value should be that of the job creator
    assertEquals("M5zQapPyTZI", executedBy);
  }

  @Test
  @DisplayName(
      "AGGREGATE_DATA_EXCHANGE job should have an executedBy value when job is updated without that value")
  void aggregateDataExchangeJobHasExecutedByValueOnUpdateTest() {
    // given an agg data exchange job is set up
    @Language("json5")
    String job =
        """
        {
             "name": "adex-job-2",
             "jobType": "AGGREGATE_DATA_EXCHANGE",
             "cronExpression": "2 2 * ? * *",
             "jobParameters": {
                 "dataExchangeIds": [
                     "RandomUid86"
                 ]
             }
         }
        """;
    String jobId = assertStatus(HttpStatus.CREATED, POST("/jobConfigurations", job));

    HttpResponse get = GET("/jobConfigurations/" + jobId);
    String executedBy = get.content().getString("executedBy").string();

    // and the executedBy value is set
    assertEquals("M5zQapPyTZI", executedBy);

    // when that job is updated (e.g. new cron expression)
    @Language("json5")
    String jobUpdated =
        """
        {
             "name": "adex-job-2",
             "jobType": "AGGREGATE_DATA_EXCHANGE",
             "cronExpression": "12 12 * ? * *",
             "jobParameters": {
                 "dataExchangeIds": [
                     "RandomUid86"
                 ]
             }
         }
        """;
    assertStatus(HttpStatus.OK, PUT("/jobConfigurations/" + jobId, jobUpdated));

    // then the executedBy value should still be that of the job creator
    HttpResponse get2 = GET("/jobConfigurations/" + jobId);
    String executedBy2 = get2.content().getString("executedBy").string();

    assertEquals("M5zQapPyTZI", executedBy2);
  }

  @Test
  @DisplayName("META_DATA_SYNC job should have an executedBy value when job set up")
  void metadataSyncJobHasExecutedByValueTest() {

    // given a metadata sync job is set up
    @Language("json5")
    String job =
        """
        {
             "name": "metadata-sync-job-1",
             "jobType": "META_DATA_SYNC",
             "cronExpression": "0 0 22 ? * *"
         }
        """;
    String jobId = assertStatus(HttpStatus.CREATED, POST("/jobConfigurations", job));

    // when retrieving the job config
    HttpResponse get = GET("/jobConfigurations/" + jobId);
    assertEquals(HttpStatus.OK, get.status());
    String executedBy = get.content().getString("executedBy").string();

    // then the executedBy value should be that of the job creator
    assertEquals("M5zQapPyTZI", executedBy);
  }

  @Test
  @DisplayName(
      "META_DATA_SYNC job should have an executedBy value when job is updated without that value")
  void metadataSyncJobHasExecutedByValueOnUpdateTest() {
    // given a metadata sync job is set up
    @Language("json5")
    String job =
        """
        {
             "name": "metadata-sync-job-2",
             "jobType": "META_DATA_SYNC",
             "cronExpression": "0 0 21 ? * *"
         }
        """;
    String jobId = assertStatus(HttpStatus.CREATED, POST("/jobConfigurations", job));

    HttpResponse get = GET("/jobConfigurations/" + jobId);
    String executedBy = get.content().getString("executedBy").string();

    // and the executedBy value is set
    assertEquals("M5zQapPyTZI", executedBy);

    // when that job is updated (e.g. new cron expression)
    @Language("json5")
    String jobUpdated =
        """
        {
             "name": "metadata-sync-job-2",
             "jobType": "META_DATA_SYNC",
             "cronExpression": "0 0 20 ? * *"
         }
        """;
    assertStatus(HttpStatus.OK, PUT("/jobConfigurations/" + jobId, jobUpdated));

    // then the executedBy value should still be that of the job creator
    HttpResponse get2 = GET("/jobConfigurations/" + jobId);
    String executedBy2 = get2.content().getString("executedBy").string();

    assertEquals("M5zQapPyTZI", executedBy2);
  }

  @Test
  void testEnableRequiresAuthority() {
    String json =
        """
        {
        "name": "test-enable",
        "jobType": "DATA_INTEGRITY",
        "cronExpression": "0 0 13 ? * MON-FRI"
        }
        """;
    String jobId = assertStatus(HttpStatus.CREATED, POST("/jobConfigurations", json));
    switchToNewUser("guest");
    assertStatus(HttpStatus.FORBIDDEN, POST("/jobConfigurations/" + jobId + "/enable"));
    switchToNewUser("scheduling-admin", "F_SCHEDULING_ADMIN");
    assertStatus(HttpStatus.NO_CONTENT, POST("/jobConfigurations/" + jobId + "/enable"));
  }

  @Test
  void testDisableRequiresAuthority() {
    String json =
        """
        {
        "name": "test-disable",
        "jobType": "DATA_INTEGRITY",
        "cronExpression": "0 0 14 ? * MON-FRI"
        }
        """;
    String jobId = assertStatus(HttpStatus.CREATED, POST("/jobConfigurations", json));
    switchToNewUser("guest");
    assertStatus(HttpStatus.FORBIDDEN, POST("/jobConfigurations/" + jobId + "/disable"));
    switchToNewUser("scheduling-admin", "F_SCHEDULING_ADMIN");
    assertStatus(HttpStatus.NO_CONTENT, POST("/jobConfigurations/" + jobId + "/disable"));
  }

  @Test
  void testEnableRequiresSchedule_Update() {
    String job1Json =
        """
        {
        "name": "test-disable",
        "jobType": "DATA_INTEGRITY",
        "cronExpression": "0 0 15 ? * MON-FRI"
        }
        """;
    String job1Id = assertStatus(HttpStatus.CREATED, POST("/jobConfigurations", job1Json));
    JsonJobConfiguration job1 =
        GET("/jobConfigurations/" + job1Id).content().as(JsonJobConfiguration.class);
    assertTrue(job1.isEnabled(), "newly created config should be enabled");
    assertNotNull(job1.getCronExpression());

    // create another job
    String job2Json =
        "{'name':'b','jobType':'DATA_INTEGRITY','cronExpression':'0 0 19 ? * MON-FRI'}";
    String job2Id = assertStatus(HttpStatus.CREATED, POST("/jobConfigurations", job2Json));
    // make both part of a queue
    String queueJson =
        format("{'cronExpression':'0 0 1 ? * *','sequence':['%s','%s']}", job1Id, job2Id);
    assertStatus(HttpStatus.CREATED, POST("/scheduler/queues/testQueue", queueJson));
    // delete the queue
    assertStatus(HttpStatus.NO_CONTENT, DELETE("/scheduler/queues/testQueue"));

    job1 = GET("/jobConfigurations/" + job1Id).content().as(JsonJobConfiguration.class);
    assertNull(job1.getCronExpression());

    assertStatus(HttpStatus.CONFLICT, POST("/jobConfigurations/" + job1Id + "/enable"));
  }
}
