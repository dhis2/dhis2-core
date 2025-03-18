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

import jakarta.persistence.EntityManager;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeRequest;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.merge.MergeType;
import org.hisp.dhis.merge.MergeValidator;
import org.springframework.stereotype.Service;

/**
 * Main class for a {@link CategoryOption} merge.
 *
 * @author david mackessy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryOptionMergeService implements MergeService {

  private final CategoryService categoryService;
  private final CategoryOptionMergeHandler categoryOptionMergeHandler;
  private final MergeValidator validator;
  private final EntityManager entityManager;
  private List<MetadataMergeHandler> metadataMergeHandlers;

  @Override
  public MergeRequest validate(@Nonnull MergeParams params, @Nonnull MergeReport mergeReport) {
    return validator.validateUIDs(params, mergeReport, MergeType.CATEGORY_OPTION);
  }

  @Override
  public MergeReport merge(@Nonnull MergeRequest request, @Nonnull MergeReport mergeReport) {
    log.info("Performing CategoryOption merge");

    List<CategoryOption> sources =
        categoryService.getCategoryOptionsByUid(UID.toValueList(request.getSources()));
    CategoryOption target = categoryService.getCategoryOption(request.getTarget().getValue());

    // merge metadata
    log.info("Handling CategoryOption reference associations and merges");
    metadataMergeHandlers.forEach(h -> h.merge(sources, target));

    // a flush is required here to bring the system into a consistent state. This is required so
    // that the deletion handler hooks, which are usually done using JDBC (non-Hibernate), can
    // see the most up-to-date state, including merges done using Hibernate.
    entityManager.flush();

    // handle deletes
    if (request.isDeleteSources()) handleDeleteSources(sources, mergeReport);

    return mergeReport;
  }

  private void handleDeleteSources(List<CategoryOption> sources, MergeReport mergeReport) {
    log.info("Deleting source CategoryOptions");
    for (CategoryOption source : sources) {
      mergeReport.addDeletedSource(source.getUid());
      categoryService.deleteCategoryOption(source);
    }
  }

  @PostConstruct
  private void initMergeHandlers() {
    metadataMergeHandlers =
        List.of(
            categoryOptionMergeHandler::handleCategories,
            categoryOptionMergeHandler::handleCategoryOptionCombos,
            categoryOptionMergeHandler::handleOrganisationUnits,
            categoryOptionMergeHandler::handleCategoryOptionGroups,
            categoryOptionMergeHandler::handleCategoryDimensions);
  }

  /**
   * Functional interface representing a {@link CategoryOption} data merge operation.
   *
   * @author david mackessy
   */
  @FunctionalInterface
  public interface MetadataMergeHandler {
    void merge(@Nonnull List<CategoryOption> sources, @Nonnull CategoryOption target);
  }
}
