package org.hisp.dhis.scheduling;

import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.SystemActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
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
    jobConfigActions.post("/" + jobId + "/execute", "null").validateStatus(200);

    // then it should complete without errors
    ApiResponse apiResponse =
        systemActions.waitUntilTaskCompleted("AGGREGATE_DATA_EXCHANGE", jobId, 24);
    apiResponse
        .validate()
        .body("level[0]", equalTo("INFO"))
        .body("message[0]", equalTo(""))
        .body("completed[0]", equalTo(true));
  }
}
