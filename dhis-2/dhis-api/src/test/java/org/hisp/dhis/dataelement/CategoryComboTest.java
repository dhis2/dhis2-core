/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.dataelement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
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
  void testGenerateOptionCombosSetNamesCombosInCategoryOrder() {
    Set<CategoryOptionCombo> set = categoryCombo.generateOptionCombosSet();

    assertTrue(
        set.stream().anyMatch(coc -> "OptionB, OptionC, OptionF".equals(coc.getName())),
        "option combo names must list the option names in category order");
    set.forEach(coc -> assertEquals(computedName(coc), coc.getName()));
  }

  @Test
  void testGenerateOptionCombosSetNamesMatchComputedNamesWithSharedOption() {
    // an option shared by two categories appears once per category in a computed name
    Category categoryD = new Category("CategoryD", DataDimensionType.DISAGGREGATION);
    categoryD.getCategoryOptions().add(categoryOptionA);
    categoryD.getCategoryOptions().add(categoryOptionC);
    CategoryCombo shared = new CategoryCombo("Shared", DataDimensionType.DISAGGREGATION);
    shared.getCategories().add(categoryA);
    shared.getCategories().add(categoryD);

    Set<CategoryOptionCombo> set = shared.generateOptionCombosSet();

    assertEquals(4, set.size());
    assertTrue(set.stream().anyMatch(coc -> "OptionA, OptionA".equals(coc.getName())));
    set.forEach(coc -> assertEquals(computedName(coc), coc.getName()));
  }

  /** The name an unnamed option combo computes for itself from its combo's categories. */
  private static String computedName(CategoryOptionCombo coc) {
    CategoryOptionCombo unnamed = new CategoryOptionCombo();
    unnamed.setCategoryCombo(coc.getCategoryCombo());
    unnamed.setCategoryOptions(coc.getCategoryOptions());
    return unnamed.getName();
  }

  @Test
  void testGenerateOptionCombosSet() {
    Set<CategoryOptionCombo> set = categoryCombo.generateOptionCombosSet();
    assertNotNull(set);
    assertEquals(8, set.size());
    assertTrue(
        set.contains(
            createCategoryOptionCombo(
                categoryCombo, categoryOptionA, categoryOptionC, categoryOptionE)));
    assertTrue(
        set.contains(
            createCategoryOptionCombo(
                categoryCombo, categoryOptionA, categoryOptionC, categoryOptionF)));
    assertTrue(
        set.contains(
            createCategoryOptionCombo(
                categoryCombo, categoryOptionA, categoryOptionD, categoryOptionE)));
    assertTrue(
        set.contains(
            createCategoryOptionCombo(
                categoryCombo, categoryOptionA, categoryOptionD, categoryOptionF)));
    assertTrue(
        set.contains(
            createCategoryOptionCombo(
                categoryCombo, categoryOptionB, categoryOptionC, categoryOptionE)));
    assertTrue(
        set.contains(
            createCategoryOptionCombo(
                categoryCombo, categoryOptionB, categoryOptionC, categoryOptionF)));
    assertTrue(
        set.contains(
            createCategoryOptionCombo(
                categoryCombo, categoryOptionB, categoryOptionD, categoryOptionE)));
    assertTrue(
        set.contains(
            createCategoryOptionCombo(
                categoryCombo, categoryOptionB, categoryOptionD, categoryOptionF)));
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
