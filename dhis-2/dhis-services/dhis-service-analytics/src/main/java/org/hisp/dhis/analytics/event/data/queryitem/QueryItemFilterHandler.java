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

import java.util.Date;
import org.hisp.dhis.common.QueryItem;

/**
 * Strategy interface for handling filter application to QueryItem based on item type. Follows the
 * same pattern as {@link
 * org.hisp.dhis.analytics.event.data.programindicator.ctefactory.CteSqlFactory}.
 *
 * <p>Implementations handle specific types of query items (e.g., date fields, event status, org
 * units) with their own filter parsing and validation logic.
 */
public interface QueryItemFilterHandler {

  /**
   * Returns {@code true} when this handler can process filters for the given QueryItem.
   *
   * @param queryItem the query item to check
   * @return true if this handler supports the query item type
   */
  boolean supports(QueryItem queryItem);

  /**
   * Applies filters from the dimension string parts to the query item.
   *
   * @param queryItem the query item to add filters to
   * @param filterParts the split dimension string (index 0 is item ID, 1+ are filters)
   * @param dimensionString original dimension string for error messages
   * @param relativePeriodDate reference date for relative period calculations (may be null)
   */
  void applyFilters(
      QueryItem queryItem, String[] filterParts, String dimensionString, Date relativePeriodDate);
}
