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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Supplier;
import org.hisp.dhis.tracker.report.ImportReport;
import org.hisp.dhis.tracker.report.Status;
import org.hisp.dhis.tracker.report.ValidationReport;
import org.hisp.dhis.tracker.validation.ValidationCode;
import org.junit.jupiter.api.function.Executable;

/**
 * Collection of tracker specific assertions to help in asserting for example validation errors.
 *
 * <p>While it might work to compose assertions that already use JUnit 5 assertAll <a href=
 * "https://junit.org/junit5/docs/5.8.2/api/org.junit.jupiter.api/org/junit/jupiter/api/Assertions.html#assertAll(java.lang.String,java.util.Collection)">...</a>
 * using assertAll its documentation does not mention that it is designed for this use. Keep that in
 * mind when using the assertions in this class in tests.
 *
 * <p>Note: some assertions are duplicates of AssertValidations. This is due to a constraint in our
 * dependencies. dhis-test-integration cannot use test scope dependency on dhis-service-tracker
 * otherwise it would not report coverage for tracker code. This means we do not have access to test
 * code within dhis-service-tracker. Moving the assertions into dhis-support-test would need a
 * dependency on tracker, which would make it not a generic test module. We will have to live with
 * the duplicated assertion code until we have a better solution.
 */
public class Assertions {
  /**
   * assertHasErrors asserts the report contains only errors of given codes in any order.
   *
   * @param report import report to be asserted on
   * @param codes expected error codes
   */
  public static void assertHasOnlyErrors(ImportReport report, ValidationCode... codes) {
    assertHasOnlyErrors(report.getValidationReport(), codes);
  }

  /**
   * assertHasErrors asserts the report contains only errors of given codes in any order.
   *
   * @param report validation report to be asserted on
   * @param codes expected error codes
   */
  public static void assertHasOnlyErrors(ValidationReport report, ValidationCode... codes) {
    assertHasErrors(report, codes.length, codes);
  }

  /**
   * assertHasWarnings asserts the report contains only given warnings in any order.
   *
   * @param report import report to be asserted on
   * @param codes expected warning codes
   */
  public static void assertHasOnlyWarnings(ImportReport report, ValidationCode... codes) {
    assertHasOnlyWarnings(report.getValidationReport(), codes);
  }

  /**
   * assertHasWarnings asserts the report contains only given warnings in any order.
   *
   * @param report validation report to be asserted on
   * @param codes expected warning codes
   */
  public static void assertHasOnlyWarnings(ValidationReport report, ValidationCode... codes) {
    assertHasWarnings(report, codes.length, codes);
  }

  /**
   * assertHasErrors asserts the report contains given count of errors and errors contain given
   * codes in any order.<br>
   * <em>NOTE:</em> prefer {@link #assertHasOnlyErrors(ImportReport, ValidationCode...)} or {@link
   * #assertHasErrors(ValidationReport, ValidationCode...)}. Rethink your test if you need this
   * assertion. If you want to make sure a certain number of errors are present, why do you not care
   * about what errors are present? The intention of an assertion like <code>
   * assertHasErrors(report, 13, ValidationCode.E1000);</code> is not clear.
   *
   * @param report import report to be asserted on
   * @param codes expected error codes
   */
  public static void assertHasErrors(ImportReport report, int count, ValidationCode... codes) {
    assertHasErrors(report.getValidationReport(), count, codes);
  }

  /**
   * assertHasErrors asserts the report contains given count of errors and errors contain given
   * codes in any order. <em>NOTE:</em> prefer {@link #assertHasOnlyErrors(ImportReport,
   * ValidationCode...)} or {@link #assertHasErrors(ValidationReport, ValidationCode...)}. Rethink
   * your test if you need this assertion. If you want to make sure a certain number of errors are
   * present, why do you not care about what errors are present? The intention of an assertion like
   * <code>assertHasErrors(report, 13, ValidationCode.E1000);</code> is not clear.
   *
   * @param report validation report to be asserted on
   * @param codes expected error codes
   */
  public static void assertHasErrors(ValidationReport report, int count, ValidationCode... codes) {
    assertTrue(report.hasErrors(), "error not found since report has no errors");
    ArrayList<Executable> executables = new ArrayList<>();
    executables.add(
        () ->
            assertEquals(
                count,
                report.getErrors().size(),
                String.format(
                    "mismatch in number of expected error(s), got %s", report.getErrors())));
    Arrays.stream(codes)
        .map(c -> ((Executable) () -> assertHasError(report, c)))
        .forEach(executables::add);
    assertAll("assertHasErrors", executables);
  }

  /**
   * assertHasWarnings asserts the report contains given count of warnings and warnings that contain
   * given code and that are linked to given tracker object in any order. <em>NOTE:</em> prefer
   * {@link #assertHasOnlyWarnings(ImportReport, ValidationCode...)} or {@link
   * #assertHasOnlyWarnings(ValidationReport, ValidationCode...)} .Rethink your test if you need
   * this assertion. If you want to make sure a certain number of warnings are present, why do you
   * not care about what warnings are present? The intention of an assertion like <code>
   * assertHasWarnings(report, 13, ValidationCode.E1000);</code> is not clear.
   *
   * @param report validation report to be asserted on
   * @param codes expected warning codes
   */
  public static void assertHasWarnings(
      ValidationReport report, int count, ValidationCode... codes) {
    assertTrue(report.hasWarnings(), "warning not found since report has no warnings");
    ArrayList<Executable> executables = new ArrayList<>();
    executables.add(
        () ->
            assertEquals(
                count,
                report.getWarnings().size(),
                String.format(
                    "mismatch in number of expected warning(s), got %s", report.getWarnings())));
    Arrays.stream(codes)
        .map(c -> ((Executable) () -> assertHasWarning(report, c)))
        .forEach(executables::add);
    assertAll("assertHasWarnings", executables);
  }

  /**
   * assertHasErrors asserts the report contains errors of given codes in any order.
   *
   * @param report validation report to be asserted on
   * @param codes expected error codes
   */
  public static void assertHasErrors(ValidationReport report, ValidationCode... codes) {
    assertTrue(report.hasErrors(), "error not found since report has no errors");
    ArrayList<Executable> executables = new ArrayList<>();
    Arrays.stream(codes)
        .map(c -> ((Executable) () -> assertHasError(report, c)))
        .forEach(executables::add);
    assertAll("assertHasErrors", executables);
  }

  public static void assertHasError(ImportReport report, ValidationCode code) {
    assertNotNull(report);
    assertAll(
        () ->
            assertEquals(
                Status.ERROR,
                report.getStatus(),
                errorMessage(
                    "Expected import with status OK, instead got:\n",
                    report.getValidationReport())),
        () -> assertHasError(report.getValidationReport(), code));
  }

  public static void assertHasError(ImportReport report, ValidationCode code, String entityUid) {
    assertNotNull(report);
    assertAll(
        () ->
            assertEquals(
                Status.ERROR,
                report.getStatus(),
                errorMessage(
                    "Expected import with status OK, instead got:\n",
                    report.getValidationReport())),
        () -> assertHasError(report.getValidationReport(), code, entityUid));
  }

  public static void assertHasWarning(ValidationReport report, ValidationCode code) {
    assertTrue(report.hasWarnings(), "warning not found since report has no warnings");
    assertTrue(
        report.hasWarning(w -> Objects.equals(code.name(), w.getWarningCode())),
        String.format(
            "warning with code %s not found in report with warning(s) %s",
            code, report.getWarnings()));
  }

  public static void assertHasError(ValidationReport report, ValidationCode code) {
    assertTrue(report.hasErrors(), "error not found since report has no errors");
    assertTrue(
        report.hasError(err -> Objects.equals(code.name(), err.getErrorCode())),
        String.format(
            "error with code %s not found in report with error(s) %s", code, report.getErrors()));
  }

  public static void assertHasError(
      ValidationReport report, ValidationCode code, String entityUid) {
    assertTrue(report.hasErrors(), "error not found since report has no errors");
    assertTrue(
        report.hasError(
            err ->
                Objects.equals(code.name(), err.getErrorCode())
                    && Objects.equals(entityUid, err.getUid())),
        String.format(
            "error with code %s for entity %s not found in report with error(s) %s",
            code, entityUid, report.getErrors()));
  }

  public static void assertNoErrorsAndNoWarnings(ImportReport report) {
    assertNotNull(report);
    assertAll(
        () ->
            assertEquals(
                Status.OK,
                report.getStatus(),
                errorMessage(
                    "Expected import with status OK, instead got:\n",
                    report.getValidationReport())),
        () ->
            assertEquals(
                Collections.emptyList(),
                report.getValidationReport().getWarnings(),
                "Expected import without warnings, instead got:\n"
                    + report.getValidationReport().getWarnings()));
  }

  public static void assertNoErrors(ImportReport report) {
    assertNotNull(report);
    assertEquals(
        Status.OK,
        report.getStatus(),
        errorMessage(
            "Expected import with status OK, instead got:\n", report.getValidationReport()));
  }

  public static void assertNoErrors(ValidationReport report) {
    assertNotNull(report);
    assertFalse(
        report.hasErrors(), errorMessage("Expected no validation errors, instead got:\n", report));
  }

  private static Supplier<String> errorMessage(String errorTitle, ValidationReport report) {
    return () -> {
      StringBuilder msg = new StringBuilder(errorTitle);
      report
          .getErrors()
          .forEach(
              e -> {
                msg.append(e.getErrorCode());
                msg.append(": ");
                msg.append(e.getMessage());
                msg.append('\n');
              });
      return msg.toString();
    };
  }
}
