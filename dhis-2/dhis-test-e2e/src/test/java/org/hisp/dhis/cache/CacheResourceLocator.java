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
package org.hisp.dhis.cache;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;

final class CacheResourceLocator {
  private static final File DATA_SET_FIXTURE =
      new File("src/test/resources/metadata/adex-metadata.json");
  private static final File VISUALIZATION_FIXTURE =
      new File("src/test/resources/metadata/metadata_viz_expr_dim_item.json");

  private final RestApiActions dataSets = new RestApiActions("/dataSets");
  private final RestApiActions visualizations = new RestApiActions("/visualizations");
  private final RestApiActions userDataStore = new RestApiActions("/userDataStore");
  private final LoginActions loginActions = new LoginActions();
  private final MetadataActions metadataActions = new MetadataActions();

  private String firstDataSetId;
  private String firstVisualizationId;

  String firstDataSetId() {
    if (firstDataSetId == null) {
      firstDataSetId = resolveFirstId(dataSets, "dataSets.id[0]");
      if (firstDataSetId == null) {
        importFixture(DATA_SET_FIXTURE);
        firstDataSetId = resolveFirstId(dataSets, "dataSets.id[0]");
      }
      assertNotNull(firstDataSetId, "Expected at least one seeded data set for cache tests");
    }

    return firstDataSetId;
  }

  String firstVisualizationId() {
    if (firstVisualizationId == null) {
      firstVisualizationId = resolveFirstId(visualizations, "visualizations.id[0]");
      if (firstVisualizationId == null) {
        importFixture(VISUALIZATION_FIXTURE);
        firstVisualizationId = resolveFirstId(visualizations, "visualizations.id[0]");
      }
      assertNotNull(
          firstVisualizationId, "Expected at least one seeded visualization for cache tests");
    }

    return firstVisualizationId;
  }

  void ensureUserDataStoreProbeEntry() {
    ApiResponse response = userDataStore.put("cache-e2e/probe", "{\"seed\":\"cache-probe\"}");
    assertTrue(
        response.statusCode() == 200 || response.statusCode() == 201,
        "Expected user datastore probe setup to succeed");
  }

  private String resolveFirstId(RestApiActions actions, String pathExpression) {
    return actions
        .get(new QueryParamsBuilder().add("fields=id").add("page=1").add("pageSize=1"))
        .extractString(pathExpression);
  }

  private void importFixture(File file) {
    assertTrue(file.exists(), "Expected cache metadata fixture to exist: " + file.getPath());
    loginActions.loginAsSuperUser();
    metadataActions.importAndValidateMetadata(file, "async=false");
  }
}
