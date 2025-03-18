/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.tracker;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang3.StringUtils.joinWith;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.PAGER;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionItemKeywords.Keyword;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.common.ValueStatus;

@NoArgsConstructor(access = PRIVATE)
public class ResponseHelper {

  public static final int UNLIMITED_PAGING = 0;

  /**
   * Applies the headers to the given if the given query specifies headers.
   *
   * @param grid the {@link Grid}.
   * @param params the {@link EventQueryParams}.
   */
  public static void applyHeaders(Grid grid, EventQueryParams params) {
    if (params.hasHeaders()) {
      grid.retainColumns(params.getHeaders());
    }
  }

  /**
   * Extracts a list of {@link Keyword} from the dimensions present in the given params.
   *
   * @param params where to extract the dimensions from.
   * @return the list of keywords.
   */
  public static List<Keyword> getDimensionsKeywords(EventQueryParams params) {
    return params.getDimensions().stream()
        .map(DimensionalObject::getDimensionItemKeywords)
        .filter(
            dimensionItemKeywords ->
                dimensionItemKeywords != null && !dimensionItemKeywords.isEmpty())
        .flatMap(dk -> dk.getKeywords().stream())
        .toList();
  }

  /**
   * Add information about row context. The row context is based on the origin of the repeatable
   * stage value. Please see the {@link ValueStatus}.
   *
   * @param grid the {@link Grid}.
   */
  public static void setRowContextColumns(Grid grid) {
    Map<Integer, Map<String, Object>> oldRowContext = grid.getRowContext();
    Map<Integer, Map<String, Object>> newRowContext = new TreeMap<>();

    oldRowContext
        .keySet()
        .forEach(
            rowKey -> {
              Map<String, Object> newCols = new HashMap<>();
              Map<String, Object> cols = oldRowContext.get(rowKey);
              cols.keySet()
                  .forEach(
                      colKey ->
                          newCols.put(
                              Integer.toString(grid.getIndexOfHeader(colKey)), cols.get(colKey)));
              newRowContext.put(rowKey, newCols);
            });

    grid.setRowContext(newRowContext);
  }

  /**
   * Applies paging to the given grid if the given query specifies paging.
   *
   * @param params the {@link EventQueryParams}.
   * @param totalCount the total count.
   * @param grid the {@link Grid}.
   */
  public static void addPaging(EventQueryParams params, long totalCount, Grid grid) {
    if (params.isPaging()) {
      Pager pager =
          params.isTotalPages()
              ? new Pager(params.getPageWithDefault(), totalCount, params.getPageSizeWithDefault())
              : new SlimPager(
                  params.getPageWithDefault(),
                  params.getPageSizeWithDefault(),
                  grid.hasLastDataRow());

      grid.getMetaData().put(PAGER.getKey(), pager);
    }
  }

  /**
   * Based on the given item this method returns the correct UID based on internal rules.
   *
   * @param item the current QueryItem.
   * @return the correct UID based on the item type.
   */
  public static String getItemUid(QueryItem item) {
    String uid = item.getItem().getUid();

    if (item.hasProgramStage()) {
      uid = joinWith(".", item.getProgramStage().getUid(), uid);
    }

    return uid;
  }
}
