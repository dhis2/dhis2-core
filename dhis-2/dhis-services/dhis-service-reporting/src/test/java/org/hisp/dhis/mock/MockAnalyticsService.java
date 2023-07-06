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
package org.hisp.dhis.mock;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.period.Period;

/** Configurable mock implementation of AnalyticsService for testing purposes. */
public class MockAnalyticsService implements AnalyticsService {
  /** A fixed grid to return regardless of params. */
  private Grid grid;

  public void setGrid(Grid grid) {
    this.grid = grid;
  }

  /** A map of grids to retrun depending on a date that matches params. */
  private Map<Date, Grid> dateGridMap = null;

  public void setDateGridMap(Map<Date, Grid> dateGridMap) {
    this.dateGridMap = dateGridMap;
  }

  /** A map of grids to retrun depending on a base dimenstional object. */
  private Map<String, Grid> itemGridMap = null;

  public void setItemGridMap(Map<String, Grid> objectGridMap) {
    this.itemGridMap = objectGridMap;
  }

  public MockAnalyticsService() {}

  @Override
  public Grid getAggregatedDataValues(DataQueryParams params) {
    return getGrid(params);
  }

  @Override
  public Grid getAggregatedDataValues(
      DataQueryParams params, List<String> columns, List<String> rows) {
    throw new NotImplementedException("");
  }

  @Override
  public Grid getRawDataValues(DataQueryParams params) {
    throw new NotImplementedException("");
  }

  @Override
  public DataValueSet getAggregatedDataValueSet(DataQueryParams params) {
    throw new NotImplementedException("");
  }

  @Override
  public Grid getAggregatedDataValues(AnalyticalObject object) {
    throw new NotImplementedException("");
  }

  @Override
  public Map<String, Object> getAggregatedDataValueMapping(DataQueryParams params) {
    throw new NotImplementedException("");
  }

  @Override
  public Map<String, Object> getAggregatedDataValueMapping(AnalyticalObject object) {
    throw new NotImplementedException("");
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Find the pre-filled grid matching matching either a date in the the parameters' date range, or
   * a date in a PERIOD filter
   *
   * @param params the parameters
   * @return the grid matching the parameters' date range
   */
  private Grid getGrid(DataQueryParams params) {
    if (grid != null) {
      return grid;
    }

    if (dateGridMap != null) {
      return getDateGrid(params);
    }

    if (itemGridMap != null) {
      return getItemGrid(params);
    }

    throw new RuntimeException("Couldn't find grid to retrun");
  }

  /**
   * Find the pre-filled grid matching matching either a date in the the parameters' date range, or
   * a date in a PERIOD filter
   *
   * @param params the parameters
   * @return the grid matching the parameters' date range
   */
  private Grid getDateGrid(DataQueryParams params) {
    for (Map.Entry<Date, Grid> e : dateGridMap.entrySet()) {
      if (params.getStartDate() != null) {
        if (params.getStartDate().compareTo(e.getKey()) <= 0
            && params.getEndDate().compareTo(e.getKey()) >= 0) {
          return e.getValue();
        }
      } else {
        for (DimensionalObject o : params.getFilters()) {
          if (o.getDimensionType() == DimensionType.PERIOD && o.getItems() != null) {
            for (DimensionalItemObject item : o.getItems()) {
              Period period = (Period) item;

              if (period.getStartDate().compareTo(e.getKey()) <= 0
                  && period.getEndDate().compareTo(e.getKey()) >= 0) {
                return e.getValue();
              }
            }
          }
        }
      }
    }

    throw new RuntimeException("Couldn't find grid for date in params");
  }

  /**
   * Find the pre-filled grid matching matching a
   *
   * @param params the parameters
   * @return the grid matching the parameters' base dimenstional object
   */
  private Grid getItemGrid(DataQueryParams params) {
    for (DimensionalObject o : params.getDimensions()) {
      if (o.getItems() != null) {
        for (DimensionalItemObject i : o.getItems()) {
          Grid g = itemGridMap.get(i.getDimensionItem());

          if (g != null) {
            return g;
          }
        }
      }
    }

    throw new RuntimeException("Couldn't find grid for base dimensional object in params");
  }
}
