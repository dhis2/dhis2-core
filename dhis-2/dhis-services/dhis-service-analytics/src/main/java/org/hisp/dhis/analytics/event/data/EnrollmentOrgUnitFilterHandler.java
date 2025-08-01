/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static lombok.AccessLevel.PRIVATE;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.AGGREGATE;
import static org.hisp.dhis.common.RequestTypeAware.EndpointItem.ENROLLMENT;
import static org.hisp.dhis.common.ValueType.ORGANISATION_UNIT;

import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.QueryItem;

/**
 * Helper class to concentrate all required logic to support SQL/query handling for enrollments org.
 * unit filtering .
 */
@NoArgsConstructor(access = PRIVATE)
public class EnrollmentOrgUnitFilterHandler {

  /**
   * Checks if org. unit item filter handling is needed for enrollments/aggregate.
   *
   * @param params the {@link EventQueryParams}.
   * @param item the {@link QueryItem}.
   * @return true if handling is required, false otherwise.
   */
  public static boolean handleEnrollmentOrgUnitFilter(EventQueryParams params, QueryItem item) {
    return params.hasOrgUnitFilterInItem()
        && params.getEndpointAction() == AGGREGATE
        && params.getEndpointItem() == ENROLLMENT
        && item.getValueType() == ORGANISATION_UNIT;
  }

  /**
   * Checks if org. unit item filter handling is needed for enrollments/aggregate.
   *
   * @param params the {@link EventQueryParams}.
   * @return true if handling is required, false otherwise.
   */
  public static boolean handleEnrollmentOrgUnitFilter(EventQueryParams params) {
    return params.hasOrgUnitFilterInItem()
        && params.getEndpointAction() == AGGREGATE
        && params.getEndpointItem() == ENROLLMENT;
  }

  /**
   * Checks handling is needed for enrollments/aggregate.
   *
   * @param request the {@link EventDataQueryRequest}.
   * @return true if handling is required, false otherwise.
   */
  public static boolean isAggregateEnrollment(EventDataQueryRequest request) {
    return request.getEndpointAction() == AGGREGATE && request.getEndpointItem() == ENROLLMENT;
  }

  /**
   * Checks if handling is needed for enrollments/aggregate.
   *
   * @param params the {@link EventQueryParams}.
   * @return true if handling is required, false otherwise.
   */
  public static boolean isAggregateEnrollment(EventQueryParams params) {
    return params.getEndpointAction() == AGGREGATE && params.getEndpointItem() == ENROLLMENT;
  }
}
