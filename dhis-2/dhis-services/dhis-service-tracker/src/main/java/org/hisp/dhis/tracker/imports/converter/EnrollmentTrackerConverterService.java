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

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RequiredArgsConstructor
@Service
public class EnrollmentTrackerConverterService
    implements RuleEngineConverterService<
        org.hisp.dhis.tracker.imports.domain.Enrollment, Enrollment> {
  private final NotesConverterService notesConverterService;

  @Override
  public Enrollment from(
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.Enrollment enrollment,
      UserDetails user) {
    Enrollment preheatEnrollment = preheat.getEnrollment(enrollment.getEnrollment());
    return from(preheat, enrollment, preheatEnrollment, user);
  }

  @Override
  public List<Enrollment> from(
      TrackerPreheat preheat,
      List<org.hisp.dhis.tracker.imports.domain.Enrollment> enrollments,
      UserDetails user) {
    return enrollments.stream().map(enrollment -> from(preheat, enrollment, user)).toList();
  }

  @Override
  public Enrollment fromForRuleEngine(
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.Enrollment enrollment,
      UserDetails user) {
    Enrollment from = from(preheat, enrollment, null, user);
    Enrollment preheatEnrollment = preheat.getEnrollment(enrollment.getUid());
    if (preheatEnrollment != null) {
      from.setCreated(preheatEnrollment.getCreated());
    }
    return from;
  }

  private Enrollment from(
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.Enrollment enrollment,
      Enrollment dbEnrollment,
      UserDetails user) {
    OrganisationUnit organisationUnit = preheat.getOrganisationUnit(enrollment.getOrgUnit());

    Program program = preheat.getProgram(enrollment.getProgram());

    TrackedEntity trackedEntity = preheat.getTrackedEntity(enrollment.getTrackedEntity());

    Date now = new Date();

    if (isNewEntity(dbEnrollment)) {
      dbEnrollment = new Enrollment();
      dbEnrollment.setUid(
          !StringUtils.isEmpty(enrollment.getEnrollment())
              ? enrollment.getEnrollment()
              : enrollment.getUid());
      dbEnrollment.setCreated(now);
      dbEnrollment.setStoredBy(enrollment.getStoredBy());
      dbEnrollment.setCreatedByUserInfo(UserInfoSnapshot.from(user));
    }

    dbEnrollment.setLastUpdated(now);
    dbEnrollment.setLastUpdatedByUserInfo(UserInfoSnapshot.from(user));
    dbEnrollment.setDeleted(false);
    dbEnrollment.setCreatedAtClient(DateUtils.fromInstant(enrollment.getCreatedAtClient()));
    dbEnrollment.setLastUpdatedAtClient(DateUtils.fromInstant(enrollment.getUpdatedAtClient()));

    Date enrollmentDate = DateUtils.fromInstant(enrollment.getEnrolledAt());
    Date occurredDate = DateUtils.fromInstant(enrollment.getOccurredAt());

    dbEnrollment.setEnrollmentDate(enrollmentDate);
    dbEnrollment.setOccurredDate(occurredDate != null ? occurredDate : enrollmentDate);
    dbEnrollment.setOrganisationUnit(organisationUnit);
    dbEnrollment.setProgram(program);
    dbEnrollment.setTrackedEntity(trackedEntity);
    dbEnrollment.setFollowup(enrollment.isFollowUp());
    dbEnrollment.setGeometry(enrollment.getGeometry());

    EnrollmentStatus previousStatus = dbEnrollment.getStatus();
    dbEnrollment.setStatus(
        ObjectUtils.firstNonNull(enrollment.getStatus(), EnrollmentStatus.ACTIVE));

    if (previousStatus != dbEnrollment.getStatus()) {
      if (dbEnrollment.isCompleted()) {
        dbEnrollment.setCompletedDate(now);
        dbEnrollment.setCompletedBy(user.getUsername());
      } else if (EnrollmentStatus.CANCELLED == dbEnrollment.getStatus()) {
        dbEnrollment.setCompletedDate(now);
      }
    }

    if (isNotEmpty(enrollment.getNotes())) {
      dbEnrollment
          .getNotes()
          .addAll(notesConverterService.from(preheat, enrollment.getNotes(), user));
    }
    return dbEnrollment;
  }
}
