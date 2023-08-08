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
package org.hisp.dhis.deduplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeduplicationHelper {
  private final CurrentUserService currentUserService;

  private final AclService aclService;

  private final RelationshipService relationshipService;

  private final OrganisationUnitService organisationUnitService;

  private final EnrollmentService enrollmentService;

  public String getInvalidReferenceErrors(DeduplicationMergeParams params) {
    TrackedEntity original = params.getOriginal();
    TrackedEntity duplicate = params.getDuplicate();
    MergeObject mergeObject = params.getMergeObject();

    /*
     * Step 1: Make sure all uids in mergeObject is valid
     */
    List<String> uids =
        ListUtils.distinctUnion(
            mergeObject.getTrackedEntityAttributes(),
            mergeObject.getEnrollments(),
            mergeObject.getRelationships());
    for (String uid : uids) {
      if (!CodeGenerator.isValidUid(uid)) {
        return "Invalid uid '" + uid + "'.";
      }
    }

    /*
     * Step 2: Make sure all references objects exists in duplicate
     */
    Set<String> validTrackedEntityAttributes =
        duplicate.getTrackedEntityAttributeValues().stream()
            .map(teav -> teav.getAttribute().getUid())
            .collect(Collectors.toSet());

    Set<String> validRelationships =
        duplicate.getRelationshipItems().stream()
            .map(rel -> rel.getRelationship().getUid())
            .collect(Collectors.toSet());

    Set<String> validEnrollments =
        duplicate.getEnrollments().stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet());

    for (String tea : mergeObject.getTrackedEntityAttributes()) {
      if (!validTrackedEntityAttributes.contains(tea)) {
        return "Duplicate has no attribute '" + tea + "'.";
      }
    }

    String rel =
        mergeObject.getRelationships().stream()
            .filter(r -> !validRelationships.contains(r))
            .findFirst()
            .orElse(null);

    if (rel != null) {
      return "Duplicate has no relationship '" + rel + "'.";
    }

    for (String enrollmentUid : mergeObject.getEnrollments()) {
      if (!validEnrollments.contains(enrollmentUid)) {
        return "Duplicate has no enrollment '" + enrollmentUid + "'.";
      }
    }

    /*
     * Step 3: Duplicate Relationships and Enrollments
     */
    Set<Relationship> relationshipsToMerge =
        params.getDuplicate().getRelationshipItems().stream()
            .map(RelationshipItem::getRelationship)
            .filter(r -> mergeObject.getRelationships().contains(r.getUid()))
            .collect(Collectors.toSet());
    String duplicateRelationshipError =
        getDuplicateRelationshipError(params.getOriginal(), relationshipsToMerge);

    if (duplicateRelationshipError != null) {
      return "Invalid relationship '"
          + duplicateRelationshipError
          + "'. A similar relationship already exists on original.";
    }

    Set<String> programUidOfExistingEnrollments =
        original.getEnrollments().stream()
            .map(Enrollment::getProgram)
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet());

    String duplicateEnrollment =
        duplicate.getEnrollments().stream()
            .filter(enrollment -> mergeObject.getEnrollments().contains(enrollment.getUid()))
            .map(Enrollment::getProgram)
            .map(IdentifiableObject::getUid)
            .filter(programUidOfExistingEnrollments::contains)
            .findAny()
            .orElse(null);

    if (duplicateEnrollment != null) {
      return "Invalid enrollment '"
          + duplicateEnrollment
          + "'. A similar enrollment already exists on original.";
    }

    /*
     * Step 4: Make sure no relationships will become self-referencing.
     */
    Set<String> relationshipsToMergeUids =
        relationshipsToMerge.stream().map(IdentifiableObject::getUid).collect(Collectors.toSet());

    String selfReferencingRelationship =
        original.getRelationshipItems().stream()
            .map(RelationshipItem::getRelationship)
            .map(IdentifiableObject::getUid)
            .filter(relationshipsToMergeUids::contains)
            .findFirst()
            .orElse(null);

    if (selfReferencingRelationship != null) {
      return "Invalid relationship '"
          + selfReferencingRelationship
          + "'. Relationship is between original and duplicate.";
    }

    return null;
  }

  public String getDuplicateRelationshipError(
      TrackedEntity original, Set<Relationship> relationships) {
    Set<Relationship> originalRelationships =
        original.getRelationshipItems().stream()
            .map(RelationshipItem::getRelationship)
            .collect(Collectors.toSet());

    for (Relationship originalRel : originalRelationships) {
      for (Relationship rel : relationships) {
        if (isSameRelationship(originalRel, rel)) {
          return rel.getUid();
        }
      }
    }

    return null;
  }

  private boolean isSameRelationship(Relationship a, Relationship b) {
    /*
     * relationshipType must be the same for unidirectional, to and to OR
     * from and from must be the same for bidirectional, to and from OR from
     * and to must be the same (Unless the previous check was true)
     */
    return a.getRelationshipType().equals(b.getRelationshipType())
        && ((isSameRelationshipItem(a.getFrom(), b.getFrom())
                || isSameRelationshipItem(a.getTo(), b.getTo()))
            || (a.getRelationshipType().isBidirectional()
                && (isSameRelationshipItem(a.getFrom(), b.getTo())
                    || isSameRelationshipItem(a.getTo(), b.getFrom()))));
  }

  private boolean isSameRelationshipItem(RelationshipItem a, RelationshipItem b) {
    IdentifiableObject idoA =
        ObjectUtils.firstNonNull(a.getTrackedEntity(), a.getEnrollment(), a.getEvent());
    IdentifiableObject idoB =
        ObjectUtils.firstNonNull(b.getTrackedEntity(), b.getEnrollment(), b.getEvent());

    return idoA.getUid().equals(idoB.getUid());
  }

  public MergeObject generateMergeObject(TrackedEntity original, TrackedEntity duplicate)
      throws PotentialDuplicateForbiddenException, PotentialDuplicateConflictException {
    if (!duplicate.getTrackedEntityType().equals(original.getTrackedEntityType())) {
      throw new PotentialDuplicateForbiddenException(
          "Potential Duplicate does not have the same tracked entity type as the original");
    }

    List<String> attributes = getMergeableAttributes(original, duplicate);

    List<String> relationships = getMergeableRelationships(original, duplicate);

    List<String> enrollments = getMergeableEnrollments(original, duplicate);

    return MergeObject.builder()
        .trackedEntityAttributes(attributes)
        .relationships(relationships)
        .enrollments(enrollments)
        .build();
  }

  public String getUserAccessErrors(
      TrackedEntity original, TrackedEntity duplicate, MergeObject mergeObject) {
    User user = currentUserService.getCurrentUser();

    if (user == null
        || !(user.isAuthorized("ALL") || user.isAuthorized("F_TRACKED_ENTITY_MERGE"))) {
      return "Missing required authority for merging tracked entities.";
    }

    if (!aclService.canDataWrite(user, original.getTrackedEntityType())
        || !aclService.canDataWrite(user, duplicate.getTrackedEntityType())) {
      return "Missing data write access to Tracked Entity Type.";
    }

    List<RelationshipType> relationshipTypes =
        relationshipService.getRelationships(mergeObject.getRelationships()).stream()
            .map(Relationship::getRelationshipType)
            .distinct()
            .collect(Collectors.toList());

    if (relationshipTypes.stream().anyMatch(rt -> !aclService.canDataWrite(user, rt))) {
      return "Missing data write access to one or more Relationship Types.";
    }

    List<Enrollment> enrollments = enrollmentService.getEnrollments(mergeObject.getEnrollments());

    if (enrollments.stream().anyMatch(e -> !aclService.canDataWrite(user, e.getProgram()))) {
      return "Missing data write access to one or more Programs.";
    }

    if (!organisationUnitService.isInUserHierarchyCached(user, original.getOrganisationUnit())
        || !organisationUnitService.isInUserHierarchyCached(
            user, duplicate.getOrganisationUnit())) {
      return "Missing access to organisation unit of one or both entities.";
    }

    return null;
  }

  private List<String> getMergeableAttributes(TrackedEntity original, TrackedEntity duplicate)
      throws PotentialDuplicateConflictException {
    Map<String, String> existingTeavs =
        original.getTrackedEntityAttributeValues().stream()
            .collect(
                Collectors.toMap(
                    teav -> teav.getAttribute().getUid(), TrackedEntityAttributeValue::getValue));

    List<String> attributes = new ArrayList<>();

    for (TrackedEntityAttributeValue teav : duplicate.getTrackedEntityAttributeValues()) {
      String existingVal = existingTeavs.get(teav.getAttribute().getUid());

      if (existingVal != null) {
        if (!existingVal.equals(teav.getValue())) {
          throw new PotentialDuplicateConflictException(
              "Potential Duplicate contains conflicting value and cannot be merged.");
        }
      } else {
        attributes.add(teav.getAttribute().getUid());
      }
    }

    return attributes;
  }

  private List<String> getMergeableRelationships(TrackedEntity original, TrackedEntity duplicate) {
    List<String> relationships = new ArrayList<>();

    for (RelationshipItem ri : duplicate.getRelationshipItems()) {
      TrackedEntity from = ri.getRelationship().getFrom().getTrackedEntity();
      TrackedEntity to = ri.getRelationship().getTo().getTrackedEntity();

      if ((from != null && from.getUid().equals(original.getUid()))
          || (to != null && to.getUid().equals(original.getUid()))) {
        continue;
      }

      boolean duplicateRelationship = false;

      for (RelationshipItem ri2 : original.getRelationshipItems()) {
        TrackedEntity originalFrom = ri2.getRelationship().getFrom().getTrackedEntity();
        TrackedEntity originalTo = ri2.getRelationship().getTo().getTrackedEntity();

        if ((originalFrom != null && originalFrom.getUid().equals(duplicate.getUid()))
            || (originalTo != null && originalTo.getUid().equals(duplicate.getUid()))) {
          continue;
        }

        if (isSameRelationship(ri2.getRelationship(), ri.getRelationship())) {
          duplicateRelationship = true;
          break;
        }
      }

      if (duplicateRelationship) {
        continue;
      }

      relationships.add(ri.getRelationship().getUid());
    }

    return relationships;
  }

  private List<String> getMergeableEnrollments(TrackedEntity original, TrackedEntity duplicate)
      throws PotentialDuplicateConflictException {
    List<String> enrollments = new ArrayList<>();

    Set<String> programs =
        original.getEnrollments().stream()
            .filter(e -> !e.isDeleted())
            .map(e -> e.getProgram().getUid())
            .collect(Collectors.toSet());

    for (Enrollment enrollment : duplicate.getEnrollments()) {
      if (programs.contains(enrollment.getProgram().getUid())) {
        throw new PotentialDuplicateConflictException(
            "Potential Duplicate contains enrollments with the same program"
                + " and cannot be merged.");
      }
      enrollments.add(enrollment.getUid());
    }

    return enrollments;
  }
}
