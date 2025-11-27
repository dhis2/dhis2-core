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
package org.hisp.dhis.analytics.event.data.queryitem;

import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;

import java.util.Date;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;

/**
 * Default fallback handler for generic query item filters.
 *
 * <p>Handles all query items not matched by more specific handlers. Expects operator:value pairs in
 * the format: dimension:OP:value:OP2:value2
 *
 * <p>The filter parts array must have an odd length (dimension + N*(operator + value) pairs).
 *
 * <p>This handler also applies time format correction for TIME-type DataElement items, converting
 * HH.MM format to HH:MM format.
 */
public class GenericFilterHandler implements QueryItemFilterHandler {

  /**
   * Always returns true as this is the default fallback handler. Must be registered last in the
   * handler chain.
   */
  @Override
  public boolean supports(QueryItem queryItem) {
    return true;
  }

  @Override
  public void applyFilters(
      QueryItem queryItem, String[] filterParts, String dimensionString, Date relativePeriodDate) {
    // Validate odd length: dimension + N*(operator + value)
    if (filterParts.length % 2 != 1) {
      throwIllegalQueryEx(ErrorCode.E7222, dimensionString);
    }

    // Apply filters starting from index 1 (skip dimension ID at index 0)
    for (int i = 1; i < filterParts.length; i += 2) {
      QueryOperator operator = QueryOperator.fromString(filterParts[i]);
      QueryFilter filter = new QueryFilter(operator, filterParts[i + 1]);

      // Apply time format correction for TIME-type DataElement items
      modifyFilterWhenTimeQueryItem(queryItem, filter);

      queryItem.addFilter(filter);
    }
  }

  /**
   * Modifies filter value for TIME-type DataElement items. Frontend uses HH.MM format but database
   * expects HH:MM format.
   *
   * <p>This correction is not applied to date fields like EVENT_DATE.
   */
  private void modifyFilterWhenTimeQueryItem(QueryItem queryItem, QueryFilter filter) {
    // Skip date fields - they don't need time format correction
    if (EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME.equals(queryItem.getItemId())
        && queryItem.getValueType() == ValueType.DATE) {
      return;
    }

    // Apply correction for TIME-type DataElement items
    if (queryItem.getItem() instanceof DataElement
        && ((DataElement) queryItem.getItem()).getValueType() == ValueType.TIME) {
      filter.setFilter(filter.getFilter().replace(".", ":"));
    }
  }
}
