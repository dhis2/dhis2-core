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
import org.hisp.dhis.feedback.MergeReport;
import org.springframework.transaction.annotation.Transactional;

/**
 * Interface for merging {@link IdentifiableObject}s
 *
 * @author david mackessy
 */
public interface MergeService {

  /**
   * This method transforms a {@link MergeParams} to a {@link MergeRequest}. If there are any
   * errors/issues with the params then the {@link MergeReport} should be updated.
   *
   * @param params params to transform to {@link MergeRequest}
   * @param mergeReport report to be updated if any issues/errors with the {@link MergeParams}
   * @return {@link MergeRequest}
   */
  MergeRequest validate(@Nonnull MergeParams params, @Nonnull MergeReport mergeReport);

  /**
   * This method merges a {@link MergeRequest}. If there are any errors/issues with the {@link
   * MergeRequest} then the {@link MergeReport} should be updated.
   *
   * @param request request to be merged
   * @param mergeReport report to be updated if any issues/errors with the {@link MergeRequest}
   * @return {@link MergeReport}
   */
  @Transactional
  MergeReport merge(@Nonnull MergeRequest request, @Nonnull MergeReport mergeReport);

  /**
   * Gets the {@link MergeType} used by the service.
   *
   * @return {@link MergeType}
   */
  MergeType getMergeType();
}
