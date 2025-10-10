/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.outlier.service;

import static org.hisp.dhis.analytics.OutlierDetectionAlgorithm.MODIFIED_Z_SCORE;
import static org.hisp.dhis.analytics.common.ColumnHeader.ABSOLUTE_DEVIATION;
import static org.hisp.dhis.analytics.common.ColumnHeader.ATTRIBUTE_OPTION_COMBO;
import static org.hisp.dhis.analytics.common.ColumnHeader.ATTRIBUTE_OPTION_COMBO_NAME;
import static org.hisp.dhis.analytics.common.ColumnHeader.CATEGORY_OPTION_COMBO;
import static org.hisp.dhis.analytics.common.ColumnHeader.CATEGORY_OPTION_COMBO_NAME;
import static org.hisp.dhis.analytics.common.ColumnHeader.DIMENSION;
import static org.hisp.dhis.analytics.common.ColumnHeader.DIMENSION_NAME;
import static org.hisp.dhis.analytics.common.ColumnHeader.LOWER_BOUNDARY;
import static org.hisp.dhis.analytics.common.ColumnHeader.MEAN;
import static org.hisp.dhis.analytics.common.ColumnHeader.MEDIAN;
import static org.hisp.dhis.analytics.common.ColumnHeader.MEDIAN_ABS_DEVIATION;
import static org.hisp.dhis.analytics.common.ColumnHeader.ORG_UNIT;
import static org.hisp.dhis.analytics.common.ColumnHeader.ORG_UNIT_NAME;
import static org.hisp.dhis.analytics.common.ColumnHeader.ORG_UNIT_NAME_HIERARCHY;
import static org.hisp.dhis.analytics.common.ColumnHeader.PERIOD;
import static org.hisp.dhis.analytics.common.ColumnHeader.PERIOD_NAME;
import static org.hisp.dhis.analytics.common.ColumnHeader.STANDARD_DEVIATION;
import static org.hisp.dhis.analytics.common.ColumnHeader.UPPER_BOUNDARY;
import static org.hisp.dhis.analytics.common.ColumnHeader.VALUE;
import static org.hisp.dhis.analytics.common.ColumnHeader.ZSCORE;
import static org.hisp.dhis.common.IdentifiableProperty.CODE;
import static org.hisp.dhis.common.IdentifiableProperty.ID;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.hisp.dhis.analytics.cache.OutliersCache;
import org.hisp.dhis.analytics.common.ColumnHeader;
import org.hisp.dhis.analytics.common.TableInfoReader;
import org.hisp.dhis.analytics.data.DimensionalObjectProvider;
import org.hisp.dhis.analytics.outlier.data.Outlier;
import org.hisp.dhis.analytics.outlier.data.OutlierRequest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.ExecutionPlan;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.comparator.AscendingPeriodComparator;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class AnalyticsOutlierService {

  private final AnalyticsZScoreOutlierDetector zScoreOutlierDetector;

  private final OutliersCache outliersCache;

  private final OrganisationUnitService organisationUnitService;

  private final UserService userService;

  private final TableInfoReader tableInfoReader;

  private final IdentifiableObjectManager idObjectManager;

  private final DimensionalObjectProvider dimensionalObjectProducer;

  /**
   * Transform the incoming request into api response (json).
   *
   * @param request the {@link OutlierRequest}.
   * @return the {@link Grid}.
   */
  public Grid getOutliers(OutlierRequest request) throws IllegalQueryException {
    List<Outlier> outliers =
        outliersCache.getOrFetch(request, p -> zScoreOutlierDetector.getOutliers(request));

    Grid grid = new ListGrid();
    setHeaders(grid, request);
    setMetaData(grid, outliers, request);
    setRows(grid, outliers, request);

    return grid;
  }

  public Grid getOutliersPerformanceMetrics(OutlierRequest request) {
    List<ExecutionPlan> executionPlans = zScoreOutlierDetector.getExecutionPlans(request);

    Grid grid = new ListGrid();
    grid.addPerformanceMetrics(executionPlans);

    return grid;
  }

  /**
   * Transform the incoming request into api response (csv download).
   *
   * @param request the {@link OutlierRequest}.
   */
  public void getOutlierAsCsv(OutlierRequest request, Writer writer)
      throws IllegalQueryException, IOException {
    GridUtils.toCsv(getOutliers(request), writer);
  }

  /**
   * Transform the incoming request into api response (xml).
   *
   * @param request the {@link OutlierRequest}.
   */
  public void getOutliersAsXml(OutlierRequest request, OutputStream outputStream)
      throws IllegalQueryException {
    GridUtils.toXml(getOutliers(request), outputStream);
  }

  /**
   * Transform the incoming request into api response (xls download).
   *
   * @param request the {@link OutlierRequest}.
   */
  public void getOutliersAsXls(OutlierRequest request, OutputStream outputStream)
      throws IllegalQueryException, IOException {
    GridUtils.toXls(getOutliers(request), outputStream);
  }

  /**
   * Transform the incoming request into api response (xlsx download).
   *
   * @param request the {@link OutlierRequest}.
   */
  public void getOutliersAsXlsx(OutlierRequest request, OutputStream outputStream)
      throws IllegalQueryException, IOException {
    GridUtils.toXlsx(getOutliers(request), outputStream);
  }

  /**
   * Transform the incoming request into api response (html).
   *
   * @param request the {@link OutlierRequest}.
   */
  public void getOutliersAsHtml(OutlierRequest request, Writer writer)
      throws IllegalQueryException {
    GridUtils.toHtml(getOutliers(request), writer);
  }

  /**
   * Transform the incoming request into api response (html with css).
   *
   * @param request the {@link OutlierRequest}.
   */
  public void getOutliersAsHtmlCss(OutlierRequest request, Writer writer)
      throws IllegalQueryException {
    GridUtils.toHtmlCss(getOutliers(request), writer);
  }

  /**
   * The inclusion of outliers is an optional aspect of the analytics table. The outliers API entry
   * point can generate a proper response only when outliers are exported along with the analytics
   * table. The 'sourceid' column serves as a reliable indicator for successful outliers export. Its
   * absence implies that the outliers were not exported.
   */
  public void checkAnalyticsTableForOutliers() {
    if (tableInfoReader.getInfo("analytics").getColumns().stream()
        .noneMatch("sourceid"::equalsIgnoreCase)) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E7180));
    }
  }

  private void setHeaders(Grid grid, OutlierRequest request) {
    boolean isModifiedZScore = request.getAlgorithm() == MODIFIED_Z_SCORE;

    String zScoreOrModZScoreItem =
        isModifiedZScore ? ColumnHeader.MODIFIED_Z_SCORE.getItem() : ZSCORE.getItem();
    String zScoreOrModZScoreName =
        isModifiedZScore ? ColumnHeader.MODIFIED_Z_SCORE.getName() : ZSCORE.getName();

    String meanOrMedianItem = isModifiedZScore ? MEDIAN.getItem() : MEAN.getItem();
    String meanOrMedianName = isModifiedZScore ? MEDIAN.getName() : MEAN.getName();

    String deviationItem =
        isModifiedZScore ? MEDIAN_ABS_DEVIATION.getItem() : STANDARD_DEVIATION.getItem();
    String deviationName =
        isModifiedZScore ? MEDIAN_ABS_DEVIATION.getName() : STANDARD_DEVIATION.getName();

    grid.addHeader(new GridHeader(DIMENSION.getItem(), DIMENSION.getName(), TEXT, false, false));
    grid.addHeader(
        new GridHeader(DIMENSION_NAME.getItem(), DIMENSION_NAME.getName(), TEXT, false, false));
    grid.addHeader(new GridHeader(PERIOD.getItem(), PERIOD.getName(), TEXT, false, false));
    grid.addHeader(
        new GridHeader(PERIOD_NAME.getItem(), PERIOD_NAME.getName(), TEXT, false, false));
    grid.addHeader(new GridHeader(ORG_UNIT.getItem(), ORG_UNIT.getName(), TEXT, false, false));
    grid.addHeader(
        new GridHeader(ORG_UNIT_NAME.getItem(), ORG_UNIT_NAME.getName(), TEXT, false, false));
    grid.addHeader(
        new GridHeader(
            ORG_UNIT_NAME_HIERARCHY.getItem(),
            ORG_UNIT_NAME_HIERARCHY.getName(),
            TEXT,
            false,
            false));
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
    grid.addHeader(
        new GridHeader(zScoreOrModZScoreItem, zScoreOrModZScoreName, NUMBER, false, false));
    grid.addHeader(
        new GridHeader(LOWER_BOUNDARY.getItem(), LOWER_BOUNDARY.getName(), NUMBER, false, false));
    grid.addHeader(
        new GridHeader(UPPER_BOUNDARY.getItem(), UPPER_BOUNDARY.getName(), NUMBER, false, false));
  }

  /**
   * The method add the metadata into the response grid.
   *
   * @param grid the {@link Grid}
   * @param outliers the list of {@link Outlier}
   * @param request the {@link OutlierRequest}
   */
  private void setMetaData(Grid grid, List<Outlier> outliers, OutlierRequest request) {
    grid.addMetaData("algorithm", request.getAlgorithm());
    grid.addMetaData("threshold", request.getThreshold());
    grid.addMetaData("orderBy", request.getOrderBy());
    grid.addMetaData("maxResults", request.getMaxResults());
    grid.addMetaData("count", outliers.size());
  }

  /**
   * The method add the rows into the response grid.
   *
   * @param grid the {@link Grid}
   * @param outliers the list of {@link Outlier}
   * @param outlierRequest the {@link OutlierRequest}
   */
  private void setRows(Grid grid, List<Outlier> outliers, OutlierRequest outlierRequest) {
    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    outliers.forEach(
        outlier -> {
          boolean isModifiedZScore = outlierRequest.getAlgorithm() == MODIFIED_Z_SCORE;
          OrganisationUnit ou = organisationUnitService.getOrganisationUnit(outlier.getOu());
          Collection<OrganisationUnit> roots =
              currentUser != null ? currentUser.getOrganisationUnits() : null;

          grid.addRow();

          IdentifiableObject object = idObjectManager.get(DataElement.class, outlier.getDx());
          grid.addValue(getIdProperty(object, outlier.getDx(), outlierRequest.getOutputIdScheme()));
          grid.addValue(getIdProperty(object, outlier.getDx(), IdScheme.NAME));

          grid.addValue(outlier.getPe());
          grid.addValue(getPeriodName(outlierRequest, outlier));

          object = idObjectManager.get(OrganisationUnit.class, outlier.getOu());
          grid.addValue(getIdProperty(object, outlier.getOu(), outlierRequest.getOutputIdScheme()));
          grid.addValue(getIdProperty(object, outlier.getOu(), IdScheme.NAME));

          grid.addValue(ou.getParentNameGraph(roots, true, " / ", false));

          object = idObjectManager.get(CategoryOptionCombo.class, outlier.getCoc());
          grid.addValue(
              getIdProperty(object, outlier.getCoc(), outlierRequest.getOutputIdScheme()));
          grid.addValue(getIdProperty(object, outlier.getCoc(), IdScheme.NAME));

          object = idObjectManager.get(CategoryOptionCombo.class, outlier.getAoc());
          grid.addValue(
              getIdProperty(object, outlier.getAoc(), outlierRequest.getOutputIdScheme()));
          grid.addValue(getIdProperty(object, outlier.getAoc(), IdScheme.NAME));

          grid.addValue(outlier.getValue());
          grid.addValue(isModifiedZScore ? outlier.getMedian() : outlier.getMean());
          grid.addValue(outlier.getStdDev());
          grid.addValue(outlier.getAbsDev());
          grid.addValue(outlier.getZScore());
          grid.addValue(outlier.getLowerBound());
          grid.addValue(outlier.getUpperBound());
        });
  }

  /**
   * The method retrieves ID Property. Depend on the IdScheme parameter it could be ID, UID, UUID,
   * Code or Name. The default property is the UID.
   *
   * @param object the {@link IdentifiableObject}
   * @param uid the {@link String}, default UID of the identifiable object (data element,
   *     organisation unit, category option combo, etc...)
   * @param idScheme the {@link IdScheme}
   * @return ID Property of the identifiable object (ID, UID, UUID, Code or Name)
   */
  private String getIdProperty(IdentifiableObject object, String uid, IdScheme idScheme) {
    if (object == null || idScheme == IdScheme.UID || idScheme == IdScheme.UUID) {
      return uid;
    }
    if (idScheme.getIdentifiableProperty() == ID) {
      return Long.toString(object.getId());
    }
    if (idScheme.getIdentifiableProperty() == CODE) {
      return object.getCode();
    }

    return object.getName();
  }

  /**
   * The method retrieves period name if available. The default name is iso period date (for example
   * 202401).
   *
   * @param outlierRequest the {@link OutlierRequest}
   * @param outlier the {@link Outlier}
   * @return the period name based on iso date
   */
  private String getPeriodName(OutlierRequest outlierRequest, Outlier outlier) {
    Stream<PeriodDimension> periodStream =
        outlierRequest.hasPeriods()
            ? outlierRequest.getPeriods().stream()
                .filter(p -> outlier.getPe().equalsIgnoreCase(p.getIsoDate()))
            : dimensionalObjectProducer
                .getPeriodDimension(List.of(outlier.getPe()), null)
                .getItems()
                .stream()
                .map(p -> (PeriodDimension) p);

    return periodStream
        .min(AscendingPeriodComparator.INSTANCE)
        .map(PeriodDimension::getName)
        .orElse(outlier.getPe());
  }
}
