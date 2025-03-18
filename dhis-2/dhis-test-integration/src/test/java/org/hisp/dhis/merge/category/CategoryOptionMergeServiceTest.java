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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.hypersistence.utils.jdbc.validator.SQLStatementCountValidator;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.CategoryDimensionStore;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryService;
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
  @DisplayName("Category refs to source CategoryOptions are replaced, sources not deleted")
  void categoryRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given category state before merge
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco1");
    CategoryOption coSource1 = categoryMetadata.co1();
    CategoryOption coSource2 = categoryMetadata.co2();
    CategoryOption coTarget = categoryMetadata.co3();

    List<Category> categorySourcesBefore =
        categoryService.getCategoriesByCategoryOption(UID.of(coSource1, coSource2));
    List<Category> categoryTargetBefore =
        categoryService.getCategoriesByCategoryOption(Set.of(UID.of(coTarget)));

    assertEquals(
        1, categorySourcesBefore.size(), "Expect 1 category with source category option refs");
    assertEquals(
        1, categoryTargetBefore.size(), "Expect 1 category with target category option refs");

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource1, coSource2), coTarget);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<Category> categorySources =
        categoryService.getCategoriesByCategoryOption(UID.of(coSource1, coSource2));
    List<Category> categoryTarget =
        categoryService.getCategoriesByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, categorySources.size(), "Expect 0 entries with source category option refs");
    assertEquals(2, categoryTarget.size(), "Expect 2 entries with target category option refs");

    // 4 custom + 1 default
    assertEquals(5, allCategoryOptions.size(), "Expect 5 category options present");
    assertTrue(allCategoryOptions.containsAll(categoryMetadata.getCategoryOptions()));
  }

  @Test
  @DisplayName("Category refs to source CategoryOptions are replaced, sources are deleted")
  void categoryRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given category state before merge
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco2");
    CategoryOption coSource1 = categoryMetadata.co1();
    CategoryOption coSource2 = categoryMetadata.co2();
    CategoryOption coTarget = categoryMetadata.co3();
    List<Category> categorySourcesBefore =
        categoryService.getCategoriesByCategoryOption(UID.of(coSource1, coSource2));
    List<Category> categoryTargetBefore =
        categoryService.getCategoriesByCategoryOption(List.of(UID.of(coTarget)));

    assertEquals(
        1, categorySourcesBefore.size(), "Expect 2 category with source category option refs");
    assertEquals(
        1, categoryTargetBefore.size(), "Expect 1 category with target category option refs");

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource1, coSource2), coTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<Category> categorySources =
        categoryService.getCategoriesByCategoryOption(UID.of(coSource1, coSource2));
    List<Category> categoryTarget =
        categoryService.getCategoriesByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, categorySources.size(), "Expect 0 entries with source category option refs");
    assertEquals(2, categoryTarget.size(), "Expect 3 entries with target category option refs");

    // 2 custom + 1 default
    assertEquals(3, allCategoryOptions.size(), "Expect 3 category options present");
    assertTrue(allCategoryOptions.contains(coTarget));
    assertFalse(
        allCategoryOptions.containsAll(List.of(coSource1, coSource2)),
        "source cat options are no longer present");
  }

  // -----------------------------
  // ---- CategoryOptionCombo ----
  // -----------------------------
  @Test
  @DisplayName(
      "CategoryOptionCombo refs to source CategoryOptions are replaced, sources not deleted")
  void catOptComboRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco3");
    CategoryOption coSource1 = categoryMetadata.co1();
    CategoryOption coSource2 = categoryMetadata.co2();
    CategoryOption coTarget = categoryMetadata.co3();

    // confirm cat option combo state before merge
    List<CategoryOptionCombo> sourceCocsBefore =
        categoryService.getCategoryOptionCombosByCategoryOption(UID.of(coSource1, coSource2));
    List<CategoryOptionCombo> targetCocsBefore =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(coTarget)));

    assertEquals(4, sourceCocsBefore.size(), "Expect 4 entries with source category option refs");
    assertEquals(2, targetCocsBefore.size(), "Expect 2 entries with target category option refs");

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource1, coSource2), coTarget);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryOptionCombo> sourceCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(UID.of(coSource1, coSource2));
    List<CategoryOptionCombo> targetCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceCocs.size(), "Expect 0 entries with source category option refs");
    assertEquals(4, targetCocs.size(), "Expect 4 entries with target category option refs");
    assertEquals(5, allCategoryOptions.size(), "Expect 5 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget, coSource1, coSource2)));
  }

  @Test
  @DisplayName("Expect the correct number of SQL delete queries when merging")
  void catOptMergeQueryTest() throws ConflictException {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco4");
    CategoryOption coSource1 = categoryMetadata.co1();
    CategoryOption coTarget = categoryMetadata.co3();

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource1), coTarget);
    mergeParams.setDeleteSources(true);

    SQLStatementCountValidator.reset();
    categoryOptionMergeService.processMerge(mergeParams);

    // then
    assertDeleteCount(2);
    assertNull(
        categoryService.getCategoryOption(coSource1.getUid()),
        "source cat option should not exist");
  }

  @Test
  @DisplayName(
      "CategoryOptionCombo refs to source CategoryOptions are replaced, sources are deleted")
  void catOptComboRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco5");
    CategoryOption coSource1 = categoryMetadata.co1();
    CategoryOption coSource2 = categoryMetadata.co2();
    CategoryOption coTarget = categoryMetadata.co3();

    // confirm cat option combos state before merge
    List<CategoryOptionCombo> sourceCocsBefore =
        categoryService.getCategoryOptionCombosByCategoryOption(UID.of(coSource1, coSource2));
    List<CategoryOptionCombo> targetCocsBefore =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(coTarget)));

    assertEquals(4, sourceCocsBefore.size(), "Expect 4 entries with source category option refs");
    assertEquals(2, targetCocsBefore.size(), "Expect 2 entries with target category option refs");

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource1, coSource2), coTarget);
    mergeParams.setDeleteSources(true);

    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryOptionCombo> sourceCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(UID.of(coSource1, coSource2));
    List<CategoryOptionCombo> targetCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceCocs.size(), "Expect 0 entries with source category option refs");
    assertEquals(4, targetCocs.size(), "Expect 4 entries with target category option refs");

    // 6 custom + 1 default
    assertEquals(3, allCategoryOptions.size(), "Expect 3 category options present");
    assertTrue(allCategoryOptions.contains(coTarget));
    assertFalse(allCategoryOptions.containsAll(List.of(coSource1, coSource2)));
  }

  // -----------------------------
  // --------- Org Unit ----------
  // -----------------------------
  @Test
  @DisplayName("OrgUnit refs to source CategoryOptions are replaced, sources not deleted")
  void orgUnitRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco7");
    CategoryOption coSource1 = categoryMetadata.co1();
    CategoryOption coSource2 = categoryMetadata.co2();
    CategoryOption coTarget = categoryMetadata.co3();

    OrganisationUnit ou1 = createOrganisationUnit('x');
    ou1.addCategoryOption(coSource1);

    OrganisationUnit ou2 = createOrganisationUnit('y');
    ou2.addCategoryOption(coSource2);

    OrganisationUnit ou3 = createOrganisationUnit('z');
    ou3.addCategoryOption(coTarget);

    manager.save(List.of(ou1, ou2, ou3));

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource1, coSource2), coTarget);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<OrganisationUnit> orgUnitSources =
        organisationUnitService.getByCategoryOption(UID.of(coSource1, coSource2));
    List<OrganisationUnit> orgUnitTarget =
        organisationUnitService.getByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, orgUnitSources.size(), "Expect 0 entries with source org units refs");
    assertEquals(3, orgUnitTarget.size(), "Expect 3 entries with target org unit refs");

    assertEquals(5, allCategoryOptions.size(), "Expect 5 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget, coSource1, coSource2)));
  }

  @Test
  @DisplayName("OrgUnit refs to source CategoryOptions are replaced, sources deleted")
  void orgUnitRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco8");
    CategoryOption coSource1 = categoryMetadata.co1();
    CategoryOption coSource2 = categoryMetadata.co2();
    CategoryOption coTarget = categoryMetadata.co3();

    OrganisationUnit ou1 = createOrganisationUnit('x');
    ou1.addCategoryOption(coSource1);

    OrganisationUnit ou2 = createOrganisationUnit('y');
    ou2.addCategoryOption(coSource2);

    OrganisationUnit ou3 = createOrganisationUnit('z');
    ou3.addCategoryOption(coTarget);

    manager.save(List.of(ou1, ou2, ou3));

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource1, coSource2), coTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<OrganisationUnit> orgUnitSources =
        organisationUnitService.getByCategoryOption(UID.of(coSource1, coSource2));
    List<OrganisationUnit> orgUnitTarget =
        organisationUnitService.getByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, orgUnitSources.size(), "Expect 0 entries with source org units refs");
    assertEquals(3, orgUnitTarget.size(), "Expect 3 entries with target org unit refs");

    assertEquals(3, allCategoryOptions.size(), "Expect 3 category options present");
    assertTrue(allCategoryOptions.contains(coTarget));
    assertFalse(allCategoryOptions.containsAll(List.of(coSource1, coSource2)));
  }

  @Test
  @DisplayName(
      "1 OrgUnit with refs to source & target CategoryOption results in OrgUnit with just target")
  void orgUnitSourceAndTargetRefsTest() throws ConflictException {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco9");
    CategoryOption coSource1 = categoryMetadata.co1();
    CategoryOption coSource2 = categoryMetadata.co2();
    CategoryOption coTarget = categoryMetadata.co3();

    OrganisationUnit ou = createOrganisationUnit('o');
    ou.addCategoryOption(coSource1);
    ou.addCategoryOption(coSource2);
    ou.addCategoryOption(coTarget);

    manager.save(ou);

    // confirm org unit state before merge
    List<OrganisationUnit> sourceOusBefore =
        organisationUnitService.getByCategoryOption(UID.of(coSource1, coSource2));
    List<OrganisationUnit> targetOusBefore =
        organisationUnitService.getByCategoryOption(List.of(UID.of(coTarget)));

    assertEquals(1, sourceOusBefore.size(), "Expect 1 entry with source category option refs");
    assertEquals(1, targetOusBefore.size(), "Expect 1 entry with target category option ref");

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource1, coSource2), coTarget);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<OrganisationUnit> sourceOus =
        organisationUnitService.getByCategoryOption(UID.of(coSource1, coSource2));
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
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget, coSource1, coSource2)));
  }

  // -----------------------------
  // --- Category Option Group ---
  // -----------------------------
  @Test
  @DisplayName(
      "CategoryOptionGroup refs to source CategoryOptions are replaced, sources not deleted")
  void catOptionGroupSourcesNotDeletedTest() throws ConflictException {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco10");
    CategoryOption coSource1 = categoryMetadata.co1();
    CategoryOption coSource2 = categoryMetadata.co2();
    CategoryOption coTarget = categoryMetadata.co3();

    CategoryOptionGroup cog1 = createCategoryOptionGroup('x');
    cog1.addCategoryOption(coSource1);

    CategoryOptionGroup cog2 = createCategoryOptionGroup('y');
    cog2.addCategoryOption(coSource2);

    CategoryOptionGroup cog3 = createCategoryOptionGroup('z');
    cog3.addCategoryOption(coTarget);

    manager.save(List.of(cog1, cog2, cog3));

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource1, coSource2), coTarget);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryOptionGroup> cogSources =
        categoryService.getCategoryOptionGroupByCategoryOption(UID.of(coSource1, coSource2));
    List<CategoryOptionGroup> cogTarget =
        categoryService.getCategoryOptionGroupByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, cogSources.size(), "Expect 0 entries with source cat option group refs");
    assertEquals(3, cogTarget.size(), "Expect 3 entries with target cat option group refs");

    assertEquals(5, allCategoryOptions.size(), "Expect 5 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget, coSource1, coSource2)));
  }

  @Test
  @DisplayName("CategoryOptionGroup refs to source CategoryOptions are replaced, sources deleted")
  void catOptionGroupSourcesDeletedTest() throws ConflictException {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco11");
    CategoryOption coSource1 = categoryMetadata.co1();
    CategoryOption coSource2 = categoryMetadata.co2();
    CategoryOption coTarget = categoryMetadata.co3();

    CategoryOptionGroup cog1 = createCategoryOptionGroup('x');
    cog1.addCategoryOption(coSource1);

    CategoryOptionGroup cog2 = createCategoryOptionGroup('y');
    cog2.addCategoryOption(coSource2);

    CategoryOptionGroup cog3 = createCategoryOptionGroup('z');
    cog3.addCategoryOption(coTarget);

    manager.save(List.of(cog1, cog2, cog3));

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource1, coSource2), coTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryOptionGroup> cogSources =
        categoryService.getCategoryOptionGroupByCategoryOption(UID.of(coSource1, coSource2));
    List<CategoryOptionGroup> cogTarget =
        categoryService.getCategoryOptionGroupByCategoryOption(List.of(UID.of(coTarget)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, cogSources.size(), "Expect 0 entries with source cat option group refs");
    assertEquals(3, cogTarget.size(), "Expect 3 entries with target cat option group refs");

    assertEquals(3, allCategoryOptions.size(), "Expect 3 category options present");
    assertTrue(allCategoryOptions.contains(coTarget));
    assertFalse(allCategoryOptions.containsAll(List.of(coSource1, coSource2)));
  }

  @Test
  @DisplayName(
      "1 CategoryOptionGroup with refs to source & target CategoryOption results in CategoryOptionGroup with just target")
  void catOptGroupSourceAndTargetRefsTest() throws ConflictException {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco12");
    CategoryOption coSource1 = categoryMetadata.co1();
    CategoryOption coSource2 = categoryMetadata.co2();
    CategoryOption coTarget = categoryMetadata.co3();

    CategoryOptionGroup cog = createCategoryOptionGroup('g');
    cog.addCategoryOption(coSource1);
    cog.addCategoryOption(coSource2);
    cog.addCategoryOption(coTarget);
    manager.save(cog);

    // confirm cat option combo state before merge
    List<CategoryOptionGroup> sourceCogsBefore =
        categoryService.getCategoryOptionGroupByCategoryOption(UID.of(coSource1, coSource2));
    List<CategoryOptionGroup> targetCogsBefore =
        categoryService.getCategoryOptionGroupByCategoryOption(List.of(UID.of(coTarget)));

    assertEquals(1, sourceCogsBefore.size(), "Expect 1 entry with source category option ref");
    assertEquals(1, targetCogsBefore.size(), "Expect 1 entry with target category option ref");

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource1, coSource2), coTarget);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryOptionGroup> sourceCogs =
        categoryService.getCategoryOptionGroupByCategoryOption(UID.of(coSource1, coSource2));
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
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget, coSource1, coSource2)));
  }

  // -----------------------------
  // ----- Category Dimension ----
  // -----------------------------
  @Test
  @DisplayName("CategoryDimension refs to source CategoryOptions are replaced, sources not deleted")
  void catDimensionSourcesNotDeletedTest() throws ConflictException {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco13");
    CategoryOption coSource1 = categoryMetadata.co1();
    CategoryOption coSource2 = categoryMetadata.co2();
    CategoryOption coTarget = categoryMetadata.co3();

    CategoryDimension cd1 = createCategoryDimension(categoryMetadata.c1());
    cd1.getItems().add(coSource1);

    CategoryDimension cd2 = createCategoryDimension(categoryMetadata.c1());
    cd2.getItems().add(coSource2);

    CategoryDimension cd3 = createCategoryDimension(categoryMetadata.c2());
    cd3.getItems().add(coTarget);

    dimensionStore.save(cd1);
    dimensionStore.save(cd2);
    dimensionStore.save(cd3);

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource1, coSource2), coTarget);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryDimension> cogSources =
        dimensionStore.getByCategoryOption(List.of(coSource1.getUid(), coSource2.getUid()));
    List<CategoryDimension> cogTarget =
        dimensionStore.getByCategoryOption(List.of(coTarget.getUid()));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, cogSources.size(), "Expect 0 entries with source category dimension refs");
    assertEquals(3, cogTarget.size(), "Expect 3 entries with target category dimension refs");

    assertEquals(5, allCategoryOptions.size(), "Expect 5 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget, coSource1, coSource2)));
  }

  @Test
  @DisplayName("CategoryDimension refs to source CategoryOptions are replaced, sources deleted")
  void catDimensionSourcesDeletedTest() throws ConflictException {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mco13");
    CategoryOption coSource1 = categoryMetadata.co1();
    CategoryOption coSource2 = categoryMetadata.co2();
    CategoryOption coTarget = categoryMetadata.co3();

    CategoryDimension cd1 = createCategoryDimension(categoryMetadata.c1());
    cd1.getItems().add(coSource1);

    CategoryDimension cd2 = createCategoryDimension(categoryMetadata.c1());
    cd2.getItems().add(coSource2);

    CategoryDimension cd3 = createCategoryDimension(categoryMetadata.c2());
    cd3.getItems().add(coTarget);

    dimensionStore.save(cd1);
    dimensionStore.save(cd2);
    dimensionStore.save(cd3);

    // when
    MergeParams mergeParams = getMergeParams(List.of(coSource1, coSource2), coTarget);
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryDimension> cdSources =
        dimensionStore.getByCategoryOption(List.of(coSource1.getUid(), coSource2.getUid()));
    List<CategoryDimension> cdTarget =
        dimensionStore.getByCategoryOption(List.of(coTarget.getUid()));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, cdSources.size(), "Expect 0 entries with source category dimension refs");
    assertEquals(3, cdTarget.size(), "Expect 3 entries with target category dimension refs");

    assertEquals(3, allCategoryOptions.size(), "Expect 3 category options present");
    assertTrue(allCategoryOptions.contains(coTarget));
    assertFalse(allCategoryOptions.containsAll(List.of(coSource1, coSource2)));
  }

  private MergeParams getMergeParams(List<CategoryOption> sources, CategoryOption target) {
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(sources.toArray(new CategoryOption[0])));
    mergeParams.setTarget(UID.of(target));
    return mergeParams;
  }
}
