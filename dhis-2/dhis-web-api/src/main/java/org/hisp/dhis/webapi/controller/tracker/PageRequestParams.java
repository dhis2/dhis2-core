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
package org.hisp.dhis.webapi.controller.tracker;

import org.hisp.dhis.common.OpenApi;

/**
 * {@link PageRequestParams} represent the HTTP request parameters that configure whether the
 * response should be paginated and if so how. Tracker supports disabling pagination via {@code
 * paging=false} and enabling pagination via {@code paging=true}. Enabling and disabling parameters
 * are mutually exclusive. We can thus not set default values in our {@code RequestParams} classes
 * as we would not be able to discern a user supplied parameter value from a default value.
 *
 * <p>{@code totalPages=true} is only supported on paginated responses.
 */
@OpenApi.Shared(name = "TrackerPageRequestParams")
public interface PageRequestParams {
  /** Returns the page number to be returned. */
  Integer getPage();

  /** Returns the number of items to be returned. */
  Integer getPageSize();

  /** Indicates whether to include the total number of items and pages in the paginated response. */
  boolean isTotalPages();

  /**
   * Indicates whether to return all items {@code paging=false} or a page of items {@code
   * paging=true}.
   */
  boolean isPaging();
}
