/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.merge.indicator;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.merge.MergeQuery;
import org.hisp.dhis.merge.MergeRequest;
import org.hisp.dhis.merge.MergeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Main class for indicator type merge.
 *
 * @author david mackessy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultIndicatorTypeMergeService implements MergeService<IndicatorType> {

  private final MergeValidator indicatorTypeMergeValidator;
  private final IndicatorService indicatorService;
  private final IdentifiableObjectManager idObjectManager;

  @Override
  @Transactional
  public MergeReport merge(MergeRequest<IndicatorType> request, MergeReport mergeReport) {
    indicatorTypeMergeValidator.validate(request, mergeReport);

    if (mergeReport.hasErrorMessages()) {
      return mergeReport;
    }

    reassignIndicatorAssociations(request);
    handleDeleteSources(request, mergeReport);

    return mergeReport;
  }

  @Override
  public MergeRequest<IndicatorType> getFromQuery(MergeQuery query, MergeReport mergeReport) {
    // sources
    Set<IndicatorType> sources = new HashSet<>();
    if (query.getSources() == null || query.getSources().isEmpty()) {
      mergeReport.addErrorMessage(new ErrorMessage(ErrorCode.E1530));
    } else {
      for (String uid : query.getSources()) {
        Optional<IndicatorType> indicatorType =
            getAndVerifyIndicatorType(uid, mergeReport, "Source");
        indicatorType.ifPresent(sources::add);
      }
    }

    // target
    if (query.getTarget() == null) {
      mergeReport.addErrorMessage(new ErrorMessage(ErrorCode.E1531));
      return MergeRequest.empty();
    }
    Optional<IndicatorType> target =
        getAndVerifyIndicatorType(query.getTarget(), mergeReport, "Target");

    if (target.isPresent()) {
      return MergeRequest.<IndicatorType>builder()
          .sources(sources)
          .target(target.get())
          .deleteSources(query.isDeleteSources())
          .build();
    } else return MergeRequest.empty();
  }

  /**
   * Handles deletion of the source {@link IndicatorType}.
   *
   * @param request the {@link IndicatorTypeMergeRequest}.
   */
  private void handleDeleteSources(MergeRequest<IndicatorType> request, MergeReport mergeReport) {
    if (request.isDeleteSources()) {

      for (IndicatorType source : request.getSources()) {
        mergeReport.addDeletedSource(source.getUid());
        idObjectManager.delete(source);
      }
    }
  }

  /**
   * Retrieves the {@link IndicatorType} with the given identifier. Throws an {@link
   * IllegalQueryException} if it does not exist.
   *
   * @param uid the indicator type identifier
   */
  public Optional<IndicatorType> getAndVerifyIndicatorType(
      String uid, MergeReport mergeReport, String indType) {

    Optional<IndicatorType> indicatorType =
        Optional.ofNullable(idObjectManager.get(IndicatorType.class, uid));

    return indicatorType.or(
        () -> {
          mergeReport.addErrorMessage(new ErrorMessage(ErrorCode.E1533, indType, uid));
          return Optional.empty();
        });
  }

  /**
   * All {@link Indicator} associations with source {@link IndicatorType}s are reassigned to the
   * target {@link IndicatorType}
   *
   * @param request {@link IndicatorTypeMergeRequest}
   */
  private void reassignIndicatorAssociations(MergeRequest<IndicatorType> request) {
    IndicatorType target = request.getTarget();
    List<Indicator> associatedIndicators =
        indicatorService.getAssociatedIndicators(request.getSources());
    associatedIndicators.forEach(ind -> ind.setIndicatorType(target));
  }
}
