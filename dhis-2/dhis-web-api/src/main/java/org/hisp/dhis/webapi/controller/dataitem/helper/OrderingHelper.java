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
package org.hisp.dhis.webapi.controller.dataitem.helper;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.webapi.controller.dataitem.validator.OrderValidator.ORDERING_ATTRIBUTE_NAME;
import static org.hisp.dhis.webapi.controller.dataitem.validator.OrderValidator.ORDERING_VALUE;

import java.util.Set;
import lombok.NoArgsConstructor;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Helper class responsible for providing sorting capabilities.
 *
 * @author maikel arabori
 */
@NoArgsConstructor(access = PRIVATE)
public class OrderingHelper {
  /**
   * Sets the ordering defined by orderParams into the paramsMap. It will set the given
   * "orderParams" into the provided "paramsMap". It's important to highlight that the "key" added
   * to the "paramsMap" will contain the actual order param, ie.: "name" + "Order". So, if there is
   * a "name" as order param, the "key" will result in "nameOrder". This method is used to set the
   * ordering at database level.
   *
   * @param orderParams the source of ordering params
   * @param paramsMap the map that will receive the order params
   */
  public static void setOrderingParams(OrderParams orderParams, MapSqlParameterSource paramsMap) {
    if (orderParams != null && isNotEmpty(orderParams.getOrders())) {
      Set<String> orders = orderParams.getOrders();

      for (String order : orders) {
        String[] orderAttributeValuePair = order.split(":");

        // Concatenation of param name (ie.:"name") + "Order". It will
        // result in "nameOrder".
        paramsMap.addValue(
            trimToEmpty(orderAttributeValuePair[ORDERING_ATTRIBUTE_NAME]).concat("Order"),
            trimToEmpty(orderAttributeValuePair[ORDERING_VALUE]));
      }
    }
  }
}
