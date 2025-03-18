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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import static org.hisp.dhis.program.ProgramType.WITHOUT_REGISTRATION;
import static org.hisp.dhis.program.ProgramType.WITH_REGISTRATION;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.test.random.BeanRandomizer;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class EventProgramEnrollmentSupplierTest extends TestBase {

  @InjectMocks private EventProgramEnrollmentSupplier supplier;

  @Mock private EntityManager entityManager;

  private List<Enrollment> enrollments;

  private Program programWithRegistration;

  private Program programWithoutRegistration;

  private TrackerObjects params;

  private final BeanRandomizer rnd = BeanRandomizer.create();

  private TrackerPreheat preheat;

  @BeforeEach
  public void setUp() {
    enrollments = rnd.objects(Enrollment.class, 2).toList();
    // set the OrgUnit parent to null to avoid recursive errors when mapping
    enrollments.forEach(p -> p.getOrganisationUnit().setParent(null));
    enrollments.forEach(p -> p.getTrackedEntity().getOrganisationUnit().setParent(null));

    programWithRegistration = createProgram('A');
    programWithRegistration.setProgramType(WITH_REGISTRATION);
    Enrollment enrollmentWithRegistration = enrollments.get(0);
    enrollmentWithRegistration.setProgram(programWithRegistration);

    programWithoutRegistration = createProgram('B');
    programWithoutRegistration.setProgramType(WITHOUT_REGISTRATION);
    Enrollment enrollmentWithoutRegistration = enrollments.get(1);
    enrollmentWithoutRegistration.setProgram(programWithoutRegistration);

    params = TrackerObjects.builder().build();
    preheat = new TrackerPreheat();
  }

  // TODO: MAS. Fix this test, it is failing because of the recursive mapping of the OrgUnit
  @Test
  @Disabled
  void verifySupplierWhenNoProgramsArePresent() {
    enrollments = rnd.objects(Enrollment.class, 1).toList();
    // set the OrgUnit parent to null to avoid recursive errors when mapping
    enrollments.forEach(p -> p.getOrganisationUnit().setParent(null));
    Enrollment enrollment = enrollments.get(0);
    enrollment.setProgram(programWithoutRegistration);

    this.supplier.preheatAdd(params, preheat);

    assertEnrollmentInPreheat(
        enrollment, preheat.getEnrollmentsWithoutRegistration(programWithoutRegistration.getUid()));
  }

  private void assertEnrollmentInPreheat(Enrollment expected, Enrollment actual) {
    assertEquals(expected.getUid(), actual.getUid());
    assertEquals(expected.getProgram().getUid(), actual.getProgram().getUid());
    assertEquals(actual, preheat.getEnrollment(UID.of(actual)));
  }
}
