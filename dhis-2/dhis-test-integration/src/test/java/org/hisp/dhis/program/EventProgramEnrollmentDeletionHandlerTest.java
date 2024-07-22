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
package org.hisp.dhis.program;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class EventProgramEnrollmentDeletionHandlerTest extends PostgresIntegrationTestBase {
  @Autowired ProgramService programService;
  @Autowired EnrollmentService enrollmentService;
  @Autowired IdentifiableObjectManager manager;

  private OrganisationUnit orgUnit;
  private TrackedEntity trackedEntity;

  @BeforeEach
  void setUp() {
    orgUnit = createOrganisationUnit('O');
    manager.save(orgUnit);
    trackedEntity = createTrackedEntity(orgUnit);
    manager.save(trackedEntity);
  }

  @Test
  void shouldDeleteEnrollmentsWhenDeletingEventProgram()
      throws ForbiddenException, NotFoundException {
    Program program = createProgramWithoutRegistration('P');
    programService.addProgram(program);
    Enrollment enrollment = createEnrollment(program);

    assertNotNull(enrollmentService.getEnrollment(enrollment.getUid()));

    programService.deleteProgram(program);

    assertThrows(
        NotFoundException.class, () -> enrollmentService.getEnrollment(enrollment.getUid()));
  }

  @Test
  void shouldNotDeleteEnrollmentsWhenDeletingTrackerProgram()
      throws ForbiddenException, NotFoundException {
    Program program = createProgram('P');
    programService.addProgram(program);
    Enrollment enrollment = createEnrollment(program);

    assertNotNull(enrollmentService.getEnrollment(enrollment.getUid()));
    assertThrows(DeleteNotAllowedException.class, () -> programService.deleteProgram(program));
  }

  private Enrollment createEnrollment(Program program) {
    Enrollment enrollment = createEnrollment(program, trackedEntity, orgUnit);
    manager.save(enrollment);

    return enrollment;
  }
}
