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
package org.hisp.dhis.metadata.metadata_import;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.SystemActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.jsontree.JsonArray;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests that the metadata import is running as a continuous import.
 *
 * <p>This means several small (fast) imports should run directly after another and not be executed
 * with a gap of 20 seconds for the loop cycle time of the scheduler as each type otherwise can only
 * run one job per cycle.
 *
 * @author Jan Bernitt
 */
class ContinuousMetadataImportTest extends ApiTest {

  private MetadataActions metadataActions;
  private SystemActions systemActions;
  private RestApiActions jobConfigurationActions;

  @BeforeAll
  public void beforeAll() {
    metadataActions = new MetadataActions();
    systemActions = new SystemActions();
    jobConfigurationActions = new RestApiActions("jobConfigurations");

    new LoginActions().loginAsSuperUser();
  }

  @Test
  void testRunContinuousImportJobs() throws Exception {
    JsonObject object =
        new FileReaderUtils()
            .readJsonAndGenerateData(new File("src/test/resources/metadata/uniqueMetadata.json"));
    // setup: import metadata so that we have references and can clean up
    QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
    queryParamsBuilder.addAll(
        "async=false",
        "importReportMode=DEBUG",
        "importStrategy=CREATE_AND_UPDATE",
        "atomicMode=NONE");
    ApiResponse response = metadataActions.post(object, queryParamsBuilder);

    // send 5 async request to check later
    queryParamsBuilder.add("async=true");
    List<String> taskIds = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      response = metadataActions.post(object, queryParamsBuilder);
      String taskId = response.extractString("response.id");
      assertNotNull(taskId, "Task id was not returned");
      taskIds.add(taskId);
    }

    for (String taskId : taskIds) {
      systemActions.waitUntilTaskCompleted("METADATA_IMPORT", taskId);
    }

    DateTimeFormatter timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    String urlTemplate =
        "/gist?headless=true&fields=id,lastFinished,lastExecuted&order=lastExecuted:asc&filter=id:in:[%s]";
    JsonArray jobConfigs =
        jobConfigurationActions
            .get(urlTemplate.formatted(String.join(",", taskIds)))
            .validateStatus(200)
            .getBodyAsJsonValue();

    LocalDateTime lastFinished = null;
    for (org.hisp.dhis.jsontree.JsonObject jobConfig :
        jobConfigs.asList(org.hisp.dhis.jsontree.JsonObject.class)) {
      LocalDateTime lastExecuted =
          LocalDateTime.parse(jobConfig.getString("lastExecuted").string(), timestamp);
      if (lastFinished != null) {
        long millisBetweenExecution =
            lastExecuted.toInstant(ZoneOffset.UTC).toEpochMilli()
                - lastFinished.toInstant(ZoneOffset.UTC).toEpochMilli();
        assertTrue(
            millisBetweenExecution < 20_000,
            "Time between execution should not be longer than 20 seconds (scheduler cycle time) by was: %d ms"
                .formatted(millisBetweenExecution));
      }
      lastFinished = LocalDateTime.parse(jobConfig.getString("lastFinished").string(), timestamp);
      ;
    }
  }
}
