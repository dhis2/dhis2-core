/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge.category.optioncombo;

import jakarta.persistence.EntityManager;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
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
 * Main class for a {@link CategoryOptionCombo} merge.
 *
 * @author david mackessy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryOptionComboMergeService implements MergeService {

  private final CategoryService categoryService;
  private final MetadataCategoryOptionComboMergeHandler metadataMergeHandler;
  private final DataCategoryOptionComboMergeHandler dataMergeHandler;
  private final MergeValidator validator;
  private final EntityManager entityManager;
  private List<MetadataMergeHandler> metadataMergeHandlers;
  private List<DataMergeHandler> dataMergeHandlers;
  private List<DataMergeHandlerNoTarget> auditMergeHandlers;

  @Override
  public MergeRequest validate(@Nonnull MergeParams params, @Nonnull MergeReport mergeReport) {
    MergeRequest request =
        validator.validateUIDs(params, mergeReport, MergeType.CATEGORY_OPTION_COMBO);

    // merge-specific validation
    if (params.getDataMergeStrategy() == null) {
      mergeReport.addErrorMessage(new ErrorMessage(ErrorCode.E1534));
    }
    return request;
  }

  @Override
  public MergeReport merge(@Nonnull MergeRequest request, @Nonnull MergeReport mergeReport) {
    log.info("Performing CategoryOptionCombo merge");

    List<CategoryOptionCombo> sources =
        categoryService.getCategoryOptionCombosByUid(request.getSources());
    CategoryOptionCombo target =
        categoryService.getCategoryOptionCombo(request.getTarget().getValue());

    // merge metadata
    log.info("Handling CategoryOptionCombo reference associations and merges");
    metadataMergeHandlers.forEach(h -> h.merge(sources, target));
    dataMergeHandlers.forEach(h -> h.merge(sources, target, request));
    auditMergeHandlers.forEach(h -> h.merge(sources, request));

    // a flush is required here to bring the system into a consistent state. This is required so
    // that the deletion handler hooks, which are usually done using JDBC (non-Hibernate), can
    // see the most up-to-date state, including merges done using Hibernate.
    entityManager.flush();

    // handle deletes
    if (request.isDeleteSources()) handleDeleteSources(sources, mergeReport);

    return mergeReport;
  }

  private void handleDeleteSources(List<CategoryOptionCombo> sources, MergeReport mergeReport) {
    log.info("Deleting source CategoryOptionCombos");
    for (CategoryOptionCombo source : sources) {
      mergeReport.addDeletedSource(source.getUid());
      categoryService.deleteCategoryOptionCombo(source);
    }
  }

  @PostConstruct
  private void initMergeHandlers() {
    metadataMergeHandlers =
        List.of(
            metadataMergeHandler::handleCategoryOptions,
            metadataMergeHandler::handleCategoryCombos,
            metadataMergeHandler::handlePredictors,
            metadataMergeHandler::handleDataElementOperands,
            metadataMergeHandler::handleMinMaxDataElements,
            metadataMergeHandler::handleSmsCodes,
            metadataMergeHandler::handleIndicators,
            metadataMergeHandler::handleExpressions);

    dataMergeHandlers =
        List.of(
            dataMergeHandler::handleDataValues,
            dataMergeHandler::handleDataApprovals,
            dataMergeHandler::handleEvents,
            dataMergeHandler::handleCompleteDataSetRegistrations);

    auditMergeHandlers =
        List.of(
            dataMergeHandler::handleDataValueAudits, dataMergeHandler::handleDataApprovalAudits);
  }

  /**
   * Functional interface representing a {@link CategoryOptionCombo} data merge operation.
   *
   * @author david mackessy
   */
  @FunctionalInterface
  public interface MetadataMergeHandler {
    void merge(@Nonnull List<CategoryOptionCombo> sources, @Nonnull CategoryOptionCombo target);
  }

  @FunctionalInterface
  public interface DataMergeHandler {
    void merge(
        @Nonnull List<CategoryOptionCombo> sources,
        @Nonnull CategoryOptionCombo target,
        @Nonnull MergeRequest request);
  }

  @FunctionalInterface
  public interface DataMergeHandlerNoTarget {
    void merge(@Nonnull List<CategoryOptionCombo> sources, @Nonnull MergeRequest request);
  }
}
