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
package org.hisp.dhis.tracker.acl;

import static org.hisp.dhis.tracker.acl.TrackerOwnershipManager.NO_READ_ACCESS_TO_ORG_UNIT;
import static org.hisp.dhis.tracker.acl.TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.UserDetails;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
@RequiredArgsConstructor
@Component
public class DefaultTrackerAccessManager implements TrackerAccessManager {

  private final AclService aclService;
  private final TrackerOwnershipManager ownershipAccessManager;
  private final TrackerProgramService trackerProgramService;

  @Override
  public List<String> canRead(
      @Nonnull UserDetails user, @CheckForNull TrackedEntity trackedEntity) {
    if (user.isSuper() || trackedEntity == null) {
      return List.of();
    }

    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();
    if (!aclService.canDataRead(user, trackedEntityType)) {
      return List.of(
          "User has no data read access to tracked entity type: " + trackedEntityType.getUid());
    }

    List<Program> tetPrograms =
        trackerProgramService.getAccessibleTrackerPrograms(trackedEntityType);
    if (tetPrograms.isEmpty()) {
      return List.of("User has no access to any program");
    }

    if (tetPrograms.stream()
        .anyMatch(p -> ownershipAccessManager.hasAccess(user, trackedEntity, p))) {
      return List.of();
    } else {
      return List.of(OWNERSHIP_ACCESS_DENIED);
    }
  }

  @Override
  public List<String> canCreate(@Nonnull UserDetails user, TrackedEntity trackedEntity) {
    if (user.isSuper() || trackedEntity == null) {
      return List.of();
    }

    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();

    if (!aclService.canDataWrite(user, trackedEntityType)) {
      return List.of(
          "User has no data write access to tracked entity type: " + trackedEntityType.getUid());
    }

    return List.of();
  }

  @Override
  public List<String> canUpdate(UserDetails user, TrackedEntity trackedEntity) {
    if (user.isSuper() || trackedEntity == null) {
      return List.of();
    }

    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();

    if (!aclService.canDataWrite(user, trackedEntityType)) {
      return List.of(
          "User has no data write access to tracked entity type: " + trackedEntityType.getUid());
    }

    List<Program> tetPrograms =
        trackerProgramService.getAccessibleTrackerPrograms(trackedEntityType);

    if (tetPrograms.isEmpty()) {
      return List.of("User has no access to any program");
    }

    if (tetPrograms.stream().anyMatch(p -> canWrite(user, trackedEntity, p))) {
      return List.of();
    } else {
      return List.of(OWNERSHIP_ACCESS_DENIED);
    }
  }

  @Override
  public List<String> canDelete(@NotNull UserDetails user, TrackedEntity trackedEntity) {
    return canUpdate(user, trackedEntity);
  }

  /** Check Program data write access and Tracked Entity Program Ownership */
  private boolean canWrite(UserDetails user, TrackedEntity trackedEntity, Program program) {
    return aclService.canDataWrite(user, program)
        && ownershipAccessManager.hasAccess(user, trackedEntity, program);
  }

  @Override
  public List<String> canRead(
      @Nonnull UserDetails user, Enrollment enrollment, boolean skipOwnershipCheck) {
    if (user.isSuper() || enrollment == null) {
      return List.of();
    }

    Program program = enrollment.getProgram();
    List<String> errors = new ArrayList<>();
    if (!aclService.canDataRead(user, program)) {
      errors.add("User has no data read access to program: " + program.getUid());
    }

    if (!aclService.canDataRead(user, program.getTrackedEntityType())) {
      errors.add(
          "User has no data read access to tracked entity type: "
              + program.getTrackedEntityType().getUid());
    }

    if (!skipOwnershipCheck
        && !ownershipAccessManager.hasAccess(user, enrollment.getTrackedEntity(), program)) {
      errors.add(OWNERSHIP_ACCESS_DENIED);
    }

    return errors;
  }

  @Override
  public List<String> canCreate(
      @Nonnull UserDetails user, Enrollment enrollment, boolean skipOwnershipCheck) {
    if (user.isSuper() || enrollment == null) {
      return List.of();
    }

    Program program = enrollment.getProgram();
    List<String> errors = new ArrayList<>();
    OrganisationUnit ou = enrollment.getOrganisationUnit();
    if (ou != null && !user.isInUserHierarchy(ou.getStoredPath())) {
      errors.add("User has no create access to organisation unit: " + ou.getUid());
    }

    if (!aclService.canDataWrite(user, program)) {
      errors.add("User has no data write access to program: " + program.getUid());
    }

    if (!aclService.canDataRead(user, program.getTrackedEntityType())) {
      errors.add(
          "User has no data read access to tracked entity type: "
              + (program.getTrackedEntityType() != null
                  ? program.getTrackedEntityType().getUid()
                  : null));
    }

    if (!skipOwnershipCheck
        && !ownershipAccessManager.hasAccess(user, enrollment.getTrackedEntity(), program)) {
      errors.add(OWNERSHIP_ACCESS_DENIED);
    }

    return errors;
  }

  @Override
  public List<String> canUpdate(
      @Nonnull UserDetails user, Enrollment enrollment, boolean skipOwnershipCheck) {
    if (user.isSuper() || enrollment == null) {
      return List.of();
    }

    Program program = enrollment.getProgram();
    List<String> errors = new ArrayList<>();
    if (!aclService.canDataWrite(user, program)) {
      errors.add("User has no data write access to program: " + program.getUid());
    }

    if (!aclService.canDataRead(user, program.getTrackedEntityType())) {
      errors.add(
          "User has no data read access to tracked entity type: "
              + program.getTrackedEntityType().getUid());
    }

    if (!skipOwnershipCheck
        && !ownershipAccessManager.hasAccess(user, enrollment.getTrackedEntity(), program)) {
      errors.add(OWNERSHIP_ACCESS_DENIED);
    }

    return errors;
  }

  @Override
  public List<String> canDelete(
      @Nonnull UserDetails user, Enrollment enrollment, boolean skipOwnershipCheck) {
    return canUpdate(user, enrollment, skipOwnershipCheck);
  }

  @Override
  public List<String> canRead(
      @Nonnull UserDetails user, TrackerEvent event, boolean skipOwnershipCheck) {
    if (user.isSuper() || event == null) {
      return List.of();
    }

    ProgramStage programStage = event.getProgramStage();

    if (isNull(programStage)) {
      return List.of();
    }

    Program program = programStage.getProgram();
    List<String> errors = new ArrayList<>();
    if (!aclService.canDataRead(user, program)) {
      errors.add("User has no data read access to program: " + program.getUid());
    }

    if (!aclService.canDataRead(user, programStage)) {
      errors.add("User has no data read access to program stage: " + programStage.getUid());
    }

    if (!aclService.canDataRead(user, program.getTrackedEntityType())) {
      errors.add(
          "User has no data read access to tracked entity type: "
              + program.getTrackedEntityType().getUid());
    }

    if (!skipOwnershipCheck
        && !ownershipAccessManager.hasAccess(
            user, event.getEnrollment().getTrackedEntity(), program)) {
      errors.add(OWNERSHIP_ACCESS_DENIED);
    }

    errors.addAll(canRead(user, event.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canRead(
      @Nonnull UserDetails user,
      TrackerEvent event,
      DataElement dataElement,
      boolean skipOwnershipCheck) {

    if (user.isSuper()) {
      return List.of();
    }

    List<String> errors = new ArrayList<>();
    errors.addAll(canRead(user, event, skipOwnershipCheck));

    if (!aclService.canRead(user, dataElement)) {
      errors.add("User has no read access to data element: " + dataElement.getUid());
    }

    return errors;
  }

  @Override
  public List<String> canCreate(
      @Nonnull UserDetails user, TrackerEvent event, boolean skipOwnershipCheck) {
    if (user.isSuper() || event == null) {
      return List.of();
    }

    ProgramStage programStage = event.getProgramStage();

    if (isNull(programStage)) {
      return List.of();
    }

    Program program = programStage.getProgram();
    List<String> errors = new ArrayList<>();
    OrganisationUnit ou = event.getOrganisationUnit();
    if (ou != null) {
      boolean isInHierarchy =
          event.isCreatableInSearchScope()
              ? user.isInUserEffectiveSearchOrgUnitHierarchy(ou.getStoredPath())
              : user.isInUserHierarchy(ou.getStoredPath());

      if (!isInHierarchy) {
        errors.add("User has no create access to organisation unit: " + ou.getUid());
      }
    }

    canCreateOrDeleteWithRegistration(
        errors, user, event, skipOwnershipCheck, programStage, program);

    errors.addAll(canWrite(user, event.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canUpdate(
      @Nonnull UserDetails user, TrackerEvent event, boolean skipOwnershipCheck) {
    if (user.isSuper() || event == null) {
      return List.of();
    }

    ProgramStage programStage = event.getProgramStage();

    if (isNull(programStage)) {
      return List.of();
    }

    Program program = programStage.getProgram();
    List<String> errors = new ArrayList<>();

    canManageWithRegistration(errors, user, programStage, program);

    OrganisationUnit ou = event.getOrganisationUnit();
    if (ou != null && !user.isInUserEffectiveSearchOrgUnitHierarchy(ou.getStoredPath())) {
      errors.add("User has no update access to organisation unit: " + ou.getUid());
    }

    if (!skipOwnershipCheck
        && !ownershipAccessManager.hasAccess(
            user, event.getEnrollment().getTrackedEntity(), program)) {
      errors.add(OWNERSHIP_ACCESS_DENIED);
    }

    errors.addAll(canWrite(user, event.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canDelete(
      @Nonnull UserDetails user, TrackerEvent event, boolean skipOwnershipCheck) {
    if (user.isSuper() || event == null) {
      return List.of();
    }

    ProgramStage programStage = event.getProgramStage();

    if (isNull(programStage)) {
      return List.of();
    }

    Program program = programStage.getProgram();

    List<String> errors = new ArrayList<>();
    if (program.isWithoutRegistration()) {
      OrganisationUnit ou = event.getOrganisationUnit();
      if (ou != null && !user.isInUserHierarchy(ou.getStoredPath())) {
        errors.add("User has no delete access to organisation unit: " + ou.getUid());
      }

      if (!aclService.canDataWrite(user, program)) {
        errors.add("User has no data write access to program: " + program.getUid());
      }
    } else {
      canCreateOrDeleteWithRegistration(
          errors, user, event, skipOwnershipCheck, programStage, program);
    }

    errors.addAll(canWrite(user, event.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canRead(@Nonnull UserDetails user, SingleEvent event) {
    if (user.isSuper() || event == null) {
      return List.of();
    }
    ProgramStage programStage = event.getProgramStage();
    if (isNull(programStage)) {
      return List.of();
    }
    Program program = programStage.getProgram();
    List<String> errors = new ArrayList<>();
    if (!aclService.canDataRead(user, program)) {
      errors.add("User has no data read access to program: " + program.getUid());
    }
    OrganisationUnit ou = event.getOrganisationUnit();
    if (!canAccess(user, program, ou)) {
      errors.add(NO_READ_ACCESS_TO_ORG_UNIT + ": " + ou.getUid());
    }
    errors.addAll(canRead(user, event.getAttributeOptionCombo()));
    return errors;
  }

  @Override
  public List<String> canRead(
      @Nonnull UserDetails user, SingleEvent event, DataElement dataElement) {

    if (user.isSuper()) {
      return List.of();
    }

    List<String> errors = new ArrayList<>();
    errors.addAll(canRead(user, event));

    if (!aclService.canRead(user, dataElement)) {
      errors.add("User has no read access to data element: " + dataElement.getUid());
    }

    return errors;
  }

  @Override
  public List<String> canWrite(@Nonnull UserDetails user, SingleEvent event) {
    if (user.isSuper() || event == null) {
      return List.of();
    }

    ProgramStage programStage = event.getProgramStage();

    if (isNull(programStage)) {
      return List.of();
    }

    Program program = programStage.getProgram();
    List<String> errors = new ArrayList<>();
    if (!aclService.canDataWrite(user, program)) {
      errors.add("User has no data write access to program: " + program.getUid());
    }

    errors.addAll(canWrite(user, event.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canRead(@Nonnull UserDetails user, Relationship relationship) {
    if (user.isSuper() || relationship == null) {
      return List.of();
    }

    RelationshipType relationshipType = relationship.getRelationshipType();
    List<String> errors = new ArrayList<>();
    if (!aclService.canDataRead(user, relationshipType)) {
      errors.add("User has no data read access to relationshipType: " + relationshipType.getUid());
    }

    RelationshipItem from = relationship.getFrom();
    RelationshipItem to = relationship.getTo();

    errors.addAll(canRead(user, from.getTrackedEntity()));
    errors.addAll(canRead(user, from.getEnrollment(), false));
    errors.addAll(canRead(user, from.getTrackerEvent(), false));
    errors.addAll(canRead(user, from.getSingleEvent()));

    errors.addAll(canRead(user, to.getTrackedEntity()));
    errors.addAll(canRead(user, to.getEnrollment(), false));
    errors.addAll(canRead(user, to.getTrackerEvent(), false));
    errors.addAll(canRead(user, to.getSingleEvent()));

    return errors;
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> canCreate(@Nonnull UserDetails user, Relationship relationship) {
    if (user.isSuper() || relationship == null) {
      return List.of();
    }

    RelationshipType relationshipType = relationship.getRelationshipType();
    List<String> errors = new ArrayList<>();

    if (!aclService.canDataWrite(user, relationshipType)) {
      errors.add("User has no data write access to relationshipType: " + relationshipType.getUid());
    }

    RelationshipItem from = relationship.getFrom();
    RelationshipItem to = relationship.getTo();
    boolean isBidirectional = relationshipType.isBidirectional();

    errors.addAll(canUpdate(user, from.getTrackedEntity()));
    errors.addAll(canUpdate(user, from.getEnrollment(), false));
    errors.addAll(canUpdate(user, from.getTrackerEvent(), false));
    errors.addAll(canWrite(user, from.getSingleEvent()));

    if (isBidirectional) {
      errors.addAll(canUpdate(user, to.getTrackedEntity()));
      errors.addAll(canUpdate(user, to.getEnrollment(), false));
      errors.addAll(canUpdate(user, to.getTrackerEvent(), false));
      errors.addAll(canWrite(user, to.getSingleEvent()));
    } else {
      errors.addAll(canRead(user, to.getTrackedEntity()));
      errors.addAll(canRead(user, to.getEnrollment(), false));
      errors.addAll(canRead(user, to.getTrackerEvent(), false));
      errors.addAll(canRead(user, to.getSingleEvent()));
    }
    return errors;
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> canDelete(UserDetails user, @Nonnull Relationship relationship) {
    RelationshipType relationshipType = relationship.getRelationshipType();
    List<String> errors = new ArrayList<>();

    if (!aclService.canDataWrite(user, relationshipType)) {
      errors.add("User has no data write access to relationshipType: " + relationshipType.getUid());
    }

    RelationshipItem from = relationship.getFrom();
    RelationshipItem to = relationship.getTo();
    boolean isBidirectional = relationshipType.isBidirectional();

    errors.addAll(canUpdate(user, from.getTrackedEntity()));
    errors.addAll(canUpdate(user, from.getEnrollment(), false));
    errors.addAll(canUpdate(user, from.getTrackerEvent(), false));
    errors.addAll(canWrite(user, from.getSingleEvent()));

    if (isBidirectional) {
      errors.addAll(canUpdate(user, to.getTrackedEntity()));
      errors.addAll(canUpdate(user, to.getEnrollment(), false));
      errors.addAll(canUpdate(user, to.getTrackerEvent(), false));
      errors.addAll(canWrite(user, to.getSingleEvent()));
    }
    return errors;
  }

  /**
   * Checks if user has access to organisation unit under defined tracker program protection level
   *
   * @param user the user to check access for
   * @param program program to check against protection level
   * @param orgUnit the org unit to be checked under user's scope and program protection
   * @return true if user has access to the org unit under the mentioned program context, otherwise
   *     return false
   */
  boolean canAccess(@Nonnull UserDetails user, Program program, OrganisationUnit orgUnit) {
    if (orgUnit == null) {
      return false;
    }

    if (user.isSuper()) {
      return true;
    }

    if (program != null && (program.isClosed() || program.isProtected())) {
      return user.isInUserHierarchy(orgUnit.getStoredPath());
    }

    return user.isInUserEffectiveSearchOrgUnitHierarchy(orgUnit.getStoredPath());
  }

  private List<String> canRead(@Nonnull UserDetails user, CategoryOptionCombo categoryOptionCombo) {

    if (user.isSuper() || categoryOptionCombo == null) {
      return List.of();
    }

    List<String> errors = new ArrayList<>();
    for (CategoryOption categoryOption : categoryOptionCombo.getCategoryOptions()) {
      if (!aclService.canDataRead(user, categoryOption)) {
        errors.add("User has no read access to category option: " + categoryOption.getUid());
      }
    }

    return errors;
  }

  private List<String> canWrite(
      @Nonnull UserDetails user, CategoryOptionCombo categoryOptionCombo) {
    if (user.isSuper() || categoryOptionCombo == null) {
      return List.of();
    }

    List<String> errors = new ArrayList<>();
    for (CategoryOption categoryOption : categoryOptionCombo.getCategoryOptions()) {
      if (!aclService.canDataWrite(user, categoryOption)) {
        errors.add("User has no write access to category option: " + categoryOption.getUid());
      }
    }

    return errors;
  }

  private void canCreateOrDeleteWithRegistration(
      List<String> errors,
      UserDetails user,
      TrackerEvent event,
      boolean skipOwnershipCheck,
      ProgramStage programStage,
      Program program) {
    canManageWithRegistration(errors, user, programStage, program);

    if (!skipOwnershipCheck
        && !ownershipAccessManager.hasAccess(
            user, event.getEnrollment().getTrackedEntity(), program)) {
      errors.add(OWNERSHIP_ACCESS_DENIED);
    }
  }

  private void canManageWithRegistration(
      List<String> errors, UserDetails user, ProgramStage programStage, Program program) {
    if (!aclService.canDataWrite(user, programStage)) {
      errors.add("User has no data write access to program stage: " + programStage.getUid());
    }

    if (!aclService.canDataRead(user, program)) {
      errors.add("User has no data read access to program: " + program.getUid());
    }

    if (!aclService.canDataRead(user, program.getTrackedEntityType())) {
      errors.add(
          "User has no data read access to tracked entity type: "
              + program.getTrackedEntityType().getUid());
    }
  }

  private boolean isNull(ProgramStage programStage) {
    return programStage == null || programStage.getProgram() == null;
  }
}
