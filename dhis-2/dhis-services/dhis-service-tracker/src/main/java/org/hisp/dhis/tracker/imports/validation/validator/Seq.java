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

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.TrackerType;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.validation.Error;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.hisp.dhis.tracker.imports.validation.Warning;

/**
 * Seq as in sequence is a {@link Validator} applying a sequence of validators to given input in
 * order until a {@link Validator} fails i.e. adds an error to {@link Reporter}. Using {@link Seq}
 * conveys that the {@link Validator}s are dependent on each other. Use {@link All} if you want to
 * express independence between {@link Validator}s.
 *
 * @param <T> type of input to be validated
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Seq<T> implements Validator<T> {

  private final List<Validator<T>> validators;

  public static <T> Seq<T> seq(List<Validator<T>> validators) {
    return new Seq<>(validators);
  }

  public static <T> Seq<T> seq(Validator<T> v1) {
    return new Seq<>(List.of(v1));
  }

  public static <T> Seq<T> seq(Validator<T> v1, Validator<T> v2) {
    return new Seq<>(List.of(v1, v2));
  }

  public static <T> Seq<T> seq(Validator<T> v1, Validator<T> v2, Validator<T> v3) {
    return new Seq<>(List.of(v1, v2, v3));
  }

  public static <T> Seq<T> seq(Validator<T> v1, Validator<T> v2, Validator<T> v3, Validator<T> v4) {
    return new Seq<>(List.of(v1, v2, v3, v4));
  }

  public static <T> Seq<T> seq(
      Validator<T> v1, Validator<T> v2, Validator<T> v3, Validator<T> v4, Validator<T> v5) {
    return new Seq<>(List.of(v1, v2, v3, v4, v5));
  }

  public static <T> Seq<T> seq(
      Validator<T> v1,
      Validator<T> v2,
      Validator<T> v3,
      Validator<T> v4,
      Validator<T> v5,
      Validator<T> v6) {
    return new Seq<>(List.of(v1, v2, v3, v4, v5, v6));
  }

  public static <T> Seq<T> seq(
      Validator<T> v1,
      Validator<T> v2,
      Validator<T> v3,
      Validator<T> v4,
      Validator<T> v5,
      Validator<T> v6,
      Validator<T> v7) {
    return new Seq<>(List.of(v1, v2, v3, v4, v5, v6, v7));
  }

  public static <T> Seq<T> seq(
      Validator<T> v1,
      Validator<T> v2,
      Validator<T> v3,
      Validator<T> v4,
      Validator<T> v5,
      Validator<T> v6,
      Validator<T> v7,
      Validator<T> v8) {
    return new Seq<>(List.of(v1, v2, v3, v4, v5, v6, v7, v8));
  }

  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, T input) {
    WrappingReporter wrappedReporter = new WrappingReporter(reporter);

    for (Validator<T> validator : validators) {
      if ((input instanceof TrackerDto
              && !validator.needsToRun(bundle.getStrategy((TrackerDto) input)))
          || (!(input instanceof TrackerDto)
              && !validator.needsToRun(bundle.getImportStrategy()))) {
        continue;
      }

      validator.validate(wrappedReporter, bundle, input);

      if (wrappedReporter.validationFailed) {
        return; // only apply next validator if previous one was successful
      }
    }
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return true; // Seq is used to compose other Validators, so it should always run
  }

  /**
   * WrappingReporter wraps and delegates to a {@link Reporter} to capture whether a {@link
   * Validator} added an error. This is needed to know when to stop executing the {@link Validator}
   * sequence.
   */
  private static class WrappingReporter extends Reporter {

    private final Reporter original;

    private boolean validationFailed = false;

    WrappingReporter(Reporter original) {
      super(original.getIdSchemes(), false);
      this.original = original;
    }

    @Override
    public boolean hasErrors() {
      return original.hasErrors();
    }

    @Override
    public boolean hasErrorReport(Predicate<Error> test) {
      return original.hasErrorReport(test);
    }

    @Override
    public boolean addErrorIf(
        BooleanSupplier expression, TrackerDto dto, ValidationCode code, Object... args) {
      boolean failed = original.addErrorIf(expression, dto, code, args);
      if (!validationFailed) {
        validationFailed = failed;
      }
      return validationFailed;
    }

    @Override
    public boolean addErrorIfNull(
        Object object, TrackerDto dto, ValidationCode code, Object... args) {
      boolean failed = original.addErrorIfNull(object, dto, code, args);
      if (!validationFailed) {
        validationFailed = failed;
      }
      return validationFailed;
    }

    @Override
    public boolean addError(TrackerDto dto, ValidationCode code, Object... args) {
      validationFailed = original.addError(dto, code, args);
      return validationFailed;
    }

    @Override
    public boolean addError(Error error) {
      validationFailed = original.addError(error);
      return validationFailed;
    }

    @Override
    public boolean hasWarnings() {
      return original.hasWarnings();
    }

    @Override
    public boolean hasWarningReport(Predicate<Warning> test) {
      return original.hasWarningReport(test);
    }

    @Override
    public void addWarningIf(
        BooleanSupplier expression, TrackerDto dto, ValidationCode code, Object... args) {
      original.addWarningIf(expression, dto, code, args);
    }

    @Override
    public void addWarning(TrackerDto dto, ValidationCode code, Object... args) {
      original.addWarning(dto, code, args);
    }

    @Override
    public void addWarning(Warning warning) {
      original.addWarning(warning);
    }

    @Override
    public boolean isInvalid(TrackerDto dto) {
      return original.isInvalid(dto);
    }

    @Override
    public boolean isInvalid(TrackerType trackerType, String uid) {
      return original.isInvalid(trackerType, uid);
    }
  }
}
