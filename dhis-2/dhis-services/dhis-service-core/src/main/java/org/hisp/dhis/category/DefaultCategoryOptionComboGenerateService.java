/*
 * Copyright (c) 2004-2022, University of Oslo
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

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author david mackessy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultCategoryOptionComboGenerateService
    implements CategoryOptionComboGenerateService {

  private final Object lock = new Object();
  private final CategoryService categoryService;
  private final CategoryComboStore categoryComboStore;
  private final TransactionTemplate transactionTemplate;

  @Override
  public void addAndPruneOptionCombos(CategoryCombo categoryCombo) {
    synchronized (lock) {
      transactionTemplate.execute(status -> addAndPruneOptionCombo(categoryCombo, null));
    }
  }

  @Override
  public void addAndPruneAllOptionCombos() {
    synchronized (lock) {
      transactionTemplate.execute(
          status -> {
            List<CategoryCombo> categoryCombos = categoryService.getAllCategoryCombos();
            for (CategoryCombo categoryCombo : categoryCombos) {
              addAndPruneOptionCombo(categoryCombo, null);
            }
            return null;
          });
    }
  }

  @Override
  public synchronized ImportSummaries addAndPruneOptionCombosWithSummary(
      @Nonnull CategoryCombo categoryCombo) {
    ImportSummaries importSummaries = new ImportSummaries();
    synchronized (lock) {
      transactionTemplate.execute(
          status -> {
            if (!categoryCombo.isValid()) {
              String msg =
                  "Category combo %s is invalid, could not update option combos"
                      .formatted(categoryCombo.getUid());
              log.warn(msg);
              importSummaries.addImportSummary(new ImportSummary(ImportStatus.ERROR, msg));
              return importSummaries;
            }
            return addAndPruneOptionCombo(categoryCombo, importSummaries);
          });
    }
    return importSummaries;
  }

  @Override
  @Transactional
  public synchronized void updateOptionCombos(Category category) {
    synchronized (lock) {
      transactionTemplate.execute(
          status -> {
            for (CategoryCombo categoryCombo : categoryService.getAllCategoryCombos()) {
              if (categoryCombo.getCategories().contains(category)) {
                addAndPruneOptionCombos(categoryCombo);
              }
            }
            return null;
          });
    }
  }

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
   * @param categoryCombo the CategoryCombo
   * @param importSummaries pass in an instantiated Import Summary if a report of the process is
   *     required valid one
   * @return returns an Import Summary if one was provided
   */
  @CheckForNull
  private ImportSummaries addAndPruneOptionCombo(
      @Nonnull CategoryCombo categoryCombo, ImportSummaries importSummaries) {
    Set<CategoryOptionCombo> generatedCocs = categoryCombo.generateOptionCombosList();
    Set<CategoryOptionCombo> persistedCocs =
        Sets.newHashSet(categoryComboStore.getByUid(categoryCombo.getUid()).getOptionCombos());

    // Persisted COC checks (update name or delete)
    for (CategoryOptionCombo persistedCoc : persistedCocs) {
      generatedCocs.stream()
          .filter(generatedCoc -> equalOrSameUid.test(persistedCoc, generatedCoc))
          .findFirst()
          .ifPresentOrElse(
              generatedCoc -> updateNameIfNotEqual(persistedCoc, generatedCoc, importSummaries),
              () -> deleteObsoleteCoc(persistedCoc, categoryCombo, importSummaries));
    }

    // Generated COC check (add if missing and not empty)
    for (CategoryOptionCombo generatedCoc : generatedCocs) {
      if (generatedCoc.getCategoryOptions().isEmpty()) {
        log.warn(
            "Generated category option combo %S has 0 options, skip adding for category combo `%s` as this is an invalid category option combo. Consider cleaning up the metadata model."
                .formatted(generatedCoc.getName(), categoryCombo.getName()));
      } else if (!persistedCocs.contains(generatedCoc)) {
        boolean cocAdded = categoryCombo.getOptionCombos().add(generatedCoc);
        if (cocAdded) categoryService.addCategoryOptionCombo(generatedCoc);

        String msg =
            "Added missing category option combo: `%s` for category combo: `%s`"
                .formatted(generatedCoc.getName(), categoryCombo.getName());
        log.info(msg);
        if (importSummaries != null) {
          ImportSummary importSummary = new ImportSummary();
          importSummary.setDescription(msg);
          importSummary.incrementImported();
          importSummaries.addImportSummary(importSummary);
        }
      }
    }
    return importSummaries;
  }

  private void deleteObsoleteCoc(
      CategoryOptionCombo coc, CategoryCombo cc, ImportSummaries summaries) {
    try {
      String cocName = coc.getName();
      cc.getOptionCombos().remove(coc);
      categoryService.deleteCategoryOptionComboNoRollback(coc);

      String msg =
          ("Deleted obsolete category option combo: `%s` for category combo: `%s`"
              .formatted(cocName, cc.getName()));
      log.info(msg);
      if (summaries != null) {
        ImportSummary importSummary = new ImportSummary();
        importSummary.setDescription(msg);
        importSummary.incrementDeleted();
        summaries.addImportSummary(importSummary);
      }
    } catch (DeleteNotAllowedException ex) {
      String msg =
          "Could not delete category option combo: `%s` due to `%s`"
              .formatted(coc.getName(), ex.getMessage());
      log.warn(msg);

      if (summaries != null) {
        ImportSummary importSummary = new ImportSummary();
        importSummary.setStatus(ImportStatus.WARNING);
        importSummary.setDescription(msg);
        importSummary.incrementIgnored();
        summaries.addImportSummary(importSummary);
      }
    }
  }

  private final BiPredicate<CategoryOptionCombo, CategoryOptionCombo> equalOrSameUid =
      (coc1, coc2) -> coc1.equals(coc2) || coc1.getUid().equals(coc2.getUid());

  private void updateNameIfNotEqual(
      CategoryOptionCombo coc1, CategoryOptionCombo coc2, ImportSummaries summaries) {
    if (!coc1.getName().equals(coc2.getName())) {
      coc1.setName(coc2.getName());
      if (summaries != null) {
        ImportSummary importSummary = new ImportSummary();
        importSummary.setDescription(
            "Update category option combo `%S` name to `%s`"
                .formatted(coc1.getUid(), coc1.getName()));
        importSummary.incrementUpdated();
        summaries.addImportSummary(importSummary);
      }
    }
  }
}
