/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.common.params.dimension;

import java.util.Map;

/**
 * Exact, case-sensitive keyword aliases mapping a keyword to the canonical dimension id it stands
 * for. Used at the tracked entity level so that {@code programId.ENROLLMENT_OU} behaves as {@code
 * programId.ou} and {@code programId.enrollmentouname} as {@code programId.ouname}, mirroring the
 * event pipeline. Only the literal spelling/case is aliased; any other casing is left untouched and
 * rejected downstream.
 */
public final class DimensionAliases {
  private DimensionAliases() {}

  private static final Map<String, String> ALIASES =
      Map.of("ENROLLMENT_OU", "ou", "enrollmentouname", "ouname");

  /** Returns the canonical dimension id for an exact keyword alias, or the input unchanged. */
  public static String canonicalize(String dimensionId) {
    if (dimensionId == null) {
      return null;
    }
    return ALIASES.getOrDefault(dimensionId, dimensionId);
  }

  /**
   * Canonicalizes the dimension segment (the part after the last {@code .}) of a possibly
   * program/stage-prefixed header string, leaving the prefix untouched. For example {@code
   * programId.enrollmentouname} becomes {@code programId.ouname}.
   */
  public static String canonicalizeHeader(String header) {
    if (header == null) {
      return null;
    }

    int lastDot = header.lastIndexOf('.');

    if (lastDot < 0) {
      return canonicalize(header);
    }

    return header.substring(0, lastDot + 1) + canonicalize(header.substring(lastDot + 1));
  }
}
