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
package org.hisp.dhis.common.input;

import static org.hisp.dhis.jsontree.Validation.YesNo.NO;

import java.util.function.ToIntFunction;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.jsontree.Validation;

/**
 * URL parameters for endpoints that offer paging.
 *
 * <p>Include via @{@link org.hisp.dhis.jsontree.Collapsed}.
 *
 * @param skipPaging override to {@link #paging()} to skip paging
 * @param paging paging on/off (default on)
 * @param page page no to show (default 1)
 * @param pageSize entries per page (default 50)
 */
public record PagedParams(
    Boolean skipPaging,
    @Validation(required = NO) boolean paging,
    @Validation(required = NO, minimum = 1) int page,
    @Validation(required = NO, minimum = 1, maximum = 1000) int pageSize) {

  public static final PagedParams DEFAULT = new PagedParams(null, true, 1, 50);

  @OpenApi.Ignore
  public boolean isPaged() {
    if (skipPaging != null) return !skipPaging;
    return paging;
  }

  public int offset() {
    return (page - 1) * pageSize;
  }

  public Pager toPager(int totalPages) {
    return !isPaged() ? null : new Pager(page, totalPages, pageSize);
  }

  public <T> Pager toPager(T params, ToIntFunction<T> count) {
    return !isPaged() ? null : toPager(count.applyAsInt(params));
  }
}
