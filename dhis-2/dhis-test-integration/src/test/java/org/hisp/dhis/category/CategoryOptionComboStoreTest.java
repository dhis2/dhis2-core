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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class CategoryOptionComboStoreTest extends PostgresIntegrationTestBase {
  @Autowired private CategoryOptionComboStore categoryOptionComboStore;
  @Autowired private CategoryService categoryService;
  @Autowired private CategoryOptionComboGenerateService categoryOptionComboGenerateService;
  @Autowired private DataElementService dataElementService;

  private Category categoryA;

  private Category categoryB;

  private CategoryCombo categoryComboA;

  private CategoryCombo categoryComboB;

  private CategoryOption categoryOptionA;

  private CategoryOption categoryOptionB;

  private CategoryOption categoryOptionC;

  private CategoryOption categoryOptionD;

  private CategoryOptionCombo categoryOptionComboA;

  private CategoryOptionCombo categoryOptionComboB;

  private CategoryOptionCombo categoryOptionComboC;

  private DataElement dataElementA;

  @BeforeEach
  void setUp() {
    categoryOptionA = new CategoryOption("Male");
    categoryOptionB = new CategoryOption("Female");
    categoryOptionC = new CategoryOption("0-20");
    categoryOptionD = new CategoryOption("20-100");
    categoryService.addCategoryOption(categoryOptionA);
    categoryService.addCategoryOption(categoryOptionB);
    categoryService.addCategoryOption(categoryOptionC);
    categoryService.addCategoryOption(categoryOptionD);
    categoryA = new Category("Gender", DataDimensionType.DISAGGREGATION);
    categoryA.setShortName(categoryA.getName());
    categoryB = new Category("Agegroup", DataDimensionType.DISAGGREGATION);
    categoryB.setShortName(categoryB.getName());
    categoryA.addCategoryOption(categoryOptionA);
    categoryA.addCategoryOption(categoryOptionB);
    categoryB.addCategoryOption(categoryOptionC);
    categoryB.addCategoryOption(categoryOptionD);
    categoryService.addCategory(categoryA);
    categoryService.addCategory(categoryB);
    categoryComboA = new CategoryCombo("GenderAgegroup", DataDimensionType.DISAGGREGATION);
    categoryComboB = new CategoryCombo("Gender", DataDimensionType.DISAGGREGATION);
    categoryComboA.addCategory(categoryA);
    categoryComboA.addCategory(categoryB);
    categoryComboB.addCategory(categoryA);
    categoryService.addCategoryCombo(categoryComboA);
    categoryService.addCategoryCombo(categoryComboB);
    dataElementA = createDataElement('A');
    dataElementA.setCategoryCombo(categoryComboA);
    dataElementService.addDataElement(dataElementA);
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------
  @Test
  void testAddGetCategoryOptionCombo() {
    categoryOptionComboA = new CategoryOptionCombo();
    Set<CategoryOption> categoryOptions = Sets.newHashSet(categoryOptionA, categoryOptionC);
    categoryOptionComboA.setCategoryCombo(categoryComboA);
    categoryOptionComboA.setCategoryOptions(categoryOptions);
    categoryOptionComboStore.save(categoryOptionComboA);
    long id = categoryOptionComboA.getId();
    categoryOptionComboA = categoryOptionComboStore.get(id);
    assertNotNull(categoryOptionComboA);
    assertEquals(categoryComboA, categoryOptionComboA.getCategoryCombo());
    assertEquals(categoryOptions, categoryOptionComboA.getCategoryOptions());
  }

  @Test
  void testUpdateGetCategoryOptionCombo() {
    categoryOptionComboA = new CategoryOptionCombo();
    Set<CategoryOption> categoryOptions = Sets.newHashSet(categoryOptionA, categoryOptionC);
    categoryOptionComboA.setCategoryCombo(categoryComboA);
    categoryOptionComboA.setCategoryOptions(categoryOptions);
    categoryOptionComboStore.save(categoryOptionComboA);
    long id = categoryOptionComboA.getId();
    assertNotNull(categoryOptionComboStore.get(id));
    assertEquals(categoryComboA, categoryOptionComboA.getCategoryCombo());
    assertEquals(categoryOptions, categoryOptionComboA.getCategoryOptions());
    categoryOptionComboA.setCategoryCombo(categoryComboB);
    categoryOptionComboStore.update(categoryOptionComboA);
    categoryOptionComboA = categoryOptionComboStore.get(id);
    assertNotNull(categoryOptionComboA);
    assertEquals(categoryComboB, categoryOptionComboA.getCategoryCombo());
    assertEquals(categoryOptions, categoryOptionComboA.getCategoryOptions());
  }

  @Test
  void testDeleteCategoryOptionCombo() {
    categoryOptionComboA = new CategoryOptionCombo();
    categoryOptionComboB = new CategoryOptionCombo();
    categoryOptionComboC = new CategoryOptionCombo();
    Set<CategoryOption> categoryOptions = Sets.newHashSet(categoryOptionA, categoryOptionC);
    categoryOptionComboA.setCategoryCombo(categoryComboA);
    categoryOptionComboB.setCategoryCombo(categoryComboA);
    categoryOptionComboC.setCategoryCombo(categoryComboA);
    categoryOptionComboA.setCategoryOptions(categoryOptions);
    categoryOptionComboB.setCategoryOptions(categoryOptions);
    categoryOptionComboC.setCategoryOptions(categoryOptions);
    categoryOptionComboStore.save(categoryOptionComboA);
    long idA = categoryOptionComboA.getId();
    categoryOptionComboStore.save(categoryOptionComboB);
    long idB = categoryOptionComboB.getId();
    categoryOptionComboStore.save(categoryOptionComboC);
    long idC = categoryOptionComboC.getId();
    assertNotNull(categoryOptionComboStore.get(idA));
    assertNotNull(categoryOptionComboStore.get(idB));
    assertNotNull(categoryOptionComboStore.get(idC));
    categoryOptionComboStore.delete(categoryOptionComboStore.get(idA));
    assertNull(categoryOptionComboStore.get(idA));
    assertNotNull(categoryOptionComboStore.get(idB));
    assertNotNull(categoryOptionComboStore.get(idC));
    categoryOptionComboStore.delete(categoryOptionComboStore.get(idB));
    assertNull(categoryOptionComboStore.get(idA));
    assertNull(categoryOptionComboStore.get(idB));
    assertNotNull(categoryOptionComboStore.get(idC));
    categoryOptionComboStore.delete(categoryOptionComboStore.get(idC));
    assertNull(categoryOptionComboStore.get(idA));
    assertNull(categoryOptionComboStore.get(idB));
    assertNull(categoryOptionComboStore.get(idC));
  }

  @Test
  void testGetAllCategoryOptionCombos() {
    categoryOptionComboA = new CategoryOptionCombo();
    categoryOptionComboB = new CategoryOptionCombo();
    categoryOptionComboC = new CategoryOptionCombo();
    Set<CategoryOption> categoryOptions = Sets.newHashSet(categoryOptionA, categoryOptionC);
    categoryOptionComboA.setCategoryCombo(categoryComboA);
    categoryOptionComboB.setCategoryCombo(categoryComboA);
    categoryOptionComboC.setCategoryCombo(categoryComboA);
    categoryOptionComboA.setCategoryOptions(categoryOptions);
    categoryOptionComboB.setCategoryOptions(categoryOptions);
    categoryOptionComboC.setCategoryOptions(categoryOptions);
    categoryOptionComboStore.save(categoryOptionComboA);
    categoryOptionComboStore.save(categoryOptionComboB);
    categoryOptionComboStore.save(categoryOptionComboC);
    List<CategoryOptionCombo> categoryOptionCombos = categoryOptionComboStore.getAll();
    assertNotNull(categoryOptionCombos);
    // Including default
    assertEquals(4, categoryOptionCombos.size());
  }

  @Test
  void testGetCategoryOptionCombo() {
    categoryOptionComboGenerateService.addAndPruneOptionCombos(categoryComboA);
    categoryOptionComboGenerateService.addAndPruneOptionCombos(categoryComboB);
    Set<CategoryOption> categoryOptions1 = new HashSet<>();
    categoryOptions1.add(categoryOptionA);
    categoryOptions1.add(categoryOptionC);
    Set<CategoryOption> categoryOptions2 = new HashSet<>();
    categoryOptions2.add(categoryOptionA);
    categoryOptions2.add(categoryOptionD);
    Set<CategoryOption> categoryOptions3 = new HashSet<>();
    categoryOptions3.add(categoryOptionB);
    categoryOptions3.add(categoryOptionC);
    Set<CategoryOption> categoryOptions4 = new HashSet<>();
    categoryOptions4.add(categoryOptionB);
    categoryOptions4.add(categoryOptionC);
    CategoryOptionCombo coc1 =
        categoryOptionComboStore.getCategoryOptionCombo(categoryComboA, categoryOptions1);
    CategoryOptionCombo coc2 =
        categoryOptionComboStore.getCategoryOptionCombo(categoryComboA, categoryOptions2);
    CategoryOptionCombo coc3 =
        categoryOptionComboStore.getCategoryOptionCombo(categoryComboA, categoryOptions3);
    CategoryOptionCombo coc4 =
        categoryOptionComboStore.getCategoryOptionCombo(categoryComboA, categoryOptions4);
    assertNotNull(coc1);
    assertNotNull(coc2);
    assertNotNull(coc3);
    assertNotNull(coc4);
    assertEquals(categoryComboA, coc1.getCategoryCombo());
    assertEquals(categoryComboA, coc2.getCategoryCombo());
    assertEquals(categoryComboA, coc3.getCategoryCombo());
    assertEquals(categoryComboA, coc4.getCategoryCombo());
    assertEquals(categoryOptions1, coc1.getCategoryOptions());
    assertEquals(categoryOptions2, coc2.getCategoryOptions());
    assertEquals(categoryOptions3, coc3.getCategoryOptions());
    assertEquals(categoryOptions4, coc4.getCategoryOptions());
  }

  @Test
  void testGetCategoryOptionComboNotFound() {
    categoryOptionComboGenerateService.addAndPruneOptionCombos(categoryComboA);
    categoryOptionComboGenerateService.addAndPruneOptionCombos(categoryComboB);
    CategoryOption co = new CategoryOption("10000");
    categoryService.addCategoryOption(co);
    Set<CategoryOption> options = new HashSet<>();
    options.add(co);

    assertNull(categoryOptionComboStore.getCategoryOptionCombo(categoryComboA, options));
  }

  @Test
  void testGetCategoryOptionComboGivenSubsetOfCategoryOptions() {
    categoryOptionComboGenerateService.addAndPruneOptionCombos(categoryComboA);
    categoryOptionComboGenerateService.addAndPruneOptionCombos(categoryComboB);
    Set<CategoryOption> options = new HashSet<>();
    options.add(categoryOptionA);

    assertNull(categoryOptionComboStore.getCategoryOptionCombo(categoryComboA, options));
  }

  @Test
  void testGetCategoryOptionComboByOptionGroup() {
    categoryOptionComboGenerateService.addAndPruneOptionCombos(categoryComboA);
    categoryOptionComboGenerateService.addAndPruneOptionCombos(categoryComboB);
    CategoryOptionGroup catOptionGroup = createCategoryOptionGroup('A');
    catOptionGroup.addCategoryOption(categoryOptionA);
    catOptionGroup.addCategoryOption(categoryOptionB);
    categoryService.saveCategoryOptionGroup(catOptionGroup);
    List<CategoryOptionCombo> result =
        categoryOptionComboStore.getCategoryOptionCombosByGroupUid(
            catOptionGroup.getUid(), dataElementA.getUid());
    assertNotNull(result);
    assertEquals(categoryComboA.getOptionCombos(), Sets.newHashSet(result));
  }

  @Test
  @DisplayName("Retrieving CategoryOptionCombos by CategoryOptions returns the expected objects")
  void getCatOptCombosByCatOptionsTest() {
    CategoryOption co1 = createCategoryOption('1');
    CategoryOption co2 = createCategoryOption('2');
    CategoryOption co3 = createCategoryOption('3');
    CategoryOption co4 = createCategoryOption('4');
    categoryService.addCategoryOption(co1);
    categoryService.addCategoryOption(co2);
    categoryService.addCategoryOption(co3);
    categoryService.addCategoryOption(co4);

    Category c1 = createCategory('1', co1, co2);
    Category c2 = createCategory('2', co3, co4);
    categoryService.addCategory(c1);
    categoryService.addCategory(c2);

    CategoryCombo categoryCombo = createCategoryCombo('Z', c1, c2);
    categoryService.addCategoryCombo(categoryCombo);
    categoryOptionComboGenerateService.addAndPruneOptionCombos(categoryCombo);

    List<CategoryOptionCombo> cocsByCategoryOption =
        categoryOptionComboStore.getCategoryOptionCombosByCategoryOption(
            List.of(co1.getUid(), co2.getUid(), co3.getUid()));

    assertEquals(4, cocsByCategoryOption.size(), "4 CategoryOptionCombos should be present");
    List<String> cos =
        cocsByCategoryOption.stream()
            .flatMap(coc -> coc.getCategoryOptions().stream())
            .map(IdentifiableObject::getUid)
            .toList();

    assertEquals(8, cos.size(), "8 CategoryOptions should be present");
    assertTrue(
        cos.containsAll(List.of(co1.getUid(), co2.getUid(), co3.getUid())),
        "Retrieved CategoryOption UIDs should have expected UIDs");
  }
}
