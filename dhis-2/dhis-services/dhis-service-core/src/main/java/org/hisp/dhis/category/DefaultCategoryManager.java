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

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@Slf4j
@Service("org.hisp.dhis.category.CategoryManager")
@RequiredArgsConstructor
public class DefaultCategoryManager implements CategoryManager {

  private final CategoryService categoryService;

  /**
   * Aligns the persisted state (DB) of COCs with the generated state (in-memory) of COCs for a CC.
   * The generated state is treated as the most up-to-date state of the COCs. This method does the
   * following: <br>
   *
   * <ol>
   *   <li>Delete a persisted COC if it is not present in the generated COCs
   *   <li>Updates a persisted COC name if it is present and has a different name to its generated
   *       COC match
   *   <li>Add a generated COC if it is not present in the persisted COCs
   * </ol>
   *
   * @param categoryCombo the CategoryCombo.
   */
  @Override
  @Transactional
  public void addAndPruneOptionCombos(CategoryCombo categoryCombo) {
    if (categoryCombo == null || !categoryCombo.isValid()) {
      log.warn(
          "Category combo is null or invalid, could not update option combos: {}", categoryCombo);
      return;
    }

    List<CategoryOptionCombo> generatedCocs = categoryCombo.generateOptionCombosList();
    Set<CategoryOptionCombo> persistedCocs = Sets.newHashSet(categoryCombo.getOptionCombos());

    // Persisted COC checks (update name or delete)
    for (CategoryOptionCombo persistedCoc : persistedCocs) {
      generatedCocs.stream()
          .filter(generatedCoc -> equalOrSameUid.test(persistedCoc, generatedCoc))
          .findFirst()
          .ifPresentOrElse(
              generatedCoc -> updateNameIfNotEqual.accept(persistedCoc, generatedCoc),
              () -> deleteCoc.accept(persistedCoc, categoryCombo));
    }

    // Generated COC check (add if missing)
    for (CategoryOptionCombo generatedCoc : generatedCocs) {
      if (!persistedCocs.contains(generatedCoc)) {
        categoryCombo.getOptionCombos().add(generatedCoc);
        categoryService.addCategoryOptionCombo(generatedCoc);

        log.info(
            "Added missing category option combo: {} for category combo: {}",
            generatedCoc,
            categoryCombo.getName());
      }
    }
    categoryService.updateCategoryCombo(categoryCombo);
  }

  private final BiConsumer<CategoryOptionCombo, CategoryCombo> deleteCoc =
      (coc, cc) -> {
        try {
          categoryService.deleteCategoryOptionComboNoRollback(coc);
        } catch (DeleteNotAllowedException ex) {
          log.warn("Could not delete category option combo: {} due to {}", cc, ex.getMessage());
        }
        cc.getOptionCombos().remove(coc);
        log.info(
            "Removed obsolete category option combo: {} for category combo: {}", coc, cc.getName());
      };

  private final BiPredicate<CategoryOptionCombo, CategoryOptionCombo> equalOrSameUid =
      (coc1, coc2) -> coc1.equals(coc2) || coc1.getUid().equals(coc2.getUid());

  private final BiConsumer<CategoryOptionCombo, CategoryOptionCombo> updateNameIfNotEqual =
      (coc1, coc2) -> {
        if (!coc1.getName().equals(coc2.getName())) {
          coc1.setName(coc2.getName());
        }
      };

  @Override
  @Transactional
  public void addAndPruneAllOptionCombos() {
    List<CategoryCombo> categoryCombos = categoryService.getAllCategoryCombos();

    for (CategoryCombo categoryCombo : categoryCombos) {
      addAndPruneOptionCombos(categoryCombo);
    }
  }
}
