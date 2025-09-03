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

import static org.hisp.dhis.security.Authorities.ALL;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.imports.bundle.persister.TrackerObjectDeletionService;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PotentialDuplicateRemoveTrackedEntityTest extends PostgresIntegrationTestBase {

  @Autowired private TrackerObjectDeletionService trackerObjectDeletionService;

  @Autowired private RelationshipTypeService relationshipTypeService;

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  @Autowired private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private RelationshipService relationshipService;

  @Autowired private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  private TrackedEntityType trackedEntityType;

  private OrganisationUnit organisationUnit;

  private Program program;

  private TrackedEntity original;
  private TrackedEntity duplicate;
  private TrackedEntity control1;
  private TrackedEntity control2;

  @BeforeEach
  void setupTestUser() {
    User user = getAdminUser();
    trackedEntityType = createTrackedEntityType('O');
    manager.save(trackedEntityType);
    program = createProgram('A');
    program.setTrackedEntityType(trackedEntityType);
    program.setSharing(Sharing.builder().publicAccess(AccessStringHelper.FULL).build());
    manager.save(program);
    organisationUnit = createOrganisationUnit('A');
    manager.save(organisationUnit);
    user.setTeiSearchOrganisationUnits(Set.of(organisationUnit));
    injectSecurityContextUser(user);

    original = createTrackedEntity(organisationUnit, trackedEntityType);
    duplicate = createTrackedEntity(organisationUnit, trackedEntityType);
    control1 = createTrackedEntity(organisationUnit, trackedEntityType);
    control2 = createTrackedEntity(organisationUnit, trackedEntityType);
    manager.save(original);
    manager.save(duplicate);
    manager.save(control1);
    manager.save(control2);
    manager.flush();
    manager.clear();
  }

  @Test
  void shouldDeleteTrackedEntity() throws NotFoundException {
    TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute('A');
    trackedEntityAttributeService.addTrackedEntityAttribute(trackedEntityAttribute);
    TrackedEntity trackedEntity = createTrackedEntity(trackedEntityAttribute);
    assertTrue(trackedEntityService.findTrackedEntity(UID.of(trackedEntity)).isPresent());
    removeTrackedEntity(trackedEntity);
    assertFalse(trackedEntityService.findTrackedEntity(UID.of(trackedEntity)).isPresent());
  }

  @Test
  void shouldDeleteTeAndAttributeValues() throws NotFoundException {
    TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute('A');
    trackedEntityAttributeService.addTrackedEntityAttribute(trackedEntityAttribute);
    TrackedEntity trackedEntity = createTrackedEntity(trackedEntityAttribute);
    trackedEntity
        .getTrackedEntityAttributeValues()
        .forEach(trackedEntityAttributeValueService::addTrackedEntityAttributeValue);
    assertTrue(trackedEntityService.findTrackedEntity(UID.of(trackedEntity)).isPresent());
    removeTrackedEntity(trackedEntity);
    assertFalse(trackedEntityService.findTrackedEntity(UID.of(trackedEntity)).isPresent());
    assertNull(
        trackedEntityAttributeValueService.getTrackedEntityAttributeValue(
            trackedEntity, trackedEntityAttribute));
  }

  @Test
  void shouldDeleteRelationShips() throws NotFoundException {
    RelationshipType relationshipType = createRelationshipType('A');
    relationshipTypeService.addRelationshipType(relationshipType);
    Relationship relationship1 = createTeToTeRelationship(original, control1, relationshipType);
    Relationship relationship2 = createTeToTeRelationship(control2, control1, relationshipType);
    Relationship relationship3 = createTeToTeRelationship(duplicate, control2, relationshipType);
    Relationship relationship4 = createTeToTeRelationship(control1, duplicate, relationshipType);
    Relationship relationship5 = createTeToTeRelationship(control1, original, relationshipType);
    manager.save(relationship1);
    manager.save(relationship2);
    manager.save(relationship3);
    manager.save(relationship4);
    manager.save(relationship5);
    manager.flush();
    manager.clear();
    assertTrue(trackedEntityService.findTrackedEntity(UID.of(original)).isPresent());
    assertTrue(trackedEntityService.findTrackedEntity(UID.of(duplicate)).isPresent());
    assertTrue(trackedEntityService.findTrackedEntity(UID.of(control1)).isPresent());
    assertTrue(trackedEntityService.findTrackedEntity(UID.of(control2)).isPresent());

    removeTrackedEntity(duplicate);
    assertFalse(relationshipService.findRelationship(UID.of(relationship3)).isPresent());
    assertFalse(relationshipService.findRelationship(UID.of(relationship4)).isPresent());
    assertTrue(relationshipService.findRelationship(UID.of(relationship1)).isPresent());
    assertTrue(relationshipService.findRelationship(UID.of(relationship2)).isPresent());
    assertTrue(relationshipService.findRelationship(UID.of(relationship5)).isPresent());
    assertFalse(trackedEntityService.findTrackedEntity(UID.of(duplicate)).isPresent());
  }

  @Test
  void shouldDeleteEnrollments() throws NotFoundException {
    User user =
        createAndAddUser(
            false, "user", Set.of(organisationUnit), Set.of(organisationUnit), ALL.toString());
    injectSecurityContextUser(user);

    Enrollment enrollment1 = createEnrollment(program, original, organisationUnit);
    Enrollment enrollment2 = createEnrollment(program, duplicate, organisationUnit);
    Enrollment enrollment3 = createEnrollment(program, control1, organisationUnit);
    Enrollment enrollment4 = createEnrollment(program, control2, organisationUnit);
    manager.save(enrollment1);
    manager.save(enrollment2);
    manager.save(enrollment3);
    manager.save(enrollment4);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        original, program, organisationUnit);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        duplicate, program, organisationUnit);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        control1, program, organisationUnit);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        control2, program, organisationUnit);
    original.getEnrollments().add(enrollment1);
    duplicate.getEnrollments().add(enrollment2);
    control1.getEnrollments().add(enrollment3);
    control2.getEnrollments().add(enrollment4);
    manager.update(original);
    manager.update(duplicate);
    manager.update(control1);
    manager.update(control2);
    assertTrue(trackedEntityService.findTrackedEntity(UID.of(original)).isPresent());
    assertTrue(trackedEntityService.findTrackedEntity(UID.of(duplicate)).isPresent());
    assertTrue(trackedEntityService.findTrackedEntity(UID.of(control1)).isPresent());
    assertTrue(trackedEntityService.findTrackedEntity(UID.of(control2)).isPresent());
    removeTrackedEntity(duplicate);
    assertFalse(enrollmentService.findEnrollment(UID.of(enrollment2)).isPresent());
    assertTrue(enrollmentService.findEnrollment(UID.of(enrollment1)).isPresent());
    assertTrue(enrollmentService.findEnrollment(UID.of(enrollment3)).isPresent());
    assertTrue(enrollmentService.findEnrollment(UID.of(enrollment4)).isPresent());
    assertFalse(trackedEntityService.findTrackedEntity(UID.of(duplicate)).isPresent());
  }

  private TrackedEntity createTrackedEntity(TrackedEntityAttribute trackedEntityAttribute) {
    TrackedEntity trackedEntity =
        createTrackedEntity('T', organisationUnit, trackedEntityAttribute, trackedEntityType);
    manager.save(trackedEntity);
    return trackedEntity;
  }

  private void removeTrackedEntity(TrackedEntity trackedEntity) throws NotFoundException {
    trackerObjectDeletionService.deleteTrackedEntities(List.of(UID.of(trackedEntity)));
  }
}
