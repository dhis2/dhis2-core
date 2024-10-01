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
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.test.web.HttpStatus;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonCategory;
import org.hisp.dhis.test.webapi.json.domain.JsonIdentifiableObject;
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
}
