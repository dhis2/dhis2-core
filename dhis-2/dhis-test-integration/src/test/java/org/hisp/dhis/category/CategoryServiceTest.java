/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.category;

import static org.hisp.dhis.dxf2.importsummary.ImportStatus.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.test.api.TestCategoryMetadata;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Transactional
class CategoryServiceTest extends PostgresIntegrationTestBase {

  private DataElement deA;

  private DataElement deB;

  private CategoryOption categoryOptionA;

  private CategoryOption categoryOptionB;

  private CategoryOption categoryOptionC;

  private Category categoryA;

  private Category categoryB;

  private Category categoryC;

  private CategoryCombo ccA;

  private List<CategoryOption> categoryOptions;

  @Autowired private CategoryService categoryService;

  @Autowired private IdentifiableObjectManager idObjectManager;

  @BeforeEach
  void setUp() {
    categoryOptionA = createCategoryOption('A');
    categoryOptionB = createCategoryOption('B');
    categoryOptionC = createCategoryOption('C');
    categoryService.addCategoryOption(categoryOptionA);
    categoryService.addCategoryOption(categoryOptionB);
    categoryService.addCategoryOption(categoryOptionC);
    categoryOptions = new ArrayList<>();
    categoryOptions.add(categoryOptionA);
    categoryOptions.add(categoryOptionB);
    categoryOptions.add(categoryOptionC);
  }

  @Test
  void testAddGet() {
    categoryA = createCategory('A', categoryOptionA, categoryOptionB, categoryOptionC);
    categoryB = createCategory('B', categoryOptionA, categoryOptionB, categoryOptionC);
    categoryC = createCategory('C', categoryOptionA, categoryOptionB, categoryOptionC);
    long idA = categoryService.addCategory(categoryA);
    long idB = categoryService.addCategory(categoryB);
    long idC = categoryService.addCategory(categoryC);
    assertEquals(categoryA, categoryService.getCategory(idA));
    assertEquals(categoryB, categoryService.getCategory(idB));
    assertEquals(categoryC, categoryService.getCategory(idC));
    assertEquals(categoryOptions, categoryService.getCategory(idA).getCategoryOptions());
    assertEquals(categoryOptions, categoryService.getCategory(idB).getCategoryOptions());
    assertEquals(categoryOptions, categoryService.getCategory(idC).getCategoryOptions());
  }

  @Test
  void testDelete() {
    CategoryOption[] allOptions = categoryOptions.toArray(new CategoryOption[0]);
    categoryA = createCategory('A', allOptions);
    categoryB = createCategory('B', allOptions);
    categoryC = createCategory('C', allOptions);
    long idA = categoryService.addCategory(categoryA);
    long idB = categoryService.addCategory(categoryB);
    long idC = categoryService.addCategory(categoryC);
    assertNotNull(categoryService.getCategory(idA));
    assertNotNull(categoryService.getCategory(idB));
    assertNotNull(categoryService.getCategory(idC));
    categoryService.deleteCategory(categoryA);
    assertNull(categoryService.getCategory(idA));
    assertNotNull(categoryService.getCategory(idB));
    assertNotNull(categoryService.getCategory(idC));
    categoryService.deleteCategory(categoryB);
    assertNull(categoryService.getCategory(idA));
    assertNull(categoryService.getCategory(idB));
    assertNotNull(categoryService.getCategory(idC));
  }

  @Test
  void testDeleteCategoryOption() {
    CategoryOption[] allOptions = categoryOptions.toArray(new CategoryOption[0]);
    categoryA = createCategory('A', allOptions);
    categoryB = createCategory('B', allOptions);
    long idA = categoryService.addCategory(categoryA);
    long idB = categoryService.addCategory(categoryB);
    long optionIdA = categoryOptionA.getId();
    long optionIdB = categoryOptionB.getId();
    categoryOptionA.setCategories(Sets.newHashSet(categoryA, categoryB));
    categoryOptionB.setCategories(Sets.newHashSet(categoryA, categoryB));
    categoryService.updateCategoryOption(categoryOptionA);
    categoryService.updateCategoryOption(categoryOptionB);
    assertNotNull(categoryService.getCategory(idA));
    assertNotNull(categoryService.getCategory(idB));
    categoryService.deleteCategory(categoryA);
    categoryService.deleteCategoryOption(categoryOptionB);
    assertNull(categoryService.getCategory(idA));
    assertNotNull(categoryService.getCategory(idB));
    assertNotNull(categoryService.getCategoryOption(optionIdA));
    assertNull(categoryService.getCategoryOption(optionIdB));
    assertTrue(categoryService.getCategory(idB).getCategoryOptions().contains(categoryOptionA));
    assertFalse(categoryService.getCategory(idB).getCategoryOptions().contains(categoryOptionB));
    assertFalse(categoryService.getCategoryOption(optionIdA).getCategories().contains(categoryA));
    assertTrue(categoryService.getCategoryOption(optionIdA).getCategories().contains(categoryB));
  }

  @Test
  void testAddGetCategoryGroup() {
    CategoryOptionGroup groupA = createCategoryOptionGroup('A');
    CategoryOptionGroup groupB = createCategoryOptionGroup('B');
    CategoryOptionGroup groupC = createCategoryOptionGroup('C');
    groupA.getMembers().add(categoryOptionA);
    groupA.getMembers().add(categoryOptionB);
    groupB.getMembers().add(categoryOptionC);
    long idA = categoryService.saveCategoryOptionGroup(groupA);
    long idB = categoryService.saveCategoryOptionGroup(groupB);
    long idC = categoryService.saveCategoryOptionGroup(groupC);
    assertEquals(groupA, categoryService.getCategoryOptionGroup(idA));
    assertEquals(groupB, categoryService.getCategoryOptionGroup(idB));
    assertEquals(groupC, categoryService.getCategoryOptionGroup(idC));
    assertEquals(2, categoryService.getCategoryOptionGroup(idA).getMembers().size());
    assertEquals(1, categoryService.getCategoryOptionGroup(idB).getMembers().size());
    assertEquals(0, categoryService.getCategoryOptionGroup(idC).getMembers().size());
  }

  @Test
  void testAddGetCategoryGroupSet() {
    CategoryOptionGroup groupA = createCategoryOptionGroup('A');
    CategoryOptionGroup groupB = createCategoryOptionGroup('B');
    CategoryOptionGroup groupC = createCategoryOptionGroup('C');
    groupA.getMembers().add(categoryOptionA);
    groupA.getMembers().add(categoryOptionB);
    groupB.getMembers().add(categoryOptionC);
    categoryService.saveCategoryOptionGroup(groupA);
    categoryService.saveCategoryOptionGroup(groupB);
    categoryService.saveCategoryOptionGroup(groupC);
    CategoryOptionGroupSet groupSetA = createCategoryOptionGroupSet('A');
    CategoryOptionGroupSet groupSetB = createCategoryOptionGroupSet('B');
    CategoryOptionGroupSet groupSetC = createCategoryOptionGroupSet('C');
    groupSetA.getMembers().add(groupA);
    groupSetA.getMembers().add(groupB);
    groupSetB.getMembers().add(groupC);
    long idA = categoryService.saveCategoryOptionGroupSet(groupSetA);
    long idB = categoryService.saveCategoryOptionGroupSet(groupSetB);
    long idC = categoryService.saveCategoryOptionGroupSet(groupSetC);
    assertEquals(groupSetA, categoryService.getCategoryOptionGroupSet(idA));
    assertEquals(groupSetB, categoryService.getCategoryOptionGroupSet(idB));
    assertEquals(groupSetC, categoryService.getCategoryOptionGroupSet(idC));
    assertEquals(2, categoryService.getCategoryOptionGroupSet(idA).getMembers().size());
    assertEquals(1, categoryService.getCategoryOptionGroupSet(idB).getMembers().size());
    assertEquals(0, categoryService.getCategoryOptionGroupSet(idC).getMembers().size());
  }

  @Test
  void testGetOperands() {
    setupCategoryCombo();
    categoryOptionComboGenerateService.addAndPruneOptionCombos(ccA);
    List<CategoryOptionCombo> optionCombos = Lists.newArrayList(ccA.getOptionCombos());
    deA = createDataElement('A', ccA);
    deB = createDataElement('B', ccA);
    idObjectManager.save(deA);
    idObjectManager.save(deB);
    List<DataElementOperand> operands = categoryService.getOperands(Lists.newArrayList(deA, deB));
    assertEquals(4, operands.size());
    assertTrue(operands.contains(new DataElementOperand(deA, optionCombos.get(0))));
    assertTrue(operands.contains(new DataElementOperand(deA, optionCombos.get(1))));
    assertTrue(operands.contains(new DataElementOperand(deB, optionCombos.get(0))));
    assertTrue(operands.contains(new DataElementOperand(deB, optionCombos.get(1))));
  }

  @Test
  void testGetOperandsWithTotals() {
    setupCategoryCombo();
    categoryOptionComboGenerateService.addAndPruneOptionCombos(ccA);
    List<CategoryOptionCombo> optionCombos = Lists.newArrayList(ccA.getOptionCombos());
    deA = createDataElement('A', ccA);
    deB = createDataElement('B', ccA);
    idObjectManager.save(deA);
    idObjectManager.save(deB);
    List<DataElementOperand> operands =
        categoryService.getOperands(Lists.newArrayList(deA, deB), true);
    assertEquals(6, operands.size());
    assertTrue(operands.contains(new DataElementOperand(deA)));
    assertTrue(operands.contains(new DataElementOperand(deA, optionCombos.get(0))));
    assertTrue(operands.contains(new DataElementOperand(deA, optionCombos.get(1))));
    assertTrue(operands.contains(new DataElementOperand(deB)));
    assertTrue(operands.contains(new DataElementOperand(deB, optionCombos.get(0))));
    assertTrue(operands.contains(new DataElementOperand(deB, optionCombos.get(1))));
  }

  @Test
  void testGetDisaggregationCategoryCombos() {
    setupCategoryCombo();
    assertEquals(1, categoryService.getDisaggregationCategoryCombos().size());
  }

  @Test
  void testGetDisaggregationCategoryOptionGroupSetsNoAcl() {
    CategoryOptionGroup groupA = createCategoryOptionGroup('A');
    groupA.setDataDimensionType(DataDimensionType.DISAGGREGATION);
    CategoryOptionGroup groupB = createCategoryOptionGroup('B');
    groupB.setDataDimensionType(DataDimensionType.DISAGGREGATION);
    CategoryOptionGroup groupC = createCategoryOptionGroup('C');
    groupC.setDataDimensionType(DataDimensionType.DISAGGREGATION);
    groupA.getMembers().add(categoryOptionA);
    groupA.getMembers().add(categoryOptionB);
    groupB.getMembers().add(categoryOptionC);
    categoryService.saveCategoryOptionGroup(groupA);
    categoryService.saveCategoryOptionGroup(groupB);
    categoryService.saveCategoryOptionGroup(groupC);
    CategoryOptionGroupSet groupSetA = createCategoryOptionGroupSet('A');
    groupSetA.setDataDimensionType(DataDimensionType.DISAGGREGATION);
    groupSetA.getMembers().add(groupA);
    groupSetA.getMembers().add(groupB);
    groupSetA.getMembers().add(groupC);
    categoryService.saveCategoryOptionGroupSet(groupSetA);
    assertEquals(1, categoryService.getDisaggregationCategoryOptionGroupSetsNoAcl().size());
  }

  @Test
  void testAddAndPruneAllCategoryCombos() {
    setupCategoryCombo();
    categoryOptionComboGenerateService.addAndPruneAllOptionCombos();

    assertEquals(3, categoryService.getAllCategoryOptionCombos().size());

    CategoryOption categoryOption = categoryService.getCategoryOption(categoryOptionB.getUid());
    categoryOption.setName("UpdateOption");
    categoryService.updateCategoryOption(categoryOption);
    entityManager.flush();
    entityManager.clear();

    categoryOptionComboGenerateService.addAndPruneAllOptionCombos();

    List<CategoryOptionCombo> cocs = categoryService.getAllCategoryOptionCombos();
    assertEquals(3, cocs.size());
    assertTrue(cocs.stream().anyMatch(coc -> coc.getName().contains("UpdateOption")));
  }

  @Test
  void addAndPruneCategoryCombo() {
    setupCategoryCombo();

    categoryOptionComboGenerateService.addAndPruneOptionCombos(ccA);
    assertEquals(3, categoryService.getAllCategoryOptionCombos().size());

    CategoryOption categoryOption = categoryService.getCategoryOption(categoryOptionB.getUid());
    categoryOption.setName("UpdateOption");
    categoryService.updateCategoryOption(categoryOption);
    entityManager.flush();
    entityManager.clear();

    categoryOptionComboGenerateService.addAndPruneOptionCombos(ccA);

    List<CategoryOptionCombo> cocs = categoryService.getAllCategoryOptionCombos();
    assertEquals(3, cocs.size());
    assertTrue(cocs.stream().anyMatch(coc -> coc.getName().contains("UpdateOption")));
  }

  @Test
  void addAndPruneCategoryComboWithSummary() {
    setupCategoryCombo();

    ImportSummaries importSummary =
        categoryOptionComboGenerateService.addAndPruneOptionCombosWithSummary(ccA);
    assertEquals(2, importSummary.getImported());
    assertEquals(SUCCESS, importSummary.getStatus());
    assertTrue(
        importSummary.getImportSummaries().stream()
            .map(ImportSummary::getDescription)
            .allMatch(desc -> desc.contains(ccA.getName())));

    assertEquals(3, categoryService.getAllCategoryOptionCombos().size());

    CategoryOption categoryOption = categoryService.getCategoryOption(categoryOptionB.getUid());
    categoryOption.setName("UpdateOption");
    categoryService.updateCategoryOption(categoryOption);
    entityManager.flush();
    entityManager.clear();

    ImportSummaries updateSummary =
        categoryOptionComboGenerateService.addAndPruneOptionCombosWithSummary(ccA);

    assertEquals(1, updateSummary.getUpdated());
    assertEquals(SUCCESS, updateSummary.getStatus());
    assertTrue(
        updateSummary.getImportSummaries().stream()
            .map(ImportSummary::getDescription)
            .allMatch(desc -> desc.contains("Update category option combo")));

    List<CategoryOptionCombo> cocs = categoryService.getAllCategoryOptionCombos();
    assertEquals(3, cocs.size());
    assertTrue(cocs.stream().anyMatch(coc -> coc.getName().contains("UpdateOption")));
  }

  @Test
  void addAndPruneCategoryComboWithSummaryDelete() {
    setupCategoryCombo();

    ImportSummaries importSummary =
        categoryOptionComboGenerateService.addAndPruneOptionCombosWithSummary(ccA);
    assertEquals(2, importSummary.getImported());
    assertEquals(SUCCESS, importSummary.getStatus());
    assertTrue(
        importSummary.getImportSummaries().stream()
            .map(ImportSummary::getDescription)
            .allMatch(desc -> desc.contains(ccA.getName())));

    assertEquals(3, categoryService.getAllCategoryOptionCombos().size());

    // remove option from category
    categoryA.removeCategoryOption(categoryOptionA);
    entityManager.flush();
    entityManager.clear();

    // trigger update
    ImportSummaries updateSummary =
        categoryOptionComboGenerateService.addAndPruneOptionCombosWithSummary(ccA);
    assertEquals(1, updateSummary.getDeleted());
    assertEquals(SUCCESS, updateSummary.getStatus());
    assertTrue(
        updateSummary.getImportSummaries().stream()
            .map(ImportSummary::getDescription)
            .allMatch(desc -> desc.contains("Deleted obsolete category option combo")));

    List<CategoryOptionCombo> cocs = categoryService.getAllCategoryOptionCombos();
    assertEquals(2, cocs.size());
  }

  @Test
  void noDuplicateCocTest() {
    // setup data
    TestCategoryMetadata catData = setupCategoryMetadata("a");

    assertEquals(4, catData.cc1().getOptionCombos().size());
    assertEquals(5, categoryService.getAllCategoryOptionCombos().size());

    // create new cat with exiting co
    Category catNew = createCategory('s');
    catNew.addCategoryOption(catData.co1());
    entityManager.persist(catNew);
    entityManager.flush();

    // create new cc with new cat + existing cats
    CategoryCombo ccNew = createCategoryCombo('y');
    ccNew.addCategory(catNew);
    ccNew.addCategory(catData.c1());
    ccNew.addCategory(catData.c2());
    entityManager.persist(ccNew);
    categoryOptionComboGenerateService.addAndPruneAllOptionCombos();
    entityManager.flush();

    // check expected count
    assertEquals(4, catData.cc1().getOptionCombos().size());
    assertEquals(9, categoryService.getAllCategoryOptionCombos().size());

    // add more existing co to new c
    catNew.addCategoryOption(catData.co2());
    catNew.addCategoryOption(catData.co3());
    catNew.addCategoryOption(catData.co4());
    entityManager.merge(catNew);
    categoryOptionComboGenerateService.addAndPruneAllOptionCombos();
    entityManager.flush();

    // update cocs
    categoryOptionComboGenerateService.addAndPruneAllOptionCombos();
    entityManager.flush();

    // check expected count
    assertEquals(4, catData.cc1().getOptionCombos().size());
    assertEquals(13, categoryService.getAllCategoryOptionCombos().size());
  }

  @Test
  void cocGenerationDuplicateTest() {
    // setup data - cc with 3 cats, some of which have same COs
    CategoryOption co1 = createCategoryOption('1');
    CategoryOption co2 = createCategoryOption('2');
    CategoryOption co3 = createCategoryOption('3');
    CategoryOption co4 = createCategoryOption('4');
    entityManager.persist(co1);
    entityManager.persist(co2);
    entityManager.persist(co3);
    entityManager.persist(co4);
    entityManager.flush();

    Category cat1 = createCategory('1', co1, co2);
    Category cat2 = createCategory('2', co3, co4);
    Category cat3 = createCategory('3', co1, co4);
    entityManager.persist(cat1);
    entityManager.persist(cat2);
    entityManager.persist(cat3);
    entityManager.flush();

    CategoryCombo cc = createCategoryCombo('1', cat1, cat2, cat3);
    // combo consists of 3 categories with options: [co1,co2] [co3,co4] [co1,co4]
    // all combinations (7) should be:
    // [co1,co3] [co1,co4] [co1,co3,co4] [co2,co3,co1] [co2,co3,co4] [2,4,1] [2,4]
    entityManager.persist(cc);
    entityManager.flush();

    Set<CategoryOptionCombo> categoryOptionCombos = cc.generateOptionCombosSet();
    assertEquals(7, categoryOptionCombos.size());
  }

  private void setupCategoryCombo() {
    categoryA = createCategory('A', categoryOptionA, categoryOptionB);
    categoryB = createCategory('B', categoryOptionC);
    categoryService.addCategory(categoryA);
    categoryService.addCategory(categoryB);

    ccA = createCategoryCombo('A', categoryA, categoryB);
    categoryService.addCategoryCombo(ccA);
  }
}
