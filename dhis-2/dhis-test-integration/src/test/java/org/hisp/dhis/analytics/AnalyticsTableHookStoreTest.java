/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.hisp.dhis.resourcetable.ResourceTableType;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class AnalyticsTableHookStoreTest extends SingleSetupIntegrationTestBase {

  @Autowired private AnalyticsTableHookStore sqlHookStore;

  private final String sql = "update _orgunitstructure set organisationunitid=1";

  @Test
  void testGetByType() {
    AnalyticsTableHook hookA =
        new AnalyticsTableHook(
            "NameA",
            AnalyticsTablePhase.RESOURCE_TABLE_POPULATED,
            ResourceTableType.ORG_UNIT_STRUCTURE,
            sql);
    AnalyticsTableHook hookB =
        new AnalyticsTableHook(
            "NameB",
            AnalyticsTablePhase.ANALYTICS_TABLE_POPULATED,
            AnalyticsTableType.DATA_VALUE,
            sql);
    AnalyticsTableHook hookC =
        new AnalyticsTableHook(
            "NameC",
            AnalyticsTablePhase.ANALYTICS_TABLE_POPULATED,
            AnalyticsTableType.DATA_VALUE,
            sql);
    AnalyticsTableHook hookD =
        new AnalyticsTableHook(
            "NameD", AnalyticsTablePhase.ANALYTICS_TABLE_POPULATED, AnalyticsTableType.EVENT, sql);
    sqlHookStore.save(hookA);
    sqlHookStore.save(hookB);
    sqlHookStore.save(hookC);
    sqlHookStore.save(hookD);
    List<AnalyticsTableHook> hooks =
        sqlHookStore.getByPhaseAndAnalyticsTableType(
            AnalyticsTablePhase.ANALYTICS_TABLE_POPULATED, AnalyticsTableType.DATA_VALUE);
    assertEquals(2, hooks.size());
    hooks.forEach(
        hook -> {
          assertNotNull(hook.getName());
          assertNotNull(hook.getPhase());
          assertEquals(AnalyticsTableType.DATA_VALUE, hook.getAnalyticsTableType());
        });
  }
}
