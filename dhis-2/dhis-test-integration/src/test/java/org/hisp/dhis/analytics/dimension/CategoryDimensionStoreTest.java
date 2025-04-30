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
package org.hisp.dhis.analytics.dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.analytics.CategoryDimensionStore;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author david mackessy
 */
@Transactional
class CategoryDimensionStoreTest extends PostgresIntegrationTestBase {

  @Autowired private CategoryDimensionStore categoryDimensionStore;
  @Autowired private IdentifiableObjectManager manager;

  @Test
  @DisplayName("Retrieving CategoryDimensions by CategoryOptions returns the expected objects")
  void getCatDimensionsByCatOptionsTest() {
    // given 4 CategoryDimensions, each with a different CategoryOption
    CategoryOption co1 = createCategoryOption("1", CodeGenerator.generateUid());
    CategoryOption co2 = createCategoryOption("2", CodeGenerator.generateUid());
    CategoryOption co3 = createCategoryOption("3", CodeGenerator.generateUid());
    CategoryOption co4 = createCategoryOption("4", CodeGenerator.generateUid());
    manager.save(List.of(co1, co2, co3, co4));

    Category cat1 = createCategory('1', co1);
    Category cat2 = createCategory('2', co2);
    Category cat3 = createCategory('3', co3);
    Category cat4 = createCategory('4', co4);
    manager.save(List.of(cat1, cat2, cat3, cat4));

    CategoryDimension cd1 = createCategoryDimension(cat1);
    cd1.getItems().add(co1);

    CategoryDimension cd2 = createCategoryDimension(cat2);
    cd2.getItems().add(co2);

    CategoryDimension cd3 = createCategoryDimension(cat3);
    cd3.getItems().add(co3);

    CategoryDimension cd4 = createCategoryDimension(cat4);
    cd4.getItems().add(co4);

    categoryDimensionStore.save(cd1);
    categoryDimensionStore.save(cd2);
    categoryDimensionStore.save(cd3);
    categoryDimensionStore.save(cd4);

    // when retrieving CategoryDimensions by CategoryOption (3)
    List<CategoryDimension> cdsByCategoryOption =
        categoryDimensionStore.getByCategoryOption(
            List.of(co1.getUid(), co2.getUid(), co3.getUid()));

    // then
    assertEquals(3, cdsByCategoryOption.size(), "3 CategoryDimensions should be present");
    List<String> cos =
        cdsByCategoryOption.stream()
            .flatMap(cd -> cd.getItems().stream())
            .map(IdentifiableObject::getUid)
            .toList();

    assertEquals(3, cos.size(), "3 CategoryOptions should be present");
    assertTrue(
        cos.containsAll(List.of(co1.getUid(), co2.getUid(), co3.getUid())),
        "Retrieved CategoryOption UIDs should have expected UIDs");
  }
}
