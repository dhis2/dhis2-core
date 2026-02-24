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
package org.hisp.dhis.analytics.event.data;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.hisp.dhis.analytics.event.data.DefaultEventCoordinateService.COL_NAME_ENROLLMENT_GEOMETRY;
import static org.hisp.dhis.analytics.event.data.DefaultEventCoordinateService.COL_NAME_EVENT_GEOMETRY;
import static org.hisp.dhis.analytics.event.data.DefaultEventCoordinateService.COL_NAME_GEOMETRY_LIST;
import static org.hisp.dhis.analytics.event.data.DefaultEventCoordinateService.COL_NAME_TRACKED_ENTITY_GEOMETRY;
import static org.hisp.dhis.analytics.event.data.DefaultEventDataQueryService.SortableItems.isSortable;
import static org.hisp.dhis.analytics.event.data.DefaultEventDataQueryService.SortableItems.translateItemIfNecessary;
import static org.hisp.dhis.analytics.event.data.EventPeriodUtils.hasDefaultPeriod;
import static org.hisp.dhis.analytics.event.data.EventPeriodUtils.hasPeriodDimension;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.illegalQueryExSupplier;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.DimensionConstants.DIMENSION_IDENTIFIER_SEP;
import static org.hisp.dhis.common.DimensionConstants.DIMENSION_NAME_SEP;
import static org.hisp.dhis.common.DimensionConstants.STATIC_DATE_DIMENSIONS;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemsFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;
import static org.hisp.dhis.common.EventDataQueryRequest.getStageInValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.OrgUnitField;
import org.hisp.dhis.analytics.common.ColumnHeader;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.QueryItemLocator;
import org.hisp.dhis.analytics.event.data.queryitem.QueryItemFilterHandlerRegistry;
import org.hisp.dhis.analytics.table.EnrollmentAnalyticsColumnName;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionConstants;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.GroupableItem;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RequestTypeAware;
import org.hisp.dhis.common.RequestTypeAware.EndpointAction;
import org.hisp.dhis.common.RequestTypeAware.EndpointItem;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author Lars Helge Overland
 */
@Service("org.hisp.dhis.analytics.event.EventDataQueryService")
@RequiredArgsConstructor
public class DefaultEventDataQueryService implements EventDataQueryService {
  private static final String ENROLLMENT_OU_DIMENSION = "ENROLLMENT_OU";
  private static final String LEVEL_PREFIX = "LEVEL-";

  private final ProgramService programService;

  private final ProgramStageService programStageService;

  private final DataElementService dataElementService;

  private final EventCoordinateService eventCoordinateService;

  private final QueryItemLocator queryItemLocator;

  private final TrackedEntityAttributeService attributeService;

  private final DataQueryService dataQueryService;

  private final OrganisationUnitService organisationUnitService;

  private final QueryItemFilterHandlerRegistry filterHandlerRegistry;

  @Override
  public EventQueryParams getFromRequest(EventDataQueryRequest request) {
    return getFromRequest(request, false);
  }

  @Override
  public EventQueryParams getFromRequest(EventDataQueryRequest request, boolean analyzeOnly) {
    EventQueryParams.Builder params = new EventQueryParams.Builder();

    IdScheme idScheme = IdScheme.UID;

    Locale locale = UserSettings.getCurrentSettings().getUserDbLocale();

    List<OrganisationUnit> userOrgUnits =
        dataQueryService.getUserOrgUnits(null, request.getUserOrgUnit());

    Program pr = programService.getProgram(request.getProgram());

    if (pr == null) {
      throwIllegalQueryEx(ErrorCode.E7129, request.getProgram());
    }

    ProgramStage ps =
        programStageService.getProgramStage(
            getStageInValue(request.getValue(), request.getStage()));

    if (StringUtils.isNotEmpty(request.getStage()) && ps == null) {
      throwIllegalQueryEx(ErrorCode.E7130, request.getStage());
    }

    List<String> coordinateFields = getCoordinateFields(request);

    addDimensionsToParams(params, request, userOrgUnits, pr, idScheme);

    addFiltersToParams(params, request, userOrgUnits, pr, idScheme);

    addSortToParams(params, request, pr);

    if (request.getAggregationType() != null) {
      params.withAggregationType(
          AnalyticsAggregationType.fromAggregationType(request.getAggregationType()));
    }

    EventQueryParams.Builder builder =
        params
            .withValue(getValueDimension(request.getValue()))
            .withRequestValue(request.getValue())
            .withSkipRounding(request.isSkipRounding())
            .withShowHierarchy(request.isShowHierarchy())
            .withSortOrder(request.getSortOrder())
            .withLimit(request.getLimit())
            .withOutputType(firstNonNull(request.getOutputType(), EventOutputType.EVENT))
            .withCollapseDataDimensions(request.isCollapseDataDimensions())
            .withAggregateData(request.isAggregateData())
            .withProgram(pr)
            .withProgramStage(ps)
            .withStartDate(request.getStartDate())
            .withEndDate(request.getEndDate())
            .withOrganisationUnitMode(request.getOuMode())
            .withSkipMeta(request.isSkipMeta())
            .withSkipData(request.isSkipData())
            .withCompletedOnly(request.isCompletedOnly())
            .withHierarchyMeta(request.isHierarchyMeta())
            .withCoordinatesOnly(request.isCoordinatesOnly())
            .withIncludeMetadataDetails(request.isIncludeMetadataDetails())
            .withDataIdScheme(request.getDataIdScheme())
            .withOutputIdScheme(request.getOutputIdScheme())
            .withEventStatuses(request.getEventStatus())
            .withDisplayProperty(request.getDisplayProperty())
            .withTimeField(request.getTimeField())
            .withOrgUnitField(new OrgUnitField(request.getOrgUnitField()))
            .withCoordinateFields(coordinateFields)
            .withHeaders(request.getHeaders())
            .withPage(request.getPage())
            .withPageSize(request.getPageSize())
            .withPaging(request.isPaging())
            .withTotalPages(request.isTotalPages())
            .withEnrollmentStatuses(request.getEnrollmentStatus())
            .withLocale(locale)
            .withEnhancedConditions(request.isEnhancedConditions())
            .withEndpointItem(request.getEndpointItem())
            .withEndpointAction(request.getEndpointAction())
            .withUserOrganisationUnitsCriteria(request.getUserOrganisationUnitCriteria())
            .withRowContext(request.isRowContext())
            .withUserOrgUnits(userOrgUnits);

    if (analyzeOnly) {
      builder = builder.withSkipData(true).withAnalyzeOrderId();
    }

    EventQueryParams eventQueryParams = builder.build();

    // Partitioning applies only when default period is specified

    // Empty period dimension means default period
    // Only applies for non-aggregate event queries
    if (hasPeriodDimension(eventQueryParams)
        && !hasDefaultPeriod(eventQueryParams)
        && eventQueryParams.isComingFromQuery()) {
      builder.withSkipPartitioning(true);
      eventQueryParams = builder.build();
    }

    return eventQueryParams;
  }

  @Override
  public EventQueryParams getFromAnalyticalObject(EventAnalyticalObject object) {
    Assert.notNull(object, "Event analytical object cannot be null");
    Assert.notNull(object.getProgram(), "Event analytical object must specify a program");

    EventQueryParams.Builder params = new EventQueryParams.Builder();

    IdScheme idScheme = IdScheme.UID;

    Date date = object.getRelativePeriodDate();

    Locale locale = UserSettings.getCurrentSettings().getUserDbLocale();

    object.populateAnalyticalProperties();

    for (DimensionalObject dimension : ListUtils.union(object.getColumns(), object.getRows())) {
      DimensionalObject dimObj =
          dataQueryService.getDimension(
              dimension.getDimension(),
              getDimensionalItemIds(dimension.getItems()),
              date,
              null,
              true,
              null,
              idScheme);

      if (dimObj != null) {
        params.addDimension(dimObj);
      } else {
        params.addItem(
            getQueryItem(
                dimension.getDimension(),
                dimension.getFilter(),
                object.getProgram(),
                object.getOutputType(),
                date));
      }
    }

    for (DimensionalObject filter : object.getFilters()) {
      DimensionalObject dimObj =
          dataQueryService.getDimension(
              filter.getDimension(),
              getDimensionalItemIds(filter.getItems()),
              date,
              null,
              true,
              null,
              idScheme);

      if (dimObj != null) {
        params.addFilter(dimObj);
      } else {
        params.addItemFilter(
            getQueryItem(
                filter.getDimension(),
                filter.getFilter(),
                object.getProgram(),
                object.getOutputType(),
                date));
      }
    }

    return params
        .withProgram(object.getProgram())
        .withProgramStage(object.getProgramStage())
        .withStartDate(object.getStartDate())
        .withEndDate(object.getEndDate())
        .withValue(object.getValue())
        .withOutputType(object.getOutputType())
        .withLocale(locale)
        .build();
  }

  /**
   * Returns list of coordinateFields.
   *
   * <p>All possible coordinate fields are collected. The order defines the priority of geometries
   * and is used as a parameters in SQL coalesce function.
   *
   * @param request the {@link EventDataQueryRequest}.
   * @return the coordinate column list.
   */
  @Override
  public List<String> getCoordinateFields(EventDataQueryRequest request) {
    final String program = request.getProgram();
    // TODO Remove when all web apps stop using old names of coordinate fields
    final String coordinateField = mapCoordinateField(request.getCoordinateField());
    final boolean defaultCoordinateFallback = request.isDefaultCoordinateFallback();
    final String fallbackCoordinateField = mapCoordinateField(request.getFallbackCoordinateField());

    List<String> coordinateFields = new ArrayList<>();

    if (coordinateField == null) {
      coordinateFields.add(StringUtils.EMPTY);
    } else if (COL_NAME_GEOMETRY_LIST.contains(coordinateField)) {
      coordinateFields.add(
          eventCoordinateService.validateCoordinateField(
              program, coordinateField, ErrorCode.E7221));
    } else if (EventQueryParams.EVENT_COORDINATE_FIELD.equals(coordinateField)) {
      coordinateFields.add(
          eventCoordinateService.validateCoordinateField(
              program, COL_NAME_EVENT_GEOMETRY, ErrorCode.E7221));
    } else if (EventQueryParams.ENROLLMENT_COORDINATE_FIELD.equals(coordinateField)) {
      coordinateFields.add(
          eventCoordinateService.validateCoordinateField(
              program, COL_NAME_ENROLLMENT_GEOMETRY, ErrorCode.E7221));
    } else if (EventQueryParams.TRACKER_COORDINATE_FIELD.equals(coordinateField)) {
      coordinateFields.add(
          eventCoordinateService.validateCoordinateField(
              program, COL_NAME_TRACKED_ENTITY_GEOMETRY, ErrorCode.E7221));
    }

    DataElement dataElement = dataElementService.getDataElement(coordinateField);

    if (dataElement != null) {
      coordinateFields.add(
          eventCoordinateService.validateCoordinateField(
              dataElement.getValueType(), coordinateField, ErrorCode.E7219));
    }

    TrackedEntityAttribute attribute = attributeService.getTrackedEntityAttribute(coordinateField);

    if (attribute != null) {
      coordinateFields.add(
          eventCoordinateService.validateCoordinateField(
              attribute.getValueType(), coordinateField, ErrorCode.E7220));
    }

    if (coordinateFields.isEmpty()) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E7221, coordinateField));
    }

    coordinateFields.remove(StringUtils.EMPTY);

    coordinateFields.addAll(
        eventCoordinateService.getFallbackCoordinateFields(
            program, fallbackCoordinateField, defaultCoordinateFallback));

    return coordinateFields.stream().distinct().collect(Collectors.toList());
  }

  @Override
  public QueryItem getQueryItem(String dimensionString, Program program, EventOutputType type) {
    return getQueryItem(dimensionString, program, type, null);
  }

  private void addSortToParams(
      EventQueryParams.Builder params, EventDataQueryRequest request, Program pr) {
    if (request.getAsc() != null) {
      for (String sort : request.getAsc()) {
        params.addAscSortItem(
            getSortItem(sort, pr, request.getOutputType(), request.getEndpointItem()));
      }
    }

    if (request.getDesc() != null) {
      for (String sort : request.getDesc()) {
        params.addDescSortItem(
            getSortItem(sort, pr, request.getOutputType(), request.getEndpointItem()));
      }
    }
  }

  private void addFiltersToParams(
      EventQueryParams.Builder params,
      EventDataQueryRequest request,
      List<OrganisationUnit> userOrgUnits,
      Program pr,
      IdScheme idScheme) {
    if (request.getFilter() != null) {
      for (NormalizedDimensionInput input :
          normalizeDimensionInputs(request.getFilter(), request)) {
        if (ENROLLMENT_OU_DIMENSION.equals(input.dimensionId())) {
          resolveEnrollmentOuFilter(params, request, userOrgUnits, input.items(), idScheme);
          continue;
        }

        GroupableItem groupableItem =
            dataQueryService.getDimension(
                input.dimensionId(),
                input.items(),
                request.getRelativePeriodDate(),
                userOrgUnits,
                true,
                null,
                idScheme);

        if (groupableItem != null) {
          groupableItem.setGroupUUID(input.groupUUID());
          params.addFilter((DimensionalObject) groupableItem);
        } else {
          groupableItem =
              getQueryItem(
                  input.rawDimension(),
                  pr,
                  request.getOutputType(),
                  request.getRelativePeriodDate());
          params.addItemFilter((QueryItem) groupableItem);
          groupableItem.setGroupUUID(input.groupUUID());
        }
      }
    }
  }

  private void addDimensionsToParams(
      EventQueryParams.Builder params,
      EventDataQueryRequest request,
      List<OrganisationUnit> userOrgUnits,
      Program pr,
      IdScheme idScheme) {
    if (request.getDimension() != null) {
      for (NormalizedDimensionInput input :
          normalizeDimensionInputs(request.getDimension(), request)) {
        if (ENROLLMENT_OU_DIMENSION.equals(input.dimensionId())) {
          resolveEnrollmentOuDimension(params, request, userOrgUnits, input.items(), idScheme);
          continue;
        }

        GroupableItem groupableItem =
            dataQueryService.getDimension(
                input.dimensionId(), input.items(), request, userOrgUnits, true, idScheme);

        if (groupableItem != null) {
          groupableItem.setGroupUUID(input.groupUUID());
          params.addDimension((DimensionalObject) groupableItem);
        } else {
          groupableItem =
              getQueryItem(
                  input.rawDimension(),
                  pr,
                  request.getOutputType(),
                  request.getRelativePeriodDate());
          params.addItem((QueryItem) groupableItem);
          groupableItem.setGroupUUID(input.groupUUID());
        }
      }
    }
  }

  /**
   * Helper class to track and merge period (pe) dimensions. State management for merging `pe`
   * parameters is isolated here to reduce cognitive complexity.
   */
  private static class PeriodDimensionTracker {
    private final List<String> mergedItems = new ArrayList<>();
    private int firstIndex = -1;
    private UUID firstGroupUUID = null;

    /**
     * Tracks a period dimension item for later merging.
     *
     * @param currentIndex the current index in the normalized inputs list
     * @param groupUUID the group UUID associated with the item
     * @param items the list of items to track
     */
    public void track(int currentIndex, UUID groupUUID, List<String> items) {
      if (firstIndex < 0) {
        firstIndex = currentIndex;
        firstGroupUUID = groupUUID;
      }
      mergedItems.addAll(items);
    }

    /**
     * Inserts the merged period items into the normalized inputs list at the appropriate position.
     *
     * @param inputs the list of normalized inputs
     */
    private void insertMergedInto(List<NormalizedDimensionInput> inputs) {
      if (mergedItems.isEmpty()) {
        return;
      }
      List<String> distinctItems = mergedItems.stream().distinct().toList();
      String mergedRawDimension = "pe:" + String.join(";", distinctItems);

      NormalizedDimensionInput mergedInput =
          new NormalizedDimensionInput(mergedRawDimension, "pe", distinctItems, firstGroupUUID);

      if (firstIndex >= 0 && firstIndex <= inputs.size()) {
        inputs.add(firstIndex, mergedInput);
      } else {
        inputs.add(mergedInput);
      }
    }
  }

  /**
   * Processes a single dimension and adds it to the list of normalized inputs or tracks it if it's
   * a period dimension.
   *
   * @param rawDimension the raw dimension string
   * @param groupUUID the group UUID
   * @param request the original data query request
   * @param normalizedInputs the list of normalized inputs to populate
   * @param peTracker the period dimension tracker
   */
  private void processDimension(
      String rawDimension,
      UUID groupUUID,
      EventDataQueryRequest request,
      List<NormalizedDimensionInput> normalizedInputs,
      PeriodDimensionTracker peTracker) {

    String dimensionId = getDimensionFromParam(rawDimension);
    List<String> items = getDimensionItemsFromParam(rawDimension);

    if (ENROLLMENT_OU_DIMENSION.equals(dimensionId)) {
      normalizedInputs.add(
          new NormalizedDimensionInput(rawDimension, dimensionId, items, groupUUID));
      return;
    }

    validateStaticDateDimensionSupport(dimensionId, rawDimension, request);
    DimensionAndItems normalized = normalizeStaticDateDimension(dimensionId, items);

    if ("pe".equals(normalized.dimension())) {
      peTracker.track(normalizedInputs.size(), groupUUID, normalized.items());
      return;
    }

    normalizedInputs.add(
        new NormalizedDimensionInput(
            rawDimension, normalized.dimension(), normalized.items(), groupUUID));
  }

  /**
   * Normalizes the dimension inputs by separating and processing them, merging period dimensions as
   * needed.
   *
   * @param requestDimensions the raw request dimensions
   * @param request the original data query request
   * @return the list of normalized dimension inputs
   */
  private List<NormalizedDimensionInput> normalizeDimensionInputs(
      Set<Set<String>> requestDimensions, EventDataQueryRequest request) {

    List<NormalizedDimensionInput> normalizedInputs = new ArrayList<>();
    PeriodDimensionTracker peTracker = new PeriodDimensionTracker();

    for (Set<String> dimensionGroup : requestDimensions) {
      UUID groupUUID = UUID.randomUUID();

      for (String rawDimension : dimensionGroup) {
        processDimension(rawDimension, groupUUID, request, normalizedInputs, peTracker);
      }
    }

    peTracker.insertMergedInto(normalizedInputs);
    return normalizedInputs;
  }

  private record NormalizedDimensionInput(
      String rawDimension, String dimensionId, List<String> items, UUID groupUUID) {}

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  // TODO!!! remove when all fe apps stop using old names of the coordinate fields
  /**
   * Temporary only, should not be in 2.42 release!!! Retrieves an old name of the coordinate field.
   *
   * @param coordinateField a name of the coordinate field
   * @return old name of the coordinate field.
   */
  private String mapCoordinateField(String coordinateField) {
    if ("pigeometry".equalsIgnoreCase(coordinateField)) {
      return COL_NAME_ENROLLMENT_GEOMETRY;
    }

    if ("psigeometry".equalsIgnoreCase(coordinateField)) {
      return COL_NAME_EVENT_GEOMETRY;
    }

    if ("teigeometry".equalsIgnoreCase(coordinateField)) {
      return COL_NAME_TRACKED_ENTITY_GEOMETRY;
    }

    return coordinateField;
  }

  private QueryItem getQueryItem(
      String dimension,
      String filter,
      Program program,
      EventOutputType type,
      Date relativePeriodDate) {
    if (filter != null) {
      dimension += DIMENSION_NAME_SEP + filter;
    }

    return getQueryItem(dimension, program, type, relativePeriodDate);
  }

  private QueryItem getQueryItem(
      String dimensionString, Program program, EventOutputType type, Date relativePeriodDate) {
    String[] split = dimensionString.split(DIMENSION_NAME_SEP);
    QueryItem queryItem = resolveQueryItem(split[0], dimensionString, program, type);

    if (split.length > 1) {
      filterHandlerRegistry
          .handlerFor(queryItem)
          .applyFilters(queryItem, split, dimensionString, relativePeriodDate);
    }

    return queryItem;
  }

  private QueryItem resolveQueryItem(
      String itemId, String dimensionString, Program program, EventOutputType type) {
    if (Objects.isNull(program)) {
      // support for querying program attributes by uid without passing the program
      return queryItemLocator
          .getQueryItemForTrackedEntityAttribute(itemId)
          .orElseThrow(illegalQueryExSupplier(ErrorCode.E7224, dimensionString));
    }
    return queryItemLocator.getQueryItemFromDimension(itemId, program, type);
  }

  private QueryItem getSortItem(
      String item,
      Program program,
      EventOutputType type,
      RequestTypeAware.EndpointItem endpointItem) {
    if (isSortable(item)) {
      return new QueryItem(
          new BaseDimensionalItemObject(translateItemIfNecessary(item, endpointItem)));
    }
    return getQueryItem(item, program, type, null);
  }

  private DimensionalItemObject getValueDimension(String value) {
    if (value == null) {
      return null;
    }

    String dimValue = defaultIfBlank(substringAfter(value, DIMENSION_IDENTIFIER_SEP), value);

    DataElement de = dataElementService.getDataElement(dimValue);

    if (de != null && de.isNumericType()) {
      return de;
    }

    TrackedEntityAttribute at = attributeService.getTrackedEntityAttribute(dimValue);

    if (at != null && at.isNumericType()) {
      return at;
    }

    throw new IllegalQueryException(new ErrorMessage(ErrorCode.E7223, value));
  }

  private static final Set<String> DATE_COMPARISON_OPERATORS =
      Set.of("GT", "GE", "LT", "LE", "EQ", "NE");

  private DimensionAndItems normalizeStaticDateDimension(String dimensionId, List<String> items) {
    if (dimensionId == null || items == null || items.isEmpty()) {
      return new DimensionAndItems(dimensionId, items);
    }

    if (!STATIC_DATE_DIMENSIONS.contains(dimensionId)) {
      return new DimensionAndItems(dimensionId, items);
    }

    // Operator-based items (e.g., GT:2023-01-01) bypass period normalization
    // and are handled by DateFilterHandler via the QueryItem path
    if (hasDateOperatorPrefix(items)) {
      return new DimensionAndItems(dimensionId, items);
    }

    List<String> periodItems =
        items.stream().map(item -> item + DIMENSION_NAME_SEP + dimensionId).distinct().toList();

    return new DimensionAndItems("pe", periodItems);
  }

  private static boolean hasDateOperatorPrefix(List<String> items) {
    String first = items.get(0);
    int colonIndex = first.indexOf(':');
    return colonIndex > 0 && DATE_COMPARISON_OPERATORS.contains(first.substring(0, colonIndex));
  }

  private void validateStaticDateDimensionSupport(
      String dimensionId, String dimensionString, EventDataQueryRequest request) {
    if (!DimensionConstants.CREATED.equals(dimensionId)) {
      return;
    }

    if (!isEventAggregateRequest(request)) {
      throwIllegalQueryEx(ErrorCode.E7222, dimensionString);
    }
  }

  private boolean isEventAggregateRequest(EventDataQueryRequest request) {
    return EndpointAction.AGGREGATE.equals(request.getEndpointAction())
        && EndpointItem.EVENT.equals(request.getEndpointItem());
  }

  /**
   * Resolves ENROLLMENT_OU items as org units and stores them as enrollment OU dimension items.
   * Reuses the standard OU resolution infrastructure by passing "ou" to getDimension().
   */
  private void resolveEnrollmentOuDimension(
      EventQueryParams.Builder params,
      EventDataQueryRequest request,
      List<OrganisationUnit> userOrgUnits,
      List<String> items,
      IdScheme idScheme) {
    EnrollmentOuResolution resolution =
        resolveEnrollmentOuItems(items, request, userOrgUnits, idScheme, true);

    if (!resolution.uidItems().isEmpty()) {
      params.withEnrollmentOuDimension(resolution.uidItems());
    }

    params.withEnrollmentOuDimensionLevels(resolution.levels());
  }

  /**
   * Resolves ENROLLMENT_OU items as org units and stores them as enrollment OU filter items. Reuses
   * the standard OU resolution infrastructure by passing "ou" to getDimension().
   */
  private void resolveEnrollmentOuFilter(
      EventQueryParams.Builder params,
      EventDataQueryRequest request,
      List<OrganisationUnit> userOrgUnits,
      List<String> items,
      IdScheme idScheme) {
    EnrollmentOuResolution resolution =
        resolveEnrollmentOuItems(items, request, userOrgUnits, idScheme, false);

    if (!resolution.uidItems().isEmpty()) {
      params.withEnrollmentOuFilter(resolution.uidItems());
    }

    params.withEnrollmentOuFilterLevels(resolution.levels());
  }

  private EnrollmentOuResolution resolveEnrollmentOuItems(
      List<String> items,
      EventDataQueryRequest request,
      List<OrganisationUnit> userOrgUnits,
      IdScheme idScheme,
      boolean fromDimension) {
    List<String> nonLevelItems = new ArrayList<>();
    Set<Integer> levels = new java.util.LinkedHashSet<>();

    for (String item : items) {
      if (item != null && item.startsWith(LEVEL_PREFIX)) {
        String levelId = substringAfter(item, LEVEL_PREFIX);
        Integer level = organisationUnitService.getOrganisationUnitLevelByLevelOrUid(levelId);
        if (level != null) {
          levels.add(level);
        }
      } else {
        nonLevelItems.add(item);
      }
    }

    List<DimensionalItemObject> uidItems = new ArrayList<>();

    if (!nonLevelItems.isEmpty()) {
      GroupableItem ouDimension =
          fromDimension
              ? dataQueryService.getDimension(
                  "ou", nonLevelItems, request, userOrgUnits, true, idScheme)
              : dataQueryService.getDimension(
                  "ou",
                  nonLevelItems,
                  request.getRelativePeriodDate(),
                  userOrgUnits,
                  true,
                  null,
                  idScheme);
      if (ouDimension != null) {
        uidItems.addAll(((DimensionalObject) ouDimension).getItems());
      }
    }

    if (uidItems.isEmpty() && levels.isEmpty()) {
      throwIllegalQueryEx(ErrorCode.E7143, ENROLLMENT_OU_DIMENSION);
    }

    return new EnrollmentOuResolution(uidItems, levels);
  }

  private record EnrollmentOuResolution(
      List<DimensionalItemObject> uidItems, Set<Integer> levels) {}

  private record DimensionAndItems(String dimension, List<String> items) {}

  @Getter
  @RequiredArgsConstructor
  enum SortableItems {
    ENROLLMENT_DATE(
        ColumnHeader.ENROLLMENT_DATE.getItem(),
        EventAnalyticsColumnName.ENROLLMENT_DATE_COLUMN_NAME,
        EnrollmentAnalyticsColumnName.ENROLLMENT_DATE_COLUMN_NAME),
    INCIDENT_DATE(
        ColumnHeader.INCIDENT_DATE.getItem(),
        EventAnalyticsColumnName.ENROLLMENT_OCCURRED_DATE_COLUMN_NAME,
        EnrollmentAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME),
    EVENT_DATE(
        ColumnHeader.EVENT_DATE.getItem(), EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME),
    SCHEDULED_DATE(
        ColumnHeader.SCHEDULED_DATE.getItem(), EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME),
    ORG_UNIT_NAME(ColumnHeader.ORG_UNIT_NAME.getItem()),
    ORG_UNIT_NAME_HIERARCHY(ColumnHeader.ORG_UNIT_NAME_HIERARCHY.getItem()),
    ORG_UNIT_CODE(ColumnHeader.ORG_UNIT_CODE.getItem()),
    PROGRAM_STATUS(
        ColumnHeader.PROGRAM_STATUS.getItem(),
        EventAnalyticsColumnName.ENROLLMENT_STATUS_COLUMN_NAME,
        EnrollmentAnalyticsColumnName.ENROLLMENT_STATUS_COLUMN_NAME),
    EVENT_STATUS(
        ColumnHeader.EVENT_STATUS.getItem(), EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME),
    CREATED_BY_DISPLAY_NAME(ColumnHeader.CREATED_BY_DISPLAY_NAME.getItem()),
    LAST_UPDATED_BY_DISPLAY_NAME(ColumnHeader.LAST_UPDATED_BY_DISPLAY_NAME.getItem()),
    LAST_UPDATED(ColumnHeader.LAST_UPDATED.getItem());

    private final String itemName;

    private final String eventColumnName;

    private final String enrollmentColumnName;

    SortableItems(String itemName) {
      this.itemName = itemName;
      this.eventColumnName = null;
      this.enrollmentColumnName = null;
    }

    SortableItems(String itemName, String columnName) {
      this.itemName = itemName;
      this.eventColumnName = columnName;
      this.enrollmentColumnName = columnName;
    }

    static boolean isSortable(String itemName) {
      return Arrays.stream(values()).map(SortableItems::getItemName).anyMatch(itemName::equals);
    }

    static String translateItemIfNecessary(String item, RequestTypeAware.EndpointItem type) {
      return Arrays.stream(values())
          .filter(sortableItems -> sortableItems.getItemName().equals(item))
          .findFirst()
          .map(sortableItems -> sortableItems.getColumnName(type))
          .orElse(item);
    }

    private String getColumnName(RequestTypeAware.EndpointItem type) {
      return type == RequestTypeAware.EndpointItem.EVENT ? eventColumnName : enrollmentColumnName;
    }
  }
}
