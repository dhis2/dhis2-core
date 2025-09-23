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
import java.util.Objects;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
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
  private final ProgramService programService;
  private final OrganisationUnitService organisationUnitService;

  /**
   * Check the data read permissions and ownership of a tracked entity given the programs for which
   * the user has metadata access to.
   *
   * @return No errors if a user has access to at least one program
   */
  @Override
  public List<String> canRead(User user, TrackedEntity trackedEntity) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || trackedEntity == null) {
      return List.of();
    }

    return canRead(user, trackedEntity, programService.getAllPrograms());
  }

  private List<String> canRead(User user, TrackedEntity trackedEntity, List<Program> programs) {

    if (null == trackedEntity) {
      return List.of();
    }

    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();

    if (!aclService.canDataRead(user, trackedEntityType)) {
      return List.of(
          "User has no data read access to tracked entity type: " + trackedEntityType.getUid());
    }

    initializeTrackedEntityOrgUnitParents(trackedEntity);

    List<Program> tetPrograms =
        programs.stream()
            .filter(
                p ->
                    p.isRegistration()
                        && Objects.equals(
                            p.getTrackedEntityType().getUid(),
                            trackedEntity.getTrackedEntityType().getUid()))
            .toList();

    if (tetPrograms.isEmpty()) {
      return List.of("User has no access to any program");
    }

    if (tetPrograms.stream().anyMatch(p -> canRead(user, trackedEntity, p))) {
      return List.of();
    } else {
      return List.of(OWNERSHIP_ACCESS_DENIED);
    }
  }

  /** Check Program data read access and Tracked Entity Program Ownership */
  private boolean canRead(User user, TrackedEntity trackedEntity, Program program) {
    return aclService.canDataRead(user, program)
        && ownershipAccessManager.hasAccess(user, trackedEntity, program);
  }

  /**
   * TODO This is a temporary fix, a more permanent solution needs to be found, maybe store the org
   * unit path directly in the cache as a string or avoid using an Hibernate object in the cache
   *
   * <p>The tracked entity org unit will be used as a fallback in case no owner is found. In that
   * case, it will be stored in the cache, but it's lazy loaded, meaning org unit parents won't be
   * loaded unless accessed. This is a problem because we save the org unit object in the cache, and
   * when we retrieve it, we can't get the value of the parents, since there's no session. We need
   * the parents to build the org unit path, that later will be used to validate the ownership.
   */
  private void initializeTrackedEntityOrgUnitParents(TrackedEntity trackedEntity) {
    OrganisationUnit organisationUnit = trackedEntity.getOrganisationUnit();
    while (organisationUnit.getParent() != null) {
      organisationUnit = organisationUnit.getParent();
    }
  }

  /**
   * Check the data write permissions and ownership of a tracked entity given the programs for which
   * the user has metadata access to.
   *
   * @return No errors if a user has access to at least one program
   */
  @Override
  @Transactional(readOnly = true)
  public List<String> canWrite(User user, TrackedEntity trackedEntity) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || trackedEntity == null) {
      return List.of();
    }

    return canWrite(user, trackedEntity, programService.getAllPrograms());
  }

  /**
   * Check if the user can create the TE. For a regular user, they must have data write permissions
   * to tracked entity type metadata as well as Capture Access to the Org Unit.
   *
   * @return No errors if a user has write access to the TrackedEntityType and to the OrgUnit
   */
  @Override
  @Transactional(readOnly = true)
  public List<String> canCreate(User user, TrackedEntity trackedEntity) {
    if (user == null || user.isSuper() || trackedEntity == null) {
      return List.of();
    }
    List<String> errors = new ArrayList<>();
    if (!aclService.canDataWrite(user, trackedEntity.getTrackedEntityType())) {
      errors.add(
          "User has no data write access to tracked entity type: "
              + trackedEntity.getTrackedEntityType().getUid());
    }

    if (trackedEntity.getOrganisationUnit() != null
        && !organisationUnitService.isInUserHierarchy(user, trackedEntity.getOrganisationUnit())) {
      errors.add(
          "User has no write access to organisation unit: "
              + trackedEntity.getOrganisationUnit().getUid());
    }

    return errors;
  }

  private List<String> canWrite(User user, TrackedEntity trackedEntity, List<Program> programs) {

    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();

    if (!aclService.canDataWrite(user, trackedEntityType)) {
      return List.of(
          "User has no data write access to tracked entity type: " + trackedEntityType.getUid());
    }

    initializeTrackedEntityOrgUnitParents(trackedEntity);

    List<Program> tetPrograms =
        programs.stream()
            .filter(
                p ->
                    p.isRegistration()
                        && Objects.equals(
                            p.getTrackedEntityType().getUid(),
                            trackedEntity.getTrackedEntityType().getUid()))
            .toList();

    if (tetPrograms.isEmpty()) {
      return List.of("User has no access to any program");
    }

    if (tetPrograms.stream().anyMatch(p -> canWrite(user, trackedEntity, p))) {
      return List.of();
    } else {
      return List.of(OWNERSHIP_ACCESS_DENIED);
    }
  }

  /** Check Program data write access and Tracked Entity Program Ownership */
  private boolean canWrite(User user, TrackedEntity trackedEntity, Program program) {
    return aclService.canDataWrite(user, program)
        && ownershipAccessManager.hasAccess(user, trackedEntity, program);
  }

  @Override
  public List<String> canRead(
      User user, TrackedEntity trackedEntity, Program program, boolean skipOwnershipCheck) {
    List<String> errors = canReadProgramAndTrackedEntityType(user, trackedEntity, program);

    if (!skipOwnershipCheck && !ownershipAccessManager.hasAccess(user, trackedEntity, program)) {
      errors.add(OWNERSHIP_ACCESS_DENIED);
    }

    return errors;
  }

  @Override
  public List<String> canReadProgramAndTrackedEntityType(
      User user, TrackedEntity trackedEntity, Program program) {
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
      User user, TrackedEntity trackedEntity, Program program, boolean skipOwnershipCheck) {
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
  public List<String> canRead(User user, Enrollment enrollment, boolean skipOwnershipCheck) {
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
  public List<String> canCreate(User user, Enrollment enrollment, boolean skipOwnershipCheck) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || enrollment == null) {
      return List.of();
    }

    Program program = enrollment.getProgram();
    List<String> errors = new ArrayList<>();
    OrganisationUnit ou = enrollment.getOrganisationUnit();
    if (ou != null && !organisationUnitService.isInUserHierarchy(user, ou)) {
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
  public List<String> canUpdate(User user, Enrollment enrollment, boolean skipOwnershipCheck) {
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
      if (ou != null && !organisationUnitService.isInUserHierarchy(user, ou)) {
        errors.add("User has no write access to organisation unit: " + ou.getUid());
      }
    }

    return errors;
  }

  @Override
  public List<String> canDelete(User user, Enrollment enrollment, boolean skipOwnershipCheck) {
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
      if (ou != null && !organisationUnitService.isInUserHierarchy(user, ou)) {
        errors.add("User has no delete access to organisation unit: " + ou.getUid());
      }
    }

    return errors;
  }

  @Override
  public List<String> canRead(User user, Event event, boolean skipOwnershipCheck) {
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
  public List<String> canCreate(User user, Event event, boolean skipOwnershipCheck) {
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
          ? !organisationUnitService.isInUserSearchHierarchyCached(user, ou)
          : !organisationUnitService.isInUserHierarchyCached(user, ou)) {
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
  public List<String> canUpdate(User user, Event event, boolean skipOwnershipCheck) {
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
      if (ou != null && !organisationUnitService.isInUserSearchHierarchy(user, ou)) {
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
  public List<String> canDelete(User user, Event event, boolean skipOwnershipCheck) {
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
      if (ou != null) {
        if (!organisationUnitService.isInUserHierarchyCached(user, ou)) {
          errors.add("User has no delete access to organisation unit: " + ou.getUid());
        }
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
      User user,
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
      List<String> errors, User user, ProgramStage programStage, Program program) {
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
  public List<String> canRead(User user, Relationship relationship) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || relationship == null) {
      return List.of();
    }

    RelationshipType relationshipType = relationship.getRelationshipType();
    List<String> errors = new ArrayList<>();
    if (!aclService.canDataRead(user, relationshipType)) {
      errors.add("User has no data read access to relationshipType: " + relationshipType.getUid());
    }

    List<Program> programs = programService.getAllPrograms();

    RelationshipItem from = relationship.getFrom();
    RelationshipItem to = relationship.getTo();

    errors.addAll(canRead(user, from.getTrackedEntity(), programs));
    errors.addAll(canRead(user, from.getEnrollment(), false));
    errors.addAll(canRead(user, from.getEvent(), false));

    errors.addAll(canRead(user, to.getTrackedEntity(), programs));
    errors.addAll(canRead(user, to.getEnrollment(), false));
    errors.addAll(canRead(user, to.getEvent(), false));

    return errors;
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> canWrite(User user, Relationship relationship) {
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
    boolean isBidirectional = relationshipType.isBidirectional();

    errors.addAll(canWrite(user, from.getTrackedEntity()));
    errors.addAll(canUpdate(user, from.getEnrollment(), false));
    errors.addAll(canUpdate(user, from.getEvent(), false));

    if (isBidirectional) {
      errors.addAll(canWrite(user, to.getTrackedEntity()));
      errors.addAll(canUpdate(user, to.getEnrollment(), false));
      errors.addAll(canUpdate(user, to.getEvent(), false));
    } else {
      errors.addAll(canRead(user, to.getTrackedEntity()));
      errors.addAll(canRead(user, to.getEnrollment(), false));
      errors.addAll(canRead(user, to.getEvent(), false));
    }
    return errors;
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> canDelete(User user, @Nonnull Relationship relationship) {
    RelationshipType relationshipType = relationship.getRelationshipType();
    List<String> errors = new ArrayList<>();

    if (!aclService.canDataWrite(user, relationshipType)) {
      errors.add("User has no data write access to relationshipType: " + relationshipType.getUid());
    }

    RelationshipItem from = relationship.getFrom();
    RelationshipItem to = relationship.getTo();
    boolean isBidirectional = relationshipType.isBidirectional();

    errors.addAll(canWrite(user, from.getTrackedEntity()));
    errors.addAll(canUpdate(user, from.getEnrollment(), false));
    errors.addAll(canUpdate(user, from.getEvent(), false));

    if (isBidirectional) {
      errors.addAll(canWrite(user, to.getTrackedEntity()));
      errors.addAll(canUpdate(user, to.getEnrollment(), false));
      errors.addAll(canUpdate(user, to.getEvent(), false));
    }
    return errors;
  }

  @Override
  public List<String> canRead(
      User user, Event event, DataElement dataElement, boolean skipOwnershipCheck) {
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
      User user, Event event, DataElement dataElement, boolean skipOwnershipCheck) {
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
  public List<String> canRead(User user, CategoryOptionCombo categoryOptionCombo) {
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
  public List<String> canWrite(User user, CategoryOptionCombo categoryOptionCombo) {
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
  public boolean canAccess(User user, Program program, OrganisationUnit orgUnit) {
    if (orgUnit == null) {
      return false;
    }

    if (user == null || user.isSuper()) {
      return true;
    }

    if (program != null && (program.isClosed() || program.isProtected())) {
      return organisationUnitService.isInUserHierarchy(user, orgUnit);
    }

    return organisationUnitService.isInUserSearchHierarchy(user, orgUnit);
  }

  @Override
  public String canAccessProgramOwner(
      User user, TrackedEntity trackedEntity, Program program, boolean skipOwnershipCheck) {
    if (!skipOwnershipCheck && !ownershipAccessManager.hasAccess(user, trackedEntity, program)) {
      if (program.isProtected()) {
        return ownershipAccessManager.isOwnerInUserSearchScope(user, trackedEntity, program)
            ? OWNERSHIP_ACCESS_DENIED
            : NO_READ_ACCESS_TO_ORG_UNIT;
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
