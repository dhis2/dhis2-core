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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.validateDeprecatedParameter;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.tracker.export.relationship.RelationshipOperationParams;
import org.hisp.dhis.webapi.common.UID;
import org.springframework.stereotype.Component;

/**
 * Maps query parameters from {@link RelationshipsExportController} stored in {@link
 * LegacyRequestParams} to {@link RelationshipOperationParams} which is used to fetch events from
 * the DB.
 */
@Component
@RequiredArgsConstructor
class RelationshipRequestParamsMapper {

  public RelationshipOperationParams map(RequestParams requestParams) throws BadRequestException {
    UID trackedEntity =
        validateDeprecatedParameter(
            "tei", requestParams.getTei(), "trackedEntity", requestParams.getTrackedEntity());

    int count = 0;
    if (trackedEntity != null) {
      count++;
    }
    if (requestParams.getEnrollment() != null) {
      count++;
    }
    if (requestParams.getEvent() != null) {
      count++;
    }

    if (count == 0) {
      throw new BadRequestException(
          "Missing required parameter 'trackedEntity', 'enrollment' or 'event'.");
    } else if (count > 1) {
      throw new BadRequestException(
          "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.");
    }

    return RelationshipOperationParams.builder()
        .trackedEntity(trackedEntity == null ? null : trackedEntity.getValue())
        .enrollment(
            requestParams.getEnrollment() == null ? null : requestParams.getEnrollment().getValue())
        .event(requestParams.getEvent() == null ? null : requestParams.getEvent().getValue())
        .page(requestParams.getPage())
        .pageSize(requestParams.getPageSize())
        .totalPages(requestParams.isTotalPages())
        .skipPaging(toBooleanDefaultIfNull(requestParams.isSkipPaging(), false))
        .build();
  }
}
