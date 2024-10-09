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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.DataMergeStrategy;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * All the tests in this class basically test the same thing:
 *
 * <p>- Create metadata which have source CategoryOption references
 *
 * <p>- Perform a CategoryOption merge, passing a target CategoryOption
 *
 * <p>- Check that source CategoryOptions have had their references removed/replaced with the target
 * CategoryOption
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class CategoryOptionMergeProcessorTest extends PostgresIntegrationTestBase {

  @Autowired private CategoryService categoryService;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private CategoryOptionMergeProcessor mergeProcessor;

  private CategoryOption coSource1;
  private CategoryOption coSource2;
  private CategoryOption coTarget;
  private CategoryOption coRandom;

  @BeforeEach
  public void setUp() {
    coSource1 = createCategoryOption('A');
    coSource2 = createCategoryOption('B');
    coTarget = createCategoryOption('T');
    coRandom = createCategoryOption('R');
    categoryService.addCategoryOption(coSource1);
    categoryService.addCategoryOption(coSource2);
    categoryService.addCategoryOption(coTarget);
    categoryService.addCategoryOption(coRandom);
  }

  @Test
  @DisplayName("Ensure setup data is present in system")
  void ensureDataIsPresentInSystem() {
    // given setup is complete
    // when trying to retrieve data
    List<CategoryOption> categoryOptions = categoryService.getAllCategoryOptions();

    // then 5 are expected (4 created + 1 default)
    assertEquals(5, categoryOptions.size());
  }

  // -----------------------------
  // --------- Category ----------
  // -----------------------------
  @Test
  @DisplayName("Category refs to source CategoryOptions are replaced, sources not deleted")
  void categoryRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given
    Category c1 = createCategory('1', coSource1);
    Category c2 = createCategory('2', coSource2);
    Category c3 = createCategory('3', coTarget);
    Category c4 = createCategory('4', coRandom);

    manager.save(List.of(c1, c2, c3, c4));

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Category> categorySources =
        categoryService.getCategoriesByCategoryOption(List.of(coSource1, coSource2));
    List<Category> categoryTarget =
        categoryService.getCategoriesByCategoryOption(List.of(coTarget));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertMergeSuccessfulSourcesNotDeleted(
        report, categorySources, categoryTarget, allCategoryOptions);
  }

  @Test
  @DisplayName("Category refs to source CategoryOptions are replaced, sources are deleted")
  void categoryRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given
    Category c1 = createCategory('1', coSource1);
    c1.addCategoryOption(coSource2);
    Category c2 = createCategory('2', coSource2);
    Category c3 = createCategory('3', coTarget);
    Category c4 = createCategory('4', coRandom);

    manager.save(List.of(c1, c2, c3, c4));

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Category> sourceCategories =
        categoryService.getCategoriesByCategoryOption(List.of(coSource1, coSource2));
    List<Category> targetCategories =
        categoryService.getCategoriesByCategoryOption(List.of(coTarget));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertMergeSuccessfulSourcesDeleted(
        report, sourceCategories, targetCategories, allCategoryOptions);
  }

  private MergeParams getMergeParams() {
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(List.of(coSource1.getUid(), coSource2.getUid())));
    mergeParams.setTarget(UID.of(coTarget.getUid()));
    mergeParams.setDataMergeStrategy(DataMergeStrategy.LAST_UPDATED);
    return mergeParams;
  }

  private void assertMergeSuccessfulSourcesNotDeleted(
      MergeReport report,
      Collection<?> sources,
      Collection<?> target,
      Collection<CategoryOption> categoryOptions) {
    assertFalse(report.hasErrorMessages());
    assertEquals(0, sources.size(), "Expect 0 entries with source category option refs");
    assertEquals(3, target.size(), "Expect 3 entries with target category option refs");
    assertEquals(5, categoryOptions.size(), "Expect 5 category options present");
    assertTrue(categoryOptions.containsAll(List.of(coTarget, coSource1, coSource2)));
  }

  private void assertMergeSuccessfulSourcesDeleted(
      MergeReport report,
      Collection<?> sources,
      Collection<?> target,
      Collection<CategoryOption> categoryOptions) {
    assertFalse(report.hasErrorMessages());
    assertEquals(0, sources.size(), "Expect 0 entries with source category option refs");
    assertEquals(3, target.size(), "Expect 3 entries with target category option refs");
    assertEquals(3, categoryOptions.size(), "Expect 3 category options present");
    assertTrue(categoryOptions.contains(coTarget));
  }
}
