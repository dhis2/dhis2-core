/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.merge.orgunit;

import org.hisp.dhis.feedback.ConflictException;

/**
 * Main interface for org unit merge.
 *
 * @author Lars Helge Overland
 */
public interface OrgUnitMergeService {
  /**
   * Performs an org unit merge operation. Acquires the merge lock before the org units in the query
   * are resolved, so that the merge operates on the state committed by any preceding merge.
   *
   * @param query the {@link OrgUnitMergeQuery}.
   * @throws ConflictException if another merge is in progress.
   */
  void merge(OrgUnitMergeQuery query) throws ConflictException;

  /**
   * Performs an org unit merge operation.
   *
   * @param request the {@link OrgUnitMergeRequest}.
   * @throws ConflictException if another merge is in progress.
   */
  void merge(OrgUnitMergeRequest request) throws ConflictException;

  /**
   * Converts the given {@link OrgUnitMergeQuery} to an {@link OrgUnitMergeRequest}.
   *
   * @param request the {@link OrgUnitMergeQuery}.
   * @return an {@link OrgUnitMergeRequest}.
   */
  OrgUnitMergeRequest getFromQuery(OrgUnitMergeQuery query);
}
