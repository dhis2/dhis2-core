/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateOrderParams;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validatePaginationBounds;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.tracker.export.event.EventChangeLogOperationParams;
import org.hisp.dhis.tracker.export.event.EventChangeLogOperationParams.EventChangeLogOperationParamsBuilder;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;

class ChangeLogRequestParamsMapper {
  private ChangeLogRequestParamsMapper() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * This mapper is different from other tracker exporter mappers as it takes in the orderable
   * fields. This difference comes from {@link org.hisp.dhis.tracker.export.event.EventChangeLog}
   * being the view which is already returned from the service/store. Tracker exporter services
   * return a representation we have to map to a view model. This mapping for example for events is
   * done in {@link EventMapper} is not necessary for change logs.
   */
  static EventChangeLogOperationParams map(
      Set<String> orderableFields, ChangeLogRequestParams requestParams)
      throws BadRequestException {
    validatePaginationBounds(requestParams.getPage(), requestParams.getPageSize());
    validateOrderParams(requestParams.getOrder(), orderableFields);

    EventChangeLogOperationParamsBuilder builder = EventChangeLogOperationParams.builder();
    mapOrderParam(builder, requestParams.getOrder());
    return builder.build();
  }

  private static void mapOrderParam(
      EventChangeLogOperationParamsBuilder builder, List<OrderCriteria> orders) {
    if (orders == null || orders.isEmpty()) {
      return;
    }

    orders.forEach(order -> builder.orderBy(order.getField(), order.getDirection()));
  }
}
