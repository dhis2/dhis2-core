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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import static java.util.Collections.emptySet;
import static org.hisp.dhis.util.ObjectUtils.applyIfNotNull;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.parseFilters;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateDeprecatedParameter;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateDeprecatedUidsParameter;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateOrderParams;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateOrgUnitModeForEnrollmentsAndEvents;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.tracker.export.event.EventOperationParams.EventOperationParamsBuilder;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;
import org.springframework.stereotype.Component;

/**
 * Maps query parameters from {@link EventsExportController} stored in {@link EventRequestParams} to
 * {@link EventOperationParams} which is used to fetch events from the DB.
 */
@Component
@RequiredArgsConstructor
class EventRequestParamsMapper {
  private static final Set<String> ORDERABLE_FIELD_NAMES = EventMapper.ORDERABLE_FIELDS.keySet();

  private final EventFieldsParamMapper eventsMapper;

  public EventOperationParams map(EventRequestParams eventRequestParams)
      throws BadRequestException {
    OrganisationUnitSelectionMode orgUnitMode =
        validateDeprecatedParameter(
            "ouMode",
            eventRequestParams.getOuMode(),
            "orgUnitMode",
            eventRequestParams.getOrgUnitMode());

    orgUnitMode =
        validateOrgUnitModeForEnrollmentsAndEvents(
            eventRequestParams.getOrgUnit() != null
                ? Set.of(eventRequestParams.getOrgUnit())
                : emptySet(),
            orgUnitMode);

    EnrollmentStatus enrollmentStatus =
        validateDeprecatedParameter(
            "programStatus",
            eventRequestParams.getProgramStatus(),
            "enrollmentStatus",
            eventRequestParams.getEnrollmentStatus());

    UID attributeCategoryCombo =
        validateDeprecatedParameter(
            "attributeCc",
            eventRequestParams.getAttributeCc(),
            "attributeCategoryCombo",
            eventRequestParams.getAttributeCategoryCombo());

    Set<UID> attributeCategoryOptions =
        validateDeprecatedUidsParameter(
            "attributeCos",
            eventRequestParams.getAttributeCos(),
            "attributeCategoryOptions",
            eventRequestParams.getAttributeCategoryOptions());

    Set<UID> eventUids =
        validateDeprecatedUidsParameter(
            "event", eventRequestParams.getEvent(), "events", eventRequestParams.getEvents());

    validateFilter(eventRequestParams.getFilter(), eventUids);
    Map<UID, List<QueryFilter>> dataElementFilters = parseFilters(eventRequestParams.getFilter());
    Map<UID, List<QueryFilter>> attributeFilters =
        parseFilters(eventRequestParams.getFilterAttributes());

    Set<UID> assignedUsers =
        validateDeprecatedUidsParameter(
            "assignedUser",
            eventRequestParams.getAssignedUser(),
            "assignedUsers",
            eventRequestParams.getAssignedUsers());

    validateUpdateDurationParams(eventRequestParams);
    validateOrderParams(
        eventRequestParams.getOrder(), ORDERABLE_FIELD_NAMES, "data element and attribute");

    EventOperationParamsBuilder builder =
        EventOperationParams.builder()
            .program(eventRequestParams.getProgram())
            .programStage(eventRequestParams.getProgramStage())
            .orgUnit(eventRequestParams.getOrgUnit())
            .trackedEntity(eventRequestParams.getTrackedEntity())
            .enrollmentStatus(enrollmentStatus)
            .followUp(eventRequestParams.getFollowUp())
            .orgUnitMode(orgUnitMode)
            .assignedUserMode(eventRequestParams.getAssignedUserMode())
            .assignedUsers(assignedUsers)
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
            .attributeCategoryCombo(attributeCategoryCombo)
            .attributeCategoryOptions(attributeCategoryOptions)
            .idSchemes(eventRequestParams.getIdSchemes())
            .includeAttributes(false)
            .includeAllDataElements(false)
            .dataElementFilters(dataElementFilters)
            .attributeFilters(attributeFilters)
            .events(eventUids)
            .enrollments(eventRequestParams.getEnrollments())
            .includeDeleted(eventRequestParams.isIncludeDeleted())
            .eventParams(eventsMapper.map(eventRequestParams.getFields()));

    mapOrderParam(builder, eventRequestParams.getOrder());

    return builder.build();
  }

  private static void validateFilter(String filter, Set<UID> eventIds) throws BadRequestException {
    if (!CollectionUtils.isEmpty(eventIds) && !StringUtils.isEmpty(filter)) {
      throw new BadRequestException("Event UIDs and filters can not be specified at the same time");
    }
  }

  private void mapOrderParam(EventOperationParamsBuilder builder, List<OrderCriteria> orders) {
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

  private void validateUpdateDurationParams(EventRequestParams eventRequestParams)
      throws BadRequestException {
    if (eventRequestParams.getUpdatedWithin() != null
        && (eventRequestParams.getUpdatedAfter() != null
            || eventRequestParams.getUpdatedBefore() != null)) {
      throw new BadRequestException(
          "Last updated from and/or to and last updated duration cannot be specified simultaneously");
    }

    if (eventRequestParams.getUpdatedWithin() != null
        && DateUtils.getDuration(eventRequestParams.getUpdatedWithin()) == null) {
      throw new BadRequestException(
          "Duration is not valid: " + eventRequestParams.getUpdatedWithin());
    }
  }
}
