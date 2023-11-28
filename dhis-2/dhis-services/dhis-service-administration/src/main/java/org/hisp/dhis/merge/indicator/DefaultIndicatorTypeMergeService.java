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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.util.ObjectUtils;
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
public class DefaultIndicatorTypeMergeService implements IndicatorTypeMergeService {

  private final IndicatorTypeMergeValidator validator;
  private final IndicatorService indicatorService;
  private final IdentifiableObjectManager idObjectManager;

  @Override
  @Transactional
  public void merge(IndicatorTypeMergeRequest request) {
    log.info("Indicator type merge request: {}", request);

    validator.validate(request);

    // merge manually here
    IndicatorType indicatorTypeTarget = request.getTarget();
    List<Indicator> associatedIndicators =
        indicatorService.getAssociatedIndicators(request.getSources());
    associatedIndicators.forEach(ind -> ind.setIndicatorType(indicatorTypeTarget));
    idObjectManager.update(indicatorTypeTarget);

    handleDeleteSources(request);

    log.info("Indicator type merge operation done: {}", request);
  }

  @Override
  public IndicatorTypeMergeRequest getFromQuery(IndicatorTypeMergeQuery query) {
    Set<IndicatorType> sources =
        query.getSources().stream()
            .map(this::getAndVerifyIndicatorType)
            .collect(Collectors.toSet());

    IndicatorType target = idObjectManager.get(IndicatorType.class, query.getTarget());

    return new IndicatorTypeMergeRequest.Builder()
        .addSources(sources)
        .withTarget(target)
        .withDeleteSources(query.isDeleteSources())
        .build();
  }

  // -------------------------------------------------------------------------
  // Private methods
  // -------------------------------------------------------------------------

  /**
   * Handles deletion of the source {@link IndicatorType}.
   *
   * @param request the {@link IndicatorTypeMergeRequest}.
   */
  private void handleDeleteSources(IndicatorTypeMergeRequest request) {
    if (request.isDeleteSources()) {

      for (IndicatorType indicatorType : request.getSources()) {
        idObjectManager.delete(indicatorType);
      }
    }
  }

  /**
   * Retrieves the indicator type with the given identifier. Throws an {@link IllegalQueryException}
   * if it does not exist.
   *
   * @param uid the indicator type identifier.
   * @throws IllegalQueryException if the object is null.
   */
  private IndicatorType getAndVerifyIndicatorType(String uid) throws IllegalQueryException {
    return ObjectUtils.throwIfNull(
        idObjectManager.get(IndicatorType.class, uid),
        () -> new IllegalQueryException(new ErrorMessage(ErrorCode.E1533, uid)));
  }
}
