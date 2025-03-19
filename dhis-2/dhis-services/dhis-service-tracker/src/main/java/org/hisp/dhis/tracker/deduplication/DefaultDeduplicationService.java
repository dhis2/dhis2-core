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

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.imports.bundle.persister.TrackerObjectDeletionService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("org.hisp.dhis.tracker.deduplication.domain.deduplication.DeduplicationService")
@RequiredArgsConstructor
public class DefaultDeduplicationService implements DeduplicationService {
  private final HibernatePotentialDuplicateStore potentialDuplicateStore;

  private final TrackerObjectDeletionService trackerObjectDeletionService;

  private final DeduplicationHelper deduplicationHelper;

  private final UserService userService;

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public PotentialDuplicate getPotentialDuplicate(@Nonnull UID uid) throws NotFoundException {
    PotentialDuplicate potentialDuplicate = potentialDuplicateStore.getByUid(uid.getValue());
    if (potentialDuplicate == null) {
      throw new NotFoundException(PotentialDuplicate.class, uid);
    }
    return potentialDuplicate;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean exists(@Nonnull PotentialDuplicate potentialDuplicate)
      throws PotentialDuplicateConflictException {
    return potentialDuplicateStore.exists(potentialDuplicate);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public List<PotentialDuplicate> getPotentialDuplicates(
      @Nonnull PotentialDuplicateCriteria criteria) {
    return potentialDuplicateStore.getPotentialDuplicates(criteria);
  }

  @Nonnull
  @Override
  public Page<PotentialDuplicate> getPotentialDuplicates(
      @Nonnull PotentialDuplicateCriteria criteria, @Nonnull PageParams pageParams) {
    return potentialDuplicateStore.getPotentialDuplicates(criteria, pageParams);
  }

  @Override
  @Transactional
  public void updatePotentialDuplicate(@Nonnull PotentialDuplicate potentialDuplicate) {
    setPotentialDuplicateUserNameInfo(potentialDuplicate);
    potentialDuplicateStore.update(potentialDuplicate);
  }

  @Override
  @Transactional
  public void autoMerge(@Nonnull DeduplicationMergeParams params)
      throws PotentialDuplicateConflictException,
          PotentialDuplicateForbiddenException,
          ForbiddenException,
          NotFoundException {
    validateCanBeAutoMerged(params);

    params.setMergeObject(
        deduplicationHelper.generateMergeObject(params.getOriginal(), params.getDuplicate()));
    merge(params);
  }

  @Override
  @Transactional
  public void manualMerge(@Nonnull DeduplicationMergeParams deduplicationMergeParams)
      throws PotentialDuplicateForbiddenException,
          ForbiddenException,
          NotFoundException,
          PotentialDuplicateConflictException {
    String invalidReference =
        deduplicationHelper.getInvalidReferenceErrors(deduplicationMergeParams);
    if (invalidReference != null) {
      throw new PotentialDuplicateConflictException("Merging conflict: " + invalidReference);
    }

    merge(deduplicationMergeParams);
  }

  private void validateCanBeAutoMerged(DeduplicationMergeParams params)
      throws PotentialDuplicateConflictException {
    TrackedEntity original = params.getOriginal();
    TrackedEntity duplicate = params.getDuplicate();

    String prefix = "PotentialDuplicate cannot be merged automatically: ";
    if (!original.getTrackedEntityType().equals(duplicate.getTrackedEntityType())) {
      throw new PotentialDuplicateConflictException(
          prefix + "Entities have different Tracked Entity Types.");
    }

    if (original.isDeleted() || duplicate.isDeleted()) {
      throw new PotentialDuplicateConflictException(
          prefix + "One or both entities have already been marked as deleted.");
    }

    if (haveSameEnrollment(original.getEnrollments(), duplicate.getEnrollments())) {
      throw new PotentialDuplicateConflictException(
          prefix + "Both entities enrolled in the same program.");
    }

    Set<TrackedEntityAttributeValue> trackedEntityAttributeValueA =
        original.getTrackedEntityAttributeValues();
    Set<TrackedEntityAttributeValue> trackedEntityAttributeValueB =
        duplicate.getTrackedEntityAttributeValues();

    if (sameAttributesAreEquals(trackedEntityAttributeValueA, trackedEntityAttributeValueB)) {
      throw new PotentialDuplicateConflictException(
          prefix + "Entities have conflicting values for the same attributes.");
    }
  }

  private void merge(DeduplicationMergeParams params)
      throws PotentialDuplicateForbiddenException, ForbiddenException, NotFoundException {
    TrackedEntity original = params.getOriginal();
    TrackedEntity duplicate = params.getDuplicate();
    MergeObject mergeObject = params.getMergeObject();

    String accessError = deduplicationHelper.getUserAccessErrors(original, duplicate, mergeObject);

    if (accessError != null) {
      throw new PotentialDuplicateForbiddenException("Insufficient access: " + accessError);
    }

    potentialDuplicateStore.moveTrackedEntityAttributeValues(
        original, duplicate, mergeObject.getTrackedEntityAttributes());
    potentialDuplicateStore.moveRelationships(original, duplicate, mergeObject.getRelationships());
    potentialDuplicateStore.moveEnrollments(original, duplicate, mergeObject.getEnrollments());
    try {
      trackerObjectDeletionService.deleteTrackedEntities(List.of(UID.of(duplicate)));
    } catch (NotFoundException e) {
      throw new RuntimeException("Could not find TrackedEntity: " + duplicate.getUid());
    }
    updateTeAndPotentialDuplicate(params, original);
    potentialDuplicateStore.auditMerge(params);
  }

  private boolean haveSameEnrollment(
      Set<Enrollment> originalEnrollments, Set<Enrollment> duplicateEnrollments) {
    Set<String> originalPrograms =
        originalEnrollments.stream()
            .filter(e -> !e.isDeleted())
            .map(e -> e.getProgram().getUid())
            .collect(Collectors.toSet());
    Set<String> duplicatePrograms =
        duplicateEnrollments.stream()
            .filter(e -> !e.isDeleted())
            .map(e -> e.getProgram().getUid())
            .collect(Collectors.toSet());

    originalPrograms.retainAll(duplicatePrograms);

    return !originalPrograms.isEmpty();
  }

  private void updateTeAndPotentialDuplicate(
      DeduplicationMergeParams deduplicationMergeParams, TrackedEntity original) {
    updateOriginalTe(original);
    updatePotentialDuplicateStatus(deduplicationMergeParams.getPotentialDuplicate());
  }

  private void updatePotentialDuplicateStatus(PotentialDuplicate potentialDuplicate) {
    setPotentialDuplicateUserNameInfo(potentialDuplicate);
    potentialDuplicate.setStatus(DeduplicationStatus.MERGED);
    potentialDuplicateStore.update(potentialDuplicate);
  }

  private void updateOriginalTe(TrackedEntity original) {
    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    original.setLastUpdated(new Date());
    original.setLastUpdatedBy(currentUser);
    original.setLastUpdatedByUserInfo(
        UserInfoSnapshot.from(CurrentUserUtil.getCurrentUserDetails()));
  }

  private boolean sameAttributesAreEquals(
      Set<TrackedEntityAttributeValue> trackedEntityAttributeValueA,
      Set<TrackedEntityAttributeValue> trackedEntityAttributeValueB) {
    if (trackedEntityAttributeValueA.isEmpty() || trackedEntityAttributeValueB.isEmpty()) {
      return false;
    }

    for (TrackedEntityAttributeValue teavA : trackedEntityAttributeValueA) {
      for (TrackedEntityAttributeValue teavB : trackedEntityAttributeValueB) {
        if (teavA.getAttribute().equals(teavB.getAttribute())
            && !teavA.getValue().equals(teavB.getValue())) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  @Transactional
  public void addPotentialDuplicate(@Nonnull PotentialDuplicate potentialDuplicate)
      throws PotentialDuplicateConflictException {
    if (potentialDuplicate.getStatus() != DeduplicationStatus.OPEN) {
      throw new PotentialDuplicateConflictException(
          String.format(
              "Invalid status %s, creating potential duplicate is allowed using: %s",
              potentialDuplicate.getStatus(), DeduplicationStatus.OPEN));
    }

    setPotentialDuplicateUserNameInfo(potentialDuplicate);
    potentialDuplicateStore.save(potentialDuplicate);
  }

  private void setPotentialDuplicateUserNameInfo(PotentialDuplicate potentialDuplicate) {
    if (potentialDuplicate.getCreatedByUserName() == null) {
      potentialDuplicate.setCreatedByUserName(CurrentUserUtil.getCurrentUsername());
    }
    potentialDuplicate.setLastUpdatedByUserName(CurrentUserUtil.getCurrentUsername());
  }
}
