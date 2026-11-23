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
package org.hisp.dhis.analytics.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.hisp.dhis.analytics.AnalyticsTableHook;
import org.hisp.dhis.analytics.AnalyticsTablePhase;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.resourcetable.ResourceTableType;
import org.hisp.dhis.security.acl.AclService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Morten Svanæs
 */
@ExtendWith(MockitoExtension.class)
class HibernateAnalyticsTableHookStoreTest {

  @Mock private EntityManager entityManager;
  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private ApplicationEventPublisher publisher;
  @Mock private AclService aclService;
  @Mock private DhisConfigurationProvider config;

  private HibernateAnalyticsTableHookStore store;

  @BeforeEach
  void setUp() {
    store =
        new HibernateAnalyticsTableHookStore(
            entityManager, jdbcTemplate, publisher, aclService, config);
  }

  @Test
  void executeSkipsSqlWhenFeatureDisabled() {
    when(config.isDisabled(ConfigurationKey.ANALYTICS_TABLE_HOOKS_ENABLED)).thenReturn(true);

    AnalyticsTableHook hook =
        new AnalyticsTableHook(
            "Danger",
            AnalyticsTablePhase.RESOURCE_TABLE_POPULATED,
            ResourceTableType.ORG_UNIT_STRUCTURE,
            "drop table users");
    hook.setUid("abc123");

    assertEquals(0, store.executeAnalyticsTableSqlHooks(List.of(hook)));

    verify(jdbcTemplate, never()).execute(anyString());
  }

  @Test
  void executeRunsSqlWhenFeatureEnabled() {
    when(config.isDisabled(ConfigurationKey.ANALYTICS_TABLE_HOOKS_ENABLED)).thenReturn(false);

    AnalyticsTableHook hook =
        new AnalyticsTableHook(
            "Index",
            AnalyticsTablePhase.RESOURCE_TABLE_POPULATED,
            ResourceTableType.ORG_UNIT_STRUCTURE,
            "create index if not exists idx on analytics_rs_orgunitstructure(organisationunitid)");
    hook.setUid("def456");

    assertEquals(1, store.executeAnalyticsTableSqlHooks(List.of(hook)));

    verify(jdbcTemplate).execute(hook.getSql());
  }
}
