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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService;
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
public class EnrollmentPersister
    extends AbstractTrackerPersister<org.hisp.dhis.tracker.imports.domain.Enrollment, Enrollment> {
  private final TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  public EnrollmentPersister(
      ReservedValueService reservedValueService,
      TrackedEntityProgramOwnerService trackedEntityProgramOwnerService,
      TrackedEntityChangeLogService trackedEntityChangeLogService) {
    super(reservedValueService, trackedEntityChangeLogService);

    this.trackedEntityProgramOwnerService = trackedEntityProgramOwnerService;
  }

  @Override
  protected void updateAttributes(
      EntityManager entityManager,
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.Enrollment enrollment,
      Enrollment enrollmentToPersist,
      UserDetails user) {
    handleTrackedEntityAttributeValues(
        entityManager,
        preheat,
        enrollment.getAttributes(),
        enrollmentToPersist.getTrackedEntity(),
        user);
  }

  @Override
  protected void updatePreheat(TrackerPreheat preheat, Enrollment enrollment) {
    preheat.putEnrollment(enrollment);
    preheat.addProgramOwner(
        UID.of(enrollment.getTrackedEntity()),
        enrollment.getProgram().getUid(),
        enrollment.getOrganisationUnit());
  }

  @Override
  protected TrackerNotificationDataBundle handleNotifications(
      TrackerBundle bundle, Enrollment enrollment, List<NotificationTrigger> triggers) {

    return TrackerNotificationDataBundle.builder()
        .klass(Enrollment.class)
        .enrollmentNotifications(bundle.getEnrollmentNotifications().get(UID.of(enrollment)))
        .object(enrollment.getUid())
        .importStrategy(bundle.getImportStrategy())
        .accessedBy(bundle.getUser().getUsername())
        .enrollment(enrollment)
        .program(enrollment.getProgram())
        .triggers(triggers)
        .build();
  }

  @Override
  protected List<NotificationTrigger> determineNotificationTriggers(
      TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.Enrollment entity) {
    Enrollment persistedEnrollment = preheat.getEnrollment(entity.getUid());
    List<NotificationTrigger> triggers = new ArrayList<>();

    if (persistedEnrollment == null) {
      // New enrollment
      triggers.add(NotificationTrigger.ENROLLMENT);

      // New enrollment that is completed
      if (entity.getStatus() == EnrollmentStatus.COMPLETED) {
        triggers.add(NotificationTrigger.ENROLLMENT_COMPLETION);
      }
    } else {
      // Existing enrollment that has changed to completed
      if (persistedEnrollment.getStatus() != entity.getStatus()
          && entity.getStatus() == EnrollmentStatus.COMPLETED) {
        triggers.add(NotificationTrigger.ENROLLMENT_COMPLETION);
      }
    }

    return triggers;
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
      Enrollment entity) {
    if (isNew(bundle, trackerDto)
        && (bundle.getPreheat().getProgramOwner().get(UID.of(entity.getTrackedEntity())) == null
            || bundle
                    .getPreheat()
                    .getProgramOwner()
                    .get(UID.of(entity.getTrackedEntity()))
                    .get(entity.getProgram().getUid())
                == null)) {
      trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
          entity.getTrackedEntity(), entity.getProgram(), entity.getOrganisationUnit());
    }
  }

  @Override
  protected void updateDataValues(
      EntityManager entityManager,
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.Enrollment trackerDto,
      Enrollment payloadEntity,
      Enrollment currentEntity,
      UserDetails user) {
    // DO NOTHING - TE HAVE NO DATA VALUES
  }

  @Override
  protected Set<UID> getUpdatedTrackedEntities(Enrollment entity) {
    return Set.of(UID.of(entity.getTrackedEntity()));
  }

  @Override
  protected Enrollment cloneEntityProperties(
      TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.Enrollment trackerDto) {
    return null;
    // NO NEED TO CLONE RELATIONSHIP PROPERTIES
  }
}
