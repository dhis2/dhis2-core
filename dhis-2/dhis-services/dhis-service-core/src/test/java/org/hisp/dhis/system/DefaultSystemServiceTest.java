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
package org.hisp.dhis.system;

import static org.hisp.dhis.analytics.AnalyticsTableType.COMPLETENESS;
import static org.hisp.dhis.analytics.AnalyticsTableType.DATA_VALUE;
import static org.hisp.dhis.analytics.AnalyticsTableType.ENROLLMENT;
import static org.hisp.dhis.analytics.AnalyticsTableType.EVENT;
import static org.hisp.dhis.analytics.AnalyticsTableType.TRACKED_ENTITY_INSTANCE_EVENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link DefaultSystemService}'s per-{@link AnalyticsTableType} update-timestamp mapping,
 * added for DHIS2-21992 (Phase 1: System Info surface only).
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
class DefaultSystemServiceTest {

  @Test
  void getLastSuccessfulUpdateByType_onlyIncludesLatestPartitionCapableTypes() {
    Map<AnalyticsTableType, Date> result =
        DefaultSystemService.getLastSuccessfulUpdateByType(type -> new Date(type.ordinal()));

    assertEquals(
        Set.of(DATA_VALUE, COMPLETENESS, EVENT, TRACKED_ENTITY_INSTANCE_EVENTS), result.keySet());
  }

  @Test
  void getLastSuccessfulUpdateByType_excludesTypeWithoutLatestPartitionSupport() {
    Map<AnalyticsTableType, Date> result =
        DefaultSystemService.getLastSuccessfulUpdateByType(type -> new Date(type.ordinal()));

    assertFalse(result.containsKey(ENROLLMENT));
  }

  @Test
  void getLastSuccessfulUpdateByType_valuesComeFromTheGivenGetter() {
    Date eventDate = new Date(123456L);
    Map<AnalyticsTableType, Date> result =
        DefaultSystemService.getLastSuccessfulUpdateByType(
            type -> type == EVENT ? eventDate : new Date(0L));

    assertEquals(eventDate, result.get(EVENT));
    assertEquals(new Date(0L), result.get(DATA_VALUE));
  }
}
