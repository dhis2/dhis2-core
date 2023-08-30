/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class CategoryOptionComboServiceTest extends TransactionalIntegrationTest {

  @Autowired private CategoryService categoryService;

  @Autowired private AttributeService attributeService;

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

  // -------------------------------------------------------------------------
  // Fixture
  // -------------------------------------------------------------------------
  @Override
  public void setUpTest() throws Exception {
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
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------
  @Test
  void testAddGetCategoryOptionCombo() {
    categoryOptionComboA = new CategoryOptionCombo();
    Set<CategoryOption> categoryOptions = Sets.newHashSet(categoryOptionA, categoryOptionB);
    categoryOptionComboA.setCategoryCombo(categoryComboA);
    categoryOptionComboA.setCategoryOptions(categoryOptions);
    long id = categoryService.addCategoryOptionCombo(categoryOptionComboA);
    categoryOptionComboA = categoryService.getCategoryOptionCombo(id);
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
    long id = categoryService.addCategoryOptionCombo(categoryOptionComboA);
    categoryOptionComboA = categoryService.getCategoryOptionCombo(id);
    assertNotNull(categoryOptionComboA);
    assertEquals(categoryComboA, categoryOptionComboA.getCategoryCombo());
    assertEquals(categoryOptions, categoryOptionComboA.getCategoryOptions());
    categoryOptionComboA.setCategoryCombo(categoryComboB);
    categoryService.updateCategoryOptionCombo(categoryOptionComboA);
    categoryOptionComboA = categoryService.getCategoryOptionCombo(id);
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
    long idA = categoryService.addCategoryOptionCombo(categoryOptionComboA);
    long idB = categoryService.addCategoryOptionCombo(categoryOptionComboB);
    long idC = categoryService.addCategoryOptionCombo(categoryOptionComboC);
    assertNotNull(categoryService.getCategoryOptionCombo(idA));
    assertNotNull(categoryService.getCategoryOptionCombo(idB));
    assertNotNull(categoryService.getCategoryOptionCombo(idC));
    categoryService.deleteCategoryOptionCombo(categoryService.getCategoryOptionCombo(idA));
    assertNull(categoryService.getCategoryOptionCombo(idA));
    assertNotNull(categoryService.getCategoryOptionCombo(idB));
    assertNotNull(categoryService.getCategoryOptionCombo(idC));
    categoryService.deleteCategoryOptionCombo(categoryService.getCategoryOptionCombo(idB));
    assertNull(categoryService.getCategoryOptionCombo(idA));
    assertNull(categoryService.getCategoryOptionCombo(idB));
    assertNotNull(categoryService.getCategoryOptionCombo(idC));
    categoryService.deleteCategoryOptionCombo(categoryService.getCategoryOptionCombo(idC));
    assertNull(categoryService.getCategoryOptionCombo(idA));
    assertNull(categoryService.getCategoryOptionCombo(idB));
    assertNull(categoryService.getCategoryOptionCombo(idC));
  }

  @Test
  void testDeleteCategoryOptionComboLinkedToCategory() {
    categoryOptionComboA = new CategoryOptionCombo();
    categoryOptionComboB = new CategoryOptionCombo();
    categoryOptionComboC = new CategoryOptionCombo();
    categoryOptionComboA.setCategoryCombo(categoryComboA);
    categoryOptionComboB.setCategoryCombo(categoryComboA);
    categoryOptionComboC.setCategoryCombo(categoryComboA);
    categoryOptionComboA.setCategoryOptions(Sets.newHashSet(categoryOptionA, categoryOptionC));
    categoryOptionComboB.setCategoryOptions(Sets.newHashSet(categoryOptionA, categoryOptionB));
    categoryOptionComboC.setCategoryOptions(Sets.newHashSet(categoryOptionB, categoryOptionC));
    long idA = categoryService.addCategoryOptionCombo(categoryOptionComboA);
    long idB = categoryService.addCategoryOptionCombo(categoryOptionComboB);
    long idC = categoryService.addCategoryOptionCombo(categoryOptionComboC);
    categoryComboA.setCategories(Lists.newArrayList(categoryA));
    categoryComboA.setOptionCombos(
        Sets.newHashSet(categoryOptionComboA, categoryOptionComboB, categoryOptionComboC));
    categoryService.updateCategoryCombo(categoryComboA);
    categoryA.setCategoryCombos(Lists.newArrayList(categoryComboA));
    categoryService.updateCategory(categoryA);
    assertNotNull(categoryService.getCategoryOptionCombo(idA));
    assertNotNull(categoryService.getCategoryOptionCombo(idB));
    assertNotNull(categoryService.getCategoryOptionCombo(idC));
    categoryService.deleteCategoryOptionCombo(categoryService.getCategoryOptionCombo(idA));
    assertNull(categoryService.getCategoryOptionCombo(idA));
    Set<CategoryOptionCombo> optionCombos =
        categoryService.getCategory(categoryA.getId()).getCategoryCombos().stream()
            .flatMap(cc -> cc.getOptionCombos().stream())
            .collect(Collectors.toSet());
    assertFalse(optionCombos.contains(categoryOptionComboA));
    assertNotNull(categoryService.getCategoryOptionCombo(idB));
    assertTrue(optionCombos.contains(categoryOptionComboB));
    assertNotNull(categoryService.getCategoryOptionCombo(idC));
    assertTrue(optionCombos.contains(categoryOptionComboC));
  }

  @Test
  void testDeleteCategory() {
    categoryOptionComboA = new CategoryOptionCombo();
    categoryOptionComboB = new CategoryOptionCombo();
    categoryOptionComboC = new CategoryOptionCombo();
    categoryOptionComboA.setCategoryCombo(categoryComboA);
    categoryOptionComboB.setCategoryCombo(categoryComboA);
    categoryOptionComboC.setCategoryCombo(categoryComboA);
    categoryOptionComboA.setCategoryOptions(Sets.newHashSet(categoryOptionA, categoryOptionC));
    categoryOptionComboB.setCategoryOptions(Sets.newHashSet(categoryOptionA, categoryOptionB));
    categoryOptionComboC.setCategoryOptions(Sets.newHashSet(categoryOptionB, categoryOptionC));
    long idA = categoryService.addCategoryOptionCombo(categoryOptionComboA);
    long idB = categoryService.addCategoryOptionCombo(categoryOptionComboB);
    long idC = categoryService.addCategoryOptionCombo(categoryOptionComboC);
    categoryComboA.setCategories(Lists.newArrayList(categoryA));
    categoryComboA.setOptionCombos(
        Sets.newHashSet(categoryOptionComboA, categoryOptionComboB, categoryOptionComboC));
    categoryService.updateCategoryCombo(categoryComboA);
    categoryA.setCategoryCombos(Lists.newArrayList(categoryComboA));
    categoryService.updateCategory(categoryA);
    assertNotNull(categoryService.getCategoryOptionCombo(idA));
    assertNotNull(categoryService.getCategoryOptionCombo(idB));
    assertNotNull(categoryService.getCategoryOptionCombo(idC));
    assertThrows(DeleteNotAllowedException.class, () -> categoryService.deleteCategory(categoryA));
  }

  @Test
  void testDeleteCategoryCombo() {
    categoryOptionComboA = new CategoryOptionCombo();
    categoryOptionComboB = new CategoryOptionCombo();
    categoryOptionComboC = new CategoryOptionCombo();
    categoryOptionComboA.setCategoryCombo(categoryComboA);
    categoryOptionComboB.setCategoryCombo(categoryComboA);
    categoryOptionComboC.setCategoryCombo(categoryComboA);
    categoryOptionComboA.setCategoryOptions(Sets.newHashSet(categoryOptionA, categoryOptionC));
    categoryOptionComboB.setCategoryOptions(Sets.newHashSet(categoryOptionA, categoryOptionB));
    categoryOptionComboC.setCategoryOptions(Sets.newHashSet(categoryOptionB, categoryOptionC));
    long idA = categoryService.addCategoryOptionCombo(categoryOptionComboA);
    long idB = categoryService.addCategoryOptionCombo(categoryOptionComboB);
    long idC = categoryService.addCategoryOptionCombo(categoryOptionComboC);
    long comboId = categoryComboA.getId();
    categoryComboA.setCategories(Lists.newArrayList(categoryA));
    categoryComboA.setOptionCombos(
        Sets.newHashSet(categoryOptionComboA, categoryOptionComboB, categoryOptionComboC));
    categoryService.updateCategoryCombo(categoryComboA);
    categoryA.setCategoryCombos(Lists.newArrayList(categoryComboA));
    categoryService.updateCategory(categoryA);
    assertNotNull(categoryService.getCategoryOptionCombo(idA));
    assertNotNull(categoryService.getCategoryOptionCombo(idB));
    assertNotNull(categoryService.getCategoryOptionCombo(idC));
    categoryService.deleteCategoryCombo(categoryComboA);
    assertNull(categoryService.getCategoryCombo(comboId));
    assertNull(categoryService.getCategoryOptionCombo(idA));
    assertNull(categoryService.getCategoryOptionCombo(idB));
    assertNull(categoryService.getCategoryOptionCombo(idC));
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
    categoryService.addCategoryOptionCombo(categoryOptionComboA);
    categoryService.addCategoryOptionCombo(categoryOptionComboB);
    categoryService.addCategoryOptionCombo(categoryOptionComboC);
    List<CategoryOptionCombo> categoryOptionCombos = categoryService.getAllCategoryOptionCombos();
    assertNotNull(categoryOptionCombos);
    // Including default
    assertEquals(4, categoryOptionCombos.size());
    // category option combo
  }

  @Test
  void testGetCategoryOptionComboName() {
    categoryOptionComboA = new CategoryOptionCombo();
    Set<CategoryOption> categoryOptions = Sets.newHashSet(categoryOptionA, categoryOptionC);
    categoryOptionComboA.setCategoryCombo(categoryComboA);
    categoryOptionComboA.setCategoryOptions(categoryOptions);
    categoryService.addCategoryOptionCombo(categoryOptionComboA);
    String expected = "Male, 0-20";
    assertEquals(expected, categoryOptionComboA.getName());
  }

  @Test
  void testAddAttributeValue() {
    categoryOptionComboA = new CategoryOptionCombo();
    Set<CategoryOption> categoryOptions = Sets.newHashSet(categoryOptionA, categoryOptionB);
    categoryOptionComboA.setCategoryCombo(categoryComboA);
    categoryOptionComboA.setCategoryOptions(categoryOptions);
    long id = categoryService.addCategoryOptionCombo(categoryOptionComboA);
    categoryOptionComboA = categoryService.getCategoryOptionCombo(id);
    Attribute attribute1 = new Attribute("attribute 1", ValueType.TEXT);
    attribute1.setCategoryOptionComboAttribute(true);
    attributeService.addAttribute(attribute1);
    AttributeValue avA = new AttributeValue("value 1");
    avA.setAttribute(attribute1);
    categoryOptionComboA.getAttributeValues().add(avA);
    categoryService.updateCategoryOptionCombo(categoryOptionComboA);
    categoryOptionComboA = categoryService.getCategoryOptionCombo(id);
    assertFalse(categoryOptionComboA.getAttributeValues().isEmpty());
    categoryOptionComboA.getAttributeValues().clear();
    categoryService.updateCategoryOptionCombo(categoryOptionComboA);
    categoryOptionComboA = categoryService.getCategoryOptionCombo(id);
    assertTrue(categoryOptionComboA.getAttributeValues().isEmpty());
  }
}
