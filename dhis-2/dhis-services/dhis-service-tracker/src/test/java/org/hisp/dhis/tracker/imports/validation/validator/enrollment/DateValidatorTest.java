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

import static java.time.Instant.now;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1020;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1021;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1023;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1025;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1052;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasNoError;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class DateValidatorTest {

  private DateValidator validator;

  @Mock private TrackerPreheat preheat;

  private TrackerBundle bundle;

  private Reporter reporter;

  @BeforeEach
  public void setUp() {
    validator = new DateValidator();

    bundle = TrackerBundle.builder().preheat(preheat).build();

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  @Test
  void testMandatoryDatesMustBePresent() {
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(UID.generate())
            .program(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .occurredAt(now())
            .build();

    when(preheat.getProgram(enrollment.getProgram())).thenReturn(new Program());

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1025);
  }

  @Test
  void testDatesMustNotBeInTheFuture() {
    final Instant dateInTheFuture = now().plus(Duration.ofDays(1));
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(UID.generate())
            .program(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .occurredAt(dateInTheFuture)
            .enrolledAt(dateInTheFuture)
            .build();

    when(preheat.getProgram(enrollment.getProgram())).thenReturn(new Program());

    validator.validate(reporter, bundle, enrollment);

    assertAll(
        () -> assertHasError(reporter, enrollment, E1020),
        () -> assertHasError(reporter, enrollment, E1021));
  }

  @Test
  void testDatesWithNoTimeZoneMustNotBeInTheFuture() {
    ZoneId systemZone = ZoneId.systemDefault();
    LocalDate tomorrow = LocalDate.now(systemZone).plusDays(1);
    Instant dateTomorrow = tomorrow.atStartOfDay(systemZone).toInstant();

    // Create enrollment with dates set to tomorrow
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(UID.generate())
            .program(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .occurredAt(dateTomorrow)
            .enrolledAt(dateTomorrow)
            .build();

    when(preheat.getProgram(enrollment.getProgram())).thenReturn(new Program());

    // Run validation
    validator.validate(reporter, bundle, enrollment);

    // Assert that the future dates are detected as errors
    assertAll(
        () -> assertHasError(reporter, enrollment, E1020), // enrolledAt in the future
        () -> assertHasError(reporter, enrollment, E1021) // occurredAt in the future
        );
  }

  @Test
  void testDatesShouldBeAllowedOnSameDayIfFutureDatesAreNotAllowed() {
    final Instant today = now().plus(Duration.ofMinutes(1));
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(UID.generate())
            .program(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .occurredAt(today)
            .enrolledAt(today)
            .build();

    when(preheat.getProgram(enrollment.getProgram())).thenReturn(new Program());

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void testDatesCanBeInTheFuture() {
    final Instant dateInTheFuture = now().plus(Duration.ofDays(2));
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(UID.generate())
            .program(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .occurredAt(dateInTheFuture)
            .enrolledAt(dateInTheFuture)
            .build();

    Program program = new Program();
    program.setSelectEnrollmentDatesInFuture(true);
    program.setSelectIncidentDatesInFuture(true);
    when(preheat.getProgram(enrollment.getProgram())).thenReturn(program);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void testFailOnMissingOccurredAtDate() {
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(UID.generate())
            .program(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .enrolledAt(now())
            .build();

    Program program = new Program();
    program.setDisplayIncidentDate(true);
    when(preheat.getProgram(enrollment.getProgram())).thenReturn(program);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1023);
  }

  @Test
  void shouldFailWhenCompletedAtIsPresentAndStatusIsNotCompleted() {
    Enrollment enrollment = new Enrollment();
    enrollment.setEnrollment(UID.generate());
    enrollment.setProgram(MetadataIdentifier.ofUid(CodeGenerator.generateUid()));
    enrollment.setOccurredAt(now());
    enrollment.setCompletedAt(now());
    enrollment.setStatus(EnrollmentStatus.ACTIVE);

    when(preheat.getProgram(enrollment.getProgram())).thenReturn(new Program());

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1052);
  }

  @ParameterizedTest
  @EnumSource(
      value = EnrollmentStatus.class,
      names = {"COMPLETED", "CANCELLED"})
  void shouldValidateWhenCompletedAtIsPresentAndStatusAcceptCompletedAt(EnrollmentStatus status) {
    Enrollment enrollment = new Enrollment();
    enrollment.setEnrollment(UID.generate());
    enrollment.setProgram(MetadataIdentifier.ofUid(CodeGenerator.generateUid()));
    enrollment.setOccurredAt(now());
    enrollment.setCompletedAt(now());
    enrollment.setStatus(status);

    when(preheat.getProgram(enrollment.getProgram())).thenReturn(new Program());

    validator.validate(reporter, bundle, enrollment);

    assertHasNoError(reporter, enrollment, E1052);
  }
}
