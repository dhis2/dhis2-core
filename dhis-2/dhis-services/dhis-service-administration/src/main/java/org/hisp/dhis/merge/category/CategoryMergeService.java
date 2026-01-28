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

import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeRequest;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.merge.MergeType;
import org.hisp.dhis.merge.MergeValidator;
import org.springframework.stereotype.Service;

/**
 * Merge Service for {@link Category}s. Validates and handles merging references.
 *
 * @author david mackessy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryMergeService implements MergeService {

  private final CategoryService categoryService;
  private final CategoryMergeHandler categoryMergeHandler;
  private final MergeValidator validator;
  private final EntityManager entityManager;
  private List<MetadataMergeHandler> metadataMergeHandlers;

  @Override
  public MergeRequest validate(@Nonnull MergeParams params, @Nonnull MergeReport mergeReport) {
    MergeRequest request = validator.validateUIDs(params, mergeReport, MergeType.CATEGORY);

    // if there are already errors, skip additional validation
    if (mergeReport.hasErrorMessages()) {
      return request;
    }

    // perform Category-specific validation
    List<Category> sourceCategories = categoryService.getCategoriesByUid(request.getSources());
    Category targetCategory = categoryService.getCategory(request.getTarget().getValue());
    validateCategoryOptions(sourceCategories, targetCategory, mergeReport);
    if (mergeReport.hasErrorMessages()) {
      return request;
    }

    validateCategoryCombos(sourceCategories, targetCategory, mergeReport);
    return request;
  }

  /**
   * Validates that all source and target Categories contain identical CategoryOptions (UID check).
   *
   * @param sources list of source Categories
   * @param target target Category
   * @param mergeReport merge report to update with error if validation fails
   */
  private void validateCategoryOptions(
      List<Category> sources, Category target, MergeReport mergeReport) {
    Set<String> sourceCoUids =
        sources.stream()
            .map(Category::getCategoryOptions)
            .flatMap(co -> co.stream().map(BaseMetadataObject::getUid))
            .collect(Collectors.toSet());

    Set<String> targetCoUids =
        target.getCategoryOptions().stream()
            .map(BaseMetadataObject::getUid)
            .collect(Collectors.toSet());

    // if source and target categories have different category options, add an error to report
    if (!sourceCoUids.equals(targetCoUids)) {
      mergeReport.addErrorMessage(
          new ErrorMessage(
              ErrorCode.E1535, String.join(", ", sourceCoUids), String.join(", ", targetCoUids)));
    }
  }

  /**
   * Validates that source and target Categories do not share any CategoryCombos (UID check).
   *
   * @param sources list of source Categories
   * @param target target Category
   * @param mergeReport merge report to update with errors if validation fails
   */
  private void validateCategoryCombos(
      List<Category> sources, Category target, MergeReport mergeReport) {
    List<String> sourceUids = sources.stream().map(BaseIdentifiableObject::getUid).toList();
    String targetUid = target.getUid();

    Set<String> sourceCatCombos =
        categoryService.getCategoryCombosByCategory(sourceUids).stream()
            .map(BaseMetadataObject::getUid)
            .collect(Collectors.toSet());
    Set<String> targetCatCombos =
        categoryService.getCategoryCombosByCategory(List.of(targetUid)).stream()
            .map(BaseMetadataObject::getUid)
            .collect(Collectors.toSet());

    Set<String> sharedCombos = new HashSet<>(sourceCatCombos);
    sharedCombos.retainAll(targetCatCombos);

    if (!sharedCombos.isEmpty()) {
      mergeReport.addErrorMessage(
          new ErrorMessage(ErrorCode.E1536, String.join(",", sharedCombos)));
    }
  }

  @Override
  public MergeReport merge(@Nonnull MergeRequest request, @Nonnull MergeReport mergeReport) {
    log.info("Performing Category merge");

    List<Category> sources =
        request.getSources().stream()
            .map(uid -> categoryService.getCategory(uid.getValue()))
            .toList();
    Category target = categoryService.getCategory(request.getTarget().getValue());

    // merge metadata
    log.info("Handling Category reference associations and merges");
    metadataMergeHandlers.forEach(h -> h.merge(sources, target));

    // a flush is required here to bring the system into a consistent state. This is required so
    // that the deletion handler hooks, which are usually done using JDBC (non-Hibernate), can
    // see the most up-to-date state, including merges done using Hibernate.
    entityManager.flush();

    // handle deletes
    if (request.isDeleteSources()) handleDeleteSources(sources, mergeReport);

    return mergeReport;
  }

  private void handleDeleteSources(List<Category> sources, MergeReport mergeReport) {
    log.info("Deleting source Categories");
    for (Category source : sources) {
      mergeReport.addDeletedSource(source.getUid());
      categoryService.deleteCategory(source);
    }
  }

  @PostConstruct
  private void initMergeHandlers() {
    metadataMergeHandlers =
        List.of(
            categoryMergeHandler::handleCategoryOptions,
            categoryMergeHandler::handleCategoryCombos,
            categoryMergeHandler::handleUsers,
            categoryMergeHandler::handleCategoryDimensions);
  }

  /**
   * Functional interface representing a {@link Category} data merge operation.
   *
   * @author david mackessy
   */
  @FunctionalInterface
  public interface MetadataMergeHandler {
    void merge(@Nonnull List<Category> sources, @Nonnull Category target);
  }
}
