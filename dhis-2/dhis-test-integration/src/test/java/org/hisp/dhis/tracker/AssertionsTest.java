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

import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link Assertions}. Uncomment the commented assertions in each test if you want to
 * iterate on the assertion messages. They are a copy of the {@link
 * org.junit.jupiter.api.function.Executable} in the <code>assertThrows</code> statements.
 */
public class AssertionsTest {
  @Test
  void testAssertHasOnlyErrorsFailsIfReportHasNoErrors() {

    TrackerValidationReport report = new TrackerValidationReport();

    assertThrows(AssertionError.class, () -> assertHasOnlyErrors(report, TrackerErrorCode.E1000));
    // assertHasOnlyErrors(report, TrackerErrorCode.E1000);
  }

  @Test
  void testAssertHasOnlyErrorsFailsIfReportHasMoreErrors() {

    TrackerValidationReport report = new TrackerValidationReport();
    report.addError(TrackerErrorReport.builder().errorCode(TrackerErrorCode.E1000).build());
    report.addError(TrackerErrorReport.builder().errorCode(TrackerErrorCode.E1019).build());
    report.addError(TrackerErrorReport.builder().errorCode(TrackerErrorCode.E1041).build());

    assertThrows(
        AssertionError.class,
        () -> assertHasOnlyErrors(report, TrackerErrorCode.E1000, TrackerErrorCode.E1019));
    // assertHasOnlyErrors( report, TrackerErrorCode.E1000,
    // TrackerErrorCode.E1019 );
  }

  @Test
  void testAssertHasOnlyErrorsFailsIfReportHasLessErrors() {

    TrackerValidationReport report = new TrackerValidationReport();
    report.addError(TrackerErrorReport.builder().errorCode(TrackerErrorCode.E1000).build());

    assertThrows(
        AssertionError.class,
        () -> assertHasOnlyErrors(report, TrackerErrorCode.E1000, TrackerErrorCode.E1003));
    // assertHasOnlyErrors( report, TrackerErrorCode.E1000,
    // TrackerErrorCode.E1003 );
  }

  @Test
  void testAssertHasOnlyErrorsSucceeds() {

    TrackerValidationReport report = new TrackerValidationReport();
    report.addError(TrackerErrorReport.builder().errorCode(TrackerErrorCode.E1000).build());

    assertHasOnlyErrors(report, TrackerErrorCode.E1000);
  }

  @Test
  void testAssertHasErrorsFailsIfReportHasLessErrors() {

    TrackerValidationReport report = new TrackerValidationReport();
    report.addError(TrackerErrorReport.builder().errorCode(TrackerErrorCode.E1000).build());
    report.addError(TrackerErrorReport.builder().errorCode(TrackerErrorCode.E1001).build());

    assertThrows(AssertionError.class, () -> assertHasErrors(report, 3, TrackerErrorCode.E1000));
    // assertHasErrors( report,3, TrackerErrorCode.E1000 );
  }

  @Test
  void testAssertHasErrorsSucceeds() {

    TrackerValidationReport report = new TrackerValidationReport();
    report.addError(TrackerErrorReport.builder().errorCode(TrackerErrorCode.E1000).build());
    report.addError(TrackerErrorReport.builder().errorCode(TrackerErrorCode.E1001).build());

    assertHasErrors(report, 2, TrackerErrorCode.E1000);
  }

  @Test
  void testAssertHasErrorFailsIfReportDoesNotHaveError() {

    TrackerValidationReport report = new TrackerValidationReport();
    report.addError(TrackerErrorReport.builder().errorCode(TrackerErrorCode.E1000).build());
    report.addError(TrackerErrorReport.builder().errorCode(TrackerErrorCode.E1041).build());

    assertThrows(AssertionError.class, () -> assertHasError(report, TrackerErrorCode.E1019));
    // assertHasError( report, TrackerErrorCode.E1019 );
  }

  @Test
  void testAssertHasErrorFailsIfReportDoesNotHaveErrors() {

    TrackerValidationReport report = new TrackerValidationReport();

    assertThrows(AssertionError.class, () -> assertHasError(report, TrackerErrorCode.E1019));
    // assertHasError( report, TrackerErrorCode.E1019 );
  }

  @Test
  void testAssertHasErrorSucceeds() {

    TrackerValidationReport report = new TrackerValidationReport();
    report.addError(TrackerErrorReport.builder().errorCode(TrackerErrorCode.E1000).build());

    assertHasError(report, TrackerErrorCode.E1000);
  }
}
