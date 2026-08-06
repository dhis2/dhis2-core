/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.analytics.trackedentity;

import static org.hisp.dhis.analytics.QueryKey.NO_VALUE;
import static org.hisp.dhis.feedback.ErrorCode.E7246;

import java.util.Collection;
import java.util.List;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorMessage;

/**
 * Validates that the reserved no-value keyword {@code D2__NOVALUE} is used only on option-set
 * dimensions in tracked entity query filters.
 */
public final class NoValueFilterValidator {
  private NoValueFilterValidator() {}

  /**
   * Throws an {@link IllegalQueryException} if any non-option-set dimension is filtered by the
   * reserved no-value keyword.
   *
   * @param params the parsed common params.
   */
  public static void validate(CommonParsedParams params) {
    for (DimensionIdentifier<DimensionParam> dimensionIdentifier :
        params.getDimensionIdentifiers()) {
      DimensionParam dimension = dimensionIdentifier.getDimension();
      boolean isOptionSet = dimension.isQueryItem() && dimension.getQueryItem().hasOptionSet();

      List<String> values =
          dimension.getItems().stream().flatMap(item -> item.getValues().stream()).toList();

      ErrorMessage error = validateValues(isOptionSet, values);

      if (error != null) {
        throw new IllegalQueryException(error);
      }
    }
  }

  /**
   * Returns an error if the reserved no-value keyword is used on a non-option-set dimension.
   *
   * @param isOptionSet whether the dimension is backed by an option set.
   * @param values the filter values.
   */
  static ErrorMessage validateValues(boolean isOptionSet, Collection<String> values) {
    if (!isOptionSet && values.contains(NO_VALUE)) {
      return new ErrorMessage(E7246, NO_VALUE);
    }

    return null;
  }
}
