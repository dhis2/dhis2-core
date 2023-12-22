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
import static org.hisp.dhis.merge.IndicatorTypeMergeTest.createIndicator;
import static org.hisp.dhis.merge.IndicatorTypeMergeTest.createIndicatorType;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IndicatorMergeTest extends ApiTest {

  private RestApiActions indicatorApiActions;
  private RestApiActions indicatorTypeApiActions;
  private RestApiActions formsActions;
  private LoginActions loginActions;
  private RestApiActions dataSetActions;
  private RestApiActions indicatorGroupActions;
  private RestApiActions sectionActions;

  @BeforeAll
  public void before() {
    loginActions = new LoginActions();
    indicatorApiActions = new RestApiActions("indicators");
    indicatorTypeApiActions = new RestApiActions("indicatorTypes");
    formsActions = new RestApiActions("dataEntryForms");
    dataSetActions = new RestApiActions("dataSets");
    indicatorGroupActions = new RestApiActions("indicatorGroups");
    sectionActions = new RestApiActions("sections");
    loginActions.loginAsSuperUser();
  }

  @Test
  @DisplayName("Valid Indicator merge completes successfully")
  void testValidIndicatorMerge() {
    // given
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

    // config

    // indicators (2 x source & 1 target)
    String sourceUid1 =
        indicatorApiActions
            .post(createIndicator("Ind4", indTypeUid1))
            .validateStatus(201)
            .extractUid();
    String sourceUid2 =
        indicatorApiActions
            .post(createIndicator("Ind5", indTypeUid2))
            .validateStatus(201)
            .extractUid();
    String targetUid =
        indicatorApiActions
            .post(createIndicator("Ind6", indTypeUid3))
            .validateStatus(201)
            .extractUid();

    // indicators containing refs to other indicators in numerator/denominator
    // indicator with numerator and denominator x 2
    String i4 =
        indicatorApiActions
            .post(createIndicatorWithRefs("Ind5", indTypeUid2, sourceUid1, sourceUid2))
            .validateStatus(201)
            .extractUid();
    String i5 =
        indicatorApiActions
            .post(createIndicatorWithRefs("Ind6", indTypeUid3, targetUid, sourceUid2))
            .validateStatus(201)
            .extractUid();

    // 2 data entry forms containing indicator refs in html code
    String form1Uid =
        formsActions
            .post(createDataEntryForm("custom form 1", sourceUid2, sourceUid1))
            .validateStatus(201)
            .extractUid();

    String form2Uid =
        formsActions
            .post(createDataEntryForm("custom form 2", targetUid, sourceUid2))
            .validateStatus(201)
            .extractUid();

    // datasets (have 1 part of a source and 1 not)
    String ds1Uid =
        dataSetActions
            .post(createDataSet("Data set 1", sourceUid1, sourceUid2))
            .validateStatus(201)
            .extractUid();
    String ds2Uid =
        dataSetActions.post(createDataSet("Data set 2", i4, i5)).validateStatus(201).extractUid();

    // indicator groups (have 1 part of a source and 1 not)
    String ig1Uid =
        indicatorGroupActions
            .post(createIndicatorGroup("Group 1", sourceUid1, sourceUid2))
            .validateStatus(201)
            .extractUid();
    String ig2Uid =
        indicatorGroupActions
            .post(createIndicatorGroup("Group 2", i4, i5))
            .validateStatus(201)
            .extractUid();

    // sections (have 1 part of a source and 1 not)
    // TODO sections still exist and can't be deleted until sections resolved
    String s1Uid =
        sectionActions
            .post(createSection("Group 1", ds1Uid, sourceUid1, sourceUid2))
            .validateStatus(201)
            .extractUid();
    String s2Uid =
        sectionActions
            .post(createSection("Group 2", ds2Uid, i4, i5))
            .validateStatus(201)
            .extractUid();

    //
    //
    //
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
        .body("response.mergeReport.message", equalTo("INDICATOR merge complete"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("INDICATOR"))
        .body("response.mergeReport.sourcesDeleted", hasItems(sourceUid1, sourceUid2));

    // and sources are deleted & target exists
    indicatorApiActions.get(sourceUid1).validateStatus(404);
    indicatorApiActions.get(sourceUid1).validateStatus(404);
    indicatorApiActions.get(targetUid).validateStatus(200);

    // and all groups now reference target indicator type
    // and all datasets now reference target indicator type
    // and all sections now reference target indicator type
    // and all config now reference target indicator type
    // and all ddi now reference target indicator type
    // and all indicator numer/denom now reference target indicator type
    // and all forms now reference target indicator type
    indicatorApiActions
        .get(sourceUid1)
        .validate()
        .statusCode(200)
        .body("indicatorType.id", equalTo(indTypeUid3));

    indicatorApiActions
        .get(sourceUid2)
        .validate()
        .statusCode(200)
        .body("indicatorType.id", equalTo(indTypeUid3));

    indicatorApiActions
        .get(targetUid)
        .validate()
        .statusCode(200)
        .body("indicatorType.id", equalTo(indTypeUid3));
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
