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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.api.TestCategoryMetadata;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoryOptionComboControllerIntegrationTest extends PostgresControllerIntegrationTestBase {

  @Autowired private CategoryService categoryService;

  @Test
  @DisplayName("Updating an existing CategoryOptionCombo's CategoryCombo is prohibited")
  void updatingCategoryOptionCombosCategoryComboProhibitedTest() {
    // Given an existing category option combo
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("X");
    CategoryOptionCombo coc1 = categoryMetadata.coc1();

    // When trying to update its category combo
    CategoryCombo newCategoryCombo = createCategoryCombo("Y");
    categoryService.addCategoryCombo(newCategoryCombo);
    @Language("json5")
    String cocWithUpdatedCatCombo =
        cocWithCcAndCos(
            newCategoryCombo.getUid(),
            categoryMetadata.co1().getUid(),
            categoryMetadata.co3().getUid());

    // Then it is prohibited
    assertTrue(
        PUT("/categoryOptionCombos/" + coc1.getUid(), cocWithUpdatedCatCombo)
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class)
            .getMessage()
            .contains(
                "The CategoryOptionCombo CategoryCombo relationship cannot be updated once set"));
  }

  private String cocWithCcAndCos(String cc, String co1, String co2) {
    return """
          {
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          }
        """
        .formatted(cc, co1, co2);
  }

  private String cocEmptyOptions(String cc) {
    return """
          {
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": []
          }
        """
        .formatted(cc);
  }
}
