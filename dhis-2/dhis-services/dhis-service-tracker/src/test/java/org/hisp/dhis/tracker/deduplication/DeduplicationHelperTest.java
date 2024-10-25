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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith({MockitoExtension.class})
class DeduplicationHelperTest extends TestBase {
  @InjectMocks private DeduplicationHelper deduplicationHelper;
  @Mock private AclService aclService;

  @Mock private RelationshipService relationshipService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private EnrollmentService enrollmentService;

  @Mock private UserService userService;

  private OrganisationUnit organisationUnitA;

  private OrganisationUnit organisationUnitB;

  private TrackedEntityType trackedEntityTypeA;

  private TrackedEntityType trackedEntityTypeB;

  private RelationshipType relationshipType;

  private RelationshipType relationshipTypeBidirectional;

  private TrackedEntityAttribute attribute;

  private Enrollment enrollment;

  private MergeObject mergeObject;

  private User user;

  private UserDetails currentUserDetails;

  @BeforeEach
  public void setUp() throws ForbiddenException, NotFoundException {
    Set<UID> relationshipUids = UID.of(CodeGenerator.generateUid(), CodeGenerator.generateUid());
    List<String> attributeUids = List.of(CodeGenerator.generateUid(), CodeGenerator.generateUid());
    Set<UID> enrollmentUids = UID.of(CodeGenerator.generateUid(), CodeGenerator.generateUid());

    organisationUnitA = createOrganisationUnit('A');
    organisationUnitB = createOrganisationUnit('B');
    trackedEntityTypeA = createTrackedEntityType('A');
    trackedEntityTypeB = createTrackedEntityType('B');
    relationshipType = createRelationshipType('A');
    relationshipTypeBidirectional = createRelationshipType('B');
    attribute = createTrackedEntityAttribute('A');
    enrollment = createEnrollment(createProgram('A'), getTrackedEntityA(), organisationUnitA);
    mergeObject =
        MergeObject.builder()
            .relationships(UID.toValueList(relationshipUids))
            .trackedEntityAttributes(attributeUids)
            .enrollments(UID.toValueList(enrollmentUids))
            .build();
    user = makeUser("A", Lists.newArrayList("F_TRACKED_ENTITY_MERGE"));

    currentUserDetails = UserDetails.fromUser(user);
    injectSecurityContext(currentUserDetails);

    relationshipType.setBidirectional(false);
    relationshipTypeBidirectional.setBidirectional(true);

    when(aclService.canDataWrite(currentUserDetails, trackedEntityTypeA)).thenReturn(true);
    when(aclService.canDataWrite(currentUserDetails, trackedEntityTypeB)).thenReturn(true);
    when(aclService.canDataWrite(currentUserDetails, relationshipType)).thenReturn(true);
    when(aclService.canDataWrite(currentUserDetails, enrollment.getProgram())).thenReturn(true);

    when(relationshipService.getRelationships(relationshipUids)).thenReturn(getRelationships());
    when(enrollmentService.getEnrollments(enrollmentUids)).thenReturn(getEnrollments());
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnitA)).thenReturn(true);
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnitB)).thenReturn(true);
  }

  @Test
  void shouldHaveUserAccess() throws ForbiddenException, NotFoundException {
    when(userService.getUserByUsername(user.getUsername())).thenReturn(user);

    String hasUserAccess =
        deduplicationHelper.getUserAccessErrors(
            getTrackedEntityA(), getTrackedEntityB(), mergeObject);

    assertNull(hasUserAccess);
  }

  @Test
  @Disabled("There should really by no situation where the user is null")
  void shouldNotHaveUserAccessWhenUserIsNull() throws ForbiddenException, NotFoundException {
    clearSecurityContext();

    String hasUserAccess =
        deduplicationHelper.getUserAccessErrors(
            getTrackedEntityA(), getTrackedEntityB(), mergeObject);

    assertNotNull(hasUserAccess);
    assertEquals("Missing required authority for merging tracked entities.", hasUserAccess);
  }

  @Test
  void shouldNotHaveUserAccessWhenUserHasNoMergeRoles()
      throws ForbiddenException, NotFoundException {
    injectSecurityContext(UserDetails.fromUser(getNoMergeAuthsUser()));

    String hasUserAccess =
        deduplicationHelper.getUserAccessErrors(
            getTrackedEntityA(), getTrackedEntityB(), mergeObject);

    assertNotNull(hasUserAccess);
    assertEquals("Missing required authority for merging tracked entities.", hasUserAccess);
  }

  @Test
  void shouldNotHaveUserAccessWhenUserHasNoAccessToOriginalTEType()
      throws ForbiddenException, NotFoundException {
    when(aclService.canDataWrite(currentUserDetails, trackedEntityTypeA)).thenReturn(false);

    String hasUserAccess =
        deduplicationHelper.getUserAccessErrors(
            getTrackedEntityA(), getTrackedEntityB(), mergeObject);

    assertNotNull(hasUserAccess);
    assertEquals("Missing data write access to Tracked Entity Type.", hasUserAccess);
  }

  @Test
  void shouldNotHaveUserAccessWhenUserHasNoAccessToDuplicateTEType()
      throws ForbiddenException, NotFoundException {

    when(aclService.canDataWrite(currentUserDetails, trackedEntityTypeB)).thenReturn(false);
    when(userService.getUserByUsername(user.getUsername())).thenReturn(user);

    String hasUserAccess =
        deduplicationHelper.getUserAccessErrors(
            getTrackedEntityA(), getTrackedEntityB(), mergeObject);

    assertNotNull(hasUserAccess);
    assertEquals("Missing data write access to Tracked Entity Type.", hasUserAccess);
  }

  @Test
  void shouldNotHaveUserAccessWhenUserHasNoAccessToRelationshipType()
      throws ForbiddenException, NotFoundException {
    when(aclService.canDataWrite(currentUserDetails, relationshipType)).thenReturn(false);

    String hasUserAccess =
        deduplicationHelper.getUserAccessErrors(
            getTrackedEntityA(), getTrackedEntityB(), mergeObject);

    assertNotNull(hasUserAccess);
    assertEquals("Missing data write access to one or more Relationship Types.", hasUserAccess);
  }

  @Test
  void shouldNotHaveUserAccessWhenUserHasNoWriteAccessToEnrollment()
      throws ForbiddenException, NotFoundException {
    when(aclService.canDataWrite(currentUserDetails, enrollment.getProgram())).thenReturn(false);

    String hasUserAccess =
        deduplicationHelper.getUserAccessErrors(
            getTrackedEntityA(), getTrackedEntityB(), mergeObject);

    assertNotNull(hasUserAccess);
    assertEquals("Missing data write access to one or more Programs.", hasUserAccess);
  }

  @Test
  void shouldNotHaveUserAccessWhenUserHasNoCaptureScopeAccessToOriginalOrgUnit()
      throws ForbiddenException, NotFoundException {
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnitA))
        .thenReturn(false);

    String hasUserAccess =
        deduplicationHelper.getUserAccessErrors(
            getTrackedEntityA(), getTrackedEntityB(), mergeObject);

    assertNotNull(hasUserAccess);
    assertEquals("Missing access to organisation unit of one or both entities.", hasUserAccess);
  }

  @Test
  void shouldNotHaveUserAccessWhenUserHasNoCaptureScopeAccessToDuplicateOrgUnit()
      throws ForbiddenException, NotFoundException {
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnitB))
        .thenReturn(false);

    when(userService.getUserByUsername(user.getUsername())).thenReturn(user);

    String hasUserAccess =
        deduplicationHelper.getUserAccessErrors(
            getTrackedEntityA(), getTrackedEntityB(), mergeObject);

    assertNotNull(hasUserAccess);
    assertEquals("Missing access to organisation unit of one or both entities.", hasUserAccess);
  }

  @Test
  void shouldFailGenerateMergeObjectDifferentTrackedEntityType() {
    assertThrows(
        PotentialDuplicateForbiddenException.class,
        () -> deduplicationHelper.generateMergeObject(getTrackedEntityA(), getTrackedEntityB()));
  }

  @Test
  void shouldFailGenerateMergeObjectConflictingValue() {
    TrackedEntity original = getTrackedEntityA();

    TrackedEntityAttributeValue attributeValueOriginal = new TrackedEntityAttributeValue();
    attributeValueOriginal.setAttribute(attribute);
    attributeValueOriginal.setTrackedEntity(original);
    attributeValueOriginal.setValue("Attribute-Original");

    original.getTrackedEntityAttributeValues().add(attributeValueOriginal);

    TrackedEntity duplicate = getTrackedEntityA();

    TrackedEntityAttributeValue attributeValueDuplicate = new TrackedEntityAttributeValue();
    attributeValueDuplicate.setAttribute(attribute);
    attributeValueDuplicate.setTrackedEntity(duplicate);
    attributeValueDuplicate.setValue("Attribute-Duplicate");

    duplicate.getTrackedEntityAttributeValues().add(attributeValueDuplicate);

    assertThrows(
        PotentialDuplicateConflictException.class,
        () -> deduplicationHelper.generateMergeObject(original, duplicate));
  }

  @Test
  void shouldGenerateMergeObjectForAttribute()
      throws PotentialDuplicateConflictException, PotentialDuplicateForbiddenException {
    TrackedEntity original = getTrackedEntityA();

    TrackedEntityAttributeValue attributeValueOriginal = new TrackedEntityAttributeValue();
    attributeValueOriginal.setAttribute(attribute);
    attributeValueOriginal.setTrackedEntity(original);
    attributeValueOriginal.setValue("Attribute-Original");

    original.getTrackedEntityAttributeValues().add(attributeValueOriginal);

    TrackedEntity duplicate = getTrackedEntityA();

    TrackedEntityAttributeValue attributeValueDuplicate = new TrackedEntityAttributeValue();
    TrackedEntityAttribute duplicateAttribute = createTrackedEntityAttribute('B');
    attributeValueDuplicate.setAttribute(duplicateAttribute);
    attributeValueDuplicate.setTrackedEntity(duplicate);
    attributeValueDuplicate.setValue("Attribute-Duplicate");

    duplicate.getTrackedEntityAttributeValues().add(attributeValueDuplicate);

    MergeObject actualMergeObject = deduplicationHelper.generateMergeObject(original, duplicate);

    assertFalse(actualMergeObject.getTrackedEntityAttributes().isEmpty());

    actualMergeObject
        .getTrackedEntityAttributes()
        .forEach(a -> assertEquals(duplicateAttribute.getUid(), a));
  }

  @Test
  void testMergeObjectRelationship()
      throws PotentialDuplicateConflictException, PotentialDuplicateForbiddenException {
    TrackedEntity original = getTrackedEntityA();

    TrackedEntity another = getTrackedEntityA();

    TrackedEntity duplicate = getTrackedEntityA();

    Relationship anotherBaseRelationship = getRelationship();

    RelationshipItem relationshipItemAnotherTo =
        getRelationshipItem(anotherBaseRelationship, another);
    RelationshipItem relationshipItemAnotherFrom =
        getRelationshipItem(anotherBaseRelationship, duplicate);

    Relationship anotherRelationship =
        getRelationship(relationshipItemAnotherTo, relationshipItemAnotherFrom);
    RelationshipItem anotherRelationshipItem = getRelationshipItem(anotherRelationship, duplicate);

    duplicate.getRelationshipItems().add(anotherRelationshipItem);

    MergeObject actualMergeObject = deduplicationHelper.generateMergeObject(original, duplicate);

    assertTrue(actualMergeObject.getTrackedEntityAttributes().isEmpty());

    assertFalse(actualMergeObject.getRelationships().isEmpty());

    actualMergeObject
        .getRelationships()
        .forEach(r -> assertEquals(anotherRelationship.getUid(), r));

    Relationship baseRelationship = getRelationship();

    RelationshipItem relationshipItemTo = getRelationshipItem(baseRelationship, original);
    RelationshipItem relationshipItemFrom = getRelationshipItem(baseRelationship, duplicate);

    Relationship relationship = getRelationship(relationshipItemTo, relationshipItemFrom);
    RelationshipItem relationshipItem = getRelationshipItem(relationship, duplicate);

    duplicate.getRelationshipItems().add(relationshipItem);

    actualMergeObject = deduplicationHelper.generateMergeObject(original, duplicate);

    assertEquals(1, actualMergeObject.getRelationships().size());
  }

  @Test
  void shouldGenerateMergeObjectWIthEnrollments()
      throws PotentialDuplicateConflictException, PotentialDuplicateForbiddenException {
    TrackedEntity original = getTrackedEntityA();
    Program programA = createProgram('A');
    Enrollment enrollmentA = createEnrollment(programA, original, organisationUnitA);
    enrollmentA.setUid("enrollmentA");
    original.getEnrollments().add(enrollmentA);

    TrackedEntity duplicate = getTrackedEntityA();
    Program programB = createProgram('B');
    Enrollment enrollmentB = createEnrollment(programB, duplicate, organisationUnitA);
    enrollmentB.setUid("enrollmentB");
    duplicate.getEnrollments().add(enrollmentB);

    MergeObject generatedMergeObject = deduplicationHelper.generateMergeObject(original, duplicate);

    assertEquals("enrollmentB", generatedMergeObject.getEnrollments().get(0));
  }

  @Test
  void shouldFailGenerateMergeObjectEnrollmentsSameProgram() {
    TrackedEntity original = getTrackedEntityA();

    Program program = createProgram('A');
    Enrollment enrollmentA = createEnrollment(program, original, organisationUnitA);
    original.getEnrollments().add(enrollmentA);

    TrackedEntity duplicate = getTrackedEntityA();
    Enrollment enrollmentB = createEnrollment(program, duplicate, organisationUnitA);
    duplicate.getEnrollments().add(enrollmentB);

    assertThrows(
        PotentialDuplicateConflictException.class,
        () -> deduplicationHelper.generateMergeObject(original, duplicate));
  }

  @Test
  void shouldFailGetDuplicateRelationshipErrorWithDuplicateRelationshipsWithTrackedEntities() {
    TrackedEntity teA = getTrackedEntityA();
    TrackedEntity teB = getTrackedEntityB();
    TrackedEntity teC = getTrackedEntityB();

    // A->C, B->C
    RelationshipItem fromA = new RelationshipItem();
    RelationshipItem toA = new RelationshipItem();
    RelationshipItem fromB = new RelationshipItem();
    RelationshipItem toB = new RelationshipItem();

    fromA.setTrackedEntity(teA);
    toA.setTrackedEntity(teC);
    fromB.setTrackedEntity(teB);
    toB.setTrackedEntity(teC);

    Relationship relA = new Relationship();
    Relationship relB = new Relationship();

    relA.setAutoFields();
    relB.setAutoFields();

    relA.setRelationshipType(relationshipType);
    relB.setRelationshipType(relationshipType);

    relA.setFrom(fromA);
    relA.setTo(toA);
    relB.setFrom(fromB);
    relB.setTo(toB);

    fromA.setRelationship(relA);
    toA.setRelationship(relA);

    fromB.setRelationship(relB);
    toB.setRelationship(relB);

    teA.getRelationshipItems().add(fromA);
    teB.getRelationshipItems().add(fromB);

    assertNotNull(
        deduplicationHelper.getDuplicateRelationshipError(
            teA,
            teB.getRelationshipItems().stream()
                .map(RelationshipItem::getRelationship)
                .collect(Collectors.toSet())));
  }

  @Test
  void
      shouldFailGetDuplicateRelationshipErrorWithDuplicateRelationshipsWithTrackedEntitiesBidirectional() {
    TrackedEntity teA = getTrackedEntityA();
    TrackedEntity teB = getTrackedEntityB();
    TrackedEntity teC = getTrackedEntityB();

    // A->C, B->C
    RelationshipItem fromA = new RelationshipItem();
    RelationshipItem toA = new RelationshipItem();
    RelationshipItem fromB = new RelationshipItem();
    RelationshipItem toB = new RelationshipItem();

    fromA.setTrackedEntity(teC);
    toA.setTrackedEntity(teA);
    fromB.setTrackedEntity(teB);
    toB.setTrackedEntity(teC);

    Relationship relA = new Relationship();
    Relationship relB = new Relationship();

    relA.setAutoFields();
    relB.setAutoFields();

    relA.setRelationshipType(relationshipTypeBidirectional);
    relB.setRelationshipType(relationshipTypeBidirectional);

    relA.setFrom(fromA);
    relA.setTo(toA);
    relB.setFrom(fromB);
    relB.setTo(toB);

    fromA.setRelationship(relA);
    toA.setRelationship(relA);

    fromB.setRelationship(relB);
    toB.setRelationship(relB);

    teA.getRelationshipItems().add(fromA);
    teB.getRelationshipItems().add(fromB);

    assertNotNull(
        deduplicationHelper.getDuplicateRelationshipError(
            teA,
            teB.getRelationshipItems().stream()
                .map(RelationshipItem::getRelationship)
                .collect(Collectors.toSet())));
  }

  private List<Relationship> getRelationships() {
    Relationship relationshipA = new Relationship();
    relationshipA.setRelationshipType(relationshipType);

    return Lists.newArrayList(relationshipA);
  }

  private List<Enrollment> getEnrollments() {
    return Lists.newArrayList(enrollment);
  }

  private TrackedEntity getTrackedEntityA() {
    TrackedEntity te = createTrackedEntity(organisationUnitA);
    te.setTrackedEntityType(trackedEntityTypeA);

    return te;
  }

  private TrackedEntity getTrackedEntityB() {
    TrackedEntity te = createTrackedEntity(organisationUnitB);
    te.setTrackedEntityType(trackedEntityTypeB);

    return te;
  }

  private User getNoMergeAuthsUser() {
    return makeUser("A", Lists.newArrayList("USELESS_AUTH"));
  }

  private Relationship getRelationship() {
    Relationship relationship = new Relationship();
    relationship.setAutoFields();
    relationship.setRelationshipType(relationshipType);

    return relationship;
  }

  private Relationship getRelationship(RelationshipItem to, RelationshipItem from) {
    Relationship relationship = getRelationship();
    relationship.setTo(to);
    relationship.setFrom(from);

    return relationship;
  }

  private RelationshipItem getRelationshipItem(
      Relationship relationship, TrackedEntity trackedEntity) {
    RelationshipItem relationshipItem = new RelationshipItem();
    relationshipItem.setRelationship(relationship);
    relationshipItem.setTrackedEntity(trackedEntity);

    return relationshipItem;
  }
}
