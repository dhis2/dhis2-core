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

import static org.hisp.dhis.program.EnrollmentStatus.ACTIVE;
import static org.hisp.dhis.program.ProgramType.WITHOUT_REGISTRATION;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventProgramEnrollmentServiceTest extends PostgresIntegrationTestBase {

  @Autowired private EventProgramEnrollmentService eventProgramEnrollmentService;

  @Autowired private IdentifiableObjectManager manager;

  private Program programA;

  private Program programB;

  private Program eventProgram;

  private Enrollment enrollmentA;

  private Enrollment enrollmentB;

  private Enrollment enrollmentC;

  private Enrollment eventProgramEnrollment;

  @BeforeEach
  void setUp() {
    programA = createProgram('A');
    manager.save(programA);
    programB = createProgram('B');
    manager.save(programB);
    eventProgram = createProgram('C');
    eventProgram.setProgramType(WITHOUT_REGISTRATION);
    manager.save(eventProgram);

    OrganisationUnit organisationUnitA = createOrganisationUnit('A');
    manager.save(organisationUnitA);
    OrganisationUnit organisationUnitB = createOrganisationUnit('B');
    manager.save(organisationUnitB);

    TrackedEntity trackedEntity = createTrackedEntity(organisationUnitA);
    manager.save(trackedEntity);

    enrollmentA = createEnrollment(programA, trackedEntity, organisationUnitA);
    manager.save(enrollmentA);
    enrollmentB = createEnrollment(programB, trackedEntity, organisationUnitB);
    manager.save(enrollmentB);
    enrollmentC = createEnrollment(programA, trackedEntity, organisationUnitB);
    manager.save(enrollmentC);
    eventProgramEnrollment = createEnrollment(eventProgram, trackedEntity, organisationUnitA);
    manager.save(eventProgramEnrollment);
  }

  @Test
  void testGetEnrollmentsByProgram() {
    List<Enrollment> enrollments = eventProgramEnrollmentService.getEnrollments(programA);
    assertContainsOnly(List.of(enrollmentA, enrollmentC), enrollments);

    enrollments = eventProgramEnrollmentService.getEnrollments(programB);
    assertContainsOnly(List.of(enrollmentB), enrollments);
  }

  @Test
  void shouldReturnEnrollmentsWhenGettingEnrollmentsOfAnEventProgram() {
    assertContainsOnly(
        List.of(eventProgramEnrollment),
        eventProgramEnrollmentService.getEnrollments(eventProgram, ACTIVE));
  }

  @Test
  void shouldReturnNoEnrollmentsWhenGettingEnrollmentsOfATrackerProgram() {
    assertEquals(enrollmentA, manager.get(Enrollment.class, enrollmentA.getUid()));
    assertIsEmpty(eventProgramEnrollmentService.getEnrollments(programA, ACTIVE));
  }
}
