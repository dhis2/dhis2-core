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
package org.hisp.dhis.tracker.validation.validator;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.validation.Reporter;
import org.hisp.dhis.tracker.validation.Validator;

/**
 * All is a {@link Validator} applying a sequence of {@link Validator}s to given input. All {@link
 * Validator}s are called irrespective of whether they added an error to {@link Reporter} or not.
 * Using {@link All} conveys that the {@link Validator}s are independent. Use {@link Seq} if you
 * want to express a dependency between {@link Validator}s.
 *
 * <p>Note: in theory {@link #validators} could run concurrently. Right now they are run
 * sequentially.
 *
 * @param <T> type of input to be validated
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class All<T> implements Validator<T> {
  private final List<Validator<T>> validators;

  public static <T> All<T> all(List<Validator<T>> validators) {
    return new All<>(validators);
  }

  public static <T> All<T> all(Validator<T> v1) {
    return new All<>(List.of(v1));
  }

  public static <T> All<T> all(Validator<T> v1, Validator<T> v2) {
    return new All<>(List.of(v1, v2));
  }

  public static <T> All<T> all(Validator<T> v1, Validator<T> v2, Validator<T> v3) {
    return new All<>(List.of(v1, v2, v3));
  }

  public static <T> All<T> all(Validator<T> v1, Validator<T> v2, Validator<T> v3, Validator<T> v4) {
    return new All<>(List.of(v1, v2, v3, v4));
  }

  public static <T> All<T> all(
      Validator<T> v1, Validator<T> v2, Validator<T> v3, Validator<T> v4, Validator<T> v5) {
    return new All<>(List.of(v1, v2, v3, v4, v5));
  }

  public static <T> All<T> all(
      Validator<T> v1,
      Validator<T> v2,
      Validator<T> v3,
      Validator<T> v4,
      Validator<T> v5,
      Validator<T> v6) {
    return new All<>(List.of(v1, v2, v3, v4, v5, v6));
  }

  public static <T> All<T> all(
      Validator<T> v1,
      Validator<T> v2,
      Validator<T> v3,
      Validator<T> v4,
      Validator<T> v5,
      Validator<T> v6,
      Validator<T> v7) {
    return new All<>(List.of(v1, v2, v3, v4, v5, v6, v7));
  }

  public static <T> All<T> all(
      Validator<T> v1,
      Validator<T> v2,
      Validator<T> v3,
      Validator<T> v4,
      Validator<T> v5,
      Validator<T> v6,
      Validator<T> v7,
      Validator<T> v8) {
    return new All<>(List.of(v1, v2, v3, v4, v5, v6, v7, v8));
  }

  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, T input) {
    for (Validator<T> validator : validators) {
      if ((input instanceof TrackerDto
              && !validator.needsToRun(bundle.getStrategy((TrackerDto) input)))
          || (!(input instanceof TrackerDto)
              && !validator.needsToRun(bundle.getImportStrategy()))) {
        continue;
      }

      validator.validate(reporter, bundle, input);
    }
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return true; // All is used to compose other Validators, so it should always run
  }
}
