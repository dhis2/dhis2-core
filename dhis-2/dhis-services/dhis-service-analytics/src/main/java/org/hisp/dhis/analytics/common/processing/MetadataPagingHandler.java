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
package org.hisp.dhis.analytics.common.processing;

import org.hisp.dhis.analytics.common.params.AnalyticsPagingParams;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.SlimPager;

/**
 * This class handles the logic necessary to generate the metadata paging object.
 *
 * <p>It builds all data structure and maps required by the metadata object. It works on top of
 * common objects, so it can be reused by different analytics services/endpoints.
 *
 * <p>This class and methods were pulled from other part of the code, so we could have a centralized
 * way to generate and keep the logic related to analytics metadata elements. Light changes were
 * applied to make the code slightly cleaner. Major structural changes were not applied to reduce
 * the risk of bugs.
 */
class MetadataPagingHandler {
  /**
   * Handles all required logic/rules related to the paging element (if applicable), and returns the
   * correct {@link Pager} object.
   *
   * @param grid the {@link Grid}.
   * @param pagingParams the {@link AnalyticsPagingParams}.
   * @param rowsCount the total count.
   */
  Pager handle(Grid grid, AnalyticsPagingParams pagingParams, long rowsCount) {
    Pager pager;

    if (pagingParams.showTotalPages()) {
      pager =
          new Pager(
              pagingParams.getPageWithDefault(), rowsCount, pagingParams.getPageSizeWithDefault());

      // Always try to remove last row.
      removeLastRow(pagingParams, grid);
    } else {
      boolean isLastPage = handleLastPageFlag(pagingParams, grid);

      pager =
          new SlimPager(
              pagingParams.getPageWithDefault(), pagingParams.getPageSizeWithDefault(), isLastPage);
    }
    return pager;
  }

  /**
   * This method will handle the "lastPage" flag. Here, we assume that the given {@Grid} might have
   * page results + 1. We use this assumption to return the correct boolean value.
   *
   * @param pagingParams the {@link AnalyticsPagingParams}.
   * @param grid the {@link Grid}.
   * @return return true if this is the last page, false otherwise.
   */
  private boolean handleLastPageFlag(AnalyticsPagingParams pagingParams, Grid grid) {
    boolean isLastPage =
        grid.getHeight() > 0 && grid.getHeight() < pagingParams.getPageSizePlusOne();

    removeLastRow(pagingParams, grid);

    return isLastPage;
  }

  /**
   * As grid should have page size + 1 results, we need to remove the last row if there are more
   * pages left.
   *
   * @param pagingParams the {@link AnalyticsPagingParams}.
   * @param grid the {@link Grid}.
   */
  private void removeLastRow(AnalyticsPagingParams pagingParams, Grid grid) {
    boolean hasNextPageRow = grid.getHeight() == pagingParams.getPageSizePlusOne();

    if (hasNextPageRow) {
      grid.removeCurrentWriteRow();
    }
  }
}
