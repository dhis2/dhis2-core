/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.trackedentity.query.context.querybuilder;

import static java.lang.Math.abs;
import static lombok.AccessLevel.PRIVATE;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;

/** Utility class for handling offsets. */
@NoArgsConstructor(access = PRIVATE)
public class OffsetHelper {

  /**
   * Given a stream of objects, a comparator and an offset, returns the object at the specified
   * offset.
   *
   * @param stream the stream of objects
   * @param comparator the comparator to sort the objects
   * @param offset the offset
   * @param <T> the type of the objects
   * @return the object at the specified offset
   */
  public static <T> Optional<T> getItemBasedOnOffset(
      Stream<T> stream, Comparator<T> comparator, int offset) {
    if (offset > 0) { // 1 first (--> skip 0), 2 second (--> skip 1), 3 third (--> skip 2), etc.
      return stream
          // positive offset means sort by ascending date
          .sorted(comparator.reversed())
          .skip(offset - 1L)
          .findFirst();
    }
    // 0 latest, -1 second latest (--> skip 1), -2 third latest (--> skip 2), etc.
    return stream.sorted(comparator).skip(-offset).findFirst();
  }

  /**
   * Returns the offset as a string. If the offset is negative, the absolute value is incremented by
   * 1. This is due to the fact that the row_number() function in SQL starts at 1.
   *
   * @param offset the offset
   * @return the offset as a string
   */
  public static Offset getOffset(Integer offset) {
    if (offset > 0) {
      return new Offset(String.valueOf(offset), "asc");
    }
    // this is due to the fact that the row_number() function in SQL starts at 1
    return new Offset(String.valueOf(abs(offset) + 1), "desc");
  }

  public record Offset(String offset, String direction) {}
}
