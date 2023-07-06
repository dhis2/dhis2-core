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
package org.hisp.dhis.tracker;

import static org.hisp.dhis.tracker.Assertions.assertHasError;
import static org.hisp.dhis.tracker.Assertions.assertHasErrors;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.hisp.dhis.tracker.report.Error;
import org.hisp.dhis.tracker.report.ValidationReport;
import org.hisp.dhis.tracker.validation.ValidationCode;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link Assertions}. Uncomment the commented assertions in each test if you want to
 * iterate on the assertion messages. They are a copy of the {@link
 * org.junit.jupiter.api.function.Executable} in the <code>assertThrows</code> statements.
 */
public class AssertionsTest {
  @Test
  void testAssertHasOnlyErrorsFailsIfReportHasNoErrors() {
    ValidationReport report = ValidationReport.emptyReport();

    assertThrows(AssertionError.class, () -> assertHasOnlyErrors(report, ValidationCode.E1000));
  }

  @Test
  void testAssertHasOnlyErrorsFailsIfReportHasMoreErrors() {

    ValidationReport report =
        new ValidationReport(
            List.of(
                Error.builder().errorCode(ValidationCode.E1000.name()).build(),
                Error.builder().errorCode(ValidationCode.E1019.name()).build(),
                Error.builder().errorCode(ValidationCode.E1041.name()).build()),
            List.of());

    assertThrows(
        AssertionError.class,
        () -> assertHasOnlyErrors(report, ValidationCode.E1000, ValidationCode.E1019));
  }

  @Test
  void testAssertHasOnlyErrorsFailsIfReportHasLessErrors() {

    ValidationReport report =
        new ValidationReport(
            List.of(Error.builder().errorCode(ValidationCode.E1000.name()).build()), List.of());

    assertThrows(
        AssertionError.class,
        () -> assertHasOnlyErrors(report, ValidationCode.E1000, ValidationCode.E1003));
  }

  @Test
  void testAssertHasOnlyErrorsSucceeds() {

    ValidationReport report =
        new ValidationReport(
            List.of(Error.builder().errorCode(ValidationCode.E1000.name()).build()), List.of());

    assertHasOnlyErrors(report, ValidationCode.E1000);
  }

  @Test
  void testAssertHasErrorsFailsIfReportHasLessErrors() {
    ValidationReport report =
        new ValidationReport(
            List.of(
                Error.builder().errorCode(ValidationCode.E1000.name()).build(),
                Error.builder().errorCode(ValidationCode.E1001.name()).build()),
            List.of());

    assertThrows(AssertionError.class, () -> assertHasErrors(report, 3, ValidationCode.E1000));
    // assertHasErrors( report,3, ValidationCode.E1000 );
  }

  @Test
  void testAssertHasErrorsSucceeds() {
    ValidationReport report =
        new ValidationReport(
            List.of(
                Error.builder().errorCode(ValidationCode.E1000.name()).build(),
                Error.builder().errorCode(ValidationCode.E1001.name()).build()),
            List.of());

    assertHasErrors(report, 2, ValidationCode.E1000);
  }

  @Test
  void testAssertHasErrorFailsIfReportDoesNotHaveError() {

    ValidationReport report =
        new ValidationReport(
            List.of(
                Error.builder().errorCode(ValidationCode.E1000.name()).build(),
                Error.builder().errorCode(ValidationCode.E1041.name()).build()),
            List.of());

    assertThrows(AssertionError.class, () -> assertHasError(report, ValidationCode.E1019));
  }

  @Test
  void testAssertHasErrorFailsIfReportDoesNotHaveErrors() {
    ValidationReport report = ValidationReport.emptyReport();

    assertThrows(AssertionError.class, () -> assertHasError(report, ValidationCode.E1019));
    // assertHasError( report, ValidationCode.E1019 );
  }

  @Test
  void testAssertHasErrorSucceeds() {

    ValidationReport report =
        new ValidationReport(
            List.of(Error.builder().errorCode(ValidationCode.E1000.name()).build()), List.of());

    assertHasError(report, ValidationCode.E1000);
  }
}
