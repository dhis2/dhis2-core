/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateFilter;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateOrderParams;

import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLogOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLogOperationParams.TrackedEntityChangeLogOperationParamsBuilder;
import org.hisp.dhis.webapi.controller.tracker.export.ChangeLogRequestParams;

class ChangeLogRequestParamsMapper {
  private ChangeLogRequestParamsMapper() {
    throw new IllegalStateException("Utility class");
  }

  static TrackedEntityChangeLogOperationParams map(
      Set<String> orderableFields,
      Set<Pair<String, Class<?>>> filterableFields,
      ChangeLogRequestParams requestParams)
      throws BadRequestException {
    validateOrderParams(requestParams.getOrder(), orderableFields);
    validateFilter(requestParams.getFilter(), filterableFields);

    TrackedEntityChangeLogOperationParamsBuilder builder =
        TrackedEntityChangeLogOperationParams.builder();
    mapOrderParam(builder, requestParams.getOrder());
    mapFilterParam(builder, requestParams.getFilter());
    return builder.build();
  }

  private static void mapOrderParam(
      TrackedEntityChangeLogOperationParamsBuilder builder, List<OrderCriteria> orders) {
    if (orders == null || orders.isEmpty()) {
      return;
    }

    orders.forEach(order -> builder.orderBy(order.getField(), order.getDirection()));
  }

  private static void mapFilterParam(
      TrackedEntityChangeLogOperationParamsBuilder builder, String filter) {
    if (filter != null) {
      String[] split = filter.split(":");
      builder.filterBy(split[0], new QueryFilter(QueryOperator.EQ, split[2]));
    }
  }
}
