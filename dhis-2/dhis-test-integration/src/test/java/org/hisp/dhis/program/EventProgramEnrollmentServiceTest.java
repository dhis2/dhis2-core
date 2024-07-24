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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class EventProgramEnrollmentServiceTest extends PostgresIntegrationTestBase {

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private EventProgramEnrollmentService eventProgramEnrollmentService;

  @Autowired private ProgramService programService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private TrackedEntityService trackedEntityService;

  private Program programA;

  private Program programB;

  private Enrollment enrollmentA;

  private Enrollment enrollmentB;

  private Enrollment enrollmentC;

  @BeforeEach
  void setUp() {
    OrganisationUnit organisationUnitA = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnitA);
    OrganisationUnit organisationUnitB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(organisationUnitB);

    TrackedEntity trackedEntityA = createTrackedEntity(organisationUnitA);
    trackedEntityService.addTrackedEntity(trackedEntityA);
    TrackedEntity trackedEntityB = createTrackedEntity(organisationUnitB);
    trackedEntityService.addTrackedEntity(trackedEntityB);

    programA = createProgram('A', new HashSet<>(), organisationUnitA);
    programService.addProgram(programA);
    programA.setSharing(Sharing.builder().publicAccess("rwrw----").build());
    programService.updateProgram(programA);

    programB = createProgram('B', new HashSet<>(), organisationUnitA);
    programService.addProgram(programB);

    enrollmentA = createEnrollment(programA, trackedEntityA, organisationUnitA);
    manager.save(enrollmentA);

    enrollmentB = createEnrollment(programB, trackedEntityA, organisationUnitB);
    manager.save(enrollmentB);

    enrollmentC = createEnrollment(programA, trackedEntityA, organisationUnitB);
    manager.save(enrollmentC);

    User user =
        createAndAddUser(
            false, "user", Set.of(organisationUnitA), Set.of(organisationUnitA), "F_EXPORT_DATA");
    user.setTeiSearchOrganisationUnits(Set.of(organisationUnitA, organisationUnitB));
    user.setOrganisationUnits(Set.of(organisationUnitA));

    injectSecurityContextUser(user);
  }

  @Test
  void testGetEnrollmentsByProgram() {
    List<Enrollment> enrollments = eventProgramEnrollmentService.getEnrollments(programA);
    assertEquals(2, enrollments.size());
    assertTrue(enrollments.contains(enrollmentA));
    assertTrue(enrollments.contains(enrollmentC));

    enrollments = eventProgramEnrollmentService.getEnrollments(programB);
    assertEquals(1, enrollments.size());
    assertTrue(enrollments.contains(enrollmentB));
  }
}
