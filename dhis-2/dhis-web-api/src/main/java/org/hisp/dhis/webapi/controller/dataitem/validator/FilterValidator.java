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

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.feedback.ErrorCode.E2014;
import static org.hisp.dhis.feedback.ErrorCode.E2034;
import static org.hisp.dhis.feedback.ErrorCode.E2035;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Attribute.getNames;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.getCombinations;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Custom.getPropertyNames;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Operation.getAbbreviations;

import java.util.Set;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.webapi.controller.dataitem.Filter;

/**
 * Validator class responsible for validating filter parameters.
 *
 * @author maikel arabori
 */
@NoArgsConstructor(access = PRIVATE)
public class FilterValidator {
  public static final byte FILTER_ATTRIBUTE_NAME = 0;

  public static final byte FILTER_OPERATOR = 1;

  /**
   * Checks if the given set o filters are valid, and contains only filter names and operators
   * supported.
   *
   * @param filters in the format filterName:eq:aWord
   * @throws IllegalQueryException if the set contains a non-supported name or operator, or and
   *     invalid syntax.
   */
  public static void checkNamesAndOperators(Set<String> filters) {
    if (isNotEmpty(filters)) {
      for (String filter : filters) {
        {
          String[] filterAttributeValuePair = filter.split(":");
          boolean filterHasCorrectForm = filterAttributeValuePair.length == 3;

          if (filterHasCorrectForm) {
            String attributeName = trimToEmpty(filterAttributeValuePair[FILTER_ATTRIBUTE_NAME]);

            String operator = trimToEmpty(filterAttributeValuePair[FILTER_OPERATOR]);

            if (!getNames().contains(attributeName)
                && !getPropertyNames().contains(attributeName)) {
              throw new IllegalQueryException(new ErrorMessage(E2034, attributeName));
            }

            if (!getAbbreviations().contains(operator)) {
              throw new IllegalQueryException(new ErrorMessage(E2035, operator));
            }

            if (getCombinations().stream()
                .noneMatch(combination -> filter.startsWith(combination))) {
              throw new IllegalQueryException(
                  new ErrorMessage(E2035, substringBeforeLast(filter, ":")));
            }
          } else {
            throw new IllegalQueryException(new ErrorMessage(E2014, filter));
          }
        }
      }
    }
  }

  /**
   * Simply checks if the given set of filters contains any one of the provided prefixes.
   *
   * @param filters
   * @param withPrefixes
   * @return true if a filter prefix is found, false otherwise.
   */
  public static boolean containsFilterWithAnyOfPrefixes(
      Set<String> filters, String... withPrefixes) {
    if (isNotEmpty(filters) && withPrefixes != null && withPrefixes.length > 0) {
      for (String filter : filters) {
        for (String prefix : withPrefixes) {
          if (filterHasPrefix(filter, prefix)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  /**
   * Simply checks if a given filter start the prefix provided.
   *
   * @param filter the full filter param, in the format: name:eq:someName, where 'name' is the
   *     attribute and 'eq' is the operator
   * @param prefix the prefix to be matched. See {@link Filter.Combination} for valid ones
   * @return true if the current filter starts with the given prefix, false otherwise
   */
  public static boolean filterHasPrefix(String filter, String prefix) {
    if (isNotBlank(prefix) && isNotBlank(filter)) {
      return trimToEmpty(filter).startsWith(trimToEmpty(prefix));
    }

    return false;
  }
}
