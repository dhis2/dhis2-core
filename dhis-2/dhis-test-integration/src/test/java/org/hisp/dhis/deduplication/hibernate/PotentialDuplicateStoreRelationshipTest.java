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
package org.hisp.dhis.deduplication.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.collect.Lists;
import java.util.List;
import org.hisp.dhis.deduplication.PotentialDuplicateStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Disabled(
    "moveRelationships method do not really belong to a store now. We should a better place for it")
class PotentialDuplicateStoreRelationshipTest extends IntegrationTestBase {

  @Autowired private PotentialDuplicateStore potentialDuplicateStore;

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private RelationshipService relationshipService;

  @Autowired private RelationshipTypeService relationshipTypeService;

  @Autowired private OrganisationUnitService organisationUnitService;

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
    original = createTrackedEntity(ou);
    duplicate = createTrackedEntity(ou);
    extra1 = createTrackedEntity(ou);
    extra2 = createTrackedEntity(ou);
    trackedEntityService.addTrackedEntity(original);
    trackedEntityService.addTrackedEntity(duplicate);
    trackedEntityService.addTrackedEntity(extra1);
    trackedEntityService.addTrackedEntity(extra2);
    relationshipTypeBiDirectional = createRelationshipType('A');
    relationshipTypeUniDirectional = createRelationshipType('B');
    relationshipTypeBiDirectional.setBidirectional(true);
    relationshipTypeUniDirectional.setBidirectional(false);
    relationshipTypeService.addRelationshipType(relationshipTypeBiDirectional);
    relationshipTypeService.addRelationshipType(relationshipTypeUniDirectional);
  }

  @Test
  void moveSingleBiDirectionalRelationship() {
    Relationship bi1 = createTeiToTeiRelationship(original, extra2, relationshipTypeBiDirectional);
    Relationship bi2 = createTeiToTeiRelationship(duplicate, extra1, relationshipTypeBiDirectional);
    Relationship bi3 = createTeiToTeiRelationship(duplicate, extra2, relationshipTypeBiDirectional);
    Relationship bi4 = createTeiToTeiRelationship(extra1, extra2, relationshipTypeBiDirectional);
    relationshipService.addRelationship(bi1);
    relationshipService.addRelationship(bi2);
    relationshipService.addRelationship(bi3);
    relationshipService.addRelationship(bi4);
    transactionTemplate.execute(
        status -> {
          List<String> relationships = Lists.newArrayList(bi2.getUid());
          potentialDuplicateStore.moveRelationships(original, duplicate, relationships);
          return null;
        });
    transactionTemplate.execute(
        status -> {
          dbmsManager.clearSession();
          Relationship _bi1 = relationshipService.getRelationship(bi1.getUid());
          Relationship _bi2 = relationshipService.getRelationship(bi2.getUid());
          Relationship _bi3 = relationshipService.getRelationship(bi3.getUid());
          Relationship _bi4 = relationshipService.getRelationship(bi4.getUid());
          assertNotNull(_bi1);
          assertEquals(original.getUid(), _bi1.getFrom().getTrackedEntity().getUid());
          assertEquals(extra2.getUid(), _bi1.getTo().getTrackedEntity().getUid());
          assertNotNull(_bi2);
          assertEquals(original.getUid(), _bi2.getFrom().getTrackedEntity().getUid());
          assertEquals(extra1.getUid(), _bi2.getTo().getTrackedEntity().getUid());
          assertNotNull(_bi3);
          assertEquals(duplicate.getUid(), _bi3.getFrom().getTrackedEntity().getUid());
          assertEquals(extra2.getUid(), _bi3.getTo().getTrackedEntity().getUid());
          assertNotNull(_bi4);
          assertEquals(extra1.getUid(), _bi4.getFrom().getTrackedEntity().getUid());
          assertEquals(extra2.getUid(), _bi4.getTo().getTrackedEntity().getUid());
          return null;
        });
  }

  @Test
  void moveSingleUniDirectionalRelationship() {
    Relationship uni1 =
        createTeiToTeiRelationship(original, extra2, relationshipTypeUniDirectional);
    Relationship uni2 =
        createTeiToTeiRelationship(duplicate, extra1, relationshipTypeUniDirectional);
    Relationship uni3 =
        createTeiToTeiRelationship(extra2, duplicate, relationshipTypeUniDirectional);
    Relationship uni4 = createTeiToTeiRelationship(extra1, extra2, relationshipTypeUniDirectional);
    relationshipService.addRelationship(uni1);
    relationshipService.addRelationship(uni2);
    relationshipService.addRelationship(uni3);
    relationshipService.addRelationship(uni4);
    original = trackedEntityService.getTrackedEntity(original.getUid());
    duplicate = trackedEntityService.getTrackedEntity(duplicate.getUid());
    List<String> relationships = Lists.newArrayList(uni3.getUid());
    potentialDuplicateStore.moveRelationships(original, duplicate, relationships);
    trackedEntityService.updateTrackedEntity(original);
    trackedEntityService.updateTrackedEntity(duplicate);
    Relationship _uni1 = relationshipService.getRelationship(uni1.getUid());
    Relationship _uni2 = relationshipService.getRelationship(uni2.getUid());
    Relationship _uni3 = relationshipService.getRelationship(uni3.getUid());
    Relationship _uni4 = relationshipService.getRelationship(uni4.getUid());
    assertNotNull(_uni1);
    assertEquals(original.getUid(), _uni1.getFrom().getTrackedEntity().getUid());
    assertEquals(extra2.getUid(), _uni1.getTo().getTrackedEntity().getUid());
    assertNotNull(_uni2);
    assertEquals(duplicate.getUid(), _uni2.getFrom().getTrackedEntity().getUid());
    assertEquals(extra1.getUid(), _uni2.getTo().getTrackedEntity().getUid());
    assertNotNull(_uni3);
    assertEquals(extra2.getUid(), _uni3.getFrom().getTrackedEntity().getUid());
    assertEquals(original.getUid(), _uni3.getTo().getTrackedEntity().getUid());
    assertNotNull(_uni4);
    assertEquals(extra1.getUid(), _uni4.getFrom().getTrackedEntity().getUid());
    assertEquals(extra2.getUid(), _uni4.getTo().getTrackedEntity().getUid());
  }

  @Test
  void moveMultipleRelationship() {
    Relationship uni1 =
        createTeiToTeiRelationship(original, extra2, relationshipTypeUniDirectional);
    Relationship uni2 =
        createTeiToTeiRelationship(duplicate, extra1, relationshipTypeUniDirectional);
    Relationship bi1 =
        createTeiToTeiRelationship(extra2, duplicate, relationshipTypeUniDirectional);
    Relationship bi2 = createTeiToTeiRelationship(extra1, extra2, relationshipTypeUniDirectional);
    relationshipService.addRelationship(uni1);
    relationshipService.addRelationship(uni2);
    relationshipService.addRelationship(bi1);
    relationshipService.addRelationship(bi2);
    transactionTemplate.execute(
        status -> {
          List<String> relationships = Lists.newArrayList(uni2.getUid(), bi1.getUid());
          potentialDuplicateStore.moveRelationships(original, duplicate, relationships);
          return null;
        });
    transactionTemplate.execute(
        status -> {
          dbmsManager.clearSession();
          Relationship _uni1 = relationshipService.getRelationship(uni1.getUid());
          Relationship _uni2 = relationshipService.getRelationship(uni2.getUid());
          Relationship _bi1 = relationshipService.getRelationship(bi1.getUid());
          Relationship _bi2 = relationshipService.getRelationship(bi2.getUid());
          assertNotNull(_uni1);
          assertEquals(original.getUid(), _uni1.getFrom().getTrackedEntity().getUid());
          assertEquals(extra2.getUid(), _uni1.getTo().getTrackedEntity().getUid());
          assertNotNull(_uni2);
          assertEquals(original.getUid(), _uni2.getFrom().getTrackedEntity().getUid());
          assertEquals(extra1.getUid(), _uni2.getTo().getTrackedEntity().getUid());
          assertNotNull(_bi1);
          assertEquals(extra2.getUid(), _bi1.getFrom().getTrackedEntity().getUid());
          assertEquals(original.getUid(), _bi1.getTo().getTrackedEntity().getUid());
          assertNotNull(_bi2);
          assertEquals(extra1.getUid(), _bi2.getFrom().getTrackedEntity().getUid());
          assertEquals(extra2.getUid(), _bi2.getTo().getTrackedEntity().getUid());
          return null;
        });
  }
}
