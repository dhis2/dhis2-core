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

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.api.TestCategoryMetadata;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonCategoryOptionCombo;
import org.hisp.dhis.test.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.test.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoryOptionComboControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private CategoryService categoryService;

  @Test
  @DisplayName(
      "Default CategoryOptionCombo should be present in payload when defaults are INCLUDE by default")
  void getAllCatOptionCombosIncludingDefaultsTest() {
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("dcoc1");
    JsonArray categoryCombos =
        GET("/categoryOptionCombos").content(HttpStatus.OK).getArray("categoryOptionCombos");

    Set<String> cocNames = new HashSet<>(categoryMetadata.getCocNames());
    cocNames.add("default");
    assertEquals(
        cocNames,
        categoryCombos.stream()
            .map(jcoc -> jcoc.as(JsonCategoryOptionCombo.class))
            .map(JsonIdentifiableObject::getDisplayName)
            .collect(Collectors.toSet()),
        "Returned catOptionCombos equal custom catOptionCombos and default catOptionCombo");
  }

  @Test
  @DisplayName("Default CategoryOptionCombo should not be present in payload when EXCLUDE defaults")
  void catOptionCombosExcludingDefaultTest() {
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("dcoc2");
    JsonArray catOptionCombos =
        GET("/categoryOptionCombos?defaults=EXCLUDE")
            .content(HttpStatus.OK)
            .getArray("categoryOptionCombos");

    Set<String> catOptionComboNames =
        catOptionCombos.stream()
            .map(jcoc -> jcoc.as(JsonCategoryOptionCombo.class))
            .map(JsonIdentifiableObject::getDisplayName)
            .collect(Collectors.toSet());

    assertEquals(
        categoryMetadata.getCocNames(),
        catOptionComboNames,
        "Returned catOptionCombos include custom catOptionCombos only");

    assertFalse(
        catOptionComboNames.contains("default"), "default catOptionCombo is not in payload");
  }

  @Test
  @DisplayName("Invalid merge with source and target missing")
  void testInvalidMerge() {
    JsonMixed mergeResponse =
        POST(
                "/categoryOptionCombos/merge",
                """
            {
                "sources": ["Uid00000010"],
                "target": "Uid00000012",
                "deleteSources": true,
                "dataMergeStrategy": "DISCARD"
            }""")
            .content(HttpStatus.CONFLICT);
    assertEquals("Conflict", mergeResponse.getString("httpStatus").string());
    assertEquals("WARNING", mergeResponse.getString("status").string());
    assertEquals(
        "One or more errors occurred, please see full details in merge report.",
        mergeResponse.getString("message").string());

    JsonArray errors =
        mergeResponse.getObject("response").getObject("mergeReport").getArray("mergeErrors");
    JsonObject error1 = errors.getObject(0);
    JsonObject error2 = errors.getObject(1);
    assertEquals(
        "SOURCE CategoryOptionCombo does not exist: `Uid00000010`",
        error1.getString("message").string());
    assertEquals(
        "TARGET CategoryOptionCombo does not exist: `Uid00000012`",
        error2.getString("message").string());
  }

  @Test
  @DisplayName("invalid merge, missing required auth")
  void testMergeNoAuth() {
    switchToNewUser("noAuth", "NoAuth");
    JsonMixed mergeResponse =
        POST(
                "/categoryOptionCombos/merge",
                """
                {
                    "sources": ["Uid00000010"],
                    "target": "Uid00000012",
                    "deleteSources": true,
                    "dataMergeStrategy": "DISCARD"
                }""")
            .content(HttpStatus.FORBIDDEN);
    assertEquals("Forbidden", mergeResponse.getString("httpStatus").string());
    assertEquals("ERROR", mergeResponse.getString("status").string());
    assertEquals(
        "Access is denied, requires one Authority from [F_CATEGORY_OPTION_COMBO_MERGE]",
        mergeResponse.getString("message").string());
  }

  @Test
  @DisplayName("invalid merge, missing dataMergeStrategy")
  void mergeMissingDataMergeStrategyTest() {
    JsonWebMessage validationErrorMsg =
        assertWebMessage(
            "Conflict",
            409,
            "WARNING",
            "One or more errors occurred, please see full details in merge report.",
            POST(
                    "/categoryOptionCombos/merge",
                    """
                    {
                        "sources": ["CocUid00001"],
                        "target": "CocUid00002",
                        "deleteSources": true
                    }""")
                .content(HttpStatus.CONFLICT));

    JsonErrorReport errorReport =
        validationErrorMsg.find(
            JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E1534);
    assertNotNull(errorReport);
    assertEquals(
        "dataMergeStrategy field must be specified. With value `DISCARD` or `LAST_UPDATED`",
        errorReport.getMessage());
  }

  @Test
  @DisplayName("invalid merge, UID is for type other than CategoryOptionCombo")
  void mergeIncorrectTypeTest() {
    JsonWebMessage validationErrorMsg =
        assertWebMessage(
            "Conflict",
            409,
            "WARNING",
            "One or more errors occurred, please see full details in merge report.",
            POST(
                    "/categoryOptionCombos/merge",
                    """
                {
                    "sources": ["bjDvmb4bfuf"],
                    "target": "CocUid00002",
                    "deleteSources": true,
                    "dataMergeStrategy": "DISCARD"
                }""")
                .content(HttpStatus.CONFLICT));

    JsonErrorReport errorReport =
        validationErrorMsg.find(
            JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E1533);
    assertNotNull(errorReport);
    assertEquals(
        "SOURCE CategoryOptionCombo does not exist: `bjDvmb4bfuf`", errorReport.getMessage());
  }

  @Test
  @DisplayName("Duplicate default category option combos should not be allowed")
  void catOptionCombosDuplicatedDefaultTest() {
    JsonObject response =
        GET("/categoryOptionCombos?filter=name:eq:default&fields=id,categoryCombo[id],categoryOptions[id]")
            .content();
    JsonList<JsonCategoryOptionCombo> catOptionCombos =
        response.getList("categoryOptionCombos", JsonCategoryOptionCombo.class);
    String defaultCatOptionComboOptions =
        catOptionCombos.get(0).getCategoryOptions().get(0).getId();
    String defaultCatOptionComboCatComboId = catOptionCombos.get(0).getCategoryCombo().getId();

    JsonWebMessage jsonWebMessage =
        POST(
                "/categoryOptionCombos/",
                """
                { "name": "Not default",
                "categoryOptions" : [{"id" : "%s"}],
                "categoryCombo" : {"id" : "%s"} }
                """
                    .formatted(defaultCatOptionComboOptions, defaultCatOptionComboCatComboId))
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class);
    assertEquals(
        "Creating a single CategoryOptionCombo is forbidden through this endpoint",
        jsonWebMessage.getMessage());
  }

  @Test
  @DisplayName("Can delete a duplicate default COC")
  void canAllowDeleteDuplicatedDefaultCOC() {
    // Revert to the service layer as the API should not allow us to create a duplicate default COC
    CategoryOptionCombo defaultCOC = categoryService.getDefaultCategoryOptionCombo();
    CategoryCombo categoryCombo =
        categoryService.getCategoryCombo(defaultCOC.getCategoryCombo().getUid());
    CategoryOptionCombo existingCategoryOptionCombo =
        categoryService.getCategoryOptionCombo(defaultCOC.getUid());
    CategoryOptionCombo categoryOptionComboDuplicate = new CategoryOptionCombo();
    categoryOptionComboDuplicate.setAutoFields();
    categoryOptionComboDuplicate.setCategoryCombo(categoryCombo);
    Set<CategoryOption> newCategoryOptions =
        new HashSet<>(existingCategoryOptionCombo.getCategoryOptions());
    categoryOptionComboDuplicate.setCategoryOptions(newCategoryOptions);
    categoryOptionComboDuplicate.setName("dupDefault");
    categoryService.addCategoryOptionCombo(categoryOptionComboDuplicate);

    // Can delete the duplicated default COC
    assertStatus(
        HttpStatus.OK, DELETE("/categoryOptionCombos/" + categoryOptionComboDuplicate.getUid()));
  }

  @Test
  @DisplayName("Calls to POST /categoryOptionCombos should be rejected")
  void postCategoryOptionCombosRejectedTest() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Creating a single CategoryOptionCombo is forbidden through this endpoint",
        POST("/categoryOptionCombos", coc()).content(HttpStatus.CONFLICT).as(JsonWebMessage.class));
  }

  private String coc() {
    return """
          {
            "code": "new coc",
            "name": "new coc",
            "categoryCombo": {
              "id": "bjDvmb4bfuf"
            },
            "categoryOptions": [
              {
                "id": "xYerKDKCefk"
              }
            ]
          }
      """;
  }
}
