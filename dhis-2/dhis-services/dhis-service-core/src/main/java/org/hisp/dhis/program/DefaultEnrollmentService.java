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
package org.hisp.dhis.program;

import java.util.Date;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.notification.event.ProgramEnrollmentNotificationEvent;
import org.hisp.dhis.programrule.engine.EnrollmentEvaluationEvent;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.program.EnrollmentService")
public class DefaultEnrollmentService implements EnrollmentService {
  private final EnrollmentStore enrollmentStore;

  private final TrackedEntityService trackedEntityService;

  private final ApplicationEventPublisher eventPublisher;

  private final TrackerOwnershipManager trackerOwnershipAccessManager;

  @Override
  @Transactional
  public long addEnrollment(Enrollment enrollment) {
    enrollmentStore.save(enrollment);
    return enrollment.getId();
  }

  @Override
  @Transactional
  public void deleteEnrollment(Enrollment enrollment) {
    enrollment.setStatus(EnrollmentStatus.CANCELLED);
    enrollmentStore.update(enrollment);
    enrollmentStore.delete(enrollment);
  }

  @Override
  @Transactional
  public void hardDeleteEnrollment(Enrollment enrollment) {
    enrollmentStore.hardDelete(enrollment);
  }

  @Override
  @Transactional(readOnly = true)
  public Enrollment getEnrollment(long id) {
    return enrollmentStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Enrollment getEnrollment(String uid) {
    return enrollmentStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Enrollment> getEnrollments(@Nonnull List<String> uids) {
    return enrollmentStore.getByUid(uids);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean enrollmentExists(String uid) {
    return enrollmentStore.exists(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean enrollmentExistsIncludingDeleted(String uid) {
    return enrollmentStore.existsIncludingDeleted(uid);
  }

  @Override
  @Transactional
  public void updateEnrollment(Enrollment enrollment) {
    enrollmentStore.update(enrollment);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Enrollment> getEnrollments(Program program) {
    return enrollmentStore.get(program);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Enrollment> getEnrollments(Program program, EnrollmentStatus status) {
    return enrollmentStore.get(program, status);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Enrollment> getEnrollments(
      TrackedEntity trackedEntity, Program program, EnrollmentStatus status) {
    return enrollmentStore.get(trackedEntity, program, status);
  }

  private Enrollment prepareEnrollment(
      TrackedEntity trackedEntity,
      Program program,
      Date enrollmentDate,
      Date occurredDate,
      OrganisationUnit organisationUnit,
      String uid) {
    if (program.getTrackedEntityType() != null
        && !program.getTrackedEntityType().equals(trackedEntity.getTrackedEntityType())) {
      throw new IllegalQueryException(
          "Tracked entity must have same tracked entity as program: " + program.getUid());
    }

    Enrollment enrollment = new Enrollment();
    enrollment.setUid(CodeGenerator.isValidUid(uid) ? uid : CodeGenerator.generateUid());
    enrollment.setOrganisationUnit(organisationUnit);
    enrollment.enrollTrackedEntity(trackedEntity, program);

    if (enrollmentDate != null) {
      enrollment.setEnrollmentDate(enrollmentDate);
    } else {
      enrollment.setEnrollmentDate(new Date());
    }

    if (occurredDate != null) {
      enrollment.setOccurredDate(occurredDate);
    } else {
      enrollment.setOccurredDate(new Date());
    }

    enrollment.setStatus(EnrollmentStatus.ACTIVE);

    return enrollment;
  }

  @Override
  @Transactional
  public Enrollment enrollTrackedEntity(
      TrackedEntity trackedEntity,
      Program program,
      Date enrollmentDate,
      Date occurredDate,
      OrganisationUnit organisationUnit) {
    return enrollTrackedEntity(
        trackedEntity,
        program,
        enrollmentDate,
        occurredDate,
        organisationUnit,
        CodeGenerator.generateUid());
  }

  @Override
  @Transactional
  public Enrollment enrollTrackedEntity(
      TrackedEntity trackedEntity,
      Program program,
      Date enrollmentDate,
      Date occurredDate,
      OrganisationUnit organisationUnit,
      String uid) {
    Enrollment enrollment =
        prepareEnrollment(
            trackedEntity, program, enrollmentDate, occurredDate, organisationUnit, uid);
    addEnrollment(enrollment);
    trackerOwnershipAccessManager.assignOwnership(
        trackedEntity, program, organisationUnit, true, true);
    eventPublisher.publishEvent(new ProgramEnrollmentNotificationEvent(this, enrollment.getId()));
    eventPublisher.publishEvent(new EnrollmentEvaluationEvent(this, enrollment.getId()));
    updateEnrollment(enrollment);
    trackedEntityService.updateTrackedEntity(trackedEntity);
    return enrollment;
  }
}
