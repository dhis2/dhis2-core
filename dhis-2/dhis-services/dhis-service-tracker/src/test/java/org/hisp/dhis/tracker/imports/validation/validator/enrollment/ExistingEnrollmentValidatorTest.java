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
package org.hisp.dhis.tracker.imports.validation.validator.enrollment;

import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1015;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1016;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
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

  private static final String PROGRAM_UID = "program";

  private static final UID TRACKED_ENTITY_UID = UID.generate();

  private static final UID ENROLLMENT_UID = UID.generate();

  private static final UID ANOTHER_ENROLLMENT_UID = UID.generate();

  @BeforeEach
  public void setUp() {
    validator = new ExistingEnrollmentValidator();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(enrollment.getProgram()).thenReturn(MetadataIdentifier.ofUid(PROGRAM_UID));
    when(enrollment.getTrackedEntity()).thenReturn(TRACKED_ENTITY_UID);
    when(enrollment.getStatus()).thenReturn(EnrollmentStatus.ACTIVE);
    when(enrollment.getEnrollment()).thenReturn(ENROLLMENT_UID);
    when(enrollment.getUid()).thenReturn(ENROLLMENT_UID);
    when(enrollment.getTrackerType()).thenCallRealMethod();

    when(preheat.getTrackedEntity(TRACKED_ENTITY_UID)).thenReturn(trackedEntity);
    when(trackedEntity.getUid()).thenReturn(TRACKED_ENTITY_UID.getValue());

    Program program = new Program();
    program.setOnlyEnrollOnce(false);
    program.setUid(PROGRAM_UID);

    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(program);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  @Test
  void shouldExitCancelledStatus() {
    when(enrollment.getStatus()).thenReturn(EnrollmentStatus.CANCELLED);
    validator.validate(reporter, bundle, enrollment);

    verify(preheat, times(0)).getProgram(PROGRAM_UID);
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
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(program);
    when(enrollment.getStatus()).thenReturn(EnrollmentStatus.COMPLETED);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassValidation() {
    Program program = new Program();
    program.setOnlyEnrollOnce(true);

    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(program);

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
    program.setUid(PROGRAM_UID);
    program.setOnlyEnrollOnce(true);

    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(program);
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
    setTeInDb();

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1015);
  }

  @Test
  void shouldFailNotActiveEnrollmentAlreadyInDbAndEnrollOnce() {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setOnlyEnrollOnce(true);

    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(program);
    setTeInDb(EnrollmentStatus.COMPLETED);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1016);
  }

  @Test
  void shouldPassNotActiveEnrollmentAlreadyInDbAndNotEnrollOnce() {
    setTeInDb(EnrollmentStatus.COMPLETED);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailAnotherEnrollmentAndEnrollOnce() {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setOnlyEnrollOnce(true);

    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(program);
    setEnrollmentInPayload(EnrollmentStatus.COMPLETED);
    setTeInDb();

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1016);
  }

  @Test
  void shouldPassWhenAnotherEnrollmentAndNotEnrollOnce() {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setOnlyEnrollOnce(false);

    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(program);
    setEnrollmentInPayload(EnrollmentStatus.COMPLETED);
    setTeInDb();

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  private void setTeInDb() {
    setTeInDb(EnrollmentStatus.ACTIVE);
  }

  private void setTeInDb(EnrollmentStatus status) {
    when(preheat.getTrackedEntityToEnrollmentMap())
        .thenReturn(
            new HashMap<>() {
              {
                Enrollment enrollment = new Enrollment();

                Program program = new Program();
                program.setUid(PROGRAM_UID);

                enrollment.setUid(ANOTHER_ENROLLMENT_UID.getValue());
                enrollment.setStatus(status);
                enrollment.setProgram(program);

                put(TRACKED_ENTITY_UID, Collections.singletonList(enrollment));
              }
            });
  }

  private void setEnrollmentInPayload(EnrollmentStatus status) {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollmentInBundle =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(ANOTHER_ENROLLMENT_UID)
            .program(MetadataIdentifier.ofUid(PROGRAM_UID))
            .trackedEntity(TRACKED_ENTITY_UID)
            .status(status)
            .build();

    when(bundle.getEnrollments()).thenReturn(Collections.singletonList(enrollmentInBundle));
  }
}
