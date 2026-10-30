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

import java.util.List;
import org.hisp.dhis.common.QueryItem;
import org.springframework.stereotype.Component;

/**
 * Registry that selects the appropriate {@link QueryItemFilterHandler} for a given query item.
 *
 * <p>Note that the order of the handlers is important: specific handlers are registered first, with
 * the generic fallback handler registered last (it always matches).
 */
@Component
public class QueryItemFilterHandlerRegistry {

  private final List<QueryItemFilterHandler> handlers;

  public QueryItemFilterHandlerRegistry() {
    // Order matters: specific handlers first, generic last
    handlers =
        List.of(
            new DateFilterHandler(),
            new EventStatusFilterHandler(),
            new OrgUnitFilterHandler(),
            new GenericFilterHandler()); // Must be last (always matches)
  }

  /**
   * Finds the appropriate handler for the given query item.
   *
   * @param queryItem the query item to find a handler for
   * @return the matching handler (GenericFilterHandler as fallback, never null)
   */
  public QueryItemFilterHandler handlerFor(QueryItem queryItem) {
    return handlers.stream()
        .filter(h -> h.supports(queryItem))
        .findFirst()
        .orElseThrow(); // GenericFilterHandler always matches, so this won't throw
  }
}
