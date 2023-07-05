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
package org.hisp.dhis.tracker.imports.validation.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import org.hisp.dhis.tracker.imports.TrackerType;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validation;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.ValidationResult;

/**
 * Assert that a {@link Reporter} or a {@link ValidationResult} contains a given {@link Validation}
 * error or warning for a specific {@link TrackerDto}.
 */
public class AssertValidations {
  private AssertValidations() {
    throw new IllegalStateException("utility class");
  }

  public static void assertMissingProperty(
      Reporter reporter, TrackerDto dto, ValidationCode code, String property) {
    assertHasError(
        reporter,
        dto,
        code,
        "Missing required " + dto.getTrackerType().getName() + " property: `" + property + "`.");
  }

  public static void assertHasError(Reporter reporter, TrackerDto dto, ValidationCode code) {
    assertHasError(reporter.getErrors(), dto, code);
  }

  public static void assertHasError(
      Reporter reporter, TrackerDto dto, ValidationCode code, String messageContains) {
    assertHasError(reporter.getErrors(), dto, code, messageContains);
  }

  public static void assertHasWarning(Reporter reporter, TrackerDto dto, ValidationCode code) {
    assertHasWarning(reporter.getWarnings(), dto, code);
  }

  public static void assertHasError(ValidationResult result, TrackerDto dto, ValidationCode code) {
    assertHasError(result.getErrors(), dto, code);
  }

  public static void assertHasWarning(
      ValidationResult result, TrackerDto dto, ValidationCode code) {
    assertHasWarning(result.getWarnings(), dto, code);
  }

  public static void assertHasError(
      Collection<? extends Validation> validations, TrackerDto dto, ValidationCode code) {
    assertHasValidation(validations, "error", dto, code);
  }

  public static void assertHasError(
      Collection<? extends Validation> validations,
      TrackerDto dto,
      ValidationCode code,
      String messageContains) {
    assertHasValidation(validations, "error", dto, code, messageContains);
  }

  public static void assertHasWarning(
      Collection<? extends Validation> validations, TrackerDto dto, ValidationCode code) {
    assertHasValidation(validations, "warning", dto, code);
  }

  private static void assertHasValidation(
      Collection<? extends Validation> validations,
      String validationType,
      TrackerDto dto,
      ValidationCode code) {
    TrackerType type = dto.getTrackerType();
    String uid = dto.getUid();
    assertFalse(
        validations.isEmpty(),
        validationType + " not found since " + validationType + "s is empty");
    assertTrue(
        validations.stream()
            .anyMatch(
                v ->
                    code.name().equals(v.getCode())
                        && type.name().equals(v.getType())
                        && uid.equals(v.getUid())),
        String.format(
            "%s with code %s for %s with uid %s not found in %s(s) %s",
            validationType, code, type.getName(), uid, validationType, validations));
  }

  private static void assertHasValidation(
      Collection<? extends Validation> validations,
      String validationType,
      TrackerDto dto,
      ValidationCode code,
      String messageContains) {
    TrackerType type = dto.getTrackerType();
    String uid = dto.getUid();
    assertFalse(
        validations.isEmpty(),
        validationType + " not found since " + validationType + "s is empty");
    assertTrue(
        validations.stream()
            .anyMatch(
                v ->
                    code.name().equals(v.getCode())
                        && type.name().equals(v.getType())
                        && uid.equals(v.getUid())
                        && v.getMessage().contains(messageContains)),
        String.format(
            "%s with code %s for %s with uid %s and (partial) message '%s' not found in %s(s) %s",
            validationType,
            code,
            type.getName(),
            uid,
            messageContains,
            validationType,
            validations));
  }
}
