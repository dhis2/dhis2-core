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

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.NotFoundException;
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
  void shouldGetPotentialDuplicatesByStatus() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate1 =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    deduplicationService.addPotentialDuplicate(potentialDuplicate1);
    potentialDuplicate1.setStatus(DeduplicationStatus.INVALID);
    deduplicationService.updatePotentialDuplicate(potentialDuplicate1);
    PotentialDuplicate potentialDuplicate2 =
        new PotentialDuplicate(TRACKED_ENTITY_C, TRACKED_ENTITY_D);
    deduplicationService.addPotentialDuplicate(potentialDuplicate2);

    PotentialDuplicateCriteria criteria = new PotentialDuplicateCriteria();
    criteria.setTrackedEntities(List.of(TRACKED_ENTITY_A, TRACKED_ENTITY_C));
    criteria.setStatus(DeduplicationStatus.OPEN);

    assertContainsOnly(
        List.of(potentialDuplicate2), deduplicationService.getPotentialDuplicates(criteria));

    criteria.setStatus(DeduplicationStatus.INVALID);

    assertContainsOnly(
        List.of(potentialDuplicate1), deduplicationService.getPotentialDuplicates(criteria));

    criteria.setStatus(DeduplicationStatus.ALL);

    assertContainsOnly(
        List.of(potentialDuplicate1, potentialDuplicate2),
        deduplicationService.getPotentialDuplicates(criteria));
  }

  @Test
  void shouldAddPotentialDuplicate() throws PotentialDuplicateConflictException, NotFoundException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);

    deduplicationService.addPotentialDuplicate(potentialDuplicate);

    assertEquals(
        potentialDuplicate,
        deduplicationService.getPotentialDuplicateByUid(UID.of(potentialDuplicate.getUid())));
  }

  @Test
  void shouldFailAddingPotentialDuplicateGivenInvalidSatus() {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    potentialDuplicate.setStatus(DeduplicationStatus.ALL);

    PotentialDuplicateConflictException exception =
        assertThrows(
            PotentialDuplicateConflictException.class,
            () -> deduplicationService.addPotentialDuplicate(potentialDuplicate));

    assertStartsWith("Invalid status", exception.getMessage());
  }

  @Test
  void shouldSucceedCheckingExistence() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);

    deduplicationService.addPotentialDuplicate(potentialDuplicate);

    assertTrue(deduplicationService.exists(potentialDuplicate));
  }

  @Test
  void shouldFailCheckingExistenceIfNoDuplicateIsGiven()
      throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);

    deduplicationService.addPotentialDuplicate(potentialDuplicate);

    assertThrows(
        PotentialDuplicateConflictException.class,
        () -> deduplicationService.exists(new PotentialDuplicate(TRACKED_ENTITY_A, null)));
  }

  @Test
  void shouldFailCheckingExistenceIfNoOriginalIsGiven() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);

    deduplicationService.addPotentialDuplicate(potentialDuplicate);

    assertThrows(
        PotentialDuplicateConflictException.class,
        () -> deduplicationService.exists(new PotentialDuplicate(null, TRACKED_ENTITY_B)));
  }

  @Test
  void shouldFindExistingDuplicateIfGivenDuplicateInReverseOrder()
      throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    PotentialDuplicate potentialDuplicateReverse =
        new PotentialDuplicate(TRACKED_ENTITY_B, TRACKED_ENTITY_A);

    deduplicationService.addPotentialDuplicate(potentialDuplicate);

    assertTrue(deduplicationService.exists(potentialDuplicateReverse));
  }

  @Test
  void shouldGetAllPotentialDuplicatedByTrackedEntities()
      throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_B);
    PotentialDuplicate potentialDuplicate1 =
        new PotentialDuplicate(TRACKED_ENTITY_C, TRACKED_ENTITY_D);
    PotentialDuplicate potentialDuplicate2 =
        new PotentialDuplicate(TRACKED_ENTITY_A, TRACKED_ENTITY_D);
    deduplicationService.addPotentialDuplicate(potentialDuplicate);
    deduplicationService.addPotentialDuplicate(potentialDuplicate1);
    deduplicationService.addPotentialDuplicate(potentialDuplicate2);

    PotentialDuplicateCriteria criteria = new PotentialDuplicateCriteria();
    criteria.setTrackedEntities(List.of(TRACKED_ENTITY_A));

    List<PotentialDuplicate> list = deduplicationService.getPotentialDuplicates(criteria);

    assertContainsOnly(List.of(potentialDuplicate, potentialDuplicate2), list);
  }

  @Test
  void shouldUpdatePotentialDuplicate() throws PotentialDuplicateConflictException {
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
