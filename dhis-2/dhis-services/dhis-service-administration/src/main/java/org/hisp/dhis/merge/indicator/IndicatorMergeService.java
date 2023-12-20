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

import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeRequest;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.merge.indicator.handler.MetadataIndicatorMergeHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Main class for indicator type merge.
 *
 * @author david mackessy
 */
@Service
@RequiredArgsConstructor
public class IndicatorMergeService implements MergeService {

  private final IndicatorService indicatorService;
  private final IdentifiableObjectManager idObjectManager;
  private final MetadataIndicatorMergeHandler metadataIndicatorMergeHandler;
  private ImmutableList<IndicatorMergeHandler> handlers;

  @Override
  public MergeRequest validate(@Nonnull MergeParams params, @Nonnull MergeReport mergeReport) {
    // sources
    Set<UID> sources = new HashSet<>(); // TODO possible to generify & reuse?
    Optional.ofNullable(params.getSources())
        .filter(CollectionUtils::isNotEmpty)
        .ifPresentOrElse(
            ids -> getSourcesAndVerify(ids, mergeReport, sources),
            () -> mergeReport.addErrorMessage(new ErrorMessage(ErrorCode.E1540)));

    // target
    Optional<UID> target = Optional.ofNullable(params.getTarget());
    checkIsTargetInSources(sources, target, mergeReport);

    return target
        .map(t -> getTargetAndVerify(t, mergeReport, sources, params))
        .orElseGet(
            () -> {
              mergeReport.addErrorMessage(new ErrorMessage(ErrorCode.E1541));
              return MergeRequest.empty();
            });
  }

  @Override
  @Transactional
  public MergeReport merge(@Nonnull MergeRequest request, @Nonnull MergeReport mergeReport) {
    List<Indicator> sources =
        indicatorService.getIndicatorsByUid(UID.toValueList(request.getSources()));
    Indicator target = indicatorService.getIndicator(request.getTarget().getValue());

    // merge metadata
    handlers.forEach(h -> h.merge(sources, target));

    // handle deletes
    if (request.isDeleteSources()) handleDeleteSources(sources, mergeReport);

    return mergeReport;
  }

  private void checkIsTargetInSources(
      Set<UID> sources, Optional<UID> target, MergeReport mergeReport) {
    target.ifPresent(
        t -> {
          if (sources.contains(t)) mergeReport.addErrorMessage(new ErrorMessage(ErrorCode.E1532));
        });
  }

  private void handleDeleteSources(List<Indicator> sources, MergeReport mergeReport) {
    for (Indicator source : sources) {
      mergeReport.addDeletedSource(source.getUid());
      idObjectManager.delete(source);
    }
  }

  /**
   * Retrieves the {@link Indicator} with the given identifier. If a valid {@link Indicator} is not
   * found then an empty {@link Optional} is returned and an error is added to the report.
   *
   * @param uid the indicator identifier
   * @return {@link Optional<UID>}
   */
  private Optional<UID> getAndVerifyIndicator(UID uid, MergeReport mergeReport, String ind) {
    return Optional.ofNullable(idObjectManager.get(Indicator.class, uid.getValue()))
        .map(i -> UID.of(i.getUid()))
        .or(
            () -> {
              mergeReport.addErrorMessage(new ErrorMessage(ErrorCode.E1543, ind, uid));
              return Optional.empty();
            });
  }

  private void getSourcesAndVerify(Set<UID> uids, MergeReport report, Set<UID> indicators) {
    uids.forEach(uid -> getAndVerifyIndicator(uid, report, "Source").ifPresent(indicators::add));
  }

  private MergeRequest getTargetAndVerify(
      UID target, MergeReport report, Set<UID> sources, MergeParams params) {
    return getAndVerifyIndicator(target, report, "Target")
        .map(
            t ->
                MergeRequest.builder()
                    .sources(sources)
                    .target(t)
                    .deleteSources(params.isDeleteSources())
                    .build())
        .orElse(MergeRequest.empty());
  }

  @PostConstruct
  private void initMergeHandlers() {
    handlers =
        ImmutableList.<IndicatorMergeHandler>builder()
            // data set - remove source & add target
            .add(metadataIndicatorMergeHandler::mergeDataSets)

            // indicator group - remove source & add target
            .add(metadataIndicatorMergeHandler::mergeIndicatorGroups)

            // data dimensional item - set target as indicator

            // section - remove source & add target
            .add(metadataIndicatorMergeHandler::mergeSections)

            // configuration - remove source & add target

            // handle indicator numerator / denominator

            // handle data entry forms (custom forms - html property (STRING))
            .build();
  }
}
