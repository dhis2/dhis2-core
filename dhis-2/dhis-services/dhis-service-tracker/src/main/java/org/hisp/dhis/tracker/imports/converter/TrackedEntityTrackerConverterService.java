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
package org.hisp.dhis.tracker.imports.converter;

import java.util.Date;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
public class TrackedEntityTrackerConverterService
    implements TrackerConverterService<
        org.hisp.dhis.tracker.imports.domain.TrackedEntity, TrackedEntity> {

  @Override
  public TrackedEntity from(
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity,
      UserDetails user) {
    TrackedEntity dbTrackedEntity = preheat.getTrackedEntity(trackedEntity.getTrackedEntity());
    OrganisationUnit organisationUnit = preheat.getOrganisationUnit(trackedEntity.getOrgUnit());
    TrackedEntityType trackedEntityType =
        preheat.getTrackedEntityType(trackedEntity.getTrackedEntityType());

    Date now = new Date();

    if (isNewEntity(dbTrackedEntity)) {
      dbTrackedEntity = new TrackedEntity();
      dbTrackedEntity.setUid(trackedEntity.getTrackedEntity());
      dbTrackedEntity.setCreated(now);
      dbTrackedEntity.setCreatedByUserInfo(UserInfoSnapshot.from(user));
    }

    dbTrackedEntity.setLastUpdatedByUserInfo(UserInfoSnapshot.from(user));
    dbTrackedEntity.setStoredBy(trackedEntity.getStoredBy());
    dbTrackedEntity.setLastUpdated(now);
    dbTrackedEntity.setDeleted(false);
    dbTrackedEntity.setPotentialDuplicate(trackedEntity.isPotentialDuplicate());
    dbTrackedEntity.setCreatedAtClient(DateUtils.fromInstant(trackedEntity.getCreatedAtClient()));
    dbTrackedEntity.setLastUpdatedAtClient(
        DateUtils.fromInstant(trackedEntity.getUpdatedAtClient()));
    dbTrackedEntity.setOrganisationUnit(organisationUnit);
    dbTrackedEntity.setTrackedEntityType(trackedEntityType);
    dbTrackedEntity.setInactive(trackedEntity.isInactive());
    dbTrackedEntity.setGeometry(trackedEntity.getGeometry());

    return dbTrackedEntity;
  }
}
