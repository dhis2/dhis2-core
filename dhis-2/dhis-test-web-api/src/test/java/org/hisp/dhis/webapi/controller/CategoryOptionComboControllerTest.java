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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonCategoryOptionCombo;
import org.hisp.dhis.test.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.test.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoryOptionComboControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private CategoryService categoryService;

  @BeforeEach
  void setUp() {
    CategoryCombo catComboA = createCategoryCombo('A');
    CategoryCombo catComboB = createCategoryCombo('B');
    CategoryCombo catComboC = createCategoryCombo('C');
    categoryService.addCategoryCombo(catComboA);
    categoryService.addCategoryCombo(catComboB);
    categoryService.addCategoryCombo(catComboC);

    CategoryOption catOptA = createCategoryOption('A');
    CategoryOption catOptB = createCategoryOption('B');
    CategoryOption catOptC = createCategoryOption('C');
    categoryService.addCategoryOption(catOptA);
    categoryService.addCategoryOption(catOptB);
    categoryService.addCategoryOption(catOptC);

    CategoryOptionCombo cocA =
        createCategoryOptionCombo("CatOptCombo A", "CocUid00001", catComboA, catOptA);
    CategoryOptionCombo cocB =
        createCategoryOptionCombo("CatOptCombo B", "CocUid00002", catComboB, catOptB);
    CategoryOptionCombo cocC =
        createCategoryOptionCombo("CatOptCombo C", "CocUid00003", catComboC, catOptC);
    categoryService.addCategoryOptionCombo(cocA);
    categoryService.addCategoryOptionCombo(cocB);
    categoryService.addCategoryOptionCombo(cocC);
  }

  @Test
  @DisplayName(
      "Default CategoryOptionCombo should be present in payload when defaults are INCLUDE by default")
  void getAllCatOptionCombosIncludingDefaultsTest() {
    JsonArray categoryCombos =
        GET("/categoryOptionCombos").content(HttpStatus.OK).getArray("categoryOptionCombos");

    assertEquals(
        Set.of("CatOptCombo C", "CatOptCombo B", "CatOptCombo A", "default"),
        categoryCombos.stream()
            .map(jcoc -> jcoc.as(JsonCategoryOptionCombo.class))
            .map(JsonIdentifiableObject::getDisplayName)
            .collect(Collectors.toSet()),
        "Returned catOptionCombos equal custom catOptions and default catOption");
  }

  @Test
  @DisplayName("Default CategoryOptionCombo should not be present in payload when EXCLUDE defaults")
  void catOptionCombosExcludingDefaultTest() {
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
        Set.of("CatOptCombo C", "CatOptCombo B", "CatOptCombo A"),
        catOptionComboNames,
        "Returned catOptionCombos include custom catOptions only");

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
}
