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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.fileresource.FileResourceStore;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.bundle.TrackerObjectsMapper;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.notification.EntityNotifications;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.programrule.engine.Notification;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.model.TrackedEntityProgramOwner;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class EnrollmentPersister
    extends AbstractTrackerPersister<org.hisp.dhis.tracker.imports.domain.Enrollment, Enrollment> {
  private final TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  public EnrollmentPersister(
      DataSource dataSource,
      FileResourceStore fileResourceStore,
      ObjectMapper objectMapper,
      TrackedEntityProgramOwnerService trackedEntityProgramOwnerService) {
    super(dataSource, fileResourceStore, objectMapper);
    this.trackedEntityProgramOwnerService = trackedEntityProgramOwnerService;
  }

  @Override
  protected String sequenceName() {
    return "enrollment_sequence";
  }

  @Override
  protected void assignId(Enrollment entity, long id) {
    entity.setId(id);
  }

  @Override
  protected void stageInsert(Enrollment entity, EntityWriteBatch batch) {
    batch.stageInsert(entity);
  }

  @Override
  protected void stageUpdate(Enrollment entity, EntityWriteBatch batch) {
    batch.stageUpdate(entity);
  }

  @Override
  protected void updateAttributes(
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.Enrollment enrollment,
      Enrollment enrollmentToPersist,
      UserDetails user,
      ChangeLogAccumulator changeLogs,
      EntityWriteBatch batch,
      Map<Long, Map<MetadataIdentifier, TrackedEntityAttributeValue>> existingAttributeValues) {
    handleTrackedEntityAttributeValues(
        preheat,
        enrollment.getAttributes(),
        enrollmentToPersist.getTrackedEntity(),
        user,
        changeLogs,
        batch,
        existingAttributeValues);
  }

  @Override
  protected Set<String> trackedEntityUidsForAttributeLoad(
      List<org.hisp.dhis.tracker.imports.domain.Enrollment> dtos) {
    return dtos.stream()
        .map(org.hisp.dhis.tracker.imports.domain.Enrollment::getTrackedEntity)
        .filter(java.util.Objects::nonNull)
        .map(UID::getValue)
        .collect(Collectors.toSet());
  }

  @Override
  protected void updatePreheat(TrackerPreheat preheat, Enrollment enrollment) {
    preheat.putEnrollment(enrollment);
    preheat.addProgramOwner(
        enrollment.getTrackedEntity().getUID(),
        enrollment.getProgram().getUid(),
        enrollment.getOrganisationUnit());
  }

  @Override
  protected boolean isBeingCompleted(
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.Enrollment entity,
      boolean isNew) {
    if (entity.getStatus() != EnrollmentStatus.COMPLETED) {
      return false;
    }
    if (isNew) {
      return true;
    }
    Enrollment persisted = preheat.getEnrollment(entity.getUID());
    return persisted != null && persisted.getStatus() != EnrollmentStatus.COMPLETED;
  }

  @Override
  protected EntityNotifications collectNotifications(
      TrackerBundle bundle, Enrollment enrollment, boolean isNew, boolean completedInThisImport) {
    EnumSet<NotificationTrigger> applicableTriggers = EnumSet.noneOf(NotificationTrigger.class);
    if (isNew) {
      applicableTriggers.add(NotificationTrigger.ENROLLMENT);
    }
    if (completedInThisImport) {
      applicableTriggers.add(NotificationTrigger.COMPLETION);
    }
    Set<ProgramNotificationTemplate> matchedTemplates =
        filterTemplates(enrollment.getProgram().getNotificationTemplates(), applicableTriggers);
    List<Notification> ruleEngineNotifications =
        bundle.getEnrollmentNotifications().getOrDefault(enrollment.getUID(), List.of());

    Set<Notification> notifications = mergeNotifications(matchedTemplates, ruleEngineNotifications);
    return notifications.isEmpty() ? null : new EntityNotifications(enrollment, notifications);
  }

  @Override
  protected Enrollment convert(
      TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.Enrollment enrollment) {
    return TrackerObjectsMapper.map(bundle.getPreheat(), enrollment, bundle.getUser());
  }

  @Override
  protected TrackerType getType() {
    return TrackerType.ENROLLMENT;
  }

  @Override
  protected List<org.hisp.dhis.tracker.imports.domain.Enrollment> getByType(TrackerBundle bundle) {
    return bundle.getEnrollments();
  }

  @Override
  protected void persistOwnership(
      TrackerBundle bundle,
      org.hisp.dhis.tracker.imports.domain.Enrollment trackerDto,
      Enrollment entity,
      EntityWriteBatch batch) {
    if ((bundle.getPreheat().getProgramOwner().get(entity.getTrackedEntity().getUID()) == null
        || bundle
                .getPreheat()
                .getProgramOwner()
                .get(entity.getTrackedEntity().getUID())
                .get(entity.getProgram().getUid())
            == null)) {
      // Mirrors DefaultTrackedEntityProgramOwnerService.createTrackedEntityProgramOwner, but stages
      // the row into the write batch instead of a per-enrollment Hibernate save (see
      // AbstractTrackerPersister#persistOwnership and TrackedEntityProgramOwnerWriter).
      TrackedEntityProgramOwner owner =
          new TrackedEntityProgramOwner(
              entity.getTrackedEntity(), entity.getProgram(), entity.getOrganisationUnit());
      owner.updateDates();
      owner.setCreatedBy(CurrentUserUtil.getCurrentUsername());
      batch.stageOwnershipInsert(owner);
      // The store's save()/update() invalidate the program-owner cache; since we bypass the store,
      // invalidate it here so a stale "no owner / registering org unit" entry cached before this
      // import does not survive the newly created ownership.
      trackedEntityProgramOwnerService.invalidateOwnershipCache(
          entity.getTrackedEntity(), entity.getProgram());
    }
  }

  @Override
  protected void updateDataValues(
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.Enrollment trackerDto,
      Enrollment payloadEntity,
      Enrollment currentEntity,
      UserDetails user,
      ChangeLogAccumulator changeLogs) {
    // DO NOTHING - ENROLLMENTS HAVE NO DATA VALUES
  }

  @Override
  protected Set<UID> getUpdatedTrackedEntities(Enrollment entity) {
    return Set.of(entity.getTrackedEntity().getUID());
  }

  @Override
  protected Enrollment cloneEntityProperties(
      TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.Enrollment trackerDto) {
    return null;
    // NO NEED TO CLONE RELATIONSHIP PROPERTIES
  }
}
