/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 */
package org.hisp.dhis.user.authz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultAuthzServiceTest {

  @Mock private UserService userService;

  private InMemoryAuthzVersionStore store;
  private DefaultAuthzService authzService;

  @BeforeEach
  void setUp() {
    store = new InMemoryAuthzVersionStore();
    authzService = new DefaultAuthzService(store, userService);
  }

  @Test
  void effectiveGenIsMaxOfUserAndRoleGens() {
    UserDetails principal =
        UserDetails.empty().username("alice").userRoleIds(Set.of("r1", "r2")).build();

    store.bumpUserGen("alice"); // 1
    store.bumpRoleGen("r1"); // 1
    store.bumpRoleGen("r2"); // 1
    store.bumpRoleGen("r2"); // 2

    assertEquals(2L, authzService.effectiveGen(principal));
  }

  @Test
  void roleBumpIsO1AndDoesNotTouchUserGen() {
    assertEquals(1L, authzService.bumpRoleAuthz("r1"));
    assertEquals(0L, store.getUserGen("alice"));
    assertEquals(1L, store.getRoleGen("r1"));
  }

  @Test
  void ensureFreshRebuildsAfterRoleBump() {
    UserDetails v1 =
        UserDetails.empty()
            .username("alice")
            .userRoleIds(Set.of("r1"))
            .allAuthorities(Set.of("F_OLD"))
            .build();
    UserDetails v2 =
        UserDetails.empty()
            .username("alice")
            .userRoleIds(Set.of("r1"))
            .allAuthorities(Set.of("F_NEW"))
            .build();

    when(userService.createUserDetailsByUsername("alice")).thenReturn(v1, v2);

    UserDetails first = authzService.ensureFresh(v1);
    assertEquals(Set.of("F_OLD"), first.getAllAuthorities());

    authzService.bumpRoleAuthz("r1");
    UserDetails second = authzService.ensureFresh(v1);
    assertEquals(Set.of("F_NEW"), second.getAllAuthorities());
    assertNotSame(first, second);
    verify(userService, times(2)).createUserDetailsByUsername("alice");
  }

  @Test
  void ensureFreshUsesCacheForSameEffectiveGen() {
    UserDetails v1 =
        UserDetails.empty().username("alice").userRoleIds(Set.of("r1")).build();
    when(userService.createUserDetailsByUsername("alice")).thenReturn(v1);

    UserDetails a = authzService.ensureFresh(v1);
    UserDetails b = authzService.ensureFresh(v1);
    assertSame(a, b);
    verify(userService, times(1)).createUserDetailsByUsername("alice");
    assertTrue(authzService.metric("authz.soft_refresh") >= 1);
  }
}
