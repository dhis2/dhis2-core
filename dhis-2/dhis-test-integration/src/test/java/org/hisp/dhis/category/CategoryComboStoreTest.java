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
package org.hisp.dhis.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CategoryComboStoreTest extends PostgresIntegrationTestBase {
  @Autowired private CategoryComboStore categoryComboStore;

  @Test
  @DisplayName("Retrieving CategoryCombos by CategoryOptionCombos returns the expected entries")
  void getCatOptionComboTest() {
    // given
    CategoryOption co1 = createCategoryOption("1A", CodeGenerator.generateUid());
    CategoryOption co2 = createCategoryOption("1B", CodeGenerator.generateUid());
    CategoryOption co3 = createCategoryOption("2A", CodeGenerator.generateUid());
    CategoryOption co4 = createCategoryOption("2B", CodeGenerator.generateUid());
    CategoryOption co5 = createCategoryOption("3A", CodeGenerator.generateUid());
    CategoryOption co6 = createCategoryOption("4A", CodeGenerator.generateUid());
    categoryService.addCategoryOption(co1);
    categoryService.addCategoryOption(co2);
    categoryService.addCategoryOption(co3);
    categoryService.addCategoryOption(co4);
    categoryService.addCategoryOption(co5);
    categoryService.addCategoryOption(co6);

    Category c1 = createCategory('1', co1, co2);
    Category c2 = createCategory('2', co3, co4);
    Category c3 = createCategory('3', co5);
    Category c4 = createCategory('4', co6);
    categoryService.addCategory(c1);
    categoryService.addCategory(c2);
    categoryService.addCategory(c3);
    categoryService.addCategory(c4);

    CategoryCombo cc1 = createCategoryCombo('1', c1, c2);
    CategoryCombo cc2 = createCategoryCombo('2', c3, c4);
    categoryService.addCategoryCombo(cc1);
    categoryService.addCategoryCombo(cc2);

    categoryService.generateOptionCombos(cc1);
    categoryService.generateOptionCombos(cc2);

    CategoryOptionCombo coc1 = getCocWithOptions("1A", "2B");
    CategoryOptionCombo coc2 = getCocWithOptions("2A", "1B");

    // when
    List<CategoryCombo> catCombosByCategoryOptionCombo =
        categoryComboStore.getByCategoryOptionCombo(UID.of(coc1.getUid(), coc2.getUid()));

    // then
    assertEquals(1, catCombosByCategoryOptionCombo.size(), "1 CategoryCombo should be present");
    List<String> categoryCombos =
        catCombosByCategoryOptionCombo.stream().map(BaseIdentifiableObject::getUid).toList();

    assertTrue(
        categoryCombos.contains(cc1.getUid()),
        "Retrieved CategoryCombo UID should equal the expected value");
  }

  private CategoryOptionCombo getCocWithOptions(String co1, String co2) {
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    return allCategoryOptionCombos.stream()
        .filter(
            coc -> {
              List<String> categoryOptions =
                  coc.getCategoryOptions().stream().map(BaseIdentifiableObject::getName).toList();
              return categoryOptions.containsAll(List.of(co1, co2));
            })
        .toList()
        .get(0);
  }
}
