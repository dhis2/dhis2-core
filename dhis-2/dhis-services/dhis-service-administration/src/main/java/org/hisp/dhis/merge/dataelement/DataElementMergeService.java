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
package org.hisp.dhis.merge.dataelement;

import com.google.common.collect.ImmutableList;
import jakarta.persistence.EntityManager;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.CommonMergeHandler;
import org.hisp.dhis.merge.MergeHandler;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeRequest;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.merge.MergeType;
import org.hisp.dhis.merge.MergeValidator;
import org.hisp.dhis.merge.dataelement.handler.AnalyticalDataElementMergeHandler;
import org.hisp.dhis.merge.dataelement.handler.DataDataElementMergeHandler;
import org.hisp.dhis.merge.dataelement.handler.MetadataDataElementMergeHandler;
import org.hisp.dhis.merge.dataelement.handler.TrackerDataElementMergeHandler;
import org.springframework.stereotype.Service;

/**
 * Main class for a {@link org.hisp.dhis.dataelement.DataElement} merge.
 *
 * @author david mackessy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataElementMergeService implements MergeService {

  private final DataElementService dataElementService;
  private final MetadataDataElementMergeHandler metadataMergeHandler;
  private final DataDataElementMergeHandler dataDataElementMergeHandler;
  private final AnalyticalDataElementMergeHandler analyticalMergeHandler;
  private final TrackerDataElementMergeHandler trackerMergeHandler;
  private final CommonMergeHandler commonMergeHandler;
  private final MergeValidator validator;
  private final DataElementMergeValidator dataElementMergeValidator;
  private final EntityManager entityManager;
  private ImmutableList<DataElementMergeHandler> metadataMergeHandlers;
  private ImmutableList<MergeHandler> commonMergeHandlers;
  private ImmutableList<DataElementDataMergeHandler> dataMergeHandlers;
  private ImmutableList<DataElementAuditMergeHandler> auditMergeHandlers;

  @Override
  public MergeRequest validate(@Nonnull MergeParams params, @Nonnull MergeReport mergeReport) {
    MergeRequest request = validator.validateUIDs(params, mergeReport, MergeType.DATA_ELEMENT);
    if (mergeReport.hasErrorMessages()) return request;

    // data element-specific validation
    if (params.getDataMergeStrategy() == null) {
      mergeReport.addErrorMessage(new ErrorMessage(ErrorCode.E1534));
      return request;
    }

    DataElement deTarget = dataElementService.getDataElement(request.getTarget().getValue());
    List<DataElement> deSources =
        dataElementService.getDataElementsByUid(
            request.getSources().stream().map(UID::getValue).toList());

    MergeReport report =
        dataElementMergeValidator.validateValueType(deTarget, deSources, mergeReport);

    if (report.hasErrorMessages()) return request;

    dataElementMergeValidator.validateDomainType(deTarget, deSources, mergeReport);
    return request;
  }

  @Override
  public MergeReport merge(@Nonnull MergeRequest request, @Nonnull MergeReport mergeReport) {
    log.info("Performing DataElement merge");

    List<DataElement> sources =
        dataElementService.getDataElementsByUid(UID.toValueList(request.getSources()));
    DataElement target = dataElementService.getDataElement(request.getTarget().getValue());

    // merge metadata
    log.info("Handling DataElement reference associations and merges");
    dataMergeHandlers.forEach(h -> h.merge(sources, target, request));
    metadataMergeHandlers.forEach(h -> h.merge(sources, target));
    commonMergeHandlers.forEach(h -> h.merge(sources, target));
    auditMergeHandlers.forEach(h -> h.merge(sources, request));

    // a flush is required here to bring Hibernate into a consistent state, due to some of the
    // above merge operations involving required native queries (queries involving JSONB). This
    // eliminates possible stale state issues.
    entityManager.flush();

    // handle deletes
    if (request.isDeleteSources()) handleDeleteSources(sources, mergeReport);

    return mergeReport;
  }

  private void handleDeleteSources(List<DataElement> sources, MergeReport mergeReport) {
    log.info("Deleting source DataElements");
    for (DataElement source : sources) {
      mergeReport.addDeletedSource(source.getUid());
      dataElementService.deleteDataElement(source);
    }
  }

  @PostConstruct
  private void initMergeHandlers() {
    metadataMergeHandlers =
        ImmutableList.<DataElementMergeHandler>builder()
            .add(analyticalMergeHandler::handleDataDimensionItems)
            .add(analyticalMergeHandler::handleEventVisualization)
            .add(analyticalMergeHandler::handleTrackedEntityDataElementDimension)
            .add(trackerMergeHandler::handleProgramStageDataElement)
            .add(trackerMergeHandler::handleProgramStageSection)
            .add(trackerMergeHandler::handleProgramNotificationTemplate)
            .add(trackerMergeHandler::handleProgramRuleVariable)
            .add(trackerMergeHandler::handleProgramRuleAction)
            .add(trackerMergeHandler::handleProgramIndicatorExpression)
            .add(trackerMergeHandler::handleProgramIndicatorFilter)
            .add(metadataMergeHandler::handlePredictor)
            .add(metadataMergeHandler::handlePredictorGeneratorExpression)
            .add(metadataMergeHandler::handlePredictorSampleSkipTestExpression)
            .add(metadataMergeHandler::handleMinMaxDataElement)
            .add(metadataMergeHandler::handleDataElementOperand)
            .add(metadataMergeHandler::handleDataSetElement)
            .add(metadataMergeHandler::handleSection)
            .add(metadataMergeHandler::handleDataElementGroup)
            .add(metadataMergeHandler::handleSmsCode)
            .build();

    dataMergeHandlers =
        ImmutableList.<DataElementDataMergeHandler>builder()
            .add(trackerMergeHandler::handleEventDataValues)
            .add(dataDataElementMergeHandler::handleDataValueDataElement)
            .build();

    auditMergeHandlers =
        ImmutableList.<DataElementAuditMergeHandler>builder()
            .add(trackerMergeHandler::handleEventChangeLogs)
            .add(dataDataElementMergeHandler::handleDataValueAuditDataElement)
            .build();

    commonMergeHandlers =
        ImmutableList.<MergeHandler>builder()
            .add(commonMergeHandler::handleRefsInIndicatorExpression)
            .add(commonMergeHandler::handleRefsInCustomForms)
            .build();
  }

  /**
   * Functional interface representing a {@link DataElement} audit merge operation.
   *
   * @author david mackessy
   */
  @FunctionalInterface
  public interface DataElementAuditMergeHandler {
    void merge(@Nonnull List<DataElement> sources, @Nonnull MergeRequest mergeRequest);
  }

  /**
   * Functional interface representing a {@link DataElement} data merge operation.
   *
   * @author david mackessy
   */
  @FunctionalInterface
  public interface DataElementDataMergeHandler {
    void merge(
        @Nonnull List<DataElement> sources,
        @Nonnull DataElement target,
        @Nonnull MergeRequest mergeRequest);
  }

  /**
   * Functional interface representing a {@link DataElement} merge operation.
   *
   * @author david mackessy
   */
  @FunctionalInterface
  public interface DataElementMergeHandler {
    void merge(List<DataElement> sources, DataElement target);
  }
}
