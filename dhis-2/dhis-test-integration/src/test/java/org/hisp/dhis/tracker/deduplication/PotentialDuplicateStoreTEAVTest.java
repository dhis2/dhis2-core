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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@Disabled(
    "moveAttributes method do not really belong to a store now. We should a better place for it")
class PotentialDuplicateStoreTEAVTest extends PostgresIntegrationTestBase {

  @Autowired private HibernatePotentialDuplicateStore potentialDuplicateStore;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  @Autowired private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private TransactionTemplate transactionTemplate;

  @Autowired private DbmsManager dbmsManager;

  private TrackedEntity original;

  private TrackedEntity duplicate;

  private TrackedEntity control;

  private TrackedEntityAttribute trackedEntityAttributeA;

  private TrackedEntityAttribute trackedEntityAttributeB;

  private TrackedEntityAttribute trackedEntityAttributeC;

  private TrackedEntityAttribute trackedEntityAttributeD;

  private TrackedEntityAttribute trackedEntityAttributeE;

  @BeforeEach
  void setUp() {
    OrganisationUnit ou = createOrganisationUnit("OU_A");
    organisationUnitService.addOrganisationUnit(ou);

    TrackedEntityType trackedEntityType = createTrackedEntityType('O');
    manager.save(trackedEntityType);
    original = createTrackedEntity(ou, trackedEntityType);
    duplicate = createTrackedEntity(ou, trackedEntityType);
    control = createTrackedEntity(ou, trackedEntityType);
    manager.save(original);
    manager.save(duplicate);
    manager.save(control);
    trackedEntityAttributeA = createTrackedEntityAttribute('A');
    trackedEntityAttributeB = createTrackedEntityAttribute('B');
    trackedEntityAttributeC = createTrackedEntityAttribute('C');
    trackedEntityAttributeD = createTrackedEntityAttribute('D');
    trackedEntityAttributeE = createTrackedEntityAttribute('E');
    trackedEntityAttributeService.addTrackedEntityAttribute(trackedEntityAttributeA);
    trackedEntityAttributeService.addTrackedEntityAttribute(trackedEntityAttributeB);
    trackedEntityAttributeService.addTrackedEntityAttribute(trackedEntityAttributeC);
    trackedEntityAttributeService.addTrackedEntityAttribute(trackedEntityAttributeD);
    trackedEntityAttributeService.addTrackedEntityAttribute(trackedEntityAttributeE);
    original.addAttributeValue(
        createTrackedEntityAttributeValue('A', original, trackedEntityAttributeA));
    original.addAttributeValue(
        createTrackedEntityAttributeValue('A', original, trackedEntityAttributeB));
    original.addAttributeValue(
        createTrackedEntityAttributeValue('A', original, trackedEntityAttributeC));
    duplicate.addAttributeValue(
        createTrackedEntityAttributeValue('B', duplicate, trackedEntityAttributeA));
    duplicate.addAttributeValue(
        createTrackedEntityAttributeValue('B', duplicate, trackedEntityAttributeB));
    duplicate.addAttributeValue(
        createTrackedEntityAttributeValue('B', duplicate, trackedEntityAttributeC));
    duplicate.addAttributeValue(
        createTrackedEntityAttributeValue('B', duplicate, trackedEntityAttributeD));
    duplicate.addAttributeValue(
        createTrackedEntityAttributeValue('B', duplicate, trackedEntityAttributeE));
    control.addAttributeValue(
        createTrackedEntityAttributeValue('C', control, trackedEntityAttributeA));
    control.addAttributeValue(
        createTrackedEntityAttributeValue('C', control, trackedEntityAttributeB));
    control.addAttributeValue(
        createTrackedEntityAttributeValue('C', control, trackedEntityAttributeC));
    original
        .getTrackedEntityAttributeValues()
        .forEach(trackedEntityAttributeValueService::addTrackedEntityAttributeValue);
    duplicate
        .getTrackedEntityAttributeValues()
        .forEach(trackedEntityAttributeValueService::addTrackedEntityAttributeValue);
    control
        .getTrackedEntityAttributeValues()
        .forEach(trackedEntityAttributeValueService::addTrackedEntityAttributeValue);
  }

  @Test
  void moveTrackedEntityAttributeValuesSingleTea() {
    Set<UID> teas = Set.of(UID.of(trackedEntityAttributeA));
    transactionTemplate.execute(
        status -> {
          potentialDuplicateStore.moveTrackedEntityAttributeValues(original, duplicate, teas);
          return null;
        });
    transactionTemplate.execute(
        status -> {
          // Clear the session so we get new data from the DB for the next
          // queries.
          dbmsManager.clearSession();
          TrackedEntity originalFromDB = manager.get(TrackedEntity.class, original.getUid());
          TrackedEntity duplicateFromDB = manager.get(TrackedEntity.class, duplicate.getUid());
          assertNotNull(originalFromDB);
          assertNotNull(duplicateFromDB);
          assertEquals(3, originalFromDB.getTrackedEntityAttributeValues().size());
          assertEquals(4, duplicateFromDB.getTrackedEntityAttributeValues().size());
          originalFromDB
              .getTrackedEntityAttributeValues()
              .forEach(
                  teav -> {
                    if (teas.contains(teav.getAttribute().getUid())) {
                      assertEquals("AttributeB", teav.getValue());
                    } else {
                      assertEquals("AttributeA", teav.getValue());
                    }
                  });
          TrackedEntity controlFromDB = manager.get(TrackedEntity.class, control.getUid());
          assertNotNull(controlFromDB);
          assertEquals(3, controlFromDB.getTrackedEntityAttributeValues().size());
          return null;
        });
  }

  @Test
  void moveTrackedEntityAttributeValuesMultipleTeas() {
    Set<UID> teas = UID.of(trackedEntityAttributeA, trackedEntityAttributeB);
    transactionTemplate.execute(
        status -> {
          potentialDuplicateStore.moveTrackedEntityAttributeValues(original, duplicate, teas);
          return null;
        });
    transactionTemplate.execute(
        status -> {
          // Clear the session so we get new data from the DB for the next
          // queries.
          dbmsManager.clearSession();
          TrackedEntity originalFromDB = manager.get(TrackedEntity.class, original.getUid());
          TrackedEntity duplicateoriginalFromDB =
              manager.get(TrackedEntity.class, duplicate.getUid());
          assertNotNull(originalFromDB);
          assertNotNull(duplicateoriginalFromDB);
          assertEquals(3, originalFromDB.getTrackedEntityAttributeValues().size());
          assertEquals(3, duplicateoriginalFromDB.getTrackedEntityAttributeValues().size());
          originalFromDB
              .getTrackedEntityAttributeValues()
              .forEach(
                  teav -> {
                    if (teas.contains(teav.getAttribute().getUid())) {
                      assertEquals("AttributeB", teav.getValue());
                    } else {
                      assertEquals("AttributeA", teav.getValue());
                    }
                  });
          TrackedEntity controlFromDB = manager.get(TrackedEntity.class, control.getUid());
          assertNotNull(controlFromDB);
          assertEquals(3, controlFromDB.getTrackedEntityAttributeValues().size());
          return null;
        });
  }

  @Test
  void moveTrackedEntityAttributeValuesByOverwritingAndCreatingNew() {
    Set<UID> teas = UID.of(trackedEntityAttributeD, trackedEntityAttributeB);
    transactionTemplate.execute(
        status -> {
          potentialDuplicateStore.moveTrackedEntityAttributeValues(original, duplicate, teas);
          return null;
        });
    transactionTemplate.execute(
        status -> {
          // Clear the session so we get new data from the DB for the next
          // queries.
          dbmsManager.clearSession();
          TrackedEntity originalFromDB = manager.get(TrackedEntity.class, original.getUid());
          TrackedEntity duplicateoriginalFromDB =
              manager.get(TrackedEntity.class, duplicate.getUid());
          assertNotNull(originalFromDB);
          assertNotNull(duplicateoriginalFromDB);
          assertEquals(4, originalFromDB.getTrackedEntityAttributeValues().size());
          assertEquals(3, duplicateoriginalFromDB.getTrackedEntityAttributeValues().size());
          originalFromDB
              .getTrackedEntityAttributeValues()
              .forEach(
                  teav -> {
                    if (teas.contains(teav.getAttribute().getUid())) {
                      assertEquals("AttributeB", teav.getValue());
                    } else {
                      assertEquals("AttributeA", teav.getValue());
                    }
                  });
          TrackedEntity controlFromDB = manager.get(TrackedEntity.class, control.getUid());
          assertNotNull(controlFromDB);
          assertEquals(3, controlFromDB.getTrackedEntityAttributeValues().size());
          return null;
        });
  }
}
