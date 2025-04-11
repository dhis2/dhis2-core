/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.deduplication;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DeduplicationServiceIntegrationTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private DeduplicationService deduplicationService;

  private UID trackedEntityAOriginal;
  private UID trackedEntityADuplicate;
  private UID trackedEntityBOriginal;
  private UID trackedEntityBDuplicate;
  private UID trackedEntityCOriginal;
  private UID trackedEntityCDuplicate;

  @BeforeEach
  void setUp() throws IOException {
    testSetup.importMetadata();

    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    TrackerObjects trackerObjects = testSetup.importTrackerData();
    TrackerObjects duplicateTrackedEntities =
        testSetup.importTrackerData("tracker/deduplication/potential_duplicates.json");

    trackedEntityAOriginal = testSetup.getTrackedEntity(trackerObjects, "QS6w44flWAf").getUid();
    trackedEntityADuplicate =
        testSetup.getTrackedEntity(duplicateTrackedEntities, "DS6w44flWAf").getUid();
    trackedEntityBOriginal = testSetup.getTrackedEntity(trackerObjects, "dUE514NMOlo").getUid();
    trackedEntityBDuplicate =
        testSetup.getTrackedEntity(duplicateTrackedEntities, "DUE514NMOlo").getUid();
    trackedEntityCOriginal = testSetup.getTrackedEntity(trackerObjects, "mHWCacsGYYn").getUid();
    trackedEntityCDuplicate =
        testSetup.getTrackedEntity(duplicateTrackedEntities, "DHWCacsGYYn").getUid();
  }

  @Test
  void shouldAddPotentialDuplicate() throws PotentialDuplicateConflictException, NotFoundException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(trackedEntityAOriginal, trackedEntityADuplicate);

    deduplicationService.addPotentialDuplicate(potentialDuplicate);

    assertEquals(
        potentialDuplicate,
        deduplicationService.getPotentialDuplicate(UID.of(potentialDuplicate.getUid())));
  }

  @Test
  void shouldFailAddingPotentialDuplicateGivenInvalidSatus() {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(trackedEntityAOriginal, trackedEntityADuplicate);
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
        new PotentialDuplicate(trackedEntityAOriginal, trackedEntityADuplicate);

    deduplicationService.addPotentialDuplicate(potentialDuplicate);

    assertTrue(deduplicationService.exists(potentialDuplicate));
  }

  @Test
  void shouldFailCheckingExistenceIfNoDuplicateIsGiven()
      throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(trackedEntityAOriginal, trackedEntityADuplicate);

    deduplicationService.addPotentialDuplicate(potentialDuplicate);

    assertThrows(
        PotentialDuplicateConflictException.class,
        () -> deduplicationService.exists(new PotentialDuplicate(trackedEntityAOriginal, null)));
  }

  @Test
  void shouldFailCheckingExistenceIfNoOriginalIsGiven() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(trackedEntityAOriginal, trackedEntityADuplicate);

    deduplicationService.addPotentialDuplicate(potentialDuplicate);

    assertThrows(
        PotentialDuplicateConflictException.class,
        () -> deduplicationService.exists(new PotentialDuplicate(null, trackedEntityADuplicate)));
  }

  @Test
  void shouldFindExistingDuplicateIfGivenDuplicateInReverseOrder()
      throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(trackedEntityAOriginal, trackedEntityADuplicate);
    PotentialDuplicate potentialDuplicateReverse =
        new PotentialDuplicate(trackedEntityADuplicate, trackedEntityAOriginal);

    deduplicationService.addPotentialDuplicate(potentialDuplicate);

    assertTrue(deduplicationService.exists(potentialDuplicateReverse));
  }

  @Test
  void shouldGetPotentialDuplicatesByTrackedEntities() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate1 =
        new PotentialDuplicate(trackedEntityAOriginal, trackedEntityADuplicate);
    PotentialDuplicate potentialDuplicate2 =
        new PotentialDuplicate(trackedEntityBOriginal, trackedEntityBDuplicate);
    PotentialDuplicate potentialDuplicate3 =
        new PotentialDuplicate(trackedEntityAOriginal, trackedEntityCDuplicate);
    deduplicationService.addPotentialDuplicate(potentialDuplicate1);
    deduplicationService.addPotentialDuplicate(potentialDuplicate2);
    deduplicationService.addPotentialDuplicate(potentialDuplicate3);

    PotentialDuplicateCriteria criteria = new PotentialDuplicateCriteria();
    criteria.setTrackedEntities(List.of(trackedEntityAOriginal));

    List<PotentialDuplicate> list = deduplicationService.getPotentialDuplicates(criteria);

    assertContainsOnly(List.of(potentialDuplicate1, potentialDuplicate3), list);
  }

  @Test
  void shouldGetPotentialDuplicatesByStatus() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate1 =
        new PotentialDuplicate(trackedEntityAOriginal, trackedEntityADuplicate);
    deduplicationService.addPotentialDuplicate(potentialDuplicate1);
    potentialDuplicate1.setStatus(DeduplicationStatus.INVALID);
    deduplicationService.updatePotentialDuplicate(potentialDuplicate1);
    PotentialDuplicate potentialDuplicate2 =
        new PotentialDuplicate(trackedEntityBOriginal, trackedEntityBDuplicate);
    deduplicationService.addPotentialDuplicate(potentialDuplicate2);

    PotentialDuplicateCriteria criteria = new PotentialDuplicateCriteria();
    criteria.setTrackedEntities(List.of(trackedEntityAOriginal, trackedEntityBOriginal));
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
  void shouldGetPaginatedPotentialDuplicatesGivenNonDefaultPageSize()
      throws PotentialDuplicateConflictException, BadRequestException {
    PotentialDuplicate potentialDuplicate1 =
        new PotentialDuplicate(trackedEntityAOriginal, trackedEntityADuplicate);
    PotentialDuplicate potentialDuplicate2 =
        new PotentialDuplicate(trackedEntityBOriginal, trackedEntityBDuplicate);
    PotentialDuplicate potentialDuplicate3 =
        new PotentialDuplicate(trackedEntityCOriginal, trackedEntityCDuplicate);
    deduplicationService.addPotentialDuplicate(potentialDuplicate1);
    deduplicationService.addPotentialDuplicate(potentialDuplicate2);
    deduplicationService.addPotentialDuplicate(potentialDuplicate3);

    PotentialDuplicateCriteria criteria = new PotentialDuplicateCriteria();
    criteria.setOrder(List.of(OrderCriteria.of("original", SortDirection.ASC)));
    criteria.setTrackedEntities(
        List.of(trackedEntityAOriginal, trackedEntityBOriginal, trackedEntityCOriginal));

    Page<UID> firstPage =
        deduplicationService
            .getPotentialDuplicates(criteria, PageParams.of(1, 2, false))
            .withMappedItems(PotentialDuplicate::getOriginal);

    assertEquals(
        new Page<>(List.of(trackedEntityAOriginal, trackedEntityBOriginal), 1, 2, null, null, 2),
        firstPage,
        "first page");

    Page<UID> secondPage =
        deduplicationService
            .getPotentialDuplicates(criteria, PageParams.of(2, 2, false))
            .withMappedItems(PotentialDuplicate::getOriginal);

    assertEquals(
        new Page<>(List.of(trackedEntityCOriginal), 2, 2, null, 1, null),
        secondPage,
        "second (last) page");

    Page<UID> thirdPage =
        deduplicationService
            .getPotentialDuplicates(criteria, PageParams.of(3, 3, false))
            .withMappedItems(PotentialDuplicate::getOriginal);

    assertEquals(new Page<>(List.of(), 3, 3, null, 2, null), thirdPage, "past the last page");
  }

  @Test
  void shouldUpdatePotentialDuplicate()
      throws PotentialDuplicateConflictException, NotFoundException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(trackedEntityAOriginal, trackedEntityADuplicate);

    deduplicationService.addPotentialDuplicate(potentialDuplicate);

    assertEquals(
        DeduplicationStatus.OPEN,
        deduplicationService
            .getPotentialDuplicate(UID.of(potentialDuplicate.getUid()))
            .getStatus());

    potentialDuplicate.setStatus(DeduplicationStatus.INVALID);

    deduplicationService.updatePotentialDuplicate(potentialDuplicate);

    assertEquals(
        DeduplicationStatus.INVALID,
        deduplicationService
            .getPotentialDuplicate(UID.of(potentialDuplicate.getUid()))
            .getStatus());
  }

  @Test
  void shouldThrowWhenOrderFieldNotExists() throws PotentialDuplicateConflictException {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(trackedEntityAOriginal, trackedEntityADuplicate);

    deduplicationService.addPotentialDuplicate(potentialDuplicate);

    PotentialDuplicateCriteria criteria = new PotentialDuplicateCriteria();
    criteria.setOrder(List.of(OrderCriteria.of("field", SortDirection.ASC)));

    assertThrows(
        IllegalArgumentException.class,
        () -> deduplicationService.getPotentialDuplicates(criteria));
  }
}
