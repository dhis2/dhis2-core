/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.export.enrollment;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.hisp.dhis.program.Program;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnrollmentQueryParamsTest {
  private Program program;
  private List<Program> accessiblePrograms;
  private EnrollmentQueryParams params;

  @BeforeEach
  void setUp() {
    program = new Program();
    accessiblePrograms = List.of(program);
    params = new EnrollmentQueryParams();
  }

  @Test
  void shouldAddProgramIfAccessibleProgramsNotSet() {
    params.setEnrolledInTrackerProgram(program);
    assertEquals(program, params.getEnrolledInTrackerProgram());
    assertIsEmpty(params.getAccessibleTrackerPrograms());
  }

  @Test
  void shouldAddAccessibleProgramsIfProgramsNotSet() {
    params.setAccessibleTrackerPrograms(List.of(program));
    assertContainsOnly(accessiblePrograms, params.getAccessibleTrackerPrograms());
    assertNull(params.getEnrolledInTrackerProgram());
  }

  @Test
  void shouldFailWhenAddingAccessibleProgramsIfProgramAlreadySet() {
    params.setEnrolledInTrackerProgram(program);

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> params.setAccessibleTrackerPrograms(accessiblePrograms));
    assertEquals(
        "Cannot set 'accessibleTrackerPrograms' when 'enrolledInTrackerProgram' is already set.",
        exception.getMessage());
  }

  @Test
  void shouldFailWhenAddingProgramIfAccessibleProgramsAlreadySet() {
    params.setAccessibleTrackerPrograms(accessiblePrograms);

    Exception exception =
        assertThrows(
            IllegalArgumentException.class, () -> params.setEnrolledInTrackerProgram(program));
    assertEquals(
        "Cannot set 'enrolledInTrackerProgram' when 'accessibleTrackerPrograms' is already set.",
        exception.getMessage());
  }
}
