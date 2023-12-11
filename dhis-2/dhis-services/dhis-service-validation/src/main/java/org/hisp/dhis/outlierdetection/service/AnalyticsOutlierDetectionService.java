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

import static org.hisp.dhis.analytics.common.ColumnHeader.ABSOLUTE_DEVIATION;
import static org.hisp.dhis.analytics.common.ColumnHeader.ATTRIBUTE_OPTION_COMBO;
import static org.hisp.dhis.analytics.common.ColumnHeader.ATTRIBUTE_OPTION_COMBO_NAME;
import static org.hisp.dhis.analytics.common.ColumnHeader.CATEGORY_OPTION_COMBO;
import static org.hisp.dhis.analytics.common.ColumnHeader.CATEGORY_OPTION_COMBO_NAME;
import static org.hisp.dhis.analytics.common.ColumnHeader.DIMENSION;
import static org.hisp.dhis.analytics.common.ColumnHeader.DIMENSION_NAME;
import static org.hisp.dhis.analytics.common.ColumnHeader.LOWER_BOUNDARY;
import static org.hisp.dhis.analytics.common.ColumnHeader.MEDIAN;
import static org.hisp.dhis.analytics.common.ColumnHeader.MEDIAN_ABS_DEVIATION;
import static org.hisp.dhis.analytics.common.ColumnHeader.ORG_UNIT;
import static org.hisp.dhis.analytics.common.ColumnHeader.ORG_UNIT_NAME;
import static org.hisp.dhis.analytics.common.ColumnHeader.PERIOD;
import static org.hisp.dhis.analytics.common.ColumnHeader.STANDARD_DEVIATION;
import static org.hisp.dhis.analytics.common.ColumnHeader.UPPER_BOUNDARY;
import static org.hisp.dhis.analytics.common.ColumnHeader.VALUE;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm.MOD_Z_SCORE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.common.ColumnHeader;
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

    String meanOrMedianItem = isModifiedZScore ? MEDIAN.getItem() : ColumnHeader.MEAN.getItem();
    String meanOrMedianName = isModifiedZScore ? MEDIAN.getName() : ColumnHeader.MEAN.getName();

    String deviationItem =
        isModifiedZScore ? MEDIAN_ABS_DEVIATION.getItem() : STANDARD_DEVIATION.getItem();
    String deviationName =
        isModifiedZScore ? MEDIAN_ABS_DEVIATION.getName() : STANDARD_DEVIATION.getName();

    grid.addHeader(new GridHeader(DIMENSION.getItem(), DIMENSION.getName(), TEXT, false, false));
    grid.addHeader(
        new GridHeader(DIMENSION_NAME.getItem(), DIMENSION_NAME.getName(), TEXT, false, false));
    grid.addHeader(new GridHeader(PERIOD.getItem(), PERIOD.getName(), TEXT, false, false));
    grid.addHeader(new GridHeader(ORG_UNIT.getItem(), ORG_UNIT.getName(), TEXT, false, false));
    grid.addHeader(
        new GridHeader(ORG_UNIT_NAME.getItem(), ORG_UNIT_NAME.getName(), TEXT, false, false));
    grid.addHeader(
        new GridHeader(
            CATEGORY_OPTION_COMBO.getItem(), CATEGORY_OPTION_COMBO.getName(), TEXT, false, false));
    grid.addHeader(
        new GridHeader(
            CATEGORY_OPTION_COMBO_NAME.getItem(),
            CATEGORY_OPTION_COMBO_NAME.getName(),
            TEXT,
            false,
            false));
    grid.addHeader(
        new GridHeader(
            ATTRIBUTE_OPTION_COMBO.getItem(),
            ATTRIBUTE_OPTION_COMBO.getName(),
            TEXT,
            false,
            false));
    grid.addHeader(
        new GridHeader(
            ATTRIBUTE_OPTION_COMBO_NAME.getItem(),
            ATTRIBUTE_OPTION_COMBO_NAME.getName(),
            TEXT,
            false,
            false));
    grid.addHeader(new GridHeader(VALUE.getItem(), VALUE.getName(), NUMBER, false, false));
    grid.addHeader(new GridHeader(meanOrMedianItem, meanOrMedianName, NUMBER, false, false));
    grid.addHeader(new GridHeader(deviationItem, deviationName, NUMBER, false, false));
    grid.addHeader(
        new GridHeader(
            ABSOLUTE_DEVIATION.getItem(), ABSOLUTE_DEVIATION.getName(), NUMBER, false, false));
    grid.addHeader(new GridHeader(isModifiedZScore ? "modifiedzscore" : "zscore", NUMBER));
    grid.addHeader(
        new GridHeader(LOWER_BOUNDARY.getItem(), LOWER_BOUNDARY.getName(), NUMBER, false, false));
    grid.addHeader(
        new GridHeader(UPPER_BOUNDARY.getItem(), UPPER_BOUNDARY.getName(), NUMBER, false, false));
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
