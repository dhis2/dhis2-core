/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;

import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Tests metadata integrity check for category option combinations without a category combination.
 * {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/categories/coc_no_category_combo.yaml
 * }
 *
 * @implNote The test for category combinations without a category combo is impossible to set up in
 *     current versions of DHIS2 through the API.
 * @author Jason P. Pickering
 */
class DataIntegrityCategoryOptionComboNoCatComboControllerTest
    extends AbstractDataIntegrityIntegrationTest {
  private static final String check = "catoptioncombos_no_catcombo";

  private static final String detailsIdType = "categoryOptionCombos";

  @Test
  void testCategoryOptionCombosWithCatCombosExist() {
    String categoryOptionSour =
        assertStatus(
            HttpStatus.CREATED,
            POST("/categoryOptions", "{ 'name': 'Sour', 'shortName': 'Sour' }"));

    String categoryOptionSweet =
        assertStatus(
            HttpStatus.CREATED,
            POST("/categoryOptions", "{ 'name': 'Sweet', 'shortName': 'Sweet' }"));

    String categoryOptionRed =
        assertStatus(
            HttpStatus.CREATED, POST("/categoryOptions", "{ 'name': 'Red', 'shortName': 'Red' }"));

    String categoryColor =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categories",
                "{ 'name': 'Color', 'shortName': 'Color', 'dataDimensionType': 'DISAGGREGATION' ,"
                    + "'categoryOptions' : [{'id' : '"
                    + categoryOptionRed
                    + "'} ] }"));

    String categoryTaste =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categories",
                "{ 'name': 'Taste', 'shortName': 'Taste', 'dataDimensionType': 'DISAGGREGATION' ,"
                    + "'categoryOptions' : [{'id' : '"
                    + categoryOptionSour
                    + "'}, {'id' : '"
                    + categoryOptionSweet
                    + "'} ] }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categoryCombos",
            "{ 'name' : 'Taste and color', "
                + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                + "{'id' : '"
                + categoryColor
                + "'} , {'id' : '"
                + categoryTaste
                + "'}]} "));

    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }
}
