/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 */
package org.hisp.dhis.user.authz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryAuthzVersionStoreTest {

  private InMemoryAuthzVersionStore store;

  @BeforeEach
  void setUp() {
    store = new InMemoryAuthzVersionStore();
  }

  @Test
  void missingKeysAreZero() {
    assertEquals(0L, store.getUserGen("alice"));
    assertEquals(0L, store.getRoleGen("roleA"));
  }

  @Test
  void bumpUserIncrements() {
    assertEquals(1L, store.bumpUserGen("alice"));
    assertEquals(2L, store.bumpUserGen("alice"));
    assertEquals(2L, store.getUserGen("alice"));
    assertEquals(0L, store.getUserGen("bob"));
  }

  @Test
  void bumpRoleIncrementsIndependently() {
    assertEquals(1L, store.bumpRoleGen("roleA"));
    assertEquals(1L, store.bumpRoleGen("roleB"));
    assertEquals(2L, store.bumpRoleGen("roleA"));
    assertEquals(2L, store.getRoleGen("roleA"));
    assertEquals(1L, store.getRoleGen("roleB"));
  }

  @Test
  void bumpUserGensBatch() {
    store.bumpUserGens(List.of("a", "b", "a"));
    assertEquals(2L, store.getUserGen("a"));
    assertEquals(1L, store.getUserGen("b"));
  }
}
