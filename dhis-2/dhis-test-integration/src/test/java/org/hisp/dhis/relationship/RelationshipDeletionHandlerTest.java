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
package org.hisp.dhis.relationship;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RelationshipDeletionHandlerTest extends PostgresIntegrationTestBase {
  private RelationshipType relationshipType;

  private RelationshipType deletableRelationshipType;

  @Autowired private IdentifiableObjectManager manager;

  @BeforeAll
  public void setUp() {
    relationshipType = createRelationshipType('A');
    manager.save(relationshipType);
    deletableRelationshipType = createRelationshipType('B');
    manager.save(deletableRelationshipType);
    OrganisationUnit orgUnit = createOrganisationUnit('A');
    manager.save(orgUnit);
    TrackedEntityType trackedEntityType = createTrackedEntityType('O');
    manager.save(trackedEntityType);
    TrackedEntity trackedEntityFrom = createTrackedEntity(orgUnit, trackedEntityType);
    manager.save(trackedEntityFrom);
    TrackedEntity trackedEntityTo = createTrackedEntity(orgUnit, trackedEntityType);
    manager.save(trackedEntityTo);
    Relationship relationship =
        createTeToTeRelationship(trackedEntityFrom, trackedEntityTo, relationshipType);
    manager.save(relationship);
  }

  @Test
  void shouldThrowExceptionWhenRelationshipTypeIsDeletedButARelationshipIsInTheDB() {
    assertThrows(DeleteNotAllowedException.class, () -> manager.delete(relationshipType));
  }

  @Test
  void shouldSuccessfullyDeleteRelationshipTypeWhenNoLinkedRelationshipIsInTheDB() {
    manager.delete(deletableRelationshipType);

    assertNull(manager.get(RelationshipType.class, deletableRelationshipType.getUid()));
  }
}
