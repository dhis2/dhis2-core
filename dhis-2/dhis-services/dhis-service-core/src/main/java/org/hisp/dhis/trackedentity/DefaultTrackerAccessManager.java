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
package org.hisp.dhis.trackedentity;

import static org.hisp.dhis.trackedentity.TrackerOwnershipManager.NO_READ_ACCESS_TO_ORG_UNIT;
import static org.hisp.dhis.trackedentity.TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED;
import static org.hisp.dhis.trackedentity.TrackerOwnershipManager.PROGRAM_ACCESS_CLOSED;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
@RequiredArgsConstructor
@Component
public class DefaultTrackerAccessManager implements TrackerAccessManager {

  private final AclService aclService;
  private final TrackerOwnershipManager ownershipAccessManager;

  @Override
  public List<String> canRead(UserDetails user, TrackedEntity trackedEntity) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || trackedEntity == null) {
      return List.of();
    }

    OrganisationUnit ou = trackedEntity.getOrganisationUnit();
    List<String> errors = new ArrayList<>();
    // ou should never be null, but needs to be checked for legacy reasons
    if (ou != null && !user.isInUserSearchHierarchy(ou.getPath())) {
      errors.add(NO_READ_ACCESS_TO_ORG_UNIT + ": " + ou.getUid());
    }

    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();

    if (!aclService.canDataRead(user, trackedEntityType)) {
      errors.add(
          "User has no data read access to tracked entity type: " + trackedEntityType.getUid());
    }

    return errors;
  }

  @Override
  public List<String> canWrite(UserDetails user, TrackedEntity trackedEntity) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || trackedEntity == null) {
      return List.of();
    }

    OrganisationUnit ou = trackedEntity.getOrganisationUnit();

    List<String> errors = new ArrayList<>();
    // ou should never be null, but needs to be checked for legacy reasons
    if (ou != null && !user.isInUserSearchHierarchy(ou.getPath())) {
      errors.add("User has no write access to organisation unit: " + ou.getUid());
    }

    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();

    if (!aclService.canDataWrite(user, trackedEntityType)) {
      errors.add("User has no data write access to tracked entity: " + trackedEntityType.getUid());
    }

    return errors;
  }

  @Override
  public List<String> canRead(
      UserDetails user, TrackedEntity trackedEntity, Program program, boolean skipOwnershipCheck) {
    List<String> errors = canReadProgramAndTrackedEntityType(user, trackedEntity, program);

    if (!skipOwnershipCheck && !ownershipAccessManager.hasAccess(user, trackedEntity, program)) {
      errors.add(OWNERSHIP_ACCESS_DENIED);
    }

    return errors;
  }

  @Override
  public List<String> canRead(
      UserDetails user,
      TrackedEntity trackedEntity,
      Program program,
      OrganisationUnit organisationUnit,
      boolean skipOwnershipCheck) {
    List<String> errors = canReadProgramAndTrackedEntityType(user, trackedEntity, program);

    if (!skipOwnershipCheck && !ownershipAccessManager.hasAccess(user, trackedEntity, program)) {
      errors.add(OWNERSHIP_ACCESS_DENIED);
    }

    return errors;
  }

  @Override
  public List<String> canReadProgramAndTrackedEntityType(
      UserDetails user, TrackedEntity trackedEntity, Program program) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || trackedEntity == null) {
      return List.of();
    }

    List<String> errors = new ArrayList<>();
    if (!aclService.canDataRead(user, program)) {
      errors.add("User has no data read access to program: " + program.getUid());
    }

    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();

    if (!aclService.canDataRead(user, trackedEntityType)) {
      errors.add(
          "User has no data read access to tracked entity type: " + trackedEntityType.getUid());
    }

    return errors;
  }

  @Override
  public List<String> canWrite(
      UserDetails user, TrackedEntity trackedEntity, Program program, boolean skipOwnershipCheck) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || trackedEntity == null) {
      return List.of();
    }

    List<String> errors = new ArrayList<>();
    if (!aclService.canDataWrite(user, program)) {
      errors.add("User has no data write access to program: " + program.getUid());
    }

    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();

    if (!aclService.canDataWrite(user, trackedEntityType)) {
      errors.add("User has no data write access to tracked entity: " + trackedEntityType.getUid());
    }

    if (!skipOwnershipCheck && !ownershipAccessManager.hasAccess(user, trackedEntity, program)) {
      errors.add(OWNERSHIP_ACCESS_DENIED);
    }

    return errors;
  }

  @Override
  public List<String> canRead(UserDetails user, Enrollment enrollment, boolean skipOwnershipCheck) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || enrollment == null) {
      return List.of();
    }

    Program program = enrollment.getProgram();
    List<String> errors = new ArrayList<>();
    if (!aclService.canDataRead(user, program)) {
      errors.add("User has no data read access to program: " + program.getUid());
    }

    if (!program.isWithoutRegistration()) {
      if (!aclService.canDataRead(user, program.getTrackedEntityType())) {
        errors.add(
            "User has no data read access to tracked entity type: "
                + program.getTrackedEntityType().getUid());
      }

      if (!skipOwnershipCheck
          && !ownershipAccessManager.hasAccess(user, enrollment.getTrackedEntity(), program)) {
        errors.add(OWNERSHIP_ACCESS_DENIED);
      }
    } else // this branch will only happen if coming from /events
    {
      OrganisationUnit ou = enrollment.getOrganisationUnit();

      if (ou != null && !canAccess(user, program, ou)) {
        errors.add(NO_READ_ACCESS_TO_ORG_UNIT + ": " + ou.getUid());
      }
    }

    return errors;
  }

  @Override
  public List<String> canCreate(
      UserDetails user, Enrollment enrollment, boolean skipOwnershipCheck) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || enrollment == null) {
      return List.of();
    }

    Program program = enrollment.getProgram();
    List<String> errors = new ArrayList<>();
    OrganisationUnit ou = enrollment.getOrganisationUnit();
    if (ou != null && !user.isInUserHierarchy(ou.getPath())) {
      errors.add("User has no create access to organisation unit: " + ou.getUid());
    }

    if (!aclService.canDataWrite(user, program)) {
      errors.add("User has no data write access to program: " + program.getUid());
    }

    if (!program.isWithoutRegistration()) {
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
    }

    return errors;
  }

  @Override
  public List<String> canUpdate(
      UserDetails user, Enrollment enrollment, boolean skipOwnershipCheck) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || enrollment == null) {
      return List.of();
    }

    Program program = enrollment.getProgram();
    List<String> errors = new ArrayList<>();
    if (!aclService.canDataWrite(user, program)) {
      errors.add("User has no data write access to program: " + program.getUid());
    }

    if (!program.isWithoutRegistration()) {
      if (!aclService.canDataRead(user, program.getTrackedEntityType())) {
        errors.add(
            "User has no data read access to tracked entity type: "
                + program.getTrackedEntityType().getUid());
      }

      if (!skipOwnershipCheck
          && !ownershipAccessManager.hasAccess(user, enrollment.getTrackedEntity(), program)) {
        errors.add(OWNERSHIP_ACCESS_DENIED);
      }

    } else {
      OrganisationUnit ou = enrollment.getOrganisationUnit();
      if (ou != null && !user.isInUserHierarchy(ou.getPath())) {
        errors.add("User has no write access to organisation unit: " + ou.getUid());
      }
    }

    return errors;
  }

  @Override
  public List<String> canDelete(
      UserDetails user, Enrollment enrollment, boolean skipOwnershipCheck) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || enrollment == null) {
      return List.of();
    }

    Program program = enrollment.getProgram();
    List<String> errors = new ArrayList<>();
    if (!aclService.canDataWrite(user, program)) {
      errors.add("User has no data write access to program: " + program.getUid());
    }

    if (!program.isWithoutRegistration()) {
      if (!aclService.canDataRead(user, program.getTrackedEntityType())) {
        errors.add(
            "User has no data read access to tracked entity type: "
                + program.getTrackedEntityType().getUid());
      }

      if (!skipOwnershipCheck
          && !ownershipAccessManager.hasAccess(user, enrollment.getTrackedEntity(), program)) {
        errors.add(OWNERSHIP_ACCESS_DENIED);
      }
    } else {
      OrganisationUnit ou = enrollment.getOrganisationUnit();
      if (ou != null && !user.isInUserHierarchy(ou.getPath())) {
        errors.add("User has no delete access to organisation unit: " + ou.getUid());
      }
    }

    return errors;
  }

  @Override
  public List<String> canRead(UserDetails user, Event event, boolean skipOwnershipCheck) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || event == null) {
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

    if (!program.isWithoutRegistration()) {
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
    } else {
      OrganisationUnit ou = event.getOrganisationUnit();

      if (!canAccess(user, program, ou)) {
        errors.add(NO_READ_ACCESS_TO_ORG_UNIT + ": " + ou.getUid());
      }
    }

    errors.addAll(canRead(user, event.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canCreate(UserDetails user, Event event, boolean skipOwnershipCheck) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || event == null) {
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
      if (event.isCreatableInSearchScope()
          ? !user.isInUserSearchHierarchy(ou.getPath())
          : !user.isInUserHierarchy(ou.getPath())) {
        errors.add("User has no create access to organisation unit: " + ou.getUid());
      }
    }

    if (program.isWithoutRegistration()) {
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
  public List<String> canUpdate(UserDetails user, Event event, boolean skipOwnershipCheck) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || event == null) {
      return List.of();
    }

    ProgramStage programStage = event.getProgramStage();

    if (isNull(programStage)) {
      return List.of();
    }

    Program program = programStage.getProgram();
    List<String> errors = new ArrayList<>();
    if (program.isWithoutRegistration()) {
      if (!aclService.canDataWrite(user, program)) {
        errors.add("User has no data write access to program: " + program.getUid());
      }
    } else {
      canManageWithRegistration(errors, user, programStage, program);

      OrganisationUnit ou = event.getOrganisationUnit();
      if (ou != null && !user.isInUserSearchHierarchy(ou.getPath())) {
        errors.add("User has no update access to organisation unit: " + ou.getUid());
      }

      if (!skipOwnershipCheck
          && !ownershipAccessManager.hasAccess(
              user, event.getEnrollment().getTrackedEntity(), program)) {
        errors.add(OWNERSHIP_ACCESS_DENIED);
      }
    }

    errors.addAll(canWrite(user, event.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canDelete(UserDetails user, Event event, boolean skipOwnershipCheck) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || event == null) {
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
      if (ou != null && !user.isInUserDataHierarchy(ou.getPath())) {
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

  private void canCreateOrDeleteWithRegistration(
      List<String> errors,
      UserDetails user,
      Event event,
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

  @Override
  public List<String> canRead(UserDetails user, Relationship relationship) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || relationship == null) {
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
    errors.addAll(canRead(user, from.getEvent(), false));

    errors.addAll(canRead(user, to.getTrackedEntity()));
    errors.addAll(canRead(user, to.getEnrollment(), false));
    errors.addAll(canRead(user, to.getEvent(), false));

    return errors;
  }

  @Override
  public List<String> canWrite(UserDetails user, Relationship relationship) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || relationship == null) {
      return List.of();
    }

    RelationshipType relationshipType = relationship.getRelationshipType();
    List<String> errors = new ArrayList<>();

    if (!aclService.canDataWrite(user, relationshipType)) {
      errors.add("User has no data write access to relationshipType: " + relationshipType.getUid());
    }

    RelationshipItem from = relationship.getFrom();
    RelationshipItem to = relationship.getTo();

    errors.addAll(canWrite(user, from.getTrackedEntity()));
    errors.addAll(canUpdate(user, from.getEnrollment(), false));
    errors.addAll(canUpdate(user, from.getEvent(), false));

    errors.addAll(canWrite(user, to.getTrackedEntity()));
    errors.addAll(canUpdate(user, to.getEnrollment(), false));
    errors.addAll(canUpdate(user, to.getEvent(), false));

    return errors;
  }

  @Override
  public List<String> canRead(
      UserDetails user, Event event, DataElement dataElement, boolean skipOwnershipCheck) {
    if (user == null || user.isSuper()) {
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
  public List<String> canWrite(
      UserDetails user, Event event, DataElement dataElement, boolean skipOwnershipCheck) {
    if (user == null || user.isSuper()) {
      return List.of();
    }

    List<String> errors = new ArrayList<>();
    errors.addAll(canUpdate(user, event, skipOwnershipCheck));

    if (!aclService.canRead(user, dataElement)) {
      errors.add("User has no read access to data element: " + dataElement.getUid());
    }

    return errors;
  }

  @Override
  public List<String> canRead(UserDetails user, CategoryOptionCombo categoryOptionCombo) {
    if (user == null || user.isSuper() || categoryOptionCombo == null) {
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

  @Override
  public List<String> canWrite(UserDetails user, CategoryOptionCombo categoryOptionCombo) {
    if (user == null || user.isSuper() || categoryOptionCombo == null) {
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

  @Override
  public boolean canAccess(UserDetails user, Program program, OrganisationUnit orgUnit) {
    if (orgUnit == null) {
      return false;
    }

    if (user == null || user.isSuper()) {
      return true;
    }

    if (program != null && (program.isClosed() || program.isProtected())) {
      return user.isInUserHierarchy(orgUnit.getPath());
    }

    return user.isInUserSearchHierarchy(orgUnit.getPath());
  }

  @Override
  public String canAccessProgramOwner(
      UserDetails user, TrackedEntity trackedEntity, Program program, boolean skipOwnershipCheck) {
    if (!skipOwnershipCheck && !ownershipAccessManager.hasAccess(user, trackedEntity, program)) {
      if (program.isProtected()) {
        return OWNERSHIP_ACCESS_DENIED;
      }

      if (program.isClosed()) {
        return PROGRAM_ACCESS_CLOSED;
      }

      return NO_READ_ACCESS_TO_ORG_UNIT;
    }

    return null;
  }

  private boolean isNull(ProgramStage programStage) {
    return programStage == null || programStage.getProgram() == null;
  }
}
