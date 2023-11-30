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

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.feedback.MergeReport;
import org.springframework.transaction.annotation.Transactional;

/**
 * Interface for merging {@link BaseIdentifiableObject}s
 *
 * @param <T> type extending {@link BaseIdentifiableObject}
 * @author david mackessy
 */
public interface MergeService<T extends BaseIdentifiableObject> {

  /**
   * This method transforms a {@link MergeQuery} to a {@link MergeRequest}. If there are any
   * errors/issues with the query then the {@link MergeReport} should be updated.
   *
   * @param query query to transform to {@link MergeRequest}
   * @param mergeReport report to be updated if any issues/errors with the {@link MergeQuery}
   * @return {@link MergeRequest}
   */
  MergeRequest<T> transform(MergeQuery query, MergeReport mergeReport);

  /**
   * This method validates a {@link MergeRequest}. If there are any errors/issues with the query
   * then the {@link MergeReport} should be updated.
   *
   * @param request request to be validated
   * @param mergeReport report to be updated if any issues/errors with the {@link MergeRequest}
   * @return {@link MergeReport}
   */
  MergeReport validate(MergeRequest<T> request, MergeReport mergeReport);

  /**
   * This method merges a {@link MergeRequest}. If there are any errors/issues with the {@link
   * MergeRequest} then the {@link MergeReport} should be updated.
   *
   * @param request request to be merged
   * @param mergeReport report to be updated if any issues/errors with the {@link MergeRequest}
   * @return {@link MergeReport}
   */
  @Transactional
  MergeReport merge(MergeRequest<T> request, MergeReport mergeReport);
}
