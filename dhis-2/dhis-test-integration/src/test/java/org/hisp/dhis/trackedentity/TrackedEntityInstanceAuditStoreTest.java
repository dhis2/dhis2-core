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

  @Test
  void testGetAuditsByParams() {
    TrackedEntityInstanceAudit teiaA =
        new TrackedEntityInstanceAudit("WGW7UnVcIIb", "Access", CREATED, "userA", AuditType.CREATE);
    TrackedEntityInstanceAudit teiaB =
        new TrackedEntityInstanceAudit("WGW7UnVcIIb", "Access", CREATED, "userB", AuditType.UPDATE);
    TrackedEntityInstanceAudit teiaC =
        new TrackedEntityInstanceAudit("zIAwTY3Drrn", "Access", CREATED, "userA", AuditType.UPDATE);
    TrackedEntityInstanceAudit teiaD =
        new TrackedEntityInstanceAudit("zIAwTY3Drrn", "Access", CREATED, "userB", AuditType.DELETE);

    store.addTrackedEntityInstanceAudit(teiaA);
    store.addTrackedEntityInstanceAudit(teiaB);
    store.addTrackedEntityInstanceAudit(teiaC);
    store.addTrackedEntityInstanceAudit(teiaD);

    TrackedEntityInstanceAuditQueryParams params =
        new TrackedEntityInstanceAuditQueryParams()
            .setTrackedEntityInstances(List.of("WGW7UnVcIIb"));

    assertContainsOnly(List.of(teiaA, teiaB), store.getTrackedEntityInstanceAudits(params));

    params = new TrackedEntityInstanceAuditQueryParams().setUsers(List.of("userA"));

    assertContainsOnly(List.of(teiaA, teiaC), store.getTrackedEntityInstanceAudits(params));

    params = new TrackedEntityInstanceAuditQueryParams().setAuditTypes(List.of(AuditType.UPDATE));

    assertContainsOnly(List.of(teiaB, teiaC), store.getTrackedEntityInstanceAudits(params));

    params =
        new TrackedEntityInstanceAuditQueryParams()
            .setAuditTypes(List.of(AuditType.CREATE, AuditType.DELETE));

    assertContainsOnly(List.of(teiaA, teiaD), store.getTrackedEntityInstanceAudits(params));

    params =
        new TrackedEntityInstanceAuditQueryParams()
            .setTrackedEntityInstances(List.of("WGW7UnVcIIb"))
            .setUsers(List.of("userA"));

    assertContainsOnly(List.of(teiaA), store.getTrackedEntityInstanceAudits(params));
  }
}
