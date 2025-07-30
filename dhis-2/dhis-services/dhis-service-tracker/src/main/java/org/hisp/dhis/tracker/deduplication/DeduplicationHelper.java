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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeduplicationHelper {
  private final AclService aclService;

  private final RelationshipService relationshipService;

  private final OrganisationUnitService organisationUnitService;

  private final EnrollmentService enrollmentService;

  private final UserService userService;

  public String getInvalidReferenceErrors(DeduplicationMergeParams params) {
    TrackedEntity original = params.getOriginal();
    TrackedEntity duplicate = params.getDuplicate();
    MergeObject mergeObject = params.getMergeObject();

    /*
     * Step 1: Make sure all reference objects exists in duplicate
     */
    Set<UID> validTrackedEntityAttributes =
        duplicate.getTrackedEntityAttributeValues().stream()
            .map(teav -> UID.of(teav.getAttribute()))
            .collect(Collectors.toSet());

    Set<UID> validRelationships =
        duplicate.getRelationshipItems().stream()
            .map(rel -> UID.of(rel.getRelationship()))
            .collect(Collectors.toSet());

    Set<UID> validEnrollments =
        duplicate.getEnrollments().stream().map(UID::of).collect(Collectors.toSet());

    for (UID tea : mergeObject.getTrackedEntityAttributes()) {
      if (!validTrackedEntityAttributes.contains(tea)) {
        return "Duplicate has no attribute '" + tea + "'.";
      }
    }

    UID rel =
        mergeObject.getRelationships().stream()
            .filter(r -> !validRelationships.contains(r))
            .findFirst()
            .orElse(null);

    if (rel != null) {
      return "Duplicate has no relationship '" + rel + "'.";
    }

    for (UID enrollmentUid : mergeObject.getEnrollments()) {
      if (!validEnrollments.contains(enrollmentUid)) {
        return "Duplicate has no enrollment '" + enrollmentUid + "'.";
      }
    }

    /*
     * Step 2: Duplicate Relationships and Enrollments
     */
    Set<Relationship> relationshipsToMerge =
        params.getDuplicate().getRelationshipItems().stream()
            .map(RelationshipItem::getRelationship)
            .filter(r -> mergeObject.getRelationships().contains(UID.of(r)))
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
            .filter(enrollment -> mergeObject.getEnrollments().contains(UID.of(enrollment)))
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
     * Step 3: Make sure no relationships will become self-referencing.
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
        ObjectUtils.firstNonNull(
            a.getTrackedEntity(), a.getEnrollment(), a.getTrackerEvent(), a.getSingleEvent());
    IdentifiableObject idoB =
        ObjectUtils.firstNonNull(
            b.getTrackedEntity(), b.getEnrollment(), b.getTrackerEvent(), a.getSingleEvent());

    return idoA.getUid().equals(idoB.getUid());
  }

  public MergeObject generateMergeObject(TrackedEntity original, TrackedEntity duplicate)
      throws PotentialDuplicateForbiddenException, PotentialDuplicateConflictException {
    if (!duplicate.getTrackedEntityType().equals(original.getTrackedEntityType())) {
      throw new PotentialDuplicateForbiddenException(
          "Potential Duplicate does not have the same tracked entity type as the original");
    }

    Set<UID> attributes = getMergeableAttributes(original, duplicate);

    Set<UID> relationships = getMergeableRelationships(original, duplicate);

    Set<UID> enrollments = getMergeableEnrollments(original, duplicate);

    return MergeObject.builder()
        .trackedEntityAttributes(attributes)
        .relationships(relationships)
        .enrollments(enrollments)
        .build();
  }

  public String getUserAccessErrors(
      TrackedEntity original, TrackedEntity duplicate, MergeObject mergeObject)
      throws ForbiddenException, NotFoundException {

    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();

    if (!(currentUserDetails.getAllAuthorities().contains(ALL.name())
        || currentUserDetails
            .getAllAuthorities()
            .contains(Authorities.F_TRACKED_ENTITY_MERGE.name()))) {
      return "Missing required authority for merging tracked entities.";
    }

    if (!aclService.canDataWrite(currentUserDetails, original.getTrackedEntityType())
        || !aclService.canDataWrite(currentUserDetails, duplicate.getTrackedEntityType())) {
      return "Missing data write access to Tracked Entity Type.";
    }

    List<RelationshipType> relationshipTypes =
        relationshipService.findRelationships(mergeObject.getRelationships()).stream()
            .map(Relationship::getRelationshipType)
            .distinct()
            .toList();

    if (relationshipTypes.stream()
        .anyMatch(rt -> !aclService.canDataWrite(currentUserDetails, rt))) {
      return "Missing data write access to one or more Relationship Types.";
    }

    List<Enrollment> enrollments = enrollmentService.findEnrollments(mergeObject.getEnrollments());

    if (enrollments.stream()
        .anyMatch(e -> !aclService.canDataWrite(currentUserDetails, e.getProgram()))) {
      return "Missing data write access to one or more Programs.";
    }

    User user = userService.getUserByUsername(currentUserDetails.getUsername());

    if (!organisationUnitService.isInUserHierarchyCached(user, original.getOrganisationUnit())
        || !organisationUnitService.isInUserHierarchyCached(
            user, duplicate.getOrganisationUnit())) {
      return "Missing access to organisation unit of one or both entities.";
    }

    return null;
  }

  private Set<UID> getMergeableAttributes(TrackedEntity original, TrackedEntity duplicate)
      throws PotentialDuplicateConflictException {
    Map<UID, String> existingTeavs =
        original.getTrackedEntityAttributeValues().stream()
            .collect(
                Collectors.toMap(
                    teav -> UID.of(teav.getAttribute()), TrackedEntityAttributeValue::getValue));

    Set<UID> attributes = new HashSet<>();

    for (TrackedEntityAttributeValue teav : duplicate.getTrackedEntityAttributeValues()) {
      String existingVal = existingTeavs.get(UID.of(teav.getAttribute()));

      if (existingVal != null) {
        if (!existingVal.equals(teav.getValue())) {
          throw new PotentialDuplicateConflictException(
              "Potential Duplicate contains conflicting value and cannot be merged.");
        }
      } else {
        attributes.add(UID.of(teav.getAttribute()));
      }
    }

    return attributes;
  }

  private Set<UID> getMergeableRelationships(TrackedEntity original, TrackedEntity duplicate) {
    Set<UID> relationships = new HashSet<>();

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

      relationships.add(UID.of(ri.getRelationship()));
    }

    return relationships;
  }

  private Set<UID> getMergeableEnrollments(TrackedEntity original, TrackedEntity duplicate)
      throws PotentialDuplicateConflictException {
    Set<UID> enrollments = new HashSet<>();

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
      enrollments.add(UID.of(enrollment));
    }

    return enrollments;
  }
}
