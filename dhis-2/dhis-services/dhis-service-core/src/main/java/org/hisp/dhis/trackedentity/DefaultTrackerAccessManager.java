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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.trackedentity.TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
@Component("org.hisp.dhis.dxf2.events.TrackerAccessManager")
public class DefaultTrackerAccessManager implements TrackerAccessManager {
  private final AclService aclService;

  private final TrackerOwnershipManager ownershipAccessManager;

  private final OrganisationUnitService organisationUnitService;

  private final ProgramService programService;

  public DefaultTrackerAccessManager(
      AclService aclService,
      TrackerOwnershipManager ownershipAccessManager,
      OrganisationUnitService organisationUnitService,
      ProgramService programService) {
    checkNotNull(aclService);
    checkNotNull(ownershipAccessManager);
    checkNotNull(organisationUnitService);

    this.aclService = aclService;
    this.ownershipAccessManager = ownershipAccessManager;
    this.organisationUnitService = organisationUnitService;
    this.programService = programService;
  }

  /**
   * Check the data read permissions and ownership of a tracked entity given the programs for which
   * the user has metadata access to.
   *
   * @return No errors if a user has access to at least one program
   */
  @Override
  public List<String> canRead(User user, TrackedEntityInstance trackedEntity) {
    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || trackedEntity == null) {
      return List.of();
    }

    return canRead(user, trackedEntity, programService.getAllPrograms());
  }

  private List<String> canRead(
      User user, TrackedEntityInstance trackedEntity, List<Program> programs) {

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
                p -> Objects.equals(p.getTrackedEntityType(), trackedEntity.getTrackedEntityType()))
            .collect(Collectors.toList());

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
  private boolean canRead(User user, TrackedEntityInstance trackedEntity, Program program) {
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
  private void initializeTrackedEntityOrgUnitParents(TrackedEntityInstance trackedEntity) {
    OrganisationUnit organisationUnit = trackedEntity.getOrganisationUnit();
    while (organisationUnit.getParent() != null) {
      organisationUnit = organisationUnit.getParent();
    }
  }

  @Override
  public List<String> canWrite(User user, TrackedEntityInstance trackedEntityInstance) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || trackedEntityInstance == null) {
      return errors;
    }

    OrganisationUnit ou = trackedEntityInstance.getOrganisationUnit();

    if (ou != null) { // ou should never be null, but needs to be checked for legacy reasons
      if (!organisationUnitService.isInUserSearchHierarchyCached(user, ou)) {
        errors.add("User has no write access to organisation unit: " + ou.getUid());
      }
    }

    TrackedEntityType trackedEntityType = trackedEntityInstance.getTrackedEntityType();

    if (!aclService.canDataWrite(user, trackedEntityType)) {
      errors.add("User has no data write access to tracked entity: " + trackedEntityType.getUid());
    }

    return errors;
  }

  @Override
  public List<String> canRead(
      User user,
      TrackedEntityInstance trackedEntityInstance,
      Program program,
      boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || trackedEntityInstance == null) {
      return errors;
    }

    if (!aclService.canDataRead(user, program)) {
      errors.add("User has no data read access to program: " + program.getUid());
    }

    TrackedEntityType trackedEntityType = trackedEntityInstance.getTrackedEntityType();

    if (!aclService.canDataRead(user, trackedEntityType)) {
      errors.add("User has no data read access to tracked entity: " + trackedEntityType.getUid());
    }

    if (!skipOwnershipCheck
        && !ownershipAccessManager.hasAccess(user, trackedEntityInstance, program)) {
      errors.add(OWNERSHIP_ACCESS_DENIED);
    }

    return errors;
  }

  @Override
  public List<String> canWrite(
      User user,
      TrackedEntityInstance trackedEntityInstance,
      Program program,
      boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || trackedEntityInstance == null) {
      return errors;
    }

    if (!aclService.canDataWrite(user, program)) {
      errors.add("User has no data write access to program: " + program.getUid());
    }

    TrackedEntityType trackedEntityType = trackedEntityInstance.getTrackedEntityType();

    if (!aclService.canDataWrite(user, trackedEntityType)) {
      errors.add("User has no data write access to tracked entity: " + trackedEntityType.getUid());
    }

    if (!skipOwnershipCheck
        && !ownershipAccessManager.hasAccess(user, trackedEntityInstance, program)) {
      errors.add(OWNERSHIP_ACCESS_DENIED);
    }

    return errors;
  }

  @Override
  public List<String> canRead(
      User user, ProgramInstance programInstance, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || programInstance == null) {
      return errors;
    }

    Program program = programInstance.getProgram();

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
          && !ownershipAccessManager.hasAccess(
              user, programInstance.getEntityInstance(), program)) {
        errors.add(OWNERSHIP_ACCESS_DENIED);
      }
    } else // this branch will only happen if coming from /events
    {
      OrganisationUnit ou = programInstance.getOrganisationUnit();

      if (ou != null && !canAccess(user, program, ou)) {
        errors.add("User has no read access to organisation unit: " + ou.getUid());
      }
    }

    return errors;
  }

  @Override
  public List<String> canCreate(
      User user, ProgramInstance programInstance, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || programInstance == null) {
      return errors;
    }

    Program program = programInstance.getProgram();

    OrganisationUnit ou = programInstance.getOrganisationUnit();
    if (ou != null) {
      if (!organisationUnitService.isInUserHierarchyCached(user, ou)) {
        errors.add("User has no create access to organisation unit: " + ou.getUid());
      }
    }

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
          && !ownershipAccessManager.hasAccess(
              user, programInstance.getEntityInstance(), program)) {
        errors.add(OWNERSHIP_ACCESS_DENIED);
      }
    }

    return errors;
  }

  @Override
  public List<String> canUpdate(
      User user, ProgramInstance programInstance, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || programInstance == null) {
      return errors;
    }

    Program program = programInstance.getProgram();

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
          && !ownershipAccessManager.hasAccess(
              user, programInstance.getEntityInstance(), program)) {
        errors.add(OWNERSHIP_ACCESS_DENIED);
      }

    } else {
      OrganisationUnit ou = programInstance.getOrganisationUnit();
      if (ou != null) {
        if (!organisationUnitService.isInUserHierarchyCached(user, ou)) {
          errors.add("User has no write access to organisation unit: " + ou.getUid());
        }
      }
    }

    return errors;
  }

  @Override
  public List<String> canDelete(
      User user, ProgramInstance programInstance, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || programInstance == null) {
      return errors;
    }

    Program program = programInstance.getProgram();

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
          && !ownershipAccessManager.hasAccess(
              user, programInstance.getEntityInstance(), program)) {
        errors.add(OWNERSHIP_ACCESS_DENIED);
      }
    } else {
      OrganisationUnit ou = programInstance.getOrganisationUnit();
      if (ou != null) {
        if (!organisationUnitService.isInUserHierarchyCached(user, ou)) {
          errors.add("User has no delete access to organisation unit: " + ou.getUid());
        }
      }
    }

    return errors;
  }

  @Override
  public List<String> canRead(
      User user, ProgramStageInstance programStageInstance, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || programStageInstance == null) {
      return errors;
    }

    ProgramStage programStage = programStageInstance.getProgramStage();

    if (isNull(programStage)) {
      return errors;
    }

    Program program = programStage.getProgram();

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
              user, programStageInstance.getProgramInstance().getEntityInstance(), program)) {
        errors.add(OWNERSHIP_ACCESS_DENIED);
      }
    } else {
      OrganisationUnit ou = programStageInstance.getOrganisationUnit();

      if (!canAccess(user, program, ou)) {
        errors.add("User has no read access to organisation unit: " + ou.getUid());
      }
    }

    errors.addAll(canRead(user, programStageInstance.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canCreate(
      User user, ProgramStageInstance programStageInstance, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || programStageInstance == null) {
      return errors;
    }

    ProgramStage programStage = programStageInstance.getProgramStage();

    if (isNull(programStage)) {
      return errors;
    }

    Program program = programStage.getProgram();

    OrganisationUnit ou = programStageInstance.getOrganisationUnit();
    if (ou != null) {
      if (programStageInstance.isCreatableInSearchScope()
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

      if (!skipOwnershipCheck
          && !ownershipAccessManager.hasAccess(
              user, programStageInstance.getProgramInstance().getEntityInstance(), program)) {
        errors.add(OWNERSHIP_ACCESS_DENIED);
      }
    }

    errors.addAll(canWrite(user, programStageInstance.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canUpdate(
      User user, ProgramStageInstance programStageInstance, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || programStageInstance == null) {
      return errors;
    }

    ProgramStage programStage = programStageInstance.getProgramStage();

    if (isNull(programStage)) {
      return errors;
    }

    Program program = programStage.getProgram();

    if (program.isWithoutRegistration()) {
      if (!aclService.canDataWrite(user, program)) {
        errors.add("User has no data write access to program: " + program.getUid());
      }
    } else {
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

      OrganisationUnit ou = programStageInstance.getOrganisationUnit();
      if (ou != null) {
        if (!organisationUnitService.isInUserSearchHierarchy(user, ou)) {
          errors.add("User has no update access to organisation unit: " + ou.getUid());
        }
      }

      if (!skipOwnershipCheck
          && !ownershipAccessManager.hasAccess(
              user, programStageInstance.getProgramInstance().getEntityInstance(), program)) {
        errors.add(OWNERSHIP_ACCESS_DENIED);
      }
    }

    errors.addAll(canWrite(user, programStageInstance.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canDelete(
      User user, ProgramStageInstance programStageInstance, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || programStageInstance == null) {
      return errors;
    }

    ProgramStage programStage = programStageInstance.getProgramStage();

    if (isNull(programStage)) {
      return errors;
    }

    Program program = programStage.getProgram();

    if (program.isWithoutRegistration()) {
      OrganisationUnit ou = programStageInstance.getOrganisationUnit();
      if (ou != null) {
        if (!organisationUnitService.isInUserHierarchyCached(user, ou)) {
          errors.add("User has no delete access to organisation unit: " + ou.getUid());
        }
      }

      if (!aclService.canDataWrite(user, program)) {
        errors.add("User has no data write access to program: " + program.getUid());
      }
    } else {
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

      if (!skipOwnershipCheck
          && !ownershipAccessManager.hasAccess(
              user, programStageInstance.getProgramInstance().getEntityInstance(), program)) {
        errors.add(OWNERSHIP_ACCESS_DENIED);
      }
    }

    errors.addAll(canWrite(user, programStageInstance.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canRead(User user, Relationship relationship) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || relationship == null) {
      return errors;
    }

    RelationshipType relationshipType = relationship.getRelationshipType();
    if (!aclService.canDataRead(user, relationshipType)) {
      errors.add("User has no data read access to relationshipType: " + relationshipType.getUid());
    }

    List<Program> programs = programService.getAllPrograms();

    RelationshipItem from = relationship.getFrom();
    RelationshipItem to = relationship.getTo();

    errors.addAll(canRead(user, from.getTrackedEntityInstance(), programs));
    errors.addAll(canRead(user, from.getProgramInstance(), false));
    errors.addAll(canRead(user, from.getProgramStageInstance(), false));

    errors.addAll(canRead(user, to.getTrackedEntityInstance(), programs));
    errors.addAll(canRead(user, to.getProgramInstance(), false));
    errors.addAll(canRead(user, to.getProgramStageInstance(), false));

    return errors;
  }

  @Override
  public List<String> canWrite(User user, Relationship relationship) {
    List<String> errors = new ArrayList<>();
    RelationshipType relationshipType;
    RelationshipItem from;
    RelationshipItem to;

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || relationship == null) {
      return errors;
    }

    relationshipType = relationship.getRelationshipType();

    if (!aclService.canDataWrite(user, relationshipType)) {
      errors.add("User has no data write access to relationshipType: " + relationshipType.getUid());
    }

    from = relationship.getFrom();
    to = relationship.getTo();

    errors.addAll(canWrite(user, from.getTrackedEntityInstance()));
    errors.addAll(canUpdate(user, from.getProgramInstance(), false));
    errors.addAll(canUpdate(user, from.getProgramStageInstance(), false));

    errors.addAll(canWrite(user, to.getTrackedEntityInstance()));
    errors.addAll(canUpdate(user, to.getProgramInstance(), false));
    errors.addAll(canUpdate(user, to.getProgramStageInstance(), false));

    return errors;
  }

  @Override
  public List<String> canRead(
      User user,
      ProgramStageInstance programStageInstance,
      DataElement dataElement,
      boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    if (user == null || user.isSuper()) {
      return errors;
    }

    errors.addAll(canRead(user, programStageInstance, skipOwnershipCheck));

    if (!aclService.canRead(user, dataElement)) {
      errors.add("User has no read access to data element: " + dataElement.getUid());
    }

    return errors;
  }

  @Override
  public List<String> canWrite(
      User user,
      ProgramStageInstance programStageInstance,
      DataElement dataElement,
      boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    if (user == null || user.isSuper()) {
      return errors;
    }

    errors.addAll(canUpdate(user, programStageInstance, skipOwnershipCheck));

    if (!aclService.canRead(user, dataElement)) {
      errors.add("User has no read access to data element: " + dataElement.getUid());
    }

    return errors;
  }

  @Override
  public List<String> canRead(User user, CategoryOptionCombo categoryOptionCombo) {
    List<String> errors = new ArrayList<>();

    if (user == null || user.isSuper() || categoryOptionCombo == null) {
      return errors;
    }

    for (CategoryOption categoryOption : categoryOptionCombo.getCategoryOptions()) {
      if (!aclService.canDataRead(user, categoryOption)) {
        errors.add("User has no read access to category option: " + categoryOption.getUid());
      }
    }

    return errors;
  }

  @Override
  public List<String> canWrite(User user, CategoryOptionCombo categoryOptionCombo) {
    List<String> errors = new ArrayList<>();

    if (user == null || user.isSuper() || categoryOptionCombo == null) {
      return errors;
    }

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

  private boolean isNull(ProgramStage programStage) {
    return programStage == null || programStage.getProgram() == null;
  }
}
