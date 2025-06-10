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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import static java.util.Collections.emptySet;
import static org.hisp.dhis.util.ObjectUtils.applyIfNotNull;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateDeprecatedParameter;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateMandatoryProgram;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateOrderParams;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateOrgUnitModeForEnrollmentsAndEvents;
import static org.hisp.dhis.webapi.controller.tracker.export.FilterParser.parseFilters;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventFields;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventOperationParams;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventOperationParams.TrackerEventOperationParamsBuilder;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;
import org.springframework.stereotype.Component;

/**
 * Maps query parameters from {@link EventsExportController} stored in {@link EventRequestParams} to
 * {@link TrackerEventOperationParams} which is used to fetch events from the DB.
 */
@Component
@RequiredArgsConstructor
public class TrackerEventRequestParamsMapper {
  private static final Set<String> ORDERABLE_FIELD_NAMES = EventMapper.ORDERABLE_FIELDS.keySet();

  private final FieldFilterService fieldFilterService;

  public TrackerEventOperationParams map(
      EventRequestParams eventRequestParams, TrackerIdSchemeParams idSchemeParams)
      throws BadRequestException {
    validateMandatoryProgram(eventRequestParams.getProgram());
    OrganisationUnitSelectionMode orgUnitMode =
        validateOrgUnitModeForEnrollmentsAndEvents(
            eventRequestParams.getOrgUnit() != null
                ? Set.of(eventRequestParams.getOrgUnit())
                : emptySet(),
            eventRequestParams.getOrgUnitMode());

    EnrollmentStatus enrollmentStatus =
        validateDeprecatedParameter(
            "programStatus",
            eventRequestParams.getProgramStatus(),
            "enrollmentStatus",
            eventRequestParams.getEnrollmentStatus());

    validateFilter(eventRequestParams.getFilter(), eventRequestParams.getEvents());
    Map<UID, List<QueryFilter>> dataElementFilters =
        parseFilters("filter", eventRequestParams.getFilter());
    Map<UID, List<QueryFilter>> attributeFilters =
        parseFilters("filterAttributes", eventRequestParams.getFilterAttributes());

    validateUpdateDurationParams(eventRequestParams);
    validateOrderParams(
        eventRequestParams.getOrder(), ORDERABLE_FIELD_NAMES, "data element and attribute");

    TrackerEventOperationParamsBuilder builder =
        TrackerEventOperationParams.builder()
            .program(eventRequestParams.getProgram())
            .programStage(eventRequestParams.getProgramStage())
            .orgUnit(eventRequestParams.getOrgUnit())
            .trackedEntity(eventRequestParams.getTrackedEntity())
            .enrollmentStatus(enrollmentStatus)
            .followUp(eventRequestParams.getFollowUp())
            .orgUnitMode(orgUnitMode)
            .assignedUserMode(eventRequestParams.getAssignedUserMode())
            .assignedUsers(eventRequestParams.getAssignedUsers())
            .occurredAfter(
                applyIfNotNull(eventRequestParams.getOccurredAfter(), StartDateTime::toDate))
            .occurredBefore(
                applyIfNotNull(eventRequestParams.getOccurredBefore(), EndDateTime::toDate))
            .scheduledAfter(
                applyIfNotNull(eventRequestParams.getScheduledAfter(), StartDateTime::toDate))
            .scheduledBefore(
                applyIfNotNull(eventRequestParams.getScheduledBefore(), EndDateTime::toDate))
            .updatedAfter(
                applyIfNotNull(eventRequestParams.getUpdatedAfter(), StartDateTime::toDate))
            .updatedBefore(
                applyIfNotNull(eventRequestParams.getUpdatedBefore(), EndDateTime::toDate))
            .updatedWithin(eventRequestParams.getUpdatedWithin())
            .enrollmentEnrolledBefore(
                applyIfNotNull(
                    eventRequestParams.getEnrollmentEnrolledBefore(), EndDateTime::toDate))
            .enrollmentEnrolledAfter(
                applyIfNotNull(
                    eventRequestParams.getEnrollmentEnrolledAfter(), StartDateTime::toDate))
            .enrollmentOccurredBefore(
                applyIfNotNull(
                    eventRequestParams.getEnrollmentOccurredBefore(), EndDateTime::toDate))
            .enrollmentOccurredAfter(
                applyIfNotNull(
                    eventRequestParams.getEnrollmentOccurredAfter(), StartDateTime::toDate))
            .eventStatus(eventRequestParams.getStatus())
            .attributeCategoryCombo(eventRequestParams.getAttributeCategoryCombo())
            .attributeCategoryOptions(eventRequestParams.getAttributeCategoryOptions())
            .events(eventRequestParams.getEvents())
            .enrollments(eventRequestParams.getEnrollments())
            .includeDeleted(eventRequestParams.isIncludeDeleted())
            .fields(
                TrackerEventFields.of(
                    f ->
                        fieldFilterService.filterIncludes(
                            Event.class, eventRequestParams.getFields(), f),
                    FieldPath.FIELD_PATH_SEPARATOR))
            .idSchemeParams(idSchemeParams);

    mapOrderParam(builder, eventRequestParams.getOrder());
    mapDataElementFilterParam(builder, dataElementFilters);
    mapAttributeFilterParam(builder, attributeFilters);

    return builder.build();
  }

  private static void validateFilter(String filter, Set<UID> eventIds) throws BadRequestException {
    if (!CollectionUtils.isEmpty(eventIds) && !StringUtils.isEmpty(filter)) {
      throw new BadRequestException("Event UIDs and filters can not be specified at the same time");
    }
  }

  private void mapOrderParam(
      TrackerEventOperationParamsBuilder builder, List<OrderCriteria> orders) {
    if (orders == null || orders.isEmpty()) {
      return;
    }

    for (OrderCriteria order : orders) {
      if (EventMapper.ORDERABLE_FIELDS.containsKey(order.getField())) {
        builder.orderBy(EventMapper.ORDERABLE_FIELDS.get(order.getField()), order.getDirection());
      } else {
        builder.orderBy(UID.of(order.getField()), order.getDirection());
      }
    }
  }

  private void mapDataElementFilterParam(
      TrackerEventOperationParamsBuilder builder, Map<UID, List<QueryFilter>> dataElementFilters) {
    if (dataElementFilters == null || dataElementFilters.isEmpty()) {
      return;
    }

    for (Entry<UID, List<QueryFilter>> entry : dataElementFilters.entrySet()) {
      if (entry.getValue().isEmpty()) {
        builder.filterByDataElement(entry.getKey());
      } else {
        builder.filterByDataElement(entry.getKey(), entry.getValue());
      }
    }
  }

  private void mapAttributeFilterParam(
      TrackerEventOperationParamsBuilder builder, Map<UID, List<QueryFilter>> attributeFilters) {
    if (attributeFilters == null || attributeFilters.isEmpty()) {
      return;
    }

    for (Entry<UID, List<QueryFilter>> entry : attributeFilters.entrySet()) {
      if (entry.getValue().isEmpty()) {
        builder.filterByAttribute(entry.getKey());
      } else {
        builder.filterByAttribute(entry.getKey(), entry.getValue());
      }
    }
  }

  private void validateUpdateDurationParams(EventRequestParams eventRequestParams)
      throws BadRequestException {
    if (eventRequestParams.getUpdatedWithin() != null
        && (eventRequestParams.getUpdatedAfter() != null
            || eventRequestParams.getUpdatedBefore() != null)) {
      throw new BadRequestException(
          "Last updated from and/or to and last updated duration cannot be specified"
              + " simultaneously");
    }

    if (eventRequestParams.getUpdatedWithin() != null
        && DateUtils.getDuration(eventRequestParams.getUpdatedWithin()) == null) {
      throw new BadRequestException(
          "Duration is not valid: " + eventRequestParams.getUpdatedWithin());
    }
  }
}
