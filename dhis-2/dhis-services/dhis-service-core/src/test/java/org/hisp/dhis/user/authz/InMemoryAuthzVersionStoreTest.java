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

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Svanæs
 */
class InMemoryAuthzVersionStoreTest {

  private InMemoryAuthzVersionStore store;

  @BeforeEach
  void setUp() {
    store = new InMemoryAuthzVersionStore();
  }

  @Test
  void missingKeysAreZero() {
    assertEquals(0L, store.getEpoch());
    assertEquals(0L, store.getMaxGen("u1", Set.of()));
    assertEquals(0L, store.getMaxGen("u1", Set.of("r1", "r2")));
  }

  @Test
  void userAndRoleBumpsAreIndependent() {
    store.bumpUserGen("u1");
    store.bumpRoleGen("r1");

    assertEquals(1L, store.getMaxGen("u1", Set.of()));
    assertEquals(1L, store.getMaxGen("other", Set.of("r1")));
    assertEquals(1L, store.getMaxGen("u1", Set.of("r1")));
    assertEquals(0L, store.getMaxGen("u2", Set.of("r2")));
  }

  @Test
  void everyBumpMovesEpoch() {
    assertEquals(0L, store.getEpoch());
    store.bumpUserGen("u1");
    assertEquals(1L, store.getEpoch());
    store.bumpRoleGen("r1");
    assertEquals(2L, store.getEpoch());
    store.bumpUserGen("u1");
    assertEquals(3L, store.getEpoch());
  }

  @Test
  void batchBumpIsDistinctAndAdvancesEpochOnce() {
    store.bumpUserGens(java.util.Arrays.asList("u2", "u1", "u1", "", "  ", null, "u2"));

    assertEquals(1L, store.getEpoch());
    assertEquals(1L, store.getMaxGen("u1", Set.of()));
    assertEquals(1L, store.getMaxGen("u2", Set.of()));
    assertEquals(0L, store.getMaxGen("u3", Set.of()));
  }

  @Test
  void getMaxGenPicksMaxAcrossUserAndRoles() {
    store.bumpUserGen("u1"); // user=1 epoch=1
    store.bumpRoleGen("r1"); // role=1 epoch=2
    store.bumpRoleGen("r2"); // role=1 epoch=3
    store.bumpRoleGen("r2"); // role=2 epoch=4

    assertEquals(2L, store.getMaxGen("u1", Set.of("r1", "r2")));
    assertEquals(1L, store.getMaxGen("u1", Set.of("r1")));
    assertEquals(2L, store.getMaxGen("unknown", Set.of("r2")));
  }
}
