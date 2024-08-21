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

import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTempOwnerService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ameen Mohamed
 */
@Slf4j
@Service("org.hisp.dhis.trackedentity.ApiTrackerOwnershipManager")
public class DefaultApiTrackerOwnershipManager implements ApiTrackerOwnershipManager {
  private final TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;
  private final ProgramTempOwnerService programTempOwnerService;

  public DefaultApiTrackerOwnershipManager(
      TrackedEntityProgramOwnerService trackedEntityProgramOwnerService,
      CacheProvider cacheProvider,
      ProgramTempOwnerService programTempOwnerService) {

    this.trackedEntityProgramOwnerService = trackedEntityProgramOwnerService;
    this.programTempOwnerService = programTempOwnerService;
    this.ownerCache = cacheProvider.createProgramOwnerCache();
    this.tempOwnerCache = cacheProvider.createProgramTempOwnerCache();
  }

  /** Cache for storing recent ownership checks */
  private final Cache<OrganisationUnit> ownerCache;

  /** Cache for storing recent temporary ownership checks */
  private final Cache<Boolean> tempOwnerCache;

  // -------------------------------------------------------------------------
  // Implementation
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public boolean hasAccess(UserDetails user, TrackedEntity trackedEntity, Program program) {
    if (canSkipOwnershipCheck(user, program) || trackedEntity == null) {
      return true;
    }

    OrganisationUnit ou = getOwner(trackedEntity, program, trackedEntity::getOrganisationUnit);

    final String orgUnitPath = ou.getPath();
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

    final String orgUnitPath = owningOrgUnit.getPath();
    return switch (program.getAccessLevel()) {
      case OPEN, AUDITED -> user.isInUserEffectiveSearchOrgUnitHierarchy(orgUnitPath);
      case PROTECTED ->
          user.isInUserHierarchy(orgUnitPath)
              || hasTemporaryAccessWithUid(trackedEntity, program, user);
      case CLOSED -> user.isInUserHierarchy(orgUnitPath);
    };
  }

  private boolean canSkipOwnershipCheck(UserDetails user, Program program) {
    return user == null
        || user.isSuper()
        || program == null
        || ProgramType.WITHOUT_REGISTRATION == program.getProgramType();
  }

  @Override
  public boolean isOwnerInUserSearchScope(
      UserDetails user, TrackedEntity trackedEntity, Program program) {
    return user.isInUserSearchHierarchy(
        getOwner(trackedEntity, program, trackedEntity::getOrganisationUnit).getPath());
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
        getOwnershipCacheKey(trackedEntity::getId, program),
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

  /**
   * Returns key used to store and retrieve cached records for ownership
   *
   * @return a String representing a record of ownership
   */
  private String getOwnershipCacheKey(LongSupplier trackedEntityIdSupplier, Program program) {
    return trackedEntityIdSupplier.getAsLong() + "_" + program.getUid();
  }

  /**
   * Returns key used to store and retrieve cached records for ownership
   *
   * @return a String representing a record of ownership
   */
  private String getTempOwnershipCacheKey(String teUid, String programUid, String userUid) {
    return teUid + "-" + programUid + "-" + userUid;
  }
}
