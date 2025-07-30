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
import static org.hisp.dhis.analytics.event.data.DefaultEventCoordinateService.COL_NAME_ENROLLMENT_GEOMETRY;
import static org.hisp.dhis.analytics.event.data.DefaultEventCoordinateService.COL_NAME_EVENT_GEOMETRY;
import static org.hisp.dhis.analytics.event.data.DefaultEventCoordinateService.COL_NAME_GEOMETRY_LIST;
import static org.hisp.dhis.analytics.event.data.DefaultEventCoordinateService.COL_NAME_TRACKED_ENTITY_GEOMETRY;
import static org.hisp.dhis.analytics.event.data.DefaultEventDataQueryService.SortableItems.isSortable;
import static org.hisp.dhis.analytics.event.data.DefaultEventDataQueryService.SortableItems.translateItemIfNecessary;
import static org.hisp.dhis.analytics.event.data.EnrollmentOrgUnitFilterHandler.handleEnrollmentOrgUnitFilter;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.illegalQueryExSupplier;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemsFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.OrgUnitField;
import org.hisp.dhis.analytics.common.ColumnHeader;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.QueryItemLocator;
import org.hisp.dhis.analytics.table.EnrollmentAnalyticsColumnName;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.GroupableItem;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.RequestTypeAware;
import org.hisp.dhis.common.UserOrgUnitType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
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

  private final ProgramService programService;

  private final ProgramStageService programStageService;

  private final DataElementService dataElementService;

  private final EventCoordinateService eventCoordinateService;

  private final QueryItemLocator queryItemLocator;

  private final TrackedEntityAttributeService attributeService;

  private final DataQueryService dataQueryService;

  @Override
  public EventQueryParams getFromRequest(EventDataQueryRequest request) {
    return getFromRequest(request, false);
  }

  @Override
  public EventQueryParams getFromRequest(EventDataQueryRequest request, boolean analyzeOnly) {
    EventQueryParams.Builder params = new EventQueryParams.Builder();

    IdScheme idScheme = IdScheme.UID;

    Locale locale = UserSettings.getCurrentSettings().getUserDbLocale();

    DataQueryParams dataQueryParams =
        DataQueryParams.newBuilder().withUserOrgUnitType(UserOrgUnitType.DATA_OUTPUT).build();

    List<OrganisationUnit> userOrgUnits =
        dataQueryService.getUserOrgUnits(dataQueryParams, request.getUserOrgUnit());

    Program pr = programService.getProgram(request.getProgram());

    if (pr == null) {
      throwIllegalQueryEx(ErrorCode.E7129, request.getProgram());
    }

    ProgramStage ps = programStageService.getProgramStage(request.getStage());

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

    if (hasPeriodDimension(eventQueryParams) && hasNotDefaultPeriod(eventQueryParams)) {
      builder.withSkipPartitioning(true);
      eventQueryParams = builder.build();
    }

    return eventQueryParams;
  }

  private boolean hasPeriodDimension(EventQueryParams eventQueryParams) {
    return Objects.nonNull(getPeriodDimension(eventQueryParams));
  }

  private boolean hasNotDefaultPeriod(EventQueryParams eventQueryParams) {
    return Optional.ofNullable(getPeriodDimension(eventQueryParams))
        .map(DimensionalObject::getItems)
        .orElse(List.of())
        .stream()
        .noneMatch(this::isDefaultPeriod);
  }

  private DimensionalObject getPeriodDimension(EventQueryParams eventQueryParams) {
    return eventQueryParams.getDimension(PERIOD_DIM_ID);
  }

  private boolean isDefaultPeriod(DimensionalItemObject dimensionalItemObject) {
    return ((Period) dimensionalItemObject).isDefault();
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
      for (Set<String> filterGroup : request.getFilter()) {
        UUID groupUUID = UUID.randomUUID();
        for (String dim : filterGroup) {
          String dimensionId = getDimensionFromParam(dim);

          List<String> items = getDimensionItemsFromParam(dim);

          GroupableItem groupableItem =
              dataQueryService.getDimension(
                  dimensionId,
                  items,
                  request.getRelativePeriodDate(),
                  userOrgUnits,
                  true,
                  null,
                  idScheme);

          if (groupableItem != null) {
            params.addFilter((DimensionalObject) groupableItem);
          } else {
            groupableItem = getQueryItem(dim, pr, request.getOutputType());
            params.addItemFilter((QueryItem) groupableItem);
          }

          groupableItem.setGroupUUID(groupUUID);
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
      for (Set<String> dimensionGroup : request.getDimension()) {
        UUID groupUUID = UUID.randomUUID();

        for (String dim : dimensionGroup) {
          String dimensionId = getDimensionFromParam(dim);

          List<String> items = getDimensionItemsFromParam(dim);

          GroupableItem groupableItem =
              dataQueryService.getDimension(
                  dimensionId, items, request, userOrgUnits, true, idScheme);

          if (groupableItem != null) {
            params.addDimension((DimensionalObject) groupableItem);
          } else {
            groupableItem = getQueryItem(dim, pr, request.getOutputType());
            params.addItem((QueryItem) groupableItem);
          }

          addStageToParams(params, request, groupableItem);

          groupableItem.setGroupUUID(groupUUID);
        }
      }
    }
  }

  /**
   * Adds the program stage associated with an item, if any. This happens only for enrollments
   * aggregate queries.
   *
   * @param paramsBuilder the {@link EventQueryParams.Builder}.
   * @param request the {@link EventDataQueryRequest}.
   * @param groupableItem the {@link GroupableItem}.
   */
  private void addStageToParams(
      EventQueryParams.Builder paramsBuilder,
      EventDataQueryRequest request,
      GroupableItem groupableItem) {
    if (handleEnrollmentOrgUnitFilter(request)) {
      if (groupableItem instanceof DimensionalObject dim) {
        paramsBuilder.withProgramStage(dim.getProgramStage());
      } else if (groupableItem instanceof QueryItem item) {
        paramsBuilder.withProgramStage(item.getProgramStage());
      }
    }
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
                object.getOutputType()));
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
                object.getOutputType()));
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
      String dimension, String filter, Program program, EventOutputType type) {
    if (filter != null) {
      dimension += DIMENSION_NAME_SEP + filter;
    }

    return getQueryItem(dimension, program, type);
  }

  @Override
  public QueryItem getQueryItem(String dimensionString, Program program, EventOutputType type) {
    String[] split = dimensionString.split(DIMENSION_NAME_SEP);

    if (split.length % 2 != 1) {
      throwIllegalQueryEx(ErrorCode.E7222, dimensionString);
    }

    QueryItem queryItem;
    if (Objects.isNull(program)) {
      // support for querying program attributes by uid without passing the program
      queryItem =
          queryItemLocator
              .getQueryItemForTrackedEntityAttribute(split[0])
              .orElseThrow(illegalQueryExSupplier(ErrorCode.E7224, dimensionString));
    } else {
      queryItem = queryItemLocator.getQueryItemFromDimension(split[0], program, type);
    }

    if (split.length > 1) // Filters specified
    {
      for (int i = 1; i < split.length; i += 2) {
        QueryOperator operator = QueryOperator.fromString(split[i]);
        QueryFilter filter = new QueryFilter(operator, split[i + 1]);
        // FE uses HH.MM time format instead of HH:MM. This is not
        // compatible with db table/cell values
        modifyFilterWhenTimeQueryItem(queryItem, filter);
        queryItem.addFilter(filter);
      }
    }

    return queryItem;
  }

  private static void modifyFilterWhenTimeQueryItem(QueryItem queryItem, QueryFilter filter) {
    if (queryItem.getItem() instanceof DataElement
        && ((DataElement) queryItem.getItem()).getValueType() == ValueType.TIME) {
      filter.setFilter(filter.getFilter().replace(".", ":"));
    }
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
    return getQueryItem(item, program, type);
  }

  private DimensionalItemObject getValueDimension(String value) {
    if (value == null) {
      return null;
    }

    DataElement de = dataElementService.getDataElement(value);

    if (de != null && de.isNumericType()) {
      return de;
    }

    TrackedEntityAttribute at = attributeService.getTrackedEntityAttribute(value);

    if (at != null && at.isNumericType()) {
      return at;
    }

    throw new IllegalQueryException(new ErrorMessage(ErrorCode.E7223, value));
  }

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
