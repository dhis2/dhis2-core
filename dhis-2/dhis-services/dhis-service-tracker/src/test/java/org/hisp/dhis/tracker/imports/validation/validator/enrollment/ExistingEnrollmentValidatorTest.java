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
package org.hisp.dhis.tracker.imports.validation.validator.enrollment;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1015;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1016;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ExistingEnrollmentValidatorTest {

  private ExistingEnrollmentValidator validator;

  @Mock org.hisp.dhis.tracker.imports.domain.Enrollment enrollment;

  @Mock TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @Mock private TrackedEntity trackedEntity;

  private Reporter reporter;

  private static final String programUid = "program";

  private static final String trackedEntitUid = "trackedEntity";

  private static final String enrollmentUid = "enrollment";

  @BeforeEach
  public void setUp() {
    validator = new ExistingEnrollmentValidator();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(enrollment.getProgram()).thenReturn(MetadataIdentifier.ofUid(programUid));
    when(enrollment.getTrackedEntity()).thenReturn(trackedEntitUid);
    when(enrollment.getStatus()).thenReturn(EnrollmentStatus.ACTIVE);
    when(enrollment.getEnrollment()).thenReturn(enrollmentUid);
    when(enrollment.getUid()).thenReturn(enrollmentUid);
    when(enrollment.getTrackerType()).thenCallRealMethod();

    when(preheat.getTrackedEntity(trackedEntitUid)).thenReturn(trackedEntity);
    when(trackedEntity.getUid()).thenReturn(trackedEntitUid);

    Program program = new Program();
    program.setOnlyEnrollOnce(false);
    program.setUid(programUid);

    when(preheat.getProgram(MetadataIdentifier.ofUid(programUid))).thenReturn(program);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  @Test
  void shouldExitCancelledStatus() {
    when(enrollment.getStatus()).thenReturn(EnrollmentStatus.CANCELLED);
    validator.validate(reporter, bundle, enrollment);

    verify(preheat, times(0)).getProgram(programUid);
  }

  @Test
  void shouldThrowProgramNotFound() {
    when(enrollment.getProgram()).thenReturn(null);
    when(preheat.getProgram((MetadataIdentifier) null)).thenReturn(null);

    assertThrows(
        NullPointerException.class, () -> validator.validate(reporter, bundle, enrollment));
  }

  @Test
  void shouldExitProgramOnlyEnrollOnce() {
    Program program = new Program();
    program.setOnlyEnrollOnce(false);
    when(preheat.getProgram(MetadataIdentifier.ofUid(programUid))).thenReturn(program);
    when(enrollment.getStatus()).thenReturn(EnrollmentStatus.COMPLETED);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldThrowTrackedEntityNotFound() {
    Program program = new Program();
    program.setOnlyEnrollOnce(true);

    when(preheat.getProgram(MetadataIdentifier.ofUid(programUid))).thenReturn(program);

    when(enrollment.getTrackedEntity()).thenReturn(null);
    assertThrows(
        NullPointerException.class, () -> validator.validate(reporter, bundle, enrollment));
  }

  @Test
  void shouldPassValidation() {
    Program program = new Program();
    program.setOnlyEnrollOnce(true);

    when(preheat.getProgram(MetadataIdentifier.ofUid(programUid))).thenReturn(program);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailActiveEnrollmentAlreadyInPayload() {
    setEnrollmentInPayload(EnrollmentStatus.ACTIVE);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1015);
  }

  @Test
  void shouldFailNotActiveEnrollmentAlreadyInPayloadAndEnrollOnce() {
    Program program = new Program();
    program.setUid(programUid);
    program.setOnlyEnrollOnce(true);

    when(preheat.getProgram(MetadataIdentifier.ofUid(programUid))).thenReturn(program);
    setEnrollmentInPayload(EnrollmentStatus.COMPLETED);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1016);
  }

  @Test
  void shouldPassNotActiveEnrollmentAlreadyInPayloadAndNotEnrollOnce() {
    setEnrollmentInPayload(EnrollmentStatus.COMPLETED);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailActiveEnrollmentAlreadyInDb() {
    setTeiInDb();

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1015);
  }

  @Test
  void shouldFailNotActiveEnrollmentAlreadyInDbAndEnrollOnce() {
    Program program = new Program();
    program.setUid(programUid);
    program.setOnlyEnrollOnce(true);

    when(preheat.getProgram(MetadataIdentifier.ofUid(programUid))).thenReturn(program);
    setTeiInDb(ProgramStatus.COMPLETED);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1016);
  }

  @Test
  void shouldPassNotActiveEnrollmentAlreadyInDbAndNotEnrollOnce() {
    setTeiInDb(ProgramStatus.COMPLETED);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailAnotherEnrollmentAndEnrollOnce() {
    Program program = new Program();
    program.setUid(programUid);
    program.setOnlyEnrollOnce(true);

    when(preheat.getProgram(MetadataIdentifier.ofUid(programUid))).thenReturn(program);
    setEnrollmentInPayload(EnrollmentStatus.COMPLETED);
    setTeiInDb();

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1016);
  }

  @Test
  void shouldPassWhenAnotherEnrollmentAndNotEnrollOnce() {
    Program program = new Program();
    program.setUid(programUid);
    program.setOnlyEnrollOnce(false);

    when(preheat.getProgram(MetadataIdentifier.ofUid(programUid))).thenReturn(program);
    setEnrollmentInPayload(EnrollmentStatus.COMPLETED);
    setTeiInDb();

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  private void setTeiInDb() {
    setTeiInDb(ProgramStatus.ACTIVE);
  }

  private void setTeiInDb(ProgramStatus programStatus) {
    when(preheat.getTrackedEntityToEnrollmentMap())
        .thenReturn(
            new HashMap<>() {
              {
                Enrollment enrollment = new Enrollment();

                Program program = new Program();
                program.setUid(programUid);

                enrollment.setUid("another_enrollment");
                enrollment.setStatus(programStatus);
                enrollment.setProgram(program);

                put(trackedEntitUid, Collections.singletonList(enrollment));
              }
            });
  }

  private void setEnrollmentInPayload(EnrollmentStatus enrollmentStatus) {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollmentInBundle =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment("another_enrollment")
            .program(MetadataIdentifier.ofUid(programUid))
            .trackedEntity(trackedEntitUid)
            .status(enrollmentStatus)
            .build();

    when(bundle.getEnrollments()).thenReturn(Collections.singletonList(enrollmentInBundle));
  }
}
