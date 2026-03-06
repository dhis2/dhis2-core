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
package org.hisp.dhis.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.test.api.TestCategoryMetadata;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CategoryStoreTest extends PostgresIntegrationTestBase {
  @Autowired private CategoryStore categoryStore;
  @Autowired private DbmsManager dbmsManager;

  @Test
  @DisplayName("Retrieving Categories by CategoryOptions returns the expected objects")
  void getCatByCatOptionsTest() {
    CategoryOption co1 = createCategoryOption('1');
    CategoryOption co2 = createCategoryOption('2');
    CategoryOption co3 = createCategoryOption('3');
    CategoryOption co4 = createCategoryOption('4');
    categoryService.addCategoryOption(co1);
    categoryService.addCategoryOption(co2);
    categoryService.addCategoryOption(co3);
    categoryService.addCategoryOption(co4);

    Category c1 = createCategory('1', co1);
    c1.addCategoryOption(co2);
    Category c2 = createCategory('2', co3);
    Category c3 = createCategory('3');
    Category c4 = createCategory('4');
    categoryService.addCategory(c1);
    categoryService.addCategory(c2);
    categoryService.addCategory(c3);
    categoryService.addCategory(c4);

    List<Category> categoriesByCategoryOption =
        categoryStore.getCategoriesByCategoryOption(
            List.of(co1.getUid(), co2.getUid(), co3.getUid()));

    assertEquals(2, categoriesByCategoryOption.size(), "2 Categories should be present");
    List<String> categoryOptions =
        categoriesByCategoryOption.stream()
            .flatMap(c -> c.getCategoryOptions().stream())
            .map(IdentifiableObject::getUid)
            .toList();

    assertEquals(3, categoryOptions.size(), "3 CategoryOptions should be present");
    assertTrue(
        categoryOptions.containsAll(List.of(co1.getUid(), co2.getUid(), co3.getUid())),
        "Retrieved CategoryOption UIDs should have expected UIDs");
  }

  @Test
  @DisplayName("Removing CategoryOptions from Categories returns the expected count")
  void removeCatOptionsFromCategoriesTest() {
    // given 4 Categories exist, each with 2 CategoryOptions (12)
    TestCategoryMetadata categoryMetadata1 = setupCategoryMetadata("1");
    TestCategoryMetadata categoryMetadata2 = setupCategoryMetadata("2");

    Category c1 = categoryMetadata1.c1();
    Category c2 = categoryMetadata1.c2();
    Category c3 = categoryMetadata2.c1();
    Category c4 = categoryMetadata2.c2();

    // check state before change
    assertEquals(2, c1.getCategoryOptions().size());
    assertEquals(2, c2.getCategoryOptions().size());
    assertEquals(2, c3.getCategoryOptions().size());
    assertEquals(2, c4.getCategoryOptions().size());

    // when removing CategoryOptions for 2 Categories
    int removedCatOptions =
        categoryStore.removeCatOptionCategoryRefs(Set.of(c1.getId(), c2.getId()));
    dbmsManager.clearSession();

    // then the expected number of CategoryDimensions should be updated
    assertEquals(4, removedCatOptions, "4 CategoryOptions should have been removed");

    // and Categories have the expected number of CategoryOptions
    assertEquals(0, getCategoryCatOptions(c1));
    assertEquals(0, getCategoryCatOptions(c2));
    assertEquals(2, getCategoryCatOptions(c3));
    assertEquals(2, getCategoryCatOptions(c4));
  }

  private int getCategoryCatOptions(Category c) {
    return Objects.requireNonNull(categoryStore.getByUid(c.getUid())).getCategoryOptions().size();
  }
}
