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
package org.hisp.dhis.tracker.export.relationship;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.tracker.export.Order;

@Getter
@Builder
class RelationshipQueryParams {
  public static final int DEFAULT_PAGE = 1;

  public static final int DEFAULT_PAGE_SIZE = 50;

  private final IdentifiableObject entity;

  @Setter private Integer page;

  @Setter private Integer pageSize;

  @Setter private boolean totalPages;

  @Setter private boolean skipPaging;

  private List<Order> order;

  /** Returns the page number, falls back to default value of 1 if not specified. */
  public int getPageWithDefault() {
    return page != null && page > 0 ? page : DEFAULT_PAGE;
  }

  /** Returns the page size, falls back to default value of 50 if not specified. */
  public int getPageSizeWithDefault() {
    return pageSize != null && pageSize >= 0 ? pageSize : DEFAULT_PAGE_SIZE;
  }

  /** Returns the offset based on the page number and page size. */
  public int getOffset() {
    return (getPageWithDefault() - 1) * getPageSizeWithDefault();
  }
}
