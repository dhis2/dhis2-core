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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;

import org.hisp.dhis.http.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Tests for data sets whose category combination is neither the default nor an attribute category
 * combination. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/datasets/datasets_non_attribute_category_combo.yaml
 * }
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
class DataIntegrityDatasetsNonAttributeCategoryComboControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  private static final String CHECK = "datasets_non_attribute_category_combo";

  @Test
  void testDataSetWithDisaggregationCategoryCombo() {
    String disaggregationCombo = createCategoryCombo("Sex", "DISAGGREGATION");
    String attributeCombo = createCategoryCombo("Partner", "ATTRIBUTE");
    String dataSetA = createDataSet("Invalid", disaggregationCombo);
    createDataSet("Valid", attributeCombo);

    assertHasDataIntegrityIssues("dataSets", CHECK, 50, dataSetA, "Invalid", "Sex", true);
  }

  @Test
  void testDataSetsWithAttributeOrDefaultCategoryCombo() {
    createDataSet("Valid", createCategoryCombo("Partner", "ATTRIBUTE"));
    createDataSet("Default", getDefaultCatCombo());

    assertHasNoDataIntegrityIssues("dataSets", CHECK, true);
  }

  @Test
  void testDataSetsNonAttributeCategoryComboRuns() {
    assertHasNoDataIntegrityIssues("dataSets", CHECK, false);
  }

  private String createCategoryCombo(String name, String dataDimensionType) {
    String option =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categoryOptions",
                "{ 'name': '%s option', 'shortName': '%s option' }".formatted(name, name)));
    String category =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categories",
                "{ 'name': '%s', 'shortName': '%s', 'dataDimensionType': '%s', 'categoryOptions': [{'id': '%s'}]}"
                    .formatted(name, name, dataDimensionType, option)));
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categoryCombos",
            "{ 'name': '%s', 'dataDimensionType': '%s', 'categories': [{'id': '%s'}]}"
                .formatted(name, dataDimensionType, category)));
  }

  private String createDataSet(String name, String categoryCombo) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/dataSets",
            "{ 'name': '%s', 'shortName': '%s', 'periodType': 'Monthly', 'categoryCombo': {'id': '%s'}}"
                .formatted(name, name, categoryCombo)));
  }
}
