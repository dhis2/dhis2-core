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
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLogService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.bundle.TrackerObjectsMapper;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.job.NotificationTrigger;
import org.hisp.dhis.tracker.imports.job.TrackerNotificationDataBundle;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class RelationshipPersister
    extends AbstractTrackerPersister<Relationship, org.hisp.dhis.relationship.Relationship> {

  public RelationshipPersister(
      ReservedValueService reservedValueService,
      TrackedEntityChangeLogService trackedEntityChangeLogService) {

    super(reservedValueService, trackedEntityChangeLogService);
  }

  @Override
  protected org.hisp.dhis.relationship.Relationship convert(
      TrackerBundle bundle, Relationship trackerDto) {
    if (bundle.getStrategy(trackerDto) == TrackerImportStrategy.UPDATE) {
      return null;
    }

    return TrackerObjectsMapper.map(bundle.getPreheat(), trackerDto, bundle.getUser());
  }

  @Override
  protected void updateAttributes(
      EntityManager entityManager,
      TrackerPreheat preheat,
      Relationship trackerDto,
      org.hisp.dhis.relationship.Relationship hibernateEntity,
      UserDetails user) {
    // NOTHING TO DO
  }

  @Override
  protected void updatePreheat(
      TrackerPreheat preheat, org.hisp.dhis.relationship.Relationship convertedDto) {
    // NOTHING TO DO
  }

  @Override
  protected TrackerNotificationDataBundle handleNotifications(
      TrackerBundle bundle,
      org.hisp.dhis.relationship.Relationship entity,
      List<NotificationTrigger> triggers) {
    return TrackerNotificationDataBundle.builder().build();
  }

  @Override
  protected TrackerType getType() {
    return TrackerType.RELATIONSHIP;
  }

  @Override
  protected List<Relationship> getByType(TrackerBundle bundle) {
    return bundle.getRelationships();
  }

  @Override
  protected void persistOwnership(
      TrackerBundle bundle,
      Relationship trackerDto,
      org.hisp.dhis.relationship.Relationship entity) {
    // NOTHING TO DO

  }

  @Override
  protected void updateDataValues(
      EntityManager entityManager,
      TrackerPreheat preheat,
      Relationship trackerDto,
      org.hisp.dhis.relationship.Relationship payloadEntity,
      org.hisp.dhis.relationship.Relationship currentEntity,
      UserDetails user) {
    // DO NOTHING - TE HAVE NO DATA VALUES
  }

  @Override
  protected Set<UID> getUpdatedTrackedEntities(org.hisp.dhis.relationship.Relationship entity) {
    return entity.getTrackedEntityOrigins();
  }

  @Override
  protected org.hisp.dhis.relationship.Relationship cloneEntityProperties(
      TrackerPreheat preheat, Relationship trackerDto) {
    return null;
    // NO NEED TO CLONE RELATIONSHIP PROPERTIES
  }

  @Override
  protected List<NotificationTrigger> determineNotificationTriggers(
      TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.Relationship entity) {
    return List.of();
  }
}
