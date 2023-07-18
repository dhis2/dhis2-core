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
package org.hisp.dhis.webapi.controller.dataitem.validator;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.feedback.ErrorCode.E2015;
import static org.hisp.dhis.feedback.ErrorCode.E2037;
import static org.hisp.dhis.webapi.controller.dataitem.Order.Attribute.getNames;
import static org.hisp.dhis.webapi.controller.dataitem.Order.Nature.getValues;

import java.util.Set;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorMessage;

/**
 * Validator class responsible for validating order parameters.
 *
 * @author maikel arabori
 */
public class OrderValidator {
  public static final byte ORDERING_ATTRIBUTE_NAME = 0;

  public static final byte ORDERING_VALUE = 1;

  private OrderValidator() {}

  /**
   * Checks if the given set o filters are valid, and contains only filter names and operators
   * supported.
   *
   * @param orderParams a set containing elements in the format "attributeName:asc"
   * @throws IllegalQueryException if the set contains a non-supported name or operator, or contains
   *     invalid syntax.
   */
  public static void checkOrderParams(final Set<String> orderParams) {
    if (isNotEmpty(orderParams)) {
      for (final String orderParam : orderParams) {
        final String[] orderAttributeValuePair = orderParam.split(":");
        final String orderAttributeName =
            trimToEmpty(orderAttributeValuePair[ORDERING_ATTRIBUTE_NAME]);
        final String orderValue = trimToEmpty(orderAttributeValuePair[ORDERING_VALUE]);

        final boolean filterHasCorrectForm = orderAttributeValuePair.length == 2;

        if (filterHasCorrectForm) {
          // Check for valid order attribute name. Only a few DataItem
          // attributes are allowed.
          if (!getNames().contains(orderAttributeName)) {
            throw new IllegalQueryException(new ErrorMessage(E2037, orderAttributeName));
          }

          // Check for valid ordering. Only "asc" and "desc" are
          // allowed.
          if (!getValues().contains(orderValue)) {
            throw new IllegalQueryException(new ErrorMessage(E2037, orderValue));
          }
        } else {
          throw new IllegalQueryException(new ErrorMessage(E2015, orderParam));
        }
      }
    }
  }
}
