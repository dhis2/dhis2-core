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
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class TrackedEntityAuditStoreTest extends SingleSetupIntegrationTestBase {
  private final Date CREATED = getDate(2022, 3, 1);

  @Autowired private TrackedEntityChangeLogStore store;

  private final TrackedEntityChangeLog auditA =
      new TrackedEntityChangeLog("WGW7UnVcIIb", "Access", CREATED, "userA", ChangeLogType.CREATE);
  private final TrackedEntityChangeLog auditB =
      new TrackedEntityChangeLog("WGW7UnVcIIb", "Access", CREATED, "userB", ChangeLogType.UPDATE);
  private final TrackedEntityChangeLog auditC =
      new TrackedEntityChangeLog("zIAwTY3Drrn", "Access", CREATED, "userA", ChangeLogType.UPDATE);
  private final TrackedEntityChangeLog auditD =
      new TrackedEntityChangeLog("zIAwTY3Drrn", "Access", CREATED, "userB", ChangeLogType.DELETE);

  @Test
  void shouldAuditTrackedEntity_whenAddAuditList() {
    List<TrackedEntityChangeLog> trackedEntityAuditInput = List.of(auditA, auditB);

    store.addTrackedEntityChangeLog(trackedEntityAuditInput);

    TrackedEntityChangeLogQueryParams params =
        new TrackedEntityChangeLogQueryParams().setTrackedEntities(List.of("WGW7UnVcIIb"));

    List<TrackedEntityChangeLog> trackedEntityAudits = store.getTrackedEntityChangeLogs(params);

    assertEquals(trackedEntityAuditInput.size(), trackedEntityAudits.size());
    TrackedEntityChangeLog entityAudit =
        filterByAuditType(trackedEntityAudits, ChangeLogType.CREATE);

    assertNotNull(entityAudit);
    assertEquals("userA", entityAudit.getAccessedBy());
    assertEquals("WGW7UnVcIIb", entityAudit.getTrackedEntity());

    entityAudit = filterByAuditType(trackedEntityAudits, ChangeLogType.UPDATE);

    assertNotNull(entityAudit);
    assertEquals("userB", entityAudit.getAccessedBy());
    assertEquals("WGW7UnVcIIb", entityAudit.getTrackedEntity());
  }

  private static TrackedEntityChangeLog filterByAuditType(
      List<TrackedEntityChangeLog> trackedEntityAuditsStore, ChangeLogType changeLogType) {
    return trackedEntityAuditsStore.stream()
        .filter(a -> a.getAuditType() == changeLogType)
        .findFirst()
        .orElse(null);
  }

  @Test
  void testGetAuditsByParams() {

    store.addTrackedEntityChangeLog(auditA);
    store.addTrackedEntityChangeLog(auditB);
    store.addTrackedEntityChangeLog(auditC);
    store.addTrackedEntityChangeLog(auditD);

    TrackedEntityChangeLogQueryParams params =
        new TrackedEntityChangeLogQueryParams().setTrackedEntities(List.of("WGW7UnVcIIb"));

    assertContainsOnly(List.of(auditA, auditB), store.getTrackedEntityChangeLogs(params));

    params = new TrackedEntityChangeLogQueryParams().setUsers(List.of("userA"));

    assertContainsOnly(List.of(auditA, auditC), store.getTrackedEntityChangeLogs(params));

    params = new TrackedEntityChangeLogQueryParams().setAuditTypes(List.of(ChangeLogType.UPDATE));

    assertContainsOnly(List.of(auditB, auditC), store.getTrackedEntityChangeLogs(params));

    params =
        new TrackedEntityChangeLogQueryParams()
            .setAuditTypes(List.of(ChangeLogType.CREATE, ChangeLogType.DELETE));

    assertContainsOnly(List.of(auditA, auditD), store.getTrackedEntityChangeLogs(params));

    params =
        new TrackedEntityChangeLogQueryParams()
            .setTrackedEntities(List.of("WGW7UnVcIIb"))
            .setUsers(List.of("userA"));

    assertContainsOnly(List.of(auditA), store.getTrackedEntityChangeLogs(params));
  }
}
