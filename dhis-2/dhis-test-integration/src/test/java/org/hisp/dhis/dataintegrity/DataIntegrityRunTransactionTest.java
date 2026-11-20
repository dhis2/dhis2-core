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
package org.hisp.dhis.dataintegrity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;

/**
 * All checks of one run share the data integrity service's transaction. A check whose SQL fails in
 * PostgreSQL must not abort that transaction for the checks that run after it, nor fail the run.
 * Not {@code @Transactional}, so the service opens its own transaction as it does in production.
 */
class DataIntegrityRunTransactionTest extends PostgresIntegrationTestBase {

  private static final String FAILING = "zz_test_failing_check";
  private static final String VALID = "zz_test_valid_check";

  @Autowired private DataIntegrityService service;

  @Autowired private DataIntegrityStore store;

  @AfterEach
  void removeTestChecks() throws Exception {
    Map<String, DataIntegrityCheck> checks = checksByName();
    checks.remove(FAILING);
    checks.remove(VALID);
  }

  @Test
  void testFailingCheckDoesNotAbortLaterChecksOfTheSameRun() throws Exception {
    service.getDataIntegrityChecks(Set.of()); // load the built-in checks first
    Map<String, DataIntegrityCheck> checks = checksByName();
    // not slow: runs first; slow: runs after it (FAST_TO_SLOW order)
    checks.put(
        FAILING,
        DataIntegrityCheck.builder()
            .name(FAILING)
            .isSlow(false)
            .runSummaryCheck(c -> store.querySummary(c, "SELECT COUNT(*) FROM no_such_table"))
            .build());
    checks.put(
        VALID,
        DataIntegrityCheck.builder()
            .name(VALID)
            .isSlow(true)
            .runSummaryCheck(c -> store.querySummary(c, "SELECT 1 AS value"))
            .build());

    assertDoesNotThrow(() -> service.runSummaryChecks(Set.of(FAILING, VALID), JobProgress.noop()));

    Map<String, DataIntegritySummary> results = service.getSummaries(Set.of(FAILING, VALID), 0);
    DataIntegritySummary failing = results.get(FAILING);
    assertNotNull(failing, "failing check did not run");
    assertEquals(-1, failing.getCount());
    assertTrue(failing.getError().contains("no_such_table"), failing.getError());
    DataIntegritySummary valid = results.get(VALID);
    assertNotNull(valid, "valid check did not run");
    assertNull(valid.getError(), valid.getError());
    assertEquals(1, valid.getCount());
  }

  @SuppressWarnings("unchecked")
  private Map<String, DataIntegrityCheck> checksByName() throws Exception {
    Object target = AopTestUtils.getUltimateTargetObject(service);
    Field field = target.getClass().getDeclaredField("checksByName");
    field.setAccessible(true);
    return (Map<String, DataIntegrityCheck>) field.get(target);
  }
}
