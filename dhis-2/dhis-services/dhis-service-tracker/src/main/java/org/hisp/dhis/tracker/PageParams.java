/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker;

import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.hisp.dhis.feedback.BadRequestException;

/**
 * {@link PageParams} represent the parameters that configure the page of items to be returned by a
 * service or store.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class PageParams {
  private static final int DEFAULT_PAGE = 1;
  private static final int DEFAULT_PAGE_SIZE = 50;

  /** The page number to be returned. */
  final int page;

  /** The number of items to be returned. */
  final int pageSize;

  /** Indicates whether to fetch the total number of items. */
  final boolean pageTotal;

  private PageParams(Integer page, Integer pageSize, boolean pageTotal) throws BadRequestException {
    if (page != null && page < 1) {
      throw new BadRequestException("page must be greater than or equal to 1 if specified");
    }

    if (pageSize != null && pageSize < 1) {
      throw new BadRequestException("pageSize must be greater than or equal to 1 if specified");
    }

    this.page = Objects.requireNonNullElse(page, DEFAULT_PAGE);
    this.pageSize = Objects.requireNonNullElse(pageSize, DEFAULT_PAGE_SIZE);
    this.pageTotal = pageTotal;
  }

  public static PageParams of(Integer page, Integer pageSize, boolean pageTotal)
      throws BadRequestException {
    return new PageParams(page, pageSize, pageTotal);
  }

  /** Create page parameters for the first page of a single item with no totals. */
  public static PageParams single() {
    return new PageParams(1, 1, false);
  }

  /** Zero-based offset to be used in a SQL offset clause. */
  public int getOffset() {
    return (page - 1) * pageSize;
  }
}
