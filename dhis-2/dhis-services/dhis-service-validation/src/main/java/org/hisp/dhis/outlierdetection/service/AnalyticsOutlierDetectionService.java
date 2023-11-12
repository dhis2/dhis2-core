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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierValue;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.grid.ListGrid;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AnalyticsOutlierDetectionService extends AbstractOutlierDetectionService<Grid> {
  private final AnalyticsZScoreOutlierDetectionManager zScoreOutlierDetection;
  private SystemSettingManager systemSettingManager;

  public AnalyticsOutlierDetectionService(
          IdentifiableObjectManager idObjectManager,
          AnalyticsZScoreOutlierDetectionManager zScoreOutlierDetection, SystemSettingManager systemSettingManager) {
    super(idObjectManager);
    this.zScoreOutlierDetection = zScoreOutlierDetection;
    this.systemSettingManager = systemSettingManager;
  }

  @Override
  public Grid getOutlierValues(OutlierDetectionRequest request) throws IllegalQueryException {
    validate(request);
    List<OutlierValue> outlierValues = zScoreOutlierDetection.getOutlierValues(request);

    Grid grid = new ListGrid();
    setHeaders(grid);
    setMetaData(grid, request, outlierValues);
    setRows(grid, outlierValues);

    return grid;
  }

  @Override
  public void getOutlierValuesAsCsv(OutlierDetectionRequest request, Writer writer)
      throws IllegalQueryException, IOException {
    GridUtils.toCsv(getOutlierValues(request), writer);
  }

  @Override
  public void getOutlierValuesAsXml(OutlierDetectionRequest request, OutputStream outputStream)
      throws IllegalQueryException {
    GridUtils.toXml(getOutlierValues(request), outputStream);
  }

  @Override
  public void getOutlierValuesAsXls(OutlierDetectionRequest request, OutputStream outputStream)
      throws IllegalQueryException, IOException {
    GridUtils.toXls(getOutlierValues(request), outputStream);
  }

  @Override
  public void getOutlierValuesAsHtml(OutlierDetectionRequest request, Writer writer)
      throws IllegalQueryException {
    GridUtils.toHtml(getOutlierValues(request), writer);
  }

  @Override
  public void getOutlierValuesAsHtmlCss(OutlierDetectionRequest request, Writer writer)
      throws IllegalQueryException {
    GridUtils.toHtmlCss(getOutlierValues(request), writer);
  }

  @Override
  protected ErrorMessage validateForErrorMessage(OutlierDetectionRequest request) {
    ErrorMessage error = null;
    int maxLimit = systemSettingManager.getSystemSetting(SettingKey.ANALYTICS_MAX_LIMIT, Integer.class);

    if (request.getDataElements().isEmpty()) {
      error = new ErrorMessage(ErrorCode.E2200);
    } else if (request.getStartDate() == null || request.getEndDate() == null) {
      error = new ErrorMessage(ErrorCode.E2201);
    } else if (request.getStartDate().after(request.getEndDate())) {
      error = new ErrorMessage(ErrorCode.E2202);
    } else if (request.getOrgUnits().isEmpty()) {
      error = new ErrorMessage(ErrorCode.E2203);
    } else if (request.getThreshold() <= 0) {
      error = new ErrorMessage(ErrorCode.E2204);
    } else if (request.getMaxResults() <= 0) {
      error = new ErrorMessage(ErrorCode.E2205);
    } else if (request.getMaxResults() > maxLimit) {
      error = new ErrorMessage(ErrorCode.E2206, maxLimit);
    } else if (request.getDataStartDate() != null) {
      error = new ErrorMessage(ErrorCode.E2209);
    } else if (request.getDataEndDate() != null) {
      error = new ErrorMessage(ErrorCode.E2210);
    } else if (request.getAlgorithm() == OutlierDetectionAlgorithm.MIN_MAX) {
      error = new ErrorMessage(ErrorCode.E2211);
    }

    return error;
  }

  private void setHeaders(Grid grid) {
    grid.addHeader(new GridHeader("data element", ValueType.TEXT));
    grid.addHeader(new GridHeader("data element name", ValueType.TEXT));
    grid.addHeader(new GridHeader("period", ValueType.TEXT));
    grid.addHeader(new GridHeader("organisation unit", ValueType.TEXT));
    grid.addHeader(new GridHeader("organisation unit name", ValueType.TEXT));
    grid.addHeader(new GridHeader("category option", ValueType.TEXT));
    grid.addHeader(new GridHeader("category option name", ValueType.TEXT));
    grid.addHeader(new GridHeader("attribute option", ValueType.TEXT));
    grid.addHeader(new GridHeader("attribute option name", ValueType.TEXT));
    grid.addHeader(new GridHeader("value", ValueType.NUMBER));
    grid.addHeader(new GridHeader("mean", ValueType.NUMBER));
    grid.addHeader(new GridHeader("stdDev", ValueType.NUMBER));
    grid.addHeader(new GridHeader("absDev", ValueType.NUMBER));
    grid.addHeader(new GridHeader("zScore", ValueType.NUMBER));
    grid.addHeader(new GridHeader("lowerBound", ValueType.NUMBER));
    grid.addHeader(new GridHeader("upperBound", ValueType.NUMBER));
  }

  private void setMetaData(
      Grid grid, OutlierDetectionRequest request, List<OutlierValue> outlierValues) {
    grid.addMetaData("algorithm", request.getAlgorithm());
    grid.addMetaData("threshold", request.getThreshold());
    grid.addMetaData("orderBy", request.getOrderBy().getKey());
    grid.addMetaData("maxResults", request.getMaxResults());
    grid.addMetaData("count", outlierValues.size());
  }

  private void setRows(Grid grid, List<OutlierValue> outlierValues) {
    outlierValues.forEach(
        v -> {
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
          grid.addValue(v.getMean());
          grid.addValue(v.getStdDev());
          grid.addValue(v.getAbsDev());
          grid.addValue(v.getZScore());
          grid.addValue(v.getLowerBound());
          grid.addValue(v.getUpperBound());
        });
  }
}
