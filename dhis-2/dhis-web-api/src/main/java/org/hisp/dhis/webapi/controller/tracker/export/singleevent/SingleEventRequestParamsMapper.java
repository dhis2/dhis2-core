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
package org.hisp.dhis.webapi.controller.tracker.export.singleevent;

import static java.util.Collections.emptySet;
import static org.hisp.dhis.util.ObjectUtils.applyIfNotNull;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateEventIdsAndFilter;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateMandatoryProgram;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateOrderParams;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateOrgUnitModeForEnrollmentsAndEvents;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateUpdateDurationParams;
import static org.hisp.dhis.webapi.controller.tracker.export.FilterParser.parseFilters;
import static org.hisp.dhis.webapi.controller.tracker.export.singleevent.SingleEventMapper.ORDERABLE_FIELDS;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.export.singleevent.SingleEventFields;
import org.hisp.dhis.tracker.export.singleevent.SingleEventOperationParams;
import org.hisp.dhis.tracker.export.singleevent.SingleEventOperationParams.SingleEventOperationParamsBuilder;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;

/**
 * Maps query parameters from {@link SingleEventsExportController} stored in {@link
 * SingleEventRequestParams} to {@link SingleEventOperationParams} which is used to fetch events
 * from the DB.
 */
class SingleEventRequestParamsMapper {
  private SingleEventRequestParamsMapper() {
    throw new IllegalStateException("Utility class");
  }

  private static final Set<String> ORDERABLE_FIELD_NAMES = ORDERABLE_FIELDS.keySet();

  static SingleEventOperationParams map(
      SingleEventRequestParams eventRequestParams, TrackerIdSchemeParams idSchemeParams)
      throws BadRequestException {
    validateMandatoryProgram(eventRequestParams.getProgram());
    OrganisationUnitSelectionMode orgUnitMode =
        validateOrgUnitModeForEnrollmentsAndEvents(
            eventRequestParams.getOrgUnit() != null
                ? Set.of(eventRequestParams.getOrgUnit())
                : emptySet(),
            eventRequestParams.getOrgUnitMode());

    validateEventIdsAndFilter(eventRequestParams.getFilter(), eventRequestParams.getEvents());
    Map<UID, List<QueryFilter>> dataElementFilters =
        parseFilters("filter", eventRequestParams.getFilter());

    validateUpdateDurationParams(
        eventRequestParams.getUpdatedWithin(),
        eventRequestParams.getUpdatedAfter(),
        eventRequestParams.getUpdatedBefore());
    validateOrderParams(eventRequestParams.getOrder(), ORDERABLE_FIELD_NAMES, "data element");

    SingleEventOperationParamsBuilder builder =
        SingleEventOperationParams.builderForProgram(eventRequestParams.getProgram())
            .orgUnit(eventRequestParams.getOrgUnit())
            .orgUnitMode(orgUnitMode)
            .assignedUserMode(eventRequestParams.getAssignedUserMode())
            .assignedUsers(eventRequestParams.getAssignedUsers())
            .occurredAfter(
                applyIfNotNull(eventRequestParams.getOccurredAfter(), StartDateTime::toDate))
            .occurredBefore(
                applyIfNotNull(eventRequestParams.getOccurredBefore(), EndDateTime::toDate))
            .updatedAfter(
                applyIfNotNull(eventRequestParams.getUpdatedAfter(), StartDateTime::toDate))
            .updatedBefore(
                applyIfNotNull(eventRequestParams.getUpdatedBefore(), EndDateTime::toDate))
            .updatedWithin(eventRequestParams.getUpdatedWithin())
            .eventStatus(eventRequestParams.getStatus())
            .attributeCategoryCombo(eventRequestParams.getAttributeCategoryCombo())
            .events(eventRequestParams.getEvents())
            .includeDeleted(eventRequestParams.isIncludeDeleted())
            .fields(
                SingleEventFields.of(
                    eventRequestParams.getFields()::includes, FieldPath.FIELD_PATH_SEPARATOR))
            .idSchemeParams(idSchemeParams);

    mapOrderParam(builder, eventRequestParams.getOrder());
    mapDataElementFilterParam(builder, dataElementFilters);
    return builder.build();
  }

  private static void mapOrderParam(
      SingleEventOperationParamsBuilder builder, List<OrderCriteria> orders) {
    if (orders == null || orders.isEmpty()) {
      return;
    }

    for (OrderCriteria order : orders) {
      if (ORDERABLE_FIELDS.containsKey(order.getField())) {
        builder.orderBy(ORDERABLE_FIELDS.get(order.getField()), order.getDirection());
      } else {
        builder.orderBy(UID.of(order.getField()), order.getDirection());
      }
    }
  }

  private static void mapDataElementFilterParam(
      SingleEventOperationParamsBuilder builder, Map<UID, List<QueryFilter>> dataElementFilters) {
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
}
