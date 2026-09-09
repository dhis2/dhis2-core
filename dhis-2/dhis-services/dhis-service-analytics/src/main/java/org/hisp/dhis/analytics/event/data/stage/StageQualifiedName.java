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
package org.hisp.dhis.analytics.event.data.stage;

import static org.hisp.dhis.common.DimensionConstants.DIMENSION_IDENTIFIER_SEP;

import java.util.Optional;
import org.hisp.dhis.analytics.util.RepeatableStageParamsHelper;

/**
 * A request identifier of the form {@code <stagePrefix>.<suffix>}: a program stage, optionally
 * carrying a repeatable-stage offset such as {@code stageUid[-1]}, qualifying an item, dimension or
 * output column scoped to that stage.
 *
 * <p>Headers, dimensions and sort fields all use this shape. Parsing it here keeps the split rule
 * in one place: the separator is the last interior dot, with non-empty text on both sides.
 *
 * @param stagePrefix the text before the separator, offset included when present.
 * @param suffix the text after the separator.
 */
public record StageQualifiedName(String stagePrefix, String suffix) {

  /**
   * Parses a stage-qualified identifier.
   *
   * @return the parsed name, or empty when the identifier has no interior separator.
   */
  public static Optional<StageQualifiedName> parse(String identifier) {
    if (identifier == null) {
      return Optional.empty();
    }
    int dot = identifier.lastIndexOf(DIMENSION_IDENTIFIER_SEP);
    if (dot <= 0 || dot >= identifier.length() - 1) {
      return Optional.empty();
    }
    return Optional.of(
        new StageQualifiedName(identifier.substring(0, dot), identifier.substring(dot + 1)));
  }

  /** Returns whether the identifier has the {@code <stagePrefix>.<suffix>} shape. */
  public static boolean isStageQualified(String identifier) {
    return parse(identifier).isPresent();
  }

  /** Returns whether the stage prefix carries a repeatable-stage offset such as {@code [-1]}. */
  public boolean hasRepeatableStageOffset() {
    return !RepeatableStageParamsHelper.getRepeatableStageParams(stagePrefix).isDefaultObject();
  }

  /** Returns the same stage prefix qualifying a different suffix. */
  public StageQualifiedName withSuffix(String newSuffix) {
    return new StageQualifiedName(stagePrefix, newSuffix);
  }

  @Override
  public String toString() {
    return stagePrefix + DIMENSION_IDENTIFIER_SEP + suffix;
  }
}
