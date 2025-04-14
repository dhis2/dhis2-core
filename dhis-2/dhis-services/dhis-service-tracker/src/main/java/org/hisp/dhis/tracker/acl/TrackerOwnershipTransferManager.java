/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
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
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service("org.hisp.dhis.tracker.acl.TrackerOwnershipTransferManager")
public class TrackerOwnershipTransferManager {
  private static final int TEMPORARY_OWNERSHIP_VALIDITY_IN_HOURS = 3;

  private final TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;
  private final ProgramTempOwnershipAuditService programTempOwnershipAuditService;
  private final ProgramTempOwnerService programTempOwnerService;
  private final ProgramOwnershipHistoryService programOwnershipHistoryService;
  private final UserService userService;
  private final ProgramService programService;
  private final IdentifiableObjectManager manager;
  private final AclService aclService;
  private final TrackerOwnershipAccessManager trackerOwnershipAccessManager;
  private final TrackedEntityService trackedEntityService;
  private final TrackerProgramService trackerProgramService;
  private final OrganisationUnitService organisationUnitService;

  public TrackerOwnershipTransferManager(
      UserService userService,
      TrackedEntityProgramOwnerService trackedEntityProgramOwnerService,
      CacheProvider cacheProvider,
      ProgramTempOwnershipAuditService programTempOwnershipAuditService,
      ProgramTempOwnerService programTempOwnerService,
      ProgramOwnershipHistoryService programOwnershipHistoryService,
      ProgramService programService,
      IdentifiableObjectManager manager,
      AclService aclService,
      TrackerOwnershipAccessManager trackerOwnershipAccessManager,
      TrackedEntityService trackedEntityService,
      TrackerProgramService trackerProgramService,
      OrganisationUnitService organisationUnitService) {

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
    this.trackerOwnershipAccessManager = trackerOwnershipAccessManager;
    this.trackedEntityService = trackedEntityService;
    this.trackerProgramService = trackerProgramService;
    this.organisationUnitService = organisationUnitService;
  }

  /** Cache for storing recent ownership checks */
  private final Cache<OrganisationUnit> ownerCache;

  /** Cache for storing recent temporary ownership checks */
  private final Cache<Boolean> tempOwnerCache;

  /** Transfers the ownership of the given TE - program pair, to the specified org unit. */
  @Transactional
  public void transferOwnership(UID trackedEntityUid, UID programUid, UID orgUnitUid)
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntity trackedEntity =
        trackedEntityService.getTrackedEntity(
            trackedEntityUid, programUid, TrackedEntityParams.FALSE);
    Program program = trackerProgramService.getTrackerProgram(programUid);
    OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(orgUnitUid.getValue());

    if (orgUnit == null) {
      throw new ForbiddenException("Org unit supplied does not exist.");
    }

    UserDetails currentUser = getCurrentUserDetails();

    if (trackerOwnershipAccessManager.hasAccess(currentUser, trackedEntity, program)) {
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

      ownerCache.invalidate(
          trackerOwnershipAccessManager.getOwnershipCacheKey(trackedEntity, program));
    } else {
      log.error("Unauthorized attempt to change ownership");
      throw new ForbiddenException(
          "User does not have access to change ownership for the entity-program combination");
    }
  }

  /**
   * Grant temporary ownership for a user for a specific tracked entity - program combination
   *
   * @param trackedEntityUid The UID of the tracked entity object
   * @param programUid The UID of the program object
   * @param reason The reason for requesting temporary ownership
   */
  @Transactional
  public void grantTemporaryOwnership(UID trackedEntityUid, UID programUid, String reason)
      throws ForbiddenException, BadRequestException {
    if (trackedEntityUid == null) {
      throw new BadRequestException("Provided tracked entity can't be null.");
    }
    UserDetails user = getCurrentUserDetails();
    // TODO(tracker) jdbc-hibernate: check the impact on performance
    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, trackedEntityUid.getValue());
    Program program = trackerProgramService.getTrackerProgram(programUid);

    validateTrackedEntity(trackedEntity, user);
    validateProgram(program, trackedEntity);
    validateUser(trackedEntity, program, user);

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
        trackerOwnershipAccessManager.getTempOwnershipCacheKey(
            trackedEntity.getUid(), program.getUid(), user.getUid()));
  }

  private void validateTrackedEntity(TrackedEntity trackedEntity, UserDetails user)
      throws ForbiddenException {
    if (trackedEntity == null) {
      throw new ForbiddenException(
          "Temporary ownership not created. Tracked entity supplied does not exist.");
    }

    if (!aclService.canDataRead(user, trackedEntity.getTrackedEntityType())) {
      throw new ForbiddenException(
          "Temporary ownership not created. User has no data read access to tracked entity type: "
              + trackedEntity.getTrackedEntityType().getUid());
    }
  }

  private void validateProgram(Program program, TrackedEntity trackedEntity)
      throws ForbiddenException {
    if (!program.isProtected()) {
      throw new ForbiddenException(
          String.format(
              "Temporary ownership not created. Temporary ownership can only be granted to protected programs. %s access level is %s.",
              program.getUid(), program.getAccessLevel().name()));
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

  private void validateUser(TrackedEntity trackedEntity, Program program, UserDetails user)
      throws ForbiddenException {
    if (user.isSuper()) {
      throw new ForbiddenException("Temporary ownership not created. Current user is a superuser.");
    }

    if (!trackerOwnershipAccessManager.isOwnerInUserSearchScope(user, trackedEntity, program)) {
      throw new ForbiddenException(
          "Temporary ownership not created. The owner of the entity-program combination is not in the user's search scope.");
    }
  }
}
