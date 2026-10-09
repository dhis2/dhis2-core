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
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.helpers.EnvUtils;
import org.hisp.dhis.test.e2e.actions.ResourceTableActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.SystemActions;
import org.hisp.dhis.test.e2e.actions.aggregate.DataValueSetActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Checks that a continuous analytics table update ({@code lastYears=0}) picks up aggregate data
 * values and tracker events that are added, updated and deleted after the full analytics export.
 *
 * <p>Unlike the other analytics tests, this test changes data and runs the analytics table export
 * again. It is ordered last, so no other test sees the change. It writes an aggregate data value
 * for a data element, org unit and period that have no data value in the database and that no other
 * test queries, and an event with its own UID in an existing enrollment.
 *
 * <p>The test uses existing metadata and runs no analytics table update other than continuous ones.
 * A continuous update does not regenerate the resource tables, so new metadata would be missing
 * from them, and an update that does regenerate them would also pick up the metadata other tests
 * import after the full export, giving the tables columns that the existing ones lack. These are
 * known limitations of continuous updates, not what this test checks. For the same reason, each
 * continuous update covers only the table type under test, and uses the outlier setting of the full
 * export, so the tables keep the same columns.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
@Order(Integer.MAX_VALUE)
@EnabledIf(
    value = "supportsContinuousAnalytics",
    disabledReason = "Continuous analytics is only supported on Postgres and Doris")
public class ContinuousAnalyticsTest extends AnalyticsApiTest {
  /** Louse Borne Typhus - Relapsing fever (Deaths &lt; 5 yrs), default category combo. */
  private static final String DATA_ELEMENT = "NpJtsQkMTm3";

  /** Mortality &lt; 5 years. */
  private static final String DATA_SET = "pBOMPrpg1QX";

  /** Ngelehun CHC. */
  private static final String ORG_UNIT = "DiszpKrYNg8";

  private static final String PERIOD = "202201";

  /** TB program. */
  private static final String PROGRAM = "ur1Edk5Oe2n";

  /** Sputum smear microscopy test, a repeatable stage. */
  private static final String PROGRAM_STAGE = "jdRD35YwbRH";

  /** TB smear microscopy number of specimen. */
  private static final String EVENT_DATA_ELEMENT = "yLIPuJHRgey";

  /** An active enrollment in the TB program, at Mogbongisseh MCHP. */
  private static final String ENROLLMENT = "GwALejH6kxb";

  private static final String EVENT_ORG_UNIT = "DJr17K6RWzO";

  private static final String EVENT = "caE2eEvt001";

  private static final String SKIP_ALL_BUT_AGGREGATE =
      "skipEvents=true&skipEnrollment=true&skipTrackedEntities=true&skipOrgUnitOwnership=true"
          + "&skipValidationResult=true";

  private static final String SKIP_ALL_BUT_EVENTS =
      "skipAggregate=true&skipEnrollment=true&skipTrackedEntities=true&skipOrgUnitOwnership=true"
          + "&skipValidationResult=true";

  private static final long ANALYTICS_TIMEOUT_SECONDS = TimeUnit.MINUTES.toSeconds(10);

  private RestApiActions analyticsActions;

  private DataValueSetActions dataValueSetActions;

  private RestApiActions trackerActions;

  @BeforeAll
  public void setup() {
    analyticsActions = new RestApiActions("analytics");
    dataValueSetActions = new DataValueSetActions();
    trackerActions = new RestApiActions("tracker");
  }

  static boolean supportsContinuousAnalytics() {
    String dataSource = EnvUtils.getDataSource();
    return dataSource.equalsIgnoreCase("postgres") || dataSource.equalsIgnoreCase("doris");
  }

  @Test
  @Timeout(value = 15, unit = TimeUnit.MINUTES)
  void continuousUpdatePicksUpAddedUpdatedAndDeletedDataValues() {
    getAnalytics().validate().statusCode(200).body("rows", empty());

    // Added after the full export
    importDataValue("11", "CREATE_AND_UPDATE");
    runContinuousUpdate(SKIP_ALL_BUT_AGGREGATE);
    validateRow(getAnalytics(), List.of(DATA_ELEMENT, PERIOD, "11"));

    // Updated after the previous continuous update
    importDataValue("17", "CREATE_AND_UPDATE");
    runContinuousUpdate(SKIP_ALL_BUT_AGGREGATE);
    ApiResponse updated = getAnalytics();
    updated.validate().statusCode(200).body("rows", hasSize(equalTo(1)));
    validateRow(updated, List.of(DATA_ELEMENT, PERIOD, "17"));

    // Deleted after the previous continuous update
    importDataValue("17", "DELETE");
    runContinuousUpdate(SKIP_ALL_BUT_AGGREGATE);
    getAnalytics().validate().statusCode(200).body("rows", empty());
  }

  @Test
  @Timeout(value = 15, unit = TimeUnit.MINUTES)
  void continuousUpdatePicksUpAddedUpdatedAndDeletedEvents() {
    getEventAnalytics().validate().statusCode(200).body("rows", empty());

    // Added after the full export
    importEvent("11", "CREATE_AND_UPDATE");
    runContinuousUpdate(SKIP_ALL_BUT_EVENTS);
    validateEventRow("11");

    // Updated after the previous continuous update
    importEvent("17", "CREATE_AND_UPDATE");
    runContinuousUpdate(SKIP_ALL_BUT_EVENTS);
    validateEventRow("17");

    // Deleted after the previous continuous update
    importEvent("17", "DELETE");
    runContinuousUpdate(SKIP_ALL_BUT_EVENTS);
    getEventAnalytics().validate().statusCode(200).body("rows", empty());
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

  private void importEvent(String value, String importStrategy) {
    JsonObject dataValue = new JsonObject();
    dataValue.addProperty("dataElement", EVENT_DATA_ELEMENT);
    dataValue.addProperty("value", value);

    JsonArray dataValues = new JsonArray();
    dataValues.add(dataValue);

    JsonObject event = new JsonObject();
    event.addProperty("event", EVENT);
    event.addProperty("program", PROGRAM);
    event.addProperty("programStage", PROGRAM_STAGE);
    event.addProperty("enrollment", ENROLLMENT);
    event.addProperty("orgUnit", EVENT_ORG_UNIT);
    event.addProperty("occurredAt", "2022-01-15");
    event.addProperty("status", "ACTIVE");
    event.add("dataValues", dataValues);

    JsonArray events = new JsonArray();
    events.add(event);

    JsonObject payload = new JsonObject();
    payload.add("events", events);

    trackerActions
        .post(
            payload,
            new QueryParamsBuilder().add("async", "false").add("importStrategy", importStrategy))
        .validate()
        .statusCode(200)
        .body("status", equalTo("OK"));
  }

  /**
   * Runs a continuous analytics table update, skipping the given table types, and checks that no
   * stage of it failed.
   */
  private void runContinuousUpdate(String skipTableTypes) {
    String taskId =
        new ResourceTableActions()
            .post("/analytics?" + continuousUpdateParams(skipTableTypes), new JsonObject())
            .validateStatus(200)
            .extractString("response.id");

    ApiResponse task =
        new SystemActions()
            .waitUntilTaskCompleted("ANALYTICS_TABLE", taskId, ANALYTICS_TIMEOUT_SECONDS);

    List<String> errors = task.extractList("findAll { it.level == 'ERROR' }.message", String.class);
    assertEquals(List.of(), errors, "Continuous analytics update reported errors");
  }

  /**
   * Returns the parameters of a continuous update that skips the given table types, with the
   * outlier setting of the full export, see the class comment.
   */
  private static String continuousUpdateParams(String skipTableTypes) {
    String exportParams = System.getProperty("analytics.api.query.params", "");
    boolean skipOutliers = exportParams.contains("skipOutliers=true");

    return "lastYears=0&" + skipTableTypes + "&skipOutliers=" + skipOutliers;
  }

  private ApiResponse getEventAnalytics() {
    return analyticsActions.get(
        "/events/query/" + PROGRAM,
        new QueryParamsBuilder()
            .add("stage", PROGRAM_STAGE)
            .add("dimension", "pe:" + PERIOD)
            .add("dimension", "ou:" + EVENT_ORG_UNIT)
            .add("dimension", PROGRAM_STAGE + "." + EVENT_DATA_ELEMENT)
            .add("skipMeta", "true"));
  }

  /** Checks that the event is the only row, with the given value, which is the last column. */
  private void validateEventRow(String value) {
    getEventAnalytics()
        .validate()
        .statusCode(200)
        .body("rows", hasSize(equalTo(1)))
        .body("rows[0][0]", equalTo(EVENT))
        .body("rows[0][-1]", equalTo(value));
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
