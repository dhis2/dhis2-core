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
package org.hisp.dhis.analytics.common.params.dimension;

import static java.util.Set.of;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Set;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DimensionParamType}. */
class DimensionParamTypeTest {
  @Test
  void mapDates() {
    CommonRequestParams request =
        new CommonRequestParams()
            .withOccurredDate(of("LAST_MONTH"))
            .withEnrollmentDate(of("2021-06-30"))
            .withLastUpdated(of("TODAY"))
            .withScheduledDate(of("YESTERDAY"));
    Collection<String> dateFilters = DimensionParamType.DATE_FILTERS.getUidsGetter().apply(request);

    Set<String> expected =
        of(
            "pe:2021-06-30:ENROLLMENT_DATE",
            "pe:LAST_MONTH:OCCURRED_DATE",
            "pe:YESTERDAY:SCHEDULED_DATE",
            "pe:TODAY:LAST_UPDATED");

    assertTrue(expected.containsAll(dateFilters));
    assertTrue(dateFilters.containsAll(expected));
  }
}
