/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.dimension.DimensionController} using (mocked)
 * REST requests.
 *
 * @author Jan Bernitt
 */
class DimensionControllerTest extends DhisControllerConvenienceTest {

  @Autowired private CategoryService categoryService;

  private String ccId;

  @BeforeEach
  void setUp() {
    CategoryOption categoryOptionA = new CategoryOption("Male");
    CategoryOption categoryOptionB = new CategoryOption("Female");
    categoryService.addCategoryOption(categoryOptionA);
    categoryService.addCategoryOption(categoryOptionB);
    Category categoryA = new Category("Gender", DataDimensionType.DISAGGREGATION);
    categoryA.setShortName(categoryA.getName());
    categoryA.addCategoryOption(categoryOptionA);
    categoryA.addCategoryOption(categoryOptionB);
    categoryService.addCategory(categoryA);
    CategoryCombo categoryComboA = new CategoryCombo("Gender", DataDimensionType.DISAGGREGATION);
    categoryComboA.addCategory(categoryA);
    categoryService.addCategoryCombo(categoryComboA);
    ccId = categoryComboA.getUid();
  }

  @Test
  void testGetDimensionsForDataSet() {
    String dsId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'periodType':'Monthly', 'categoryCombo':{'id':'"
                    + ccId
                    + "'}}"));
    JsonObject response = GET("/dimensions/dataSet/{ds}", dsId).content();
    JsonArray dimensions = response.getArray("dimensions");
    assertEquals(1, dimensions.size());
    JsonObject gender = dimensions.getObject(0);
    assertEquals("Gender", gender.getString("name").string());
  }
}
