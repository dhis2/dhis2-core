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

import com.google.common.base.MoreObjects;
import java.util.List;
import lombok.Data;
import org.apache.commons.lang3.BooleanUtils;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PagerUtils;

@Data
public class PotentialDuplicateQuery {
  public static final PotentialDuplicateQuery EMPTY = new PotentialDuplicateQuery();

  private Boolean skipPaging;

  private Boolean paging;

  private int page = 1;

  private int pageSize = Pager.DEFAULT_PAGE_SIZE;

  private int total;

  private List<String> teis;

  private DeduplicationStatus status = DeduplicationStatus.OPEN;

  public PotentialDuplicateQuery() {}

  public boolean isSkipPaging() {
    return PagerUtils.isSkipPaging(skipPaging, paging);
  }

  public boolean isPaging() {
    return BooleanUtils.toBoolean(paging);
  }

  public Pager getPager() {
    return PagerUtils.isSkipPaging(skipPaging, paging) ? null : new Pager(page, total, pageSize);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("page", page)
        .add("pageSize", pageSize)
        .add("total", total)
        .toString();
  }
}
