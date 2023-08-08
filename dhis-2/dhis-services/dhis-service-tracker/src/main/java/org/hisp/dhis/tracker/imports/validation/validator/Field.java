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

import java.util.function.Function;
import java.util.function.Predicate;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.Validator;

/**
 * Field adapts a {@link Validator} of type S to type T given a mapping {@link Function}.
 *
 * <p>Use it for example when you need a {@code Validator<TrackerBundle>} and want to validate every
 * {@code Event} in the bundles events. You would then write
 *
 * <pre>
 * field( TrackerBundle::getEvents, repeatedEventsValidator )
 * </pre>
 */
public class Field {

  private Field() {
    throw new IllegalStateException("Utility class");
  }

  public static <T, S> Validator<T> field(Function<T, S> map, Validator<S> validator) {
    return (reporter, bundle, input) -> {
      if ((input instanceof TrackerDto
              && !validator.needsToRun(bundle.getStrategy((TrackerDto) input)))
          || (!(input instanceof TrackerDto)
              && !validator.needsToRun(bundle.getImportStrategy()))) {
        return;
      }

      validator.validate(reporter, bundle, map.apply(input));
    };
  }

  /**
   * Field will create a {@link Validator} of type T out of a {@link Predicate} of type S. The input
   * to the returned {@code Validator} is of type T which is mapped using given {@code map} function
   * before applying the {@code Predicate} to it.
   *
   * <p>Note: the {@code validator} will always be executed irrespective of the {@link
   * org.hisp.dhis.tracker.imports.TrackerImportStrategy}.
   *
   * @param map function taking type T to type S
   * @param validator predicate testing input of type S
   * @param errorCode error code for error added to invalid input dto
   * @param errorMessageArgs args to be interpolated into the error codes message
   * @return validator of type T
   * @param <T> tracker dto to be validate
   * @param <S> input type of predicate
   */
  public static <T extends TrackerDto, S> Validator<T> field(
      Function<T, S> map,
      Predicate<S> validator,
      ValidationCode errorCode,
      Object... errorMessageArgs) {
    return (reporter, bundle, input) -> {
      if (!validator.test(map.apply(input))) {
        reporter.addError(input, errorCode, errorMessageArgs);
      }
    };
  }
}
