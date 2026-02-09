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
package org.hisp.dhis.merge.category.categorycombo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.feedback.MergeReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoryComboMergeServiceTest {

  @Test
  @DisplayName("When source and target CategoryCombos have identical Categories, no error reported")
  void identicalCategoriesPassTest() {
    // given 2 categories
    Category cat1 = createCategory("cat1");
    Category cat2 = createCategory("cat2");

    // and source CategoryCombo with these categories
    CategoryCombo source = createCategoryCombo("source", cat1, cat2);

    // and target CategoryCombo with the same categories
    CategoryCombo target = createCategoryCombo("target", cat1, cat2);

    MergeReport mergeReport = new MergeReport();

    // when
    CategoryComboMergeService.validateIdenticalCategories(List.of(source), target, mergeReport);

    // then no error is expected
    assertFalse(mergeReport.hasErrorMessages());
  }

  @Test
  @DisplayName(
      "When source CategoryCombo has different Categories than target, error E1545 reported")
  void differentCategoriesFailTest() {
    // given different categories for source and target
    Category cat1 = createCategory("cat1");
    Category cat2 = createCategory("cat2");
    Category cat3 = createCategory("cat3");

    CategoryCombo source = createCategoryCombo("source", cat1, cat2);
    CategoryCombo target = createCategoryCombo("target", cat1, cat3);

    MergeReport mergeReport = new MergeReport();

    // when
    CategoryComboMergeService.validateIdenticalCategories(List.of(source), target, mergeReport);

    // then error is expected
    assertTrue(mergeReport.hasErrorMessages());
    assertEquals(1, mergeReport.getMergeErrors().size());
    assertTrue(
        mergeReport
            .getMergeErrors()
            .get(0)
            .getMessage()
            .contains("Source and target CategoryCombos must have identical Categories"));
  }

  @Test
  @DisplayName(
      "When CategoryOptionCombo has correct cardinality (one option per category), no error")
  void correctCocCardinalityPassTest() {
    // given categories with options
    CategoryOption co1 = createCategoryOption("co1");
    CategoryOption co2 = createCategoryOption("co2");
    Category cat1 = createCategory("cat1");
    Category cat2 = createCategory("cat2");
    cat1.addCategoryOption(co1);
    cat2.addCategoryOption(co2);

    // source combo with COC having 2 options (correct for 2 categories)
    CategoryCombo source = createCategoryCombo("source", cat1, cat2);
    CategoryOptionCombo coc = createCategoryOptionCombo("coc1", source, co1, co2);
    source.setOptionCombos(Set.of(coc));

    CategoryCombo target = createCategoryCombo("target", cat1, cat2);

    MergeReport mergeReport = new MergeReport();

    // when
    CategoryComboMergeService.validateCategoryOptionCombos(List.of(source), target, mergeReport);

    // then no error is expected
    assertFalse(mergeReport.hasErrorMessages());
  }

  @Test
  @DisplayName(
      "When CategoryOptionCombo has incorrect cardinality (wrong number of options), error E1546 reported")
  void incorrectCocCardinalityFailTest() {
    // given categories with options
    CategoryOption co1 = createCategoryOption("co1");
    CategoryOption co2 = createCategoryOption("co2");
    Category cat1 = createCategory("cat1");
    Category cat2 = createCategory("cat2");
    cat1.addCategoryOption(co1);
    cat2.addCategoryOption(co2);

    // source combo with COC having only 1 option (incorrect for 2 categories)
    CategoryCombo source = createCategoryCombo("source", cat1, cat2);
    CategoryOptionCombo coc = createCategoryOptionCombo("coc1", source, co1);
    source.setOptionCombos(Set.of(coc));

    CategoryCombo target = createCategoryCombo("target", cat1, cat2);

    MergeReport mergeReport = new MergeReport();

    // when
    CategoryComboMergeService.validateCategoryOptionCombos(List.of(source), target, mergeReport);

    // then error is expected
    assertTrue(mergeReport.hasErrorMessages());
    assertEquals(1, mergeReport.getMergeErrors().size());
    assertTrue(
        mergeReport
            .getMergeErrors()
            .get(0)
            .getMessage()
            .contains("CategoryOptionCombo has incorrect number of CategoryOptions"));
  }

  @Test
  @DisplayName(
      "When CategoryOptionCombo has CategoryOptions not in any Category, error E1547 reported")
  void invalidCategoryOptionsFailTest() {
    // given categories with specific options
    CategoryOption co1 = createCategoryOption("co1");
    CategoryOption co2 = createCategoryOption("co2");
    CategoryOption coInvalid = createCategoryOption("coInvalid");
    Category cat1 = createCategory("cat1");
    Category cat2 = createCategory("cat2");
    cat1.addCategoryOption(co1);
    cat2.addCategoryOption(co2);

    // source combo with COC having an invalid option
    CategoryCombo source = createCategoryCombo("source", cat1, cat2);
    CategoryOptionCombo coc = createCategoryOptionCombo("coc1", source, co1, coInvalid);
    source.setOptionCombos(Set.of(coc));

    CategoryCombo target = createCategoryCombo("target", cat1, cat2);

    MergeReport mergeReport = new MergeReport();

    // when
    CategoryComboMergeService.validateCategoryOptionCombos(List.of(source), target, mergeReport);

    // then error is expected
    assertTrue(mergeReport.hasErrorMessages());
    assertTrue(
        mergeReport.getMergeErrors().stream()
            .anyMatch(
                e ->
                    e.getMessage()
                        .contains("CategoryOptionCombo has CategoryOptions that are not valid")));
  }

  @Test
  @DisplayName("Multiple sources with different categories generate multiple errors")
  void multipleSourcesDifferentCategoriesTest() {
    // given
    Category cat1 = createCategory("cat1");
    Category cat2 = createCategory("cat2");
    Category cat3 = createCategory("cat3");

    CategoryCombo source1 = createCategoryCombo("source1", cat1);
    CategoryCombo source2 = createCategoryCombo("source2", cat2);
    CategoryCombo target = createCategoryCombo("target", cat3);

    MergeReport mergeReport = new MergeReport();

    // when
    CategoryComboMergeService.validateIdenticalCategories(
        List.of(source1, source2), target, mergeReport);

    // then 2 errors expected (one for each source)
    assertTrue(mergeReport.hasErrorMessages());
    assertEquals(2, mergeReport.getMergeErrors().size());
  }

  // Helper methods

  private Category createCategory(String name) {
    Category category = new Category();
    category.setUid(CodeGenerator.generateUid());
    category.setName(name);
    return category;
  }

  private CategoryOption createCategoryOption(String name) {
    CategoryOption option = new CategoryOption();
    option.setUid(CodeGenerator.generateUid());
    option.setName(name);
    return option;
  }

  private CategoryCombo createCategoryCombo(String name, Category... categories) {
    CategoryCombo combo = new CategoryCombo();
    combo.setUid(CodeGenerator.generateUid());
    combo.setName(name);
    for (Category category : categories) {
      combo.addCategory(category);
    }
    return combo;
  }

  private CategoryOptionCombo createCategoryOptionCombo(
      String name, CategoryCombo combo, CategoryOption... options) {
    CategoryOptionCombo coc = new CategoryOptionCombo();
    coc.setUid(CodeGenerator.generateUid());
    coc.setName(name);
    coc.setCategoryCombo(combo);
    for (CategoryOption option : options) {
      coc.getCategoryOptions().add(option);
    }
    return coc;
  }
}
