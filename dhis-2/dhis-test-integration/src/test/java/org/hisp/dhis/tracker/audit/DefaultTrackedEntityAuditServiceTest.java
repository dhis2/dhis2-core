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
import static org.hisp.dhis.test.utils.Assertions.assertHasSize;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAudit;
import org.hisp.dhis.trackedentity.TrackedEntityAuditQueryParams;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DefaultTrackedEntityAuditServiceTest extends PostgresIntegrationTestBase {
  @Autowired private EnrollmentService enrollmentService;
  @Autowired private TrackedEntityAuditService auditService;
  @Autowired private TestSetup testSetup;

  private final UID program = UID.of("BFcipDERJnf");
  private final UID trackedEntity = UID.of("QS6w44flWAf");
  private static final Integer TIMEOUT = 5;
  private TrackedEntityAuditQueryParams params;
  private int countBeforeTest;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    testSetup.importTrackerData();

    params = new TrackedEntityAuditQueryParams();
    params.setTrackedEntities(List.of(trackedEntity.getValue()));
    params.setAuditTypes(List.of(READ));
  }

  @BeforeEach
  void beforeEach() {
    countBeforeTest = auditService.getTrackedEntityAuditsCount(params);
  }

  @Test
  void shouldCreateReadAuditWhenGettingEnrollmentAndTrackedEntitySupplied()
      throws ForbiddenException, BadRequestException {
    enrollmentService.findEnrollments(
        EnrollmentOperationParams.builder().program(program).trackedEntity(trackedEntity).build());
    await()
        .atMost(TIMEOUT, TimeUnit.SECONDS)
        .until(() -> auditService.getTrackedEntityAuditsCount(params) == countBeforeTest + 1);

    List<TrackedEntityAudit> actualAudits = auditService.getTrackedEntityAudits(params);
    assertHasSize(countBeforeTest + 1, actualAudits);
    TrackedEntityAudit expectedAudit =
        new TrackedEntityAudit(
            trackedEntity.getValue(), CurrentUserUtil.getCurrentUsername(), READ);
    assertAudit(expectedAudit, actualAudits.get(0));
  }

  @Test
  void shouldNotCreateReadAuditWhenGettingEnrollmentAndTrackedEntityNotSupplied()
      throws ForbiddenException, BadRequestException {
    enrollmentService.findEnrollments(EnrollmentOperationParams.builder().program(program).build());

    assertEquals(countBeforeTest, auditService.getTrackedEntityAudits(params).size());
  }

  @Test
  void shouldNotCreateReadAuditWhenNotGettingEnrollmentAndTrackedEntitySupplied()
      throws ForbiddenException, BadRequestException {
    List<Enrollment> enrollments =
        enrollmentService.findEnrollments(
            EnrollmentOperationParams.builder()
                .program(program)
                .trackedEntity(trackedEntity)
                .enrollments(Set.of(UID.generate()))
                .build());

    assertIsEmpty(enrollments);
    assertEquals(countBeforeTest, auditService.getTrackedEntityAudits(params).size());
  }

  private void assertAudit(TrackedEntityAudit expected, TrackedEntityAudit actual) {
    assertEquals(expected.getTrackedEntity(), actual.getTrackedEntity());
    assertEquals(expected.getAuditType(), actual.getAuditType());
    assertEquals(expected.getAccessedBy(), actual.getAccessedBy());
  }
}
