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
package org.hisp.dhis.webapi.controller.event.mapper;

import static org.hisp.dhis.webapi.controller.event.mapper.OrderParamsHelper.OrderColumn.findColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderParamsHelper {
  public static final String CREATED_ID = "created";

  public static final String ORG_UNIT_NAME = "ouname";

  public static final String INACTIVE_ID = "inactive";

  public static final String MAIN_QUERY_ALIAS = "TE";

  public static final String ENROLLMENT_QUERY_ALIAS = "en";

  public static List<OrderParam> toOrderParams(List<OrderCriteria> criteria) {
    return Optional.ofNullable(criteria).orElse(Collections.emptyList()).stream()
        .filter(Objects::nonNull)
        .map(
            orderCriteria -> new OrderParam(orderCriteria.getField(), orderCriteria.getDirection()))
        .collect(Collectors.toList());
  }

  public static List<String> validateOrderParams(
      List<OrderParam> orderParams, Map<String, TrackedEntityAttribute> attributes) {
    List<String> errors = new ArrayList<>();

    if (orderParams == null || orderParams.isEmpty()) {
      return errors;
    }

    for (OrderParam orderParam : orderParams) {
      if (findColumn(orderParam.getField()).isEmpty()
          && !attributes.containsKey(orderParam.getField())) {
        errors.add("Invalid order property: " + orderParam.getField());
      }
    }

    return errors;
  }

  @Getter
  @AllArgsConstructor
  public enum OrderColumn {
    TRACKEDENTITY("trackedEntity", "uid", MAIN_QUERY_ALIAS),
    CREATED_AT("createdAt", CREATED_ID, MAIN_QUERY_ALIAS),
    CREATED_AT_CLIENT("createdAtClient", "createdatclient", MAIN_QUERY_ALIAS),
    UPDATED_AT("updatedAt", "lastupdated", MAIN_QUERY_ALIAS),
    UPDATED_AT_CLIENT("updatedAtClient", "lastupdatedatclient", MAIN_QUERY_ALIAS),
    ENROLLED_AT("enrolledAt", "enrollmentdate", ENROLLMENT_QUERY_ALIAS),
    INACTIVE(INACTIVE_ID, "inactive", MAIN_QUERY_ALIAS);

    private final String propName;

    private final String column;

    private final String tableAlias;

    /**
     * @return an Optional of an OrderColumn matching by property name
     */
    public static Optional<OrderColumn> findColumn(String property) {
      return Arrays.stream(values())
          .filter(orderColumn -> orderColumn.getPropName().equals(property))
          .findFirst();
    }
  }
}
