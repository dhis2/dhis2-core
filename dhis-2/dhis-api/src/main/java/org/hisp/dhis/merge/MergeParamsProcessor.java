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
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.MergeReport;

/**
 * This interface is used to process merging of {@link IdentifiableObject}s uniformly, in a
 * standardised fashion. It requires an implementation of {@link MergeService}, which will process
 * the merging for its required use case. <br>
 * It essentially calls each method of the {@link MergeService} in the following order:<br>
 *
 * <ol>
 *   <li>transform
 *   <li>validate
 *   <li>merge
 * </ol>
 */
public interface MergeParamsProcessor {

  /**
   * Processes a merge query in full, using the implemented {@link MergeService} provided.
   *
   * @param mergeService implemented {@link MergeService} to process merge steps
   * @param mergeParams {@link MergeParams} to process
   * @param mergeType {@link MergeType}
   * @return updated {@link MergeReport} with any errors
   */
  default MergeReport processMergeParams(
      @Nonnull MergeService mergeService,
      @Nonnull MergeParams mergeParams,
      @Nonnull MergeType mergeType)
      throws ConflictException {
    MergeReport mergeReport = new MergeReport(mergeType);
    MergeRequest mergeRequest = mergeService.validate(mergeParams, mergeReport);
    if (mergeReport.hasErrorMessages()) return mergeReport;

    MergeReport report = mergeService.merge(mergeRequest, mergeReport);
    if (report.hasErrorMessages()) {
      throw new ConflictException("merge error here");
    }
    return report;
  }
}
