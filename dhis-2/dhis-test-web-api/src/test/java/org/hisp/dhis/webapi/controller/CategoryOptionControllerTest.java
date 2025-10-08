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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.api.TestCategoryMetadata;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonCategoryOption;
import org.hisp.dhis.test.webapi.json.domain.JsonIdentifiableObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoryOptionControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  @DisplayName(
      "Default CategoryOption should be present in payload when defaults are INCLUDE by default")
  void getAllCatOptionsIncludingDefaultsTest() {
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("dco1");
    JsonArray categoryOptions =
        GET("/categoryOptions").content(HttpStatus.OK).getArray("categoryOptions");

    Set<String> coNames = new HashSet<>(categoryMetadata.getCoNames());
    coNames.add("default");
    assertTrue(
        coNames.containsAll(
            categoryOptions.stream()
                .map(jco -> jco.as(JsonCategoryOption.class))
                .map(JsonIdentifiableObject::getDisplayName)
                .collect(Collectors.toSet())),
        "Returned catOptions include custom catOptions and default catOption");
  }

  @Test
  @DisplayName("Default CategoryOption should not be present in payload when EXCLUDE defaults")
  void catOptionsExcludingDefaultTest() {
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("dco2");
    JsonArray categoryOptions =
        GET("/categoryOptions?defaults=EXCLUDE").content(HttpStatus.OK).getArray("categoryOptions");

    Set<String> catOptions =
        categoryOptions.stream()
            .map(jco -> jco.as(JsonCategoryOption.class))
            .map(JsonIdentifiableObject::getDisplayName)
            .collect(Collectors.toSet());

    assertTrue(
        categoryMetadata.getCoNames().containsAll(catOptions),
        "Returned catOptions include custom catOptions only");

    assertFalse(catOptions.contains("default"), "default catOption was not in payload");
  }

  @Test
  @DisplayName("Invalid merge with source and target missing")
  void testInvalidMerge() {
    JsonMixed mergeResponse =
        POST(
                "/categoryOptions/merge",
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
        "SOURCE CategoryOption does not exist: `Uid00000010`",
        error1.getString("message").string());
    assertEquals(
        "TARGET CategoryOption does not exist: `Uid00000012`",
        error2.getString("message").string());
  }

  @Test
  @DisplayName("invalid merge, missing required auth")
  void testMergeNoAuth() {
    switchToNewUser("noAuth", "NoAuth");
    JsonMixed mergeResponse =
        POST(
                "/categoryOptions/merge",
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
        "Access is denied, requires one Authority from [F_CATEGORY_OPTION_MERGE]",
        mergeResponse.getString("message").string());
  }

  @Test
  @DisplayName("Should save and get translation for CategoryOption formName")
  void testCategoryOptionFormNameTranslation() {
    // Create a category option with formName
    String coId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categoryOptions/",
                "{'name':'Test Category Option', 'shortName':'TCO', 'formName':'Test Form'}"));

    // Verify initial state - no translations
    JsonArray translations =
        GET("/categoryOptions/{id}/translations", coId).content().getArray("translations");
    assertTrue(translations.isEmpty());

    // Add translation for formName
    PUT(
            "/categoryOptions/" + coId + "/translations",
            "{'translations': [{'locale':'fr', 'property':'FORM_NAME', 'value':'Formulaire de Test'}]}")
        .content(HttpStatus.NO_CONTENT);

    // Verify translation was saved
    translations =
        GET("/categoryOptions/{id}/translations", coId).content().getArray("translations");
    assertEquals(1, translations.size());
    JsonObject translation = translations.getObject(0);
    assertEquals("fr", translation.getString("locale").string());
    assertEquals("FORM_NAME", translation.getString("property").string());
    assertEquals("Formulaire de Test", translation.getString("value").string());

    // Verify displayFormName returns translated value when locale is set
    JsonObject categoryOption =
        GET("/categoryOptions/{id}?locale=fr", coId).content().as(JsonObject.class);
    assertEquals("Formulaire de Test", categoryOption.getString("displayFormName").string());

    // Verify displayFormName returns original formName when no locale is set
    categoryOption = GET("/categoryOptions/{id}", coId).content().as(JsonObject.class);
    assertEquals("Test Form", categoryOption.getString("displayFormName").string());
  }
}
