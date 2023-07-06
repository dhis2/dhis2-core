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
package org.hisp.dhis.analytics.data.handler;

import static org.hisp.dhis.analytics.DataQueryParams.DX_INDEX;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.analytics.DataQueryParams.newBuilder;
import static org.hisp.dhis.analytics.ProcessingHint.SINGLE_INDICATOR_REPORTING_RATE_FILTER_ITEM;
import static org.hisp.dhis.analytics.ProcessingHint.SINGLE_PROGRAM_INDICATOR_REPORTING_RATE_FILTER_ITEM;
import static org.hisp.dhis.analytics.SortOrder.ASC;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;

import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.system.grid.ListGrid;
import org.springframework.stereotype.Component;

/**
 * Class responsible for aggregating all necessary data, provided by the handlers, into the Grid
 * object
 */
@Component
@RequiredArgsConstructor
public class DataAggregator {
  private final HeaderHandler headerHandler;

  private final MetadataHandler metaDataHandler;

  private final DataHandler dataHandler;

  /**
   * Returns a grid with aggregated data.
   *
   * @param params the {@link DataQueryParams}.
   * @return a grid with aggregated data.
   */
  public Grid getAggregatedDataValueGrid(DataQueryParams params) {
    params = preHandleQuery(params);

    // ---------------------------------------------------------------------
    // Headers
    // ---------------------------------------------------------------------

    Grid grid = new ListGrid();

    headerHandler.addHeaders(params, grid);

    // ---------------------------------------------------------------------
    // Data
    // ---------------------------------------------------------------------

    dataHandler.addIndicatorValues(params, grid);

    dataHandler.addDataElementValues(params, grid);

    dataHandler.addDataElementOperandValues(params, grid);

    dataHandler.addReportingRates(params, grid);

    dataHandler.addProgramDataElementAttributeIndicatorValues(params, grid);

    dataHandler.addDynamicDimensionValues(params, grid);

    dataHandler.addValidationResultValues(params, grid);

    dataHandler.addPerformanceMetrics(params, grid);

    // ---------------------------------------------------------------------
    // Meta-data
    // ---------------------------------------------------------------------

    metaDataHandler.addMetaData(params, grid);

    metaDataHandler.handleDataValueSet(params, grid);

    metaDataHandler.applyIdScheme(params, grid);

    postHandleGrid(params, grid);

    return grid;
  }

  /**
   * Returns headers, raw data and meta data as a grid.
   *
   * @param params the {@link DataQueryParams}.
   * @return a grid.
   */
  public Grid getRawDataGrid(DataQueryParams params) {
    Grid grid = new ListGrid();

    params = dataHandler.prepareForRawDataQuery(params);

    headerHandler.addHeaders(params, grid);

    dataHandler.addRawData(params, grid);

    metaDataHandler.addMetaData(params, grid);

    metaDataHandler.applyIdScheme(params, grid);

    return grid;
  }

  /**
   * Performs pre-handling of the given query and returns the immutable, handled query. If the query
   * has a single indicator as item for the data filter, the filter is set as a dimension and
   * removed as a filter.
   *
   * @param params the {@link DataQueryParams}.
   * @return a {@link DataQueryParams}.
   */
  private DataQueryParams preHandleQuery(DataQueryParams params) {
    if (params.hasSingleIndicatorAsDataFilter()
        || params.hasSingleProgramIndicatorAsDataFilter()
        || params.hasSingleReportingRateAsDataFilter()) {
      DimensionalObject dx = params.getFilter(DATA_X_DIM_ID);

      params =
          newBuilder(params)
              .addDimension(dx)
              .removeFilter(DATA_X_DIM_ID)
              .addProcessingHint(
                  params.hasSingleIndicatorAsDataFilter()
                      ? SINGLE_INDICATOR_REPORTING_RATE_FILTER_ITEM
                      : SINGLE_PROGRAM_INDICATOR_REPORTING_RATE_FILTER_ITEM)
              .build();
    }

    return params;
  }

  /**
   * Performs post-handling of the given grid. If the query has the single indicator as data filter
   * item, the column at the data dimension index is removed. If the query has sorting order, then
   * the grid is ordered on the value column based on the sorting specified.
   *
   * @param params the {@link DataQueryParams}.
   * @param the {@link Grid}.
   */
  private void postHandleGrid(DataQueryParams params, Grid grid) {
    if (params.hasProcessingHint(SINGLE_INDICATOR_REPORTING_RATE_FILTER_ITEM)
        || params.hasProcessingHint(SINGLE_PROGRAM_INDICATOR_REPORTING_RATE_FILTER_ITEM)) {
      grid.removeColumn(DX_INDEX);
    }

    if (params.hasOrder() && grid.getIndexOfHeader(VALUE_ID) >= 0) {
      int orderInt = params.getOrder().equals(ASC) ? -1 : 1;
      grid.sortGrid(grid.getIndexOfHeader(VALUE_ID) + 1, orderInt);
    }
  }

  @PostConstruct
  void init() {
    feedHandlers();
  }

  public void feedHandlers() {
    dataHandler.require(this);
  }
}
