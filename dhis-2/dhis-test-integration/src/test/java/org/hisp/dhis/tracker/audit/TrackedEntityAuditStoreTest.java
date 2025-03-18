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
package org.hisp.dhis.tracker.audit;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.audit.AuditOperationType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAudit;
import org.hisp.dhis.trackedentity.TrackedEntityAuditQueryParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class TrackedEntityAuditStoreTest extends PostgresIntegrationTestBase {
  private final Date created = getDate(2022, 3, 1);

  @Autowired private TrackedEntityAuditStore store;

  private final TrackedEntityAudit auditA =
      new TrackedEntityAudit("WGW7UnVcIIb", "Access", created, "userA", AuditOperationType.CREATE);
  private final TrackedEntityAudit auditB =
      new TrackedEntityAudit("WGW7UnVcIIb", "Access", created, "userB", AuditOperationType.UPDATE);
  private final TrackedEntityAudit auditC =
      new TrackedEntityAudit("zIAwTY3Drrn", "Access", created, "userA", AuditOperationType.UPDATE);
  private final TrackedEntityAudit auditD =
      new TrackedEntityAudit("zIAwTY3Drrn", "Access", created, "userB", AuditOperationType.DELETE);

  @Test
  void shouldAuditTrackedEntity_whenAddAuditList() {
    List<TrackedEntityAudit> trackedEntityAuditInput = List.of(auditA, auditB);

    store.addTrackedEntityAudit(trackedEntityAuditInput);

    TrackedEntityAuditQueryParams params =
        new TrackedEntityAuditQueryParams().setTrackedEntities(List.of("WGW7UnVcIIb"));

    List<TrackedEntityAudit> trackedEntityAudits = store.getTrackedEntityAudit(params);

    assertEquals(trackedEntityAuditInput.size(), trackedEntityAudits.size());
    TrackedEntityAudit entityAudit =
        filterByAuditType(trackedEntityAudits, AuditOperationType.CREATE);

    assertNotNull(entityAudit);
    assertEquals("userA", entityAudit.getAccessedBy());
    assertEquals("WGW7UnVcIIb", entityAudit.getTrackedEntity());

    entityAudit = filterByAuditType(trackedEntityAudits, AuditOperationType.UPDATE);

    assertNotNull(entityAudit);
    assertEquals("userB", entityAudit.getAccessedBy());
    assertEquals("WGW7UnVcIIb", entityAudit.getTrackedEntity());
  }

  private static TrackedEntityAudit filterByAuditType(
      List<TrackedEntityAudit> trackedEntityAuditsStore, AuditOperationType auditOperationType) {
    return trackedEntityAuditsStore.stream()
        .filter(a -> a.getAuditType() == auditOperationType)
        .findFirst()
        .orElse(null);
  }

  @Test
  void testGetAuditsByParams() {

    store.addTrackedEntityAudit(auditA);
    store.addTrackedEntityAudit(auditB);
    store.addTrackedEntityAudit(auditC);
    store.addTrackedEntityAudit(auditD);

    TrackedEntityAuditQueryParams params =
        new TrackedEntityAuditQueryParams().setTrackedEntities(List.of("WGW7UnVcIIb"));

    assertContainsOnly(List.of(auditA, auditB), store.getTrackedEntityAudit(params));

    params = new TrackedEntityAuditQueryParams().setUsers(List.of("userA"));

    assertContainsOnly(List.of(auditA, auditC), store.getTrackedEntityAudit(params));

    params = new TrackedEntityAuditQueryParams().setAuditTypes(List.of(AuditOperationType.UPDATE));

    assertContainsOnly(List.of(auditB, auditC), store.getTrackedEntityAudit(params));

    params =
        new TrackedEntityAuditQueryParams()
            .setAuditTypes(List.of(AuditOperationType.CREATE, AuditOperationType.DELETE))
            .setUsers(List.of("userA", "userB", "userC", "userD"));

    assertContainsOnly(List.of(auditA, auditD), store.getTrackedEntityAudit(params));

    params =
        new TrackedEntityAuditQueryParams()
            .setTrackedEntities(List.of("WGW7UnVcIIb"))
            .setUsers(List.of("userA"));

    assertContainsOnly(List.of(auditA), store.getTrackedEntityAudit(params));
  }
}
