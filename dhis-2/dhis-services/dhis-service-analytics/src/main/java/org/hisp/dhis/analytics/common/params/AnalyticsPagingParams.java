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
package org.hisp.dhis.analytics.common.params;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.hisp.dhis.common.Pager.DEFAULT_PAGE_SIZE;
import static org.hisp.dhis.webapi.controller.event.webrequest.PagingCriteria.DEFAULT_PAGE;

import lombok.Builder;
import lombok.Getter;

/** Class responsible for encapsulating all paging request params. */
@Getter
@Builder(toBuilder = true)
public class AnalyticsPagingParams implements IdentifiableKey {
  private Integer page;

  private Integer pageSize;

  private Boolean paging;

  private final Boolean totalPages;

  private final Boolean unlimited;

  public boolean isEmpty() {
    return page == null
        && pageSize == null
        && paging == null
        && totalPages == null
        && unlimited == null;
  }

  public boolean showTotalPages() {
    return toBooleanDefaultIfNull(paging, true) && toBooleanDefaultIfNull(totalPages, false);
  }

  public boolean isPaging() {
    return toBooleanDefaultIfNull(paging, true);
  }

  public boolean isUnlimited() {
    return toBooleanDefaultIfNull(unlimited, false);
  }

  public int getPageWithDefault() {
    return page != null && page > 0 ? page : DEFAULT_PAGE;
  }

  public int getPageSizeWithDefault() {
    return pageSize != null && pageSize >= 0 ? pageSize : DEFAULT_PAGE_SIZE;
  }

  public int getOffset() {
    return (getPageWithDefault() - 1) * getPageSizeWithDefault();
  }

  /**
   * Simply returns the current page size + 1. This is used in cases where need to know if there are
   * more results in the next page.
   *
   * @return the current page size incremented by 1.
   */
  public int getPageSizePlusOne() {
    return getPageSizeWithDefault() + 1;
  }

  public String getKey() {
    StringBuilder key = new StringBuilder();
    key.append(isPaging())
        .append(showTotalPages())
        .append(getPageWithDefault())
        .append(getPageSizeWithDefault());

    return key.toString();
  }
}
