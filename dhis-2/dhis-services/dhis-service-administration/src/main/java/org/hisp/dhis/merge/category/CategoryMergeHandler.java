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
package org.hisp.dhis.merge.category;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.CategoryDimensionStore;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryComboStore;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionStore;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserStore;
import org.springframework.stereotype.Component;

/**
 * Merge handler for metadata entities.
 *
 * @author david mackessy
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryMergeHandler {

  private final CategoryOptionStore categoryOptionStore;
  private final CategoryComboStore categoryComboStore;
  private final UserStore userStore;
  private final CategoryDimensionStore categoryDimensionStore;

  /**
   * Remove sources from {@link CategoryOption} and add target to {@link CategoryOption}. This
   * updates the owner side (Category.categoryOptions).
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleCategoryOptions(List<Category> sources, Category target) {
    List<CategoryOption> sourceCategoryOptions =
        categoryOptionStore.getCategoryOptions(UID.toUidValueSet(sources));
    sourceCategoryOptions.forEach(
        co -> {
          target.addCategoryOption(co);
          sources.forEach(src -> src.removeCategoryOption(co));
        });
    log.info("{} category options with source category refs updated", sourceCategoryOptions.size());
  }

  /**
   * Remove sources from {@link CategoryCombo} and add target to {@link CategoryCombo}
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleCategoryCombos(List<Category> sources, Category target) {
    List<CategoryCombo> sourceCategoryCombos =
        categoryComboStore.getCategoryCombosByCategory(UID.toUidValueSet(sources));
    sourceCategoryCombos.forEach(
        cc -> {
          cc.addCategory(target);
          sources.forEach(cc::removeCategory);
        });
    log.info("{} category combos with source category refs updated", sourceCategoryCombos.size());
  }

  /**
   * Remove sources from {@link User} dimension constraints and add target to {@link User} dimension
   * constraints
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleUsers(List<Category> sources, Category target) {
    int updated =
        userStore.updateCatDimensionConstraints(
            sources.stream().map(BaseIdentifiableObject::getId).collect(Collectors.toSet()),
            target.getId());
    log.info("{} user category dimension constraints with source category refs updated", updated);
  }

  /**
   * Replace source {@link Category} with target {@link Category} in {@link CategoryDimension}s
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleCategoryDimensions(List<Category> sources, Category target) {
    int updated =
        categoryDimensionStore.updateCatDimensions(
            sources.stream().map(BaseIdentifiableObject::getId).collect(Collectors.toSet()),
            target.getId());
    log.info("{} category dimensions with source category refs updated", updated);
  }
}
