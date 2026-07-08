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

import static io.hypersistence.utils.jdbc.validator.SQLStatementCountValidator.assertDeleteCount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.hypersistence.utils.jdbc.validator.SQLStatementCountValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.analytics.CategoryDimensionStore;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.api.TestCategoryMetadata;
import org.hisp.dhis.test.config.QueryCountDataSourceProxy;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
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
@Transactional
@ContextConfiguration(classes = {QueryCountDataSourceProxy.class})
class CategoryOptionMergeServiceTest extends PostgresIntegrationTestBase {

  @Autowired private CategoryService categoryService;
  @Autowired private OrganisationUnitService organisationUnitService;
  @Autowired private CategoryDimensionStore dimensionStore;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private MergeService categoryOptionMergeService;

  // -----------------------------
  // --------- Category ----------
  // -----------------------------
  @Test
  @DisplayName("Category ref to source CategoryOption is replaced, source not deleted")
  void categoryRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given source and target CategoryOptions that belong to the same Category
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco1");
    CategoryOption coSource = categoryMetadata.co1();
    CategoryOption coTarget = categoryMetadata.co2();

    List<Category> categorySourcesBefore =
        categoryService.getCategoriesByCategoryOption(Set.of(UID.of(coSource)));
    List<Category> categoryTargetBefore =
        categoryService.getCategoriesByCategoryOption(Set.of(UID.of(coTarget)));

    assertEquals(
        1, categorySourcesBefore.size(), "Expect 1 category with source category option refs");
    assertEquals(
        1, categoryTargetBefore.size(), "Expect 1 category with target category option refs");

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<Category> categorySources =
        categoryService.getCategoriesByCategoryOption(Set.of(UID.of(coSource)));
    List<Category> categoryTarget =
        categoryService.getCategoriesByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, categorySources.size(), "Expect 0 entries with source category option refs");
    assertEquals(1, categoryTarget.size(), "Expect 1 entry with target category option refs");

    // 4 custom + 1 default
    assertEquals(5, allCategoryOptions.size(), "Expect 5 category options present");
    assertTrue(allCategoryOptions.containsAll(categoryMetadata.getCategoryOptions()));
  }

  @Test
  @DisplayName("Category ref to source CategoryOption is replaced, source is deleted")
  void categoryRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given source and target CategoryOptions that belong to the same Category
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco2");
    CategoryOption coSource = categoryMetadata.co1();
    CategoryOption coTarget = categoryMetadata.co2();
    List<Category> categorySourcesBefore =
        categoryService.getCategoriesByCategoryOption(Set.of(UID.of(coSource)));
    List<Category> categoryTargetBefore =
        categoryService.getCategoriesByCategoryOption(List.of(UID.of(coTarget)));

    assertEquals(
        1, categorySourcesBefore.size(), "Expect 1 category with source category option refs");
    assertEquals(
        1, categoryTargetBefore.size(), "Expect 1 category with target category option refs");

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<Category> categorySources =
        categoryService.getCategoriesByCategoryOption(Set.of(UID.of(coSource)));
    List<Category> categoryTarget =
        categoryService.getCategoriesByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, categorySources.size(), "Expect 0 entries with source category option refs");
    assertEquals(1, categoryTarget.size(), "Expect 1 entry with target category option refs");

    // 3 custom (co2, co3, co4) + 1 default
    assertEquals(4, allCategoryOptions.size(), "Expect 4 category options present");
    assertTrue(allCategoryOptions.contains(coTarget));
    assertFalse(allCategoryOptions.contains(coSource), "source cat option is no longer present");
  }

  // -----------------------------
  // ---- CategoryOptionCombo ----
  // -----------------------------
  @Test
  @DisplayName("CategoryOptionCombo refs to source CategoryOption are replaced, source not deleted")
  void catOptComboRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given source and target CategoryOptions that belong to the same Category
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco3");
    CategoryOption coSource = categoryMetadata.co1();
    CategoryOption coTarget = categoryMetadata.co2();

    // confirm cat option combo state before merge
    List<CategoryOptionCombo> sourceCocsBefore =
        categoryService.getCategoryOptionCombosByCategoryOption(Set.of(UID.of(coSource)));
    List<CategoryOptionCombo> targetCocsBefore =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(coTarget)));

    assertEquals(2, sourceCocsBefore.size(), "Expect 2 entries with source category option refs");
    assertEquals(2, targetCocsBefore.size(), "Expect 2 entries with target category option refs");

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryOptionCombo> sourceCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(Set.of(UID.of(coSource)));
    List<CategoryOptionCombo> targetCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceCocs.size(), "Expect 0 entries with source category option refs");
    assertEquals(4, targetCocs.size(), "Expect 4 entries with target category option refs");
    assertEquals(5, allCategoryOptions.size(), "Expect 5 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget, coSource)));
  }

  @Test
  @DisplayName("Expect the correct number of SQL delete queries when merging")
  void catOptMergeQueryTest() throws ConflictException {
    // given source and target CategoryOptions that belong to the same Category
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco4");
    CategoryOption coSource = categoryMetadata.co1();
    CategoryOption coTarget = categoryMetadata.co2();

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    mergeParams.setDeleteSources(true);

    SQLStatementCountValidator.reset();
    categoryOptionMergeService.processMerge(mergeParams);

    // then
    assertDeleteCount(3);
    assertNull(
        categoryService.getCategoryOption(coSource.getUid()), "source cat option should not exist");
  }

  @Test
  @DisplayName("CategoryOptionCombo refs to source CategoryOption are replaced, source is deleted")
  void catOptComboRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given source and target CategoryOptions that belong to the same Category
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco5");
    CategoryOption coSource = categoryMetadata.co1();
    CategoryOption coTarget = categoryMetadata.co2();

    // confirm cat option combos state before merge
    List<CategoryOptionCombo> sourceCocsBefore =
        categoryService.getCategoryOptionCombosByCategoryOption(Set.of(UID.of(coSource)));
    List<CategoryOptionCombo> targetCocsBefore =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(coTarget)));

    assertEquals(2, sourceCocsBefore.size(), "Expect 2 entries with source category option refs");
    assertEquals(2, targetCocsBefore.size(), "Expect 2 entries with target category option refs");

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    mergeParams.setDeleteSources(true);

    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryOptionCombo> sourceCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(Set.of(UID.of(coSource)));
    List<CategoryOptionCombo> targetCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceCocs.size(), "Expect 0 entries with source category option refs");
    assertEquals(4, targetCocs.size(), "Expect 4 entries with target category option refs");

    // 3 custom (co2, co3, co4) + 1 default
    assertEquals(4, allCategoryOptions.size(), "Expect 4 category options present");
    assertTrue(allCategoryOptions.contains(coTarget));
    assertFalse(allCategoryOptions.contains(coSource));
  }

  // -----------------------------
  // --------- Org Unit ----------
  // -----------------------------
  @Test
  @DisplayName("OrgUnit refs to source CategoryOption are replaced, source not deleted")
  void orgUnitRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given source and target CategoryOptions that belong to the same Category
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco7");
    CategoryOption coSource = categoryMetadata.co1();
    CategoryOption coTarget = categoryMetadata.co2();

    OrganisationUnit ou1 = createOrganisationUnit('x');
    ou1.addCategoryOption(coSource);

    OrganisationUnit ou2 = createOrganisationUnit('z');
    ou2.addCategoryOption(coTarget);

    manager.save(List.of(ou1, ou2));

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<OrganisationUnit> orgUnitSources =
        organisationUnitService.getByCategoryOption(Set.of(UID.of(coSource)));
    List<OrganisationUnit> orgUnitTarget =
        organisationUnitService.getByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, orgUnitSources.size(), "Expect 0 entries with source org units refs");
    assertEquals(2, orgUnitTarget.size(), "Expect 2 entries with target org unit refs");

    assertEquals(5, allCategoryOptions.size(), "Expect 5 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget, coSource)));
  }

  @Test
  @DisplayName("OrgUnit refs to source CategoryOption are replaced, source deleted")
  void orgUnitRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given source and target CategoryOptions that belong to the same Category
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco8");
    CategoryOption coSource = categoryMetadata.co1();
    CategoryOption coTarget = categoryMetadata.co2();

    OrganisationUnit ou1 = createOrganisationUnit('x');
    ou1.addCategoryOption(coSource);

    OrganisationUnit ou2 = createOrganisationUnit('z');
    ou2.addCategoryOption(coTarget);

    manager.save(List.of(ou1, ou2));

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<OrganisationUnit> orgUnitSources =
        organisationUnitService.getByCategoryOption(Set.of(UID.of(coSource)));
    List<OrganisationUnit> orgUnitTarget =
        organisationUnitService.getByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, orgUnitSources.size(), "Expect 0 entries with source org units refs");
    assertEquals(2, orgUnitTarget.size(), "Expect 2 entries with target org unit refs");

    assertEquals(4, allCategoryOptions.size(), "Expect 4 category options present");
    assertTrue(allCategoryOptions.contains(coTarget));
    assertFalse(allCategoryOptions.contains(coSource));
  }

  @Test
  @DisplayName(
      "1 OrgUnit with refs to source & target CategoryOption results in OrgUnit with just target")
  void orgUnitSourceAndTargetRefsTest() throws ConflictException {
    // given source and target CategoryOptions that belong to the same Category
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco9");
    CategoryOption coSource = categoryMetadata.co1();
    CategoryOption coTarget = categoryMetadata.co2();

    OrganisationUnit ou = createOrganisationUnit('o');
    ou.addCategoryOption(coSource);
    ou.addCategoryOption(coTarget);

    manager.save(ou);

    // confirm org unit state before merge
    List<OrganisationUnit> sourceOusBefore =
        organisationUnitService.getByCategoryOption(Set.of(UID.of(coSource)));
    List<OrganisationUnit> targetOusBefore =
        organisationUnitService.getByCategoryOption(List.of(UID.of(coTarget)));

    assertEquals(1, sourceOusBefore.size(), "Expect 1 entry with source category option refs");
    assertEquals(1, targetOusBefore.size(), "Expect 1 entry with target category option ref");

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<OrganisationUnit> sourceOus =
        organisationUnitService.getByCategoryOption(Set.of(UID.of(coSource)));
    List<OrganisationUnit> targetOus =
        organisationUnitService.getByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();
    List<CategoryOption> catOptions =
        targetOus.stream().flatMap(orgUnit -> orgUnit.getCategoryOptions().stream()).toList();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceOus.size(), "Expect 0 entries with source category option refs");
    assertEquals(1, targetOus.size(), "Expect 1 entry with target category option ref");
    assertEquals(1, catOptions.size());
    assertTrue(catOptions.contains(coTarget));
    assertEquals(5, allCategoryOptions.size(), "Expect 5 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget, coSource)));
  }

  // -----------------------------
  // --- Category Option Group ---
  // -----------------------------
  @Test
  @DisplayName("CategoryOptionGroup refs to source CategoryOption are replaced, source not deleted")
  void catOptionGroupSourcesNotDeletedTest() throws ConflictException {
    // given source and target CategoryOptions that belong to the same Category
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco10");
    CategoryOption coSource = categoryMetadata.co1();
    CategoryOption coTarget = categoryMetadata.co2();

    CategoryOptionGroup cog1 = createCategoryOptionGroup('x');
    cog1.addCategoryOption(coSource);

    CategoryOptionGroup cog2 = createCategoryOptionGroup('z');
    cog2.addCategoryOption(coTarget);

    manager.save(List.of(cog1, cog2));

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryOptionGroup> cogSources =
        categoryService.getCategoryOptionGroupByCategoryOption(Set.of(UID.of(coSource)));
    List<CategoryOptionGroup> cogTarget =
        categoryService.getCategoryOptionGroupByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, cogSources.size(), "Expect 0 entries with source cat option group refs");
    assertEquals(2, cogTarget.size(), "Expect 2 entries with target cat option group refs");

    assertEquals(5, allCategoryOptions.size(), "Expect 5 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget, coSource)));
  }

  @Test
  @DisplayName("CategoryOptionGroup refs to source CategoryOption are replaced, source deleted")
  void catOptionGroupSourcesDeletedTest() throws ConflictException {
    // given source and target CategoryOptions that belong to the same Category
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco11");
    CategoryOption coSource = categoryMetadata.co1();
    CategoryOption coTarget = categoryMetadata.co2();

    CategoryOptionGroup cog1 = createCategoryOptionGroup('x');
    cog1.addCategoryOption(coSource);

    CategoryOptionGroup cog2 = createCategoryOptionGroup('z');
    cog2.addCategoryOption(coTarget);

    manager.save(List.of(cog1, cog2));

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryOptionGroup> cogSources =
        categoryService.getCategoryOptionGroupByCategoryOption(Set.of(UID.of(coSource)));
    List<CategoryOptionGroup> cogTarget =
        categoryService.getCategoryOptionGroupByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, cogSources.size(), "Expect 0 entries with source cat option group refs");
    assertEquals(2, cogTarget.size(), "Expect 2 entries with target cat option group refs");

    assertEquals(4, allCategoryOptions.size(), "Expect 4 category options present");
    assertTrue(allCategoryOptions.contains(coTarget));
    assertFalse(allCategoryOptions.contains(coSource));
  }

  @Test
  @DisplayName(
      "1 CategoryOptionGroup with refs to source & target CategoryOption results in CategoryOptionGroup with just target")
  void catOptGroupSourceAndTargetRefsTest() throws ConflictException {
    // given source and target CategoryOptions that belong to the same Category
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco12");
    CategoryOption coSource = categoryMetadata.co1();
    CategoryOption coTarget = categoryMetadata.co2();

    CategoryOptionGroup cog = createCategoryOptionGroup('g');
    cog.addCategoryOption(coSource);
    cog.addCategoryOption(coTarget);
    manager.save(cog);

    // confirm cat option combo state before merge
    List<CategoryOptionGroup> sourceCogsBefore =
        categoryService.getCategoryOptionGroupByCategoryOption(Set.of(UID.of(coSource)));
    List<CategoryOptionGroup> targetCogsBefore =
        categoryService.getCategoryOptionGroupByCategoryOption(List.of(UID.of(coTarget)));

    assertEquals(1, sourceCogsBefore.size(), "Expect 1 entry with source category option ref");
    assertEquals(1, targetCogsBefore.size(), "Expect 1 entry with target category option ref");

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryOptionGroup> sourceCogs =
        categoryService.getCategoryOptionGroupByCategoryOption(Set.of(UID.of(coSource)));
    List<CategoryOptionGroup> targetCogs =
        categoryService.getCategoryOptionGroupByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();
    List<CategoryOption> catOptions =
        targetCogs.stream().flatMap(catOptGroup -> catOptGroup.getMembers().stream()).toList();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceCogs.size(), "Expect 0 entries with source category option refs");
    assertEquals(1, targetCogs.size(), "Expect 1 entry with target category option ref");
    assertEquals(1, catOptions.size());
    assertTrue(catOptions.contains(coTarget));
    assertEquals(5, allCategoryOptions.size(), "Expect 5 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget, coSource)));
  }

  // -----------------------------
  // ----- Category Dimension ----
  // -----------------------------
  @Test
  @DisplayName("CategoryDimension refs to source CategoryOption are replaced, source not deleted")
  void catDimensionSourcesNotDeletedTest() throws ConflictException {
    // given source and target CategoryOptions that belong to the same Category
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco13");
    CategoryOption coSource = categoryMetadata.co1();
    CategoryOption coTarget = categoryMetadata.co2();

    CategoryDimension cd1 = createCategoryDimension(categoryMetadata.c1());
    cd1.getItems().add(coSource);

    CategoryDimension cd2 = createCategoryDimension(categoryMetadata.c1());
    cd2.getItems().add(coTarget);

    dimensionStore.save(cd1);
    dimensionStore.save(cd2);

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryDimension> cogSources =
        dimensionStore.getByCategoryOption(List.of(coSource.getUid()));
    List<CategoryDimension> cogTarget =
        dimensionStore.getByCategoryOption(List.of(coTarget.getUid()));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, cogSources.size(), "Expect 0 entries with source category dimension refs");
    assertEquals(2, cogTarget.size(), "Expect 2 entries with target category dimension refs");

    assertEquals(5, allCategoryOptions.size(), "Expect 5 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget, coSource)));
  }

  @Test
  @DisplayName("CategoryDimension refs to source CategoryOption are replaced, source deleted")
  void catDimensionSourcesDeletedTest() throws ConflictException {
    // given source and target CategoryOptions that belong to the same Category
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco14");
    CategoryOption coSource = categoryMetadata.co1();
    CategoryOption coTarget = categoryMetadata.co2();

    CategoryDimension cd1 = createCategoryDimension(categoryMetadata.c1());
    cd1.getItems().add(coSource);

    CategoryDimension cd2 = createCategoryDimension(categoryMetadata.c1());
    cd2.getItems().add(coTarget);

    dimensionStore.save(cd1);
    dimensionStore.save(cd2);

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryDimension> cdSources =
        dimensionStore.getByCategoryOption(List.of(coSource.getUid()));
    List<CategoryDimension> cdTarget =
        dimensionStore.getByCategoryOption(List.of(coTarget.getUid()));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, cdSources.size(), "Expect 0 entries with source category dimension refs");
    assertEquals(2, cdTarget.size(), "Expect 2 entries with target category dimension refs");

    assertEquals(4, allCategoryOptions.size(), "Expect 4 category options present");
    assertTrue(allCategoryOptions.contains(coTarget));
    assertFalse(allCategoryOptions.contains(coSource));
  }

  // -----------------------------
  // ----- Validation ------------
  // -----------------------------
  @Test
  @DisplayName("Merge succeeds when source and target CategoryOptions share identical Categories")
  void identicalCategoriesMergeTest() throws ConflictException {
    // given a source and target CategoryOption that belong to the same Category
    CategoryOption coSource = createCategoryOption('A');
    CategoryOption coTarget = createCategoryOption('B');
    categoryService.addCategoryOption(coSource);
    categoryService.addCategoryOption(coTarget);

    Category category = createCategory('Q', coSource, coTarget);
    categoryService.addCategory(category);

    OrganisationUnit ou = createOrganisationUnit('q');
    ou.addCategoryOption(coSource);
    manager.save(ou);

    // when merging a source into a target that shares identical Category membership
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then no validation errors, source refs are replaced with the target and source is deleted
    assertFalse(
        report.hasErrorMessages(), "Expect no merge errors for an identical-Category merge");

    List<OrganisationUnit> ouSource =
        organisationUnitService.getByCategoryOption(List.of(UID.of(coSource)));
    List<OrganisationUnit> ouTarget =
        organisationUnitService.getByCategoryOption(List.of(UID.of(coTarget)));

    assertEquals(0, ouSource.size(), "Expect 0 org unit refs to source");
    assertEquals(1, ouTarget.size(), "Expect org unit ref moved to target");
    assertNull(
        categoryService.getCategoryOption(coSource.getUid()), "Source option should be deleted");
    assertNotNull(
        categoryService.getCategoryOption(coTarget.getUid()), "Target option should remain");
  }

  @Test
  @DisplayName("Merge is rejected when source and target CategoryOptions have different Categories")
  void differentCategoriesRejectedTest() {
    // given source (in cat1) and target (in cat2) which do not share identical Category membership
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco15");
    CategoryOption coSource = categoryMetadata.co1();
    CategoryOption coTarget = categoryMetadata.co3();

    assertEquals(
        Set.of(categoryMetadata.c1().getUid()),
        coSource.getCategories().stream().map(Category::getUid).collect(Collectors.toSet()),
        "precondition: source belongs to cat1 only");
    assertEquals(
        Set.of(categoryMetadata.c2().getUid()),
        coTarget.getCategories().stream().map(Category::getUid).collect(Collectors.toSet()),
        "precondition: target belongs to cat2 only");

    // when a cross-Category merge is attempted
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    mergeParams.setDeleteSources(true);
    ConflictException exception =
        assertThrows(
            ConflictException.class, () -> categoryOptionMergeService.processMerge(mergeParams));

    // then a validation error is reported and no merge is performed
    assertTrue(exception.getMessage().contains("Merge validation error"));
    assertNotNull(exception.getMergeReport());
    assertTrue(
        exception.getMergeReport().hasErrorMessages(),
        "Expect a validation error message in the merge report");

    // and source references/state are untouched (merge did not run)
    List<CategoryOptionCombo> sourceCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(coSource)));
    assertEquals(2, sourceCocs.size(), "Source CategoryOptionCombo refs should be unchanged");
    assertNotNull(
        categoryService.getCategoryOption(coSource.getUid()),
        "Source CategoryOption should still exist");
  }

  @Test
  @DisplayName("Merge succeeds when source and target share identical multiple Categories")
  void identicalMultipleCategoriesMergeTest() throws ConflictException {
    // given a source and target CategoryOption that BOTH belong to the same two Categories
    CategoryOption coSource = createCategoryOption('M');
    CategoryOption coTarget = createCategoryOption('N');
    categoryService.addCategoryOption(coSource);
    categoryService.addCategoryOption(coTarget);

    Category catA = createCategory('A', coSource, coTarget);
    Category catB = createCategory('B', coSource, coTarget);
    categoryService.addCategory(catA);
    categoryService.addCategory(catB);

    // precondition: source and target both belong to {catA, catB}
    assertEquals(
        Set.of(catA.getUid(), catB.getUid()),
        coSource.getCategories().stream().map(Category::getUid).collect(Collectors.toSet()),
        "source belongs to both categories");
    assertEquals(
        Set.of(catA.getUid(), catB.getUid()),
        coTarget.getCategories().stream().map(Category::getUid).collect(Collectors.toSet()),
        "target belongs to both categories");

    OrganisationUnit ou = createOrganisationUnit('m');
    ou.addCategoryOption(coSource);
    manager.save(ou);

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then the merge is allowed (identical multi-Category membership) and refs move to the target
    assertFalse(report.hasErrorMessages(), "Expect no merge errors for identical multi-Category");
    assertEquals(0, organisationUnitService.getByCategoryOption(Set.of(UID.of(coSource))).size());
    assertEquals(1, organisationUnitService.getByCategoryOption(List.of(UID.of(coTarget))).size());
    assertNull(
        categoryService.getCategoryOption(coSource.getUid()), "Source option should be deleted");
    assertNotNull(
        categoryService.getCategoryOption(coTarget.getUid()), "Target option should remain");
  }

  @Test
  @DisplayName("Merge is rejected when source Categories are a subset of target Categories")
  void overlappingButNotIdenticalCategoriesRejectedTest() {
    // given source in {catA} and target in {catA, catB} - overlapping but NOT identical
    CategoryOption coSource = createCategoryOption('P');
    CategoryOption coTarget = createCategoryOption('Q');
    categoryService.addCategoryOption(coSource);
    categoryService.addCategoryOption(coTarget);

    Category catA = createCategory('C', coSource, coTarget);
    Category catB = createCategory('D', coTarget);
    categoryService.addCategory(catA);
    categoryService.addCategory(catB);

    // precondition: source in 1 category, target in 2 (target is a superset)
    assertEquals(1, coSource.getCategories().size(), "source belongs to 1 category");
    assertEquals(2, coTarget.getCategories().size(), "target belongs to 2 categories");

    // when / then - exact set equality is required, a subset/overlap is not enough
    MergeParams mergeParams = getMergeParams(List.of(coSource), coTarget);
    mergeParams.setDeleteSources(true);
    ConflictException exception =
        assertThrows(
            ConflictException.class, () -> categoryOptionMergeService.processMerge(mergeParams));

    assertTrue(exception.getMessage().contains("Merge validation error"));
    assertEquals(
        1,
        exception.getMergeReport().getMergeErrors().size(),
        "Expect 1 validation error for the mismatched source");
    assertNotNull(
        categoryService.getCategoryOption(coSource.getUid()),
        "Source should not be deleted when validation fails");
  }

  // -----------------------------
  // ----- Multiple sources ------
  // -----------------------------
  @Test
  @DisplayName(
      "CategoryOptionCombo refs to 4 source CategoryOptions are replaced, sources not deleted")
  void catOptComboRefsReplacedMultipleSourcesNotDeletedTest() throws ConflictException {
    // given 4 sources and 1 target that all belong to the same Category
    MultiSourceFixture fixture = setupMultiSourceSameCategory("mcoMS1");
    List<CategoryOption> sources = fixture.sources();
    CategoryOption target = fixture.target();
    Set<UID> sourceUids = sources.stream().map(UID::of).collect(Collectors.toSet());

    // confirm cat option combo state before merge
    List<CategoryOptionCombo> sourceCocsBefore =
        categoryService.getCategoryOptionCombosByCategoryOption(sourceUids);
    List<CategoryOptionCombo> targetCocsBefore =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(target)));

    assertEquals(4, sourceCocsBefore.size(), "Expect 4 entries with source category option refs");
    assertEquals(1, targetCocsBefore.size(), "Expect 1 entry with target category option refs");

    // when
    MergeParams mergeParams = getMergeParams(sources, target);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then all 4 source COC refs are moved to the target
    List<CategoryOptionCombo> sourceCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(sourceUids);
    List<CategoryOptionCombo> targetCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(target)));

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceCocs.size(), "Expect 0 entries with source category option refs");
    // NB: the 4 source COCs collapse onto the target's existing COC option set, so the target is
    // now referenced by 5 structurally-duplicate COCs. This is a known outcome of a same-Category
    // merge and is surfaced to the caller via the merge report advisory (see MergeWebResponse);
    // the duplicates should be resolved via the `category_option_combos_have_duplicates` check.
    assertEquals(5, targetCocs.size(), "Expect 5 entries with target category option refs");
    // all sources still present (not deleted)
    assertTrue(
        sources.stream().allMatch(s -> categoryService.getCategoryOption(s.getUid()) != null),
        "All source options should still exist");
  }

  @Test
  @DisplayName("CategoryOptionCombo refs to 4 source CategoryOptions are replaced, sources deleted")
  void catOptComboRefsReplacedMultipleSourcesDeletedTest() throws ConflictException {
    // given 4 sources and 1 target that all belong to the same Category
    MultiSourceFixture fixture = setupMultiSourceSameCategory("mcoMS2");
    List<CategoryOption> sources = fixture.sources();
    CategoryOption target = fixture.target();
    Set<UID> sourceUids = sources.stream().map(UID::of).collect(Collectors.toSet());

    assertEquals(
        4,
        categoryService.getCategoryOptionCombosByCategoryOption(sourceUids).size(),
        "Expect 4 entries with source category option refs");

    // when
    MergeParams mergeParams = getMergeParams(sources, target);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then all 4 source COC refs are moved to the target and all sources are deleted
    assertFalse(report.hasErrorMessages());
    assertEquals(
        0,
        categoryService.getCategoryOptionCombosByCategoryOption(sourceUids).size(),
        "Expect 0 entries with source category option refs");
    assertEquals(
        5,
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(target))).size(),
        "Expect 5 entries with target category option refs");
    assertEquals(4, report.getSourcesDeleted().size(), "Expect all 4 source options to be deleted");
    assertTrue(
        sources.stream().allMatch(s -> categoryService.getCategoryOption(s.getUid()) == null),
        "No source options should exist after delete");
    assertNotNull(
        categoryService.getCategoryOption(target.getUid()), "Target option should remain");
  }

  @Test
  @DisplayName("Merge with multiple sources is rejected when one source has different Categories")
  void multipleSourcesOneDifferentCategoryRejectedTest() {
    // given 4 sources + target that share a Category
    MultiSourceFixture fixture = setupMultiSourceSameCategory("mcoMS3");
    List<CategoryOption> sources = new ArrayList<>(fixture.sources());
    CategoryOption target = fixture.target();

    // and a 5th source that belongs to a different Category
    CategoryOption differentCatOption =
        createCategoryOption("mcoMS3-diff", CodeGenerator.generateUid());
    categoryService.addCategoryOption(differentCatOption);
    Category differentCategory = createCategory("mcoMS3-diffCat", differentCatOption);
    categoryService.addCategory(differentCategory);
    sources.add(differentCatOption);

    // when merging all 5 sources
    MergeParams mergeParams = getMergeParams(sources, target);
    mergeParams.setDeleteSources(true);
    ConflictException exception =
        assertThrows(
            ConflictException.class, () -> categoryOptionMergeService.processMerge(mergeParams));

    // then only the mismatched source produces a validation error and no merge is performed
    assertTrue(exception.getMessage().contains("Merge validation error"));
    assertEquals(
        1,
        exception.getMergeReport().getMergeErrors().size(),
        "Expect exactly 1 validation error for the mismatched source");
    assertTrue(
        sources.stream().allMatch(s -> categoryService.getCategoryOption(s.getUid()) != null),
        "No sources should be deleted when validation fails");
  }

  /**
   * Sets up a fixture where 4 source {@link CategoryOption}s and 1 target CategoryOption all belong
   * to the same {@link Category} (identical Category membership). A second single-option Category
   * and a {@link CategoryCombo} are added so {@link CategoryOptionCombo}s are generated: one per
   * option in the shared Category (5 in total).
   */
  private MultiSourceFixture setupMultiSourceSameCategory(String id) {
    CategoryOption s1 = createCategoryOption(id + "-s1", CodeGenerator.generateUid());
    CategoryOption s2 = createCategoryOption(id + "-s2", CodeGenerator.generateUid());
    CategoryOption s3 = createCategoryOption(id + "-s3", CodeGenerator.generateUid());
    CategoryOption s4 = createCategoryOption(id + "-s4", CodeGenerator.generateUid());
    CategoryOption target = createCategoryOption(id + "-t", CodeGenerator.generateUid());
    CategoryOption other = createCategoryOption(id + "-o", CodeGenerator.generateUid());
    categoryService.addCategoryOption(s1);
    categoryService.addCategoryOption(s2);
    categoryService.addCategoryOption(s3);
    categoryService.addCategoryOption(s4);
    categoryService.addCategoryOption(target);
    categoryService.addCategoryOption(other);

    // shared Category containing the 4 sources + target -> all have identical Category membership
    Category shared = createCategory(id + "-A", s1, s2, s3, s4, target);
    Category second = createCategory(id + "-B", other);
    categoryService.addCategory(shared);
    categoryService.addCategory(second);

    // CategoryCombo drives generation of one COC per option in the shared Category
    CategoryCombo cc = createCategoryCombo(id + "-cc", shared, second);
    categoryService.addCategoryCombo(cc);
    categoryOptionComboGenerateService.addAndPruneOptionCombos(cc);

    return new MultiSourceFixture(List.of(s1, s2, s3, s4), target);
  }

  private record MultiSourceFixture(List<CategoryOption> sources, CategoryOption target) {}

  private MergeParams getMergeParams(List<CategoryOption> sources, CategoryOption target) {
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(sources.toArray(new CategoryOption[0])));
    mergeParams.setTarget(UID.of(target));
    return mergeParams;
  }
}
