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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonCategoryOption;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoryOptionControllerTest extends DhisControllerConvenienceTest {

  @Autowired private CategoryService categoryService;

  @BeforeEach
  void setUp() {
    CategoryOption catOptA = createCategoryOption('A');
    CategoryOption catOptB = createCategoryOption('B');
    CategoryOption catOptC = createCategoryOption('C');
    categoryService.addCategoryOption(catOptA);
    categoryService.addCategoryOption(catOptB);
    categoryService.addCategoryOption(catOptC);
  }

  @Test
  @DisplayName(
      "Default CategoryOption should be present in payload when defaults are INCLUDE by default")
  void getAllCatOptionsIncludingDefaultsTest() {
    JsonArray categoryOptions =
        GET("/categoryOptions")
            .content(org.hisp.dhis.web.HttpStatus.OK)
            .getArray("categoryOptions");

    assertTrue(
        Set.of("CategoryOptionA", "CategoryOptionB", "CategoryOptionC", "default")
            .containsAll(
                categoryOptions.asList(JsonObject.class).stream()
                    .map(jco -> jco.as(JsonCategoryOption.class))
                    .map(JsonIdentifiableObject::getDisplayName)
                    .collect(Collectors.toSet())),
        "Returned catOptions include custom catOptions and default catOption");
  }

  @Test
  @DisplayName("Default CategoryOption should not be present in payload when EXCLUDE defaults")
  void catOptionsExcludingDefaultTest() {
    JsonArray categoryOptions =
        GET("/categoryOptions?defaults=EXCLUDE")
            .content(org.hisp.dhis.web.HttpStatus.OK)
            .getArray("categoryOptions");

    Set<String> catOptions =
        categoryOptions.asList(JsonObject.class).stream()
            .map(jco -> jco.as(JsonCategoryOption.class))
            .map(JsonIdentifiableObject::getDisplayName)
            .collect(Collectors.toSet());

    assertTrue(
        Set.of("CategoryOptionA", "CategoryOptionB", "CategoryOptionC").containsAll(catOptions),
        "Returned catOptions include custom catOptions only");

    assertFalse(catOptions.contains("default"), "default catOption was not in payload");
  }
}
