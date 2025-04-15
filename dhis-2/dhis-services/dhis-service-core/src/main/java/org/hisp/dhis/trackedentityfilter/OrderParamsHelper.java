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
package org.hisp.dhis.trackedentityfilter;

import static org.hisp.dhis.trackedentityfilter.OrderParamsHelper.OrderColumn.findColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class OrderParamsHelper {
  static final String CREATED_ID = "created";

  static final String INACTIVE_ID = "inactive";

  static final String MAIN_QUERY_ALIAS = "TE";

  static final String ENROLLMENT_QUERY_ALIAS = "en";

  static List<String> validateOrderCriteria(
      List<OrderCriteria> orders, Map<String, TrackedEntityAttribute> attributes) {
    List<String> errors = new ArrayList<>();

    if (orders == null || orders.isEmpty()) {
      return errors;
    }

    for (OrderCriteria orderCriteria : orders) {
      if (findColumn(orderCriteria.getField()).isEmpty()
          && !attributes.containsKey(orderCriteria.getField())) {
        errors.add("Invalid order property: " + orderCriteria.getField());
      }
    }

    return errors;
  }

  @Getter
  @AllArgsConstructor
  enum OrderColumn {
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
    static Optional<OrderColumn> findColumn(String property) {
      return Arrays.stream(values())
          .filter(orderColumn -> orderColumn.getPropName().equals(property))
          .findFirst();
    }
  }
}
