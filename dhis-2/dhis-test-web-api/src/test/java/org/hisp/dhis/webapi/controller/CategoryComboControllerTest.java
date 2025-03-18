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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonCategoryCombo;
import org.hisp.dhis.test.webapi.json.domain.JsonIdentifiableObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoryComboControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private CategoryService categoryService;

  @BeforeEach
  void setUp() {
    CategoryCombo catComboA = createCategoryCombo('A');
    CategoryCombo catComboB = createCategoryCombo('B');
    CategoryCombo catComboC = createCategoryCombo('C');
    categoryService.addCategoryCombo(catComboA);
    categoryService.addCategoryCombo(catComboB);
    categoryService.addCategoryCombo(catComboC);
  }

  @Test
  @DisplayName(
      "Default CategoryCombo should be present in payload when defaults are INCLUDE by default")
  void getAllCatCombosIncludingDefaultsTest() {
    JsonArray categoryCombos =
        GET("/categoryCombos").content(HttpStatus.OK).getArray("categoryCombos");

    assertEquals(
        Set.of("CategoryComboC", "CategoryComboB", "CategoryComboA", "default"),
        categoryCombos.stream()
            .map(jcc -> jcc.as(JsonCategoryCombo.class))
            .map(JsonIdentifiableObject::getDisplayName)
            .collect(Collectors.toSet()),
        "Returned catCombos include custom catCombos and default catCombo");
  }

  @Test
  @DisplayName("Default CategoryCombo should not be present in payload when EXCLUDE defaults")
  void catCombosExcludingDefaultTest() {
    JsonArray categories =
        GET("/categoryCombos?defaults=EXCLUDE").content(HttpStatus.OK).getArray("categoryCombos");

    Set<String> catCombos =
        categories.stream()
            .map(jcc -> jcc.as(JsonCategoryCombo.class))
            .map(JsonIdentifiableObject::getDisplayName)
            .collect(Collectors.toSet());

    assertEquals(
        Set.of("CategoryComboC", "CategoryComboB", "CategoryComboA"),
        catCombos,
        "Returned catCombos include custom catCombos only");
  }

  @Test
  @DisplayName(
      "A CategoryCombo with default Category should not have the category field present when EXCLUDE defaults")
  void catCombosExcludingDefaultCategoryTest() {
    Category customCat = createCategory('G');
    categoryService.addCategory(customCat);
    CategoryCombo catComboWithCustomCategory = createCategoryCombo('1', customCat);
    CategoryCombo catComboWithDefaultCategory = createCategoryCombo('2');
    categoryService.addCategoryCombo(catComboWithCustomCategory);
    categoryService.addCategoryCombo(catComboWithDefaultCategory);

    JsonArray categoryCombos =
        GET("/categoryCombos?fields=categories[name,id]&defaults=EXCLUDE")
            .content(HttpStatus.OK)
            .getArray("categoryCombos");

    Set<String> catCombos =
        categoryCombos.stream()
            .map(jcc -> jcc.as(JsonCategoryCombo.class))
            .flatMap(jcc -> jcc.getCategories().stream())
            .map(JsonIdentifiableObject::getName)
            .collect(Collectors.toSet());

    assertEquals(
        Set.of("CategoryG"),
        catCombos,
        "category equals custom category only, no default category present");
  }

  @Test
  @DisplayName(
      "A CategoryCombo with default Category should have the category field present when INCLUDE defaults")
  void catCombosIncludingDefaultCategoryTest() {
    Category customCat = createCategory('G');
    categoryService.addCategory(customCat);
    CategoryCombo catComboWithCustomCategory = createCategoryCombo('1', customCat);
    CategoryCombo catComboWithDefaultCategory = createCategoryCombo('2');
    categoryService.addCategoryCombo(catComboWithCustomCategory);
    categoryService.addCategoryCombo(catComboWithDefaultCategory);

    JsonArray categoryCombos =
        GET("/categoryCombos?fields=categories[name,id]&defaults=INCLUDE")
            .content(HttpStatus.OK)
            .getArray("categoryCombos");

    Set<String> catCombos =
        categoryCombos.stream()
            .map(jcc -> jcc.as(JsonCategoryCombo.class))
            .flatMap(jcc -> jcc.getCategories().stream())
            .map(JsonIdentifiableObject::getName)
            .collect(Collectors.toSet());

    assertEquals(
        Set.of("CategoryG", "default"),
        catCombos,
        "categories equal custom category and default category");
  }

  @Test
  @DisplayName(
      "A CategoryCombo with default CategoryOptionCombo should not have the categoryOptionCombo field present when EXCLUDE defaults")
  void catExcludingDefaultCatOptionTest() {
    CategoryCombo catCombo1 = createCategoryCombo('1');
    categoryService.addCategoryCombo(catCombo1);
    CategoryOption catOpt1 = createCategoryOption('1');
    categoryService.addCategoryOption(catOpt1);
    CategoryOptionCombo customCoc =
        createCategoryOptionCombo("CatOptCombo A", "CocUid0001", catCombo1, catOpt1);
    categoryService.addCategoryOptionCombo(customCoc);

    CategoryCombo catComboWithCustomCoc = createCategoryCombo('3');
    catComboWithCustomCoc.setOptionCombos(Set.of(customCoc));
    CategoryCombo catComboWithDefaultCoc = createCategoryCombo('4');
    categoryService.addCategoryCombo(catComboWithCustomCoc);
    categoryService.addCategoryCombo(catComboWithDefaultCoc);

    JsonArray catCombos =
        GET("/categoryCombos?fields=categoryOptionCombos[name,id]&defaults=EXCLUDE")
            .content(HttpStatus.OK)
            .getArray("categoryCombos");

    Set<String> catOptionCombos =
        catCombos.stream()
            .map(jc -> jc.as(JsonCategoryCombo.class))
            .flatMap(jc -> jc.getCategoryOptionCombos().stream())
            .map(JsonIdentifiableObject::getName)
            .collect(Collectors.toSet());

    assertEquals(
        Set.of("CatOptCombo A"),
        catOptionCombos,
        "category option combos equal custom cat option combo only, no default category option combo present");
  }

  @Test
  @DisplayName(
      "A CategoryCombo with default CategoryOptionCombo should have the categoryOptionCombo field present when INCLUDE defaults")
  void catIncludingDefaultCatOptionTest() {
    CategoryCombo catCombo1 = createCategoryCombo('1');
    categoryService.addCategoryCombo(catCombo1);
    CategoryOption catOpt1 = createCategoryOption('1');
    categoryService.addCategoryOption(catOpt1);
    CategoryOptionCombo customCoc =
        createCategoryOptionCombo("CatOptCombo A", "CocUid0001", catCombo1, catOpt1);
    categoryService.addCategoryOptionCombo(customCoc);

    CategoryCombo catComboWithCustomCoc = createCategoryCombo('3');
    catComboWithCustomCoc.setOptionCombos(Set.of(customCoc));
    CategoryCombo catComboWithDefaultCoc = createCategoryCombo('4');
    categoryService.addCategoryCombo(catComboWithCustomCoc);
    categoryService.addCategoryCombo(catComboWithDefaultCoc);

    JsonArray catCombos =
        GET("/categoryCombos?fields=categoryOptionCombos[name,id]&defaults=INCLUDE")
            .content(HttpStatus.OK)
            .getArray("categoryCombos");

    Set<String> catOptionCombos =
        catCombos.stream()
            .map(jc -> jc.as(JsonCategoryCombo.class))
            .flatMap(jc -> jc.getCategoryOptionCombos().stream())
            .map(JsonIdentifiableObject::getName)
            .collect(Collectors.toSet());

    assertEquals(
        Set.of("CatOptCombo A", "default"),
        catOptionCombos,
        "category option combos equal custom cat option combo and default category option combo");
  }
}
