/*
 * Copyright (c) 2004-2024, University of Oslo
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
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.UserActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoryOptionMergeTest extends ApiTest {

  private RestApiActions categoryOptionApiActions;
  private UserActions userActions;
  private LoginActions loginActions;
  private String sourceUid1;
  private String sourceUid2;
  private String targetUid;

  @BeforeAll
  public void before() {
    userActions = new UserActions();
    loginActions = new LoginActions();
    categoryOptionApiActions = new RestApiActions("categoryOptions");
    loginActions.loginAsSuperUser();

    // add user with required merge auth
    userActions.addUserFull(
        "user", "auth", "userWithMergeAuth", "Test1234!", "F_CATEGORY_OPTION_MERGE");
  }

  @BeforeEach
  public void setup() {
    loginActions.loginAsSuperUser();
  }

  @Test
  @DisplayName(
      "Valid CategoryOption merge completes successfully with all source CategoryOption refs replaced with target CategoryOption")
  void validDataElementMergeTest() {
    // given
    sourceUid1 = setupCategoryOption("A");
    sourceUid2 = setupCategoryOption("B");
    targetUid = setupCategoryOption("C");

    // login as user with merge auth
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // when a category option request is submitted, deleting sources
    ApiResponse response =
        categoryOptionApiActions
            .post("merge", getMergeBody(sourceUid1, sourceUid2, targetUid, true, "LAST_UPDATED"))
            .validateStatus(200);

    // then a successful response is received and sources are deleted
    response
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("response.mergeReport.message", equalTo("CATEGORY_OPTION merge complete"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("CATEGORY_OPTION"))
        .body("response.mergeReport.sourcesDeleted", hasItems(sourceUid1, sourceUid2));

    // and all the following source category option references have been handled appropriately
    // and sources are deleted & target exists
    categoryOptionApiActions.get(sourceUid1).validateStatus(404);
    categoryOptionApiActions.get(sourceUid2).validateStatus(404);
    categoryOptionApiActions.get(targetUid).validateStatus(200);
  }

  @Test
  @DisplayName("CategoryOption merge fails when user has not got the required authority")
  void testDataElementMergeNoRequiredAuth() {
    userActions.addUserFull("basic", "User", "basicUser", "Test1234!", "NO_AUTH");
    loginActions.loginAsUser("basicUser", "Test1234!");

    // when
    ApiResponse response =
        categoryOptionApiActions
            .post("merge", getMergeBody(sourceUid1, sourceUid2, targetUid, true, "LAST_UPDATED"))
            .validateStatus(403);

    // then
    response
        .validate()
        .statusCode(403)
        .body("httpStatus", equalTo("Forbidden"))
        .body("status", equalTo("ERROR"))
        .body(
            "message",
            equalTo("Access is denied, requires one Authority from [F_CATEGORY_OPTION_MERGE]"));
  }

  private String setupCategoryOption(String uniqueChar) {
    return categoryOptionApiActions
        .post(createCategoryOption("source 1" + uniqueChar))
        .validateStatus(201)
        .extractUid();
  }

  private String createCategoryOption(String name) {
    return """
      {
         "name": "%s",
         "shortName": "%s"
       }
    """
        .formatted(name, name);
  }

  private JsonObject getMergeBody(
      String source1, String source2, String target, boolean deleteSources, String mergeStrategy) {
    JsonObject json = new JsonObject();
    JsonArray sources = new JsonArray();
    sources.add(source1);
    sources.add(source2);
    json.add("sources", sources);
    json.addProperty("target", target);
    json.addProperty("deleteSources", deleteSources);
    json.addProperty("dataMergeStrategy", mergeStrategy);
    return json;
  }
}
