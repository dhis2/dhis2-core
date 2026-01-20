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
package org.hisp.dhis.merge.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.CategoryDimensionStore;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryComboStore;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionStore;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * All the tests in this class basically test the same thing:
 *
 * <p>- Create metadata which have source Category references
 *
 * <p>- Perform a Category merge, passing a target Category
 *
 * <p>- Check that source Categories have had their references removed/replaced with the target
 * Category
 */
@Transactional
class CategoryMergeServiceTest extends PostgresIntegrationTestBase {

  @Autowired private CategoryService categoryService;
  @Autowired private CategoryOptionStore categoryOptionStore;
  @Autowired private CategoryComboStore categoryComboStore;
  @Autowired private CategoryDimensionStore categoryDimensionStore;
  @Autowired private UserStore userStore;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private MergeService categoryMergeService;
  @Autowired private DbmsManager dbmsManager;

  // -----------------------------
  // ----- CategoryOption --------
  // -----------------------------
  @Test
  @DisplayName("CategoryOption refs to source Categories are replaced, sources not deleted")
  void categoryOptionRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given - create category options and categories
    CategoryOption co1 = createCategoryOption('a');
    CategoryOption co2 = createCategoryOption('b');
    CategoryOption co3 = createCategoryOption('c');
    categoryService.addCategoryOption(co1);
    categoryService.addCategoryOption(co2);
    categoryService.addCategoryOption(co3);

    Category catSource1 = createCategory('x', co1);
    Category catSource2 = createCategory('y', co2);
    Category catTarget = createCategory('z', co3);
    categoryService.addCategory(catSource1);
    categoryService.addCategory(catSource2);
    categoryService.addCategory(catTarget);

    // confirm category option state before merge
    List<CategoryOption> sourceCosBefore =
        categoryOptionStore.getCategoryOptions(Set.of(catSource1.getUid(), catSource2.getUid()));
    List<CategoryOption> targetCosBefore =
        categoryOptionStore.getCategoryOptions(Set.of(catTarget.getUid()));

    assertEquals(2, sourceCosBefore.size(), "Expect 2 category options with source category refs");
    assertEquals(1, targetCosBefore.size(), "Expect 1 category option with target category ref");

    // when
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    MergeReport report = categoryMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<CategoryOption> sourceCos =
        categoryOptionStore.getCategoryOptions(Set.of(catSource1.getUid(), catSource2.getUid()));
    List<CategoryOption> targetCos =
        categoryOptionStore.getCategoryOptions(Set.of(catTarget.getUid()));
    List<Category> allCategories = manager.getAll(Category.class);

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceCos.size(), "Expect 0 category options with source category refs");
    assertEquals(3, targetCos.size(), "Expect 3 category options with target category ref");

    // 3 custom + 1 default
    assertEquals(4, allCategories.size(), "Expect 4 categories present");
    assertTrue(allCategories.containsAll(List.of(catSource1, catSource2, catTarget)));
  }

  @Test
  @DisplayName("CategoryOption refs to source Categories are replaced, sources are deleted")
  void categoryOptionRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given - create category options and categories
    CategoryOption co1 = createCategoryOption('a');
    CategoryOption co2 = createCategoryOption('b');
    CategoryOption co3 = createCategoryOption('c');
    categoryService.addCategoryOption(co1);
    categoryService.addCategoryOption(co2);
    categoryService.addCategoryOption(co3);

    Category catSource1 = createCategory('x', co1);
    Category catSource2 = createCategory('y', co2);
    Category catTarget = createCategory('z', co3);
    categoryService.addCategory(catSource1);
    categoryService.addCategory(catSource2);
    categoryService.addCategory(catTarget);

    // confirm category option state before merge
    List<CategoryOption> sourceCosBefore =
        categoryOptionStore.getCategoryOptions(Set.of(catSource1.getUid(), catSource2.getUid()));
    List<CategoryOption> targetCosBefore =
        categoryOptionStore.getCategoryOptions(Set.of(catTarget.getUid()));

    assertEquals(2, sourceCosBefore.size(), "Expect 2 category options with source category refs");
    assertEquals(1, targetCosBefore.size(), "Expect 1 category option with target category ref");

    // when
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<CategoryOption> targetCos =
        categoryOptionStore.getCategoryOptions(Set.of(catTarget.getUid()));
    List<Category> allCategories = manager.getAll(Category.class);

    assertFalse(report.hasErrorMessages());
    assertEquals(3, targetCos.size(), "Expect 3 category options with target category ref");

    // 1 custom + 1 default (sources deleted)
    assertEquals(2, allCategories.size(), "Expect 2 categories present");
    assertTrue(allCategories.contains(catTarget));
    assertNull(
        categoryService.getCategory(catSource1.getUid()), "source category 1 should be deleted");
    assertNull(
        categoryService.getCategory(catSource2.getUid()), "source category 2 should be deleted");
  }

  // -----------------------------
  // ----- CategoryCombo ---------
  // -----------------------------
  @Test
  @DisplayName("CategoryCombo refs to source Categories are replaced, sources not deleted")
  void categoryComboRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given - create categories and category combos
    CategoryOption co1 = createCategoryOption('a');
    CategoryOption co2 = createCategoryOption('b');
    CategoryOption co3 = createCategoryOption('c');
    categoryService.addCategoryOption(co1);
    categoryService.addCategoryOption(co2);
    categoryService.addCategoryOption(co3);

    Category catSource1 = createCategory('x', co1);
    Category catSource2 = createCategory('y', co2);
    Category catTarget = createCategory('z', co3);
    categoryService.addCategory(catSource1);
    categoryService.addCategory(catSource2);
    categoryService.addCategory(catTarget);

    CategoryCombo cc1 = createCategoryCombo('1', catSource1);
    CategoryCombo cc2 = createCategoryCombo('2', catSource2);
    CategoryCombo cc3 = createCategoryCombo('3', catTarget);
    categoryService.addCategoryCombo(cc1);
    categoryService.addCategoryCombo(cc2);
    categoryService.addCategoryCombo(cc3);

    // confirm category combo state before merge
    List<CategoryCombo> sourceCombosBefore =
        categoryComboStore.getCategoryCombos(Set.of(catSource1.getUid(), catSource2.getUid()));
    List<CategoryCombo> targetCombosBefore =
        categoryComboStore.getCategoryCombos(Set.of(catTarget.getUid()));

    assertEquals(
        2, sourceCombosBefore.size(), "Expect 2 category combos with source category refs");
    assertEquals(1, targetCombosBefore.size(), "Expect 1 category combo with target category ref");

    // when
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    MergeReport report = categoryMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<CategoryCombo> sourceCombos =
        categoryComboStore.getCategoryCombos(Set.of(catSource1.getUid(), catSource2.getUid()));
    List<CategoryCombo> targetCombos =
        categoryComboStore.getCategoryCombos(Set.of(catTarget.getUid()));
    List<Category> allCategories = manager.getAll(Category.class);

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceCombos.size(), "Expect 0 category combos with source category refs");
    assertEquals(3, targetCombos.size(), "Expect 3 category combos with target category ref");

    // 3 custom + 1 default
    assertEquals(4, allCategories.size(), "Expect 4 categories present");
    assertTrue(allCategories.containsAll(List.of(catSource1, catSource2, catTarget)));
  }

  @Test
  @DisplayName("CategoryCombo refs to source Categories are replaced, sources are deleted")
  void categoryComboRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given - create categories and category combos
    CategoryOption co1 = createCategoryOption('a');
    CategoryOption co2 = createCategoryOption('b');
    CategoryOption co3 = createCategoryOption('c');
    categoryService.addCategoryOption(co1);
    categoryService.addCategoryOption(co2);
    categoryService.addCategoryOption(co3);

    Category catSource1 = createCategory('x', co1);
    Category catSource2 = createCategory('y', co2);
    Category catTarget = createCategory('z', co3);
    categoryService.addCategory(catSource1);
    categoryService.addCategory(catSource2);
    categoryService.addCategory(catTarget);

    CategoryCombo cc1 = createCategoryCombo('1', catSource1);
    CategoryCombo cc2 = createCategoryCombo('2', catSource2);
    CategoryCombo cc3 = createCategoryCombo('3', catTarget);
    categoryService.addCategoryCombo(cc1);
    categoryService.addCategoryCombo(cc2);
    categoryService.addCategoryCombo(cc3);

    // when
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<CategoryCombo> targetCombos =
        categoryComboStore.getCategoryCombos(Set.of(catTarget.getUid()));
    List<Category> allCategories = manager.getAll(Category.class);

    assertFalse(report.hasErrorMessages());
    assertEquals(3, targetCombos.size(), "Expect 3 category combos with target category ref");

    // 1 custom + 1 default (sources deleted)
    assertEquals(2, allCategories.size(), "Expect 2 categories present");
    assertTrue(allCategories.contains(catTarget));
  }

  // -----------------------------
  // ----------- User ------------
  // -----------------------------
  @Test
  @DisplayName(
      "User dimension constraint refs to source Categories are replaced, sources not deleted")
  void userDimensionConstraintRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given - create categories and users with dimension constraints
    CategoryOption co1 = createCategoryOption('a');
    CategoryOption co2 = createCategoryOption('b');
    CategoryOption co3 = createCategoryOption('c');
    categoryService.addCategoryOption(co1);
    categoryService.addCategoryOption(co2);
    categoryService.addCategoryOption(co3);

    Category catSource1 = createCategory('x', co1);
    Category catSource2 = createCategory('y', co2);
    Category catTarget = createCategory('z', co3);
    categoryService.addCategory(catSource1);
    categoryService.addCategory(catSource2);
    categoryService.addCategory(catTarget);

    User user1 = makeUser("A");
    user1.getCatDimensionConstraints().add(catSource1);
    User user2 = makeUser("B");
    user2.getCatDimensionConstraints().add(catSource2);
    User user3 = makeUser("C");
    user3.getCatDimensionConstraints().add(catTarget);
    manager.save(List.of(user1, user2, user3));

    // confirm user state before merge
    List<User> sourceUsersBefore =
        userStore.getUsersByCategories(Set.of(catSource1.getUid(), catSource2.getUid()));
    List<User> targetUsersBefore = userStore.getUsersByCategories(Set.of(catTarget.getUid()));

    assertEquals(
        2, sourceUsersBefore.size(), "Expect 2 users with source category dimension constraint");
    assertEquals(
        1, targetUsersBefore.size(), "Expect 1 user with target category dimension constraint");

    // when
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    MergeReport report = categoryMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<User> sourceUsers =
        userStore.getUsersByCategories(Set.of(catSource1.getUid(), catSource2.getUid()));
    List<User> targetUsers = userStore.getUsersByCategories(Set.of(catTarget.getUid()));
    List<Category> allCategories = manager.getAll(Category.class);

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceUsers.size(), "Expect 0 users with source category dimension constraint");
    assertEquals(3, targetUsers.size(), "Expect 3 users with target category dimension constraint");

    // 3 custom + 1 default
    assertEquals(4, allCategories.size(), "Expect 4 categories present");
    assertTrue(allCategories.containsAll(List.of(catSource1, catSource2, catTarget)));
  }

  @Test
  @DisplayName(
      "User dimension constraint refs to source Categories are replaced, sources are deleted")
  void userDimensionConstraintRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given - create categories and users with dimension constraints
    CategoryOption co1 = createCategoryOption('a');
    CategoryOption co2 = createCategoryOption('b');
    CategoryOption co3 = createCategoryOption('c');
    categoryService.addCategoryOption(co1);
    categoryService.addCategoryOption(co2);
    categoryService.addCategoryOption(co3);

    Category catSource1 = createCategory('x', co1);
    Category catSource2 = createCategory('y', co2);
    Category catTarget = createCategory('z', co3);
    categoryService.addCategory(catSource1);
    categoryService.addCategory(catSource2);
    categoryService.addCategory(catTarget);

    User user1 = makeUser("D");
    user1.getCatDimensionConstraints().add(catSource1);
    User user2 = makeUser("E");
    user2.getCatDimensionConstraints().add(catSource2);
    User user3 = makeUser("F");
    user3.getCatDimensionConstraints().add(catTarget);
    manager.save(List.of(user1, user2, user3));

    // when
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<User> targetUsers = userStore.getUsersByCategories(Set.of(catTarget.getUid()));
    List<Category> allCategories = manager.getAll(Category.class);

    assertFalse(report.hasErrorMessages());
    assertEquals(3, targetUsers.size(), "Expect 3 users with target category dimension constraint");

    // 1 custom + 1 default (sources deleted)
    assertEquals(2, allCategories.size(), "Expect 2 categories present");
    assertTrue(allCategories.contains(catTarget));
  }

  // -----------------------------
  // ---- CategoryDimension ------
  // -----------------------------
  @Test
  @DisplayName("CategoryDimension refs to source Categories are replaced, sources not deleted")
  void categoryDimensionRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given - create categories and category dimensions
    CategoryOption co1 = createCategoryOption('a');
    CategoryOption co2 = createCategoryOption('b');
    CategoryOption co3 = createCategoryOption('c');
    categoryService.addCategoryOption(co1);
    categoryService.addCategoryOption(co2);
    categoryService.addCategoryOption(co3);

    Category catSource1 = createCategory('x', co1);
    Category catSource2 = createCategory('y', co2);
    Category catTarget = createCategory('z', co3);
    categoryService.addCategory(catSource1);
    categoryService.addCategory(catSource2);
    categoryService.addCategory(catTarget);

    CategoryDimension cd1 = createCategoryDimension(catSource1);
    CategoryDimension cd2 = createCategoryDimension(catSource2);
    CategoryDimension cd3 = createCategoryDimension(catTarget);
    categoryDimensionStore.save(cd1);
    categoryDimensionStore.save(cd2);
    categoryDimensionStore.save(cd3);

    // confirm category dimension state before merge
    List<CategoryDimension> sourceDimensionsBefore =
        categoryDimensionStore.getByCategory(Set.of(catSource1.getUid(), catSource2.getUid()));
    List<CategoryDimension> targetDimensionsBefore =
        categoryDimensionStore.getByCategory(Set.of(catTarget.getUid()));

    assertEquals(
        2, sourceDimensionsBefore.size(), "Expect 2 category dimensions with source category refs");
    assertEquals(
        1, targetDimensionsBefore.size(), "Expect 1 category dimension with target category ref");

    // when
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    MergeReport report = categoryMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<CategoryDimension> sourceDimensions =
        categoryDimensionStore.getByCategory(Set.of(catSource1.getUid(), catSource2.getUid()));
    List<CategoryDimension> targetDimensions =
        categoryDimensionStore.getByCategory(Set.of(catTarget.getUid()));
    List<Category> allCategories = manager.getAll(Category.class);

    assertFalse(report.hasErrorMessages());
    assertEquals(
        0, sourceDimensions.size(), "Expect 0 category dimensions with source category refs");
    assertEquals(
        3, targetDimensions.size(), "Expect 3 category dimensions with target category ref");

    // 3 custom + 1 default
    assertEquals(4, allCategories.size(), "Expect 4 categories present");
    assertTrue(allCategories.containsAll(List.of(catSource1, catSource2, catTarget)));
  }

  @Test
  @DisplayName("CategoryDimension refs to source Categories are replaced, sources are deleted")
  void categoryDimensionRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given - create categories and category dimensions
    CategoryOption co1 = createCategoryOption('a');
    CategoryOption co2 = createCategoryOption('b');
    CategoryOption co3 = createCategoryOption('c');
    categoryService.addCategoryOption(co1);
    categoryService.addCategoryOption(co2);
    categoryService.addCategoryOption(co3);

    Category catSource1 = createCategory('x', co1);
    Category catSource2 = createCategory('y', co2);
    Category catTarget = createCategory('z', co3);
    categoryService.addCategory(catSource1);
    categoryService.addCategory(catSource2);
    categoryService.addCategory(catTarget);

    CategoryDimension cd1 = createCategoryDimension(catSource1);
    CategoryDimension cd2 = createCategoryDimension(catSource2);
    CategoryDimension cd3 = createCategoryDimension(catTarget);
    categoryDimensionStore.save(cd1);
    categoryDimensionStore.save(cd2);
    categoryDimensionStore.save(cd3);

    // when
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then
    List<CategoryDimension> targetDimensions =
        categoryDimensionStore.getByCategory(Set.of(catTarget.getUid()));
    List<Category> allCategories = manager.getAll(Category.class);

    assertFalse(report.hasErrorMessages());
    assertEquals(
        3, targetDimensions.size(), "Expect 3 category dimensions with target category ref");

    // 1 custom + 1 default (sources deleted)
    assertEquals(2, allCategories.size(), "Expect 2 categories present");
    assertTrue(allCategories.contains(catTarget));
  }

  private MergeParams getMergeParams(List<Category> sources, Category target) {
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(sources.toArray(new Category[0])));
    mergeParams.setTarget(UID.of(target));
    return mergeParams;
  }
}
