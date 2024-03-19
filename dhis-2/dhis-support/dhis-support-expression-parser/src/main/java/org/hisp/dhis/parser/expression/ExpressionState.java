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
package org.hisp.dhis.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.system.util.ValidationUtils;

/**
 * Current state of an expression during evaluation.
 *
 * <p>This class holds values that can change as an expression is evaluated. These values can affect
 * how parsing is done in a subtree, or how final results are computed.
 *
 * @author Jim Grace
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExpressionState {
  /** By default, replace nulls with a default value. */
  @Builder.Default private boolean replaceNulls = true;

  /** Item query modifiers, if any, in effect during parsing. */
  @Builder.Default private QueryModifiers queryMods = null;

  /** Current program stage offset in effect. */
  @Builder.Default private int stageOffset = Integer.MIN_VALUE;

  /** Flag to check if a null date was found. */
  @Builder.Default private boolean unprotectedNullDateFound = false;

  /** Count of dimension items found. */
  @Builder.Default private int itemsFound = 0;

  /** Count of dimension item values found. */
  @Builder.Default private int itemValuesFound = 0;

  /** True if we are currently within a subexpression. */
  @Builder.Default private boolean inSubexpression = false;

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /**
   * Returns a {@see QueryModifiersBuilder} that can be used to add modifiers to be be applied while
   * parsing. If there are no query modifiers at present, returns a fresh builder. If there are
   * query modifiers at present, returns a builder based on current modifiers.
   *
   * @return a {@see QueryModifiersBuilder}
   */
  public QueryModifiers.QueryModifiersBuilder getQueryModsBuilder() {
    return (queryMods == null) ? QueryModifiers.builder() : queryMods.toBuilder();
  }

  /**
   * Handles nulls and missing values.
   *
   * <p>If we should replace nulls with the default value, then do so, and remember how many items
   * found, and how many of them had values, for subsequent MissingValueStrategy analysis.
   *
   * <p>If we should not replace nulls with the default value, then don't, as this is likely for
   * some function that is testing for nulls, and a missing value should not count towards the
   * MissingValueStrategy.
   *
   * @param value the (possibly null) value.
   * @param valueType the type of value to substitute if null.
   * @return the value we should return.
   */
  public Object handleNulls(Object value, ValueType valueType) {
    if (replaceNulls) {
      itemsFound++;
      if (value == null && valueType.isDate()) {
        unprotectedNullDateFound = true;
        return null;
      } else if (value == null) {
        return ValidationUtils.getNullReplacementValue(valueType);
      } else {
        itemValuesFound++;
      }
    }

    return value;
  }
}
