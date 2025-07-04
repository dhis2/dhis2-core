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
package org.hisp.dhis.tracker.imports.bundle.persister;

import jakarta.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLogService;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.bundle.TrackerObjectsMapper;
import org.hisp.dhis.tracker.imports.job.NotificationTrigger;
import org.hisp.dhis.tracker.imports.job.TrackerNotificationDataBundle;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class TrackedEntityPersister
    extends AbstractTrackerPersister<
        org.hisp.dhis.tracker.imports.domain.TrackedEntity, TrackedEntity> {

  public TrackedEntityPersister(
      ReservedValueService reservedValueService,
      TrackedEntityChangeLogService trackedEntityChangeLogService) {
    super(reservedValueService, trackedEntityChangeLogService);
  }

  @Override
  protected void updateAttributes(
      EntityManager entityManager,
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.TrackedEntity trackerDto,
      TrackedEntity te,
      UserDetails user) {
    handleTrackedEntityAttributeValues(
        entityManager, preheat, trackerDto.getAttributes(), te, user);
  }

  @Override
  protected void updatePreheat(TrackerPreheat preheat, TrackedEntity dto) {
    preheat.putTrackedEntities(Collections.singletonList(dto));
  }

  @Override
  protected TrackedEntity convert(
      TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.TrackedEntity trackerDto) {
    return TrackerObjectsMapper.map(bundle.getPreheat(), trackerDto, bundle.getUser());
  }

  @Override
  protected TrackerType getType() {
    return TrackerType.TRACKED_ENTITY;
  }

  @Override
  protected List<org.hisp.dhis.tracker.imports.domain.TrackedEntity> getByType(
      TrackerBundle bundle) {
    return bundle.getTrackedEntities();
  }

  @Override
  protected TrackerNotificationDataBundle handleNotifications(
      TrackerBundle bundle, TrackedEntity entity, List<NotificationTrigger> triggers) {
    return TrackerNotificationDataBundle.builder().build();
  }

  @Override
  protected void persistOwnership(
      TrackerBundle bundle,
      org.hisp.dhis.tracker.imports.domain.TrackedEntity trackerDto,
      TrackedEntity entity) {
    // DO NOTHING, TE alone does not have ownership records

  }

  @Override
  protected void updateDataValues(
      EntityManager entityManager,
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.TrackedEntity trackerDto,
      TrackedEntity payloadEntity,
      TrackedEntity currentEntity,
      UserDetails user) {
    // DO NOTHING - TE HAVE NO DATA VALUES
  }

  @Override
  protected Set<UID> getUpdatedTrackedEntities(TrackedEntity entity) {
    return Set.of(); // We don't need to keep track, TE has already been updated
  }

  @Override
  protected TrackedEntity cloneEntityProperties(
      TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.TrackedEntity trackerDto) {
    return null;
    // NO NEED TO CLONE RELATIONSHIP PROPERTIES
  }

  @Override
  protected List<NotificationTrigger> determineNotificationTriggers(
      TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.TrackedEntity entity) {
    return List.of();
  }
}
