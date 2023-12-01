package org.hisp.dhis.merge;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IndicatorTypeMergeTest extends ApiTest {

  private RestApiActions indicatorTypeApiActions;
  private LoginActions loginActions;

  @BeforeEach
  public void before() {
    loginActions = new LoginActions();
    indicatorTypeApiActions = new RestApiActions("indicatorTypes");

    loginActions.loginAsSuperUser();
  }

  @Test
  @DisplayName("Invalid source UID format results in error message")
  void testInvalidSourceUid() {
    ApiResponse response =
        indicatorTypeApiActions
            .post("merge", getMergeBody("invalid", "ValidUid001", "ValidUid002", false))
            .validateStatus(400);

    response
        .validate()
        .statusCode(400)
        .body("httpStatus", equalTo("Bad Request"))
        .body("status", equalTo("ERROR"))
        .body(
            "message",
            equalTo(
                "JSON parse error: Cannot construct instance of `org.hisp.dhis.common.UID`, problem: UID must be an alphanumeric string of 11 characters starting with a letter."));
  }

  @Test
  @DisplayName("Invalid target UID format results in error message")
  void testInvalidTargetUid() {
    ApiResponse response =
        indicatorTypeApiActions
            .post("merge", getMergeBody("ValidUid002", "ValidUid001", "invalid", false))
            .validateStatus(400);

    response
        .validate()
        .statusCode(400)
        .body("httpStatus", equalTo("Bad Request"))
        .body("status", equalTo("ERROR"))
        .body(
            "message",
            equalTo(
                "JSON parse error: Cannot construct instance of `org.hisp.dhis.common.UID`, problem: UID must be an alphanumeric string of 11 characters starting with a letter."));
  }

  @Test
  @DisplayName(
      "Valid merge request is processed, successful response received and sources not deleted")
  void testValidMergeKeepSources() {
    ApiResponse response =
        indicatorTypeApiActions
            .post("merge", getMergeBody("indType0001", "indType0002", "indType0003", false))
            .validateStatus(200);

    response
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("response.message", equalTo("INDICATOR_TYPE merge complete"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("INDICATOR_TYPE"))
        .body("response.mergeReport.sourcesDeleted", empty());

    indicatorTypeApiActions.get("indType0001").validateStatus(200);
    indicatorTypeApiActions.get("indType0002").validateStatus(200);
    indicatorTypeApiActions.get("indType0003").validateStatus(200);
  }

  // todo create new indicators & types for each test

  @Test
  @DisplayName("Valid merge request is processed, successful response received and sources deleted")
  void testValidMergeDeleteSources() {
    ApiResponse response =
        indicatorTypeApiActions
            .post("merge", getMergeBody("indType0001", "indType0002", "indType0003", true))
            .validateStatus(200);

    response
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("response.message", equalTo("INDICATOR_TYPE merge complete"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("INDICATOR_TYPE"))
        .body("response.mergeReport.sourcesDeleted", contains("indType0001", "indType0002"));

    indicatorTypeApiActions.get("indType0001").validateStatus(404);
    indicatorTypeApiActions.get("indType0002").validateStatus(404);
    indicatorTypeApiActions.get("indType0003").validateStatus(200);
  }

  private JsonObject getMergeBody(
      String source1, String source2, String target, boolean deleteSources) {
    JsonObject json = new JsonObject();
    JsonArray array = new JsonArray();
    array.add(source1);
    array.add(source2);
    json.add("sources", array);
    json.addProperty("target", target);
    json.addProperty("deleteSources", deleteSources);
    return json;
  }
}
