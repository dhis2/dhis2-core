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
package org.hisp.dhis.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.test.api.TestCategoryMetadata;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CategoryComboStoreTest extends PostgresIntegrationTestBase {
  @Autowired private CategoryComboStore categoryComboStore;
  @Autowired private DbmsManager dbmsManager;

  @Test
  @DisplayName("Retrieving CategoryCombos by CategoryOptions returns the expected objects")
  void getCatByCatOptionsTest() {
    // Given category 3 cat combos exists
    TestCategoryMetadata categoryMetadata1 = setupCategoryMetadata("1");
    TestCategoryMetadata categoryMetadata2 = setupCategoryMetadata("2");
    setupCategoryMetadata("3");

    // When getting category combos by categories
    List<CategoryCombo> categoryCombos =
        categoryComboStore.getCategoryCombosByCategory(
            UID.of(categoryMetadata1.c2(), categoryMetadata2.c1()));

    // Then the expected 2 CategoryCombos are retrieved
    assertEquals(2, categoryCombos.size(), "2 CategoryCombos should be present");
    List<String> categoryComboUids =
        categoryCombos.stream().map(BaseMetadataObject::getUid).toList();
    assertTrue(
        categoryComboUids.containsAll(
            List.of(categoryMetadata1.cc1().getUid(), categoryMetadata2.cc1().getUid())),
        "Retrieved CategoryCombo UIDs should have expected UIDs");
  }

  @Test
  @DisplayName("Updating CategoryCombo's Categories returns the expected count and state")
  void updateCatComboCategoriesTest() {
    // given 2 CategoryCombos exist, each with 2 Categories
    TestCategoryMetadata categoryMetadata1 = setupCategoryMetadata("1");
    TestCategoryMetadata categoryMetadata2 = setupCategoryMetadata("2");

    Category c1 = categoryMetadata1.c1();
    Category c2 = categoryMetadata1.c2();
    Category c3 = categoryMetadata2.c1();
    Category c4 = categoryMetadata2.c2();
    CategoryCombo cc1 = categoryMetadata1.cc1();
    CategoryCombo cc2 = categoryMetadata2.cc1();

    // check state before change
    assertEquals(2, cc1.getCategories().size());
    assertEquals(2, cc2.getCategories().size());

    checkHasExpectedCategories(cc1, c1, c2);
    checkHasExpectedCategories(cc2, c3, c4);

    // when updating Categories for 1 CategoryCombo
    int removedCatOptions =
        categoryComboStore.updateCatComboCategoryRefs(Set.of(c1.getId(), c2.getId()), c3.getId());
    dbmsManager.clearSession();

    // then the expected number of Categories should be updated
    assertEquals(2, removedCatOptions, "2 CategoryCombo's Categories should have been updated");

    // and updated CategoryCombos have expected state
    CategoryCombo cc1Updated = categoryComboStore.getByUid(cc1.getUid());
    CategoryCombo cc2Updated = categoryComboStore.getByUid(cc2.getUid());
    assertNotNull(cc1Updated);
    assertNotNull(cc2Updated);

    assertEquals(2, cc1Updated.getCategories().size());
    assertEquals(2, cc2Updated.getCategories().size());

    checkHasExpectedCategories(cc1Updated, c3, c3);
    checkHasExpectedCategories(cc2Updated, c3, c4);
  }

  private void checkHasExpectedCategories(CategoryCombo cc, Category... categories) {
    Set<String> allCategoryUids =
        Arrays.stream(categories).map(BaseIdentifiableObject::getUid).collect(Collectors.toSet());
    Set<String> catComboCategoryUids =
        cc.getCategories().stream().map(BaseIdentifiableObject::getUid).collect(Collectors.toSet());
    assertEquals(allCategoryUids, catComboCategoryUids);
  }
}
