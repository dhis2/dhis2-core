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
package org.hisp.dhis.merge.category.option;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.CategoryDimensionStore;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboStore;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupStore;
import org.hisp.dhis.category.CategoryStore;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.springframework.stereotype.Component;

/**
 * Merge handler for metadata entities.
 *
 * @author david mackessy
 */
@Component
@RequiredArgsConstructor
public class CategoryOptionMergeHandler {

  private final CategoryStore categoryStore;
  private final CategoryOptionComboStore categoryOptionComboStore;
  private final CategoryOptionGroupStore categoryOptionGroupStore;
  private final OrganisationUnitStore organisationUnitStore;
  private final CategoryDimensionStore categoryDimensionStore;

  /**
   * Remove sources from {@link Category} and add target to {@link Category}
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleCategories(List<CategoryOption> sources, CategoryOption target) {
    List<Category> sourceCategories =
        categoryStore.getCategoriesByCategoryOption(UID.toUidValueSet(sources));
    sourceCategories.forEach(
        c -> {
          c.addCategoryOption(target);
          c.removeCategoryOptions(sources);
        });
  }

  /**
   * Remove sources from {@link CategoryOptionCombo} and add target to {@link CategoryOptionCombo}
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleCategoryOptionCombos(List<CategoryOption> sources, CategoryOption target) {
    List<CategoryOptionCombo> sourceCocs =
        categoryOptionComboStore.getCategoryOptionCombosByCategoryOption(
            UID.toUidValueSet(sources));

    sources.forEach(s -> s.removeCategoryOptionCombos(sourceCocs));
    target.addCategoryOptionCombos(sourceCocs);
  }

  /**
   * Remove sources from {@link OrganisationUnit} and add target to {@link OrganisationUnit}
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleOrganisationUnits(List<CategoryOption> sources, CategoryOption target) {
    List<OrganisationUnit> sourceOus =
        organisationUnitStore.getByCategoryOption(UID.toUidValueSet(sources));

    sourceOus.forEach(
        ou -> {
          ou.addCategoryOption(target);
          ou.removeCategoryOptions(sources);
        });
  }

  /**
   * Remove sources from {@link CategoryOptionGroup} and add target to {@link CategoryOptionGroup}
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleCategoryOptionGroups(List<CategoryOption> sources, CategoryOption target) {
    List<CategoryOptionGroup> sourceCogs =
        categoryOptionGroupStore.getByCategoryOption(UID.toUidValueSet(sources));

    sourceCogs.forEach(
        cog -> {
          cog.addCategoryOption(target);
          cog.removeCategoryOptions(sources);
        });
  }

  /**
   * Remove sources from {@link CategoryDimension} and add target to {@link CategoryDimension}
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleCategoryDimensions(List<CategoryOption> sources, CategoryOption target) {
    List<CategoryDimension> sourceCds =
        categoryDimensionStore.getByCategoryOption(UID.toUidValueSet(sources));

    sourceCds.forEach(
        cd -> {
          cd.addItem(target);
          cd.removeItems(sources);
        });
  }
}
