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

import static org.hisp.dhis.common.DataDimensionType.DISAGGREGATION;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpStatus.CONFLICT;
import static org.hisp.dhis.http.HttpStatus.CREATED;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonCategory;
import org.hisp.dhis.test.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoryControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private CategoryService categoryService;

  @BeforeEach
  void setUp() {
    Category catA = createCategory('A');
    Category catB = createCategory('B');
    Category catC = createCategory('C');
    categoryService.addCategory(catA);
    categoryService.addCategory(catB);
    categoryService.addCategory(catC);
  }

  @Test
  @DisplayName("Default Category should be present in payload when defaults are INCLUDE by default")
  void getAllCategoriesIncludingDefaultsTest() {
    JsonArray categories = GET("/categories").content(HttpStatus.OK).getArray("categories");

    assertEquals(
        Set.of("CategoryA", "CategoryB", "CategoryC", "default"),
        categories.stream()
            .map(jde -> jde.as(JsonCategory.class))
            .map(JsonIdentifiableObject::getDisplayName)
            .collect(Collectors.toSet()),
        "Returned category IDs include custom categories and default category");
  }

  @Test
  @DisplayName("Default Category should not be present in payload when EXCLUDE defaults")
  void categoriesExcludingDefaultTest() {
    JsonArray categories =
        GET("/categories?defaults=EXCLUDE").content(HttpStatus.OK).getArray("categories");

    Set<String> catDisplayNames =
        categories.stream()
            .map(jc -> jc.as(JsonCategory.class))
            .map(JsonIdentifiableObject::getDisplayName)
            .collect(Collectors.toSet());

    assertEquals(
        Set.of("CategoryC", "CategoryB", "CategoryA"),
        catDisplayNames,
        "Returned categories include custom categories only");

    assertFalse(catDisplayNames.contains("default"), "default category was not in payload");
  }

  @Test
  @DisplayName(
      "A Category with default CategoryOption should not have the category option field present when EXCLUDE defaults")
  void catExcludingDefaultCatOptionTest() {
    CategoryOption customCat = createCategoryOption('P');
    categoryService.addCategoryOption(customCat);
    Category catWithCustomCatOption = createCategory('1', customCat);
    Category catWithDefaultCategory = createCategory('2');
    categoryService.addCategory(catWithCustomCatOption);
    categoryService.addCategory(catWithDefaultCategory);

    JsonArray categories =
        GET("/categories?fields=categoryOptions[name,id]&defaults=EXCLUDE")
            .content(HttpStatus.OK)
            .getArray("categories");

    Set<String> catOptions =
        categories.stream()
            .map(jc -> jc.as(JsonCategory.class))
            .flatMap(jc -> jc.getCategoryOptions().stream())
            .map(JsonIdentifiableObject::getName)
            .collect(Collectors.toSet());

    assertEquals(
        Set.of("CategoryOptionP"),
        catOptions,
        "category option equals custom cat option only, no default category option present");
  }

  @Test
  @DisplayName(
      "A Category with default CategoryOption should have the category option field present when INCLUDE defaults")
  void catIncludingDefaultCatOptionTest() {
    CategoryOption customCat = createCategoryOption('P');
    categoryService.addCategoryOption(customCat);
    Category catWithCustomCatOption = createCategory('1', customCat);
    Category catWithDefaultCategory = createCategory('2');
    categoryService.addCategory(catWithCustomCatOption);
    categoryService.addCategory(catWithDefaultCategory);

    JsonArray categories =
        GET("/categories?fields=categoryOptions[name,id]&defaults=INCLUDE")
            .content(HttpStatus.OK)
            .getArray("categories");

    Set<String> catOptions =
        categories.stream()
            .map(jc -> jc.as(JsonCategory.class))
            .flatMap(jc -> jc.getCategoryOptions().stream())
            .map(JsonIdentifiableObject::getName)
            .collect(Collectors.toSet());

    assertEquals(
        Set.of("CategoryOptionP", "default"),
        catOptions,
        "category options equal custom cat option and default category option");
  }

  @Test
  void testMaxOptionsPerCategoryLimit() {
    List<String> options = IntStream.range(1, 55).mapToObj(i -> "o" + i).toList();
    JsonWebMessage msg =
        postCategory("maxOpt", DISAGGREGATION, options).content(CONFLICT).as(JsonWebMessage.class);
    assertContains(
        " cannot have more than 50 options, but had: 54",
        msg.getResponse().getArray("errorReports").getObject(0).getString("message").string());
  }

  @Test
  void testMaxCategoriesPerComboLimit() {
    DataDimensionType type = DISAGGREGATION;
    String catId1 = assertStatus(CREATED, postCategory("maxCat1", type, List.of("a", "b")));
    String catId2 = assertStatus(CREATED, postCategory("maxCat2", type, List.of("c", "d")));
    String catId3 = assertStatus(CREATED, postCategory("maxCat3", type, List.of("e", "f")));
    String catId4 = assertStatus(CREATED, postCategory("maxCat4", type, List.of("g", "h")));
    String catId5 = assertStatus(CREATED, postCategory("maxCat5", type, List.of("i", "j")));
    String catId6 = assertStatus(CREATED, postCategory("maxCat6", type, List.of("k", "l")));
    JsonWebMessage msg =
        postCategoryCombo("maxCat", type, List.of(catId1, catId2, catId3, catId4, catId5, catId6))
            .content(CONFLICT)
            .as(JsonWebMessage.class);
    assertContains(
        " cannot combine more than 5 categories, but had: 6",
        msg.getResponse().getArray("errorReports").getObject(0).getString("message").string());
  }

  @Test
  void testMaxCombinationsLimit() {
    DataDimensionType type = DISAGGREGATION;
    List<String> options1 = List.of("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8");
    List<String> options2 = List.of("b1", "b2", "b3", "b4", "b5", "b6", "b7", "b8");
    List<String> options3 = List.of("c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8");
    String catId1 = assertStatus(CREATED, postCategory("maxComb1", type, options1));
    String catId2 = assertStatus(CREATED, postCategory("maxComb2", type, options2));
    String catId3 = assertStatus(CREATED, postCategory("maxComb3", type, options3));
    JsonWebMessage msg =
        postCategoryCombo("maxComb", type, List.of(catId1, catId2, catId3))
            .content(CONFLICT)
            .as(JsonWebMessage.class);
    assertContains(
        " cannot have more than 500 combinations, but requires: 512",
        msg.getResponse().getArray("errorReports").getObject(0).getString("message").string());
  }
}
