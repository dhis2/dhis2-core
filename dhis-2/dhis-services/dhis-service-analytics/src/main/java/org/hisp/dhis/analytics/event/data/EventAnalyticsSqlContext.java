/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.analytics.event.data;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nonnull;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.EndpointItem;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.commons.util.SqlHelper;

/**
 * Carries the three collaborators that analytics SQL assembly helpers consistently need together:
 * the {@link EventQueryParams} describing the query, the {@link CteContext} accumulating CTE
 * definitions, and the {@link SqlHelper} driving conjunction/disjunction punctuation.
 *
 * <p>Used by helper components extracted from {@link AbstractJdbcEventAnalyticsManager} to avoid
 * threading three positional parameters through deeply nested method signatures.
 */
public record EventAnalyticsSqlContext(
    @Nonnull EventQueryParams params,
    @Nonnull CteContext cteContext,
    @Nonnull SqlHelper sqlHelper) {

  public EventAnalyticsSqlContext {
    requireNonNull(params);
    requireNonNull(cteContext);
    requireNonNull(sqlHelper);
  }

  /**
   * Creates a context with a fresh {@link CteContext} and {@link SqlHelper}. The CTE context
   * defaults to {@link EndpointItem#ENROLLMENT}, matching the fallback used by {@code
   * AbstractJdbcEventAnalyticsManager#getCteDefinitions} when no context is supplied. Event-path
   * callers that need {@link EndpointItem#EVENT} should construct the record directly.
   */
  public static EventAnalyticsSqlContext of(@Nonnull EventQueryParams params) {
    return new EventAnalyticsSqlContext(
        params, new CteContext(EndpointItem.ENROLLMENT), new SqlHelper());
  }
}
