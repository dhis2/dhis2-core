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
import org.hisp.dhis.feedback.ErrorCode;

/**
 * Handles filter application for stage-specific organisation unit (OU) query items.
 *
 * <p>Supports:
 *
 * <ul>
 *   <li>Simple IN filter: ou:ouA;ouB;ouC
 *   <li>Explicit operator:value pairs: ou:IN:ouA;ouB;ouC
 * </ul>
 */
public class OrgUnitFilterHandler implements QueryItemFilterHandler {

  @Override
  public boolean supports(QueryItem queryItem) {
    return EventAnalyticsColumnName.OU_COLUMN_NAME.equals(queryItem.getItemId())
        && queryItem.hasProgramStage();
  }

  @Override
  public void applyFilters(
      QueryItem queryItem, String[] filterParts, String dimensionString, Date relativePeriodDate) {
    if (filterParts.length == 2) {
      // Simple filter: ou:ouA;ouB;ouC -> use IN operator
      queryItem.addFilter(new QueryFilter(QueryOperator.IN, filterParts[1]));
    } else if (filterParts.length > 2) {
      // Multiple operator:value filters: ou:IN:ouA;ouB;ouC
      addGenericFilters(queryItem, filterParts);
    } else {
      throwIllegalQueryEx(ErrorCode.E7222, dimensionString);
    }
  }

  private void addGenericFilters(QueryItem queryItem, String[] filterParts) {
    for (int i = 1; i < filterParts.length; i += 2) {
      QueryOperator operator = QueryOperator.fromString(filterParts[i]);
      QueryFilter filter = new QueryFilter(operator, filterParts[i + 1]);
      queryItem.addFilter(filter);
    }
  }
}
