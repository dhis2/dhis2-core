/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.deduplication;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;

@Data
public class PotentialDuplicateCriteria extends PagingAndSortingCriteriaAdapter {
  // TODO(tracker): set paging=true once skipPaging is removed. Both cannot have a default right
  // now. This would lead to invalid parameters if the user passes the other param i.e.
  // skipPaging==paging.
  // isPaged() handles the default case of skipPaging==paging==null => paging enabled
  @OpenApi.Property(defaultValue = "true")
  private Boolean paging;

  private List<String> trackedEntities = new ArrayList<>();

  private DeduplicationStatus status = DeduplicationStatus.OPEN;

  /**
   * Indicates whether to return a page of items or all items. By default, responses are paginated.
   *
   * <p>Note: this assumes {@link #getPaging()} and {@link #getSkipPaging()} have been validated.
   * Preference is given to {@link #getPaging()} as the other parameter is deprecated.
   */
  @OpenApi.Ignore
  public boolean isPaged() {
    if (getPaging() != null) {
      return Boolean.TRUE.equals(getPaging());
    }

    if (getSkipPaging() != null) {
      return Boolean.FALSE.equals(getSkipPaging());
    }

    return true;
  }
}
