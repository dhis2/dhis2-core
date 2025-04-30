/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StatsTest {

  @ParameterizedTest
  @MethodSource("statsValues")
  @DisplayName("New Stats have expected values")
  void updatedStatsHaveExpectedValuesTest(Stats newStats, Stats expectedStats) {
    assertEquals(expectedStats, newStats);
  }

  @Test
  @DisplayName("Stats total has correct value")
  void statsTotalHasCorrectValueTest() {
    Stats stats = new Stats(1, 2, 3, 4);
    assertEquals(10, stats.getTotal());
  }

  public static Stream<Arguments> statsValues() {
    return Stream.of(
        Arguments.of(new Stats(0, 0, 0, 0).createdInc(1), new Stats(1, 0, 0, 0)),
        Arguments.of(new Stats(0, 0, 0, 0).updatedInc(1), new Stats(0, 1, 0, 0)),
        Arguments.of(new Stats(0, 0, 0, 0).deletedInc(1), new Stats(0, 0, 1, 0)),
        Arguments.of(new Stats(0, 0, 0, 0).ignoredInc(1), new Stats(0, 0, 0, 1)),
        Arguments.of(new Stats(3, 4, 5, 6).createdInc(1), new Stats(4, 4, 5, 6)),
        Arguments.of(new Stats(3, 4, 5, 6).updatedInc(1), new Stats(3, 5, 5, 6)),
        Arguments.of(new Stats(3, 4, 5, 6).deletedInc(1), new Stats(3, 4, 6, 6)),
        Arguments.of(new Stats(3, 4, 5, 6).ignoredInc(1), new Stats(3, 4, 5, 7)),
        Arguments.of(new Stats(0, 0, 0, 0).withStats(new Stats(1, 1, 1, 1)), new Stats(1, 1, 1, 1)),
        Arguments.of(
            new Stats(1, 2, 3, 4).withStats(new Stats(9, 9, 9, 0)), new Stats(10, 11, 12, 4)));
  }
}
