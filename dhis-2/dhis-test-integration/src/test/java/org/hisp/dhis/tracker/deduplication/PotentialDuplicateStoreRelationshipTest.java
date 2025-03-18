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
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@Disabled(
    "moveRelationships method do not really belong to a store now. We should a better place for it")
class PotentialDuplicateStoreRelationshipTest extends PostgresIntegrationTestBase {

  @Autowired private HibernatePotentialDuplicateStore potentialDuplicateStore;

  @Autowired private RelationshipTypeService relationshipTypeService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TransactionTemplate transactionTemplate;

  @Autowired private DbmsManager dbmsManager;

  private TrackedEntity original;

  private TrackedEntity duplicate;

  private TrackedEntity extra1;

  private TrackedEntity extra2;

  private RelationshipType relationshipTypeBiDirectional;

  private RelationshipType relationshipTypeUniDirectional;

  @BeforeEach
  void setUp() {
    OrganisationUnit ou = createOrganisationUnit("OU_A");
    organisationUnitService.addOrganisationUnit(ou);

    TrackedEntityType trackedEntityType = createTrackedEntityType('O');
    manager.save(trackedEntityType);
    original = createTrackedEntity(ou, trackedEntityType);
    duplicate = createTrackedEntity(ou, trackedEntityType);
    extra1 = createTrackedEntity(ou, trackedEntityType);
    extra2 = createTrackedEntity(ou, trackedEntityType);
    manager.save(original);
    manager.save(duplicate);
    manager.save(extra1);
    manager.save(extra2);
    relationshipTypeBiDirectional = createRelationshipType('A');
    relationshipTypeUniDirectional = createRelationshipType('B');
    relationshipTypeBiDirectional.setBidirectional(true);
    relationshipTypeUniDirectional.setBidirectional(false);
    relationshipTypeService.addRelationshipType(relationshipTypeBiDirectional);
    relationshipTypeService.addRelationshipType(relationshipTypeUniDirectional);
  }

  @Test
  void moveSingleBiDirectionalRelationship() {
    Relationship bi1 = createTeToTeRelationship(original, extra2, relationshipTypeBiDirectional);
    Relationship bi2 = createTeToTeRelationship(duplicate, extra1, relationshipTypeBiDirectional);
    Relationship bi3 = createTeToTeRelationship(duplicate, extra2, relationshipTypeBiDirectional);
    Relationship bi4 = createTeToTeRelationship(extra1, extra2, relationshipTypeBiDirectional);
    manager.save(bi1);
    manager.save(bi2);
    manager.save(bi3);
    manager.save(bi4);
    transactionTemplate.execute(
        status -> {
          Set<UID> relationships = Set.of(UID.of(bi2));
          potentialDuplicateStore.moveRelationships(original, duplicate, relationships);
          return null;
        });
    transactionTemplate.execute(
        status -> {
          dbmsManager.clearSession();
          Relationship bi1FromDB = getRelationship(bi1.getUid());
          Relationship bi2FromDB = getRelationship(bi2.getUid());
          Relationship bi3FromDB = getRelationship(bi3.getUid());
          Relationship bi4FromDB = getRelationship(bi4.getUid());
          assertNotNull(bi1FromDB);
          assertEquals(original.getUid(), bi1FromDB.getFrom().getTrackedEntity().getUid());
          assertEquals(extra2.getUid(), bi1FromDB.getTo().getTrackedEntity().getUid());
          assertNotNull(bi2FromDB);
          assertEquals(original.getUid(), bi2FromDB.getFrom().getTrackedEntity().getUid());
          assertEquals(extra1.getUid(), bi2FromDB.getTo().getTrackedEntity().getUid());
          assertNotNull(bi3FromDB);
          assertEquals(duplicate.getUid(), bi3FromDB.getFrom().getTrackedEntity().getUid());
          assertEquals(extra2.getUid(), bi3FromDB.getTo().getTrackedEntity().getUid());
          assertNotNull(bi4FromDB);
          assertEquals(extra1.getUid(), bi4FromDB.getFrom().getTrackedEntity().getUid());
          assertEquals(extra2.getUid(), bi4FromDB.getTo().getTrackedEntity().getUid());
          return null;
        });
  }

  @Test
  void moveSingleUniDirectionalRelationship() {
    Relationship uni1 = createTeToTeRelationship(original, extra2, relationshipTypeUniDirectional);
    Relationship uni2 = createTeToTeRelationship(duplicate, extra1, relationshipTypeUniDirectional);
    Relationship uni3 = createTeToTeRelationship(extra2, duplicate, relationshipTypeUniDirectional);
    Relationship uni4 = createTeToTeRelationship(extra1, extra2, relationshipTypeUniDirectional);
    manager.save(uni1);
    manager.save(uni2);
    manager.save(uni3);
    manager.save(uni4);
    original = manager.get(TrackedEntity.class, original.getUid());
    duplicate = manager.get(TrackedEntity.class, duplicate.getUid());
    Set<UID> relationships = Set.of(UID.of(uni3));
    potentialDuplicateStore.moveRelationships(original, duplicate, relationships);
    manager.update(original);
    manager.update(duplicate);
    Relationship uni1FromDB = getRelationship(uni1.getUid());
    Relationship uni2FromDB = getRelationship(uni2.getUid());
    Relationship uni3FromDB = getRelationship(uni3.getUid());
    Relationship uni4FromDB = getRelationship(uni4.getUid());
    assertNotNull(uni1FromDB);
    assertEquals(original.getUid(), uni1FromDB.getFrom().getTrackedEntity().getUid());
    assertEquals(extra2.getUid(), uni1FromDB.getTo().getTrackedEntity().getUid());
    assertNotNull(uni2FromDB);
    assertEquals(duplicate.getUid(), uni2FromDB.getFrom().getTrackedEntity().getUid());
    assertEquals(extra1.getUid(), uni2FromDB.getTo().getTrackedEntity().getUid());
    assertNotNull(uni3FromDB);
    assertEquals(extra2.getUid(), uni3FromDB.getFrom().getTrackedEntity().getUid());
    assertEquals(original.getUid(), uni3FromDB.getTo().getTrackedEntity().getUid());
    assertNotNull(uni4FromDB);
    assertEquals(extra1.getUid(), uni4FromDB.getFrom().getTrackedEntity().getUid());
    assertEquals(extra2.getUid(), uni4FromDB.getTo().getTrackedEntity().getUid());
  }

  @Test
  void moveMultipleRelationship() {
    Relationship uni1 = createTeToTeRelationship(original, extra2, relationshipTypeUniDirectional);
    Relationship uni2 = createTeToTeRelationship(duplicate, extra1, relationshipTypeUniDirectional);
    Relationship bi1 = createTeToTeRelationship(extra2, duplicate, relationshipTypeUniDirectional);
    Relationship bi2 = createTeToTeRelationship(extra1, extra2, relationshipTypeUniDirectional);
    manager.save(uni1);
    manager.save(uni2);
    manager.save(bi1);
    manager.save(bi2);
    transactionTemplate.execute(
        status -> {
          Set<UID> relationships = UID.of(uni2, bi1);
          potentialDuplicateStore.moveRelationships(original, duplicate, relationships);
          return null;
        });
    transactionTemplate.execute(
        status -> {
          dbmsManager.clearSession();
          Relationship uni1FromDB = getRelationship(uni1.getUid());
          Relationship uni2FromDB = getRelationship(uni2.getUid());
          Relationship bi1FromDB = getRelationship(bi1.getUid());
          Relationship bi2FromDB = getRelationship(bi2.getUid());
          assertNotNull(uni1FromDB);
          assertEquals(original.getUid(), uni1FromDB.getFrom().getTrackedEntity().getUid());
          assertEquals(extra2.getUid(), uni1FromDB.getTo().getTrackedEntity().getUid());
          assertNotNull(uni2FromDB);
          assertEquals(original.getUid(), uni2FromDB.getFrom().getTrackedEntity().getUid());
          assertEquals(extra1.getUid(), uni2FromDB.getTo().getTrackedEntity().getUid());
          assertNotNull(bi1FromDB);
          assertEquals(extra2.getUid(), bi1FromDB.getFrom().getTrackedEntity().getUid());
          assertEquals(original.getUid(), bi1FromDB.getTo().getTrackedEntity().getUid());
          assertNotNull(bi2FromDB);
          assertEquals(extra1.getUid(), bi2FromDB.getFrom().getTrackedEntity().getUid());
          assertEquals(extra2.getUid(), bi2FromDB.getTo().getTrackedEntity().getUid());
          return null;
        });
  }

  private Relationship getRelationship(String uid) {
    return manager.get(Relationship.class, uid);
  }
}
