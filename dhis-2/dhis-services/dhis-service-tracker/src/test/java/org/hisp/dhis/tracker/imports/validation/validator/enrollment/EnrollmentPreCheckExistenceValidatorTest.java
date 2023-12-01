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

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1080;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1081;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1113;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentPreCheckExistenceValidatorTest {
  private static final String SOFT_DELETED_ENROLLMENT_UID = "SoftDeletedEnrollmentId";

  private static final String ENROLLMENT_UID = "EnrollmentId";

  private static final String NOT_PRESENT_ENROLLMENT_UID = "NotPresentEnrollmentId";

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  private ExistenceValidator validator;

  private Reporter reporter;

  @BeforeEach
  void setUp() {
    when(bundle.getPreheat()).thenReturn(preheat);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);

    validator = new ExistenceValidator();
  }

  @Test
  void verifyEnrollmentValidationSuccessWhenIsCreateAndEnrollmentIsNotPresent() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(NOT_PRESENT_ENROLLMENT_UID)
            .build();
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyEnrollmentValidationSuccessWhenEnrollmentIsNotPresent() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(NOT_PRESENT_ENROLLMENT_UID)
            .build();
    when(bundle.getStrategy(any(org.hisp.dhis.tracker.imports.domain.Enrollment.class)))
        .thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyEnrollmentValidationSuccessWhenIsUpdate() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_UID)
            .build();
    when(preheat.getEnrollment(ENROLLMENT_UID)).thenReturn(getEnrollment());
    when(bundle.getStrategy(any(org.hisp.dhis.tracker.imports.domain.Enrollment.class)))
        .thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyEnrollmentValidationFailsWhenIsSoftDeleted() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(SOFT_DELETED_ENROLLMENT_UID)
            .build();
    when(preheat.getEnrollment(SOFT_DELETED_ENROLLMENT_UID)).thenReturn(getSoftDeletedEnrollment());
    when(bundle.getStrategy(any(org.hisp.dhis.tracker.imports.domain.Enrollment.class)))
        .thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1113);
  }

  @Test
  void verifyEnrollmentValidationFailsWhenIsCreateAndEnrollmentIsAlreadyPresent() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_UID)
            .build();
    when(preheat.getEnrollment(ENROLLMENT_UID)).thenReturn(getEnrollment());
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1080);
  }

  @Test
  void verifyEnrollmentValidationFailsWhenIsUpdateAndEnrollmentIsNotPresent() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(NOT_PRESENT_ENROLLMENT_UID)
            .build();
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.UPDATE);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1081);
  }

  private Enrollment getSoftDeletedEnrollment() {
    Enrollment enrollment = new Enrollment();
    enrollment.setUid(SOFT_DELETED_ENROLLMENT_UID);
    enrollment.setDeleted(true);
    return enrollment;
  }

  private Enrollment getEnrollment() {
    Enrollment enrollment = new Enrollment();
    enrollment.setUid(ENROLLMENT_UID);
    enrollment.setDeleted(false);
    return enrollment;
  }
}
