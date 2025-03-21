/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller.event.webrequest;

import java.util.Optional;
import org.hisp.dhis.common.OpenApi;

/**
 * Paging parameters
 *
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
public interface PagingCriteria {
  Integer DEFAULT_PAGE = 1;

  Integer DEFAULT_PAGE_SIZE = 50;

  /** Page number to return. */
  Integer getPage();

  /** Page size. */
  Integer getPageSize();

  /** Indicates whether to include the total number of pages in the paging response. */
  boolean isTotalPages();

  /** Indicates whether paging should be skipped. */
  Boolean isSkipPaging();

  @OpenApi.Ignore
  default Integer getFirstResult() {
    Integer page = Optional.ofNullable(getPage()).filter(p -> p > 0).orElse(DEFAULT_PAGE);

    Integer pageSize =
        Optional.ofNullable(getPageSize()).filter(ps -> ps > 0).orElse(DEFAULT_PAGE_SIZE);

    return (page - 1) * pageSize;
  }
}
