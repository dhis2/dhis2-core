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

import static org.hisp.dhis.tracker.acl.OwnershipCacheUtils.getOwnershipCacheKey;
import static org.hisp.dhis.tracker.acl.OwnershipCacheUtils.getTempOwnershipCacheKey;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
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
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ameen Mohamed
 */
@Slf4j
@Service("org.hisp.dhis.trackedentity.TrackerOwnershipManager")
public class DefaultTrackerOwnershipManager implements TrackerOwnershipManager {
  private static final int TEMPORARY_OWNERSHIP_VALIDITY_IN_HOURS = 3;

  private final TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;
  private final ProgramTempOwnershipAuditService programTempOwnershipAuditService;
  private final ProgramTempOwnerService programTempOwnerService;
  private final ProgramOwnershipHistoryService programOwnershipHistoryService;
  private final UserService userService;
  private final ProgramService programService;
  private final IdentifiableObjectManager manager;
  private final AclService aclService;

  public DefaultTrackerOwnershipManager(
      UserService userService,
      TrackedEntityProgramOwnerService trackedEntityProgramOwnerService,
      CacheProvider cacheProvider,
      ProgramTempOwnershipAuditService programTempOwnershipAuditService,
      ProgramTempOwnerService programTempOwnerService,
      ProgramOwnershipHistoryService programOwnershipHistoryService,
      ProgramService programService,
      IdentifiableObjectManager manager,
      AclService aclService) {

    this.userService = userService;
    this.trackedEntityProgramOwnerService = trackedEntityProgramOwnerService;
    this.programTempOwnershipAuditService = programTempOwnershipAuditService;
    this.programOwnershipHistoryService = programOwnershipHistoryService;
    this.programTempOwnerService = programTempOwnerService;
    this.programService = programService;
    this.manager = manager;
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
      TrackedEntity trackedEntity, Program program, OrganisationUnit orgUnit)
      throws ForbiddenException {
    if (trackedEntity == null || program == null || orgUnit == null) {
      return;
    }

    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();

    if (hasAccess(currentUser, trackedEntity, program)) {
      if (!programService.hasOrgUnit(program, orgUnit)) {
        throw new ForbiddenException(
            String.format(
                "The program %s is not associated to the org unit %s",
                program.getUid(), orgUnit.getUid()));
      }

      TrackedEntityProgramOwner teProgramOwner =
          trackedEntityProgramOwnerService.getTrackedEntityProgramOwner(trackedEntity, program);

      // TODO(tracker) jdbc-hibernate: check the impact on performance
      TrackedEntity hibernateTrackedEntity =
          manager.get(TrackedEntity.class, trackedEntity.getUid());
      if (teProgramOwner != null && !teProgramOwner.getOrganisationUnit().equals(orgUnit)) {
        ProgramOwnershipHistory programOwnershipHistory =
            new ProgramOwnershipHistory(
                program,
                hibernateTrackedEntity,
                teProgramOwner.getOrganisationUnit(),
                teProgramOwner.getLastUpdated(),
                teProgramOwner.getCreatedBy());
        programOwnershipHistoryService.addProgramOwnershipHistory(programOwnershipHistory);
        trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner(
            hibernateTrackedEntity, program, orgUnit);
      }
    } else {
      log.error("Unauthorized attempt to change ownership");
      throw new ForbiddenException(
          "User does not have access to change ownership for the entity-program combination");
    }
  }

  @Override
  @Transactional
  public void grantTemporaryOwnership(
      @Nonnull TrackedEntity trackedEntity, Program program, UserDetails user, String reason)
      throws ForbiddenException {
    validateGrantTemporaryOwnershipInputs(trackedEntity, program, user);

    if (trackedEntity.getTrackedEntityType().isAllowAuditLog()) {
      programTempOwnershipAuditService.addProgramTempOwnershipAudit(
          new ProgramTempOwnershipAudit(program, trackedEntity, reason, user.getUsername()));
    }

    ProgramTempOwner programTempOwner =
        new ProgramTempOwner(
            program,
            trackedEntity,
            reason,
            userService.getUser(user.getUid()),
            TEMPORARY_OWNERSHIP_VALIDITY_IN_HOURS);
    programTempOwnerService.addProgramTempOwner(programTempOwner);
    tempOwnerCache.invalidate(
        getTempOwnershipCacheKey(trackedEntity.getUid(), program.getUid(), user.getUid()));
  }

  private void validateGrantTemporaryOwnershipInputs(
      TrackedEntity trackedEntity, Program program, UserDetails user) throws ForbiddenException {
    if (program == null) {
      throw new ForbiddenException(
          "Temporary ownership not created. Program supplied does not exist.");
    }

    if (trackedEntity == null) {
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

    if (!isOwnerInUserSearchScope(user, trackedEntity, program)) {
      throw new ForbiddenException(
          "Temporary ownership not created. The owner of the entity-program combination is not in the user's search scope.");
    }

    if (!aclService.canDataRead(user, program)) {
      throw new ForbiddenException(
          "Temporary ownership not created. User has no data read access to program: "
              + program.getUid());
    }

    if (!aclService.canDataRead(user, trackedEntity.getTrackedEntityType())) {
      throw new ForbiddenException(
          "Temporary ownership not created. User has no data read access to tracked entity type: "
              + trackedEntity.getTrackedEntityType().getUid());
    }

    if (!Objects.equals(
        program.getTrackedEntityType().getUid(), trackedEntity.getTrackedEntityType().getUid())) {
      throw new ForbiddenException(
          String.format(
              "Temporary ownership not created. The tracked entity type of the program %s differs from that of the tracked entity %s.",
              program.getTrackedEntityType().getUid(),
              trackedEntity.getTrackedEntityType().getUid()));
    }
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasAccess(UserDetails user, TrackedEntity trackedEntity, Program program) {
    if (canSkipOwnershipCheck(user, program) || trackedEntity == null) {
      return true;
    }

    OrganisationUnit ou = getOwner(trackedEntity, program, trackedEntity::getOrganisationUnit);

    final String orgUnitPath = ou.getStoredPath();
    return switch (program.getAccessLevel()) {
      case OPEN, AUDITED -> user.isInUserEffectiveSearchOrgUnitHierarchy(orgUnitPath);
      case PROTECTED ->
          user.isInUserHierarchy(orgUnitPath) || hasTemporaryAccess(trackedEntity, program, user);
      case CLOSED -> user.isInUserHierarchy(orgUnitPath);
    };
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasAccess(
      UserDetails user, String trackedEntity, OrganisationUnit owningOrgUnit, Program program) {
    if (canSkipOwnershipCheck(user, program) || trackedEntity == null || owningOrgUnit == null) {
      return true;
    }

    final String orgUnitPath = owningOrgUnit.getStoredPath();
    return switch (program.getAccessLevel()) {
      case OPEN, AUDITED -> user.isInUserEffectiveSearchOrgUnitHierarchy(orgUnitPath);
      case PROTECTED ->
          user.isInUserHierarchy(orgUnitPath)
              || hasTemporaryAccessWithUid(trackedEntity, program, user);
      case CLOSED -> user.isInUserHierarchy(orgUnitPath);
    };
  }

  @Override
  public boolean canSkipOwnershipCheck(UserDetails user, Program program) {
    return program == null || canSkipOwnershipCheck(user, program.getProgramType());
  }

  @Override
  public boolean canSkipOwnershipCheck(UserDetails user, ProgramType programType) {
    return user == null || user.isSuper() || ProgramType.WITHOUT_REGISTRATION == programType;
  }

  @Override
  public boolean isOwnerInUserSearchScope(
      UserDetails user, TrackedEntity trackedEntity, Program program) {
    return user.isInUserSearchHierarchy(
        getOwner(trackedEntity, program, trackedEntity::getOrganisationUnit).getStoredPath());
  }

  // -------------------------------------------------------------------------
  // Private Helper Methods
  // -------------------------------------------------------------------------

  /**
   * Get the current owner of this TE-program combination. Falls back to the registered organisation
   * unit if no owner explicitly exists for the program.
   *
   * @param trackedEntity the TE.
   * @param program The program
   * @return The owning organisation unit.
   */
  private OrganisationUnit getOwner(
      TrackedEntity trackedEntity,
      Program program,
      Supplier<OrganisationUnit> orgUnitIfMissingSupplier) {
    return ownerCache.get(
        getOwnershipCacheKey(trackedEntity, program),
        s -> {
          TrackedEntityProgramOwner trackedEntityProgramOwner =
              trackedEntityProgramOwnerService.getTrackedEntityProgramOwner(trackedEntity, program);

          return Optional.ofNullable(trackedEntityProgramOwner)
              .map(tepo -> recursivelyInitializeOrgUnit(tepo.getOrganisationUnit()))
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
   * @param trackedEntity The tracked entity object
   * @param program The program object
   * @param user The user object against which the check has to be performed
   * @return true if the user has temporary access, false otherwise
   */
  private boolean hasTemporaryAccess(
      TrackedEntity trackedEntity, Program program, UserDetails user) {
    if (canSkipOwnershipCheck(user, program) || trackedEntity == null) {
      return true;
    }
    return tempOwnerCache.get(
        getTempOwnershipCacheKey(trackedEntity.getUid(), program.getUid(), user.getUid()),
        s ->
            (programTempOwnerService.getValidTempOwnerRecordCount(
                    program, trackedEntity.getUid(), user)
                > 0));
  }

  private boolean hasTemporaryAccessWithUid(
      String trackedEntityUid, Program program, UserDetails user) {
    if (canSkipOwnershipCheck(user, program) || trackedEntityUid == null) {
      return true;
    }

    return tempOwnerCache.get(
        getTempOwnershipCacheKey(trackedEntityUid, program.getUid(), user.getUid()),
        s ->
            programTempOwnerService.getValidTempOwnerRecordCount(program, trackedEntityUid, user)
                > 0);
  }
}
