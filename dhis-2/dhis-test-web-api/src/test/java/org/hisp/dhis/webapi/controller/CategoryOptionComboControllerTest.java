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
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonCategoryOptionCombo;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoryOptionComboControllerTest extends DhisControllerConvenienceTest {

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
        createCategoryOptionCombo("CatOptCombo A", "CocUid0001", catComboA, catOptA);
    CategoryOptionCombo cocB =
        createCategoryOptionCombo("CatOptCombo B", "CocUid0002", catComboB, catOptB);
    CategoryOptionCombo cocC =
        createCategoryOptionCombo("CatOptCombo C", "CocUid0003", catComboC, catOptC);
    categoryService.addCategoryOptionCombo(cocA);
    categoryService.addCategoryOptionCombo(cocB);
    categoryService.addCategoryOptionCombo(cocC);
  }

  @Test
  @DisplayName(
      "Default CategoryOptionCombo should be present in payload when defaults are INCLUDE by default")
  void getAllCatOptionCombosIncludingDefaultsTest() {
    JsonArray categoryCombos =
        GET("/categoryOptionCombos")
            .content(org.hisp.dhis.web.HttpStatus.OK)
            .getArray("categoryOptionCombos");

    assertEquals(
        Set.of("CatOptCombo C", "CatOptCombo B", "CatOptCombo A", "default"),
        categoryCombos.asList(JsonObject.class).stream()
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
            .content(org.hisp.dhis.web.HttpStatus.OK)
            .getArray("categoryOptionCombos");

    Set<String> catOptionComboNames =
        catOptionCombos.asList(JsonObject.class).stream()
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
}
