/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_TABLE_HOOKS_ENABLED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.resourcetable.ResourceTableType;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Runs a stored {@link AnalyticsTableHook} through resource table generation, the way a hook
 * created before an upgrade would run, and checks it only executes when {@code
 * analytics.table_hooks.enabled} is on. The hook creates a marker table so execution is observable.
 */
class AnalyticsTableHookExecutionTest extends TransactionalIntegrationTest {

  @Autowired private AnalyticsTableHookStore hookStore;

  @Autowired private ResourceTableService resourceTableService;

  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void restoreDefaultConfig() {
    dhisConfigurationProvider.getProperties().remove(ANALYTICS_TABLE_HOOKS_ENABLED.getKey());
  }

  @Test
  void storedHookIsNotExecutedByDefault() {
    assertFalse(generateResourceTablesWithMarkerHook("hook_marker_default"));
  }

  @Test
  void storedHookIsExecutedWhenFeatureEnabled() {
    dhisConfigurationProvider.getProperties().put(ANALYTICS_TABLE_HOOKS_ENABLED.getKey(), "on");

    assertTrue(generateResourceTablesWithMarkerHook("hook_marker_enabled"));
  }

  private boolean generateResourceTablesWithMarkerHook(String markerTable) {
    hookStore.save(
        new AnalyticsTableHook(
            "Marker " + markerTable,
            AnalyticsTablePhase.RESOURCE_TABLE_POPULATED,
            ResourceTableType.ORG_UNIT_STRUCTURE,
            "create table " + markerTable + " (id integer)"));

    resourceTableService.generateResourceTables();

    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            "select to_regclass(?) is not null", Boolean.class, "public." + markerTable));
  }
}
