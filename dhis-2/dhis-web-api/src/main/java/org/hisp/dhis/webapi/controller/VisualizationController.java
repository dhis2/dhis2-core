/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensions;
import static org.hisp.dhis.feedback.ErrorCode.E4002;
import static org.hisp.dhis.visualization.OutlierAnalysis.MAX_RESULTS_MAX_VALUE;
import static org.hisp.dhis.visualization.OutlierAnalysis.MAX_RESULTS_MIN_VALUE;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dxf2.expressiondimensionitem.ExpressionDimensionItemService;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.legend.LegendSetService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.query.GetObjectParams;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.Visualization;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/visualizations")
@OpenApi.Document(classifiers = {"team:analytics", "purpose:metadata"})
public class VisualizationController
    extends AbstractCrudController<Visualization, GetObjectListParams> {
  private final LegendSetService legendSetService;

  private final DimensionService dimensionService;

  private final I18nManager i18nManager;

  private final ExpressionDimensionItemService expressionDimensionItemService;

  public VisualizationController(
      final LegendSetService legendSetService,
      DimensionService dimensionService,
      I18nManager i18nManager,
      ExpressionDimensionItemService expressionDimensionItemService) {
    this.legendSetService = legendSetService;
    this.dimensionService = dimensionService;
    this.i18nManager = i18nManager;
    this.expressionDimensionItemService = expressionDimensionItemService;
  }

  @Override
  protected Visualization deserializeJsonEntity(HttpServletRequest request) throws IOException {
    Visualization visualization = super.deserializeJsonEntity(request);

    validate(visualization);
    addDimensionsInto(visualization);

    return visualization;
  }

  private void validate(Visualization visualization) {
    if (visualization != null
        && visualization.getOutlierAnalysis() != null
        && !visualization.getOutlierAnalysis().isValid()) {
      throw new IllegalQueryException(
          E4002,
          "maxResults",
          MAX_RESULTS_MIN_VALUE,
          MAX_RESULTS_MAX_VALUE,
          visualization.getOutlierAnalysis().getMaxResults());
    }
  }

  private void addDimensionsInto(Visualization visualization) {
    if (visualization != null) {
      dimensionService.mergeAnalyticalObject(visualization);

      visualization.getColumnDimensions().clear();
      visualization.getRowDimensions().clear();
      visualization.getFilterDimensions().clear();

      visualization.getColumnDimensions().addAll(getDimensions(visualization.getColumns()));
      visualization.getRowDimensions().addAll(getDimensions(visualization.getRows()));
      visualization.getFilterDimensions().addAll(getDimensions(visualization.getFilters()));

      maybeLoadLegendSetInto(visualization);
    }
  }

  /**
   * Load the current/existing legendSet (if any is set) into the current visualization object, so
   * the relationship can be persisted.
   *
   * @param visualization
   */
  private void maybeLoadLegendSetInto(Visualization visualization) {
    if (visualization.getLegendDefinitions() != null
        && visualization.getLegendDefinitions().getLegendSet() != null) {
      visualization
          .getLegendDefinitions()
          .setLegendSet(
              legendSetService.getLegendSet(
                  visualization.getLegendDefinitions().getLegendSet().getUid()));
    }
  }

  @Override
  public void postProcessResponseEntities(
      List<Visualization> entityList, GetObjectListParams params) {
    if (CollectionUtils.isEmpty(entityList)) return;

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    Set<OrganisationUnit> organisationUnits =
        currentUser.getDataViewOrganisationUnitsWithFallback();
    I18nFormat i18nFormat = i18nManager.getI18nFormat();
    entityList.forEach(
        visualization -> {
          visualization.populateAnalyticalProperties();

          for (OrganisationUnit organisationUnit : visualization.getOrganisationUnits()) {
            visualization
                .getParentGraphMap()
                .put(organisationUnit.getUid(), organisationUnit.getParentGraph(organisationUnits));
          }

          if (isNotEmpty(visualization.getPeriods())) {
            for (Period period : visualization.getPeriods()) {
              period.setName(i18nFormat.formatPeriod(period));
            }
          }

          addExpressionDimensionItemElementsToDataDimensionItems(visualization);
        });
  }

  @Override
  public void postProcessResponseEntity(Visualization visualization, GetObjectParams params) {
    if (visualization != null) {
      visualization.populateAnalyticalProperties();

      User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
      Set<OrganisationUnit> organisationUnits =
          currentUser.getDataViewOrganisationUnitsWithFallback();

      for (OrganisationUnit organisationUnit : visualization.getOrganisationUnits()) {
        visualization
            .getParentGraphMap()
            .put(organisationUnit.getUid(), organisationUnit.getParentGraph(organisationUnits));
      }

      I18nFormat i18nFormat = i18nManager.getI18nFormat();

      if (isNotEmpty(visualization.getPeriods())) {
        for (Period period : visualization.getPeriods()) {
          period.setName(i18nFormat.formatPeriod(period));
        }
      }

      addExpressionDimensionItemElementsToDataDimensionItems(visualization);
    }
  }

  private void addExpressionDimensionItemElementsToDataDimensionItems(Visualization visualization) {
    List<DataDimensionItem> dataDimensionItems = new ArrayList<>();

    visualization.getDataDimensionItems().stream()
        .filter(ddi -> ddi.getExpressionDimensionItem() != null)
        .forEach(
            ddi -> {
              List<DimensionalItemObject> expressionItems =
                  expressionDimensionItemService.getExpressionItems(ddi);

              expressionItems.forEach(
                  ei -> {
                    DataDimensionItem dataDimensionItem = new DataDimensionItem();

                    switch (ei.getDimensionItemType()) {
                      case DATA_ELEMENT:
                        dataDimensionItem.setDataElement((DataElement) ei);
                        dataDimensionItems.add(dataDimensionItem);
                        break;
                      case DATA_ELEMENT_OPERAND:
                        dataDimensionItem.setDataElementOperand((DataElementOperand) ei);
                        dataDimensionItems.add(dataDimensionItem);
                        break;
                      default:
                        // ignore
                        break;
                    }
                  });
            });

    if (!dataDimensionItems.isEmpty()) {
      visualization.getDataDimensionItems().addAll(dataDimensionItems);
    }
  }
}
