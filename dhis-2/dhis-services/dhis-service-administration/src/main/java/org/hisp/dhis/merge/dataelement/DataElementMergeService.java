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
package org.hisp.dhis.merge.dataelement;

import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeRequest;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.merge.MergeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Main class for {@link org.hisp.dhis.dataelement.DataElement} merge.
 *
 * @author david mackessy
 */
@Service
@RequiredArgsConstructor
public class DataElementMergeService implements MergeService {

  private final DataElementService dataElementService;
  private final DefaultDataElementMergeHandler dataElementMergeHandler;
  private final MergeValidator validator;
  private ImmutableList<org.hisp.dhis.merge.dataelement.DataElementMergeHandler> handlers;

  @Override
  public MergeRequest validate(@Nonnull MergeParams params, @Nonnull MergeReport mergeReport) {
    // sources
    Set<UID> sources = new HashSet<>();
    validator.verifySources(params.getSources(), sources, mergeReport, DataElement.class);

    // target
    validator.checkIsTargetInSources(sources, params.getTarget(), mergeReport, DataElement.class);

    return validator.verifyTarget(mergeReport, sources, params, DataElement.class);
  }

  @Override
  @Transactional
  public MergeReport merge(@Nonnull MergeRequest request, @Nonnull MergeReport mergeReport) {
    List<DataElement> sources =
        dataElementService.getDataElementsByUid(UID.toValueList(request.getSources()));
    DataElement target = dataElementService.getDataElement(request.getTarget().getValue());

    // merge metadata
    handlers.forEach(h -> h.merge(sources, target));

    // handle deletes
    if (request.isDeleteSources()) handleDeleteSources(sources, mergeReport);

    return mergeReport;
  }

  private void handleDeleteSources(List<DataElement> sources, MergeReport mergeReport) {
    for (DataElement source : sources) {
      mergeReport.addDeletedSource(source.getUid());
      dataElementService.deleteDataElement(source);
    }
  }

  @PostConstruct
  private void initMergeHandlers() {
    handlers =
        ImmutableList.<org.hisp.dhis.merge.dataelement.DataElementMergeHandler>builder()
            .add(dataElementMergeHandler::handleMinMaxDataElement)
            .build();
  }
}
