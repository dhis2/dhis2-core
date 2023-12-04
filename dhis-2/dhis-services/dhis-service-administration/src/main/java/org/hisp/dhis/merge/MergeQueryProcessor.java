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
package org.hisp.dhis.merge;

import javax.annotation.Nonnull;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.feedback.MergeReport;

/**
 * This class is used to process merging of {@link BaseIdentifiableObject}s uniformly, in a
 * standardised fashion. It requires an implementation of {@link MergeService}, which will process
 * the merging for its required use case. <br>
 * It essentially calls each method of the {@link MergeService} in the following order:<br>
 *
 * <ol>
 *   <li>transform
 *   <li>validate
 *   <li>merge
 * </ol>
 *
 * @param <T> type extending BaseIdentifiableObject
 */
public class MergeQueryProcessor<T extends BaseIdentifiableObject> {

  public MergeQueryProcessor(MergeService<T> mergeService) {
    this.mergeService = mergeService;
  }

  private final MergeService<T> mergeService;

  /**
   * Processes a merge query in full, using the implemented {@link MergeService} provided.
   *
   * @param mergeQuery {@link MergeQuery} to process
   * @param mergeType {@link MergeType}
   * @return updated {@link MergeReport} with any errors
   */
  public MergeReport processMergeQuery(
      @Nonnull MergeQuery mergeQuery, @Nonnull MergeType mergeType) {
    MergeReport mergeReport = new MergeReport(mergeType);
    MergeRequest<T> mergeRequest = mergeService.transform(mergeQuery, mergeReport);
    if (mergeReport.hasErrorMessages()) return mergeReport;

    MergeRequest<T> validatedRequest = mergeService.validate(mergeRequest, mergeReport);
    if (mergeReport.hasErrorMessages()) return mergeReport;

    return mergeService.merge(validatedRequest, mergeReport);
  }
}
