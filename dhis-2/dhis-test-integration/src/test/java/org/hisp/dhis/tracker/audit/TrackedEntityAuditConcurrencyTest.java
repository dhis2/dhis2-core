/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.audit;

import static org.awaitility.Awaitility.await;
import static org.hisp.dhis.audit.AuditOperationType.READ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAuditQueryParams;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Guards against silently losing tracked entity audits under concurrent reads.
 *
 * <p>The audit write is asynchronous. If the audit values are resolved on the async thread instead
 * of the caller thread, the entity handed across the boundary can be a proxy whose Hibernate
 * session has already been closed; dereferencing it throws LazyInitializationException, which the
 * async uncaught-exception handler swallows. The read then succeeds while its audit record is
 * silently dropped. That failed on roughly 84% of reads before the values were resolved on the
 * caller thread.
 *
 * <p>Deliberately NOT {@code @Transactional}: the worker threads must see committed setup data and
 * drive their own service-level transactions, which is what creates the contention.
 *
 * @author Morten Svanæs
 */
@TestInstance(Lifecycle.PER_CLASS)
class TrackedEntityAuditConcurrencyTest extends PostgresIntegrationTestBase {

  @Autowired private EnrollmentService enrollmentService;
  @Autowired private TrackedEntityAuditService auditService;
  @Autowired private TestSetup testSetup;

  private static final int THREADS = 8;
  private static final int READS_PER_THREAD = 5;
  private static final int EXPECTED_AUDITS = THREADS * READS_PER_THREAD;

  private final UID program = UID.of("BFcipDERJnf");
  private final UID trackedEntity = UID.of("QS6w44flWAf");

  private UserDetails importUser;
  private TrackedEntityAuditQueryParams params;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();
    User user = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(user);
    testSetup.importTrackerData();

    importUser = UserDetails.fromUser(userService.getUser("tTgjgobT1oS"));
    params = new TrackedEntityAuditQueryParams();
    params.setTrackedEntities(List.of(trackedEntity.getValue()));
    params.setAuditTypes(List.of(READ));
  }

  @Test
  void shouldNotLoseAuditsWhenReadingConcurrently() throws Exception {
    int countBefore = auditService.getTrackedEntityAuditsCount(params);

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .program(program)
            .trackedEntities(Set.of(trackedEntity))
            .build();

    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(THREADS);
    List<Throwable> failures = new CopyOnWriteArrayList<>();

    try {
      for (int i = 0; i < THREADS; i++) {
        pool.submit(
            () -> {
              try {
                injectSecurityContextNoSettings(importUser);
                start.await();
                for (int n = 0; n < READS_PER_THREAD; n++) {
                  enrollmentService.findEnrollments(operationParams);
                }
              } catch (Throwable t) {
                failures.add(t);
              } finally {
                done.countDown();
              }
            });
      }

      start.countDown();
      assertTrue(done.await(2, TimeUnit.MINUTES), "concurrent reads did not finish in time");
    } finally {
      pool.shutdownNow();
    }

    assertEquals(List.of(), failures, "concurrent reads failed");

    await()
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                assertEquals(
                    countBefore + EXPECTED_AUDITS,
                    auditService.getTrackedEntityAuditsCount(params),
                    "every concurrent read must produce exactly one audit record"));
  }
}
