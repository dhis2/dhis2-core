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
package org.hisp.dhis.merge;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IndicatorTypeMergeTest extends ApiTest {

  private RestApiActions indicatorTypeApiActions;
  private RestApiActions indicatorApiActions;
  private LoginActions loginActions;
  private UserActions userActions;

  @BeforeEach
  public void before() {
    loginActions = new LoginActions();
    indicatorTypeApiActions = new RestApiActions("indicatorTypes");
    indicatorApiActions = new RestApiActions("indicators");
    userActions = new UserActions();
    loginActions.loginAsSuperUser();
  }

  @Test
  @DisplayName("Invalid source UID format results in error message")
  void testInvalidSourceUid() {
    // when a merge request with an invalid source UID format is submitted
    ApiResponse response =
        indicatorTypeApiActions
            .post("merge", getMergeBody("invalid", "ValidUid001", "ValidUid002", false))
            .validateStatus(400);

    // then an error message is received advising of the UID constraints
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
    // when a merge request with an invalid UID format is submitted
    ApiResponse response =
        indicatorTypeApiActions
            .post("merge", getMergeBody("ValidUid002", "ValidUid001", "invalid", false))
            .validateStatus(400);

    // then an error message is received advising of the UID constraints
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
    // given 3 indicators and 3 indicator types exist
    // indicator types
    String indTypeUid1 =
        indicatorTypeApiActions
            .post(createIndicatorType("A", 98, true))
            .validateStatus(201)
            .extractUid();
    String indTypeUid2 =
        indicatorTypeApiActions
            .post(createIndicatorType("B", 99, false))
            .validateStatus(201)
            .extractUid();
    String indTypeUid3 =
        indicatorTypeApiActions
            .post(createIndicatorType("C", 100, true))
            .validateStatus(201)
            .extractUid();

    // indicators referencing the indicator types
    String i1 =
        indicatorApiActions
            .post(createIndicator("Ind1", indTypeUid1))
            .validateStatus(201)
            .extractUid();
    String i2 =
        indicatorApiActions
            .post(createIndicator("Ind2", indTypeUid2))
            .validateStatus(201)
            .extractUid();
    String i3 =
        indicatorApiActions
            .post(createIndicator("Ind3", indTypeUid3))
            .validateStatus(201)
            .extractUid();

    // when an indicator type merge request is submitted, keeping sources
    ApiResponse response =
        indicatorTypeApiActions
            .post("merge", getMergeBody(indTypeUid1, indTypeUid2, indTypeUid3, false))
            .validateStatus(200);

    // then a successful response is received and no sources are deleted
    response
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("response.mergeReport.message", equalTo("INDICATOR_TYPE merge complete"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("INDICATOR_TYPE"))
        .body("response.mergeReport.sourcesDeleted", empty());

    // and sources & target exist
    indicatorTypeApiActions.get(indTypeUid1).validateStatus(200);
    indicatorTypeApiActions.get(indTypeUid2).validateStatus(200);
    indicatorTypeApiActions.get(indTypeUid3).validateStatus(200);

    // and all indicators now have target indicator type reference
    indicatorApiActions
        .get(i1)
        .validate()
        .statusCode(200)
        .body("indicatorType.id", equalTo(indTypeUid3));

    indicatorApiActions
        .get(i2)
        .validate()
        .statusCode(200)
        .body("indicatorType.id", equalTo(indTypeUid3));

    indicatorApiActions
        .get(i3)
        .validate()
        .statusCode(200)
        .body("indicatorType.id", equalTo(indTypeUid3));
  }

  @Test
  @DisplayName(
      "Valid merge request is processed, successful response received and sources are deleted")
  void testValidMergeDeleteSources() {
    // given 3 indicators and 3 indicator types exist
    // indicator types
    String indTypeUid1 =
        indicatorTypeApiActions
            .post(createIndicatorType("D", 98, true))
            .validateStatus(201)
            .extractUid();
    String indTypeUid2 =
        indicatorTypeApiActions
            .post(createIndicatorType("E", 99, false))
            .validateStatus(201)
            .extractUid();
    String indTypeUid3 =
        indicatorTypeApiActions
            .post(createIndicatorType("F", 100, true))
            .validateStatus(201)
            .extractUid();

    // indicators
    String i1 =
        indicatorApiActions
            .post(createIndicator("Ind4", indTypeUid1))
            .validateStatus(201)
            .extractUid();
    String i2 =
        indicatorApiActions
            .post(createIndicator("Ind5", indTypeUid2))
            .validateStatus(201)
            .extractUid();
    String i3 =
        indicatorApiActions
            .post(createIndicator("Ind6", indTypeUid3))
            .validateStatus(201)
            .extractUid();

    // when an indicator type merge request is submitted, deleting sources
    ApiResponse response =
        indicatorTypeApiActions
            .post("merge", getMergeBody(indTypeUid1, indTypeUid2, indTypeUid3, true))
            .validateStatus(200);

    // then a successful response is received and sources are deleted
    response
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("response.mergeReport.message", equalTo("INDICATOR_TYPE merge complete"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("INDICATOR_TYPE"))
        .body("response.mergeReport.sourcesDeleted", hasItems(indTypeUid1, indTypeUid2));

    // and sources are deleted & target exists
    indicatorTypeApiActions.get(indTypeUid1).validateStatus(404);
    indicatorTypeApiActions.get(indTypeUid2).validateStatus(404);
    indicatorTypeApiActions.get(indTypeUid3).validateStatus(200);

    // and all indicators now reference target indicator type
    indicatorApiActions
        .get(i1)
        .validate()
        .statusCode(200)
        .body("indicatorType.id", equalTo(indTypeUid3));

    indicatorApiActions
        .get(i2)
        .validate()
        .statusCode(200)
        .body("indicatorType.id", equalTo(indTypeUid3));

    indicatorApiActions
        .get(i3)
        .validate()
        .statusCode(200)
        .body("indicatorType.id", equalTo(indTypeUid3));
  }

  @Test
  @DisplayName("Invalid merge request with no sources results in failure response")
  void testInvalidMergeNoSources() {
    // given a target indicator type exists
    String indTypeUid1 =
        indicatorTypeApiActions
            .post(createIndicatorType("G", 98, true))
            .validateStatus(201)
            .extractUid();

    // when a merge request with no sources is sent
    ApiResponse response =
        indicatorTypeApiActions
            .post("merge", getMergeBodyNoSources(indTypeUid1, true))
            .validateStatus(400);

    // then a response with an error is received
    response
        .validate()
        .statusCode(400)
        .body("httpStatus", equalTo("Bad Request"))
        .body("status", equalTo("ERROR"))
        .body("response.mergeReport.message", equalTo("INDICATOR_TYPE merge has errors"))
        .body(
            "response.mergeReport.mergeErrors[0].message",
            equalTo("At least one source indicator type must be specified"))
        .body("response.mergeReport.mergeErrors[0].errorCode", equalTo("E1530"))
        .body("response.mergeReport.mergeType", equalTo("INDICATOR_TYPE"))
        .body("response.mergeReport.sourcesDeleted", empty());
  }

  @Test
  @DisplayName("Invalid merge request with no target results in failure response")
  void testInvalidMergeNoSourcesTarget() {
    // given a valid source indicator type exists
    String indTypeUid1 =
        indicatorTypeApiActions
            .post(createIndicatorType("H", 98, true))
            .validateStatus(201)
            .extractUid();

    // when a merge request with no target is sent
    ApiResponse response =
        indicatorTypeApiActions
            .post("merge", getMergeBodyNoTarget(indTypeUid1, true))
            .validateStatus(400);

    // then a response with an error is received
    response
        .validate()
        .statusCode(400)
        .body("httpStatus", equalTo("Bad Request"))
        .body("status", equalTo("ERROR"))
        .body("response.mergeReport.message", equalTo("INDICATOR_TYPE merge has errors"))
        .body(
            "response.mergeReport.mergeErrors[0].message",
            equalTo("Target indicator type must be specified"))
        .body("response.mergeReport.mergeErrors[0].errorCode", equalTo("E1531"))
        .body("response.mergeReport.mergeType", equalTo("INDICATOR_TYPE"))
        .body("response.mergeReport.sourcesDeleted", empty());
  }

  @Test
  @DisplayName("Invalid merge request has errors when target is in sources")
  void testInvalidMergeTargetInSources() {
    // given indicator types exist
    String indTypeUid1 =
        indicatorTypeApiActions
            .post(createIndicatorType("J", 98, true))
            .validateStatus(201)
            .extractUid();
    String indTypeUid2 =
        indicatorTypeApiActions
            .post(createIndicatorType("K", 99, false))
            .validateStatus(201)
            .extractUid();
    String indTypeUid3 =
        indicatorTypeApiActions
            .post(createIndicatorType("L", 100, true))
            .validateStatus(201)
            .extractUid();

    // when a merge request has the target contained in the sources
    ApiResponse response =
        indicatorTypeApiActions
            .post("merge", getMergeBody(indTypeUid1, indTypeUid3, indTypeUid3, true))
            .validateStatus(400);

    // then a response with an error is received
    response
        .validate()
        .statusCode(400)
        .body("httpStatus", equalTo("Bad Request"))
        .body("status", equalTo("ERROR"))
        .body("response.mergeReport.message", equalTo("INDICATOR_TYPE merge has errors"))
        .body(
            "response.mergeReport.mergeErrors[0].message",
            equalTo("Target indicator type cannot be a source indicator type"))
        .body("response.mergeReport.mergeErrors[0].errorCode", equalTo("E1532"))
        .body("response.mergeReport.mergeType", equalTo("INDICATOR_TYPE"))
        .body("response.mergeReport.sourcesDeleted", empty());
  }

  @Test
  @DisplayName("Invalid merge request has multiple errors when sources and target don't exist")
  void testInvalidMergeTargetAndSourcesDontExist() {
    // when a merge request is sent with invalid source and target UIDs
    ApiResponse response =
        indicatorTypeApiActions
            .post("merge", getMergeBody("invalidUid1", "invalidUid2", "invalidUid3", false))
            .validateStatus(400);

    // then a response with 3 errors is received (2 x sources, 1 x target)
    response
        .validate()
        .statusCode(400)
        .body("httpStatus", equalTo("Bad Request"))
        .body("status", equalTo("ERROR"))
        .body("response.mergeReport.message", equalTo("INDICATOR_TYPE merge has errors"))
        .body("response.mergeReport.mergeErrors.size()", equalTo(3))
        .body("response.mergeReport.mergeType", equalTo("INDICATOR_TYPE"))
        .body("response.mergeReport.sourcesDeleted", empty());
  }

  @Test
  @DisplayName("Invalid merge request no auth")
  void testInvalidMergeNoAuth() {
    // given a user with no indicator type merge auth
    userActions.addUser("basicUser", "passTest1!");
    loginActions.loginAsUser("basicUser", "passTest1!");

    // when they submit a merge request
    ApiResponse response =
        indicatorTypeApiActions
            .post("merge", getMergeBody("invalidUid1", "invalidUid2", "invalidUid3", false))
            .validateStatus(403);

    // then they get an auth error
    response
        .validate()
        .statusCode(403)
        .body("httpStatus", equalTo("Forbidden"))
        .body("status", equalTo("ERROR"))
        .body("message", equalTo("Access is denied"));
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

  private String getMergeBodyNoSources(String target, boolean deleteSources) {
    return """
     {
        "sources": [],
        "target": "%s",
        "number": "%b"
     }
    """
        .formatted(target, deleteSources);
  }

  private String getMergeBodyNoTarget(String source, boolean deleteSources) {
    return """
     {
        "sources": ["%s"],
        "target": null,
        "number": "%b"
     }
    """
        .formatted(source, deleteSources);
  }

  private String createIndicatorType(String name, int factor, boolean isNumber) {
    return """
     {
        "name": "test indicator type %s",
        "factor": "%d",
        "number": "%b"
     }
    """
        .formatted(name, factor, isNumber);
  }

  private String createIndicator(String name, String indicatorType) {
    return """
     {
        "name": "test indicator %s",
        "shortName": "test short %s",
        "dimensionItemType": "INDICATOR",
        "numerator": "#{fbfJHSPpUQD}",
        "denominator": "#{h0xKKjijTdI}",
        "indicatorType": {
            "id": "%s"
        }
     }
    """
        .formatted(name, name, indicatorType);
  }
}
