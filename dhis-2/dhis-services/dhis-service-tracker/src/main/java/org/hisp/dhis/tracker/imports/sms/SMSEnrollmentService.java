/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.imports.sms;

import static java.util.Objects.requireNonNullElseGet;

import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.notification.event.ProgramEnrollmentNotificationEvent;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.acl.TrackerOwnershipManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Deprecated(since = "2.42")
// TODO(tracker) This class will be removed as soon as the SMS feature uses the importer
@Service("org.hisp.dhis.tracker.imports.sms.SMSEnrollmentService")
public class SMSEnrollmentService {

  private final IdentifiableObjectManager manager;
  private final TrackerOwnershipManager trackerOwnershipAccessManager;
  private final ApplicationEventPublisher eventPublisher;

  public void enrollTrackedEntity(
      TrackedEntity trackedEntity,
      Program program,
      OrganisationUnit organisationUnit,
      Date occurredDate) {
    enrollTrackedEntity(
        trackedEntity, program, organisationUnit, occurredDate, CodeGenerator.generateUid());
  }

  public Enrollment enrollTrackedEntity(
      TrackedEntity trackedEntity,
      Program program,
      OrganisationUnit organisationUnit,
      Date occurredDate,
      String enrollmentUid) {
    Enrollment enrollment =
        prepareEnrollment(trackedEntity, program, occurredDate, organisationUnit, enrollmentUid);
    manager.save(enrollment);
    trackerOwnershipAccessManager.assignOwnership(
        trackedEntity, program, organisationUnit, true, true);
    eventPublisher.publishEvent(new ProgramEnrollmentNotificationEvent(this, enrollment.getId()));
    manager.update(enrollment);
    manager.update(trackedEntity);

    return enrollment;
  }

  private Enrollment prepareEnrollment(
      TrackedEntity trackedEntity,
      Program program,
      Date occurredDate,
      OrganisationUnit organisationUnit,
      String enrollmentUid) {
    if (program.getTrackedEntityType() != null
        && !program.getTrackedEntityType().equals(trackedEntity.getTrackedEntityType())) {
      throw new IllegalQueryException(
          "Tracked entity must have same tracked entity as program: " + program.getUid());
    }

    Enrollment enrollment = new Enrollment();
    enrollment.setUid(enrollmentUid);
    enrollment.setOrganisationUnit(organisationUnit);
    enrollment.enrollTrackedEntity(trackedEntity, program);
    enrollment.setEnrollmentDate(new Date());
    enrollment.setOccurredDate(requireNonNullElseGet(occurredDate, Date::new));
    enrollment.setStatus(EnrollmentStatus.ACTIVE);

    return enrollment;
  }
}
