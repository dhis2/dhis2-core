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
package org.hisp.dhis.dxf2.datavalueset;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class DataValueSetServiceGetFromUrlTest extends SingleSetupIntegrationTestBase {
  @Autowired private CategoryService categoryService;

  @Autowired private DataValueSetService dataValueSetService;

  private CategoryOption categoryOptionA;

  private CategoryOption categoryOptionB;

  private CategoryOption categoryOptionC;

  private CategoryOption categoryOptionD;

  private CategoryOption categoryOptionE;

  private Category categoryA;

  private Category categoryB;

  private CategoryCombo categoryComboA;

  private CategoryOptionCombo optionComboA;

  private CategoryOptionCombo optionComboB;

  private CategoryOptionCombo optionComboC;

  private CategoryOptionCombo optionComboD;

  @Override
  public void setUpTest() {
    categoryOptionA = createCategoryOption('A');
    categoryOptionB = createCategoryOption('B');
    categoryOptionC = createCategoryOption('C');
    categoryOptionD = createCategoryOption('D');
    categoryOptionE = createCategoryOption('E');
    categoryService.addCategoryOption(categoryOptionA);
    categoryService.addCategoryOption(categoryOptionB);
    categoryService.addCategoryOption(categoryOptionC);
    categoryService.addCategoryOption(categoryOptionD);
    categoryService.addCategoryOption(categoryOptionE);

    categoryA = createCategory('A', categoryOptionA, categoryOptionB);
    categoryB = createCategory('B', categoryOptionC, categoryOptionD);
    categoryService.addCategory(categoryA);
    categoryService.addCategory(categoryB);

    categoryComboA = createCategoryCombo('A', categoryA, categoryB);
    categoryService.addCategoryCombo(categoryComboA);

    optionComboA = createCategoryOptionCombo(categoryComboA, categoryOptionA, categoryOptionC);
    optionComboB = createCategoryOptionCombo(categoryComboA, categoryOptionA, categoryOptionD);
    optionComboC = createCategoryOptionCombo(categoryComboA, categoryOptionB, categoryOptionC);
    optionComboD = createCategoryOptionCombo(categoryComboA, categoryOptionB, categoryOptionD);
    categoryService.addCategoryOptionCombo(optionComboA);
    categoryService.addCategoryOptionCombo(optionComboB);
    categoryService.addCategoryOptionCombo(optionComboC);
    categoryService.addCategoryOptionCombo(optionComboD);
  }

  @Test
  void testGetFromUrlWithAttributes() {
    DataValueSetQueryParams params = new DataValueSetQueryParams();
    params.setAttributeCombo(categoryComboA.getUid());
    params.setAttributeOptions(Set.of(categoryOptionA.getUid(), categoryOptionC.getUid()));

    DataExportParams exportParams = dataValueSetService.getFromUrl(params);

    assertContainsOnly(Set.of(optionComboA), exportParams.getAttributeOptionCombos());
  }

  @Test
  void testGetFromUrlWithAttributesException() {
    DataValueSetQueryParams params = new DataValueSetQueryParams();
    params.setAttributeCombo(categoryComboA.getUid());
    params.setAttributeOptions(Set.of(categoryOptionA.getUid(), categoryOptionE.getUid()));

    assertThrows(IllegalQueryException.class, () -> dataValueSetService.getFromUrl(params));
  }
}
