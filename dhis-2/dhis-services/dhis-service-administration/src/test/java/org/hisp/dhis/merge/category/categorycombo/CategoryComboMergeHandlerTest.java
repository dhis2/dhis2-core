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
package org.hisp.dhis.merge.category.categorycombo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CategoryComboMergeHandler} methods that operate purely on in-memory domain
 * objects, requiring no store or service dependencies.
 */
class CategoryComboMergeHandlerTest {

  private CategoryComboMergeHandler handler;

  @BeforeEach
  void setUp() {
    handler = new CategoryComboMergeHandler(null, null, null, null, null, null);
  }

  // ---------------------------------------------------------------------------
  // handleCategories
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("handleCategories does not throw ConcurrentModificationException")
  void handleCategories_doesNotThrowConcurrentModificationException() {
    // given a source with 3 categories - with only 2, the iterator cursor reaches the end of the
    // shrunk list and exits early without throwing; 3+ categories reliably trigger the CME because
    // after the first removal the list still has elements remaining when next() is called again
    Category cat1 = createCategory("cat1");
    Category cat2 = createCategory("cat2");
    Category cat3 = createCategory("cat3");
    CategoryCombo source = createCategoryCombo("source", cat1, cat2, cat3);
    CategoryCombo target = createCategoryCombo("target", cat1, cat2, cat3);

    // then
    assertDoesNotThrow(() -> handler.handleCategories(List.of(source), target));
  }

  @Test
  @DisplayName("handleCategories removes source CategoryCombo from each Category")
  void handleCategories_removesSourceFromCategories() {
    // given
    Category cat1 = createCategory("cat1");
    Category cat2 = createCategory("cat2");
    CategoryCombo source = createCategoryCombo("source", cat1, cat2);
    CategoryCombo target = createCategoryCombo("target", cat1, cat2);

    // when
    handler.handleCategories(List.of(source), target);

    // then source is no longer referenced by either category
    assertFalse(cat1.getCategoryCombos().contains(source));
    assertFalse(cat2.getCategoryCombos().contains(source));
  }

  @Test
  @DisplayName("handleCategories with multiple sources removes all sources from each Category")
  void handleCategories_multipleSources_removesAllSourcesFromCategories() {
    // given two sources sharing the same categories
    Category cat1 = createCategory("cat1");
    Category cat2 = createCategory("cat2");
    CategoryCombo source1 = createCategoryCombo("source1", cat1, cat2);
    CategoryCombo source2 = createCategoryCombo("source2", cat1, cat2);
    CategoryCombo target = createCategoryCombo("target", cat1, cat2);

    // when
    assertDoesNotThrow(() -> handler.handleCategories(List.of(source1, source2), target));

    // then both sources are removed from both categories
    assertFalse(cat1.getCategoryCombos().contains(source1));
    assertFalse(cat1.getCategoryCombos().contains(source2));
    assertFalse(cat2.getCategoryCombos().contains(source1));
    assertFalse(cat2.getCategoryCombos().contains(source2));
  }

  @Test
  @DisplayName("handleCategories leaves target CategoryCombo references on Categories intact")
  void handleCategories_doesNotRemoveTargetFromCategories() {
    // given
    Category cat1 = createCategory("cat1");
    Category cat2 = createCategory("cat2");
    CategoryCombo source = createCategoryCombo("source", cat1, cat2);
    CategoryCombo target = createCategoryCombo("target", cat1, cat2);

    // when
    handler.handleCategories(List.of(source), target);

    // then target is still referenced by both categories
    assertTrue(cat1.getCategoryCombos().contains(target));
    assertTrue(cat2.getCategoryCombos().contains(target));
  }

  // ---------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------

  private Category createCategory(String name) {
    Category category = new Category();
    category.setUid(CodeGenerator.generateUid());
    category.setName(name);
    return category;
  }

  private CategoryCombo createCategoryCombo(String name, Category... categories) {
    CategoryCombo combo = new CategoryCombo();
    combo.setUid(CodeGenerator.generateUid());
    combo.setName(name);
    for (Category category : categories) {
      combo.addCategory(category);
    }
    return combo;
  }
}
