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

import static org.hisp.dhis.test.TestBase.createProgram;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.EndpointItem;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.commons.util.SqlHelper;
import org.junit.jupiter.api.Test;

class EventAnalyticsSqlContextTest {

  @Test
  void ofParamsReturnsContextWithGivenParamsAndNonNullCollaborators() {
    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(createProgram('A')).build();

    EventAnalyticsSqlContext ctx = EventAnalyticsSqlContext.of(params);

    assertSame(params, ctx.params());
    assertNotNull(ctx.cteContext());
    assertNotNull(ctx.sqlHelper());
  }

  @Test
  void ofParamsUsesEnrollmentEndpointMatchingAbstractParentDefault() {
    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(createProgram('B')).build();

    EventAnalyticsSqlContext ctx = EventAnalyticsSqlContext.of(params);

    assertSame(EndpointItem.ENROLLMENT, ctx.cteContext().getEndpointItem());
  }

  @Test
  void directConstructionExposesAllComponents() {
    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(createProgram('C')).build();
    CteContext cteContext = new CteContext(EndpointItem.EVENT);
    SqlHelper sqlHelper = new SqlHelper();

    EventAnalyticsSqlContext ctx = new EventAnalyticsSqlContext(params, cteContext, sqlHelper);

    assertSame(params, ctx.params());
    assertSame(cteContext, ctx.cteContext());
    assertSame(sqlHelper, ctx.sqlHelper());
  }

  @Test
  void nullParamsRejected() {
    assertThrows(
        NullPointerException.class,
        () ->
            new EventAnalyticsSqlContext(
                null, new CteContext(EndpointItem.EVENT), new SqlHelper()));
  }

  @Test
  void nullCteContextRejected() {
    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(createProgram('D')).build();
    assertThrows(
        NullPointerException.class,
        () -> new EventAnalyticsSqlContext(params, null, new SqlHelper()));
  }

  @Test
  void nullSqlHelperRejected() {
    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(createProgram('E')).build();
    assertThrows(
        NullPointerException.class,
        () -> new EventAnalyticsSqlContext(params, new CteContext(EndpointItem.EVENT), null));
  }
}
