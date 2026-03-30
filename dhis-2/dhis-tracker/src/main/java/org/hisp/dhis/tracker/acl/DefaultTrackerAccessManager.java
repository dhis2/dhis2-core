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
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.imports.validation.ErrorMessage;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.Relationship;
import org.hisp.dhis.tracker.model.RelationshipItem;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.hisp.dhis.user.UserDetails;
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
  public List<String> canRead(@Nonnull UserDetails user, @Nonnull TrackedEntity trackedEntity) {
    if (user.isSuper()) {
      return List.of();
    }

    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();
    if (!aclService.canDataRead(user, trackedEntityType)) {
      return List.of(
          "User has no data read access to tracked entity type: " + trackedEntityType.getUid());
    }

    List<Program> tetPrograms =
        trackerProgramService.getTrackerProgramsWithDataReadAccess(trackedEntityType);
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
  public List<ErrorMessage> canCreate(
      @Nonnull UserDetails user, @Nonnull TrackedEntity trackedEntity) {
    List<ErrorMessage> errors = new ArrayList<>();
    if (user.isSuper()) {
      return List.of();
    }

    if (!user.isInUserHierarchy(trackedEntity.getOrganisationUnit().getStoredPath())) {
      errors.add(
          new ErrorMessage(
              ValidationCode.E1000, user.getUid(), List.of(trackedEntity.getOrganisationUnit())));
    }

    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();
    if (!aclService.canDataWrite(user, trackedEntityType)) {
      errors.add(
          new ErrorMessage(
              ValidationCode.E1001, user.getUid(), List.of(trackedEntityType.getUid())));
    }

    return errors;
  }

  @Override
  public List<ErrorMessage> canUpdate(UserDetails user, @Nonnull TrackedEntity trackedEntity) {
    List<ErrorMessage> errors = new ArrayList<>();
    if (user.isSuper()) {
      return List.of();
    }

    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();

    if (!aclService.canDataWrite(user, trackedEntityType)) {
      errors.add(
          new ErrorMessage(
              ValidationCode.E1001, user.getUid(), List.of(trackedEntityType.getUid())));
    }

    List<Program> tetPrograms =
        trackerProgramService.getTrackerProgramsWithDataWriteAccess(trackedEntityType);

    if (tetPrograms.isEmpty()) {
      errors.add(
          new ErrorMessage(
              ValidationCode.E1323, user.getUid(), List.of(trackedEntityType.getUid())));
    } else if (tetPrograms.stream()
        .noneMatch(p -> ownershipAccessManager.hasAccess(user, trackedEntity, p))) {
      errors.add(
          new ErrorMessage(ValidationCode.E1324, user.getUid(), List.of(trackedEntity.getUid())));
    }

    return errors;
  }

  @Override
  public List<ErrorMessage> canDelete(UserDetails user, @Nonnull TrackedEntity trackedEntity) {
    if (user.isSuper()) {
      return List.of();
    }

    List<ErrorMessage> errors = new ArrayList<>();
    if (!user.isInUserHierarchy(trackedEntity.getOrganisationUnit().getStoredPath())) {
      errors.add(
          new ErrorMessage(
              ValidationCode.E1000, user.getUid(), List.of(trackedEntity.getOrganisationUnit())));
    }

    errors.addAll(canUpdate(user, trackedEntity));

    return errors;
  }

  @Override
  public List<String> canRead(@Nonnull UserDetails user, @Nonnull Enrollment enrollment) {
    if (user.isSuper()) {
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

    if (!ownershipAccessManager.hasAccess(user, enrollment.getTrackedEntity(), program)) {
      errors.add(OWNERSHIP_ACCESS_DENIED);
    }

    return errors;
  }

  @Override
  public List<ErrorMessage> canCreate(@Nonnull UserDetails user, @Nonnull Enrollment enrollment) {
    if (user.isSuper()) {
      return List.of();
    }

    List<ErrorMessage> errors = new ArrayList<>(canUpdate(user, enrollment));

    OrganisationUnit enrollmentOrgUnit = enrollment.getOrganisationUnit();
    if (enrollmentOrgUnit != null && !user.isInUserHierarchy(enrollmentOrgUnit.getStoredPath())) {
      errors.add(
          new ErrorMessage(
              ValidationCode.E1000,
              user.getUid(),
              List.of(user.getUid(), enrollmentOrgUnit.getUid())));
    }

    return errors;
  }

  @Override
  public List<ErrorMessage> canUpdate(@Nonnull UserDetails user, @Nonnull Enrollment enrollment) {
    if (user.isSuper()) {
      return List.of();
    }

    Program program = enrollment.getProgram();
    List<ErrorMessage> errors = new ArrayList<>();
    if (!aclService.canDataWrite(user, program)) {
      errors.add(
          new ErrorMessage(
              ValidationCode.E1091, user.getUid(), List.of(user.getUid(), program.getUid())));
    }

    if (!aclService.canDataRead(user, program.getTrackedEntityType())) {
      errors.add(
          new ErrorMessage(
              ValidationCode.E1104,
              user.getUid(),
              List.of(user.getUid(), program.getUid(), program.getTrackedEntityType().getUid())));
    }

    if (!ownershipAccessManager.hasAccess(user, enrollment.getTrackedEntity(), program)) {
      errors.add(
          new ErrorMessage(
              ValidationCode.E1102,
              user.getUid(),
              List.of(user.getUid(), enrollment.getTrackedEntity().getUid(), program.getUid())));
    }

    errors.addAll(canWriteCategoryOptionCombo(user, enrollment.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<ErrorMessage> canDelete(@Nonnull UserDetails user, @Nonnull Enrollment enrollment) {
    if (user.isSuper()) {
      return List.of();
    }

    return new ArrayList<>(canCreate(user, enrollment));
  }

  @Override
  public List<String> canRead(@Nonnull UserDetails user, TrackerEvent event) {
    if (user.isSuper() || event == null) {
      return List.of();
    }

    ProgramStage programStage = event.getProgramStage();

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

    if (!ownershipAccessManager.hasAccess(
        user, event.getEnrollment().getTrackedEntity(), program)) {
      errors.add(OWNERSHIP_ACCESS_DENIED);
    }

    errors.addAll(canRead(user, event.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canRead(
      @Nonnull UserDetails user, TrackerEvent event, DataElement dataElement) {
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
  public List<String> canCreate(@Nonnull UserDetails user, TrackerEvent event) {
    if (user.isSuper() || event == null) {
      return List.of();
    }

    ProgramStage programStage = event.getProgramStage();

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

    canCreateOrDeleteWithRegistration(errors, user, event, programStage, program);

    errors.addAll(canWrite(user, event.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canUpdate(@Nonnull UserDetails user, TrackerEvent event) {
    if (user.isSuper() || event == null) {
      return List.of();
    }

    ProgramStage programStage = event.getProgramStage();

    Program program = programStage.getProgram();
    List<String> errors = new ArrayList<>();

    canManageWithRegistration(errors, user, programStage, program);

    OrganisationUnit ou = event.getOrganisationUnit();
    if (ou != null && !user.isInUserEffectiveSearchOrgUnitHierarchy(ou.getStoredPath())) {
      errors.add("User has no update access to organisation unit: " + ou.getUid());
    }

    if (!ownershipAccessManager.hasAccess(
        user, event.getEnrollment().getTrackedEntity(), program)) {
      errors.add(OWNERSHIP_ACCESS_DENIED);
    }

    errors.addAll(canWrite(user, event.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canDelete(@Nonnull UserDetails user, TrackerEvent event) {

    if (user.isSuper() || event == null) {
      return List.of();
    }

    ProgramStage programStage = event.getProgramStage();

    Program program = programStage.getProgram();

    List<String> errors = new ArrayList<>();

    canCreateOrDeleteWithRegistration(errors, user, event, programStage, program);

    errors.addAll(canWrite(user, event.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canRead(@Nonnull UserDetails user, SingleEvent event) {
    if (user.isSuper() || event == null) {
      return List.of();
    }
    ProgramStage programStage = event.getProgramStage();

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
  public List<String> canCreate(@Nonnull UserDetails user, SingleEvent event) {
    if (user.isSuper() || event == null) {
      return List.of();
    }

    ProgramStage programStage = event.getProgramStage();

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

    errors.addAll(canRead(user, from));
    errors.addAll(canRead(user, to));

    return errors;
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> canCreate(UserDetails user, Relationship relationship) {
    if (user.isSuper() || relationship == null) return List.of();
    List<String> errors = new ArrayList<>(canWriteRelationship(user, relationship));
    if (!relationship.getRelationshipType().isBidirectional()) {
      errors.addAll(canRead(user, relationship.getTo()));
    }
    return errors;
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> canDelete(UserDetails user, @Nonnull Relationship relationship) {
    if (user.isSuper()) return List.of();
    return canWriteRelationship(user, relationship);
  }

  private List<String> canWriteRelationship(UserDetails user, Relationship relationship) {
    RelationshipType type = relationship.getRelationshipType();
    List<String> errors = new ArrayList<>();
    if (!aclService.canDataWrite(user, type)) {
      errors.add("User has no data write access to relationshipType: " + type.getUid());
    }
    errors.addAll(canWrite(user, relationship.getFrom()));
    if (type.isBidirectional()) {
      errors.addAll(canWrite(user, relationship.getTo()));
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

  private List<String> canRead(@Nonnull UserDetails user, RelationshipItem item) {
    if (item.getTrackedEntity() != null) return canRead(user, item.getTrackedEntity());
    if (item.getEnrollment() != null) return canRead(user, item.getEnrollment());
    if (item.getTrackerEvent() != null) return canRead(user, item.getTrackerEvent());
    if (item.getSingleEvent() != null) return canRead(user, item.getSingleEvent());
    return List.of();
  }

  private List<String> canWrite(@Nonnull UserDetails user, RelationshipItem item) {
    if (item.getTrackedEntity() != null)
      return canUpdate(user, item.getTrackedEntity()).stream()
          .map(eo -> eo.validationCode().getMessage())
          .toList();
    if (item.getEnrollment() != null)
      return canUpdate(user, item.getEnrollment()).stream()
          .map(eo -> eo.validationCode().getMessage())
          .toList();
    if (item.getTrackerEvent() != null) return canUpdate(user, item.getTrackerEvent());
    if (item.getSingleEvent() != null) return canCreate(user, item.getSingleEvent());
    return List.of();
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

  private List<ErrorMessage> canWriteCategoryOptionCombo(
      @Nonnull UserDetails user, CategoryOptionCombo categoryOptionCombo) {
    if (user.isSuper() || categoryOptionCombo == null) {
      return List.of();
    }

    List<ErrorMessage> errors = new ArrayList<>();
    for (CategoryOption categoryOption : categoryOptionCombo.getCategoryOptions()) {
      if (!aclService.canDataWrite(user, categoryOption)) {
        errors.add(
            new ErrorMessage(
                ValidationCode.E1099,
                user.getUid(),
                List.of(user.getUid(), categoryOption.getUid())));
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
      ProgramStage programStage,
      Program program) {
    canManageWithRegistration(errors, user, programStage, program);

    if (!ownershipAccessManager.hasAccess(
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
}
