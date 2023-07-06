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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@Slf4j
@Service("org.hisp.dhis.category.CategoryManager")
public class DefaultCategoryManager implements CategoryManager {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final CategoryService categoryService;

  public DefaultCategoryManager(CategoryService categoryService) {
    checkNotNull(categoryService);

    this.categoryService = categoryService;
  }

  // -------------------------------------------------------------------------
  // CategoryOptionCombo
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void addAndPruneOptionCombos(CategoryCombo categoryCombo) {
    if (categoryCombo == null || !categoryCombo.isValid()) {
      log.warn(
          "Category combo is null or invalid, could not update option combos: " + categoryCombo);
      return;
    }

    List<CategoryOptionCombo> generatedOptionCombos = categoryCombo.generateOptionCombosList();
    Set<CategoryOptionCombo> persistedOptionCombos =
        Sets.newHashSet(categoryCombo.getOptionCombos());

    boolean modified = false;

    Iterator<CategoryOptionCombo> iterator = persistedOptionCombos.iterator();

    while (iterator.hasNext()) {
      CategoryOptionCombo persistedOptionCombo = iterator.next();

      boolean isDelete = true;

      for (CategoryOptionCombo optionCombo : generatedOptionCombos) {
        if (optionCombo.equals(persistedOptionCombo)
            || persistedOptionCombo.getUid().equals(optionCombo.getUid())) {
          isDelete = false;
          if (!optionCombo.getName().equals(persistedOptionCombo.getName())) {
            persistedOptionCombo.setName(optionCombo.getName());
            modified = true;
          }
        }
      }

      if (isDelete) {
        try {
          categoryService.deleteCategoryOptionComboNoRollback(persistedOptionCombo);
        } catch (DeleteNotAllowedException ex) {
          log.warn("Could not delete category option combo: " + persistedOptionCombo);
          continue;
        }

        iterator.remove();
        categoryCombo.getOptionCombos().remove(persistedOptionCombo);

        log.info(
            "Deleted obsolete category option combo: "
                + persistedOptionCombo
                + " for category combo: "
                + categoryCombo.getName());
        modified = true;
      }
    }

    for (CategoryOptionCombo optionCombo : generatedOptionCombos) {
      if (!persistedOptionCombos.contains(optionCombo)) {
        categoryCombo.getOptionCombos().add(optionCombo);
        categoryService.addCategoryOptionCombo(optionCombo);

        log.info(
            "Added missing category option combo: "
                + optionCombo
                + " for category combo: "
                + categoryCombo.getName());
        modified = true;
      }
    }

    if (modified) {
      categoryService.updateCategoryCombo(categoryCombo);
    }
  }

  @Override
  @Transactional
  public void addAndPruneAllOptionCombos() {
    List<CategoryCombo> categoryCombos = categoryService.getAllCategoryCombos();

    for (CategoryCombo categoryCombo : categoryCombos) {
      addAndPruneOptionCombos(categoryCombo);
    }
  }
}
