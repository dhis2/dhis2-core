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

import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryComboStore;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataintegrity.DataIntegrityDetails;
import org.hisp.dhis.dataintegrity.DataIntegrityService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeRequest;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.merge.MergeType;
import org.hisp.dhis.merge.MergeValidator;
import org.hisp.dhis.scheduling.JobProgress;
import org.springframework.stereotype.Service;

/**
 * Merge Service for {@link CategoryCombo}s. Validates and handles merging references.
 *
 * @author david mackessy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryComboMergeService implements MergeService {

  private final CategoryService categoryService;
  private final DataIntegrityService dataIntegrityService;
  private final CategoryComboStore categoryComboStore;
  private final CategoryComboMergeHandler categoryComboMergeHandler;
  private final MergeValidator validator;
  private final EntityManager entityManager;
  private List<MetadataMergeHandler> metadataMergeHandlers;
  private static final String DUPLICATE_COCS_CHECK = "category_option_combos_have_duplicates";

  @Override
  public MergeRequest validate(@Nonnull MergeParams params, @Nonnull MergeReport mergeReport) {
    MergeRequest request = validator.validateUIDs(params, mergeReport, MergeType.CATEGORY_COMBO);

    // if there are already errors, skip additional validation
    if (mergeReport.hasErrorMessages()) {
      return request;
    }

    // fetch the actual CategoryCombos for validation
    List<CategoryCombo> sourceCategoryCombos =
        categoryComboStore.getCategoryCombosByUid(request.getSources());
    CategoryCombo targetCategoryCombo = categoryService.getCategoryCombo(request.getTarget());

    Set<String> sourceCcNames =
        sourceCategoryCombos.stream().map(CategoryCombo::getName).collect(Collectors.toSet());
    String targetCcName = targetCategoryCombo.getName();

    // validate that sources and target have identical categories
    validateIdenticalCategories(sourceCategoryCombos, targetCategoryCombo, mergeReport);
    if (mergeReport.hasErrorMessages()) {
      return request;
    }

    // validate CategoryOptionCombos
    checkForDuplicateCocsForCc(sourceCcNames, targetCcName, mergeReport);
    validateCategoryOptionCombos(sourceCategoryCombos, targetCategoryCombo, mergeReport);

    return request;
  }

  /**
   * Run a data integrity check for duplicate CategoryOptionCombos in CategoryCombos. Check the
   * details returned for duplicates.
   */
  private void checkForDuplicateCocsForCc(
      Set<String> sourceCcNames, @Nonnull String targetCcName, @Nonnull MergeReport mergeReport) {
    dataIntegrityService.runDetailsChecks(Set.of(DUPLICATE_COCS_CHECK), JobProgress.noop());
    Map<String, DataIntegrityDetails> checkDetails =
        dataIntegrityService.getDetails(Set.of(DUPLICATE_COCS_CHECK), 10_000L);

    checkForDuplicates(
        sourceCcNames, targetCcName, checkDetails.get(DUPLICATE_COCS_CHECK), mergeReport);
  }

  /**
   * Check the data integrity details for duplicate CategoryOptionCombos in CategoryCombos. The
   * check details contain the names of any duplicates found, so it's a name check.
   */
  protected static void checkForDuplicates(
      Set<String> sourceCcNames,
      String targetCcName,
      @CheckForNull DataIntegrityDetails checkDetails,
      @Nonnull MergeReport mergeReport) {
    if (checkDetails == null) return;
    checkDetails
        .getIssues()
        .forEach(
            issue -> {
              // check if CC is present
              Set<String> catCombos = new HashSet<>(issue.getRefs());
              for (String catComboName : catCombos) {
                if (sourceCcNames.contains(catComboName) || targetCcName.equals(catComboName)) {
                  mergeReport.addErrorMessage(
                      new ErrorMessage(ErrorCode.E1548, issue.getName(), catComboName));
                }
              }
            });
  }

  /**
   * Validates that all source CategoryCombos have the same Categories as the target. This is
   * required because CategoryOptionCombos are tied to specific Categories via their
   * CategoryOptions.
   *
   * @param sources list of source CategoryCombos
   * @param target target CategoryCombo
   * @param mergeReport report to update with errors
   */
  protected static void validateIdenticalCategories(
      List<CategoryCombo> sources, CategoryCombo target, MergeReport mergeReport) {
    Set<String> targetCategoryUids =
        target.getCategories().stream()
            .map(BaseIdentifiableObject::getUid)
            .collect(Collectors.toSet());

    for (CategoryCombo source : sources) {
      Set<String> sourceCategoryUids =
          source.getCategories().stream()
              .map(BaseIdentifiableObject::getUid)
              .collect(Collectors.toSet());

      if (!sourceCategoryUids.equals(targetCategoryUids)) {
        mergeReport.addErrorMessage(
            new ErrorMessage(
                ErrorCode.E1545, source.getUid(), String.join(", ", sourceCategoryUids)));
      }
    }
  }

  /**
   * Validates that all CategoryOptionCombos in the source CategoryCombos are valid: <br>
   * - have the correct number of CategoryOptions (one per Category) <br>
   * - have CategoryOptions that are part of a Category from the CategoryCombo <br>
   *
   * @param sources list of source CategoryCombos
   * @param target target CategoryCombo
   * @param mergeReport for reporting errors
   */
  protected static void validateCategoryOptionCombos(
      List<CategoryCombo> sources, CategoryCombo target, MergeReport mergeReport) {
    int expectedOptionCount = target.getCategories().size();

    Set<UID> validCategoryOptionUids =
        target.getCategories().stream()
            .flatMap(c -> c.getCategoryOptions().stream())
            .map(IdentifiableObject::getUID)
            .collect(Collectors.toSet());

    // 1. check for COC cardinality - number of options match number of Categories
    for (CategoryCombo source : sources) {
      for (CategoryOptionCombo coc : source.getOptionCombos()) {
        // check cardinality
        int actualOptionCount = coc.getCategoryOptions().size();
        if (actualOptionCount != expectedOptionCount) {
          mergeReport.addErrorMessage(
              new ErrorMessage(
                  ErrorCode.E1546,
                  String.valueOf(expectedOptionCount),
                  String.valueOf(actualOptionCount),
                  coc.getUid()));
        }

        // 2. check for COC CO validity - must be part of C from CC
        // check that all CategoryOptions are valid for the target's Categories
        Set<UID> invalidOptions =
            coc.getCategoryOptions().stream()
                .map(IdentifiableObject::getUID)
                .collect(Collectors.toSet());
        invalidOptions.removeAll(validCategoryOptionUids);

        if (!invalidOptions.isEmpty()) {
          mergeReport.addErrorMessage(
              new ErrorMessage(ErrorCode.E1547, String.join(", ", UID.toValueSet(invalidOptions))));
        }
      }
    }
  }

  @Override
  public MergeReport merge(@Nonnull MergeRequest request, @Nonnull MergeReport mergeReport) {
    log.info("Performing CategoryCombo merge");

    List<CategoryCombo> sources = categoryComboStore.getCategoryCombosByUid(request.getSources());
    CategoryCombo target = categoryService.getCategoryCombo(request.getTarget().getValue());

    // merge metadata
    log.info("Handling CategoryCombo reference associations and merges");
    metadataMergeHandlers.forEach(h -> h.merge(sources, target));

    // a flush is required here to bring the system into a consistent state. This is required so
    // that the deletion handler hooks, which are usually done using JDBC (non-Hibernate), can
    // see the most up-to-date state, including merges done using Hibernate.
    entityManager.flush();

    // handle deletes
    if (request.isDeleteSources()) handleDeleteSources(sources, mergeReport);

    return mergeReport;
  }

  private void handleDeleteSources(List<CategoryCombo> sources, MergeReport mergeReport) {
    log.info("Deleting source CategoryCombos");
    for (CategoryCombo source : sources) {
      categoryService.deleteCategoryCombo(source);
      mergeReport.addDeletedSource(source.getUid());
    }
  }

  @PostConstruct
  private void initMergeHandlers() {
    metadataMergeHandlers =
        List.of(
            categoryComboMergeHandler::handleCategories,
            categoryComboMergeHandler::handleCategoryOptionCombos,
            categoryComboMergeHandler::handleDataElements,
            categoryComboMergeHandler::handleDataSets,
            categoryComboMergeHandler::handleDataSetElements,
            categoryComboMergeHandler::handlePrograms,
            categoryComboMergeHandler::handleProgramEnrollments,
            categoryComboMergeHandler::handleDataApprovalWorkflows,
            categoryComboMergeHandler::handleProgramIndicatorCategoryCombos,
            categoryComboMergeHandler::handleProgramIndicatorAttributeCombos);
  }

  /**
   * Functional interface representing a {@link CategoryCombo} data merge operation.
   *
   * @author david mackessy
   */
  @FunctionalInterface
  public interface MetadataMergeHandler {
    void merge(@Nonnull List<CategoryCombo> sources, @Nonnull CategoryCombo target);
  }
}
