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
package org.hisp.dhis.metadata.metadata_import;

import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author david mackessy
 */
class VisualizationWithExpressionDimensionTest extends ApiTest {
  private MetadataActions metadataActions;
  private RestApiActions vizActions;
  private LoginActions loginActions;
  private final File metadata =
      new File("src/test/resources/metadata/metadata_viz_expr_dim_item.json");

  @BeforeAll
  public void before() {
    loginActions = new LoginActions();
    metadataActions = new MetadataActions();
    vizActions = new RestApiActions("/visualizations");
  }

  @AfterEach
  public void after() {
    QueryParamsBuilder queryParamsBuilder =
        new QueryParamsBuilder().add("importStrategy", "DELETE");
    metadataActions.postFile(metadata, queryParamsBuilder);
  }

  @Test
  @DisplayName("Importing new Visualization with new expression dimension item succeeds")
  void importVizWithExpressionDimensionItemTest() {
    loginActions.loginAsSuperUser();

    // When importing a new visualization with a new expression dimension item
    ApiResponse dataSetResponse = metadataActions.importMetadata(metadata);

    // Then the import succeeds
    dataSetResponse.validate().statusCode(200).body("response.stats.created", equalTo(2));

    // And the imported visualization has the expected ref to the expression dimension item
    vizActions
        .get("VizUid00001")
        .validate()
        .statusCode(200)
        .body("dataDimensionItems[0].expressionDimensionItem.id", equalTo("ExpUid00001"));
  }

  @Test
  @DisplayName("Importing existing Visualization with existing expression dimension item succeeds")
  void importExistingVizWithExpressionDimensionItemTest() {
    loginActions.loginAsSuperUser();

    // Given a new visualization with a new expression dimension item exist
    ApiResponse dataSetResponse = metadataActions.importMetadata(metadata);
    dataSetResponse.validate().statusCode(200).body("response.stats.created", equalTo(2));

    // When importing an existing visualization with an existing expression dimension item
    File metadataFile2 = new File("src/test/resources/metadata/metadata_viz_expr_ref.json");
    ApiResponse dataSetResponse2 = metadataActions.importMetadata(metadataFile2);

    // Then the import succeeds
    dataSetResponse2
        .validate()
        .statusCode(200)
        .body("response.stats.created", equalTo(0))
        .body("response.stats.updated", equalTo(1));

    // And the updated visualization still has the expected ref to the expression dimension iterm
    vizActions
        .get("VizUid00001")
        .validate()
        .statusCode(200)
        .body("dataDimensionItems[0].expressionDimensionItem.id", equalTo("ExpUid00001"));
  }
}
