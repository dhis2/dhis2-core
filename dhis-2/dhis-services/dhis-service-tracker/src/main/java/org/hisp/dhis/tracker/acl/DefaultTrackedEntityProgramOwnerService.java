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

import javax.annotation.Nonnull;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.user.CurrentUserUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ameen Mohamed
 */
@Service("org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService")
public class DefaultTrackedEntityProgramOwnerService implements TrackedEntityProgramOwnerService {
  private final TrackedEntityProgramOwnerStore trackedEntityProgramOwnerStore;
  private final Cache<OrganisationUnit> ownerCache;

  public DefaultTrackedEntityProgramOwnerService(
      TrackedEntityProgramOwnerStore trackedEntityProgramOwnerStore, CacheProvider cacheProvider) {
    this.trackedEntityProgramOwnerStore = trackedEntityProgramOwnerStore;
    this.ownerCache = cacheProvider.createProgramOwnerCache();
  }

  @Override
  @Transactional
  public void createTrackedEntityProgramOwner(
      TrackedEntity trackedEntity, Program program, OrganisationUnit orgUnit) {
    if (trackedEntity == null || program == null || orgUnit == null) {
      return;
    }
    trackedEntityProgramOwnerStore.save(
        buildTrackedEntityProgramOwner(trackedEntity, program, orgUnit));
    ownerCache.invalidate(getOwnershipCacheKey(trackedEntity, program));
  }

  private TrackedEntityProgramOwner buildTrackedEntityProgramOwner(
      TrackedEntity trackedEntity, Program program, OrganisationUnit ou) {
    TrackedEntityProgramOwner teProgramOwner =
        new TrackedEntityProgramOwner(trackedEntity, program, ou);
    teProgramOwner.updateDates();
    teProgramOwner.setCreatedBy(CurrentUserUtil.getCurrentUsername());
    return teProgramOwner;
  }

  @Override
  @Transactional
  public void createOrUpdateTrackedEntityProgramOwner(
      TrackedEntity trackedEntity, Program program, OrganisationUnit orgUnit) {
    if (trackedEntity == null || program == null || orgUnit == null) {
      return;
    }
    TrackedEntityProgramOwner teProgramOwner =
        trackedEntityProgramOwnerStore.getTrackedEntityProgramOwner(trackedEntity, program);
    if (teProgramOwner == null) {
      trackedEntityProgramOwnerStore.save(
          buildTrackedEntityProgramOwner(trackedEntity, program, orgUnit));
    } else {
      updateTrackedEntityProgramOwner(teProgramOwner, orgUnit);
      trackedEntityProgramOwnerStore.update(teProgramOwner);
    }
    ownerCache.invalidate(getOwnershipCacheKey(trackedEntity, program));
  }

  @Override
  @Transactional
  public void updateTrackedEntityProgramOwner(
      @Nonnull TrackedEntity trackedEntity,
      @Nonnull Program program,
      @Nonnull OrganisationUnit orgUnit)
      throws BadRequestException {
    TrackedEntityProgramOwner teProgramOwner =
        trackedEntityProgramOwnerStore.getTrackedEntityProgramOwner(trackedEntity, program);
    if (teProgramOwner == null) {
      throw new BadRequestException(
          String.format(
              "Tracked entity not transferred. No owner found for the tracked entity %s and program %s combination",
              trackedEntity.getUid(), program.getUid()));
    }
    updateTrackedEntityProgramOwner(teProgramOwner, orgUnit);
    trackedEntityProgramOwnerStore.update(teProgramOwner);
    ownerCache.invalidate(getOwnershipCacheKey(trackedEntity, program));
  }

  private void updateTrackedEntityProgramOwner(
      TrackedEntityProgramOwner teProgramOwner, OrganisationUnit ou) {
    teProgramOwner.setOrganisationUnit(ou);
    teProgramOwner.updateDates();
    teProgramOwner.setCreatedBy(CurrentUserUtil.getCurrentUsername());
  }

  @Override
  @Transactional(readOnly = true)
  public TrackedEntityProgramOwner getTrackedEntityProgramOwner(TrackedEntity te, Program program) {
    return trackedEntityProgramOwnerStore.getTrackedEntityProgramOwner(te, program);
  }
}
