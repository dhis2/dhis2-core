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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
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

  private final String check = "category_option_combos_have_duplicates";

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

    HttpResponse response = GET("/categoryOptionCombos?fields=id,name&filter=name:eq:Red");
    assertStatus(HttpStatus.OK, response);
    JsonObject responseContent = response.content();

    JsonList<JsonCategoryOptionCombo> catOptionCombos =
        responseContent.getList("categoryOptionCombos", JsonCategoryOptionCombo.class);
    assertEquals(1, catOptionCombos.size());
    String redCategoryOptionComboId = catOptionCombos.get(0).getId();

    /*We must resort to the service layer as the API will not allow us to create a duplicate*/
    CategoryCombo categoryCombo = categoryService.getCategoryCombo(testCatCombo);
    CategoryOptionCombo existingCategoryOptionCombo =
        categoryService.getCategoryOptionCombo(redCategoryOptionComboId);
    CategoryOptionCombo categoryOptionComboDuplicate = new CategoryOptionCombo();
    categoryOptionComboDuplicate.setAutoFields();
    categoryOptionComboDuplicate.setCategoryCombo(categoryCombo);
    Set<CategoryOption> newCategoryOptions =
        new HashSet<>(existingCategoryOptionCombo.getCategoryOptions());
    categoryOptionComboDuplicate.setCategoryOptions(newCategoryOptions);
    categoryOptionComboDuplicate.setName("Reddish");
    manager.persist(categoryOptionComboDuplicate);
    dbmsManager.clearSession();
    String categoryOptionComboDuplicatedID = categoryOptionComboDuplicate.getUid();
    assertNotNull(categoryOptionComboDuplicatedID);

    assertNamedMetadataObjectExists("categoryOptionCombos", "default");
    assertNamedMetadataObjectExists("categoryOptionCombos", "Red");
    assertNamedMetadataObjectExists("categoryOptionCombos", "Reddish");

    /* There are three total category option combos, so we expect 33% */
    checkDataIntegritySummary(check, 1, 33, true);

    Set<String> expectedCategoryOptCombos =
        Set.of(categoryOptionComboDuplicatedID, redCategoryOptionComboId);
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

    String categoryColor =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categories",
                "{ 'name': 'Color', 'shortName': 'Color', 'dataDimensionType': 'DISAGGREGATION' ,"
                    + "'categoryOptions' : [{'id' : '"
                    + categoryOptionRed
                    + "'}, {'id' : '"
                    + categoryOptionBlue
                    + "'} ] }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categoryCombos",
            "{ 'name' : 'Color', "
                + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                + "{'id' : '"
                + categoryColor
                + "'}]} "));
    assertHasNoDataIntegrityIssues("categoryOptionCombos", check, true);
  }

  @Test
  void testInvalidCategoryOptionCombosDivideByZero() {

    // Expect a percentage here, since there should always be the default category option combo
    assertHasNoDataIntegrityIssues("categoryOptionCombos", check, true);
  }
}
