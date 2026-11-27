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
package org.hisp.dhis.tracker.export.trackerevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Duration;
import org.hisp.dhis.deadline.Deadline;
import org.hisp.dhis.deadline.DeadlineExceededException;
import org.hisp.dhis.deadline.DeadlineHolder;
import org.hisp.dhis.test.config.QueryCountDataSourceProxy;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * The tracker event export resolves the readable attribute option combos with the export JDBC
 * template, so the lookup is bounded by the export deadline like the export query (DHIS2-22400).
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = {QueryCountDataSourceProxy.class})
class TrackerEventAocLookupDeadlineTest extends PostgresIntegrationTestBase {
  /** The readable attribute option combo lookup. */
  private static final String AOC_LOOKUP = "select distinct ccoc.categoryoptioncomboid";

  /** A user without the ALL authority, so the attribute option combo lookup runs. */
  private static final String NON_SUPERUSER = "FIgVWzUCkpw";

  @Autowired private TestSetup testSetup;
  @Autowired private JdbcTrackerEventStore store;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();
  }

  @AfterEach
  void clearDeadline() {
    DeadlineHolder.clear();
  }

  @Test
  void shouldNotRunTheAocLookupWhenTheExportDeadlineHasPassed() {
    injectSecurityContextUser(userService.getUser(NON_SUPERUSER));
    QueryCountDataSourceProxy.clearCapturedSql();
    DeadlineHolder.set(new Deadline(Duration.ofSeconds(30), 0L, () -> 1L));
    TrackerEventQueryParams params = new TrackerEventQueryParams();

    assertThrows(DeadlineExceededException.class, () -> store.getEvents(params));

    assertEquals(0, QueryCountDataSourceProxy.countCapturedSqlMatching(AOC_LOOKUP));
  }

  @Test
  void shouldRunTheAocLookupWithoutADeadline() {
    injectSecurityContextUser(userService.getUser(NON_SUPERUSER));
    QueryCountDataSourceProxy.clearCapturedSql();

    store.getEvents(new TrackerEventQueryParams());

    assertEquals(1, QueryCountDataSourceProxy.countCapturedSqlMatching(AOC_LOOKUP));
  }
}
