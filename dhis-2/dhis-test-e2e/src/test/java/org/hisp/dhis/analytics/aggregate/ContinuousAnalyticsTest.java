/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.analytics.aggregate;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.helpers.EnvUtils;
import org.hisp.dhis.test.e2e.actions.ResourceTableActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.SystemActions;
import org.hisp.dhis.test.e2e.actions.aggregate.DataValueSetActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Checks that a continuous analytics table update ({@code lastYears=0}) picks up data values that
 * are added, updated and deleted after the full analytics export.
 *
 * <p>Unlike the other analytics tests, this test changes data and runs the analytics table export
 * again. It is ordered last, and it only writes data for its own data element and data set, which
 * it imports after the full export, so no other test sees them.
 *
 * <p>A continuous update does not regenerate the resource tables, so the test regenerates them
 * after importing its metadata. Otherwise the new data element is missing from them, and its data
 * values are left out of the analytics tables.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
@Order(Integer.MAX_VALUE)
@EnabledIf(
    value = "supportsContinuousAnalytics",
    disabledReason = "Continuous analytics is only supported on Postgres and Doris")
public class ContinuousAnalyticsTest extends AnalyticsApiTest {
  private static final String METADATA_FILE =
      "src/test/resources/analytics/continuous-analytics-metadata.json";

  private static final String DATA_ELEMENT = "caE2eDe0001";

  private static final String DATA_SET = "caE2eDs0001";

  private static final String ORG_UNIT = "DiszpKrYNg8";

  private static final String PERIOD = "202201";

  private static final long ANALYTICS_TIMEOUT_SECONDS = TimeUnit.MINUTES.toSeconds(10);

  private RestApiActions analyticsActions;

  private DataValueSetActions dataValueSetActions;

  @BeforeAll
  public void setup() {
    analyticsActions = new RestApiActions("analytics");
    dataValueSetActions = new DataValueSetActions();

    new MetadataActions()
        .importMetadata(new File(METADATA_FILE), "async=false")
        .validate()
        .body("status", equalTo("OK"))
        .body("response.stats.ignored", equalTo(0));

    // Skips every table type, so only the resource tables are regenerated, and replicated to the
    // analytics database where one is used.
    runAnalyticsTableUpdate(
        "skipAggregate=true&skipEvents=true&skipEnrollment=true&skipTrackedEntities=true"
            + "&skipOrgUnitOwnership=true&skipValidationResult=true&skipOutliers=true");
  }

  static boolean supportsContinuousAnalytics() {
    String dataSource = EnvUtils.getDataSource();
    return dataSource.equalsIgnoreCase("postgres") || dataSource.equalsIgnoreCase("doris");
  }

  @Test
  @Timeout(value = 15, unit = TimeUnit.MINUTES)
  void continuousUpdatePicksUpAddedUpdatedAndDeletedDataValues() {
    // Added after the full export
    importDataValue("11", "CREATE_AND_UPDATE");
    runContinuousUpdate();
    validateRow(getAnalytics(), List.of(DATA_ELEMENT, PERIOD, "11"));

    // Updated after the previous continuous update
    importDataValue("17", "CREATE_AND_UPDATE");
    runContinuousUpdate();
    ApiResponse updated = getAnalytics();
    updated.validate().statusCode(200).body("rows", hasSize(equalTo(1)));
    validateRow(updated, List.of(DATA_ELEMENT, PERIOD, "17"));

    // Deleted after the previous continuous update
    importDataValue("17", "DELETE");
    runContinuousUpdate();
    getAnalytics().validate().statusCode(200).body("rows", empty());
  }

  private void importDataValue(String value, String importStrategy) {
    JsonObject dataValue = new JsonObject();
    dataValue.addProperty("dataElement", DATA_ELEMENT);
    dataValue.addProperty("period", PERIOD);
    dataValue.addProperty("orgUnit", ORG_UNIT);
    dataValue.addProperty("value", value);

    JsonArray dataValues = new JsonArray();
    dataValues.add(dataValue);

    JsonObject dataValueSet = new JsonObject();
    dataValueSet.addProperty("dataSet", DATA_SET);
    dataValueSet.add("dataValues", dataValues);

    dataValueSetActions
        .post(
            dataValueSet,
            new QueryParamsBuilder().add("async", "false").add("importStrategy", importStrategy))
        .validate()
        .statusCode(200)
        .body("status", equalTo("OK"));
  }

  private void runContinuousUpdate() {
    runAnalyticsTableUpdate("lastYears=0");
  }

  /** Runs an analytics table update and checks that no stage of it failed. */
  private void runAnalyticsTableUpdate(String queryParams) {
    String taskId =
        new ResourceTableActions()
            .post("/analytics?" + queryParams, new JsonObject())
            .validateStatus(200)
            .extractString("response.id");

    ApiResponse task =
        new SystemActions()
            .waitUntilTaskCompleted("ANALYTICS_TABLE", taskId, ANALYTICS_TIMEOUT_SECONDS);

    List<String> errors = task.extractList("findAll { it.level == 'ERROR' }.message", String.class);
    assertEquals(List.of(), errors, "Analytics table update reported errors");
  }

  private ApiResponse getAnalytics() {
    return analyticsActions.get(
        new QueryParamsBuilder()
            .add("dimension", "dx:" + DATA_ELEMENT)
            .add("dimension", "pe:" + PERIOD)
            .add("filter", "ou:" + ORG_UNIT)
            .add("skipMeta", "true"));
  }
}
