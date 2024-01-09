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
package org.hisp.dhis.trackedentity;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.audit.payloads.TrackedEntityInstanceAudit;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class TrackedEntityInstanceAuditStoreTest extends SingleSetupIntegrationTestBase {
  private final Date CREATED = getDate(2022, 3, 1);

  @Autowired private TrackedEntityInstanceAuditStore store;

  private final TrackedEntityInstanceAudit auditA =
      new TrackedEntityInstanceAudit("WGW7UnVcIIb", "Access", CREATED, "userA", AuditType.CREATE);
  private final TrackedEntityInstanceAudit auditB =
      new TrackedEntityInstanceAudit("WGW7UnVcIIb", "Access", CREATED, "userB", AuditType.UPDATE);
  private final TrackedEntityInstanceAudit auditC =
      new TrackedEntityInstanceAudit("zIAwTY3Drrn", "Access", CREATED, "userA", AuditType.UPDATE);
  private final TrackedEntityInstanceAudit auditD =
      new TrackedEntityInstanceAudit("zIAwTY3Drrn", "Access", CREATED, "userB", AuditType.DELETE);

  @Test
  void shouldAuditTrackedEntity_whenAddAuditList() {
    List<TrackedEntityInstanceAudit> trackedEntityAuditInput = List.of(auditA, auditB);

    store.addTrackedEntityInstanceAudit(trackedEntityAuditInput);

    TrackedEntityInstanceAuditQueryParams params =
        new TrackedEntityInstanceAuditQueryParams()
            .setTrackedEntityInstances(List.of("WGW7UnVcIIb"));

    List<TrackedEntityInstanceAudit> trackedEntityAudits =
        store.getTrackedEntityInstanceAudits(params);

    assertEquals(trackedEntityAuditInput.size(), trackedEntityAudits.size());
    TrackedEntityInstanceAudit entityAudit =
        filterByAuditType(trackedEntityAudits, AuditType.CREATE);

    assertNotNull(entityAudit);
    assertEquals("userA", entityAudit.getAccessedBy());
    assertEquals("WGW7UnVcIIb", entityAudit.getTrackedEntityInstance());

    entityAudit = filterByAuditType(trackedEntityAudits, AuditType.UPDATE);

    assertNotNull(entityAudit);
    assertEquals("userB", entityAudit.getAccessedBy());
    assertEquals("WGW7UnVcIIb", entityAudit.getTrackedEntityInstance());
  }

  private static TrackedEntityInstanceAudit filterByAuditType(
      List<TrackedEntityInstanceAudit> trackedEntityAuditsStore, AuditType auditType) {
    return trackedEntityAuditsStore.stream()
        .filter(a -> a.getAuditType() == auditType)
        .findFirst()
        .orElse(null);
  }

  @Test
  void testGetAuditsByParams() {
    store.addTrackedEntityInstanceAudit(auditA);
    store.addTrackedEntityInstanceAudit(auditB);
    store.addTrackedEntityInstanceAudit(auditC);
    store.addTrackedEntityInstanceAudit(auditD);

    TrackedEntityInstanceAuditQueryParams params =
        new TrackedEntityInstanceAuditQueryParams()
            .setTrackedEntityInstances(List.of("WGW7UnVcIIb"));

    assertContainsOnly(List.of(auditA, auditB), store.getTrackedEntityInstanceAudits(params));

    params = new TrackedEntityInstanceAuditQueryParams().setUsers(List.of("userA"));

    assertContainsOnly(List.of(auditA, auditC), store.getTrackedEntityInstanceAudits(params));

    params = new TrackedEntityInstanceAuditQueryParams().setAuditTypes(List.of(AuditType.UPDATE));

    assertContainsOnly(List.of(auditB, auditC), store.getTrackedEntityInstanceAudits(params));

    params =
        new TrackedEntityInstanceAuditQueryParams()
            .setAuditTypes(List.of(AuditType.CREATE, AuditType.DELETE));

    assertContainsOnly(List.of(auditA, auditD), store.getTrackedEntityInstanceAudits(params));

    params =
        new TrackedEntityInstanceAuditQueryParams()
            .setTrackedEntityInstances(List.of("WGW7UnVcIIb"))
            .setUsers(List.of("userA"));

    assertContainsOnly(List.of(auditA), store.getTrackedEntityInstanceAudits(params));
  }
}
