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
import org.hisp.dhis.test.config.QueryCountDataSourceProxy;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
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

  private Category cat1;
  private Category cat2;
  private Category cat3;
  private Category cat4;
  private CategoryOption coSource1A;
  private CategoryOption co1B;
  private CategoryOption co2A;
  private CategoryOption coSource2B;
  private CategoryOption coTarget3A;
  private CategoryOption co3B;
  private CategoryOption co4A;
  private CategoryOption co4B;

  @BeforeEach
  public void setUp() {
    // 8 category options
    coSource1A = createCategoryOption("1A source", CodeGenerator.generateUid());
    co1B = createCategoryOption("1B", CodeGenerator.generateUid());
    co2A = createCategoryOption("2A", CodeGenerator.generateUid());
    coSource2B = createCategoryOption("2B source", CodeGenerator.generateUid());
    coTarget3A = createCategoryOption("3A target", CodeGenerator.generateUid());
    co3B = createCategoryOption("3B", CodeGenerator.generateUid());
    co4A = createCategoryOption("4A", CodeGenerator.generateUid());
    co4B = createCategoryOption("4B", CodeGenerator.generateUid());
    categoryService.addCategoryOption(coSource1A);
    categoryService.addCategoryOption(co1B);
    categoryService.addCategoryOption(co2A);
    categoryService.addCategoryOption(coSource2B);
    categoryService.addCategoryOption(coTarget3A);
    categoryService.addCategoryOption(co3B);
    categoryService.addCategoryOption(co4A);
    categoryService.addCategoryOption(co4B);

    // 4 categories (each with 2 category options)
    cat1 = createCategory('1', coSource1A, co1B);
    cat2 = createCategory('2', co2A, coSource2B);
    cat3 = createCategory('3', coTarget3A, co3B);
    cat4 = createCategory('4', co4A, co4B);
    categoryService.addCategory(cat1);
    categoryService.addCategory(cat2);
    categoryService.addCategory(cat3);
    categoryService.addCategory(cat4);
  }

  // -----------------------------
  // --------- Category ----------
  // -----------------------------
  @Test
  @DisplayName("Category refs to source CategoryOptions are replaced, sources not deleted")
  void categoryRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given category state before merge
    List<Category> categorySourcesBefore =
        categoryService.getCategoriesByCategoryOption(
            List.of(UID.of(coSource1A.getUid()), UID.of(coSource2B.getUid())));
    List<Category> categoryTargetBefore =
        categoryService.getCategoriesByCategoryOption(List.of(UID.of(coTarget3A.getUid())));

    assertEquals(
        2, categorySourcesBefore.size(), "Expect 2 categories with source category option refs");
    assertEquals(
        1, categoryTargetBefore.size(), "Expect 1 category with target category option refs");

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<Category> categorySources =
        categoryService.getCategoriesByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<Category> categoryTarget =
        categoryService.getCategoriesByCategoryOption(List.of(UID.of(coTarget3A)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, categorySources.size(), "Expect 0 entries with source category option refs");
    assertEquals(3, categoryTarget.size(), "Expect 3 entries with target category option refs");

    // 8 custom + 1 default
    assertEquals(9, allCategoryOptions.size(), "Expect 9 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget3A, coSource1A, coSource2B)));
  }

  @Test
  @DisplayName("Category refs to source CategoryOptions are replaced, sources are deleted")
  void categoryRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given category state before merge
    List<Category> categorySourcesBefore =
        categoryService.getCategoriesByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<Category> categoryTargetBefore =
        categoryService.getCategoriesByCategoryOption(List.of(UID.of(coTarget3A)));

    assertEquals(
        2, categorySourcesBefore.size(), "Expect 2 categories with source category option refs");
    assertEquals(
        1, categoryTargetBefore.size(), "Expect 1 category with target category option refs");

    // when
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<Category> categorySources =
        categoryService.getCategoriesByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<Category> categoryTarget =
        categoryService.getCategoriesByCategoryOption(List.of(UID.of(coTarget3A)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, categorySources.size(), "Expect 0 entries with source category option refs");
    assertEquals(3, categoryTarget.size(), "Expect 3 entries with target category option refs");

    // 6 custom + 1 default
    assertEquals(7, allCategoryOptions.size(), "Expect 7 category options present");
    assertTrue(allCategoryOptions.contains(coTarget3A));
    assertFalse(allCategoryOptions.containsAll(List.of(coSource1A, coSource2B)));
  }

  // -----------------------------
  // ---- CategoryOptionCombo ----
  // -----------------------------
  @Test
  @DisplayName(
      "CategoryOptionCombo refs to source CategoryOptions are replaced, sources not deleted")
  void catOptComboRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given
    CategoryCombo cc1 = createCategoryCombo('1', cat1, cat2);
    CategoryCombo cc2 = createCategoryCombo('2', cat3, cat4);
    manager.save(List.of(cc1, cc2));
    // these calls generate cat option combos
    categoryService.updateOptionCombos(cc1);
    categoryService.updateOptionCombos(cc2);

    // confirm cat option combo state before merge
    List<CategoryOptionCombo> sourceCocsBefore =
        categoryService.getCategoryOptionCombosByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<CategoryOptionCombo> targetCocsBefore =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(coTarget3A)));

    assertEquals(3, sourceCocsBefore.size(), "Expect 3 entries with source category option refs");
    assertEquals(2, targetCocsBefore.size(), "Expect 2 entries with target category option refs");

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryOptionCombo> sourceCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<CategoryOptionCombo> targetCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(coTarget3A)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceCocs.size(), "Expect 0 entries with source category option refs");
    assertEquals(5, targetCocs.size(), "Expect 5 entries with target category option refs");
    assertEquals(9, allCategoryOptions.size(), "Expect 9 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget3A, coSource1A, coSource2B)));
  }

  @Test
  @DisplayName("Expect the correct number of SQL delete queries when merging")
  void catOptMergeQueryTest() throws ConflictException {
    // given
    CategoryCombo cc1 = createCategoryCombo('1', cat1);
    CategoryCombo cc2 = createCategoryCombo('2', cat3);
    manager.save(List.of(cc1, cc2));

    categoryService.updateOptionCombos(cc1);
    categoryService.updateOptionCombos(cc2);

    // when
    MergeParams mergeParams = getMergeParams();
    mergeParams.setSources(Set.of(UID.of(coSource1A)));
    mergeParams.setDeleteSources(true);

    SQLStatementCountValidator.reset();
    categoryOptionMergeService.processMerge(mergeParams);

    // then
    assertDeleteCount(1);
    assertNull(
        categoryService.getCategoryOption(coSource1A.getUid()),
        "source cat option should not exist");
  }

  @Test
  @DisplayName(
      "CategoryOptionCombo refs to source CategoryOptions are replaced, sources are deleted")
  void catOptComboRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given
    CategoryCombo cc1 = createCategoryCombo('1', cat1, cat2);
    CategoryCombo cc2 = createCategoryCombo('2', cat3, cat4);
    manager.save(List.of(cc1, cc2));
    categoryService.updateOptionCombos(cc1);
    categoryService.updateOptionCombos(cc2);

    // confirm cat option combos state before merge
    List<CategoryOptionCombo> sourceCocsBefore =
        categoryService.getCategoryOptionCombosByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<CategoryOptionCombo> targetCocsBefore =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(coTarget3A)));

    assertEquals(3, sourceCocsBefore.size(), "Expect 3 entries with source category option refs");
    assertEquals(2, targetCocsBefore.size(), "Expect 2 entries with target category option refs");

    // when
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryOptionCombo> sourceCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<CategoryOptionCombo> targetCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(coTarget3A)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceCocs.size(), "Expect 0 entries with source category option refs");
    assertEquals(5, targetCocs.size(), "Expect 5 entries with target category option refs");

    // 6 custom + 1 default
    assertEquals(7, allCategoryOptions.size(), "Expect 7 category options present");
    assertTrue(allCategoryOptions.contains(coTarget3A));
    assertFalse(allCategoryOptions.containsAll(List.of(coSource1A, coSource2B)));
  }

  @Test
  @DisplayName(
      "1 CategoryOptionCombo with refs to source & target CategoryOption results in CategoryOptionCombo with just target")
  void catOptComboSourceAndTargetRefsTest() throws ConflictException {
    // given
    CategoryCombo cc1 = createCategoryCombo('1', cat1, cat2);
    manager.save(cc1);

    CategoryOptionCombo coc = createCategoryOptionCombo(cc1, coSource1A, coSource2B, coTarget3A);
    manager.save(coc);

    // confirm cat option combo state before merge
    List<CategoryOptionCombo> sourceCocsBefore =
        categoryService.getCategoryOptionCombosByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<CategoryOptionCombo> targetCocsBefore =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(coTarget3A)));

    assertEquals(1, sourceCocsBefore.size(), "Expect 1 entry with source category option ref");
    assertEquals(1, targetCocsBefore.size(), "Expect 1 entry with target category option ref");

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryOptionCombo> sourceCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<CategoryOptionCombo> targetCocs =
        categoryService.getCategoryOptionCombosByCategoryOption(List.of(UID.of(coTarget3A)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();
    List<CategoryOption> catOptions =
        targetCocs.stream()
            .flatMap(optionCombo -> optionCombo.getCategoryOptions().stream())
            .toList();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceCocs.size(), "Expect 0 entries with source category option refs");
    assertEquals(1, targetCocs.size(), "Expect 1 entry with target category option ref");
    assertEquals(1, catOptions.size());
    assertTrue(catOptions.contains(coTarget3A));
    assertEquals(9, allCategoryOptions.size(), "Expect 9 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget3A, coSource1A, coSource2B)));
  }

  // -----------------------------
  // --------- Org Unit ----------
  // -----------------------------
  @Test
  @DisplayName("OrgUnit refs to source CategoryOptions are replaced, sources not deleted")
  void orgUnitRefsReplacedSourcesNotDeletedTest() throws ConflictException {
    // given
    OrganisationUnit ou1 = createOrganisationUnit('x');
    ou1.addCategoryOption(coSource1A);
    ou1.addCategoryOption(co1B);

    OrganisationUnit ou2 = createOrganisationUnit('y');
    ou2.addCategoryOption(coSource2B);
    ou2.addCategoryOption(co2A);

    OrganisationUnit ou3 = createOrganisationUnit('z');
    ou3.addCategoryOption(coTarget3A);
    ou3.addCategoryOption(co4A);

    OrganisationUnit ou4 = createOrganisationUnit('p');
    ou4.addCategoryOption(co3B);
    ou4.addCategoryOption(co4B);

    manager.save(List.of(ou1, ou2, ou3, ou4));

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<OrganisationUnit> orgUnitSources =
        organisationUnitService.getByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<OrganisationUnit> orgUnitTarget =
        organisationUnitService.getByCategoryOption(List.of(UID.of(coTarget3A)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, orgUnitSources.size(), "Expect 0 entries with source org units refs");
    assertEquals(3, orgUnitTarget.size(), "Expect 3 entries with target org unit refs");

    assertEquals(9, allCategoryOptions.size(), "Expect 9 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget3A, coSource1A, coSource2B)));
  }

  @Test
  @DisplayName("OrgUnit refs to source CategoryOptions are replaced, sources deleted")
  void orgUnitRefsReplacedSourcesDeletedTest() throws ConflictException {
    // given
    OrganisationUnit ou1 = createOrganisationUnit('x');
    ou1.addCategoryOption(coSource1A);
    ou1.addCategoryOption(co1B);

    OrganisationUnit ou2 = createOrganisationUnit('y');
    ou2.addCategoryOption(coSource2B);
    ou2.addCategoryOption(co2A);

    OrganisationUnit ou3 = createOrganisationUnit('z');
    ou3.addCategoryOption(coTarget3A);
    ou3.addCategoryOption(co4A);

    OrganisationUnit ou4 = createOrganisationUnit('p');
    ou4.addCategoryOption(co3B);
    ou4.addCategoryOption(co4B);

    manager.save(List.of(ou1, ou2, ou3, ou4));

    // when
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<OrganisationUnit> orgUnitSources =
        organisationUnitService.getByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<OrganisationUnit> orgUnitTarget =
        organisationUnitService.getByCategoryOption(List.of(UID.of(coTarget3A)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, orgUnitSources.size(), "Expect 0 entries with source org units refs");
    assertEquals(3, orgUnitTarget.size(), "Expect 3 entries with target org unit refs");

    assertEquals(7, allCategoryOptions.size(), "Expect 7 category options present");
    assertTrue(allCategoryOptions.contains(coTarget3A));
    assertFalse(allCategoryOptions.containsAll(List.of(coSource1A, coSource2B)));
  }

  @Test
  @DisplayName(
      "1 OrgUnit with refs to source & target CategoryOption results in OrgUnit with just target")
  void orgUnitSourceAndTargetRefsTest() throws ConflictException {
    // given
    OrganisationUnit ou = createOrganisationUnit('o');
    ou.addCategoryOption(coSource1A);
    ou.addCategoryOption(coSource2B);
    ou.addCategoryOption(coTarget3A);

    manager.save(ou);

    // confirm org unit state before merge
    List<OrganisationUnit> sourceOusBefore =
        organisationUnitService.getByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<OrganisationUnit> targetOusBefore =
        organisationUnitService.getByCategoryOption(List.of(UID.of(coTarget3A)));

    assertEquals(1, sourceOusBefore.size(), "Expect 1 entry with source category option refs");
    assertEquals(1, targetOusBefore.size(), "Expect 1 entry with target category option ref");

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<OrganisationUnit> sourceOus =
        organisationUnitService.getByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<OrganisationUnit> targetOus =
        organisationUnitService.getByCategoryOption(List.of(UID.of(coTarget3A)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();
    List<CategoryOption> catOptions =
        targetOus.stream().flatMap(orgUnit -> orgUnit.getCategoryOptions().stream()).toList();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceOus.size(), "Expect 0 entries with source category option refs");
    assertEquals(1, targetOus.size(), "Expect 1 entry with target category option ref");
    assertEquals(1, catOptions.size());
    assertTrue(catOptions.contains(coTarget3A));
    assertEquals(9, allCategoryOptions.size(), "Expect 9 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget3A, coSource1A, coSource2B)));
  }

  // -----------------------------
  // --- Category Option Group ---
  // -----------------------------
  @Test
  @DisplayName(
      "CategoryOptionGroup refs to source CategoryOptions are replaced, sources not deleted")
  void catOptionGroupSourcesNotDeletedTest() throws ConflictException {
    // given
    CategoryOptionGroup cog1 = createCategoryOptionGroup('x');
    cog1.addCategoryOption(coSource1A);
    cog1.addCategoryOption(co1B);

    CategoryOptionGroup cog2 = createCategoryOptionGroup('y');
    cog2.addCategoryOption(coSource2B);
    cog2.addCategoryOption(co2A);

    CategoryOptionGroup cog3 = createCategoryOptionGroup('z');
    cog3.addCategoryOption(coTarget3A);
    cog3.addCategoryOption(co4A);

    CategoryOptionGroup cog4 = createCategoryOptionGroup('p');
    cog4.addCategoryOption(co3B);
    cog4.addCategoryOption(co4B);

    manager.save(List.of(cog1, cog2, cog3, cog4));

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryOptionGroup> cogSources =
        categoryService.getCategoryOptionGroupByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<CategoryOptionGroup> cogTarget =
        categoryService.getCategoryOptionGroupByCategoryOption(List.of(UID.of(coTarget3A)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, cogSources.size(), "Expect 0 entries with source cat option group refs");
    assertEquals(3, cogTarget.size(), "Expect 3 entries with target cat option group refs");

    assertEquals(9, allCategoryOptions.size(), "Expect 9 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget3A, coSource1A, coSource2B)));
  }

  @Test
  @DisplayName("CategoryOptionGroup refs to source CategoryOptions are replaced, sources deleted")
  void catOptionGroupSourcesDeletedTest() throws ConflictException {
    // given
    CategoryOptionGroup cog1 = createCategoryOptionGroup('x');
    cog1.addCategoryOption(coSource1A);
    cog1.addCategoryOption(co1B);

    CategoryOptionGroup cog2 = createCategoryOptionGroup('y');
    cog2.addCategoryOption(coSource2B);
    cog2.addCategoryOption(co2A);

    CategoryOptionGroup cog3 = createCategoryOptionGroup('z');
    cog3.addCategoryOption(coTarget3A);
    cog3.addCategoryOption(co4A);

    CategoryOptionGroup cog4 = createCategoryOptionGroup('p');
    cog4.addCategoryOption(co3B);
    cog4.addCategoryOption(co4B);

    manager.save(List.of(cog1, cog2, cog3, cog4));

    // when
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryOptionGroup> cogSources =
        categoryService.getCategoryOptionGroupByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<CategoryOptionGroup> cogTarget =
        categoryService.getCategoryOptionGroupByCategoryOption(List.of(UID.of(coTarget3A)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, cogSources.size(), "Expect 0 entries with source cat option group refs");
    assertEquals(3, cogTarget.size(), "Expect 3 entries with target cat option group refs");

    assertEquals(7, allCategoryOptions.size(), "Expect 7 category options present");
    assertTrue(allCategoryOptions.contains(coTarget3A));
    assertFalse(allCategoryOptions.containsAll(List.of(coSource1A, coSource2B)));
  }

  @Test
  @DisplayName(
      "1 CategoryOptionGroup with refs to source & target CategoryOption results in CategoryOptionGroup with just target")
  void catOptGroupSourceAndTargetRefsTest() throws ConflictException {
    // given
    CategoryOptionGroup cog = createCategoryOptionGroup('g');
    cog.addCategoryOption(coSource1A);
    cog.addCategoryOption(coSource2B);
    cog.addCategoryOption(coTarget3A);
    manager.save(cog);

    // confirm cat option combo state before merge
    List<CategoryOptionGroup> sourceCogsBefore =
        categoryService.getCategoryOptionGroupByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<CategoryOptionGroup> targetCogsBefore =
        categoryService.getCategoryOptionGroupByCategoryOption(List.of(UID.of(coTarget3A)));

    assertEquals(1, sourceCogsBefore.size(), "Expect 1 entry with source category option ref");
    assertEquals(1, targetCogsBefore.size(), "Expect 1 entry with target category option ref");

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryOptionGroup> sourceCogs =
        categoryService.getCategoryOptionGroupByCategoryOption(
            List.of(UID.of(coSource1A), UID.of(coSource2B)));
    List<CategoryOptionGroup> targetCogs =
        categoryService.getCategoryOptionGroupByCategoryOption(List.of(UID.of(coTarget3A)));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();
    List<CategoryOption> catOptions =
        targetCogs.stream().flatMap(catOptGroup -> catOptGroup.getMembers().stream()).toList();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, sourceCogs.size(), "Expect 0 entries with source category option refs");
    assertEquals(1, targetCogs.size(), "Expect 1 entry with target category option ref");
    assertEquals(1, catOptions.size());
    assertTrue(catOptions.contains(coTarget3A));
    assertEquals(9, allCategoryOptions.size(), "Expect 9 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget3A, coSource1A, coSource2B)));
  }

  // -----------------------------
  // ----- Category Dimension ----
  // -----------------------------
  @Test
  @DisplayName("CategoryDimension refs to source CategoryOptions are replaced, sources not deleted")
  void catDimensionSourcesNotDeletedTest() throws ConflictException {
    // given
    CategoryDimension cd1 = createCategoryDimension(cat1);
    cd1.getItems().add(coSource1A);

    CategoryDimension cd2 = createCategoryDimension(cat2);
    cd2.getItems().add(coSource2B);

    CategoryDimension cd3 = createCategoryDimension(cat3);
    cd3.getItems().add(coTarget3A);

    CategoryDimension cd4 = createCategoryDimension(cat4);
    cd4.getItems().add(co3B);

    dimensionStore.save(cd1);
    dimensionStore.save(cd2);
    dimensionStore.save(cd3);
    dimensionStore.save(cd4);

    // when
    MergeParams mergeParams = getMergeParams();
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryDimension> cogSources =
        dimensionStore.getByCategoryOption(List.of(coSource1A.getUid(), coSource2B.getUid()));
    List<CategoryDimension> cogTarget =
        dimensionStore.getByCategoryOption(List.of(coTarget3A.getUid()));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, cogSources.size(), "Expect 0 entries with source category dimension refs");
    assertEquals(3, cogTarget.size(), "Expect 3 entries with target category dimension refs");

    assertEquals(9, allCategoryOptions.size(), "Expect 9 category options present");
    assertTrue(allCategoryOptions.containsAll(List.of(coTarget3A, coSource1A, coSource2B)));
  }

  @Test
  @DisplayName("CategoryDimension refs to source CategoryOptions are replaced, sources deleted")
  void catDimensionSourcesDeletedTest() throws ConflictException {
    // given
    CategoryDimension cd1 = createCategoryDimension(cat1);
    cd1.getItems().add(coSource1A);

    CategoryDimension cd2 = createCategoryDimension(cat2);
    cd2.getItems().add(coSource2B);

    CategoryDimension cd3 = createCategoryDimension(cat3);
    cd3.getItems().add(coTarget3A);

    CategoryDimension cd4 = createCategoryDimension(cat4);
    cd4.getItems().add(co3B);

    dimensionStore.save(cd1);
    dimensionStore.save(cd2);
    dimensionStore.save(cd3);
    dimensionStore.save(cd4);

    // when
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);
    MergeReport report = categoryOptionMergeService.processMerge(mergeParams);

    // then
    List<CategoryDimension> cdSources =
        dimensionStore.getByCategoryOption(List.of(coSource1A.getUid(), coSource2B.getUid()));
    List<CategoryDimension> cdTarget =
        dimensionStore.getByCategoryOption(List.of(coTarget3A.getUid()));
    List<CategoryOption> allCategoryOptions = categoryService.getAllCategoryOptions();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, cdSources.size(), "Expect 0 entries with source category dimension refs");
    assertEquals(3, cdTarget.size(), "Expect 3 entries with target category dimension refs");

    assertEquals(7, allCategoryOptions.size(), "Expect 7 category options present");
    assertTrue(allCategoryOptions.contains(coTarget3A));
    assertFalse(allCategoryOptions.containsAll(List.of(coSource1A, coSource2B)));
  }

  private MergeParams getMergeParams() {
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(List.of(coSource1A.getUid(), coSource2B.getUid())));
    mergeParams.setTarget(UID.of(coTarget3A.getUid()));
    return mergeParams;
  }
}
