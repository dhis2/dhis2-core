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
 * values, tracker events and single events that are added, updated and deleted after the full
 * analytics export.
 *
 * <p>Unlike the other analytics tests, this test changes data and runs the analytics table export
 * again. It is ordered last, so no other test sees the change. It writes an aggregate data value
 * for a data element, org unit and period that have no data value in the database and that no other
 * test queries, and events with their own UIDs: one in an existing enrollment of a tracker program,
 * and one in a single event program.
 *
 * <p>The test uses existing metadata and runs no analytics table update other than continuous ones.
 * A continuous update does not regenerate the resource tables, so new metadata would be missing
 * from them, and an update that does regenerate them would also pick up the metadata other tests
 * import after the full export, giving the tables columns that the existing ones lack. These are
 * known limitations of continuous updates, not what this test checks. For the same reason, each
 * continuous update covers only the aggregate and event tables, and uses the outlier setting of the
 * full export, so the tables keep the same columns.
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

  /**
   * In an existing active enrollment of the TB program, at Mogbongisseh MCHP. Sputum smear
   * microscopy test is a repeatable stage, and the data element is TB smear microscopy number of
   * specimen.
   */
  private static final TestEvent TRACKER_EVENT =
      new TestEvent(
          "caE2eEvt001", "ur1Edk5Oe2n", "jdRD35YwbRH", "yLIPuJHRgey", "GwALejH6kxb", "DJr17K6RWzO");

  /**
   * In the Antenatal care visit single event program, at Ngelehun CHC. The data element is WHOMCH
   * Hemoglobin value. The stage has no compulsory data elements.
   */
  private static final TestEvent SINGLE_EVENT =
      new TestEvent("caE2eSev001", "lxAQ7Zs9VYR", "dBwrot7S420", "vANAXwtLwcT", null, ORG_UNIT);

  /**
   * The table types a continuous update skips, so it only covers the aggregate and event tables,
   * see the class comment.
   */
  private static final String SKIP_TABLE_TYPES =
      "skipEnrollment=true&skipTrackedEntities=true&skipOrgUnitOwnership=true"
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
  void continuousUpdatePicksUpAddedUpdatedAndDeletedData() {
    getAnalytics().validate().statusCode(200).body("rows", empty());
    getEventAnalytics(TRACKER_EVENT).validate().statusCode(200).body("rows", empty());
    getEventAnalytics(SINGLE_EVENT).validate().statusCode(200).body("rows", empty());

    // Added after the full export
    importDataValue("11", "CREATE_AND_UPDATE");
    importEvent(TRACKER_EVENT, "11", "CREATE_AND_UPDATE");
    importEvent(SINGLE_EVENT, "11", "CREATE_AND_UPDATE");
    runContinuousUpdate();
    validateDataValueRow("11");
    validateEventRow(TRACKER_EVENT, "11");
    validateEventRow(SINGLE_EVENT, "11");

    // Updated after the previous continuous update
    importDataValue("17", "CREATE_AND_UPDATE");
    importEvent(TRACKER_EVENT, "17", "CREATE_AND_UPDATE");
    importEvent(SINGLE_EVENT, "17", "CREATE_AND_UPDATE");
    runContinuousUpdate();
    validateDataValueRow("17");
    validateEventRow(TRACKER_EVENT, "17");
    validateEventRow(SINGLE_EVENT, "17");

    // Deleted after the previous continuous update
    importDataValue("17", "DELETE");
    importEvent(TRACKER_EVENT, "17", "DELETE");
    importEvent(SINGLE_EVENT, "17", "DELETE");
    runContinuousUpdate();
    getAnalytics().validate().statusCode(200).body("rows", empty());
    getEventAnalytics(TRACKER_EVENT).validate().statusCode(200).body("rows", empty());
    getEventAnalytics(SINGLE_EVENT).validate().statusCode(200).body("rows", empty());
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

  private void importEvent(TestEvent testEvent, String value, String importStrategy) {
    JsonObject dataValue = new JsonObject();
    dataValue.addProperty("dataElement", testEvent.dataElement());
    dataValue.addProperty("value", value);

    JsonArray dataValues = new JsonArray();
    dataValues.add(dataValue);

    JsonObject event = new JsonObject();
    event.addProperty("event", testEvent.uid());
    event.addProperty("program", testEvent.program());
    event.addProperty("programStage", testEvent.programStage());
    event.addProperty("enrollment", testEvent.enrollment());
    event.addProperty("orgUnit", testEvent.orgUnit());
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

  /** Runs a continuous analytics table update and checks that no stage of it failed. */
  private void runContinuousUpdate() {
    String taskId =
        new ResourceTableActions()
            .post("/analytics?" + continuousUpdateParams(), new JsonObject())
            .validateStatus(200)
            .extractString("response.id");

    ApiResponse task =
        new SystemActions()
            .waitUntilTaskCompleted("ANALYTICS_TABLE", taskId, ANALYTICS_TIMEOUT_SECONDS);

    List<String> errors = task.extractList("findAll { it.level == 'ERROR' }.message", String.class);
    assertEquals(List.of(), errors, "Continuous analytics update reported errors");
  }

  /**
   * Returns the parameters of a continuous update of the aggregate and event tables, with the
   * outlier setting of the full export, see the class comment.
   */
  private static String continuousUpdateParams() {
    String exportParams = System.getProperty("analytics.api.query.params", "");
    boolean skipOutliers = exportParams.contains("skipOutliers=true");

    return "lastYears=0&" + SKIP_TABLE_TYPES + "&skipOutliers=" + skipOutliers;
  }

  private ApiResponse getEventAnalytics(TestEvent event) {
    return analyticsActions.get(
        "/events/query/" + event.program(),
        new QueryParamsBuilder()
            .add("stage", event.programStage())
            .add("dimension", "pe:" + PERIOD)
            .add("dimension", "ou:" + event.orgUnit())
            .add("dimension", event.programStage() + "." + event.dataElement())
            .add("skipMeta", "true"));
  }

  /** Checks that the event is the only row, with the given value, which is the last column. */
  private void validateEventRow(TestEvent event, String value) {
    getEventAnalytics(event)
        .validate()
        .statusCode(200)
        .body("rows", hasSize(equalTo(1)))
        .body("rows[0][0]", equalTo(event.uid()))
        .body("rows[0][-1]", equalTo(value));
  }

  /** Checks that the data value is the only row, with the given value. */
  private void validateDataValueRow(String value) {
    ApiResponse response = getAnalytics();
    response.validate().statusCode(200).body("rows", hasSize(equalTo(1)));
    validateRow(response, List.of(DATA_ELEMENT, PERIOD, value));
  }

  private ApiResponse getAnalytics() {
    return analyticsActions.get(
        new QueryParamsBuilder()
            .add("dimension", "dx:" + DATA_ELEMENT)
            .add("dimension", "pe:" + PERIOD)
            .add("filter", "ou:" + ORG_UNIT)
            .add("skipMeta", "true"));
  }

  /**
   * An event the test writes, with the data element it sets.
   *
   * @param enrollment the enrollment, or null for a single event.
   */
  private record TestEvent(
      String uid,
      String program,
      String programStage,
      String dataElement,
      String enrollment,
      String orgUnit) {}
}
