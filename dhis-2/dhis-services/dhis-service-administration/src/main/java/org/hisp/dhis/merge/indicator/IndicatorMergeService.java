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
package org.hisp.dhis.merge.indicator;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.merge.CommonMergeHandler;
import org.hisp.dhis.merge.MergeHandler;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeRequest;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.merge.MergeType;
import org.hisp.dhis.merge.MergeValidator;
import org.hisp.dhis.merge.indicator.handler.IndicatorMergeHandler;
import org.springframework.stereotype.Service;

/**
 * Main class for indicator merge.
 *
 * @author david mackessy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorMergeService implements MergeService {

  private final IndicatorService indicatorService;
  private final IndicatorMergeHandler indicatorMergeHandler;
  private final CommonMergeHandler commonMergeHandler;
  private final MergeValidator validator;
  private ImmutableList<MergeHandler> commonMergeHandlers;
  private ImmutableList<org.hisp.dhis.merge.indicator.IndicatorMergeHandler> mergeHandlers;

  @Override
  public MergeRequest validate(@Nonnull MergeParams params, @Nonnull MergeReport mergeReport) {
    return validator.validateUIDs(params, mergeReport, MergeType.INDICATOR);
  }

  @Override
  public MergeReport merge(@Nonnull MergeRequest request, @Nonnull MergeReport mergeReport) {
    List<Indicator> sources =
        indicatorService.getIndicatorsByUid(UID.toValueList(request.getSources()));
    Indicator target = indicatorService.getIndicator(request.getTarget().getValue());

    // merge metadata
    mergeHandlers.forEach(h -> h.merge(sources, target));
    commonMergeHandlers.forEach(h -> h.merge(sources, target));

    // handle deletes
    if (request.isDeleteSources()) handleDeleteSources(sources, mergeReport);

    return mergeReport;
  }

  private void handleDeleteSources(List<Indicator> sources, MergeReport mergeReport) {
    for (Indicator source : sources) {
      mergeReport.addDeletedSource(source.getUid());
      indicatorService.deleteIndicator(source);
    }
  }

  @PostConstruct
  private void initMergeHandlers() {
    mergeHandlers =
        ImmutableList.<org.hisp.dhis.merge.indicator.IndicatorMergeHandler>builder()
            .add(indicatorMergeHandler::handleDataSets)
            .add(indicatorMergeHandler::handleIndicatorGroups)
            .add(indicatorMergeHandler::handleSections)
            .add(indicatorMergeHandler::handleDataDimensionItems)
            .add(indicatorMergeHandler::handleVisualizations)
            .build();

    commonMergeHandlers =
        ImmutableList.<MergeHandler>builder()
            .add(commonMergeHandler::handleRefsInIndicatorExpression)
            .add(commonMergeHandler::handleRefsInCustomForms)
            .build();
  }
}
