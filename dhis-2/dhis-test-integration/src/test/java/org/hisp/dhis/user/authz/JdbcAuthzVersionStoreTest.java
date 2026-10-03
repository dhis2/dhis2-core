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
package org.hisp.dhis.user.authz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validates {@link JdbcAuthzVersionStore} ON CONFLICT SQL against real Postgres.
 *
 * @author Morten Svanæs
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class JdbcAuthzVersionStoreTest extends PostgresIntegrationTestBase {

  @Autowired private AuthzVersionStore store;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void wipeStore() {
    // Other suites in the shared CI database commit bumps; absolute gen/epoch assertions below
    // require a clean slate. Runs inside the test transaction, so it is rolled back afterwards.
    jdbcTemplate.update("delete from authz_version");
  }

  @Test
  void epochStartsAtZero() {
    assertEquals(0L, store.getEpoch());
  }

  @Test
  void bumpUserGenAdvancesUserGenAndEpoch() {
    long epochBefore = store.getEpoch();
    store.bumpUserGen("userA");

    assertEquals(epochBefore + 1, store.getEpoch());
    assertEquals(1L, store.getMaxGen("userA", Set.of()));
  }

  @Test
  void bumpRoleGenIsIndependent() {
    store.bumpUserGen("userA");
    store.bumpRoleGen("roleA");

    assertEquals(1L, store.getMaxGen("userA", Set.of()));
    assertEquals(1L, store.getMaxGen("other", Set.of("roleA")));
    assertEquals(1L, store.getMaxGen("userA", Set.of("roleA")));
  }

  @Test
  void repeatedBumpsIncrementViaUpsertConflictPath() {
    store.bumpUserGen("userB");
    store.bumpUserGen("userB");
    store.bumpUserGen("userB");

    assertEquals(3L, store.getMaxGen("userB", Set.of()));
    assertEquals(3L, store.getEpoch());
  }

  @Test
  void bumpUserGensDeduplicatesAndAdvancesEpochOnce() {
    long epochBefore = store.getEpoch();
    store.bumpUserGens(List.of("u2", "u1", "u1", "", "  ", "u2"));

    assertEquals(epochBefore + 1, store.getEpoch());
    assertEquals(1L, store.getMaxGen("u1", Set.of()));
    assertEquals(1L, store.getMaxGen("u2", Set.of()));
  }

  @Test
  void getMaxGenAcrossUserAndRoles() {
    store.bumpUserGen("userC"); // user=1
    store.bumpRoleGen("role1"); // role=1
    store.bumpRoleGen("role2"); // role=1
    store.bumpRoleGen("role2"); // role=2

    assertEquals(2L, store.getMaxGen("userC", Set.of("role1", "role2")));
  }

  @Test
  void getMaxGenUnknownKeysIsZero() {
    assertEquals(0L, store.getMaxGen("missing-user", Set.of("missing-role")));
  }
}
