/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hisp.dhis.merge.IndicatorTypeMergeTest.createIndicator;
import static org.hisp.dhis.merge.IndicatorTypeMergeTest.createIndicatorType;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.UserActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IndicatorMergeTest extends ApiTest {

  private RestApiActions indicatorApiActions;
  private RestApiActions indicatorTypeApiActions;
  private RestApiActions formsActions;
  private RestApiActions dataSetActions;
  private RestApiActions indicatorGroupActions;
  private RestApiActions sectionActions;
  private RestApiActions configActions;
  private RestApiActions visualizationActions;
  private UserActions userActions;
  private LoginActions loginActions;
  private String sourceUid1;
  private String sourceUid2;
  private String targetUid;
  private String ind4WithIndRef;
  private String ind5WithIndRef;
  private String indWithoutSourceRef;
  private String indGroupWithSources;
  private String indGroupWithNoSources;
  private String datasetWithSources;
  private String datasetWithoutSources;
  private String sectionWithSources;
  private String sectionWithoutSources;
  private String formWithSources;
  private String formWithoutSources;
  private String visUid;

  @BeforeAll
  public void before() {
    userActions = new UserActions();
    loginActions = new LoginActions();
    indicatorApiActions = new RestApiActions("indicators");
    indicatorTypeApiActions = new RestApiActions("indicatorTypes");
    formsActions = new RestApiActions("dataEntryForms");
    dataSetActions = new RestApiActions("dataSets");
    indicatorGroupActions = new RestApiActions("indicatorGroups");
    sectionActions = new RestApiActions("sections");
    configActions = new RestApiActions("configuration");
    visualizationActions = new RestApiActions("visualizations");
    loginActions.loginAsSuperUser();

    // add user with required merge auth
    userActions.addUserFull(
        "user",
        "auth",
        "userWithMergeAuth",
        "Test1234!",
        "F_INDICATOR_MERGE",
        "F_INDICATOR_DELETE",
        "F_INDICATOR_PUBLIC_ADD");
  }

  @BeforeEach
  public void setup() {
    loginActions.loginAsSuperUser();
  }

  @Test
  @DisplayName(
      "Valid Indicator merge completes successfully with all indicator associations updated")
  void testValidIndicatorMerge() {
    // given some indicators exist and have references with many other metadata types
    setupIndicatorData();

    // login as user with merge auth
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // when an indicator type merge request is submitted, deleting sources
    ApiResponse response =
        indicatorApiActions
            .post("merge", getMergeBody(sourceUid1, sourceUid2, targetUid, true))
            .validateStatus(200);

    // then a successful response is received and sources are deleted
    response
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("response.mergeReport.message", equalTo("Indicator merge complete"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("Indicator"))
        .body("response.mergeReport.sourcesDeleted", hasItems(sourceUid1, sourceUid2));

    // and all the following source indicator references have been handled appropriately
    // and sources are deleted & target exists
    indicatorApiActions.get(sourceUid1).validateStatus(404);
    indicatorApiActions.get(sourceUid2).validateStatus(404);
    indicatorApiActions.get(targetUid).validateStatus(200);

    // and all groups now reference target indicator type
    // group has had indicators replaced
    indicatorGroupActions
        .get(indGroupWithSources)
        .validate()
        .statusCode(200)
        .body("indicators", hasItem(allOf(hasEntry("id", targetUid))))
        .body(
            "indicators",
            not(hasItem(allOf(hasEntry("id", sourceUid1), hasEntry("id", sourceUid2)))));

    // group has not had indicator replaced
    indicatorGroupActions
        .get(indGroupWithNoSources)
        .validate()
        .statusCode(200)
        .body("indicators", hasItem(hasEntry("id", ind4WithIndRef)))
        .body("indicators", hasItem(hasEntry("id", ind5WithIndRef)))
        .body("indicators", not(hasItem(hasEntry("id", targetUid))));

    // and all data sets are updated where appropriate
    // data set has had indicator replaced
    dataSetActions
        .get(datasetWithSources)
        .validate()
        .statusCode(200)
        .body("indicators", hasItem(allOf(hasEntry("id", targetUid))))
        .body(
            "indicators",
            not(hasItem(allOf(hasEntry("id", sourceUid1), hasEntry("id", sourceUid2)))));

    // data set has not had indicator replaced
    dataSetActions
        .get(datasetWithoutSources)
        .validate()
        .statusCode(200)
        .body("indicators", hasItem(hasEntry("id", ind4WithIndRef)))
        .body("indicators", hasItem(hasEntry("id", ind5WithIndRef)))
        .body("indicators", not(hasItem(hasEntry("id", targetUid))));

    // and all sections are updated where appropriate
    // section has had indicator replaced
    sectionActions
        .get(sectionWithSources)
        .validate()
        .statusCode(200)
        .body("indicators", hasItem(allOf(hasEntry("id", targetUid))))
        .body(
            "indicators",
            not(hasItem(allOf(hasEntry("id", sourceUid1), hasEntry("id", sourceUid2)))));

    // section has not had indicator replaced
    sectionActions
        .get(sectionWithoutSources)
        .validate()
        .statusCode(200)
        .body("indicators", hasItem(hasEntry("id", ind4WithIndRef)))
        .body("indicators", hasItem(hasEntry("id", ind5WithIndRef)))
        .body("indicators", not(hasItem(hasEntry("id", targetUid))));

    // and all forms are updated where appropriate
    // form has had indicator replaced
    formsActions
        .get(formWithSources)
        .validate()
        .statusCode(200)
        .body("htmlCode", containsString(targetUid))
        .body("htmlCode", not(containsString(sourceUid1)))
        .body("htmlCode", not(containsString(sourceUid2)));

    // form has not had indicator replaced
    formsActions
        .get(formWithoutSources)
        .validate()
        .statusCode(200)
        .body("htmlCode", not(containsString(sourceUid1)))
        .body("htmlCode", not(containsString(sourceUid2)))
        .body("htmlCode", containsString(targetUid))
        .body("htmlCode", containsString("Uid45678901"));

    // and all indicator numerator/denominator are updated where appropriate
    // indicator has been updated
    indicatorApiActions
        .get(ind4WithIndRef)
        .validate()
        .statusCode(200)
        .body("numerator", containsString(targetUid))
        .body("numerator", not(containsString(sourceUid1)))
        .body("denominator", containsString(targetUid))
        .body("denominator", not(containsString(sourceUid2)));

    // indicator has been partially updated
    indicatorApiActions
        .get(ind5WithIndRef)
        .validate()
        .statusCode(200)
        .body("numerator", containsString(targetUid))
        .body("denominator", containsString(targetUid))
        .body("denominator", not(containsString(sourceUid2)));

    // indicator has not been updated
    indicatorApiActions
        .get(indWithoutSourceRef)
        .validate()
        .statusCode(200)
        .body("numerator", containsString(targetUid))
        .body("denominator", containsString("Uid45678901"));

    // and the visualization data dimension items now reference the target indicator
    QueryParamsBuilder paramsBuilder2 =
        new QueryParamsBuilder().add("fields", "dataDimensionItems");
    ApiResponse dataItemsResponse2 =
        visualizationActions.get(visUid, paramsBuilder2).validateStatus(200);
    dataItemsResponse2
        .validate()
        .statusCode(200)
        .body("dataDimensionItems.size()", equalTo(2))
        .body("dataDimensionItems.indicator", hasItem(hasEntry("id", targetUid)))
        .body("dataDimensionItems.indicator", not(hasItem(hasEntry("id", sourceUid1))))
        .body("dataDimensionItems.indicator", not(hasItem(hasEntry("id", sourceUid2))));

    // and config indicator group has been updated
    configActions
        .get("infrastructuralIndicators")
        .validate()
        .statusCode(200)
        .body("indicators", hasItem(allOf(hasEntry("id", targetUid))))
        .body(
            "indicators",
            not(hasItem(allOf(hasEntry("id", sourceUid1), hasEntry("id", sourceUid2)))));

    // and visualization sorting is updated
    QueryParamsBuilder paramsBuilder3 = new QueryParamsBuilder().add("fields", "sorting");
    visualizationActions
        .get(visUid, paramsBuilder3)
        .validate()
        .statusCode(200)
        .body("sorting", hasItem(allOf(hasEntry("dimension", targetUid))))
        .body("sorting", not(hasItem(allOf(hasEntry("dimension", sourceUid1)))))
        .body("sorting", not(hasItem(allOf(hasEntry("dimension", sourceUid2)))));
  }

  @Test
  @DisplayName("Indicator merge fails when user has not got the required authority")
  void testIndicatorMergeNoRequiredAuth() {
    userActions.addUserFull("basic", "User", "basicUser", "Test1234!", "NO_AUTH");
    loginActions.loginAsUser("basicUser", "Test1234!");

    // when an indicator type merge request is submitted
    ApiResponse response =
        indicatorApiActions
            .post("merge", getMergeBody(sourceUid1, sourceUid2, targetUid, true))
            .validateStatus(403);

    // then a forbidden response is received
    response
        .validate()
        .statusCode(403)
        .body("httpStatus", equalTo("Forbidden"))
        .body("status", equalTo("ERROR"))
        .body(
            "message",
            equalTo("Access is denied, requires one Authority from [F_INDICATOR_MERGE]"));
  }

  @Test
  @DisplayName("User with ALL authority can attempt merge with invalid data")
  void testIndicatorMergeAllAuth() {
    userActions.addUserFull("all", "User", "allAuthUser", "Test1234!", "ALL");
    loginActions.loginAsUser("allAuthUser", "Test1234!");

    // when a user with ALL auth submits an indicator type merge request with invalid data
    ApiResponse response =
        indicatorApiActions
            .post("merge", getMergeBody(sourceUid1, sourceUid2, targetUid, true))
            .validateStatus(409);

    // then a conflict response is received (access denied response is not received, no auth issue)
    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("status", equalTo("WARNING"))
        .body(
            "message",
            equalTo("One or more errors occurred, please see full details in merge report."));
  }

  private void setupIndicatorData() {
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

    // indicators (2 x source & 1 target)
    sourceUid1 =
        indicatorApiActions
            .post(createIndicator("Ind source 1", indTypeUid1))
            .validateStatus(201)
            .extractUid();
    sourceUid2 =
        indicatorApiActions
            .post(createIndicator("Ind source 2", indTypeUid2))
            .validateStatus(201)
            .extractUid();
    targetUid =
        indicatorApiActions
            .post(createIndicator("Ind target 3", indTypeUid3))
            .validateStatus(201)
            .extractUid();

    // indicators containing refs to other indicators in numerator/denominator
    // indicator with numerator and denominator x 2
    ind4WithIndRef =
        indicatorApiActions
            .post(createIndicatorWithRefs("Ind 4", indTypeUid2, sourceUid1, sourceUid2))
            .validateStatus(201)
            .extractUid();
    ind5WithIndRef =
        indicatorApiActions
            .post(createIndicatorWithRefs("Ind 5", indTypeUid3, targetUid, sourceUid2))
            .validateStatus(201)
            .extractUid();

    indWithoutSourceRef =
        indicatorApiActions
            .post(createIndicatorWithRefs("Ind 6", indTypeUid3, targetUid, "Uid45678901"))
            .validateStatus(201)
            .extractUid();

    // 2 data entry forms containing indicator refs in html code
    formWithSources =
        formsActions
            .post(createDataEntryForm("custom form 1", sourceUid2, sourceUid1))
            .validateStatus(201)
            .extractUid();

    formWithoutSources =
        formsActions
            .post(createDataEntryForm("custom form 2", targetUid, "Uid45678901"))
            .validateStatus(201)
            .extractUid();

    // datasets (have 1 part of a source and 1 not)
    datasetWithSources =
        dataSetActions
            .post(createDataSet("Data set 1", sourceUid1, sourceUid2))
            .validateStatus(201)
            .extractUid();
    datasetWithoutSources =
        dataSetActions
            .post(createDataSet("Data set 2", ind4WithIndRef, ind5WithIndRef))
            .validateStatus(201)
            .extractUid();

    // indicator groups (have 1 part of a source and 1 not)
    indGroupWithSources =
        indicatorGroupActions
            .post(createIndicatorGroup("Group 1", sourceUid1, sourceUid2))
            .validateStatus(201)
            .extractUid();
    indGroupWithNoSources =
        indicatorGroupActions
            .post(createIndicatorGroup("Group 2", ind4WithIndRef, ind5WithIndRef))
            .validateStatus(201)
            .extractUid();

    // sections (have 1 reference a source and 1 not)
    sectionWithSources =
        sectionActions
            .post(createSection("Group 1", datasetWithSources, sourceUid1, sourceUid2))
            .validateStatus(201)
            .extractUid();
    sectionWithoutSources =
        sectionActions
            .post(createSection("Group 2", datasetWithoutSources, ind4WithIndRef, ind5WithIndRef))
            .validateStatus(201)
            .extractUid();

    // and a visualization exists with data dimension items (indicators)
    visUid =
        visualizationActions
            .post(getVisualization(sourceUid1, sourceUid2))
            .validateStatus(201)
            .extractUid();

    // set config indicators
    configActions.post("infrastructuralIndicators", indGroupWithSources).validateStatus(204);
  }

  private String getVisualization(String sourceUid1, String sourceUid2) {
    return """
      {
        "name": "vis test 1",
        "dataDimensionItems": [
          {
            "indicator": {
              "id": "%s"
            },
            "dataDimensionItemType": "INDICATOR"
          },
          {
            "indicator": {
              "id": "%s"
            },
            "dataDimensionItemType": "INDICATOR"
          }
        ],
        "type": "COLUMN",
        "numberType": "VALUE",
        "columns": [
          {
            "items": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ],
            "dimension": "dx"
          },
          {
            "items": [
              {
                 "name": "CHP",
                 "id": "uYxK4wmcPqA",
                 "displayName": "CHP",
                 "displayShortName": "CHP",
                 "dimensionItemType": "ORGANISATION_UNIT_GROUP"
               }
            ],
            "dimension": "%s"
          }
        ],
        "sorting": [
          {
              "dimension": "%s",
              "direction": "ASC"
          }
        ],
        "displayName": "vis test 1"
      }
    """
        .formatted(sourceUid1, sourceUid2, sourceUid1, sourceUid2, sourceUid1, sourceUid1);
  }

  private String createDataEntryForm(String name, String indUid1, String indUid2) {
    // language json
    return """
      {
        "name": "%s",
        "htmlCode": "<table id=\\"registration-form\\" style=\\"width: 300px; padding: 10px;\\">\\r\\n\\t<thead>\\r\\n\\t\\t<tr>\\r\\n\\t\\t\\t<th colspan=\\"2\\" scope=\\"col\\">Child registration form</th>\\r\\n\\t\\t</tr>\\r\\n\\t</thead>\\r\\n\\t<tbody>\\r\\n\\t\\t<tr>\\r\\n\\t\\t\\t<td style=\\"text-align: right; width: 100px;\\">First name</td>\\r\\n\\t\\t\\t<td><input attributeid=\\"%s\\" title=\\"First name \\" value=\\"[First name ]\\" /></td>\\r\\n\\t\\t</tr>\\r\\n\\t\\t<tr>\\r\\n\\t\\t\\t<td style=\\"text-align: right;\\">Last name</td>\\r\\n\\t\\t\\t<td><input attributeid=\\"%s\\" title=\\"Last name \\" value=\\"[Last name ]\\" /></td>\\r\\n\\t\\t</tr>\\r\\n\\t\\t<tr>\\r\\n\\t\\t\\t<td style=\\"text-align: right;\\">Gender</td>\\r\\n\\t\\t</tr>\\r\\n\\t</tbody>\\r\\n</table>\\r\\n\\r\\n<p>&nbsp;</p>"
      }
    """
        .formatted(name, indUid1, indUid2);
  }

  private JsonObject getMergeBody(
      String source1, String source2, String target, boolean deleteSources) {
    JsonObject json = new JsonObject();
    JsonArray sources = new JsonArray();
    sources.add(source1);
    sources.add(source2);
    json.add("sources", sources);
    json.addProperty("target", target);
    json.addProperty("deleteSources", deleteSources);
    return json;
  }

  private String createIndicatorWithRefs(
      String name, String indicatorType, String indicatorRef1, String indicatorRef2) {
    return """
     {
        "name": "test indicator %s",
        "shortName": "test short %s",
        "dimensionItemType": "INDICATOR",
        "numerator": "#{%s}",
        "denominator": "#{%s}",
        "indicatorType": {
            "id": "%s"
        }
     }
    """
        .formatted(name, name, indicatorRef1, indicatorRef2, indicatorType);
  }

  private String createDataSet(String name, String indicatorUid1, String indicatorUid2) {
    return """
      {
        "name": "%s",
        "shortName": "%s",
        "periodType": "Daily",
        "indicators":[
            {
                "id": "%s"
            },
            {
                "id": "%s"
            }
        ]
      }
    """
        .formatted(name, name, indicatorUid1, indicatorUid2);
  }

  private String createIndicatorGroup(String name, String indicatorUid1, String indicatorUid2) {
    return """
      {
        "name": "%s",
        "shortName": "%s",
        "indicators":[
            {
                "id": "%s"
            },
            {
                "id": "%s"
            }
        ]
      }
    """
        .formatted(name, name, indicatorUid1, indicatorUid2);
  }

  private String createSection(
      String name, String dataSet, String indicatorUid1, String indicatorUid2) {
    return """
      {
        "name": "%s",
        "shortName": "%s",
        "dataSet": {
            "id": "%s"
        },
        "indicators":[
            {
                "id": "%s"
            },
            {
                "id": "%s"
            }
        ]
      }
    """
        .formatted(name, name, dataSet, indicatorUid1, indicatorUid2);
  }
}
