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
package org.hisp.dhis.merge.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.analytics.CategoryDimensionStore;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryComboStore;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * All the tests in this class basically test the same thing:
 *
 * <p>- Create metadata which has source Category references
 *
 * <p>- Perform a Category merge
 *
 * <p>- Check that source Categories have had their references removed/replaced with the target
 * Category
 */
@Transactional
class CategoryMergeServiceTest extends PostgresIntegrationTestBase {

  @Autowired private CategoryService categoryService;
  @Autowired private CategoryComboStore categoryComboStore;
  @Autowired private CategoryDimensionStore categoryDimensionStore;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private MergeService categoryMergeService;
  @Autowired private DbmsManager dbmsManager;

  private Category catSource1;
  private Category catSource2;
  private Category catTarget;

  @BeforeEach
  void setUpCatModel() {
    CategoryOption co1 = createCategoryOption('1');
    CategoryOption co2 = createCategoryOption('2');
    categoryService.addCategoryOption(co1);
    categoryService.addCategoryOption(co2);

    catSource1 = createCategory('1', co1, co2);
    catSource2 = createCategory('2', co1, co2);
    catTarget = createCategory('3', co1, co2);
    categoryService.addCategory(catSource1);
    categoryService.addCategory(catSource2);
    categoryService.addCategory(catTarget);
  }

  // -----------------------------
  // ----- CategoryOption --------
  // -----------------------------
  @Test
  @DisplayName("CategoryOption refs to source Categories are replaced, sources not deleted")
  void categoryOptionRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given the same 2 COs exist for target & source Cs
    checkCategoryOptionState(catSource1, 2);
    checkCategoryOptionState(catSource2, 2);
    checkCategoryOptionState(catTarget, 2);

    // when a merge is processed
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    MergeReport report = categoryMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then there are no COs with source C refs
    checkCategoryOptionState(catSource1, 0);
    checkCategoryOptionState(catSource2, 0);
    checkCategoryOptionState(catTarget, 2);

    // and no options have refs to source categories
    assertFalse(report.hasErrorMessages());

    // 3 custom + 1 default
    List<Category> allCategories = manager.getAll(Category.class);
    assertEquals(4, allCategories.size(), "Expect 4 categories present");
    assertTrue(allCategories.containsAll(List.of(catSource1, catSource2, catTarget)));
  }

  @Test
  @DisplayName("CategoryOption refs to source Categories are replaced, sources are deleted")
  void categoryOptionRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given shared category options exist for target & source categories
    checkCategoryOptionState(catSource1, 2);
    checkCategoryOptionState(catSource2, 2);
    checkCategoryOptionState(catTarget, 2);

    // when a merge is processed
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then target Category still has 2 option refs
    checkCategoryOptionState(catTarget, 2);
    assertFalse(report.hasErrorMessages());

    // and source Cs are deleted
    List<Category> allCategories = manager.getAll(Category.class);
    assertEquals(2, allCategories.size(), "Expect 2 categories present");
    assertTrue(allCategories.contains(catTarget));
    assertNull(
        categoryService.getCategory(catSource1.getUid()), "source category 1 should be deleted");
    assertNull(
        categoryService.getCategory(catSource2.getUid()), "source category 2 should be deleted");

    // and no options have refs to source categories
    List<CategoryOption> allOptions = manager.getAll(CategoryOption.class);
    allOptions.forEach(
        co ->
            assertTrue(
                co.getCategories().stream()
                    .map(BaseIdentifiableObject::getUid)
                    .noneMatch(
                        uid ->
                            uid.equals(catSource1.getUid()) || uid.equals(catSource2.getUid()))));
  }

  @Test
  @DisplayName("When Categories don't have the same CategoryOptions, the merge is rejected")
  void diffCategoryOptionsRejectedTest() {
    // given different COs exist for target & source Cs
    CategoryOption coDiff1 = createCategoryOption('4');
    CategoryOption coDiff2 = createCategoryOption('5');
    CategoryOption coDiff3 = createCategoryOption('6');
    categoryService.addCategoryOption(coDiff1);
    categoryService.addCategoryOption(coDiff2);
    categoryService.addCategoryOption(coDiff3);

    Category catSource4 = createCategory('4', coDiff1);
    Category catSource5 = createCategory('5', coDiff2);
    Category catTarget6 = createCategory('6', coDiff1, coDiff2);
    categoryService.addCategory(catSource4);
    categoryService.addCategory(catSource5);
    categoryService.addCategory(catTarget6);

    checkCategoryOptionState(catSource4, 1);
    checkCategoryOptionState(catSource5, 1);
    checkCategoryOptionState(catTarget6, 2);

    // when a merge is processed, then it is rejected
    MergeParams mergeParams = getMergeParams(List.of(catSource4, catSource5), catTarget6);
    ConflictException conflictException =
        assertThrows(ConflictException.class, () -> categoryMergeService.processMerge(mergeParams));
    assertEquals("Merge validation error", conflictException.getMessage());
    assertTrue(
        conflictException
            .getMergeReport()
            .getMergeErrors()
            .get(0)
            .getMessage()
            .contains("do not match target CategoryOptions"));

    // and the state is unchanged
    checkCategoryOptionState(catSource4, 1);
    checkCategoryOptionState(catSource5, 1);
    checkCategoryOptionState(catTarget6, 2);
  }

  private void checkCategoryOptionState(Category category, int expectedOptions) {
    List<Category> allCategories = manager.getAll(Category.class);
    assertEquals(
        expectedOptions,
        allCategories.stream()
            .filter(c -> c.getUid().equals(category.getUid()))
            .flatMap(c -> c.getCategoryOptions().stream().map(IdentifiableObject::getUid))
            .count(),
        "Expect %d category options with category ref".formatted(expectedOptions));
  }

  // -----------------------------
  // ----- CategoryCombo ---------
  // -----------------------------
  @Test
  @DisplayName("CategoryCombo refs to source Categories are replaced, sources not deleted")
  void categoryComboRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given source and target Cs exist with different CCs
    CategoryCombo cc1 = createCategoryCombo('1', catSource1);
    CategoryCombo cc2 = createCategoryCombo('2', catSource2);
    CategoryCombo cc3 = createCategoryCombo('3', catTarget);
    categoryService.addCategoryCombo(cc1);
    categoryService.addCategoryCombo(cc2);
    categoryService.addCategoryCombo(cc3);

    // confirm category combo state before merge
    List<CategoryCombo> sourceCombosBefore =
        categoryComboStore.getCategoryCombosByCategory(
            Set.of(catSource1.getUid(), catSource2.getUid()));
    List<CategoryCombo> targetCombosBefore =
        categoryComboStore.getCategoryCombosByCategory(Set.of(catTarget.getUid()));

    assertEquals(
        2, sourceCombosBefore.size(), "Expect 2 category combos with source category refs");
    assertEquals(1, targetCombosBefore.size(), "Expect 1 category combo with target category ref");

    // when a merge request is processed
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    MergeReport report = categoryMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then there are no category combos with source category refs
    List<CategoryCombo> sourceCombos =
        categoryComboStore.getCategoryCombosByCategory(
            Set.of(catSource1.getUid(), catSource2.getUid()));
    List<CategoryCombo> targetCombos =
        categoryComboStore.getCategoryCombosByCategory(Set.of(catTarget.getUid()));
    List<Category> allCategories = manager.getAll(Category.class);

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceCombos.size(), "Expect 0 category combos with source category refs");
    assertEquals(3, targetCombos.size(), "Expect 3 category combos with target category ref");

    // and source Cs still exist
    assertEquals(4, allCategories.size(), "Expect 4 categories present");
    assertTrue(allCategories.containsAll(List.of(catSource1, catSource2, catTarget)));
  }

  @Test
  @DisplayName("CategoryCombo refs to source Categories are replaced, sources are deleted")
  void categoryComboRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given source and target Cs exist with different CCs
    CategoryCombo cc1 = createCategoryCombo('1', catSource1);
    CategoryCombo cc2 = createCategoryCombo('2', catSource2);
    CategoryCombo cc3 = createCategoryCombo('3', catTarget);
    categoryService.addCategoryCombo(cc1);
    categoryService.addCategoryCombo(cc2);
    categoryService.addCategoryCombo(cc3);

    // when a merge request is processed
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then the target C has 3 CC refs
    List<CategoryCombo> targetCombos =
        categoryComboStore.getCategoryCombosByCategory(Set.of(catTarget.getUid()));
    List<Category> allCategories = manager.getAll(Category.class);

    assertFalse(report.hasErrorMessages());
    assertEquals(3, targetCombos.size(), "Expect 3 category combos with target category ref");

    // source Cs are deleted
    assertEquals(2, allCategories.size(), "Expect 2 categories present");
    assertTrue(allCategories.contains(catTarget));
  }

  @Test
  @DisplayName(
      "If a source Category shares the same CategoryCombo as a target CategoryCombo, the merge is rejected")
  void sameCategoryComboRejectedTest() {
    // given 1 source has the same CC as the target
    CategoryCombo ccSame1 = createCategoryCombo('x', catSource1, catTarget);
    CategoryCombo ccDiff1 = createCategoryCombo('y', catSource2);
    categoryService.addCategoryCombo(ccSame1);
    categoryService.addCategoryCombo(ccDiff1);

    List<CategoryCombo> sourceCombosBefore =
        categoryComboStore.getCategoryCombosByCategory(
            Set.of(catSource1.getUid(), catSource2.getUid()));
    List<CategoryCombo> targetCombosBefore =
        categoryComboStore.getCategoryCombosByCategory(Set.of(catTarget.getUid()));

    assertEquals(
        2, sourceCombosBefore.size(), "Expect 2 category combos with source category refs");
    assertEquals(1, targetCombosBefore.size(), "Expect 1 category combos with target category ref");

    // when a merge is processed, then it is rejected
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    ConflictException conflictException =
        assertThrows(ConflictException.class, () -> categoryMergeService.processMerge(mergeParams));
    assertEquals("Merge validation error", conflictException.getMessage());
    assertTrue(
        conflictException
            .getMergeReport()
            .getMergeErrors()
            .get(0)
            .getMessage()
            .contains("Source and target Categories cannot share a CategoryCombo"));

    dbmsManager.clearSession();

    // and the state is unchanged
    List<CategoryCombo> sourceCombosAfter =
        categoryComboStore.getCategoryCombosByCategory(
            Set.of(catSource1.getUid(), catSource2.getUid()));
    List<CategoryCombo> targetCombosAfter =
        categoryComboStore.getCategoryCombosByCategory(Set.of(catTarget.getUid()));

    assertEquals(2, sourceCombosAfter.size(), "Expect 2 category combos with source category refs");
    assertEquals(1, targetCombosAfter.size(), "Expect 1 category combo with target category ref");
  }

  // -----------------------------
  // ----------- User ------------
  // -----------------------------
  @Test
  @DisplayName(
      "User dimension constraint refs to source Categories are replaced, sources not deleted")
  void userDimensionConstraintRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given source and target Cs exist with different dimension constraints
    User user1 = makeUser("A");
    user1.getCatDimensionConstraints().add(catSource1);
    User user2 = makeUser("B");
    user2.getCatDimensionConstraints().add(catSource2);
    User user3 = makeUser("C");
    user3.getCatDimensionConstraints().add(catTarget);
    manager.save(List.of(user1, user2, user3));

    // confirm user state before merge
    List<User> sourceUsersBefore =
        getUsersByCategories(Set.of(catSource1.getUid(), catSource2.getUid()));
    List<User> targetUsersBefore = getUsersByCategories(Set.of(catTarget.getUid()));

    assertEquals(
        2, sourceUsersBefore.size(), "Expect 2 users with source category dimension constraint");
    assertEquals(
        1, targetUsersBefore.size(), "Expect 1 user with target category dimension constraint");

    // when a merge request is processed
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    MergeReport report = categoryMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then there are no users with source category dimension constraints
    List<User> sourceUsers = getUsersByCategories(Set.of(catSource1.getUid(), catSource2.getUid()));
    List<User> targetUsers = getUsersByCategories(Set.of(catTarget.getUid()));
    List<Category> allCategories = manager.getAll(Category.class);

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceUsers.size(), "Expect 0 users with source category dimension constraint");

    // and there are 3 users with target category dimension constraints
    assertEquals(3, targetUsers.size(), "Expect 3 users with target category dimension constraint");

    // source Cs still exist
    assertEquals(4, allCategories.size(), "Expect 4 categories present");
    assertTrue(allCategories.containsAll(List.of(catSource1, catSource2, catTarget)));
  }

  private List<User> getUsersByCategories(Set<String> categoryUids) {
    return manager.getAll(User.class).stream()
        .filter(
            user -> {
              Set<Category> catDimensionConstraints = user.getCatDimensionConstraints();
              return catDimensionConstraints.stream()
                  .anyMatch(category -> categoryUids.contains(category.getUid()));
            })
        .toList();
  }

  @Test
  @DisplayName(
      "User dimension constraint refs to source Categories are replaced, sources are deleted")
  void userDimensionConstraintRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given source and target Cs exist with different dimension constraints
    User user1 = makeUser("D");
    user1.getCatDimensionConstraints().add(catSource1);
    User user2 = makeUser("E");
    user2.getCatDimensionConstraints().add(catSource2);
    User user3 = makeUser("F");
    user3.getCatDimensionConstraints().add(catTarget);
    manager.save(List.of(user1, user2, user3));

    // when a merge request is processed
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then there are 3 users with target category dimension constraints
    List<User> targetUsers = getUsersByCategories(Set.of(catTarget.getUid()));
    List<User> sourceUsers = getUsersByCategories(Set.of(catSource1.getUid(), catSource2.getUid()));
    List<Category> allCategories = manager.getAll(Category.class);

    assertFalse(report.hasErrorMessages());
    assertEquals(3, targetUsers.size(), "Expect 3 users with target category dimension constraint");

    // then there are 0 users with source category dimension constraints
    assertEquals(0, sourceUsers.size(), "Expect 0 users with source category dimension constraint");

    // and source Cs are deleted
    assertEquals(2, allCategories.size(), "Expect 2 categories present");
    assertTrue(allCategories.contains(catTarget));
  }

  // -----------------------------
  // ---- CategoryDimension ------
  // -----------------------------
  @Test
  @DisplayName("CategoryDimension refs to source Categories are replaced, sources not deleted")
  void categoryDimensionRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given source and target Cs exist with different dimension constraints
    CategoryDimension cd1 = createCategoryDimension(catSource1);
    CategoryDimension cd2 = createCategoryDimension(catSource2);
    CategoryDimension cd3 = createCategoryDimension(catTarget);
    categoryDimensionStore.save(cd1);
    categoryDimensionStore.save(cd2);
    categoryDimensionStore.save(cd3);

    // confirm category dimension state before merge
    List<CategoryDimension> sourceDimensionsBefore =
        getByCategory(Set.of(catSource1.getUid(), catSource2.getUid()));
    List<CategoryDimension> targetDimensionsBefore = getByCategory(Set.of(catTarget.getUid()));

    assertEquals(
        2, sourceDimensionsBefore.size(), "Expect 2 category dimensions with source category refs");
    assertEquals(
        1, targetDimensionsBefore.size(), "Expect 1 category dimension with target category ref");

    // when a merge request is processed
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    MergeReport report = categoryMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then there are 0 category dimensions with source C refs
    List<CategoryDimension> sourceDimensions =
        getByCategory(Set.of(catSource1.getUid(), catSource2.getUid()));
    List<CategoryDimension> targetDimensions = getByCategory(Set.of(catTarget.getUid()));
    List<Category> allCategories = manager.getAll(Category.class);

    assertFalse(report.hasErrorMessages());
    assertEquals(
        0, sourceDimensions.size(), "Expect 0 category dimensions with source category refs");

    // then there are 3 category dimensions with target C refs
    assertEquals(
        3, targetDimensions.size(), "Expect 3 category dimensions with target category ref");

    // and source Cs still exist
    assertEquals(4, allCategories.size(), "Expect 4 categories present");
    assertTrue(allCategories.containsAll(List.of(catSource1, catSource2, catTarget)));
  }

  private List<CategoryDimension> getByCategory(Set<String> categoryUids) {
    return categoryDimensionStore.getAll().stream()
        .filter(
            cd -> {
              Category catDimension = cd.getDimension();
              return categoryUids.contains(catDimension.getUid());
            })
        .toList();
  }

  @Test
  @DisplayName("CategoryDimension refs to source Categories are replaced, sources are deleted")
  void categoryDimensionRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given source and target Cs exist with different dimension constraints
    CategoryDimension cd1 = createCategoryDimension(catSource1);
    CategoryDimension cd2 = createCategoryDimension(catSource2);
    CategoryDimension cd3 = createCategoryDimension(catTarget);
    categoryDimensionStore.save(cd1);
    categoryDimensionStore.save(cd2);
    categoryDimensionStore.save(cd3);

    // when a merge request is processed
    MergeParams mergeParams = getMergeParams(List.of(catSource1, catSource2), catTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryMergeService.processMerge(mergeParams);
    dbmsManager.clearSession();

    // then there are 3 category dimensions with target C refs
    List<CategoryDimension> targetDimensions = getByCategory(Set.of(catTarget.getUid()));
    List<CategoryDimension> sourceDimensions =
        getByCategory(Set.of(catSource1.getUid(), catSource2.getUid()));
    List<Category> allCategories = manager.getAll(Category.class);

    assertFalse(report.hasErrorMessages());
    assertEquals(
        3, targetDimensions.size(), "Expect 3 category dimensions with target category ref");

    // then there are 0 category dimensions with source C refs
    assertEquals(
        0, sourceDimensions.size(), "Expect 0 category dimensions with source category ref");

    // and source Cs are deleted
    assertEquals(2, allCategories.size(), "Expect 2 categories present");
    assertTrue(allCategories.contains(catTarget));
  }

  private Set<String> getOptionsFromCategories(Category... categories) {
    return Arrays.stream(categories)
        .flatMap(c -> c.getCategoryOptions().stream())
        .map(BaseMetadataObject::getUid)
        .collect(Collectors.toSet());
  }

  private MergeParams getMergeParams(List<Category> sources, Category target) {
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(sources.toArray(new Category[0])));
    mergeParams.setTarget(UID.of(target));
    return mergeParams;
  }
}
