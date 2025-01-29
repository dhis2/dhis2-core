/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.util;

import java.util.List;
import lombok.experimental.UtilityClass;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.QueryItem;

@UtilityClass
public class EventQueryParamsUtils {

  /**
   * Get all program indicators from event query params.
   *
   * @param params event query params
   * @return list of program indicators
   */
  public static List<QueryItem> getProgramIndicators(EventQueryParams params) {
    return params.getItems().stream().filter(QueryItem::isProgramIndicator).toList();
  }

  /**
   * Remove program stage items from EventQueryParams. This method creates a copy of the
   * EventQueryParams instance and filters out QueryItems with hasProgramStage == true.
   *
   * @param params event query params
   * @return list of program stage items
   */
  public static EventQueryParams withoutProgramStageItems(EventQueryParams params) {
    // Create a copy of the EventQueryParams instance
    EventQueryParams.Builder builder = new EventQueryParams.Builder(params);

    // Filter out QueryItems with hasProgramStage == true
    List<QueryItem> filteredItems =
        params.getItems().stream().filter(item -> !item.hasProgramStage()).toList();

    // Clear the current items and itemFilters in the builder
    builder.removeItems(); // Clears the items

    for (QueryItem item : filteredItems) {
      builder.addItem(item);
    }

    return builder.build();
  }
}
