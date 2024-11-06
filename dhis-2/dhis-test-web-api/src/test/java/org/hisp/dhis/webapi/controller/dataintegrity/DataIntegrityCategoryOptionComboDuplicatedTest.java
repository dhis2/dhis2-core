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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;

import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.json.domain.JsonCategoryOptionCombo;
import org.junit.jupiter.api.Test;

/**
 * Tests the metadata check for category option combos with the same category options.
 *
 * <p>{@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/categories/category_option_combos_have_duplicates.yaml}
 *
 * @author David Mackessy
 */
class DataIntegrityCategoryOptionComboDuplicatedTest extends AbstractDataIntegrityIntegrationTest {
  
  private final String check = "category_option_combo_duplicates";

  private String cocWithOptionsA;

  private String categoryOptionRed;

  @Test
  void testCategoryOptionCombosDuplicated() {

    categoryOptionRed =
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

    String testCatCombo =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categoryCombos",
                "{ 'name' : 'Color', "
                    + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                    + "{'id' : '"
                    + categoryColor
                    + "'}]} "));

    cocWithOptionsA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categoryOptionCombos",
                """
                { "name": "Reddish",
                "categoryOptions" : [{"id" : "%s"}],
                "categoryCombo" : {"id" : "%s"} }
                """
                    .formatted(categoryOptionRed, testCatCombo)));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categoryOptionCombos",
            """
                { "name": "Not Red",
                "categoryOptions" : [{"id" : "%s"}]},
                 "categoryCombo" : {"id" : "%s"} }
                """
                .formatted(categoryOptionRed, testCatCombo)));

    assertNamedMetadataObjectExists("categoryOptionCombos", "default");
    assertNamedMetadataObjectExists("categoryOptionCombos", "Red");
    assertNamedMetadataObjectExists("categoryOptionCombos", "Reddish");
    assertNamedMetadataObjectExists("categoryOptionCombos", "Not Red");
    
    /*We need to get the Red category option combo to be able to check the data integrity issues*/

    JsonObject response = GET("/categoryOptionCombos?fields=id,name&filter=name:eq:Red").content();
    JsonList<JsonCategoryOptionCombo> catOptionCombos =
        response.getList("categoryOptionCombos", JsonCategoryOptionCombo.class);
    String redCategoryOptionComboId = catOptionCombos.get(0).getId();
    /* There are four total category option combos, so we expect 25% */
    checkDataIntegritySummary(check, 1, 25, true);

    Set<String> expectedCategoryOptCombos = Set.of(cocWithOptionsA, redCategoryOptionComboId);
    Set<String> expectedMessages = Set.of("Red", "Reddish");
    checkDataIntegrityDetailsIssues(
        check, expectedCategoryOptCombos, expectedMessages, Set.of(), "categoryOptionCombos");
  }

  @Test
  void testCategoryOptionCombosNotDuplicated() {

    categoryOptionRed =
        assertStatus(
            HttpStatus.CREATED, POST("/categoryOptions", "{ 'name': 'Red', 'shortName': 'Red' }"));

    String categoryOptionBlue =
        assertStatus(
            HttpStatus.CREATED,
            POST("/categoryOptions", "{ 'name': 'Blue', 'shortName': 'Blue' }"));

    cocWithOptionsA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categoryOptionCombos",
                """
                { "name": "Color",
                "categoryOptions" : [{"id" : "%s"} ] }
                """
                    .formatted(categoryOptionRed)));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categoryOptionCombos",
            """
                { "name": "Colour",
                "categoryOptions" : [{"id" : "%s"} ] }
                """
                .formatted(categoryOptionBlue)));

    assertHasNoDataIntegrityIssues("categoryOptionCombos", check, true);
  }

  @Test
  void testInvalidCategoryOptionCombosDivideByZero() {

    // Expect a percentage here, since there should always be the default category option combo
    assertHasNoDataIntegrityIssues("categoryOptionCombos", check, true);
  }
}
