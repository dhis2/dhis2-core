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

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
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
@RequiredArgsConstructor
@Component
public class DefaultTrackerAccessManager implements TrackerAccessManager {
  private final AclService aclService;

  private final TrackerOwnershipManager ownershipAccessManager;

  private final OrganisationUnitService organisationUnitService;

  @Override
  public List<String> canRead(User user, TrackedEntity trackedEntity) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || trackedEntity == null) {
      return errors;
    }

    OrganisationUnit ou = trackedEntity.getOrganisationUnit();

    if (ou != null) { // ou should never be null, but needs to be checked for legacy reasons
      if (!organisationUnitService.isInUserSearchHierarchyCached(user, ou)) {
        errors.add("User has no read access to organisation unit: " + ou.getUid());
      }
    }

    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();

    if (!aclService.canDataRead(user, trackedEntityType)) {
      errors.add(
          "User has no data read access to tracked entity type: " + trackedEntityType.getUid());
    }

    return errors;
  }

  @Override
  public List<String> canWrite(User user, TrackedEntity trackedEntity) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || trackedEntity == null) {
      return errors;
    }

    OrganisationUnit ou = trackedEntity.getOrganisationUnit();

    if (ou != null) { // ou should never be null, but needs to be checked for legacy reasons
      if (!organisationUnitService.isInUserSearchHierarchyCached(user, ou)) {
        errors.add("User has no write access to organisation unit: " + ou.getUid());
      }
    }

    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();

    if (!aclService.canDataWrite(user, trackedEntityType)) {
      errors.add("User has no data write access to tracked entity: " + trackedEntityType.getUid());
    }

    return errors;
  }

  @Override
  public List<String> canRead(
      User user, TrackedEntity trackedEntity, Program program, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || trackedEntity == null) {
      return errors;
    }

    if (!aclService.canDataRead(user, program)) {
      errors.add("User has no data read access to program: " + program.getUid());
    }

    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();

    if (!aclService.canDataRead(user, trackedEntityType)) {
      errors.add("User has no data read access to tracked entity: " + trackedEntityType.getUid());
    }

    if (!skipOwnershipCheck && !ownershipAccessManager.hasAccess(user, trackedEntity, program)) {
      errors.add(TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED);
    }

    return errors;
  }

  @Override
  public List<String> canWrite(
      User user, TrackedEntity trackedEntity, Program program, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || trackedEntity == null) {
      return errors;
    }

    if (!aclService.canDataWrite(user, program)) {
      errors.add("User has no data write access to program: " + program.getUid());
    }

    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();

    if (!aclService.canDataWrite(user, trackedEntityType)) {
      errors.add("User has no data write access to tracked entity: " + trackedEntityType.getUid());
    }

    if (!skipOwnershipCheck && !ownershipAccessManager.hasAccess(user, trackedEntity, program)) {
      errors.add(TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED);
    }

    return errors;
  }

  @Override
  public List<String> canRead(User user, Enrollment enrollment, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || enrollment == null) {
      return errors;
    }

    Program program = enrollment.getProgram();

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
        errors.add(TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED);
      }
    } else // this branch will only happen if coming from /events
    {
      OrganisationUnit ou = enrollment.getOrganisationUnit();

      if (ou != null && !canAccess(user, program, ou)) {
        errors.add("User has no read access to organisation unit: " + ou.getUid());
      }
    }

    return errors;
  }

  @Override
  public List<String> canCreate(User user, Enrollment enrollment, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || enrollment == null) {
      return errors;
    }

    Program program = enrollment.getProgram();

    OrganisationUnit ou = enrollment.getOrganisationUnit();
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
          && !ownershipAccessManager.hasAccess(user, enrollment.getTrackedEntity(), program)) {
        errors.add(TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED);
      }
    }

    return errors;
  }

  @Override
  public List<String> canUpdate(User user, Enrollment enrollment, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || enrollment == null) {
      return errors;
    }

    Program program = enrollment.getProgram();

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
        errors.add(TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED);
      }

    } else {
      OrganisationUnit ou = enrollment.getOrganisationUnit();
      if (ou != null) {
        if (!organisationUnitService.isInUserHierarchyCached(user, ou)) {
          errors.add("User has no write access to organisation unit: " + ou.getUid());
        }
      }
    }

    return errors;
  }

  @Override
  public List<String> canDelete(User user, Enrollment enrollment, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || enrollment == null) {
      return errors;
    }

    Program program = enrollment.getProgram();

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
        errors.add(TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED);
      }
    } else {
      OrganisationUnit ou = enrollment.getOrganisationUnit();
      if (ou != null) {
        if (!organisationUnitService.isInUserHierarchyCached(user, ou)) {
          errors.add("User has no delete access to organisation unit: " + ou.getUid());
        }
      }
    }

    return errors;
  }

  @Override
  public List<String> canRead(User user, Event event, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || event == null) {
      return errors;
    }

    ProgramStage programStage = event.getProgramStage();

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
              user, event.getEnrollment().getTrackedEntity(), program)) {
        errors.add(TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED);
      }
    } else {
      OrganisationUnit ou = event.getOrganisationUnit();

      if (!canAccess(user, program, ou)) {
        errors.add("User has no read access to organisation unit: " + ou.getUid());
      }
    }

    errors.addAll(canRead(user, event.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canCreate(User user, Event event, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || event == null) {
      return errors;
    }

    ProgramStage programStage = event.getProgramStage();

    if (isNull(programStage)) {
      return errors;
    }

    Program program = programStage.getProgram();

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
              user, event.getEnrollment().getTrackedEntity(), program)) {
        errors.add(TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED);
      }
    }

    errors.addAll(canWrite(user, event.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canUpdate(User user, Event event, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || event == null) {
      return errors;
    }

    ProgramStage programStage = event.getProgramStage();

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

      OrganisationUnit ou = event.getOrganisationUnit();
      if (ou != null) {
        if (!organisationUnitService.isInUserSearchHierarchy(user, ou)) {
          errors.add("User has no update access to organisation unit: " + ou.getUid());
        }
      }

      if (!skipOwnershipCheck
          && !ownershipAccessManager.hasAccess(
              user, event.getEnrollment().getTrackedEntity(), program)) {
        errors.add(TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED);
      }
    }

    errors.addAll(canWrite(user, event.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canDelete(User user, Event event, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || event == null) {
      return errors;
    }

    ProgramStage programStage = event.getProgramStage();

    if (isNull(programStage)) {
      return errors;
    }

    Program program = programStage.getProgram();

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
              user, event.getEnrollment().getTrackedEntity(), program)) {
        errors.add(TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED);
      }
    }

    errors.addAll(canWrite(user, event.getAttributeOptionCombo()));

    return errors;
  }

  @Override
  public List<String> canRead(User user, Relationship relationship) {
    List<String> errors = new ArrayList<>();
    RelationshipType relationshipType;
    RelationshipItem from;
    RelationshipItem to;

    // always allow if user == null (internal process) or user is superuser
    if (user == null || user.isSuper() || relationship == null) {
      return errors;
    }

    relationshipType = relationship.getRelationshipType();

    if (!aclService.canDataRead(user, relationshipType)) {
      errors.add("User has no data read access to relationshipType: " + relationshipType.getUid());
    }

    from = relationship.getFrom();
    to = relationship.getTo();

    errors.addAll(canRead(user, from.getTrackedEntity()));
    errors.addAll(canRead(user, from.getEnrollment(), false));
    errors.addAll(canRead(user, from.getEvent(), false));

    errors.addAll(canRead(user, to.getTrackedEntity()));
    errors.addAll(canRead(user, to.getEnrollment(), false));
    errors.addAll(canRead(user, to.getEvent(), false));

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
      User user, Event event, DataElement dataElement, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    if (user == null || user.isSuper()) {
      return errors;
    }

    errors.addAll(canRead(user, event, skipOwnershipCheck));

    if (!aclService.canRead(user, dataElement)) {
      errors.add("User has no read access to data element: " + dataElement.getUid());
    }

    return errors;
  }

  @Override
  public List<String> canWrite(
      User user, Event event, DataElement dataElement, boolean skipOwnershipCheck) {
    List<String> errors = new ArrayList<>();

    if (user == null || user.isSuper()) {
      return errors;
    }

    errors.addAll(canUpdate(user, event, skipOwnershipCheck));

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
