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
package org.hisp.dhis.util;

import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A utility meant to check user input values and thus it throws {@link IllegalArgumentException}.
 *
 * @author Jan Bernitt
 * @since 2.44
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Preconditions {

  @Nonnull
  private static <T> T requireNonNull(@CheckForNull T value) {
    if (value == null) {
      try {
        // we want the genuine NPE as cause, so we throw it
        throw new NullPointerException();
      } catch (NullPointerException ex) {
        throw new IllegalArgumentException("Value must not be null", ex);
      }
    }
    return value;
  }

  /**
   * @param value value to check
   * @return the given value if it passes the requirements
   * @param <T> type of the value
   */
  @Nonnull
  public static <T extends CharSequence> T requireNonEmpty(@CheckForNull T value) {
    value = requireNonNull(value);
    if (value.isEmpty()) throw new IllegalArgumentException("String must not be empty");
    return value;
  }

  /**
   * Check that the collection has at least 1 text and all given texts are non-empty.
   *
   * @param value a collection to text values
   * @return the given value if it is non-null, non-empty and all values in it are also non-null,
   *     non-empty texts
   * @param <E> type of text
   * @param <T> type of collection
   */
  @Nonnull
  public static <E extends CharSequence, T extends Collection<E>> T requireNonEmpty(
      @CheckForNull T value) {
    value = requireNonNull(value);
    if (value.isEmpty()) throw new IllegalArgumentException("Collection must not be empty");
    value.forEach(Preconditions::requireNonEmpty);
    return value;
  }
}
