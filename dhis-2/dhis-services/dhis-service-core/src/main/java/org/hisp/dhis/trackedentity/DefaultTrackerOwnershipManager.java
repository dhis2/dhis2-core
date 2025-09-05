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
import static org.hisp.dhis.trackedentity.OwnershipCacheUtils.getOwnershipCacheKey;
import static org.hisp.dhis.trackedentity.OwnershipCacheUtils.getTempOwnershipCacheKey;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramOwnershipHistory;
import org.hisp.dhis.program.ProgramOwnershipHistoryService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTempOwner;
import org.hisp.dhis.program.ProgramTempOwnerService;
import org.hisp.dhis.program.ProgramTempOwnershipAudit;
import org.hisp.dhis.program.ProgramTempOwnershipAuditService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ameen Mohamed
 */
@Slf4j
@Service("org.hisp.dhis.trackedentity.TrackerOwnershipManager")
public class DefaultTrackerOwnershipManager implements TrackerOwnershipManager {
  private static final int TEMPORARY_OWNERSHIP_VALIDITY_IN_HOURS = 3;

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  private final ProgramTempOwnershipAuditService programTempOwnershipAuditService;

  private final ProgramTempOwnerService programTempOwnerService;

  private final ProgramOwnershipHistoryService programOwnershipHistoryService;

  private final OrganisationUnitService organisationUnitService;

  private final TrackedEntityService trackedEntityService;

  private final UserService userService;

  private final ProgramService programService;

  private final AclService aclService;

  public DefaultTrackerOwnershipManager(
      UserService userService,
      TrackedEntityProgramOwnerService trackedEntityProgramOwnerService,
      CacheProvider cacheProvider,
      ProgramTempOwnershipAuditService programTempOwnershipAuditService,
      ProgramTempOwnerService programTempOwnerService,
      ProgramOwnershipHistoryService programOwnershipHistoryService,
      TrackedEntityService trackedEntityService,
      OrganisationUnitService organisationUnitService,
      ProgramService programService,
      AclService aclService) {
    checkNotNull(userService);
    checkNotNull(trackedEntityProgramOwnerService);
    checkNotNull(cacheProvider);
    checkNotNull(programTempOwnershipAuditService);
    checkNotNull(programTempOwnerService);
    checkNotNull(programOwnershipHistoryService);
    checkNotNull(organisationUnitService);
    checkNotNull(aclService);

    this.userService = userService;
    this.trackedEntityProgramOwnerService = trackedEntityProgramOwnerService;
    this.programTempOwnershipAuditService = programTempOwnershipAuditService;
    this.programOwnershipHistoryService = programOwnershipHistoryService;
    this.programTempOwnerService = programTempOwnerService;
    this.organisationUnitService = organisationUnitService;
    this.trackedEntityService = trackedEntityService;
    this.programService = programService;
    this.ownerCache = cacheProvider.createProgramOwnerCache();
    this.tempOwnerCache = cacheProvider.createProgramTempOwnerCache();
    this.aclService = aclService;
  }

  /** Cache for storing recent ownership checks */
  private final Cache<OrganisationUnit> ownerCache;

  /** Cache for storing recent temporary ownership checks */
  private final Cache<Boolean> tempOwnerCache;

  // -------------------------------------------------------------------------
  // Implementation
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void transferOwnership(
      TrackedEntity entityInstance,
      Program program,
      OrganisationUnit orgUnit,
      boolean skipAccessValidation,
      boolean createIfNotExists)
      throws ForbiddenException {
    if (entityInstance == null || program == null || orgUnit == null) {
      return;
    }

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    if (hasAccess(currentUser, entityInstance, program) || skipAccessValidation) {
      if (!programService.hasOrgUnit(program, orgUnit)) {
        throw new ForbiddenException(
            String.format(
                "The program %s is not associated to the org unit %s",
                program.getUid(), orgUnit.getUid()));
      }

      TrackedEntityProgramOwner teProgramOwner =
          trackedEntityProgramOwnerService.getTrackedEntityProgramOwner(
              entityInstance.getId(), program.getId());

      if (teProgramOwner != null) {
        if (!teProgramOwner.getOrganisationUnit().equals(orgUnit)) {
          ProgramOwnershipHistory programOwnershipHistory =
              new ProgramOwnershipHistory(
                  program,
                  entityInstance,
                  teProgramOwner.getOrganisationUnit(),
                  teProgramOwner.getLastUpdated(),
                  teProgramOwner.getCreatedBy());
          programOwnershipHistoryService.addProgramOwnershipHistory(programOwnershipHistory);
          trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner(
              entityInstance, program, orgUnit);
        }
      } else if (createIfNotExists) {
        trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
            entityInstance, program, orgUnit);
      }

      ownerCache.invalidate(getOwnershipCacheKey(() -> entityInstance.getId(), program));
    } else {
      log.error("Unauthorized attempt to change ownership");
      throw new AccessDeniedException(
          "User does not have access to change ownership for the entity-program combination");
    }
  }

  @Override
  @Transactional
  public void assignOwnership(
      TrackedEntity entityInstance,
      Program program,
      OrganisationUnit organisationUnit,
      boolean skipAccessValidation,
      boolean overwriteIfExists) {
    if (entityInstance == null || program == null || organisationUnit == null) {
      return;
    }

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    if (hasAccess(currentUser, entityInstance, program) || skipAccessValidation) {
      TrackedEntityProgramOwner teProgramOwner =
          trackedEntityProgramOwnerService.getTrackedEntityProgramOwner(
              entityInstance.getId(), program.getId());

      if (teProgramOwner != null) {
        if (overwriteIfExists && !teProgramOwner.getOrganisationUnit().equals(organisationUnit)) {
          ProgramOwnershipHistory programOwnershipHistory =
              new ProgramOwnershipHistory(
                  program,
                  entityInstance,
                  teProgramOwner.getOrganisationUnit(),
                  teProgramOwner.getLastUpdated(),
                  teProgramOwner.getCreatedBy());
          programOwnershipHistoryService.addProgramOwnershipHistory(programOwnershipHistory);
          trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner(
              entityInstance, program, organisationUnit);
        }
      } else {
        trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
            entityInstance, program, organisationUnit);
      }

      ownerCache.invalidate(getOwnershipCacheKey(entityInstance::getId, program));
    } else {
      log.error("Unauthorized attempt to assign ownership");
      throw new AccessDeniedException(
          "User does not have access to assign ownership for the entity-program combination");
    }
  }

  @Override
  @Transactional
  public void grantTemporaryOwnership(
      TrackedEntity entityInstance, Program program, User user, String reason)
      throws ForbiddenException {

    validateGrantTemporaryOwnershipInputs(entityInstance, program, user);

    if (entityInstance.getTrackedEntityType().isAllowAuditLog()) {
      programTempOwnershipAuditService.addProgramTempOwnershipAudit(
          new ProgramTempOwnershipAudit(program, entityInstance, reason, user.getUsername()));
    }

    ProgramTempOwner programTempOwner =
        new ProgramTempOwner(
            program, entityInstance, reason, user, TEMPORARY_OWNERSHIP_VALIDITY_IN_HOURS);
    programTempOwnerService.addProgramTempOwner(programTempOwner);
    tempOwnerCache.invalidate(
        getTempOwnershipCacheKey(entityInstance.getUid(), program.getUid(), user.getUid()));
  }

  private void validateGrantTemporaryOwnershipInputs(
      TrackedEntity entityInstance, Program program, User user) throws ForbiddenException {
    if (program == null) {
      throw new ForbiddenException(
          "Temporary ownership not created. Program supplied does not exist.");
    }

    if (entityInstance == null) {
      throw new ForbiddenException(
          "Temporary ownership not created. Tracked entity supplied does not exist.");
    }

    if (user.isSuper()) {
      throw new ForbiddenException("Temporary ownership not created. Current user is a superuser.");
    }

    if (ProgramType.WITHOUT_REGISTRATION == program.getProgramType()) {
      throw new ForbiddenException(
          "Temporary ownership not created. Program supplied is not a tracker program.");
    }

    if (!program.isProtected()) {
      throw new ForbiddenException(
          String.format(
              "Temporary ownership not created. Temporary ownership can only be granted to protected programs. %s access level is %s.",
              program.getUid(), program.getAccessLevel().name()));
    }

    if (!isOwnerInUserSearchScope(user, entityInstance, program)) {
      throw new ForbiddenException(
          "Temporary ownership not created. The owner of the entity-program combination is not in the user's search scope.");
    }

    if (!aclService.canDataRead(user, program)) {
      throw new ForbiddenException(
          "Temporary ownership not created. User has no data read access to program: "
              + program.getUid());
    }

    if (!aclService.canDataRead(user, entityInstance.getTrackedEntityType())) {
      throw new ForbiddenException(
          "Temporary ownership not created. User has no data read access to tracked entity type: "
              + entityInstance.getTrackedEntityType().getUid());
    }

    if (!Objects.equals(
        program.getTrackedEntityType().getUid(), entityInstance.getTrackedEntityType().getUid())) {
      throw new ForbiddenException(
          String.format(
              "Temporary ownership not created. The tracked entity type of the program %s differs from that of the tracked entity %s.",
              program.getTrackedEntityType().getUid(),
              entityInstance.getTrackedEntityType().getUid()));
    }
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasAccess(User user, TrackedEntity entityInstance, Program program) {
    if (canSkipOwnershipCheck(user, program) || entityInstance == null) {
      return true;
    }

    OrganisationUnit ou =
        getOwner(entityInstance.getId(), program, entityInstance::getOrganisationUnit);

    return switch (program.getAccessLevel()) {
      case OPEN, AUDITED -> organisationUnitService.isInUserSearchHierarchyCached(user, ou);
      case PROTECTED ->
          organisationUnitService.isInUserHierarchyCached(user, ou)
              || hasTemporaryAccess(entityInstance, program, user);
      case CLOSED -> organisationUnitService.isInUserHierarchyCached(user, ou);
    };
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasAccess(
      User user, String entityInstance, OrganisationUnit owningOrgUnit, Program program) {
    if (canSkipOwnershipCheck(user, program) || entityInstance == null || owningOrgUnit == null) {
      return true;
    }

    return switch (program.getAccessLevel()) {
      case OPEN, AUDITED ->
          organisationUnitService.isInUserSearchHierarchyCached(user, owningOrgUnit);
      case PROTECTED ->
          organisationUnitService.isInUserHierarchyCached(user, owningOrgUnit)
              || hasTemporaryAccessWithUid(entityInstance, program, user);
      case CLOSED -> organisationUnitService.isInUserHierarchyCached(user, owningOrgUnit);
    };
  }

  @Override
  public boolean canSkipOwnershipCheck(User user, Program program) {
    return program == null || canSkipOwnershipCheck(user, program.getProgramType());
  }

  @Override
  public boolean canSkipOwnershipCheck(User user, ProgramType programType) {
    return user == null || user.isSuper() || ProgramType.WITHOUT_REGISTRATION == programType;
  }

  @Override
  public boolean isOwnerInUserSearchScope(User user, TrackedEntity trackedEntity, Program program) {
    return organisationUnitService.isInUserSearchHierarchyCached(
        user, getOwner(trackedEntity.getId(), program, trackedEntity::getOrganisationUnit));
  }

  // -------------------------------------------------------------------------
  // Private Helper Methods
  // -------------------------------------------------------------------------

  /**
   * Get the current owner of this TE-program combination. Falls back to the registered organisation
   * unit if no owner explicitly exists for the program.
   *
   * @param entityInstanceId the TE.
   * @param program The program
   * @return The owning organisation unit.
   */
  private OrganisationUnit getOwner(
      Long entityInstanceId, Program program, Supplier<OrganisationUnit> orgUnitIfMissingSupplier) {
    return ownerCache.get(
        getOwnershipCacheKey(() -> entityInstanceId, program),
        s -> {
          TrackedEntityProgramOwner trackedEntityProgramOwner =
              trackedEntityProgramOwnerService.getTrackedEntityProgramOwner(
                  entityInstanceId, program.getId());

          return Optional.ofNullable(trackedEntityProgramOwner)
              .map(
                  tepo -> {
                    return recursivelyInitializeOrgUnit(tepo.getOrganisationUnit());
                  })
              .orElseGet(orgUnitIfMissingSupplier);
        });
  }

  /**
   * This method initializes the OrganisationUnit passed on in the arguments. All the parent
   * OrganisationUnits are also recursively initialized. This is done to be able to serialize and
   * deserialize the ownership orgUnit into Redis cache.
   *
   * @param organisationUnit
   * @return
   */
  private OrganisationUnit recursivelyInitializeOrgUnit(OrganisationUnit organisationUnit) {
    // TODO: Modify the {@link
    // OrganisationUnit#isDescendant(OrganisationUnit)} and {@link
    // OrganisationUnit#isDescendant(Set)}
    // methods to use path parameter instead of recursively visiting the
    // parent OrganisationUnits.

    Hibernate.initialize(organisationUnit);
    OrganisationUnit current = organisationUnit;
    while (current.getParent() != null) {
      Hibernate.initialize(current.getParent());
      current = current.getParent();
    }
    return organisationUnit;
  }

  /**
   * Check if the user has temporary access for a specific te-program combination
   *
   * @param entityInstance The tracked entity object
   * @param program The program object
   * @param user The user object against which the check has to be performed
   * @return true if the user has temporary access, false otherwise
   */
  private boolean hasTemporaryAccess(TrackedEntity entityInstance, Program program, User user) {
    if (canSkipOwnershipCheck(user, program) || entityInstance == null) {
      return true;
    }
    return tempOwnerCache.get(
        getTempOwnershipCacheKey(entityInstance.getUid(), program.getUid(), user.getUid()),
        s -> {
          return (programTempOwnerService.getValidTempOwnerRecordCount(
                  program, entityInstance, user)
              > 0);
        });
  }

  private boolean hasTemporaryAccessWithUid(String trackedEntityUid, Program program, User user) {
    if (canSkipOwnershipCheck(user, program) || trackedEntityUid == null) {
      return true;
    }

    return tempOwnerCache.get(
        getTempOwnershipCacheKey(trackedEntityUid, program.getUid(), user.getUid()),
        s -> {
          TrackedEntity trackedEntity = trackedEntityService.getTrackedEntity(trackedEntityUid);
          if (trackedEntity == null) {
            return true;
          }
          return (programTempOwnerService.getValidTempOwnerRecordCount(program, trackedEntity, user)
              > 0);
        });
  }
}
