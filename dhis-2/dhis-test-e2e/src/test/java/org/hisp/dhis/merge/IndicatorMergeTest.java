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

import static org.hamcrest.Matchers.equalTo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IndicatorMergeTest extends ApiTest {

  private RestApiActions indicatorApiActions;
  private LoginActions loginActions;
  private UserActions userActions;

  @BeforeAll
  public void before() {
    loginActions = new LoginActions();
    indicatorApiActions = new RestApiActions("indicators");
    userActions = new UserActions();
    loginActions.loginAsSuperUser();
  }

  @Test
  @DisplayName("Valid Indicator merge completes successfully")
  void testValidMerge() {
    // given indicators exist
    String sourceIndicator1 =
        indicatorApiActions.post(createIndicator("source1", )).validateStatus(201).extractUid();

    // when a merge request with valid source UIDs is submitted
    ApiResponse response =
        indicatorApiActions
            .post("merge", getMergeBody("invalid", "ValidUid001", "ValidUid002", false))
            .validateStatus(200);

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

  private String getMergeBodyNoTarget(String target, boolean deleteSources) {
    return """
     {
        "sources": ["%s"],
        "target": null,
        "number": "%b"
     }
    """
        .formatted(target, deleteSources);
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
