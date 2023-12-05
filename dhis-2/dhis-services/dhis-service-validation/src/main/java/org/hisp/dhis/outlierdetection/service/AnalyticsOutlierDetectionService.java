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
package org.hisp.dhis.outlierdetection.service;

import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm.MOD_Z_SCORE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierDetectionResponse;
import org.hisp.dhis.outlierdetection.OutlierValue;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.grid.ListGrid;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
public class AnalyticsOutlierDetectionService {

  private final AnalyticsZScoreOutlierDetectionManager zScoreOutlierDetection;

  /**
   * Transform the incoming request into api response (json).
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @return the {@link OutlierDetectionResponse}.
   */
  public Grid getOutlierValues(OutlierDetectionRequest request) throws IllegalQueryException {
    List<OutlierValue> outlierValues = zScoreOutlierDetection.getOutlierValues(request);

    Grid grid = new ListGrid();
    setHeaders(grid, request);
    setMetaData(grid, request, outlierValues);
    setRows(grid, outlierValues, request);

    return grid;
  }

  /**
   * Transform the incoming request into api response (csv download).
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @return the {@link OutlierDetectionResponse}.
   */
  public void getOutlierValuesAsCsv(OutlierDetectionRequest request, Writer writer)
      throws IllegalQueryException, IOException {
    GridUtils.toCsv(getOutlierValues(request), writer);
  }

  /**
   * Transform the incoming request into api response (xml).
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @return the {@link OutlierDetectionResponse}.
   */
  public void getOutlierValuesAsXml(OutlierDetectionRequest request, OutputStream outputStream)
      throws IllegalQueryException {
    GridUtils.toXml(getOutlierValues(request), outputStream);
  }

  /**
   * Transform the incoming request into api response (xls download).
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @return the {@link OutlierDetectionResponse}.
   */
  public void getOutlierValuesAsXls(OutlierDetectionRequest request, OutputStream outputStream)
      throws IllegalQueryException, IOException {
    GridUtils.toXls(getOutlierValues(request), outputStream);
  }

  /**
   * Transform the incoming request into api response (html).
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @return the {@link OutlierDetectionResponse}.
   */
  public void getOutlierValuesAsHtml(OutlierDetectionRequest request, Writer writer)
      throws IllegalQueryException {
    GridUtils.toHtml(getOutlierValues(request), writer);
  }

  /**
   * Transform the incoming request into api response (html with css).
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @return the {@link OutlierDetectionResponse}.
   */
  public void getOutlierValuesAsHtmlCss(OutlierDetectionRequest request, Writer writer)
      throws IllegalQueryException {
    GridUtils.toHtmlCss(getOutlierValues(request), writer);
  }

  private void setHeaders(Grid grid, OutlierDetectionRequest request) {
    boolean isModifiedZScore = request.getAlgorithm() == MOD_Z_SCORE;

    grid.addHeader(new GridHeader("dx", TEXT));
    grid.addHeader(new GridHeader("dxname", TEXT));
    grid.addHeader(new GridHeader("pe", TEXT));
    grid.addHeader(new GridHeader("ou", TEXT));
    grid.addHeader(new GridHeader("ouname", TEXT));
    grid.addHeader(new GridHeader("co", TEXT));
    grid.addHeader(new GridHeader("coname", TEXT));
    grid.addHeader(new GridHeader("ao", TEXT));
    grid.addHeader(new GridHeader("aoname", TEXT));
    grid.addHeader(new GridHeader("value", NUMBER));
    grid.addHeader(new GridHeader(isModifiedZScore ? "median" : "mean", NUMBER));
    grid.addHeader(new GridHeader(isModifiedZScore ? "medianabsdeviation" : "stddev", NUMBER));
    grid.addHeader(new GridHeader("absdev", NUMBER));
    grid.addHeader(new GridHeader(isModifiedZScore ? "modifiedzscore" : "zscore", NUMBER));
    grid.addHeader(new GridHeader("lowerbound", NUMBER));
    grid.addHeader(new GridHeader("upperbound", NUMBER));
  }

  private void setMetaData(
      Grid grid, OutlierDetectionRequest request, List<OutlierValue> outlierValues) {
    grid.addMetaData("algorithm", request.getAlgorithm());
    grid.addMetaData("threshold", request.getThreshold());
    grid.addMetaData("orderBy", request.getOrderBy().getKey());
    grid.addMetaData("maxResults", request.getMaxResults());
    grid.addMetaData("count", outlierValues.size());
  }

  private void setRows(
      Grid grid, List<OutlierValue> outlierValues, OutlierDetectionRequest request) {
    outlierValues.forEach(
        v -> {
          boolean isModifiedZScore = request.getAlgorithm() == MOD_Z_SCORE;
          grid.addRow();
          grid.addValue(v.getDe());
          grid.addValue(v.getDeName());
          grid.addValue(v.getPe());
          grid.addValue(v.getOu());
          grid.addValue(v.getOuName());
          grid.addValue(v.getCoc());
          grid.addValue(v.getCocName());
          grid.addValue(v.getAoc());
          grid.addValue(v.getAocName());
          grid.addValue(v.getValue());
          grid.addValue(isModifiedZScore ? v.getMedian() : v.getMean());
          grid.addValue(v.getStdDev());
          grid.addValue(v.getAbsDev());
          grid.addValue(v.getZScore());
          grid.addValue(v.getLowerBound());
          grid.addValue(v.getUpperBound());
        });
  }
}
