/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Set;
import org.hisp.dhis.cache.ETagService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * TG1: Proves that composite ETag generation is deterministic regardless of Set iteration order.
 * Before the C1 fix, Set.of() non-deterministic iteration could produce different ETags for the
 * same set of entity types.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompositeETagIterationOrderTest {

  @Mock private ETagService eTagService;

  private UserDetails userDetails;
  private ConditionalETagService service;

  @BeforeEach
  void setUp() {
    service = new ConditionalETagService(eTagService);
    User user = new User();
    user.setUid("testUser123");
    user.setUsername("testuser");
    userDetails = UserDetails.fromUser(user);

    when(eTagService.getAllCacheVersion()).thenReturn(7L);
    when(eTagService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(10L);
    when(eTagService.getEntityTypeVersion(User.class)).thenReturn(20L);
    when(eTagService.getEntityTypeVersion(UserRole.class)).thenReturn(30L);
    when(eTagService.getEntityTypeVersion(UserGroup.class)).thenReturn(40L);
    when(eTagService.getTtlMinutes()).thenReturn(60);
  }

  @Test
  @DisplayName("Same set called twice produces same ETag")
  void sameSetProducesSameETag() {
    Set<Class<?>> types = Set.of(OrganisationUnit.class, User.class, UserRole.class);
    String etag1 = service.generateETag(userDetails, types);
    String etag2 = service.generateETag(userDetails, types);
    assertEquals(etag1, etag2);
  }

  @Test
  @DisplayName("Explicitly reversed insertion order produces same ETag")
  void reversedOrderProducesSameETag() {
    LinkedHashSet<Class<?>> order1 = new LinkedHashSet<>();
    order1.add(User.class);
    order1.add(UserRole.class);
    order1.add(OrganisationUnit.class);

    LinkedHashSet<Class<?>> order2 = new LinkedHashSet<>();
    order2.add(OrganisationUnit.class);
    order2.add(UserRole.class);
    order2.add(User.class);

    String etag1 = service.generateETag(userDetails, order1);
    String etag2 = service.generateETag(userDetails, order2);
    assertEquals(etag1, etag2, "ETags must be identical regardless of insertion order");
  }

  @RepeatedTest(20)
  @DisplayName("Set.of() with 4 types always produces same ETag across repeated calls")
  void repeatedSetOfProducesSameETag() {
    // Set.of() can have non-deterministic iteration order between JVM instances
    // and even between calls in some JVM versions. Repeat to catch non-determinism.
    Set<Class<?>> types =
        Set.of(OrganisationUnit.class, User.class, UserRole.class, UserGroup.class);
    String etag1 = service.generateETag(userDetails, types);
    String etag2 = service.generateETag(userDetails, types);
    assertEquals(etag1, etag2);
  }

  @Test
  @DisplayName("Single-element set is stable")
  void singleElementSetIsStable() {
    Set<Class<?>> types = Set.of(OrganisationUnit.class);
    String etag1 = service.generateETag(userDetails, types);
    String etag2 = service.generateETag(userDetails, types);
    assertEquals(etag1, etag2);
  }
}
