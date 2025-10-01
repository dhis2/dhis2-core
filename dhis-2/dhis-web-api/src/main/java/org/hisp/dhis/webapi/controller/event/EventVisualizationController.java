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
package org.hisp.dhis.webapi.controller.event;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.common.DimensionType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.common.DimensionType.PROGRAM_DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionalObjectUtils.getQualifiedDimensions;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.hisp.dhis.common.ValueType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.cache.CacheStrategy.RESPECT_SYSTEM_SETTING;
import static org.hisp.dhis.commons.collection.ListUtils.union;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.eventvisualization.EventVisualizationType.LINE_LIST;
import static org.hisp.dhis.eventvisualization.EventVisualizationType.PIVOT_TABLE;
import static org.hisp.dhis.system.util.CodecUtils.filenameEncode;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_PNG;
import static org.hisp.dhis.webapi.utils.FilterUtils.fromFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.hisp.dhis.analytics.event.data.OrganisationUnitResolver;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.eventvisualization.EventVisualizationService;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.legend.LegendSetService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.query.GetObjectParams;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.ChartService;
import org.hisp.dhis.visualization.PlotData;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller responsible for providing the basic CRUD endpoints for the model EventVisualization.
 *
 * @author maikel arabori
 */
@Controller
@RequestMapping("/api/eventVisualizations")
@AllArgsConstructor
@OpenApi.Document(classifiers = {"team:tracker", "purpose:metadata"})
public class EventVisualizationController
    extends AbstractCrudController<EventVisualization, GetObjectListParams> {
  private final DimensionService dimensionService;

  private final LegendSetService legendSetService;

  private final OrganisationUnitService organisationUnitService;

  private final OrganisationUnitResolver organisationUnitResolver;

  private final EventVisualizationService eventVisualizationService;

  private final ProgramService programService;

  private final ChartService chartService;

  private final I18nManager i18nManager;

  private final ContextUtils contextUtils;

  @GetMapping(value = {"/{uid}/data", "/{uid}/data.png"})
  void generateChart(
      @PathVariable("uid") String uid,
      @RequestParam(value = "date", required = false) Date date,
      @RequestParam(value = "ou", required = false) String ou,
      @RequestParam(value = "width", defaultValue = "800", required = false) int width,
      @RequestParam(value = "height", defaultValue = "500", required = false) int height,
      @RequestParam(value = "attachment", required = false) boolean attachment,
      HttpServletResponse response)
      throws IOException, WebMessageException {
    EventVisualization eventVisualization = eventVisualizationService.getEventVisualization(uid);

    if (eventVisualization == null) {
      throw new WebMessageException(notFound("Event visualization does not exist: " + uid));
    }

    checkChartGenerationConditions(eventVisualization);

    OrganisationUnit unit = ou != null ? organisationUnitService.getOrganisationUnit(ou) : null;

    JFreeChart jFreeChart =
        chartService.getJFreeChart(
            new PlotData(eventVisualization), date, unit, i18nManager.getI18nFormat());

    String filename = filenameEncode(eventVisualization.getName()) + ".png";

    contextUtils.configureResponse(
        response, CONTENT_TYPE_PNG, RESPECT_SYSTEM_SETTING, filename, attachment);

    ChartUtilities.writeChartAsPNG(response.getOutputStream(), jFreeChart, width, height);
  }

  @Override
  protected EventVisualization deserializeJsonEntity(HttpServletRequest request)
      throws IOException {
    EventVisualization eventVisualization = super.deserializeJsonEntity(request);

    prepare(eventVisualization);

    return eventVisualization;
  }

  @Override
  protected EventVisualization deserializeXmlEntity(HttpServletRequest request) throws IOException {
    EventVisualization eventVisualization = super.deserializeXmlEntity(request);

    prepare(eventVisualization);

    return eventVisualization;
  }

  @Override
  protected void postProcessResponseEntity(
      EventVisualization eventVisualization, GetObjectParams params) {
    eventVisualization.populateAnalyticalProperties();

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    if (currentUser != null) {
      Set<OrganisationUnit> roots = currentUser.getDataViewOrganisationUnitsWithFallback();
      addOrganizationUnitsToGraphMap(eventVisualization, roots);
      addFilterOrganizationUnitsToResponse(
          union(
              eventVisualization.getColumns(),
              eventVisualization.getRows(),
              eventVisualization.getFilters()),
          eventVisualization,
          roots);
    }

    I18nFormat format = i18nManager.getI18nFormat();

    if (eventVisualization.getPeriods() != null && !eventVisualization.getPeriods().isEmpty()) {
      for (PeriodDimension period : eventVisualization.getPeriods()) {
        period.setName(format.formatPeriod(period.getPeriod()));
      }
    }

    List<String> programUidsInDimensions =
        Stream.of(
                getPrograms(eventVisualization.getColumns().stream()),
                getPrograms(eventVisualization.getRows().stream()),
                getPrograms(eventVisualization.getFilters().stream()))
            .flatMap(Function.identity())
            .map(Program::getUid)
            .distinct()
            .toList();

    eventVisualization.setProgramDimensions(
        new ArrayList<>(programService.getPrograms(programUidsInDimensions)));
  }

  private void addOrganizationUnitsToGraphMap(
      EventVisualization eventVisualization, Set<OrganisationUnit> roots) {
    for (OrganisationUnit ou : eventVisualization.getOrganisationUnits()) {
      eventVisualization.getParentGraphMap().put(ou.getUid(), ou.getParentGraph(roots));
    }
  }

  private void addFilterOrganizationUnitsToResponse(
      List<DimensionalObject> dimensionalObjects,
      EventVisualization eventVisualization,
      Set<OrganisationUnit> roots) {
    for (DimensionalObject dimensionalObject : dimensionalObjects) {
      if ((dimensionalObject.getDimensionType() == PROGRAM_ATTRIBUTE
              || dimensionalObject.getDimensionType() == PROGRAM_DATA_ELEMENT)
          && dimensionalObject.getValueType() == ORGANISATION_UNIT) {

        List<String> orgUnitUids = fromFilter(dimensionalObject.getFilter());

        processOrganisationUnitLevelsGroups(orgUnitUids, eventVisualization);
        processOrganisationUnits(orgUnitUids, eventVisualization, roots);
      }
    }
  }

  /** Common method to process organization units once UIDs are extracted. */
  private void processOrganisationUnits(
      List<String> orgUnitUids,
      EventVisualization eventVisualization,
      Set<OrganisationUnit> roots) {
    if (!orgUnitUids.isEmpty()) {
      List<OrganisationUnit> units = organisationUnitService.getOrganisationUnitsByUid(orgUnitUids);

      for (OrganisationUnit ou : units) {
        eventVisualization.getParentGraphMap().put(ou.getUid(), ou.getParentGraph(roots));
        eventVisualization
            .getMetaData()
            .put(ou.getUid(), new MetadataItem(ou.getDisplayName(), ou.getUid(), ou.getCode()));
      }
    }
  }

  /** Common method to process organization units once UIDs are extracted. */
  private void processOrganisationUnitLevelsGroups(
      List<String> levelGroupsuids, EventVisualization eventVisualization) {
    if (!levelGroupsuids.isEmpty()) {
      for (String levelGroupUid : levelGroupsuids) {
        DimensionalItemObject ou =
            organisationUnitResolver.loadOrgUnitDimensionalItem(levelGroupUid, UID);

        if (ou != null) {
          eventVisualization.getParentGraphMap().put(ou.getUid(), EMPTY);
          eventVisualization
              .getMetaData()
              .put(ou.getUid(), new MetadataItem(ou.getDisplayName(), ou.getUid(), ou.getCode()));
        }
      }
    }
  }

  private Stream<Program> getPrograms(Stream<DimensionalObject> dimensionalObjectStream) {
    return dimensionalObjectStream.map(DimensionalObject::getProgram).filter(Objects::nonNull);
  }

  @Override
  protected void preCreateEntity(EventVisualization newEventVisualization)
      throws ConflictException {
    /*
     * Once a legacy EventVisualization is CREATED through this new endpoint, it will automatically
     * become a non-legacy EventVisualization.
     */
    forceNonLegacy(newEventVisualization);
  }

  @Override
  protected void preUpdateEntity(
      EventVisualization eventVisualization, EventVisualization newEventVisualization)
      throws ConflictException {
    /*
     * Once a legacy EventVisualization is UPDATED through this new endpoint, it will automatically
     * become a non-legacy EventVisualization.
     */
    forceNonLegacy(newEventVisualization);
  }

  private void forceNonLegacy(EventVisualization eventVisualization) {
    if (eventVisualization != null && eventVisualization.isLegacy()) {
      eventVisualization.setLegacy(false);
    }
  }

  private void prepare(EventVisualization eventVisualization) {
    dimensionService.mergeAnalyticalObject(eventVisualization);
    dimensionService.mergeEventAnalyticalObject(eventVisualization);

    eventVisualization.getColumnDimensions().clear();
    eventVisualization.getRowDimensions().clear();
    eventVisualization.getFilterDimensions().clear();
    eventVisualization.getSimpleDimensions().clear();

    eventVisualization
        .getColumnDimensions()
        .addAll(getQualifiedDimensions(eventVisualization.getColumns()));
    eventVisualization
        .getRowDimensions()
        .addAll(getQualifiedDimensions(eventVisualization.getRows()));
    eventVisualization
        .getFilterDimensions()
        .addAll(getQualifiedDimensions(eventVisualization.getFilters()));
    eventVisualization.associateSimpleDimensions();

    maybeLoadLegendSetInto(eventVisualization);
  }

  /**
   * Load the current/existing legendSet (if any is set) into the current visualization object, so
   * the relationship can be persisted.
   */
  private void maybeLoadLegendSetInto(EventVisualization eventVisualization) {
    if (eventVisualization.getLegendDefinitions() != null
        && eventVisualization.getLegendDefinitions().getLegendSet() != null) {
      eventVisualization
          .getLegendDefinitions()
          .setLegendSet(
              legendSetService.getLegendSet(
                  eventVisualization.getLegendDefinitions().getLegendSet().getUid()));
    }
  }

  private void checkChartGenerationConditions(EventVisualization eventVisualization)
      throws WebMessageException {
    if (eventVisualization.getType() == PIVOT_TABLE || eventVisualization.getType() == LINE_LIST) {
      throw new WebMessageException(
          conflict("Cannot generate chart for " + eventVisualization.getType()));
    }

    if (eventVisualization.isMultiProgram()) {
      throw new WebMessageException(
          conflict(
              "Cannot generate chart for multi-program visualization "
                  + eventVisualization.getUid()));
    }
  }
}
