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
package org.hisp.dhis.dataelement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DataDimensionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class CategoryComboTest {

  private CategoryOption categoryOptionA;

  private CategoryOption categoryOptionB;

  private CategoryOption categoryOptionC;

  private CategoryOption categoryOptionD;

  private CategoryOption categoryOptionE;

  private CategoryOption categoryOptionF;

  private Category categoryA;

  private Category categoryB;

  private Category categoryC;

  private CategoryCombo categoryCombo;

  // -------------------------------------------------------------------------
  // Fixture
  // -------------------------------------------------------------------------
  @BeforeEach
  void before() {
    categoryOptionA = new CategoryOption("OptionA");
    categoryOptionB = new CategoryOption("OptionB");
    categoryOptionC = new CategoryOption("OptionC");
    categoryOptionD = new CategoryOption("OptionD");
    categoryOptionE = new CategoryOption("OptionE");
    categoryOptionF = new CategoryOption("OptionF");
    categoryA = new Category("CategoryA", DataDimensionType.DISAGGREGATION);
    categoryB = new Category("CategoryB", DataDimensionType.DISAGGREGATION);
    categoryC = new Category("CategoryC", DataDimensionType.DISAGGREGATION);
    categoryA.getCategoryOptions().add(categoryOptionA);
    categoryA.getCategoryOptions().add(categoryOptionB);
    categoryB.getCategoryOptions().add(categoryOptionC);
    categoryB.getCategoryOptions().add(categoryOptionD);
    categoryC.getCategoryOptions().add(categoryOptionE);
    categoryC.getCategoryOptions().add(categoryOptionF);
    categoryOptionA.getCategories().add(categoryA);
    categoryOptionB.getCategories().add(categoryA);
    categoryOptionC.getCategories().add(categoryB);
    categoryOptionD.getCategories().add(categoryB);
    categoryOptionE.getCategories().add(categoryC);
    categoryOptionF.getCategories().add(categoryC);
    categoryCombo = new CategoryCombo("CategoryCombo", DataDimensionType.DISAGGREGATION);
    categoryCombo.getCategories().add(categoryA);
    categoryCombo.getCategories().add(categoryB);
    categoryCombo.getCategories().add(categoryC);
  }

  @Test
  void testGenerateOptionCombosList() {
    List<CategoryOptionCombo> list = categoryCombo.generateOptionCombosList();
    assertNotNull(list);
    assertEquals(8, list.size());
    assertEquals(
        createCategoryOptionCombo(categoryCombo, categoryOptionA, categoryOptionC, categoryOptionE),
        list.get(0));
    assertEquals(
        createCategoryOptionCombo(categoryCombo, categoryOptionA, categoryOptionC, categoryOptionF),
        list.get(1));
    assertEquals(
        createCategoryOptionCombo(categoryCombo, categoryOptionA, categoryOptionD, categoryOptionE),
        list.get(2));
    assertEquals(
        createCategoryOptionCombo(categoryCombo, categoryOptionA, categoryOptionD, categoryOptionF),
        list.get(3));
    assertEquals(
        createCategoryOptionCombo(categoryCombo, categoryOptionB, categoryOptionC, categoryOptionE),
        list.get(4));
    assertEquals(
        createCategoryOptionCombo(categoryCombo, categoryOptionB, categoryOptionC, categoryOptionF),
        list.get(5));
    assertEquals(
        createCategoryOptionCombo(categoryCombo, categoryOptionB, categoryOptionD, categoryOptionE),
        list.get(6));
    assertEquals(
        createCategoryOptionCombo(categoryCombo, categoryOptionB, categoryOptionD, categoryOptionF),
        list.get(7));
  }

  @Test
  void test() {
    List<CategoryOptionCombo> list = categoryCombo.generateOptionCombosList();
    categoryCombo.generateOptionCombos();
    assertEquals(list, categoryCombo.getSortedOptionCombos());
  }

  private static CategoryOptionCombo createCategoryOptionCombo(
      CategoryCombo categoryCombo, CategoryOption... categoryOptions) {
    CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
    categoryOptionCombo.setCategoryCombo(categoryCombo);
    for (CategoryOption categoryOption : categoryOptions) {
      categoryOptionCombo.getCategoryOptions().add(categoryOption);
    }
    return categoryOptionCombo;
  }
}
