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
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.tracker.export.event.EventOperationParams.EventOperationParamsBuilder;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
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
    Map<String, List<QueryFilter>> dataElementFilters =
        parseFilters(eventRequestParams.getFilter());
    Map<String, List<QueryFilter>> attributeFilters =
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
            .programUid(
                eventRequestParams.getProgram() != null
                    ? eventRequestParams.getProgram().getValue()
                    : null)
            .programStageUid(
                eventRequestParams.getProgramStage() != null
                    ? eventRequestParams.getProgramStage().getValue()
                    : null)
            .orgUnitUid(
                eventRequestParams.getOrgUnit() != null
                    ? eventRequestParams.getOrgUnit().getValue()
                    : null)
            .trackedEntityUid(
                eventRequestParams.getTrackedEntity() != null
                    ? eventRequestParams.getTrackedEntity().getValue()
                    : null)
            .programStatus(eventRequestParams.getProgramStatus())
            .followUp(eventRequestParams.getFollowUp())
            .orgUnitMode(orgUnitMode)
            .assignedUserMode(eventRequestParams.getAssignedUserMode())
            .assignedUsers(UID.toValueSet(assignedUsers))
            .occurredAfter(eventRequestParams.getOccurredAfter())
            .occurredBefore(eventRequestParams.getOccurredBefore())
            .scheduledAfter(eventRequestParams.getScheduledAfter())
            .scheduledBefore(eventRequestParams.getScheduledBefore())
            .updatedAfter(eventRequestParams.getUpdatedAfter())
            .updatedBefore(eventRequestParams.getUpdatedBefore())
            .updatedWithin(eventRequestParams.getUpdatedWithin())
            .enrollmentEnrolledBefore(eventRequestParams.getEnrollmentEnrolledBefore())
            .enrollmentEnrolledAfter(eventRequestParams.getEnrollmentEnrolledAfter())
            .enrollmentOccurredBefore(eventRequestParams.getEnrollmentOccurredBefore())
            .enrollmentOccurredAfter(eventRequestParams.getEnrollmentOccurredAfter())
            .eventStatus(eventRequestParams.getStatus())
            .attributeCategoryCombo(
                attributeCategoryCombo != null ? attributeCategoryCombo.getValue() : null)
            .attributeCategoryOptions(UID.toValueSet(attributeCategoryOptions))
            .idSchemes(eventRequestParams.getIdSchemes())
            .includeAttributes(false)
            .includeAllDataElements(false)
            .dataElementFilters(dataElementFilters)
            .attributeFilters(attributeFilters)
            .events(UID.toValueSet(eventUids))
            .enrollments(UID.toValueSet(eventRequestParams.getEnrollments()))
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
