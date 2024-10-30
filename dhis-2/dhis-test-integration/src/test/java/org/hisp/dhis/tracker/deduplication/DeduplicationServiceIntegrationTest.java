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
package org.hisp.dhis.tracker.deduplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DeduplicationServiceIntegrationTest extends PostgresIntegrationTestBase {
  @Autowired private DeduplicationService deduplicationService;

  private static final UID TRACKED_ENTITY_A = UID.generate();

  private static final UID TRACKED_ENTITY_B = UID.generate();

  private static final UID TRACKED_ENTITY_C = UID.generate();

  private static final UID TRACKED_ENTITY_D = UID.generate();

  @BeforeEach
  public void setupTestUser() {
    User user = createUserWithAuth("testUser");
    injectSecurityContextUser(user);
  }

  @Test
  void testGetAllPotentialDuplicateByDifferentStatus() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    deduplicationService.addPotentialDuplicate(potentialDuplicate);
    PotentialDuplicate potentialDuplicate1 =
        new PotentialDuplicate(TRACKED_ENTITY_C, TRACKED_ENTITY_D);
    deduplicationService.addPotentialDuplicate(potentialDuplicate1);

    List<UID> potentialDuplicates = List.of(TRACKED_ENTITY_A, TRACKED_ENTITY_C);

    PotentialDuplicateCriteria criteria = new PotentialDuplicateCriteria();

    criteria.setTrackedEntities(potentialDuplicates);
    assertEquals(
        potentialDuplicates.size(), deduplicationService.getPotentialDuplicates(criteria).size());

    // set one potential duplicate to invalid
    potentialDuplicate.setStatus(DeduplicationStatus.INVALID);
    deduplicationService.updatePotentialDuplicate(potentialDuplicate);

    criteria.setStatus(DeduplicationStatus.OPEN);
    deduplicationService
        .getPotentialDuplicates(criteria)
        .forEach(pd -> assertSame(DeduplicationStatus.OPEN, pd.getStatus()));

    criteria.setStatus(DeduplicationStatus.INVALID);
    deduplicationService
        .getPotentialDuplicates(criteria)
        .forEach(pd -> assertSame(DeduplicationStatus.INVALID, pd.getStatus()));

    criteria.setStatus(DeduplicationStatus.ALL);
    assertEquals(
        potentialDuplicates.size(), deduplicationService.getPotentialDuplicates(criteria).size());
  }

  @Test
  void testAddPotentialDuplicate() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    deduplicationService.addPotentialDuplicate(potentialDuplicate);
    assertNotEquals(0, potentialDuplicate.getId());
    assertEquals(
        potentialDuplicate,
        deduplicationService.getPotentialDuplicateById(potentialDuplicate.getId()));
  }

  @Test
  void testGetPotentialDuplicateByUid() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    deduplicationService.addPotentialDuplicate(potentialDuplicate);
    assertNotEquals(0, potentialDuplicate.getId());
    assertEquals(
        potentialDuplicate,
        deduplicationService.getPotentialDuplicateByUid(UID.of(potentialDuplicate)));
  }

  @Test
  void testGetPotentialDuplicateDifferentStatus() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    deduplicationService.addPotentialDuplicate(potentialDuplicate);
    PotentialDuplicate potentialDuplicate1 =
        new PotentialDuplicate(TRACKED_ENTITY_C, TRACKED_ENTITY_B);
    deduplicationService.addPotentialDuplicate(potentialDuplicate1);

    potentialDuplicate.setStatus(DeduplicationStatus.INVALID);
    deduplicationService.updatePotentialDuplicate(potentialDuplicate);

    potentialDuplicate1.setStatus(DeduplicationStatus.MERGED);
    deduplicationService.updatePotentialDuplicate(potentialDuplicate1);

    PotentialDuplicateCriteria criteria = new PotentialDuplicateCriteria();
    criteria.setTrackedEntities(Collections.singletonList(TRACKED_ENTITY_B));
    criteria.setStatus(DeduplicationStatus.INVALID);
    assertEquals(
        Collections.singletonList(potentialDuplicate),
        deduplicationService.getPotentialDuplicates(criteria));
  }

  @Test
  void testCreatePotentialDuplicateNotCreationStatus() {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    potentialDuplicate.setStatus(DeduplicationStatus.ALL);

    assertThrows(
        PotentialDuplicateConflictException.class,
        () -> deduplicationService.addPotentialDuplicate(potentialDuplicate));
  }

  @Test
  void testExistsDuplicate() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    deduplicationService.addPotentialDuplicate(potentialDuplicate);
    assertTrue(deduplicationService.exists(potentialDuplicate));
  }

  @Test
  void testShouldThrowWhenMissingTrackedEntityBProperty()
      throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    deduplicationService.addPotentialDuplicate(potentialDuplicate);
    assertThrows(
        PotentialDuplicateConflictException.class,
        () -> deduplicationService.exists(new PotentialDuplicate(TRACKED_ENTITY_A, null)));
  }

  @Test
  void testShouldThrowWhenMissingTrackedEntityAProperty()
      throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    deduplicationService.addPotentialDuplicate(potentialDuplicate);
    assertThrows(
        PotentialDuplicateConflictException.class,
        () -> deduplicationService.exists(new PotentialDuplicate(null, TRACKED_ENTITY_B)));
  }

  @Test
  void testExistsTwoTrackedEntitiesReverse() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    PotentialDuplicate potentialDuplicateReverse =
        new PotentialDuplicate(TRACKED_ENTITY_B, TRACKED_ENTITY_A);
    deduplicationService.addPotentialDuplicate(potentialDuplicate);
    assertTrue(deduplicationService.exists(potentialDuplicateReverse));
  }

  @Test
  void testGetAllPotentialDuplicatedByQuery() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    PotentialDuplicate potentialDuplicate1 =
        new PotentialDuplicate(TRACKED_ENTITY_C, TRACKED_ENTITY_D);
    PotentialDuplicate potentialDuplicate2 =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_D);
    PotentialDuplicateCriteria criteria = new PotentialDuplicateCriteria();
    deduplicationService.addPotentialDuplicate(potentialDuplicate);
    deduplicationService.addPotentialDuplicate(potentialDuplicate1);
    deduplicationService.addPotentialDuplicate(potentialDuplicate2);
    criteria.setTrackedEntities(Collections.singletonList(TRACKED_ENTITY_A));
    List<PotentialDuplicate> list = deduplicationService.getPotentialDuplicates(criteria);
    assertEquals(2, list.size());
    assertTrue(list.contains(potentialDuplicate));
    assertFalse(list.contains(potentialDuplicate1));
  }

  @Test
  void testCountPotentialDuplicates() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    PotentialDuplicate potentialDuplicate1 =
        new PotentialDuplicate(TRACKED_ENTITY_C, TRACKED_ENTITY_D);
    PotentialDuplicateCriteria criteria = new PotentialDuplicateCriteria();
    deduplicationService.addPotentialDuplicate(potentialDuplicate);
    deduplicationService.addPotentialDuplicate(potentialDuplicate1);
    criteria.setStatus(DeduplicationStatus.ALL);
    assertEquals(2, deduplicationService.countPotentialDuplicates(criteria));
    criteria.setStatus(DeduplicationStatus.OPEN);
    criteria.setTrackedEntities(Arrays.asList(TRACKED_ENTITY_A, TRACKED_ENTITY_C));
    assertEquals(2, deduplicationService.countPotentialDuplicates(criteria));
    criteria.setTrackedEntities(Collections.singletonList(TRACKED_ENTITY_C));
    assertEquals(1, deduplicationService.countPotentialDuplicates(criteria));
    criteria.setStatus(DeduplicationStatus.INVALID);
    assertEquals(0, deduplicationService.countPotentialDuplicates(criteria));
  }

  @Test
  void testUpdatePotentialDuplicate() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    deduplicationService.addPotentialDuplicate(potentialDuplicate);
    assertEquals(
        DeduplicationStatus.OPEN,
        deduplicationService.getPotentialDuplicateById(potentialDuplicate.getId()).getStatus());
    potentialDuplicate.setStatus(DeduplicationStatus.INVALID);
    deduplicationService.updatePotentialDuplicate(potentialDuplicate);
    assertEquals(
        DeduplicationStatus.INVALID,
        deduplicationService.getPotentialDuplicateById(potentialDuplicate.getId()).getStatus());
  }

  @Test
  void shouldThrowWhenOrderFieldNotExists() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    deduplicationService.addPotentialDuplicate(potentialDuplicate);

    PotentialDuplicateCriteria criteria = new PotentialDuplicateCriteria();
    criteria.setOrder(List.of(OrderCriteria.of("field", SortDirection.ASC)));
    assertThrows(
        IllegalArgumentException.class,
        () -> deduplicationService.getPotentialDuplicates(criteria));
  }
}
