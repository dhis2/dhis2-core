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
package org.hisp.dhis.analytics.common.params.dimension;

import static java.util.Collections.singletonList;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;

import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;

@Getter
@RequiredArgsConstructor(access = PRIVATE)
public class DimensionParamItem {

  private final AnalyticsQueryOperator operator;

  private final List<String> values;

  /**
   * This method parses a list of strings into a list of DimensionParamItem. Items can be either a
   * list in the form of [OP:VAL, OP2:VAL2, ...] (queryItem case) or a list of values in the form of
   * [VAL, VAL2, ...] (values case - dimensionalObject).
   *
   * @param items List of strings to be parsed.
   * @return List of DimensionParamItem.
   */
  public static List<DimensionParamItem> ofStrings(List<String> items) {
    if (isEmpty(items)) {
      return Collections.emptyList();
    }
    if (isQueryItemFormat(items)) {
      return ofQueryItemFormat(items);
    } else {
      return singletonList(new DimensionParamItem(null, items));
    }
  }

  /**
   * This method checks if the list of strings is in the queryItem format, i.e. if the first item
   * contains the dimension name separator. We assume that if the first item contains the separator,
   * then the rest of the items are also in the queryItem format.
   *
   * @param items List of strings to be checked.
   * @return True if the list is in the queryItem format, false otherwise.
   */
  private static boolean isQueryItemFormat(List<String> items) {
    return isNotEmpty(items) && items.get(0).contains(DIMENSION_NAME_SEP);
  }

  /**
   * This method parses a list of strings in the queryItem format into a list of DimensionParamItem.
   *
   * @param items List of strings to be parsed.
   * @return List of DimensionParamItem.
   */
  private static List<DimensionParamItem> ofQueryItemFormat(List<String> items) {
    return ListUtils.emptyIfNull(items).stream()
        .map(DimensionParamItem::ofQueryItemFormat)
        .toList();
  }

  /**
   * This method parses a string in the queryItem format into a DimensionParamItem by simply
   * splitting the string by the dimension name separator (:). The first part is the operator and
   * the second part is the value.
   *
   * @param item String to be parsed.
   * @return parsed DimensionParamItem.
   */
  private static DimensionParamItem ofQueryItemFormat(String item) {
    String[] parts = item.split(DIMENSION_NAME_SEP);
    AnalyticsQueryOperator queryOperator = AnalyticsQueryOperator.ofString(parts[0].trim());
    return new DimensionParamItem(queryOperator, singletonList(parts[1]));
  }
}
