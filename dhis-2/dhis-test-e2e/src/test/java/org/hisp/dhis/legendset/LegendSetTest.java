package org.hisp.dhis.legendset;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LegendSetTest extends ApiTest {

  private MetadataActions metadataActions;
  private RestApiActions legendSetActions;
  private RestApiActions dataSetActions;

  @BeforeAll
  public void init() {
    //
    metadataActions = new MetadataActions();
    legendSetActions = new RestApiActions("legendSets");
    dataSetActions = new RestApiActions("dataSets");
  }

  @Test
  @DisplayName("Deleting a legend set which is referenced by a data set is successful")
  void deleteLegendSetTest() {
    metadataActions.importMetadata(dataSetWithLegendSet()).validateStatus(200);

    // confirm data set has legend set
    dataSetActions
        .get("DsUid000001")
        .validate()
        .body("legendSets", hasItem(allOf(hasEntry("id", "LegSetUid01"))));

    // delete legend set
    ApiResponse response = legendSetActions.delete("LegSetUid01");
    response.validate().body("httpStatus", equalTo("OK")).body("httpStatusCode", equalTo(200));

    // confirm data set no longer has legend set
    dataSetActions.get("DsUid000001").validate().body("legendSets", empty());
  }

  private String dataSetWithLegendSet() {
    return """
      {
        "dataSets": [
          {
            "name": "ds 1",
            "id": "DsUid000001",
            "shortName": "ds 1",
            "periodType": "Monthly",
            "legendSets": [
              {
                "id": "LegSetUid01"
              }
            ]
          }
        ],
        "legendSets": [
          {
            "name": "Test legend11",
            "legends": [
              {
                "name": "45 - 60",
                "startValue": 45.0,
                "endValue": 60.0,
                "displayName": "45 - 60",
                "id": "LegUid00001"
              }
            ],
            "displayName": "Test legend",
            "id": "LegSetUid01"
            }
        ]
      }
      """;
  }
}
