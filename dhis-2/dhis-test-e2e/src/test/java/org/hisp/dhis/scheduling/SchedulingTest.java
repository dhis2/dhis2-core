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
package org.hisp.dhis.scheduling;

import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.SystemActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SchedulingTest extends ApiTest {

  private MetadataActions metadataActions;
  private RestApiActions jobConfigActions;
  private SystemActions systemActions;

  @BeforeAll
  public void beforeAll() {
    metadataActions = new MetadataActions();
    systemActions = new SystemActions();
    jobConfigActions = new RestApiActions("jobConfigurations");
  }

  @AfterAll
  public void afterAllDelete() {
    metadataActions
        .importMetadata(
            new File("src/test/resources/metadata/adex-metadata.json"),
            "async=false&importMode=DELETE")
        .validateStatus(200);
  }

  @Test
  @DisplayName("Agg Data Exchange job runs without errors")
  void aggDataExchangeJobRunsWithoutErrorsTest() {
    // given an agg data exchange job is set up
    metadataActions
        .importMetadata(new File("src/test/resources/metadata/adex-metadata.json"), "async=false")
        .validateStatus(200);

    String jobConfig =
        """
                      {
                          "name": "test-dx-job-2",
                          "jobType": "AGGREGATE_DATA_EXCHANGE",
                          "cronExpression": "2 1 * ? * *",
                          "jobParameters": {
                              "dataExchangeIds": [
                                  "R9Urc25BSio"
                              ]
                          }
                      }
                      """;
    String jobId = jobConfigActions.post(jobConfig).validateStatus(201).extractUid();

    // when executing it manually
    ApiResponse runJobResponse = jobConfigActions.post("/" + jobId + "/execute", "null");

    // the job may already have been picked for execution so we check if it's already running or has
    // been accepted for execution
    if (runJobResponse.statusCode() == 200
        || runJobResponse.extractString("message").contains("Job is already running")) {
      // then it should complete without errors
      ApiResponse apiResponse =
          systemActions.waitUntilTaskCompleted("AGGREGATE_DATA_EXCHANGE", jobId, 24);
      apiResponse
          .validate()
          .body("level[0]", equalTo("INFO"))
          .body("message[0]", equalTo(""))
          .body("completed[0]", equalTo(true));
    } else {
      Assertions.fail("Job execution failed for agg data exchange");
    }
  }
}
