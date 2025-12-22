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
import static org.hisp.dhis.common.DimensionConstants.OPTION_SEP;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.feedback.ErrorCode;

/**
 * Handles filter application for stage-specific EVENT_STATUS query items.
 *
 * <p>Supports semicolon-separated status values: ACTIVE;COMPLETED;SCHEDULE
 *
 * <p>Valid statuses are: ACTIVE, COMPLETED, SCHEDULE
 */
public class EventStatusFilterHandler implements QueryItemFilterHandler {

  private static final Set<String> VALID_STAGE_EVENT_STATUSES =
      Set.of("ACTIVE", "COMPLETED", "SCHEDULE");

  @Override
  public boolean supports(QueryItem queryItem) {
    return EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME.equals(queryItem.getItemId())
        && queryItem.hasProgramStage();
  }

  @Override
  public void applyFilters(
      QueryItem queryItem, String[] filterParts, String dimensionString, Date relativePeriodDate) {
    if (filterParts.length == 2) {
      parseAndAddEventStatusFilters(queryItem, filterParts[1]);
    } else {
      throwIllegalQueryEx(ErrorCode.E7222, dimensionString);
    }
  }

  private void parseAndAddEventStatusFilters(QueryItem queryItem, String filterString) {
    // Parse semicolon-separated status values (e.g., "ACTIVE;COMPLETED")
    String[] statuses = filterString.split(OPTION_SEP);

    List<String> validStatuses = new ArrayList<>();
    for (String status : statuses) {
      String trimmedStatus = status.trim().toUpperCase();
      if (!VALID_STAGE_EVENT_STATUSES.contains(trimmedStatus)) {
        throwIllegalQueryEx(ErrorCode.E7222, filterString);
      }
      validStatuses.add(trimmedStatus);
    }

    if (validStatuses.isEmpty()) {
      throwIllegalQueryEx(ErrorCode.E7222, filterString);
    }

    // Add IN filter with the valid statuses
    queryItem.addFilter(new QueryFilter(QueryOperator.IN, String.join(OPTION_SEP, validStatuses)));
  }
}
