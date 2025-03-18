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
package org.hisp.dhis.tracker.imports.bundle.persister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.persistence.EntityManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueChangeLogService;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.converter.TrackerConverterService;
import org.hisp.dhis.tracker.imports.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.imports.job.SideEffectTrigger;
import org.hisp.dhis.tracker.imports.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class EnrollmentPersister
    extends AbstractTrackerPersister<org.hisp.dhis.tracker.imports.domain.Enrollment, Enrollment> {
  private final TrackerConverterService<org.hisp.dhis.tracker.imports.domain.Enrollment, Enrollment>
      enrollmentConverter;

  private final TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  public EnrollmentPersister(
      ReservedValueService reservedValueService,
      TrackerConverterService<org.hisp.dhis.tracker.imports.domain.Enrollment, Enrollment>
          enrollmentConverter,
      TrackedEntityProgramOwnerService trackedEntityProgramOwnerService,
      TrackedEntityAttributeValueChangeLogService trackedEntityAttributeValueChangeLogService) {
    super(reservedValueService, trackedEntityAttributeValueChangeLogService);

    this.enrollmentConverter = enrollmentConverter;
    this.trackedEntityProgramOwnerService = trackedEntityProgramOwnerService;
  }

  @Override
  protected void updateAttributes(
      EntityManager entityManager,
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.Enrollment enrollment,
      Enrollment enrollmentToPersist) {
    handleTrackedEntityAttributeValues(
        entityManager,
        preheat,
        enrollment.getAttributes(),
        preheat.getTrackedEntity(enrollmentToPersist.getTrackedEntity().getUid()));
  }

  @Override
  protected void updateDataValues(
      EntityManager entityManager,
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.Enrollment enrollment,
      Enrollment enrollmentToPersist) {
    // DO NOTHING - TE HAVE NO DATA VALUES
  }

  @Override
  protected void persistNotes(
      EntityManager entityManager, TrackerPreheat preheat, Enrollment enrollment) {
    if (!enrollment.getNotes().isEmpty()) {
      for (Note note : enrollment.getNotes()) {
        if (Objects.isNull(preheat.getNote(note.getUid()))) {
          entityManager.persist(note);
        }
      }
    }
  }

  @Override
  protected void updatePreheat(TrackerPreheat preheat, Enrollment enrollment) {
    preheat.putEnrollments(Collections.singletonList(enrollment));
    preheat.addProgramOwner(
        enrollment.getTrackedEntity().getUid(),
        enrollment.getProgram().getUid(),
        enrollment.getOrganisationUnit());
  }

  @Override
  protected boolean isNew(TrackerPreheat preheat, String uid) {
    return preheat.getEnrollment(uid) == null;
  }

  @Override
  protected TrackerSideEffectDataBundle handleSideEffects(
      TrackerBundle bundle, Enrollment enrollment, List<SideEffectTrigger> triggers) {

    return TrackerSideEffectDataBundle.builder()
        .klass(Enrollment.class)
        .enrollmentRuleEffects(bundle.getEnrollmentRuleEffects())
        .eventRuleEffects(new HashMap<>())
        .object(enrollment.getUid())
        .importStrategy(bundle.getImportStrategy())
        .accessedBy(bundle.getUsername())
        .enrollment(enrollment)
        .program(enrollment.getProgram())
        .triggers(triggers)
        .build();
  }

  @Override
  protected List<SideEffectTrigger> determineSideEffectTriggers(
      TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.Enrollment entity) {
    Enrollment persistedEnrollment = preheat.getEnrollment(entity.getUid());
    List<SideEffectTrigger> triggers = new ArrayList<>();

    if (persistedEnrollment == null) {
      // New enrollment
      triggers.add(SideEffectTrigger.ENROLLMENT);

      // New enrollment that is completed
      if (entity.getStatus() == EnrollmentStatus.COMPLETED) {
        triggers.add(SideEffectTrigger.ENROLLMENT_COMPLETION);
      }
    } else {
      // Existing enrollment that has changed to completed
      if (persistedEnrollment.getStatus() != entity.getStatus().getProgramStatus()
          && entity.getStatus() == EnrollmentStatus.COMPLETED) {
        triggers.add(SideEffectTrigger.ENROLLMENT_COMPLETION);
      }
    }

    return triggers;
  }

  @Override
  protected Enrollment convert(
      TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.Enrollment enrollment) {
    return enrollmentConverter.from(bundle.getPreheat(), enrollment);
  }

  @Override
  protected TrackerType getType() {
    return TrackerType.ENROLLMENT;
  }

  @Override
  protected void persistOwnership(TrackerPreheat preheat, Enrollment entity) {
    if (isNew(preheat, entity.getUid())) {
      if (preheat.getProgramOwner().get(entity.getTrackedEntity().getUid()) == null
          || preheat
                  .getProgramOwner()
                  .get(entity.getTrackedEntity().getUid())
                  .get(entity.getProgram().getUid())
              == null) {
        trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
            entity.getTrackedEntity(), entity.getProgram(), entity.getOrganisationUnit());
      }
    }
  }

  @Override
  protected Set<UID> getUpdatedTrackedEntities(Enrollment entity) {
    return Set.of(UID.of(entity.getTrackedEntity()));
  }
}
