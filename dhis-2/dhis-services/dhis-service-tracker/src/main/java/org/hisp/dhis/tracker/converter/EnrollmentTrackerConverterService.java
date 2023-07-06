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
package org.hisp.dhis.tracker.converter;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
public class EnrollmentTrackerConverterService
    implements RuleEngineConverterService<Enrollment, ProgramInstance> {
  private final NotesConverterService notesConverterService;

  public EnrollmentTrackerConverterService(NotesConverterService notesConverterService) {
    checkNotNull(notesConverterService);

    this.notesConverterService = notesConverterService;
  }

  @Override
  public Enrollment to(ProgramInstance programInstance) {
    List<Enrollment> enrollments = to(Collections.singletonList(programInstance));

    if (enrollments.isEmpty()) {
      return null;
    }

    return enrollments.get(0);
  }

  @Override
  public List<Enrollment> to(List<ProgramInstance> programInstances) {
    List<Enrollment> enrollments = new ArrayList<>();

    programInstances.forEach(
        tei -> {
          // TODO: Add implementation
        });

    return enrollments;
  }

  @Override
  public ProgramInstance from(TrackerPreheat preheat, Enrollment enrollment) {
    ProgramInstance programInstance = preheat.getEnrollment(enrollment.getEnrollment());
    return from(preheat, enrollment, programInstance);
  }

  @Override
  public List<ProgramInstance> from(TrackerPreheat preheat, List<Enrollment> enrollments) {
    return enrollments.stream()
        .map(enrollment -> from(preheat, enrollment))
        .collect(Collectors.toList());
  }

  @Override
  public ProgramInstance fromForRuleEngine(TrackerPreheat preheat, Enrollment enrollment) {
    return from(preheat, enrollment, null);
  }

  private ProgramInstance from(
      TrackerPreheat preheat, Enrollment enrollment, ProgramInstance programInstance) {
    OrganisationUnit organisationUnit = preheat.getOrganisationUnit(enrollment.getOrgUnit());

    checkNotNull(organisationUnit, TrackerImporterAssertErrors.ORGANISATION_UNIT_CANT_BE_NULL);

    Program program = preheat.getProgram(enrollment.getProgram());

    checkNotNull(program, TrackerImporterAssertErrors.PROGRAM_CANT_BE_NULL);

    TrackedEntityInstance trackedEntityInstance =
        preheat.getTrackedEntity(enrollment.getTrackedEntity());

    Date now = new Date();

    if (isNewEntity(programInstance)) {
      programInstance = new ProgramInstance();
      programInstance.setUid(
          !StringUtils.isEmpty(enrollment.getEnrollment())
              ? enrollment.getEnrollment()
              : enrollment.getUid());
      programInstance.setCreated(now);
      programInstance.setStoredBy(enrollment.getStoredBy());
      programInstance.setCreatedByUserInfo(UserInfoSnapshot.from(preheat.getUser()));
    }

    programInstance.setLastUpdated(now);
    programInstance.setLastUpdatedByUserInfo(UserInfoSnapshot.from(preheat.getUser()));
    programInstance.setDeleted(false);
    programInstance.setCreatedAtClient(DateUtils.fromInstant(enrollment.getCreatedAtClient()));
    programInstance.setLastUpdatedAtClient(DateUtils.fromInstant(enrollment.getUpdatedAtClient()));

    Date enrollmentDate = DateUtils.fromInstant(enrollment.getEnrolledAt());
    Date incidentDate = DateUtils.fromInstant(enrollment.getOccurredAt());

    programInstance.setEnrollmentDate(enrollmentDate);
    programInstance.setIncidentDate(incidentDate != null ? incidentDate : enrollmentDate);
    programInstance.setOrganisationUnit(organisationUnit);
    programInstance.setProgram(program);
    programInstance.setEntityInstance(trackedEntityInstance);
    programInstance.setFollowup(enrollment.isFollowUp());
    programInstance.setGeometry(enrollment.getGeometry());

    if (enrollment.getStatus() == null) {
      enrollment.setStatus(EnrollmentStatus.ACTIVE);
    }

    ProgramStatus previousStatus = programInstance.getStatus();
    programInstance.setStatus(enrollment.getStatus().getProgramStatus());

    if (!Objects.equal(previousStatus, programInstance.getStatus())) {
      if (programInstance.isCompleted()) {
        programInstance.setEndDate(new Date());
        programInstance.setCompletedBy(preheat.getUsername());
      } else if (programInstance.getStatus().equals(ProgramStatus.CANCELLED)) {
        programInstance.setEndDate(new Date());
      }
    }

    if (isNotEmpty(enrollment.getNotes())) {
      programInstance
          .getComments()
          .addAll(notesConverterService.from(preheat, enrollment.getNotes()));
    }
    return programInstance;
  }
}
