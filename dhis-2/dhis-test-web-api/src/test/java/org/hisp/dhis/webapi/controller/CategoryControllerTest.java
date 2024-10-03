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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonCategory;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoryControllerTest extends DhisControllerConvenienceTest {

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
    JsonArray categories =
        GET("/categories").content(org.hisp.dhis.web.HttpStatus.OK).getArray("categories");

    assertEquals(
        Set.of("CategoryA", "CategoryB", "CategoryC", "default"),
        categories.asList(JsonObject.class).stream()
            .map(jde -> jde.as(JsonCategory.class))
            .map(JsonIdentifiableObject::getDisplayName)
            .collect(Collectors.toSet()),
        "Returned category IDs include custom categories and default category");
  }

  @Test
  @DisplayName("Default Category should not be present in payload when EXCLUDE defaults")
  void categoriesExcludingDefaultTest() {
    JsonArray categories =
        GET("/categories?defaults=EXCLUDE")
            .content(org.hisp.dhis.web.HttpStatus.OK)
            .getArray("categories");

    Set<String> catDisplayNames =
        categories.asList(JsonObject.class).stream()
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
            .content(org.hisp.dhis.web.HttpStatus.OK)
            .getArray("categories");

    Set<String> catOptions =
        categories.asList(JsonObject.class).stream()
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
            .content(org.hisp.dhis.web.HttpStatus.OK)
            .getArray("categories");

    Set<String> catOptions =
        categories.asList(JsonObject.class).stream()
            .map(jc -> jc.as(JsonCategory.class))
            .flatMap(jc -> jc.getCategoryOptions().stream())
            .map(JsonIdentifiableObject::getName)
            .collect(Collectors.toSet());

    assertEquals(
        Set.of("CategoryOptionP", "default"),
        catOptions,
        "category options equal custom cat option and default category option");
  }
}
