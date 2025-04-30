/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpStatus.CONFLICT;
import static org.hisp.dhis.http.HttpStatus.CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonImportSummary;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class VisualizationControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private IdentifiableObjectManager manager;
  private Program mockProgram;

  @BeforeEach
  public void beforeEach() {
    mockProgram = createProgram('A');
    manager.save(mockProgram);
  }

  @Test
  void testGetVisualizationWithNestedFilters() {
    JsonImportSummary report =
        POST("/metadata", Path.of("metadata/metadata_with_visualization.json"))
            .content(HttpStatus.OK)
            .get("response")
            .as(JsonImportSummary.class);
    assertEquals("OK", report.getStatus());

    JsonMixed response =
        GET("/visualizations.json?filter=id:eq:qD72aBqsHvt&fields=filters").content();
    JsonList<JsonObject> visualizations = response.getList("visualizations", JsonObject.class);
    assertEquals(1, visualizations.size());
    assertEquals(1, visualizations.get(0).getList("filters", JsonObject.class).size());
  }

  @Test
  void testPostForInvalidOutlierMaxResults() {
    // Given
    Integer invalidRange = 501;
    String body =
        """
            {
                "name": "Test Visualization",
                "type": "LINE",
                "program": {
                    "id": "IpHINAT79UW"
                },
                "outlierAnalysis": {
                    "enabled": true,
                    "outlierMethod": "MODIFIED_Z_SCORE",
                    "thresholdFactor": 3,
                    "extremeLines": {
                        "enabled": false,
                        "value": 1
                    },
                    "maxResults": ${invalidRange}
                }
            }
        """
            .replace("${invalidRange}", invalidRange.toString());

    // When
    HttpResponse response = POST("/visualizations/", body);

    // Then
    assertEquals(
        "Allowed length range for property `maxResults` is [1 to 500], but given length was 501",
        response.error(CONFLICT).getMessage());
  }

  @Test
  void testPostForNullOutlierMaxResults() {
    // Given
    String body =
        """
            {
                "name": "Test Visualization",
                "type": "LINE",
                "program": {
                    "id": "IpHINAT79UW"
                },
                "outlierAnalysis": {
                    "enabled": true,
                    "outlierMethod": "MODIFIED_Z_SCORE",
                    "thresholdFactor": 3,
                    "extremeLines": {
                        "enabled": false,
                        "value": 1
                    },
                    "maxResults": null
                }
            }
        """;

    // When
    String uid = assertStatus(CREATED, POST("/visualizations/", body));

    // Then
    String getParams = "?fields=:all,columns[:all,items,sorting]";
    JsonObject response = GET("/visualizations/" + uid + getParams).content();

    assertThat(response.get("outlierAnalysis").toString(), not(containsString("maxResults")));
  }

  @Test
  void testPostSortingObject() {
    // Given
    String dimension = "pe";
    String sorting = "'sorting': [{'dimension': '" + dimension + "', 'direction':'ASC'}]";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': [{'dimension': '"
            + dimension
            + "'}],"
            + sorting
            + "}";

    // When
    String uid = assertStatus(CREATED, POST("/visualizations/", body));

    // Then
    String getParams = "?fields=:all,columns[:all,items,sorting]";
    JsonObject response = GET("/visualizations/" + uid + getParams).content();

    assertThat(response.get("sorting").toString(), containsString("pe"));
    assertThat(response.get("sorting").toString(), containsString("ASC"));
  }

  @Test
  void testPostOutlierTypeObject() {
    // Given
    String dimension = "pe";
    String body =
        "{'name': 'Name Test', 'type': 'OUTLIER_TABLE', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': [{'dimension': '"
            + dimension
            + "'}]"
            + "}";

    // When
    String uid = assertStatus(CREATED, POST("/visualizations/", body));

    // Then
    String getParams = "?filter=type:eq:OUTLIER_TABLE&fields=:all,columns[:all,items,sorting]";
    JsonObject response = GET("/visualizations" + getParams).content();

    JsonNode visualization = response.getArray("visualizations").getArray(0).node();
    assertEquals(uid, visualization.get("id").value().toString());
    assertEquals("OUTLIER_TABLE", visualization.get("type").value().toString());
  }

  @Test
  void testPostSortingObjectForColumnItems() {
    // Given
    IndicatorType indicatorType = createIndicatorType('A');
    manager.save(indicatorType);

    Indicator mockIndicator = createIndicator('A', indicatorType);
    manager.save(mockIndicator);

    String sorting =
        "'sorting': [{'dimension': '" + mockIndicator.getUid() + "', 'direction':'ASC'}]";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': [{'dimension': 'dx',"
            + "'items': [{'id': '"
            + mockIndicator.getUid()
            + "'}]}],"
            + sorting
            + "}";

    // When
    String uid = assertStatus(CREATED, POST("/visualizations/", body));

    // Then
    String getParams = "?fields=:all,columns[:all,items,sorting]";
    JsonObject response = GET("/visualizations/" + uid + getParams).content();

    assertThat(response.get("sorting").toString(), containsString(mockIndicator.getUid()));
    assertThat(response.get("sorting").toString(), containsString("ASC"));
  }

  @Test
  void testPostMultipleSortingObject() {
    // Given
    String dimension1 = "pe";
    String dimension2 = "ou";
    String sorting =
        "'sorting': [{'dimension': '"
            + dimension1
            + "', 'direction':'ASC'},"
            + "{'dimension': '"
            + dimension2
            + "', 'direction':'DESC'}]";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': [{'dimension': '"
            + dimension1
            + "'}, {'dimension': '"
            + dimension2
            + "'}],"
            + sorting
            + "}";

    // When
    String uid = assertStatus(CREATED, POST("/visualizations/", body));

    // Then
    String getParams = "?fields=:all,columns[:all,items,sorting]";
    JsonObject response = GET("/visualizations/" + uid + getParams).content();

    assertThat(response.get("sorting").toString(), containsString("pe"));
    assertThat(response.get("sorting").toString(), containsString("ASC"));
    assertThat(response.get("sorting").toString(), containsString("ou"));
    assertThat(response.get("sorting").toString(), containsString("DESC"));
  }

  @Test
  void testPostSortingObjectWithDuplication() {
    // Given
    String dimension = "pe";
    String sorting =
        "'sorting': [{'dimension': '"
            + dimension
            + "', 'direction':'ASC'},"
            + "{'dimension': '"
            + dimension
            + "', 'direction':'DESC'}]";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': [{'dimension': '"
            + dimension
            + "'}],"
            + sorting
            + "}";

    // When
    String uid = assertStatus(CREATED, POST("/visualizations/", body));

    // Then
    String getParams = "?fields=:all,columns[:all,items,sorting]";
    JsonObject response = GET("/visualizations/" + uid + getParams).content();

    assertThat(response.get("sorting").toString(), containsString("pe"));
    assertThat(response.get("sorting").toString(), containsString("ASC"));
    assertThat(response.get("sorting").toString(), not(containsString("DESC")));
  }

  @Test
  void testRelativePeriods() {
    POST(
            "/metadata?importStrategy=CREATE_AND_UPDATE&async=false",
            Path.of("metadata/metadata_with_visualization.json"))
        .content(HttpStatus.OK)
        .as(JsonImportSummary.class);
    JsonMixed visualization =
        GET("/visualizations/qD72aBqsHvt").content(HttpStatus.OK).as(JsonMixed.class);
    assertNotNull(visualization);
    assertTrue(
        visualization.getObject("relativePeriods").getBoolean("last12Months").booleanValue());
  }

  @Test
  void testPostAndGetOptionItemInProgramAttribute() {
    // Given
    TrackedEntityAttribute attribute = createTrackedEntityAttribute('A');
    manager.save(attribute);

    Option option = createOption('O');
    manager.save(option);

    String programUid = mockProgram.getUid();
    String attributeUid = attribute.getUid();
    String optionUid = option.getUid();
    String dimUid = programUid + "." + attributeUid + "." + optionUid;
    String jsonBody =
"""
{
    "type": "PIE",
    "columns": [
        {
            "dimension": "dx",
            "items": [
                {
                    "id": "${dimUid}",
                    "name": "Program Attribute - Option",
                    "dimensionItemType": "PROGRAM_ATTRIBUTE_OPTION"
                }
            ]
        }
    ],
    "name": "OptionItem - Test"
}
"""
            .replace("${dimUid}", dimUid);

    // When
    String uid = assertStatus(CREATED, POST("/visualizations/", jsonBody));

    // Then
    String getParams = "?fields=columns[:all,items[:all]]";
    JsonObject response = GET("/visualizations/" + uid + getParams).content();

    JsonNode columnNode = response.get("columns").node().element(0);
    JsonNode itemsNode = columnNode.get("items").elementOrNull(0);

    assertEquals("dx", columnNode.get("id").value());
    assertEquals("DATA_X", columnNode.get("dimensionType").value());
    assertTrue((boolean) columnNode.get("dataDimension").value());
    assertEquals("PROGRAM_ATTRIBUTE_OPTION", itemsNode.get("dimensionItemType").value());
    assertEquals("NONE", itemsNode.get("aggregationType").value());
    assertEquals(dimUid, itemsNode.get("dimensionItem").value());
  }

  @Test
  void testPostAndGetOptionItemInDataElement() {
    // Given
    DataElement dataElement = createDataElement('A');
    manager.save(dataElement);

    Option option = createOption('O');
    manager.save(option);

    String programUid = mockProgram.getUid();
    String dataElementUid = dataElement.getUid();
    String optionUid = option.getUid();
    String dimUid = programUid + "." + dataElementUid + "." + optionUid;
    String jsonBody =
"""
{
    "type": "PIE",
    "columns": [
        {
            "dimension": "dx",
            "items": [
                {
                    "id": "${dimUid}",
                    "name": "Program Data Element - Option",
                    "dimensionItemType": "PROGRAM_DATA_ELEMENT_OPTION"
                }
            ]
        }
    ],
    "name": "OptionItem - Test"
}
"""
            .replace("${dimUid}", dimUid);

    // When
    String uid = assertStatus(CREATED, POST("/visualizations/", jsonBody));

    // Then
    String getParams = "?fields=columns[:all,items[:all]]";
    JsonObject response = GET("/visualizations/" + uid + getParams).content();

    JsonNode columnNode = response.get("columns").node().element(0);
    JsonNode itemsNode = columnNode.get("items").elementOrNull(0);

    assertEquals("dx", columnNode.get("id").value());
    assertEquals("DATA_X", columnNode.get("dimensionType").value());
    assertTrue((boolean) columnNode.get("dataDimension").value());
    assertEquals("PROGRAM_DATA_ELEMENT_OPTION", itemsNode.get("dimensionItemType").value());
    assertEquals("SUM", itemsNode.get("aggregationType").value());
  }
}
