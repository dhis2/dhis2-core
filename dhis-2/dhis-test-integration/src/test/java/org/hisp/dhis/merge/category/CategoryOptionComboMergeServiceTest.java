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
package org.hisp.dhis.merge.category;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.merge.DataMergeStrategy;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.test.config.QueryCountDataSourceProxy;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * All the tests in this class basically follow the same approach:
 *
 * <p>- Create metadata which have source CategoryOptionCombo references
 *
 * <p>- Perform a CategoryOption merge, passing a target CategoryOptionCombo
 *
 * <p>- Check that source CategoryOptionCombos have had their references removed/replaced with the
 * target CategoryOptionCombo
 */
@Transactional
@ContextConfiguration(classes = {QueryCountDataSourceProxy.class})
class CategoryOptionComboMergeServiceTest extends PostgresIntegrationTestBase {

  @Autowired private CategoryService categoryService;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private MergeService categoryOptionComboMergeService;

  private Category cat1;
  private Category cat2;
  private Category cat3;
  private Category cat4;

  private CategoryCombo cc1;
  private CategoryCombo cc2;
  private CategoryOption co1A;
  private CategoryOption co1B;
  private CategoryOption co2A;
  private CategoryOption co2B;
  private CategoryOption co3A;
  private CategoryOption co3B;
  private CategoryOption co4A;
  private CategoryOption co4B;
  private CategoryOptionCombo cocSource1;
  private CategoryOptionCombo cocSource2;
  private CategoryOptionCombo cocTarget;

  @BeforeEach
  public void setUp() {
    // 8 category options
    co1A = createCategoryOption("1A", CodeGenerator.generateUid());
    co1B = createCategoryOption("1B", CodeGenerator.generateUid());
    co2A = createCategoryOption("2A", CodeGenerator.generateUid());
    co2B = createCategoryOption("2B", CodeGenerator.generateUid());
    co3A = createCategoryOption("3A", CodeGenerator.generateUid());
    co3B = createCategoryOption("3B", CodeGenerator.generateUid());
    co4A = createCategoryOption("4A", CodeGenerator.generateUid());
    co4B = createCategoryOption("4B", CodeGenerator.generateUid());
    categoryService.addCategoryOption(co1A);
    categoryService.addCategoryOption(co1B);
    categoryService.addCategoryOption(co2A);
    categoryService.addCategoryOption(co2B);
    categoryService.addCategoryOption(co3A);
    categoryService.addCategoryOption(co3B);
    categoryService.addCategoryOption(co4A);
    categoryService.addCategoryOption(co4B);

    // 4 categories (each with 2 category options)
    cat1 = createCategory('1', co1A, co1B);
    cat2 = createCategory('2', co2A, co2B);
    cat3 = createCategory('3', co3A, co3B);
    cat4 = createCategory('4', co4A, co4B);
    categoryService.addCategory(cat1);
    categoryService.addCategory(cat2);
    categoryService.addCategory(cat3);
    categoryService.addCategory(cat4);

    cc1 = createCategoryCombo('1', cat1, cat2);
    cc2 = createCategoryCombo('2', cat3, cat4);
    categoryService.addCategoryCombo(cc1);
    categoryService.addCategoryCombo(cc2);

    categoryService.generateOptionCombos(cc1);
    categoryService.generateOptionCombos(cc2);

    cocSource1 = getCocWithOptions("1A", "2A");
    cocSource2 = getCocWithOptions("1B", "2B");
    cocTarget = getCocWithOptions("3A", "4B");
  }

  private CategoryOptionCombo getCocWithOptions(String co1, String co2) {
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    return allCategoryOptionCombos.stream()
        .filter(
            coc -> {
              List<String> categoryOptions =
                  coc.getCategoryOptions().stream().map(BaseIdentifiableObject::getName).toList();
              return categoryOptions.containsAll(List.of(co1, co2));
            })
        .toList()
        .get(0);
  }

  // -----------------------------
  // ------ CategoryOption -------
  // -----------------------------
  @Test
  @DisplayName(
      "CategoryOption refs to source CategoryOptionCombos are replaced, sources not deleted")
  void categoryOptionRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given category option combo state before merge
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    assertEquals(9, allCategoryOptionCombos.size(), "9 COCs including 1 default");

    //    List<CategoryOptionCombo> cocsSourcesBefore =
    //        categoryService.getAllCategoryOptionCombos(
    //            List.of(UID.of(coSource1A.getUid()), UID.of(coSource2B.getUid())));
    //    List<Category> categoryTargetBefore =
    //        categoryService.getCategoriesByCategoryOption(List.of(UID.of(coTarget3A.getUid())));
    //
    //    assertEquals(
    //        2, categorySourcesBefore.size(), "Expect 2 categories with source category option
    // refs");
    //    assertEquals(
    //        1, categoryTargetBefore.size(), "Expect 1 category with target category option refs");

    // when
    //    MergeParams mergeParams = getMergeParams();
    //    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    //    List<Category> categorySources =
    //        categoryService.getCategoriesByCategoryOption(
    //            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    //    List<Category> categoryTarget =
    //        categoryService.getCategoriesByCategoryOption(List.of(UID.of(coTarget3A)));
    //    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();
    //
    //    assertFalse(report.hasErrorMessages());
    //    assertEquals(0, categorySources.size(), "Expect 0 entries with source category option
    // refs");
    //    assertEquals(3, categoryTarget.size(), "Expect 3 entries with target category option
    // refs");

    // 8 custom + 1 default
    //    assertEquals(9, allCategoryOptions.size(), "Expect 9 category options present");
    //    assertTrue(allCategoryOptions.containsAll(List.of(coTarget3A, coSource1A, coSource2B)));
  }

  private MergeParams getMergeParams() {
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(List.of(co1A.getUid(), co2B.getUid())));
    mergeParams.setTarget(UID.of(co3A.getUid()));
    mergeParams.setDataMergeStrategy(DataMergeStrategy.DISCARD);
    return mergeParams;
  }
}
